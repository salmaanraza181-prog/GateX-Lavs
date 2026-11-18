package com.example.engine.analog

import com.example.engine.netlist.NetlistBuilder
import com.example.engine.netlist.NetlistGraph
import com.example.model.AnalogSimulationResult
import com.example.model.AnalogWaveformPoint
import com.example.model.CircuitPage
import com.example.model.ComponentCatalog
import com.example.model.ComponentCategory
import com.example.model.ComponentInstance
import com.example.model.ComponentType
import com.example.model.DiagnosticSeverity
import com.example.model.SimulationDiagnostic
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Real Modified Nodal Analysis (MNA) analog circuit solver supporting:
 * - Resistors, Variable Resistors, Potentiometers, Motors, Buzzers, Switches
 * - DC Voltage Sources, Batteries, Bench PSU, VCC, AC Voltage Sources, Function Generators
 * - Current Sources, Photodiodes
 * - Capacitors & Polarized Capacitors (Backward Euler transient companion model)
 * - Inductors (Backward Euler transient companion model)
 * - Diodes, Zener Diodes, LEDs (iterative piecewise-linear solver)
 * - NPN/PNP BJTs & N-MOSFET (piecewise-linear switching/active model)
 * - 7805 Voltage Regulator, SPST Relay, Voltmeter, Ammeter, Ohmmeter, Multimeter
 */
class AnalogSimulationEngine {

    private data class VoltageSourceStamp(
        val componentId: String,
        val posNode: Int,
        val negNode: Int,
        val voltage: Double
    )

    fun solveDcAndTransient(
        page: CircuitPage,
        transientDurationSec: Double = 0.05,
        steps: Int = 50
    ): AnalogSimulationResult {
        val graph = NetlistBuilder.build(page, includeClosedSwitches = true, unionIntactFuses = false)
        val diagnostics = mutableListOf<SimulationDiagnostic>()

        // Report unsupported digital-only ICs explicitly if present in analog pass
        val digitalOnlyComps = page.components.filter {
            val cat = ComponentCatalog.get(it.type).category
            cat == ComponentCategory.LOGIC_GATES || (cat == ComponentCategory.IC_7400 && it.type != ComponentType.IC_555)
        }
        if (digitalOnlyComps.isNotEmpty()) {
            diagnostics.add(
                SimulationDiagnostic(
                    severity = DiagnosticSeverity.INFO,
                    code = "DIGITAL_IN_ANALOG",
                    message = "${digitalOnlyComps.size} digital logic component(s) handled by the Digital Engine rather than MNA."
                )
            )
        }

        // Identify Ground Net(s)
        val groundNets = HashSet<String>()
        for (comp in page.components) {
            if (comp.type == ComponentType.GROUND) {
                graph.pinToNetId[NetlistBuilder.pinKey(comp.id, "gnd")]?.let { groundNets.add(it) }
            }
        }
        // Fallback reference to negative terminal of primary source if explicit GND is missing
        if (groundNets.isEmpty()) {
            val fallbackSource = page.components.firstOrNull {
                it.type == ComponentType.DC_SOURCE || it.type == ComponentType.BATTERY ||
                    it.type == ComponentType.POWER_SUPPLY || it.type == ComponentType.AC_SOURCE ||
                    it.type == ComponentType.FUNCTION_GENERATOR || it.type == ComponentType.VARIABLE_INPUT
            }
            if (fallbackSource != null) {
                val negPinId = when (fallbackSource.type) {
                    ComponentType.FUNCTION_GENERATOR -> "gnd"
                    ComponentType.VARIABLE_INPUT -> "ref"
                    else -> "neg"
                }
                graph.pinToNetId[NetlistBuilder.pinKey(fallbackSource.id, negPinId)]?.let {
                    groundNets.add(it)
                    diagnostics.add(
                        SimulationDiagnostic(
                            severity = DiagnosticSeverity.WARNING,
                            code = "IMPLICIT_GROUND",
                            message = "No explicit GND placed; using ${fallbackSource.label}(-) as 0V reference."
                        )
                    )
                }
            }
        }

        if (groundNets.isEmpty()) {
            if (page.components.isEmpty()) {
                return AnalogSimulationResult(diagnostics = diagnostics, converged = true)
            }
            diagnostics.add(
                SimulationDiagnostic(
                    severity = DiagnosticSeverity.ERROR,
                    code = "NO_GROUND",
                    message = "No Ground (GND) reference node found. Add a GND symbol to solve node voltages."
                )
            )
            return AnalogSimulationResult(diagnostics = diagnostics, converged = false)
        }

        // Map non-ground nets to 1..N indices (0 is ground)
        val nonGroundNets = graph.nets.map { it.id }.filter { it !in groundNets }
        val netToIndex = HashMap<String, Int>()
        groundNets.forEach { netToIndex[it] = 0 }
        nonGroundNets.forEachIndexed { idx, netId -> netToIndex[netId] = idx + 1 }
        val numNodes = nonGroundNets.size

        // Solve DC Operating Point
        val dcSolution = solveMnaStep(
            page = page,
            graph = graph,
            netToIndex = netToIndex,
            numNodes = numNodes,
            timeSec = 0.0,
            dtSec = 0.0,
            isTransient = false,
            prevNodeVoltages = DoubleArray(numNodes + 1),
            prevInductorCurrents = emptyMap(),
            diagnostics = diagnostics
        ) ?: return AnalogSimulationResult(diagnostics = diagnostics, converged = false)

        val nodeVoltages = HashMap<String, Double>()
        for (net in graph.nets) {
            val idx = netToIndex[net.id] ?: 0
            nodeVoltages[net.id] = dcSolution.nodeVoltages[idx]
        }

        val pinVoltages = HashMap<String, Double>()
        for ((pk, netId) in graph.pinToNetId) {
            pinVoltages[pk] = nodeVoltages[netId] ?: 0.0
        }

        // Check polarized capacitors for reverse polarity
        for (comp in page.components) {
            if (comp.type == ComponentType.POLARIZED_CAPACITOR) {
                val vp = pinVoltages[NetlistBuilder.pinKey(comp.id, "pos")] ?: 0.0
                val vn = pinVoltages[NetlistBuilder.pinKey(comp.id, "neg")] ?: 0.0
                if (vp - vn < -0.5) {
                    diagnostics.add(
                        SimulationDiagnostic(
                            severity = DiagnosticSeverity.WARNING,
                            code = "REVERSE_CAP_POLARITY",
                            message = "${comp.label} is reverse-biased (${String.format("%.2f", vp - vn)}V).",
                            componentId = comp.id
                        )
                    )
                }
            }
            if (comp.type == ComponentType.LED) {
                val cur = dcSolution.componentCurrents[comp.id] ?: 0.0
                if (cur > 0.035) {
                    diagnostics.add(
                        SimulationDiagnostic(
                            severity = DiagnosticSeverity.WARNING,
                            code = "LED_OVERCURRENT",
                            message = "${comp.label} current is ${String.format("%.1f", cur * 1000)} mA (>30mA). Add a series resistor.",
                            componentId = comp.id
                        )
                    )
                }
            }
            if (comp.type == ComponentType.RESISTOR || comp.type == ComponentType.VARIABLE_RESISTOR) {
                val pwr = dcSolution.componentPowers[comp.id] ?: 0.0
                if (pwr > 0.25) {
                    diagnostics.add(
                        SimulationDiagnostic(
                            severity = DiagnosticSeverity.WARNING,
                            code = "RESISTOR_OVERHEAT",
                            message = "${comp.label} dissipating ${String.format("%.2f", pwr)} W (>0.25W). Increase resistance to prevent burnout.",
                            componentId = comp.id
                        )
                    )
                }
            }
            if (comp.type == ComponentType.FUSE) {
                val ratingMa = comp.properties["ratingMa"]?.toDoubleOrNull() ?: 250.0
                val alreadyBlown = comp.properties["blown"] == "true"
                val curMa = (dcSolution.componentCurrents[comp.id] ?: 0.0) * 1000.0
                if (alreadyBlown) {
                    diagnostics.add(
                        SimulationDiagnostic(
                            severity = DiagnosticSeverity.WARNING,
                            code = "FUSE_BLOWN",
                            message = "${comp.label} is blown (open circuit). Tap Reset Fuse to restore.",
                            componentId = comp.id
                        )
                    )
                } else if (curMa > ratingMa) {
                    diagnostics.add(
                        SimulationDiagnostic(
                            severity = DiagnosticSeverity.ERROR,
                            code = "FUSE_BLOWN",
                            message = "${comp.label} blew at ${String.format("%.0f", curMa)} mA (exceeds ${ratingMa.toInt()} mA rating).",
                            componentId = comp.id
                        )
                    )
                }
            }
        }

        // Check for floating nodes (all voltages ~0 on disconnected islands)
        val opAmpOptionalPowerPins = page.components
            .filter { it.type == ComponentType.OP_AMP_741 }
            .flatMap { listOf(NetlistBuilder.pinKey(it.id, "vcc"), NetlistBuilder.pinKey(it.id, "vee")) }
            .toSet()
        for (net in graph.nets) {
            if (net.id !in groundNets && net.pinKeys.size <= 1) {
                val lonePin = net.pinKeys.firstOrNull()
                if (lonePin != null && lonePin in opAmpOptionalPowerPins) continue
                diagnostics.add(
                    SimulationDiagnostic(
                        severity = DiagnosticSeverity.WARNING,
                        code = "FLOATING_NODE",
                        message = "Floating pin or open circuit detected on ${lonePin ?: net.id}."
                    )
                )
            }
        }

        // Run Transient Analysis when Capacitors, Inductors, AC sources, or Oscilloscope are present
        val needsTransient = page.components.any {
            it.type == ComponentType.CAPACITOR || it.type == ComponentType.POLARIZED_CAPACITOR ||
                it.type == ComponentType.INDUCTOR || it.type == ComponentType.AC_SOURCE ||
                it.type == ComponentType.FUNCTION_GENERATOR || it.type == ComponentType.OSCILLOSCOPE
        }

        val transientSamples = mutableListOf<AnalogWaveformPoint>()
        if (needsTransient && steps > 0) {
            val dt = transientDurationSec / steps
            var prevV = DoubleArray(numNodes + 1)
            // Initial capacitor voltages from properties (default 0V)
            for (comp in page.components) {
                if (comp.type == ComponentType.CAPACITOR || comp.type == ComponentType.POLARIZED_CAPACITOR) {
                    val vInit = comp.properties["initialV"]?.toDoubleOrNull() ?: 0.0
                    val p1 = if (comp.type == ComponentType.CAPACITOR) "p1" else "pos"
                    val n1 = netToIndex[graph.pinToNetId[NetlistBuilder.pinKey(comp.id, p1)]] ?: 0
                    if (n1 > 0 && vInit != 0.0) prevV[n1] = vInit
                }
            }
            var prevIL = HashMap<String, Double>()

            val osc = page.components.firstOrNull { it.type == ComponentType.OSCILLOSCOPE }
            val probeSource = page.components.firstOrNull {
                it.type == ComponentType.AC_SOURCE || it.type == ComponentType.FUNCTION_GENERATOR ||
                    it.type == ComponentType.DC_SOURCE || it.type == ComponentType.BATTERY
            }
            val probeCap = page.components.firstOrNull {
                it.type == ComponentType.CAPACITOR || it.type == ComponentType.POLARIZED_CAPACITOR ||
                    it.type == ComponentType.DIODE || it.type == ComponentType.RESISTOR
            }

            for (s in 0..steps) {
                val t = s * dt
                val stepSol = solveMnaStep(
                    page = page,
                    graph = graph,
                    netToIndex = netToIndex,
                    numNodes = numNodes,
                    timeSec = t,
                    dtSec = dt,
                    isTransient = true,
                    prevNodeVoltages = prevV,
                    prevInductorCurrents = prevIL,
                    diagnostics = mutableListOf()
                ) ?: break

                prevV = stepSol.nodeVoltages
                prevIL = HashMap(stepSol.inductorCurrents)

                val chMap = LinkedHashMap<String, Double>()
                if (osc != null) {
                    val ch1Idx = netToIndex[graph.pinToNetId[NetlistBuilder.pinKey(osc.id, "ch1")]] ?: 0
                    val ch2Idx = netToIndex[graph.pinToNetId[NetlistBuilder.pinKey(osc.id, "ch2")]] ?: 0
                    val gndIdx = netToIndex[graph.pinToNetId[NetlistBuilder.pinKey(osc.id, "gnd")]] ?: 0
                    chMap["CH1"] = stepSol.nodeVoltages[ch1Idx] - stepSol.nodeVoltages[gndIdx]
                    chMap["CH2"] = stepSol.nodeVoltages[ch2Idx] - stepSol.nodeVoltages[gndIdx]
                } else {
                    if (probeSource != null) {
                        val posPin = if (probeSource.type == ComponentType.FUNCTION_GENERATOR) "out" else "pos"
                        val idx = netToIndex[graph.pinToNetId[NetlistBuilder.pinKey(probeSource.id, posPin)]] ?: 0
                        chMap["CH1 (${probeSource.label})"] = stepSol.nodeVoltages[idx]
                    }
                    if (probeCap != null) {
                        val pin = when (probeCap.type) {
                            ComponentType.POLARIZED_CAPACITOR -> "pos"
                            ComponentType.DIODE -> "cathode"
                            else -> "p1"
                        }
                        val idx = netToIndex[graph.pinToNetId[NetlistBuilder.pinKey(probeCap.id, pin)]] ?: 0
                        chMap["CH2 (${probeCap.label})"] = stepSol.nodeVoltages[idx]
                    }
                }
                transientSamples.add(AnalogWaveformPoint(t, chMap))
            }
        }

        // AC reactance summary if AC source + C or L exist
        val acSource = page.components.firstOrNull {
            it.type == ComponentType.AC_SOURCE || it.type == ComponentType.FUNCTION_GENERATOR
        }
        val acSummary = if (acSource != null) {
            val freq = acSource.properties["frequency"]?.toDoubleOrNull() ?: 50.0
            val caps = page.components.filter { it.type == ComponentType.CAPACITOR || it.type == ComponentType.POLARIZED_CAPACITOR }
            val inds = page.components.filter { it.type == ComponentType.INDUCTOR }
            buildString {
                append("f = ${ComponentCatalog.formatEngineeringValue(freq, "Hz")}")
                caps.firstOrNull()?.let { c ->
                    val cVal = ComponentCatalog.parseEngineeringValue(c.value, 1e-4).coerceAtLeast(1e-12)
                    val xc = 1.0 / (2.0 * PI * freq * cVal)
                    append(" | Xc(${c.label}) = ${ComponentCatalog.formatEngineeringValue(xc, "Ω")}")
                }
                inds.firstOrNull()?.let { l ->
                    val lVal = ComponentCatalog.parseEngineeringValue(l.value, 1e-2).coerceAtLeast(1e-12)
                    val xl = 2.0 * PI * freq * lVal
                    append(" | Xl(${l.label}) = ${ComponentCatalog.formatEngineeringValue(xl, "Ω")}")
                }
            }
        } else null

        // Ohmmeter equivalent resistance if Ohmmeter placed
        val ohmmeter = page.components.firstOrNull { it.type == ComponentType.OHMMETER }
        val eqResistance = if (ohmmeter != null) {
            measureEquivalentResistance(page, graph, netToIndex, numNodes, ohmmeter)
        } else null

        return AnalogSimulationResult(
            nodeVoltages = nodeVoltages,
            pinVoltages = pinVoltages,
            componentCurrents = dcSolution.componentCurrents,
            componentPowers = dcSolution.componentPowers,
            equivalentResistanceOhms = eqResistance,
            transientSamples = transientSamples,
            acSweepSummary = acSummary,
            diagnostics = diagnostics,
            converged = true
        )
    }

    private data class StepSolution(
        val nodeVoltages: DoubleArray,
        val componentCurrents: Map<String, Double>,
        val componentPowers: Map<String, Double>,
        val inductorCurrents: Map<String, Double>
    )

    private fun solveMnaStep(
        page: CircuitPage,
        graph: NetlistGraph,
        netToIndex: Map<String, Int>,
        numNodes: Int,
        timeSec: Double,
        dtSec: Double,
        isTransient: Boolean,
        prevNodeVoltages: DoubleArray,
        prevInductorCurrents: Map<String, Double>,
        diagnostics: MutableList<SimulationDiagnostic>
    ): StepSolution? {
        fun nodeOf(compId: String, pinId: String): Int {
            val netId = graph.pinToNetId[NetlistBuilder.pinKey(compId, pinId)] ?: return -1
            return netToIndex[netId] ?: 0
        }

        // Collect voltage sources
        val voltageSources = mutableListOf<VoltageSourceStamp>()
        for (comp in page.components) {
            when (comp.type) {
                ComponentType.DC_SOURCE, ComponentType.BATTERY, ComponentType.POWER_SUPPLY -> {
                    val p = nodeOf(comp.id, "pos")
                    val n = nodeOf(comp.id, "neg")
                    if (p >= 0 && n >= 0 && p != n) {
                        val v = comp.properties["voltage"]?.toDoubleOrNull()
                            ?: ComponentCatalog.parseEngineeringValue(comp.value, 5.0)
                        voltageSources.add(VoltageSourceStamp(comp.id, p, n, v))
                    }
                }
                ComponentType.VARIABLE_INPUT -> {
                    val p = nodeOf(comp.id, "out")
                    val n = nodeOf(comp.id, "ref")
                    if (p >= 0 && n >= 0 && p != n) {
                        val v = comp.properties["voltage"]?.toDoubleOrNull()
                            ?: ComponentCatalog.parseEngineeringValue(comp.value, 3.3)
                        voltageSources.add(VoltageSourceStamp(comp.id, p, n, v))
                    }
                }
                ComponentType.VCC -> {
                    val p = nodeOf(comp.id, "vcc")
                    if (p > 0) {
                        val v = comp.properties["voltage"]?.toDoubleOrNull() ?: 5.0
                        voltageSources.add(VoltageSourceStamp(comp.id, p, 0, v))
                    }
                }
                ComponentType.AC_SOURCE -> {
                    val p = nodeOf(comp.id, "pos")
                    val n = nodeOf(comp.id, "neg")
                    if (p >= 0 && n >= 0 && p != n) {
                        val amp = comp.properties["amplitude"]?.toDoubleOrNull() ?: 5.0
                        val freq = comp.properties["frequency"]?.toDoubleOrNull() ?: 50.0
                        val v = if (isTransient) amp * sin(2.0 * PI * freq * timeSec) else amp
                        voltageSources.add(VoltageSourceStamp(comp.id, p, n, v))
                    }
                }
                ComponentType.FUNCTION_GENERATOR -> {
                    val p = nodeOf(comp.id, "out")
                    val n = nodeOf(comp.id, "gnd")
                    if (p >= 0 && n >= 0 && p != n) {
                        val amp = comp.properties["amplitude"]?.toDoubleOrNull() ?: 5.0
                        val freq = comp.properties["frequency"]?.toDoubleOrNull() ?: 1000.0
                        val offset = comp.properties["offset"]?.toDoubleOrNull() ?: 0.0
                        val wave = comp.properties["waveform"] ?: "SINE"
                        val v = if (!isTransient) {
                            offset + amp
                        } else {
                            val phase = (freq * timeSec) % 1.0
                            val raw = when (wave) {
                                "SQUARE" -> if (phase < 0.5) 1.0 else -1.0
                                "TRIANGLE" -> (2.0 / PI) * asin(sin(2.0 * PI * freq * timeSec))
                                else -> sin(2.0 * PI * freq * timeSec)
                            }
                            offset + amp * raw
                        }
                        voltageSources.add(VoltageSourceStamp(comp.id, p, n, v))
                    }
                }
                else -> Unit
            }
        }

        // Track nonlinear diode/transistor states over up to 12 piecewise-linear iterations
        val diodeConducting = HashMap<String, Boolean>()
        val zenerBreakdown = HashMap<String, Boolean>()
        val bjtOn = HashMap<String, Boolean>()
        val mosfetOn = HashMap<String, Boolean>()
        val relayClosed = HashMap<String, Boolean>()
        val vswClosed = HashMap<String, Boolean>()
        // 0 = linear VCVS, +1 = clamped to V+, -1 = clamped to V-
        val opAmpClamp = HashMap<String, Int>()
        val netPinCount = graph.nets.associate { it.id to it.pinKeys.size }

        var lastSolution: DoubleArray? = null
        val m = voltageSources.size
        val size = numNodes + m
        if (size == 0) {
            return StepSolution(DoubleArray(numNodes + 1), emptyMap(), emptyMap(), emptyMap())
        }

        for (nlIter in 0 until 12) {
            val a = Array(size) { DoubleArray(size) }
            val b = DoubleArray(size)

            // Tiny Gmin conductance from every non-ground node to ground to prevent singular floating subcircuits
            for (i in 0 until numNodes) {
                a[i][i] += 1e-9
            }

            fun stampConductance(n1: Int, n2: Int, g: Double) {
                if (n1 < 0 || n2 < 0 || n1 == n2) return
                if (n1 > 0) a[n1 - 1][n1 - 1] += g
                if (n2 > 0) a[n2 - 1][n2 - 1] += g
                if (n1 > 0 && n2 > 0) {
                    a[n1 - 1][n2 - 1] -= g
                    a[n2 - 1][n1 - 1] -= g
                }
            }

            fun stampCurrentSource(fromNode: Int, toNode: Int, current: Double) {
                if (fromNode > 0) b[fromNode - 1] -= current
                if (toNode > 0) b[toNode - 1] += current
            }

            // Stamp linear & piecewise-linear passive/active elements
            for (comp in page.components) {
                when (comp.type) {
                    ComponentType.RESISTOR -> {
                        val r = ComponentCatalog.parseEngineeringValue(comp.value, 1000.0).coerceAtLeast(1e-3)
                        stampConductance(nodeOf(comp.id, "p1"), nodeOf(comp.id, "p2"), 1.0 / r)
                    }
                    ComponentType.VARIABLE_RESISTOR -> {
                        val rTot = ComponentCatalog.parseEngineeringValue(comp.value, 10000.0).coerceAtLeast(1.0)
                        val pct = (comp.properties["positionPct"]?.toDoubleOrNull() ?: 50.0).coerceIn(1.0, 100.0) / 100.0
                        stampConductance(nodeOf(comp.id, "p1"), nodeOf(comp.id, "p2"), 1.0 / (rTot * pct))
                    }
                    ComponentType.POTENTIOMETER -> {
                        val rTot = ComponentCatalog.parseEngineeringValue(comp.value, 10000.0).coerceAtLeast(1.0)
                        val pct = (comp.properties["wiperPct"]?.toDoubleOrNull() ?: 50.0).coerceIn(1.0, 99.0) / 100.0
                        val r1 = (rTot * (1.0 - pct)).coerceAtLeast(1.0)
                        val r2 = (rTot * pct).coerceAtLeast(1.0)
                        val n1 = nodeOf(comp.id, "p1")
                        val n2 = nodeOf(comp.id, "p2")
                        val nw = nodeOf(comp.id, "wiper")
                        stampConductance(n1, nw, 1.0 / r1)
                        stampConductance(nw, n2, 1.0 / r2)
                    }
                    ComponentType.CAPACITOR, ComponentType.POLARIZED_CAPACITOR -> {
                        val pPin = if (comp.type == ComponentType.CAPACITOR) "p1" else "pos"
                        val nPin = if (comp.type == ComponentType.CAPACITOR) "p2" else "neg"
                        val n1 = nodeOf(comp.id, pPin)
                        val n2 = nodeOf(comp.id, nPin)
                        if (isTransient && dtSec > 0.0) {
                            val c = ComponentCatalog.parseEngineeringValue(comp.value, 1e-4).coerceAtLeast(1e-12)
                            val geq = c / dtSec
                            val vPrev = (if (n1 >= 0) prevNodeVoltages[n1] else 0.0) -
                                (if (n2 >= 0) prevNodeVoltages[n2] else 0.0)
                            stampConductance(n1, n2, geq)
                            stampCurrentSource(n2, n1, geq * vPrev)
                        } else {
                            stampConductance(n1, n2, 1e-9)
                        }
                    }
                    ComponentType.INDUCTOR -> {
                        val n1 = nodeOf(comp.id, "p1")
                        val n2 = nodeOf(comp.id, "p2")
                        val dcr = comp.properties["dcrOhms"]?.toDoubleOrNull() ?: 0.5
                        if (isTransient && dtSec > 0.0) {
                            val l = ComponentCatalog.parseEngineeringValue(comp.value, 1e-2).coerceAtLeast(1e-9)
                            val geq = dtSec / l
                            val iPrev = prevInductorCurrents[comp.id] ?: 0.0
                            stampConductance(n1, n2, geq)
                            stampCurrentSource(n1, n2, iPrev)
                        } else {
                            stampConductance(n1, n2, 1.0 / dcr.coerceAtLeast(0.05))
                        }
                    }
                    ComponentType.CURRENT_SOURCE -> {
                        val iVal = comp.properties["current"]?.toDoubleOrNull()
                            ?: ComponentCatalog.parseEngineeringValue(comp.value, 0.01)
                        stampCurrentSource(nodeOf(comp.id, "in"), nodeOf(comp.id, "out"), iVal)
                    }
                    ComponentType.PHOTODIODE -> {
                        val lux = comp.properties["lux"]?.toDoubleOrNull() ?: 500.0
                        val iPhoto = lux * 0.2e-6
                        stampCurrentSource(nodeOf(comp.id, "cathode"), nodeOf(comp.id, "anode"), iPhoto)
                        stampConductance(nodeOf(comp.id, "anode"), nodeOf(comp.id, "cathode"), 1e-6)
                    }
                    ComponentType.DIODE, ComponentType.LED -> {
                        val nA = nodeOf(comp.id, "anode")
                        val nK = nodeOf(comp.id, "cathode")
                        val vf = comp.properties["vf"]?.toDoubleOrNull()
                            ?: if (comp.type == ComponentType.LED) 2.0 else 0.7
                        val ron = if (comp.type == ComponentType.LED) 15.0 else 5.0
                        if (diodeConducting[comp.id] == true) {
                            val g = 1.0 / ron
                            stampConductance(nA, nK, g)
                            // Equivalent Norton source modeling Vf drop: I_A->K = g*(Va - Vk - Vf)
                            stampCurrentSource(nK, nA, vf * g)
                        } else {
                            stampConductance(nA, nK, 1e-7)
                        }
                    }
                    ComponentType.ZENER_DIODE -> {
                        val nA = nodeOf(comp.id, "anode")
                        val nK = nodeOf(comp.id, "cathode")
                        val vf = comp.properties["vf"]?.toDoubleOrNull() ?: 0.7
                        val vz = comp.properties["vz"]?.toDoubleOrNull() ?: 5.1
                        when {
                            diodeConducting[comp.id] == true -> {
                                val g = 1.0 / 5.0
                                stampConductance(nA, nK, g)
                                stampCurrentSource(nK, nA, vf * g)
                            }
                            zenerBreakdown[comp.id] == true -> {
                                val g = 1.0 / 5.0
                                stampConductance(nA, nK, g)
                                stampCurrentSource(nA, nK, vz * g)
                            }
                            else -> stampConductance(nA, nK, 1e-7)
                        }
                    }
                    ComponentType.TRANSISTOR_NPN -> {
                        val nB = nodeOf(comp.id, "base")
                        val nC = nodeOf(comp.id, "collector")
                        val nE = nodeOf(comp.id, "emitter")
                        stampConductance(nB, nE, 1.0 / 1500.0)
                        if (bjtOn[comp.id] == true) {
                            stampConductance(nC, nE, 1.0 / 25.0)
                        } else {
                            stampConductance(nC, nE, 1e-7)
                        }
                    }
                    ComponentType.TRANSISTOR_PNP -> {
                        val nB = nodeOf(comp.id, "base")
                        val nE = nodeOf(comp.id, "emitter")
                        val nC = nodeOf(comp.id, "collector")
                        stampConductance(nE, nB, 1.0 / 1500.0)
                        if (bjtOn[comp.id] == true) {
                            stampConductance(nE, nC, 1.0 / 25.0)
                        } else {
                            stampConductance(nE, nC, 1e-7)
                        }
                    }
                    ComponentType.MOSFET -> {
                        val nG = nodeOf(comp.id, "gate")
                        val nD = nodeOf(comp.id, "drain")
                        val nS = nodeOf(comp.id, "source")
                        stampConductance(nG, nS, 1e-8)
                        val rds = if (mosfetOn[comp.id] == true) {
                            comp.properties["rdsOn"]?.toDoubleOrNull() ?: 0.1
                        } else 1e7
                        stampConductance(nD, nS, 1.0 / rds)
                    }
                    ComponentType.VOLTAGE_REGULATOR -> {
                        val nIn = nodeOf(comp.id, "in")
                        val nGnd = nodeOf(comp.id, "gnd")
                        val nOut = nodeOf(comp.id, "out")
                        val vout = comp.properties["vout"]?.toDoubleOrNull() ?: 5.0
                        stampConductance(nIn, nGnd, 1.0 / 1000.0)
                        // Regulated Norton source on output
                        val gOut = 1.0 / 0.5
                        stampConductance(nOut, nGnd, gOut)
                        stampCurrentSource(nGnd, nOut, vout * gOut)
                    }
                    ComponentType.RELAY -> {
                        val c1 = nodeOf(comp.id, "coil1")
                        val c2 = nodeOf(comp.id, "coil2")
                        val rCoil = comp.properties["coilResistance"]?.toDoubleOrNull() ?: 100.0
                        stampConductance(c1, c2, 1.0 / rCoil)
                        val com = nodeOf(comp.id, "com")
                        val no = nodeOf(comp.id, "no")
                        val gContact = if (relayClosed[comp.id] == true) 10.0 else 1e-8
                        stampConductance(com, no, gContact)
                    }
                    ComponentType.RELAY_SPDT -> {
                        val c1 = nodeOf(comp.id, "coil1")
                        val c2 = nodeOf(comp.id, "coil2")
                        val rCoil = comp.properties["coilResistance"]?.toDoubleOrNull() ?: 100.0
                        stampConductance(c1, c2, 1.0 / rCoil)
                        val com = nodeOf(comp.id, "com")
                        val no = nodeOf(comp.id, "no")
                        val nc = nodeOf(comp.id, "nc")
                        val energized = relayClosed[comp.id] == true
                        stampConductance(com, no, if (energized) 10.0 else 1e-8)
                        stampConductance(com, nc, if (energized) 1e-8 else 10.0)
                    }
                    ComponentType.VOLTAGE_CONTROLLED_SWITCH -> {
                        val cp = nodeOf(comp.id, "ctrl_pos")
                        val cn = nodeOf(comp.id, "ctrl_neg")
                        stampConductance(cp, cn, 1e-8)
                        val sw1 = nodeOf(comp.id, "sw1")
                        val sw2 = nodeOf(comp.id, "sw2")
                        val ron = (comp.properties["ron"]?.toDoubleOrNull() ?: 0.05).coerceAtLeast(1e-3)
                        stampConductance(sw1, sw2, if (vswClosed[comp.id] == true) 1.0 / ron else 1e-8)
                    }
                    ComponentType.MOTOR -> {
                        val r = comp.properties["resistance"]?.toDoubleOrNull() ?: 24.0
                        stampConductance(nodeOf(comp.id, "pos"), nodeOf(comp.id, "neg"), 1.0 / r)
                    }
                    ComponentType.BUZZER -> {
                        val r = comp.properties["resistance"]?.toDoubleOrNull() ?: 1000.0
                        stampConductance(nodeOf(comp.id, "pos"), nodeOf(comp.id, "neg"), 1.0 / r)
                    }
                    ComponentType.PILOT_LAMP -> {
                        val r = (comp.properties["resistance"]?.toDoubleOrNull() ?: 50.0).coerceAtLeast(1.0)
                        stampConductance(nodeOf(comp.id, "p1"), nodeOf(comp.id, "p2"), 1.0 / r)
                    }
                    ComponentType.VOLTMETER, ComponentType.MULTIMETER -> {
                        val p1 = if (comp.type == ComponentType.VOLTMETER) "neg" else "com"
                        stampConductance(nodeOf(comp.id, "pos"), nodeOf(comp.id, p1), 1e-7)
                    }
                    ComponentType.AMMETER -> {
                        stampConductance(nodeOf(comp.id, "pos"), nodeOf(comp.id, "neg"), 1000.0)
                    }
                    ComponentType.FUSE -> {
                        val blown = comp.properties["blown"] == "true"
                        val gFuse = if (blown) 1e-9 else 1.0 / 0.05
                        stampConductance(nodeOf(comp.id, "p1"), nodeOf(comp.id, "p2"), gFuse)
                    }
                    ComponentType.OP_AMP_741 -> {
                        val nPos = nodeOf(comp.id, "in_pos")
                        val nNeg = nodeOf(comp.id, "in_neg")
                        val nOut = nodeOf(comp.id, "out")
                        if (nPos >= 0 && nNeg >= 0 && nOut > 0) {
                            // High differential input impedance (10 MOhm)
                            stampConductance(nPos, nNeg, 1e-7)
                            val gain = (comp.properties["gain"]?.toDoubleOrNull() ?: 1000.0).coerceIn(10.0, 1e5)
                            val vPosRail = comp.properties["vPosRail"]?.toDoubleOrNull() ?: 12.0
                            val vNegRail = comp.properties["vNegRail"]?.toDoubleOrNull() ?: -12.0
                            val gOut = 1.0 / 25.0 // 25 Ohm output impedance
                            when (opAmpClamp[comp.id] ?: 0) {
                                1 -> {
                                    a[nOut - 1][nOut - 1] += gOut
                                    b[nOut - 1] += vPosRail * gOut
                                }
                                -1 -> {
                                    a[nOut - 1][nOut - 1] += gOut
                                    b[nOut - 1] += vNegRail * gOut
                                }
                                else -> {
                                    // Exact MNA VCVS stamp: gOut * (Vout - Aol * (V+ - V-)) = 0
                                    a[nOut - 1][nOut - 1] += gOut
                                    if (nPos > 0) a[nOut - 1][nPos - 1] -= gOut * gain
                                    if (nNeg > 0) a[nOut - 1][nNeg - 1] += gOut * gain
                                }
                            }
                        }
                    }
                    else -> Unit
                }
            }

            // Stamp independent voltage sources
            voltageSources.forEachIndexed { idx, vs ->
                val row = numNodes + idx
                if (vs.posNode > 0) {
                    a[vs.posNode - 1][row] += 1.0
                    a[row][vs.posNode - 1] += 1.0
                }
                if (vs.negNode > 0) {
                    a[vs.negNode - 1][row] -= 1.0
                    a[row][vs.negNode - 1] -= 1.0
                }
                b[row] = vs.voltage
            }

            val solved = gaussianElimination(a, b)
            if (solved == null) {
                diagnostics.add(
                    SimulationDiagnostic(
                        severity = DiagnosticSeverity.ERROR,
                        code = "SINGULAR_MATRIX",
                        message = "Circuit could not converge (possible voltage source short-circuit loop or missing reference)."
                    )
                )
                return null
            }
            lastSolution = solved

            // Update nonlinear states & check convergence
            var changed = false
            fun vOf(n: Int): Double = if (n > 0) solved[n - 1] else 0.0

            for (comp in page.components) {
                when (comp.type) {
                    ComponentType.DIODE, ComponentType.LED -> {
                        val va = vOf(nodeOf(comp.id, "anode"))
                        val vk = vOf(nodeOf(comp.id, "cathode"))
                        val vf = comp.properties["vf"]?.toDoubleOrNull()
                            ?: if (comp.type == ComponentType.LED) 2.0 else 0.7
                        val shouldConduct = (va - vk) >= vf * 0.95
                        if ((diodeConducting[comp.id] ?: false) != shouldConduct) {
                            diodeConducting[comp.id] = shouldConduct
                            changed = true
                        }
                    }
                    ComponentType.ZENER_DIODE -> {
                        val va = vOf(nodeOf(comp.id, "anode"))
                        val vk = vOf(nodeOf(comp.id, "cathode"))
                        val vf = comp.properties["vf"]?.toDoubleOrNull() ?: 0.7
                        val vz = comp.properties["vz"]?.toDoubleOrNull() ?: 5.1
                        val fwd = (va - vk) >= vf * 0.95
                        val rev = (vk - va) >= vz * 0.95
                        if ((diodeConducting[comp.id] ?: false) != fwd || (zenerBreakdown[comp.id] ?: false) != rev) {
                            diodeConducting[comp.id] = fwd
                            zenerBreakdown[comp.id] = rev
                            changed = true
                        }
                    }
                    ComponentType.TRANSISTOR_NPN -> {
                        val vb = vOf(nodeOf(comp.id, "base"))
                        val ve = vOf(nodeOf(comp.id, "emitter"))
                        val on = (vb - ve) >= 0.65
                        if ((bjtOn[comp.id] ?: false) != on) {
                            bjtOn[comp.id] = on
                            changed = true
                        }
                    }
                    ComponentType.TRANSISTOR_PNP -> {
                        val vb = vOf(nodeOf(comp.id, "base"))
                        val ve = vOf(nodeOf(comp.id, "emitter"))
                        val on = (ve - vb) >= 0.65
                        if ((bjtOn[comp.id] ?: false) != on) {
                            bjtOn[comp.id] = on
                            changed = true
                        }
                    }
                    ComponentType.MOSFET -> {
                        val vg = vOf(nodeOf(comp.id, "gate"))
                        val vs = vOf(nodeOf(comp.id, "source"))
                        val vth = comp.properties["vth"]?.toDoubleOrNull() ?: 2.0
                        val on = (vg - vs) >= vth
                        if ((mosfetOn[comp.id] ?: false) != on) {
                            mosfetOn[comp.id] = on
                            changed = true
                        }
                    }
                    ComponentType.RELAY, ComponentType.RELAY_SPDT -> {
                        val v1 = vOf(nodeOf(comp.id, "coil1"))
                        val v2 = vOf(nodeOf(comp.id, "coil2"))
                        val closed = abs(v1 - v2) >= 3.5
                        if ((relayClosed[comp.id] ?: false) != closed) {
                            relayClosed[comp.id] = closed
                            changed = true
                        }
                    }
                    ComponentType.VOLTAGE_CONTROLLED_SWITCH -> {
                        val vp = vOf(nodeOf(comp.id, "ctrl_pos"))
                        val vn = vOf(nodeOf(comp.id, "ctrl_neg"))
                        val von = comp.properties["von"]?.toDoubleOrNull() ?: 2.5
                        val closed = (vp - vn) >= von
                        if ((vswClosed[comp.id] ?: false) != closed) {
                            vswClosed[comp.id] = closed
                            changed = true
                        }
                    }
                    ComponentType.OP_AMP_741 -> {
                        val nPos = nodeOf(comp.id, "in_pos")
                        val nNeg = nodeOf(comp.id, "in_neg")
                        val nOut = nodeOf(comp.id, "out")
                        if (nPos >= 0 && nNeg >= 0 && nOut > 0) {
                            val gain = (comp.properties["gain"]?.toDoubleOrNull() ?: 1000.0).coerceIn(10.0, 1e5)
                            val vccNet = graph.pinToNetId[NetlistBuilder.pinKey(comp.id, "vcc")]
                            val veeNet = graph.pinToNetId[NetlistBuilder.pinKey(comp.id, "vee")]
                            val vPosRail = if (vccNet != null && (netPinCount[vccNet] ?: 0) >= 2) {
                                vOf(nodeOf(comp.id, "vcc"))
                            } else {
                                comp.properties["vPosRail"]?.toDoubleOrNull() ?: 12.0
                            }
                            val vNegRail = if (veeNet != null && (netPinCount[veeNet] ?: 0) >= 2) {
                                vOf(nodeOf(comp.id, "vee"))
                            } else {
                                comp.properties["vNegRail"]?.toDoubleOrNull() ?: -12.0
                            }
                            val vOut = vOf(nOut)
                            val vIdeal = gain * (vOf(nPos) - vOf(nNeg))
                            val curClamp = opAmpClamp[comp.id] ?: 0
                            val nextClamp = when (curClamp) {
                                0 -> when {
                                    vOut > vPosRail + 0.05 -> 1
                                    vOut < vNegRail - 0.05 -> -1
                                    else -> 0
                                }
                                1 -> if (vIdeal < vPosRail - 0.05) 0 else 1
                                -1 -> if (vIdeal > vNegRail + 0.05) 0 else -1
                                else -> 0
                            }
                            if (nextClamp != curClamp) {
                                opAmpClamp[comp.id] = nextClamp
                                changed = true
                            }
                        }
                    }
                    else -> Unit
                }
            }
            if (!changed) break
        }

        val x = lastSolution ?: return null
        val nodeV = DoubleArray(numNodes + 1)
        for (i in 1..numNodes) {
            nodeV[i] = x[i - 1]
        }

        val currents = HashMap<String, Double>()
        val powers = HashMap<String, Double>()
        val indCurrents = HashMap<String, Double>()

        fun vPin(compId: String, pinId: String): Double {
            val idx = nodeOf(compId, pinId)
            return if (idx > 0) nodeV[idx] else 0.0
        }

        voltageSources.forEachIndexed { idx, vs ->
            val iSrc = abs(x[numNodes + idx])
            currents[vs.componentId] = iSrc
            powers[vs.componentId] = abs(vs.voltage * iSrc)
        }

        for (comp in page.components) {
            when (comp.type) {
                ComponentType.RESISTOR -> {
                    val r = ComponentCatalog.parseEngineeringValue(comp.value, 1000.0).coerceAtLeast(1e-3)
                    val dv = vPin(comp.id, "p1") - vPin(comp.id, "p2")
                    val i = dv / r
                    currents[comp.id] = abs(i)
                    powers[comp.id] = dv * i
                }
                ComponentType.VARIABLE_RESISTOR -> {
                    val rTot = ComponentCatalog.parseEngineeringValue(comp.value, 10000.0).coerceAtLeast(1.0)
                    val pct = (comp.properties["positionPct"]?.toDoubleOrNull() ?: 50.0).coerceIn(1.0, 100.0) / 100.0
                    val r = rTot * pct
                    val dv = vPin(comp.id, "p1") - vPin(comp.id, "p2")
                    val i = dv / r
                    currents[comp.id] = abs(i)
                    powers[comp.id] = dv * i
                }
                ComponentType.DIODE, ComponentType.LED -> {
                    val va = vPin(comp.id, "anode")
                    val vk = vPin(comp.id, "cathode")
                    val vf = comp.properties["vf"]?.toDoubleOrNull()
                        ?: if (comp.type == ComponentType.LED) 2.0 else 0.7
                    val ron = if (comp.type == ComponentType.LED) 15.0 else 5.0
                    val i = if (va - vk > vf * 0.95) ((va - vk - vf) / ron).coerceAtLeast(0.0) else 0.0
                    currents[comp.id] = i
                    powers[comp.id] = (va - vk) * i
                }
                ComponentType.INDUCTOR -> {
                    val dv = vPin(comp.id, "p1") - vPin(comp.id, "p2")
                    val i = if (isTransient && dtSec > 0.0) {
                        val l = ComponentCatalog.parseEngineeringValue(comp.value, 1e-2).coerceAtLeast(1e-9)
                        (prevInductorCurrents[comp.id] ?: 0.0) + (dtSec / l) * dv
                    } else {
                        val dcr = comp.properties["dcrOhms"]?.toDoubleOrNull() ?: 0.5
                        dv / dcr
                    }
                    indCurrents[comp.id] = i
                    currents[comp.id] = abs(i)
                }
                ComponentType.AMMETER -> {
                    val dv = vPin(comp.id, "pos") - vPin(comp.id, "neg")
                    currents[comp.id] = dv * 1000.0 // 1mOhm shunt
                }
                ComponentType.MOTOR -> {
                    val r = comp.properties["resistance"]?.toDoubleOrNull() ?: 24.0
                    val dv = vPin(comp.id, "pos") - vPin(comp.id, "neg")
                    val i = dv / r
                    currents[comp.id] = abs(i)
                    powers[comp.id] = abs(dv * i)
                }
                ComponentType.PILOT_LAMP -> {
                    val r = (comp.properties["resistance"]?.toDoubleOrNull() ?: 50.0).coerceAtLeast(1.0)
                    val dv = vPin(comp.id, "p1") - vPin(comp.id, "p2")
                    val i = dv / r
                    currents[comp.id] = abs(i)
                    powers[comp.id] = abs(dv * i)
                }
                ComponentType.BUZZER -> {
                    val r = (comp.properties["resistance"]?.toDoubleOrNull() ?: 1000.0).coerceAtLeast(1.0)
                    val dv = vPin(comp.id, "pos") - vPin(comp.id, "neg")
                    val i = dv / r
                    currents[comp.id] = abs(i)
                    powers[comp.id] = abs(dv * i)
                }
                ComponentType.FUSE -> {
                    val blown = comp.properties["blown"] == "true"
                    val r = if (blown) 1e9 else 0.05
                    val dv = vPin(comp.id, "p1") - vPin(comp.id, "p2")
                    val i = dv / r
                    currents[comp.id] = abs(i)
                    powers[comp.id] = abs(dv * i)
                }
                else -> Unit
            }
        }

        return StepSolution(nodeV, currents, powers, indCurrents)
    }

    private fun measureEquivalentResistance(
        page: CircuitPage,
        graph: NetlistGraph,
        netToIndex: Map<String, Int>,
        numNodes: Int,
        ohmmeter: ComponentInstance
    ): Double? {
        val posNet = graph.pinToNetId[NetlistBuilder.pinKey(ohmmeter.id, "pos")] ?: return null
        val negNet = graph.pinToNetId[NetlistBuilder.pinKey(ohmmeter.id, "neg")] ?: return null
        if (posNet == negNet) return 0.0

        // Build passive-only network with 1A test current injected across ohmmeter probes
        val passiveComponents = page.components.map { comp ->
            if (comp.type == ComponentType.DC_SOURCE || comp.type == ComponentType.BATTERY ||
                comp.type == ComponentType.POWER_SUPPLY || comp.type == ComponentType.AC_SOURCE ||
                comp.type == ComponentType.FUNCTION_GENERATOR
            ) {
                comp.copy(properties = comp.properties + ("voltage" to "0.0"), value = "0V")
            } else comp
        }
        val testGraph = NetlistBuilder.build(page.copy(components = passiveComponents))
        val testNetToIndex = HashMap<String, Int>()
        testNetToIndex[negNet] = 0
        var nextIdx = 1
        for (net in testGraph.nets) {
            if (net.id != negNet) testNetToIndex[net.id] = nextIdx++
        }
        val n = nextIdx - 1
        if (n <= 0) return null
        val a = Array(n) { DoubleArray(n) }
        val b = DoubleArray(n)
        for (i in 0 until n) a[i][i] += 1e-10

        for (comp in passiveComponents) {
            if (comp.type == ComponentType.RESISTOR) {
                val n1 = testNetToIndex[testGraph.pinToNetId[NetlistBuilder.pinKey(comp.id, "p1")]] ?: -1
                val n2 = testNetToIndex[testGraph.pinToNetId[NetlistBuilder.pinKey(comp.id, "p2")]] ?: -1
                val r = ComponentCatalog.parseEngineeringValue(comp.value, 1000.0).coerceAtLeast(1e-3)
                val g = 1.0 / r
                if (n1 >= 0 && n2 >= 0 && n1 != n2) {
                    if (n1 > 0) a[n1 - 1][n1 - 1] += g
                    if (n2 > 0) a[n2 - 1][n2 - 1] += g
                    if (n1 > 0 && n2 > 0) {
                        a[n1 - 1][n2 - 1] -= g
                        a[n2 - 1][n1 - 1] -= g
                    }
                }
            }
        }
        val posIdx = testNetToIndex[posNet] ?: return null
        if (posIdx > 0) b[posIdx - 1] = 1.0 // 1A test current
        val sol = gaussianElimination(a, b) ?: return null
        val vTest = if (posIdx > 0) sol[posIdx - 1] else 0.0
        return if (vTest > 1e8) Double.POSITIVE_INFINITY else vTest
    }

    private fun gaussianElimination(matrix: Array<DoubleArray>, rhs: DoubleArray): DoubleArray? {
        val n = rhs.size
        val a = Array(n) { r -> matrix[r].clone() }
        val b = rhs.clone()

        for (col in 0 until n) {
            var pivot = col
            var maxVal = abs(a[col][col])
            for (row in col + 1 until n) {
                val v = abs(a[row][col])
                if (v > maxVal) {
                    maxVal = v
                    pivot = row
                }
            }
            if (maxVal < 1e-14) return null

            if (pivot != col) {
                val tmpRow = a[col]
                a[col] = a[pivot]
                a[pivot] = tmpRow
                val tmpB = b[col]
                b[col] = b[pivot]
                b[pivot] = tmpB
            }

            val diag = a[col][col]
            for (row in col + 1 until n) {
                val factor = a[row][col] / diag
                if (factor != 0.0) {
                    for (k in col until n) {
                        a[row][k] -= factor * a[col][k]
                    }
                    b[row] -= factor * b[col]
                }
            }
        }

        val x = DoubleArray(n)
        for (i in n - 1 downTo 0) {
            var sum = b[i]
            for (j in i + 1 until n) {
                sum -= a[i][j] * x[j]
            }
            x[i] = sum / a[i][i]
            if (!x[i].isFinite()) return null
        }
        return x
    }
}

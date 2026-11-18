package com.example.engine.digital

import com.example.engine.netlist.NetlistBuilder
import com.example.engine.netlist.NetlistGraph
import com.example.model.CircuitPage
import com.example.model.ComponentInstance
import com.example.model.ComponentType
import com.example.model.DiagnosticSeverity
import com.example.model.DigitalSimulationResult
import com.example.model.DigitalWaveformSample
import com.example.model.LogicLevel
import com.example.model.SimulationDiagnostic

data class DigitalMemoryState(
    val flipFlopQ: Map<String, LogicLevel> = emptyMap(),
    val prevClock: Map<String, LogicLevel> = emptyMap(),
    val clockStates: Map<String, LogicLevel> = emptyMap(),
    val tick: Int = 0,
    val history: List<DigitalWaveformSample> = emptyList()
)

data class TruthTableRow(
    val inputs: List<LogicLevel>,
    val outputs: List<LogicLevel>
)

data class TruthTableResult(
    val inputLabels: List<String>,
    val outputLabels: List<String>,
    val rows: List<TruthTableRow>
)

class DigitalSimulationEngine {

    private fun isProbeIndicator(type: ComponentType): Boolean = when (type) {
        ComponentType.LOGIC_INDICATOR,
        ComponentType.LED_INDICATOR,
        ComponentType.PROBE_RED,
        ComponentType.PROBE_GREEN,
        ComponentType.PROBE_YELLOW,
        ComponentType.PROBE_BLUE,
        ComponentType.PROBE_ORANGE,
        ComponentType.PROBE_CYAN -> true
        else -> false
    }

    fun step(
        page: CircuitPage,
        memory: DigitalMemoryState = DigitalMemoryState(),
        toggleClocks: Boolean = true
    ): Pair<DigitalSimulationResult, DigitalMemoryState> {
        val graph = NetlistBuilder.build(page, includeClosedSwitches = true)
        val diagnostics = mutableListOf<SimulationDiagnostic>()

        val nextTick = memory.tick + 1
        val nextClocks = memory.clockStates.toMutableMap()
        if (toggleClocks) {
            for (comp in page.components) {
                if (comp.type == ComponentType.CLOCK_INPUT || comp.type == ComponentType.IC_555 ||
                    comp.type == ComponentType.FUNCTION_GENERATOR
                ) {
                    val prev = nextClocks[comp.id] ?: LogicLevel.ZERO
                    nextClocks[comp.id] = if (prev == LogicLevel.ONE) LogicLevel.ZERO else LogicLevel.ONE
                }
            }
        }

        val ffQ = memory.flipFlopQ.toMutableMap()
        val prevClk = memory.prevClock.toMutableMap()
        val pinDrivers = HashMap<String, LogicLevel>()
        val netLevels = HashMap<String, LogicLevel>()
        graph.nets.forEach { netLevels[it.id] = LogicLevel.UNKNOWN }

        // Initialize sequential IC outputs from stored state
        for (comp in page.components) {
            when (comp.type) {
                ComponentType.IC_7472 -> {
                    val q = ffQ.getOrPut(comp.id) { LogicLevel.ZERO }
                    pinDrivers[NetlistBuilder.pinKey(comp.id, "q")] = q
                    pinDrivers[NetlistBuilder.pinKey(comp.id, "qn")] = q.invert()
                }
                ComponentType.IC_7473 -> {
                    val q1 = ffQ.getOrPut("${comp.id}:1") { LogicLevel.ZERO }
                    val q2 = ffQ.getOrPut("${comp.id}:2") { LogicLevel.ZERO }
                    pinDrivers[NetlistBuilder.pinKey(comp.id, "1q")] = q1
                    pinDrivers[NetlistBuilder.pinKey(comp.id, "1qn")] = q1.invert()
                    pinDrivers[NetlistBuilder.pinKey(comp.id, "2q")] = q2
                    pinDrivers[NetlistBuilder.pinKey(comp.id, "2qn")] = q2.invert()
                }
                ComponentType.IC_7474 -> {
                    val q1 = ffQ.getOrPut(comp.id) { LogicLevel.ZERO }
                    val q2 = ffQ.getOrPut("${comp.id}:2") { LogicLevel.ZERO }
                    pinDrivers[NetlistBuilder.pinKey(comp.id, "q")] = q1
                    pinDrivers[NetlistBuilder.pinKey(comp.id, "qn")] = q1.invert()
                    pinDrivers[NetlistBuilder.pinKey(comp.id, "2q")] = q2
                    pinDrivers[NetlistBuilder.pinKey(comp.id, "2qn")] = q2.invert()
                }
                ComponentType.IC_7476 -> {
                    val q1 = ffQ.getOrPut(comp.id) { LogicLevel.ZERO }
                    val q2 = ffQ.getOrPut("${comp.id}:2") { LogicLevel.ZERO }
                    pinDrivers[NetlistBuilder.pinKey(comp.id, "q")] = q1
                    pinDrivers[NetlistBuilder.pinKey(comp.id, "qn")] = q1.invert()
                    pinDrivers[NetlistBuilder.pinKey(comp.id, "2q")] = q2
                }
                ComponentType.IC_7490, ComponentType.IC_7493, ComponentType.IC_74194 -> {
                    for (bit in listOf("qa", "qb", "qc", "qd")) {
                        val b = ffQ.getOrPut("${comp.id}:$bit") { LogicLevel.ZERO }
                        pinDrivers[NetlistBuilder.pinKey(comp.id, bit)] = b
                    }
                }
                else -> Unit
            }
        }

        // Propagate combinational logic up to 32 iterations until steady state
        var hasShort = false
        for (iter in 0 until 32) {
            val prevNetLevels = HashMap(netLevels)
            pinDrivers.clear()

            for (comp in page.components) {
                driveComponentOutputs(comp, netLevels, graph, nextClocks, ffQ, pinDrivers)
            }

            hasShort = false
            for (net in graph.nets) {
                val activeDrivers = mutableListOf<LogicLevel>()
                for (pk in net.pinKeys) {
                    val d = pinDrivers[pk]
                    if (d != null && d != LogicLevel.HIGH_Z) {
                        activeDrivers.add(d)
                    }
                }
                val resolved = when {
                    activeDrivers.isEmpty() -> {
                        val hasZ = net.pinKeys.any { pinDrivers[it] == LogicLevel.HIGH_Z }
                        if (hasZ) LogicLevel.HIGH_Z else LogicLevel.UNKNOWN
                    }
                    activeDrivers.contains(LogicLevel.CONFLICT) -> {
                        hasShort = true
                        LogicLevel.CONFLICT
                    }
                    activeDrivers.contains(LogicLevel.ONE) && activeDrivers.contains(LogicLevel.ZERO) -> {
                        hasShort = true
                        LogicLevel.CONFLICT
                    }
                    activeDrivers.all { it == LogicLevel.ONE } -> LogicLevel.ONE
                    activeDrivers.all { it == LogicLevel.ZERO } -> LogicLevel.ZERO
                    activeDrivers.contains(LogicLevel.ONE) -> LogicLevel.ONE
                    activeDrivers.contains(LogicLevel.ZERO) -> LogicLevel.ZERO
                    else -> LogicLevel.UNKNOWN
                }
                netLevels[net.id] = resolved
            }

            if (iter > 0 && netLevels == prevNetLevels) break
        }

        // Update sequential flip-flops & counters on clock edges after combinational nets stabilize
        for (comp in page.components) {
            when (comp.type) {
                ComponentType.IC_7472 -> {
                    // Unconnected J/K inputs float HIGH in 74xx TTL logic
                    fun ttlPullUp(pinId: String): LogicLevel {
                        val v = readPinNet(comp.id, pinId, graph, netLevels)
                        return if (v == LogicLevel.UNKNOWN || v == LogicLevel.HIGH_Z) LogicLevel.ONE else v
                    }
                    val jEff = evalAndList(listOf(ttlPullUp("j1"), ttlPullUp("j2"), ttlPullUp("j3")))
                    val kEff = evalAndList(listOf(ttlPullUp("k1"), ttlPullUp("k2"), ttlPullUp("k3")))
                    val clk = readPinNet(comp.id, "clk", graph, netLevels)
                    val pre = readPinNet(comp.id, "pre", graph, netLevels)
                    val clr = readPinNet(comp.id, "clr", graph, netLevels)
                    val oldClk = prevClk[comp.id] ?: LogicLevel.ZERO
                    var q = ffQ[comp.id] ?: LogicLevel.ZERO
                    q = stepJkFlipFlop(jEff, kEff, clk, oldClk, pre, clr, q)
                    ffQ[comp.id] = q
                    prevClk[comp.id] = clk
                }
                ComponentType.IC_7473 -> {
                    // FF1
                    val clk1 = readPinNet(comp.id, "1clk", graph, netLevels)
                    val q1 = stepJkFlipFlop(
                        j = readPinNet(comp.id, "1j", graph, netLevels),
                        k = readPinNet(comp.id, "1k", graph, netLevels),
                        clk = clk1,
                        oldClk = prevClk["${comp.id}:1"] ?: LogicLevel.ZERO,
                        pre = LogicLevel.ONE,
                        clr = readPinNet(comp.id, "1clr", graph, netLevels),
                        currentQ = ffQ["${comp.id}:1"] ?: LogicLevel.ZERO
                    )
                    ffQ["${comp.id}:1"] = q1
                    prevClk["${comp.id}:1"] = clk1

                    // FF2
                    val clk2 = readPinNet(comp.id, "2clk", graph, netLevels)
                    val q2 = stepJkFlipFlop(
                        j = readPinNet(comp.id, "2j", graph, netLevels),
                        k = readPinNet(comp.id, "2k", graph, netLevels),
                        clk = clk2,
                        oldClk = prevClk["${comp.id}:2"] ?: LogicLevel.ZERO,
                        pre = LogicLevel.ONE,
                        clr = readPinNet(comp.id, "2clr", graph, netLevels),
                        currentQ = ffQ["${comp.id}:2"] ?: LogicLevel.ZERO
                    )
                    ffQ["${comp.id}:2"] = q2
                    prevClk["${comp.id}:2"] = clk2
                }
                ComponentType.IC_7474 -> {
                    // FF1 (backward-compatible pin IDs)
                    val clk1 = readPinNet(comp.id, "clk", graph, netLevels)
                    val q1 = stepDFlipFlop(
                        d = readPinNet(comp.id, "d", graph, netLevels),
                        clk = clk1,
                        oldClk = prevClk[comp.id] ?: LogicLevel.ZERO,
                        pre = readPinNet(comp.id, "pre", graph, netLevels),
                        clr = readPinNet(comp.id, "clr", graph, netLevels),
                        currentQ = ffQ[comp.id] ?: LogicLevel.ZERO
                    )
                    ffQ[comp.id] = q1
                    prevClk[comp.id] = clk1

                    // FF2
                    val clk2 = readPinNet(comp.id, "2clk", graph, netLevels)
                    val q2 = stepDFlipFlop(
                        d = readPinNet(comp.id, "2d", graph, netLevels),
                        clk = clk2,
                        oldClk = prevClk["${comp.id}:2"] ?: LogicLevel.ZERO,
                        pre = readPinNet(comp.id, "2pre", graph, netLevels),
                        clr = readPinNet(comp.id, "2clr", graph, netLevels),
                        currentQ = ffQ["${comp.id}:2"] ?: LogicLevel.ZERO
                    )
                    ffQ["${comp.id}:2"] = q2
                    prevClk["${comp.id}:2"] = clk2
                }
                ComponentType.IC_7476 -> {
                    // FF1 (backward-compatible pin IDs)
                    val clk1 = readPinNet(comp.id, "clk", graph, netLevels)
                    val q1 = stepJkFlipFlop(
                        j = readPinNet(comp.id, "j", graph, netLevels),
                        k = readPinNet(comp.id, "k", graph, netLevels),
                        clk = clk1,
                        oldClk = prevClk[comp.id] ?: LogicLevel.ZERO,
                        pre = readPinNet(comp.id, "pre", graph, netLevels),
                        clr = readPinNet(comp.id, "clr", graph, netLevels),
                        currentQ = ffQ[comp.id] ?: LogicLevel.ZERO
                    )
                    ffQ[comp.id] = q1
                    prevClk[comp.id] = clk1

                    // FF2
                    val clk2 = readPinNet(comp.id, "2clk", graph, netLevels)
                    val q2 = stepJkFlipFlop(
                        j = readPinNet(comp.id, "2j", graph, netLevels),
                        k = readPinNet(comp.id, "2k", graph, netLevels),
                        clk = clk2,
                        oldClk = prevClk["${comp.id}:2"] ?: LogicLevel.ZERO,
                        pre = readPinNet(comp.id, "2pre", graph, netLevels),
                        clr = LogicLevel.ONE,
                        currentQ = ffQ["${comp.id}:2"] ?: LogicLevel.ZERO
                    )
                    ffQ["${comp.id}:2"] = q2
                    prevClk["${comp.id}:2"] = clk2
                }
                ComponentType.IC_7490, ComponentType.IC_7493 -> {
                    val isDecade = comp.type == ComponentType.IC_7490
                    val r01 = readPinNet(comp.id, "r01", graph, netLevels)
                    val r02 = readPinNet(comp.id, "r02", graph, netLevels)
                    val r91 = if (isDecade) readPinNet(comp.id, "r91", graph, netLevels) else LogicLevel.ZERO
                    val r92 = if (isDecade) readPinNet(comp.id, "r92", graph, netLevels) else LogicLevel.ZERO
                    val clka = readPinNet(comp.id, "clka", graph, netLevels)
                    val clkb = readPinNet(comp.id, "clkb", graph, netLevels)
                    val oldClka = prevClk["${comp.id}:a"] ?: LogicLevel.ZERO
                    val oldClkb = prevClk["${comp.id}:b"] ?: LogicLevel.ZERO

                    var qa = if (ffQ["${comp.id}:qa"] == LogicLevel.ONE) 1 else 0
                    var qb = if (ffQ["${comp.id}:qb"] == LogicLevel.ONE) 1 else 0
                    var qc = if (ffQ["${comp.id}:qc"] == LogicLevel.ONE) 1 else 0
                    var qd = if (ffQ["${comp.id}:qd"] == LogicLevel.ONE) 1 else 0

                    if (isDecade && r91 == LogicLevel.ONE && r92 == LogicLevel.ONE) {
                        // Set BCD 9 (1001)
                        qa = 1; qb = 0; qc = 0; qd = 1
                    } else if (r01 == LogicLevel.ONE && r02 == LogicLevel.ONE) {
                        // Reset to 0000
                        qa = 0; qb = 0; qc = 0; qd = 0
                    } else {
                        val qaPrev = qa
                        val clkaFell = oldClka == LogicLevel.ONE && clka == LogicLevel.ZERO
                        if (clkaFell) {
                            qa = 1 - qa
                        }
                        val qaNet = graph.pinToNetId[NetlistBuilder.pinKey(comp.id, "qa")]
                        val clkbNet = graph.pinToNetId[NetlistBuilder.pinKey(comp.id, "clkb")]
                        val clkbTriggered = if (qaNet != null && qaNet == clkbNet) {
                            // Instant ripple from QA -> CLKB on falling edge of QA
                            qaPrev == 1 && qa == 0
                        } else {
                            oldClkb == LogicLevel.ONE && clkb == LogicLevel.ZERO
                        }
                        if (clkbTriggered) {
                            val upper = (qd shl 2) or (qc shl 1) or qb
                            val mod = if (isDecade) 5 else 8
                            val nextUpper = (upper + 1) % mod
                            qb = nextUpper and 1
                            qc = (nextUpper shr 1) and 1
                            qd = (nextUpper shr 2) and 1
                        }
                    }

                    ffQ["${comp.id}:qa"] = if (qa == 1) LogicLevel.ONE else LogicLevel.ZERO
                    ffQ["${comp.id}:qb"] = if (qb == 1) LogicLevel.ONE else LogicLevel.ZERO
                    ffQ["${comp.id}:qc"] = if (qc == 1) LogicLevel.ONE else LogicLevel.ZERO
                    ffQ["${comp.id}:qd"] = if (qd == 1) LogicLevel.ONE else LogicLevel.ZERO
                    prevClk["${comp.id}:a"] = clka
                    prevClk["${comp.id}:b"] = clkb
                }
                ComponentType.IC_74194 -> {
                    val clr = readPinNet(comp.id, "clr", graph, netLevels)
                    val clk = readPinNet(comp.id, "clk", graph, netLevels)
                    val oldClk = prevClk[comp.id] ?: LogicLevel.ZERO
                    var qa = ffQ["${comp.id}:qa"] ?: LogicLevel.ZERO
                    var qb = ffQ["${comp.id}:qb"] ?: LogicLevel.ZERO
                    var qc = ffQ["${comp.id}:qc"] ?: LogicLevel.ZERO
                    var qd = ffQ["${comp.id}:qd"] ?: LogicLevel.ZERO

                    if (clr == LogicLevel.ZERO) {
                        qa = LogicLevel.ZERO
                        qb = LogicLevel.ZERO
                        qc = LogicLevel.ZERO
                        qd = LogicLevel.ZERO
                    } else if (oldClk == LogicLevel.ZERO && clk == LogicLevel.ONE) {
                        val s1 = readPinNet(comp.id, "s1", graph, netLevels) == LogicLevel.ONE
                        val s0 = readPinNet(comp.id, "s0", graph, netLevels) == LogicLevel.ONE
                        when {
                            !s1 && !s0 -> Unit // Hold
                            !s1 && s0 -> {
                                // Shift Right: SR -> QA -> QB -> QC -> QD
                                val sr = readPinNet(comp.id, "sr", graph, netLevels)
                                qd = qc
                                qc = qb
                                qb = qa
                                qa = if (sr == LogicLevel.ONE) LogicLevel.ONE else LogicLevel.ZERO
                            }
                            s1 && !s0 -> {
                                // Shift Left: SL -> QD -> QC -> QB -> QA
                                val sl = readPinNet(comp.id, "sl", graph, netLevels)
                                qa = qb
                                qb = qc
                                qc = qd
                                qd = if (sl == LogicLevel.ONE) LogicLevel.ONE else LogicLevel.ZERO
                            }
                            else -> {
                                // Parallel Load: A..D -> QA..QD
                                qa = if (readPinNet(comp.id, "a", graph, netLevels) == LogicLevel.ONE) LogicLevel.ONE else LogicLevel.ZERO
                                qb = if (readPinNet(comp.id, "b", graph, netLevels) == LogicLevel.ONE) LogicLevel.ONE else LogicLevel.ZERO
                                qc = if (readPinNet(comp.id, "c", graph, netLevels) == LogicLevel.ONE) LogicLevel.ONE else LogicLevel.ZERO
                                qd = if (readPinNet(comp.id, "d", graph, netLevels) == LogicLevel.ONE) LogicLevel.ONE else LogicLevel.ZERO
                            }
                        }
                    }

                    ffQ["${comp.id}:qa"] = qa
                    ffQ["${comp.id}:qb"] = qb
                    ffQ["${comp.id}:qc"] = qc
                    ffQ["${comp.id}:qd"] = qd
                    prevClk[comp.id] = clk
                }
                ComponentType.IC_74595 -> {
                    val srclr = readPinNet(comp.id, "srclr", graph, netLevels)
                    val srclk = readPinNet(comp.id, "srclk", graph, netLevels)
                    val rclk = readPinNet(comp.id, "rclk", graph, netLevels)
                    val oldSrclk = prevClk["${comp.id}:sr"] ?: LogicLevel.ZERO
                    val oldRclk = prevClk["${comp.id}:r"] ?: LogicLevel.ZERO

                    val shiftBits = MutableList(8) { i -> ffQ["${comp.id}:s$i"] ?: LogicLevel.ZERO }
                    val latchBits = MutableList(8) { i -> ffQ["${comp.id}:q$i"] ?: LogicLevel.ZERO }

                    if (srclr == LogicLevel.ZERO) {
                        for (i in 0..7) shiftBits[i] = LogicLevel.ZERO
                    } else if (oldSrclk == LogicLevel.ZERO && srclk == LogicLevel.ONE) {
                        val ser = if (readPinNet(comp.id, "ser", graph, netLevels) == LogicLevel.ONE) LogicLevel.ONE else LogicLevel.ZERO
                        for (i in 7 downTo 1) shiftBits[i] = shiftBits[i - 1]
                        shiftBits[0] = ser
                    }
                    if (oldRclk == LogicLevel.ZERO && rclk == LogicLevel.ONE) {
                        for (i in 0..7) latchBits[i] = shiftBits[i]
                    }

                    for (i in 0..7) {
                        ffQ["${comp.id}:s$i"] = shiftBits[i]
                        ffQ["${comp.id}:q$i"] = latchBits[i]
                    }
                    prevClk["${comp.id}:sr"] = srclk
                    prevClk["${comp.id}:r"] = rclk
                }
                ComponentType.IC_74161 -> {
                    val clr = readPinNet(comp.id, "clr", graph, netLevels)
                    val clk = readPinNet(comp.id, "clk", graph, netLevels)
                    val oldClk = prevClk[comp.id] ?: LogicLevel.ZERO
                    var count = (if (ffQ["${comp.id}:qa"] == LogicLevel.ONE) 1 else 0) or
                        (if (ffQ["${comp.id}:qb"] == LogicLevel.ONE) 2 else 0) or
                        (if (ffQ["${comp.id}:qc"] == LogicLevel.ONE) 4 else 0) or
                        (if (ffQ["${comp.id}:qd"] == LogicLevel.ONE) 8 else 0)

                    if (clr == LogicLevel.ZERO) {
                        count = 0
                    } else if (oldClk == LogicLevel.ZERO && clk == LogicLevel.ONE) {
                        val load = readPinNet(comp.id, "load", graph, netLevels)
                        if (load == LogicLevel.ZERO) {
                            count = (if (readPinNet(comp.id, "a", graph, netLevels) == LogicLevel.ONE) 1 else 0) or
                                (if (readPinNet(comp.id, "b", graph, netLevels) == LogicLevel.ONE) 2 else 0) or
                                (if (readPinNet(comp.id, "c", graph, netLevels) == LogicLevel.ONE) 4 else 0) or
                                (if (readPinNet(comp.id, "d", graph, netLevels) == LogicLevel.ONE) 8 else 0)
                        } else {
                            val enp = readPinNet(comp.id, "enp", graph, netLevels)
                            val ent = readPinNet(comp.id, "ent", graph, netLevels)
                            if (enp != LogicLevel.ZERO && ent != LogicLevel.ZERO) {
                                count = (count + 1) and 0x0F
                            }
                        }
                    }

                    ffQ["${comp.id}:qa"] = if ((count and 1) != 0) LogicLevel.ONE else LogicLevel.ZERO
                    ffQ["${comp.id}:qb"] = if ((count and 2) != 0) LogicLevel.ONE else LogicLevel.ZERO
                    ffQ["${comp.id}:qc"] = if ((count and 4) != 0) LogicLevel.ONE else LogicLevel.ZERO
                    ffQ["${comp.id}:qd"] = if ((count and 8) != 0) LogicLevel.ONE else LogicLevel.ZERO
                    prevClk[comp.id] = clk
                }
                else -> Unit
            }
        }

        // Re-propagate once after sequential updates so outputs reflect the new edge immediately
        pinDrivers.clear()
        for (comp in page.components) {
            driveComponentOutputs(comp, netLevels, graph, nextClocks, ffQ, pinDrivers)
        }
        for (net in graph.nets) {
            val activeDrivers = net.pinKeys.mapNotNull { pinDrivers[it] }.filter { it != LogicLevel.HIGH_Z }
            netLevels[net.id] = when {
                activeDrivers.isEmpty() -> {
                    if (net.pinKeys.any { pinDrivers[it] == LogicLevel.HIGH_Z }) LogicLevel.HIGH_Z else LogicLevel.UNKNOWN
                }
                activeDrivers.contains(LogicLevel.CONFLICT) ||
                    (activeDrivers.contains(LogicLevel.ONE) && activeDrivers.contains(LogicLevel.ZERO)) -> {
                    hasShort = true
                    LogicLevel.CONFLICT
                }
                activeDrivers.contains(LogicLevel.ONE) -> LogicLevel.ONE
                activeDrivers.contains(LogicLevel.ZERO) -> LogicLevel.ZERO
                else -> LogicLevel.UNKNOWN
            }
        }

        if (hasShort) {
            diagnostics.add(
                SimulationDiagnostic(
                    severity = DiagnosticSeverity.ERROR,
                    code = "DIGITAL_SHORT",
                    message = "Bus contention or VCC-GND short detected on one or more nets."
                )
            )
        }

        val pinLevels = HashMap<String, LogicLevel>()
        for ((pk, netId) in graph.pinToNetId) {
            pinLevels[pk] = netLevels[netId] ?: LogicLevel.UNKNOWN
        }
        val wireLevels = HashMap<String, LogicLevel>()
        for ((wid, netId) in graph.wireToNetId) {
            wireLevels[wid] = netLevels[netId] ?: LogicLevel.UNKNOWN
        }

        // Record component output states (for displays / indicators / clocks)
        val componentStates = HashMap<String, Map<String, String>>()
        for (comp in page.components) {
            when (comp.type) {
                ComponentType.CLOCK_INPUT, ComponentType.IC_555 -> {
                    val lvl = nextClocks[comp.id] ?: LogicLevel.ZERO
                    componentStates[comp.id] = mapOf("state" to lvl.symbol)
                }
                ComponentType.HEX_DISPLAY, ComponentType.DIGITAL_DISPLAY,
                ComponentType.BAR_GRAPH, ComponentType.DECODED_BAR_GRAPH -> {
                    val b3 = if (pinLevels[NetlistBuilder.pinKey(comp.id, "b3")] == LogicLevel.ONE) 8 else 0
                    val b2 = if (pinLevels[NetlistBuilder.pinKey(comp.id, "b2")] == LogicLevel.ONE) 4 else 0
                    val b1 = if (pinLevels[NetlistBuilder.pinKey(comp.id, "b1")] == LogicLevel.ONE) 2 else 0
                    val b0 = if (pinLevels[NetlistBuilder.pinKey(comp.id, "b0")] == LogicLevel.ONE) 1 else 0
                    val value = b3 + b2 + b1 + b0
                    componentStates[comp.id] = mapOf("value" to value.toString(), "hex" to value.toString(16).uppercase())
                }
                ComponentType.PILOT_LAMP -> {
                    val p1 = pinLevels[NetlistBuilder.pinKey(comp.id, "p1")] ?: LogicLevel.UNKNOWN
                    val p2 = pinLevels[NetlistBuilder.pinKey(comp.id, "p2")] ?: LogicLevel.UNKNOWN
                    val lit = (p1 == LogicLevel.ONE && p2 != LogicLevel.ONE) || (p2 == LogicLevel.ONE && p1 != LogicLevel.ONE)
                    componentStates[comp.id] = mapOf("state" to if (lit) "1" else "0")
                }
                ComponentType.BUZZER -> {
                    val pos = pinLevels[NetlistBuilder.pinKey(comp.id, "pos")] ?: LogicLevel.UNKNOWN
                    val neg = pinLevels[NetlistBuilder.pinKey(comp.id, "neg")] ?: LogicLevel.UNKNOWN
                    val active = pos == LogicLevel.ONE && neg != LogicLevel.ONE
                    componentStates[comp.id] = mapOf("state" to if (active) "1" else "0")
                }
                ComponentType.TRAFFIC_LIGHT -> {
                    val r = if (pinLevels[NetlistBuilder.pinKey(comp.id, "r")] == LogicLevel.ONE) "1" else "0"
                    val y = if (pinLevels[NetlistBuilder.pinKey(comp.id, "y")] == LogicLevel.ONE) "1" else "0"
                    val g = if (pinLevels[NetlistBuilder.pinKey(comp.id, "g")] == LogicLevel.ONE) "1" else "0"
                    componentStates[comp.id] = mapOf("r" to r, "y" to y, "g" to g, "state" to if (r == "1" || y == "1" || g == "1") "1" else "0")
                }
                ComponentType.LCD_16X2 -> {
                    val en = pinLevels[NetlistBuilder.pinKey(comp.id, "en")] != LogicLevel.ZERO
                    val v = (if (pinLevels[NetlistBuilder.pinKey(comp.id, "d0")] == LogicLevel.ONE) 1 else 0) or
                        (if (pinLevels[NetlistBuilder.pinKey(comp.id, "d1")] == LogicLevel.ONE) 2 else 0) or
                        (if (pinLevels[NetlistBuilder.pinKey(comp.id, "d2")] == LogicLevel.ONE) 4 else 0) or
                        (if (pinLevels[NetlistBuilder.pinKey(comp.id, "d3")] == LogicLevel.ONE) 8 else 0)
                    val line1 = comp.properties["line1"]?.ifBlank { null } ?: comp.value.ifBlank { "GATEX LABS 16x2" }
                    val line2 = if (en) "BUS: 0x${v.toString(16).uppercase()} (${v.toString(2).padStart(4, '0')})" else "DISPLAY OFF"
                    componentStates[comp.id] = mapOf("line1" to line1.take(16), "line2" to line2, "state" to if (en) "1" else "0")
                }
                ComponentType.DOT_MATRIX_8X8 -> {
                    val rMask = (0..3).fold(0) { acc, i ->
                        acc or (if (pinLevels[NetlistBuilder.pinKey(comp.id, "r$i")] == LogicLevel.ONE) (1 shl i) else 0)
                    }
                    val cMask = (0..3).fold(0) { acc, i ->
                        acc or (if (pinLevels[NetlistBuilder.pinKey(comp.id, "c$i")] == LogicLevel.ONE) (1 shl i) else 0)
                    }
                    componentStates[comp.id] = mapOf(
                        "rMask" to rMask.toString(),
                        "cMask" to cMask.toString(),
                        "state" to if (rMask != 0 && cMask != 0) "1" else "0"
                    )
                }
                ComponentType.LOGIC_ANALYZER -> {
                    val c0 = pinLevels[NetlistBuilder.pinKey(comp.id, "ch0")]?.symbol ?: "X"
                    val c1 = pinLevels[NetlistBuilder.pinKey(comp.id, "ch1")]?.symbol ?: "X"
                    val c2 = pinLevels[NetlistBuilder.pinKey(comp.id, "ch2")]?.symbol ?: "X"
                    val c3 = pinLevels[NetlistBuilder.pinKey(comp.id, "ch3")]?.symbol ?: "X"
                    componentStates[comp.id] = mapOf("channels" to "$c0,$c1,$c2,$c3")
                }
                else -> {
                    if (isProbeIndicator(comp.type) || comp.type == ComponentType.RELAY_OUTPUT) {
                        val lvl = pinLevels[NetlistBuilder.pinKey(comp.id, "in")] ?: LogicLevel.UNKNOWN
                        componentStates[comp.id] = mapOf("state" to lvl.symbol)
                    }
                }
            }
        }

        // Capture waveform sample for Logic Analyzer / Timing Diagrams
        val trackedNodes = LinkedHashMap<String, LogicLevel>()
        for (comp in page.components) {
            when (comp.type) {
                ComponentType.CLOCK_INPUT, ComponentType.LOGIC_INPUT -> {
                    trackedNodes[comp.label] = pinLevels[NetlistBuilder.pinKey(comp.id, "out")] ?: LogicLevel.UNKNOWN
                }
                ComponentType.IC_7472, ComponentType.IC_7474, ComponentType.IC_7476 -> {
                    trackedNodes["${comp.label}.Q"] = pinLevels[NetlistBuilder.pinKey(comp.id, "q")] ?: LogicLevel.UNKNOWN
                }
                ComponentType.IC_7473 -> {
                    trackedNodes["${comp.label}.1Q"] = pinLevels[NetlistBuilder.pinKey(comp.id, "1q")] ?: LogicLevel.UNKNOWN
                }
                ComponentType.IC_7490, ComponentType.IC_7493, ComponentType.IC_74194 -> {
                    trackedNodes["${comp.label}.QA"] = pinLevels[NetlistBuilder.pinKey(comp.id, "qa")] ?: LogicLevel.UNKNOWN
                    trackedNodes["${comp.label}.QB"] = pinLevels[NetlistBuilder.pinKey(comp.id, "qb")] ?: LogicLevel.UNKNOWN
                    trackedNodes["${comp.label}.QC"] = pinLevels[NetlistBuilder.pinKey(comp.id, "qc")] ?: LogicLevel.UNKNOWN
                    trackedNodes["${comp.label}.QD"] = pinLevels[NetlistBuilder.pinKey(comp.id, "qd")] ?: LogicLevel.UNKNOWN
                }
                ComponentType.IC_7483 -> {
                    trackedNodes["${comp.label}.S1"] = pinLevels[NetlistBuilder.pinKey(comp.id, "s1")] ?: LogicLevel.UNKNOWN
                    trackedNodes["${comp.label}.S2"] = pinLevels[NetlistBuilder.pinKey(comp.id, "s2")] ?: LogicLevel.UNKNOWN
                    trackedNodes["${comp.label}.S3"] = pinLevels[NetlistBuilder.pinKey(comp.id, "s3")] ?: LogicLevel.UNKNOWN
                    trackedNodes["${comp.label}.S4"] = pinLevels[NetlistBuilder.pinKey(comp.id, "s4")] ?: LogicLevel.UNKNOWN
                    trackedNodes["${comp.label}.C4"] = pinLevels[NetlistBuilder.pinKey(comp.id, "c4")] ?: LogicLevel.UNKNOWN
                }
                ComponentType.LOGIC_ANALYZER -> {
                    trackedNodes["LA.D0"] = pinLevels[NetlistBuilder.pinKey(comp.id, "ch0")] ?: LogicLevel.UNKNOWN
                    trackedNodes["LA.D1"] = pinLevels[NetlistBuilder.pinKey(comp.id, "ch1")] ?: LogicLevel.UNKNOWN
                    trackedNodes["LA.D2"] = pinLevels[NetlistBuilder.pinKey(comp.id, "ch2")] ?: LogicLevel.UNKNOWN
                    trackedNodes["LA.D3"] = pinLevels[NetlistBuilder.pinKey(comp.id, "ch3")] ?: LogicLevel.UNKNOWN
                }
                else -> {
                    if (isProbeIndicator(comp.type)) {
                        trackedNodes[comp.label] = pinLevels[NetlistBuilder.pinKey(comp.id, "in")] ?: LogicLevel.UNKNOWN
                    }
                }
            }
        }

        val updatedHistory = (memory.history + DigitalWaveformSample(nextTick, trackedNodes)).takeLast(64)
        val nextMemory = DigitalMemoryState(
            flipFlopQ = ffQ,
            prevClock = prevClk,
            clockStates = nextClocks,
            tick = nextTick,
            history = updatedHistory
        )

        return DigitalSimulationResult(
            pinLevels = pinLevels,
            wireLevels = wireLevels,
            componentStates = componentStates,
            diagnostics = diagnostics,
            waveformHistory = updatedHistory,
            tickCount = nextTick,
            hasShortCircuit = hasShort
        ) to nextMemory
    }

    private fun stepDFlipFlop(
        d: LogicLevel,
        clk: LogicLevel,
        oldClk: LogicLevel,
        pre: LogicLevel,
        clr: LogicLevel,
        currentQ: LogicLevel
    ): LogicLevel = when {
        pre == LogicLevel.ZERO && clr == LogicLevel.ZERO -> LogicLevel.CONFLICT
        pre == LogicLevel.ZERO -> LogicLevel.ONE
        clr == LogicLevel.ZERO -> LogicLevel.ZERO
        oldClk == LogicLevel.ZERO && clk == LogicLevel.ONE -> if (d.isKnown()) d else currentQ
        else -> currentQ
    }

    private fun stepJkFlipFlop(
        j: LogicLevel,
        k: LogicLevel,
        clk: LogicLevel,
        oldClk: LogicLevel,
        pre: LogicLevel,
        clr: LogicLevel,
        currentQ: LogicLevel
    ): LogicLevel = when {
        pre == LogicLevel.ZERO && clr == LogicLevel.ZERO -> LogicLevel.CONFLICT
        pre == LogicLevel.ZERO -> LogicLevel.ONE
        clr == LogicLevel.ZERO -> LogicLevel.ZERO
        oldClk == LogicLevel.ZERO && clk == LogicLevel.ONE -> {
            val jVal = if (j == LogicLevel.ONE) 1 else 0
            val kVal = if (k == LogicLevel.ONE) 1 else 0
            when {
                jVal == 0 && kVal == 0 -> currentQ
                jVal == 0 && kVal == 1 -> LogicLevel.ZERO
                jVal == 1 && kVal == 0 -> LogicLevel.ONE
                else -> currentQ.invert()
            }
        }
        else -> currentQ
    }

    fun evaluateCombinational(page: CircuitPage): DigitalSimulationResult {
        var memory = DigitalMemoryState()
        var result = DigitalSimulationResult()
        repeat(3) {
            val (res, nextMem) = step(page, memory, toggleClocks = false)
            result = res
            memory = nextMem
        }
        return result
    }

    fun generateTruthTable(page: CircuitPage): TruthTableResult {
        val inputs = page.components.filter { it.type == ComponentType.LOGIC_INPUT }.sortedBy { it.label }
        val outputs = page.components.filter { isProbeIndicator(it.type) }.sortedBy { it.label }

        if (inputs.isEmpty() || outputs.isEmpty()) {
            return TruthTableResult(emptyList(), emptyList(), emptyList())
        }

        val limitedInputs = inputs.take(5)
        val totalRows = 1 shl limitedInputs.size
        val rows = ArrayList<TruthTableRow>(totalRows)

        for (mask in 0 until totalRows) {
            val inputValues = limitedInputs.indices.map { bit ->
                val isOne = ((mask shr (limitedInputs.size - 1 - bit)) and 1) == 1
                if (isOne) LogicLevel.ONE else LogicLevel.ZERO
            }
            val updatedComponents = page.components.map { comp ->
                val idx = limitedInputs.indexOfFirst { it.id == comp.id }
                if (idx >= 0) {
                    val bitStr = if (inputValues[idx] == LogicLevel.ONE) "1" else "0"
                    comp.copy(value = bitStr, properties = comp.properties + ("state" to bitStr))
                } else comp
            }
            val sim = evaluateCombinational(page.copy(components = updatedComponents))
            val outValues = outputs.map { outComp ->
                sim.pinLevels[NetlistBuilder.pinKey(outComp.id, "in")] ?: LogicLevel.UNKNOWN
            }
            rows.add(TruthTableRow(inputValues, outValues))
        }

        return TruthTableResult(
            inputLabels = limitedInputs.map { it.label },
            outputLabels = outputs.map { it.label },
            rows = rows
        )
    }

    private fun readPinNet(
        compId: String,
        pinId: String,
        graph: NetlistGraph,
        netLevels: Map<String, LogicLevel>
    ): LogicLevel {
        val netId = graph.pinToNetId[NetlistBuilder.pinKey(compId, pinId)] ?: return LogicLevel.UNKNOWN
        return netLevels[netId] ?: LogicLevel.UNKNOWN
    }

    private fun driveComponentOutputs(
        comp: ComponentInstance,
        netLevels: Map<String, LogicLevel>,
        graph: NetlistGraph,
        clockStates: Map<String, LogicLevel>,
        ffQ: Map<String, LogicLevel>,
        drivers: MutableMap<String, LogicLevel>
    ) {
        fun drive(pinId: String, level: LogicLevel) {
            drivers[NetlistBuilder.pinKey(comp.id, pinId)] = level
        }
        fun inPin(pinId: String): LogicLevel = readPinNet(comp.id, pinId, graph, netLevels)

        when (comp.type) {
            ComponentType.VCC -> drive("vcc", LogicLevel.ONE)
            ComponentType.GROUND -> drive("gnd", LogicLevel.ZERO)
            ComponentType.DC_SOURCE, ComponentType.BATTERY, ComponentType.POWER_SUPPLY, ComponentType.VARIABLE_INPUT -> {
                drive("pos", LogicLevel.ONE)
                drive("neg", LogicLevel.ZERO)
                if (comp.type == ComponentType.VARIABLE_INPUT) {
                    drive("out", LogicLevel.ONE)
                    drive("ref", LogicLevel.ZERO)
                }
            }
            ComponentType.LOGIC_INPUT -> {
                val state = if (comp.properties["state"] == "1" || comp.value == "1") LogicLevel.ONE else LogicLevel.ZERO
                drive("out", state)
            }
            ComponentType.CLOCK_INPUT -> {
                drive("out", clockStates[comp.id] ?: LogicLevel.ZERO)
            }
            ComponentType.FUNCTION_GENERATOR -> {
                if (comp.properties["waveform"] == "SQUARE") {
                    drive("out", clockStates[comp.id] ?: LogicLevel.ZERO)
                }
            }
            ComponentType.DIP_SWITCH -> {
                val bits = (comp.properties["bits"] ?: "0000").padEnd(4, '0')
                for (i in 0..3) {
                    drive("d$i", if (bits[i] == '1') LogicLevel.ONE else LogicLevel.ZERO)
                }
            }
            ComponentType.KEYPAD -> {
                val digit = (comp.properties["hexDigit"] ?: "0").toIntOrNull(16) ?: 0
                drive("b0", if ((digit and 1) != 0) LogicLevel.ONE else LogicLevel.ZERO)
                drive("b1", if ((digit and 2) != 0) LogicLevel.ONE else LogicLevel.ZERO)
                drive("b2", if ((digit and 4) != 0) LogicLevel.ONE else LogicLevel.ZERO)
                drive("b3", if ((digit and 8) != 0) LogicLevel.ONE else LogicLevel.ZERO)
            }
            ComponentType.GATE_AND -> {
                val ins = com.example.model.ComponentCatalog.metadataFor(comp).pins
                    .filter { it.direction == com.example.model.PinDirection.INPUT }
                    .map { inPin(it.id) }
                drive("y", evalAndList(ins))
            }
            ComponentType.GATE_OR -> {
                val ins = com.example.model.ComponentCatalog.metadataFor(comp).pins
                    .filter { it.direction == com.example.model.PinDirection.INPUT }
                    .map { inPin(it.id) }
                drive("y", evalOrList(ins))
            }
            ComponentType.GATE_NOT -> drive("y", evalNot(inPin("a")))
            ComponentType.GATE_NAND -> {
                val ins = com.example.model.ComponentCatalog.metadataFor(comp).pins
                    .filter { it.direction == com.example.model.PinDirection.INPUT }
                    .map { inPin(it.id) }
                drive("y", evalAndList(ins).invert())
            }
            ComponentType.GATE_NOR -> {
                val ins = com.example.model.ComponentCatalog.metadataFor(comp).pins
                    .filter { it.direction == com.example.model.PinDirection.INPUT }
                    .map { inPin(it.id) }
                drive("y", evalOrList(ins).invert())
            }
            ComponentType.GATE_XOR -> {
                val ins = com.example.model.ComponentCatalog.metadataFor(comp).pins
                    .filter { it.direction == com.example.model.PinDirection.INPUT }
                    .map { inPin(it.id) }
                drive("y", evalXorList(ins))
            }
            ComponentType.GATE_XNOR -> {
                val ins = com.example.model.ComponentCatalog.metadataFor(comp).pins
                    .filter { it.direction == com.example.model.PinDirection.INPUT }
                    .map { inPin(it.id) }
                drive("y", evalXorList(ins).invert())
            }
            ComponentType.GATE_BUFFER -> drive("y", inPin("a"))
            ComponentType.GATE_TRISTATE -> {
                val en = inPin("en")
                val out = when (en) {
                    LogicLevel.ONE -> inPin("a")
                    LogicLevel.ZERO -> LogicLevel.HIGH_Z
                    else -> LogicLevel.UNKNOWN
                }
                drive("y", out)
            }

            // 14-Pin 74xx Combinational ICs (All gates active!)
            ComponentType.IC_7400 -> {
                for (g in 1..4) drive("${g}y", evalAnd(inPin("${g}a"), inPin("${g}b")).invert())
            }
            ComponentType.IC_7402 -> {
                for (g in 1..4) drive("${g}y", evalOr(inPin("${g}a"), inPin("${g}b")).invert())
            }
            ComponentType.IC_7404 -> {
                for (g in 1..6) drive("${g}y", evalNot(inPin("${g}a")))
            }
            ComponentType.IC_7408 -> {
                for (g in 1..4) drive("${g}y", evalAnd(inPin("${g}a"), inPin("${g}b")))
            }
            ComponentType.IC_7410 -> {
                for (g in 1..3) drive("${g}y", evalAndList(listOf(inPin("${g}a"), inPin("${g}b"), inPin("${g}c"))).invert())
            }
            ComponentType.IC_7411 -> {
                for (g in 1..3) drive("${g}y", evalAndList(listOf(inPin("${g}a"), inPin("${g}b"), inPin("${g}c"))))
            }
            ComponentType.IC_7420 -> {
                for (g in 1..2) drive("${g}y", evalAndList(listOf(inPin("${g}a"), inPin("${g}b"), inPin("${g}c"), inPin("${g}d"))).invert())
            }
            ComponentType.IC_7421 -> {
                for (g in 1..2) drive("${g}y", evalAndList(listOf(inPin("${g}a"), inPin("${g}b"), inPin("${g}c"), inPin("${g}d"))))
            }
            ComponentType.IC_7427 -> {
                for (g in 1..3) drive("${g}y", evalOrList(listOf(inPin("${g}a"), inPin("${g}b"), inPin("${g}c"))).invert())
            }
            ComponentType.IC_7430 -> {
                val all8 = listOf("a", "b", "c", "d", "e", "f", "g", "h").map { inPin(it) }
                drive("y", evalAndList(all8).invert())
            }
            ComponentType.IC_7432 -> {
                for (g in 1..4) drive("${g}y", evalOr(inPin("${g}a"), inPin("${g}b")))
            }
            ComponentType.IC_7486 -> {
                for (g in 1..4) drive("${g}y", evalXor(inPin("${g}a"), inPin("${g}b")))
            }
            ComponentType.IC_74125 -> {
                for (g in 1..4) {
                    val oe = inPin("${g}oe")
                    val out = when (oe) {
                        LogicLevel.ZERO -> inPin("${g}a")
                        LogicLevel.ONE -> LogicLevel.HIGH_Z
                        else -> LogicLevel.HIGH_Z
                    }
                    drive("${g}y", out)
                }
            }
            ComponentType.IC_74266 -> {
                for (g in 1..4) drive("${g}y", evalXor(inPin("${g}a"), inPin("${g}b")).invert())
            }

            // 14-Pin 74xx Sequential ICs
            ComponentType.IC_7472 -> {
                val q = ffQ[comp.id] ?: LogicLevel.ZERO
                drive("q", q)
                drive("qn", q.invert())
            }
            ComponentType.IC_7473 -> {
                val q1 = ffQ["${comp.id}:1"] ?: LogicLevel.ZERO
                val q2 = ffQ["${comp.id}:2"] ?: LogicLevel.ZERO
                drive("1q", q1)
                drive("1qn", q1.invert())
                drive("2q", q2)
                drive("2qn", q2.invert())
            }
            ComponentType.IC_7474 -> {
                val q1 = ffQ[comp.id] ?: LogicLevel.ZERO
                val q2 = ffQ["${comp.id}:2"] ?: LogicLevel.ZERO
                drive("q", q1)
                drive("qn", q1.invert())
                drive("2q", q2)
                drive("2qn", q2.invert())
            }
            ComponentType.IC_7476 -> {
                val q1 = ffQ[comp.id] ?: LogicLevel.ZERO
                val q2 = ffQ["${comp.id}:2"] ?: LogicLevel.ZERO
                drive("q", q1)
                drive("qn", q1.invert())
                drive("2q", q2)
            }
            ComponentType.IC_7490, ComponentType.IC_7493, ComponentType.IC_74194 -> {
                drive("qa", ffQ["${comp.id}:qa"] ?: LogicLevel.ZERO)
                drive("qb", ffQ["${comp.id}:qb"] ?: LogicLevel.ZERO)
                drive("qc", ffQ["${comp.id}:qc"] ?: LogicLevel.ZERO)
                drive("qd", ffQ["${comp.id}:qd"] ?: LogicLevel.ZERO)
            }
            ComponentType.IC_7447 -> {
                val bi = inPin("bi")
                val lt = inPin("lt")
                val rbi = inPin("rbi")
                val bcd = (if (inPin("a") == LogicLevel.ONE) 1 else 0) or
                    (if (inPin("b") == LogicLevel.ONE) 2 else 0) or
                    (if (inPin("c") == LogicLevel.ONE) 4 else 0) or
                    (if (inPin("d") == LogicLevel.ONE) 8 else 0)

                // Active-high bitmask: bit0=a, bit1=b, bit2=c, bit3=d, bit4=e, bit5=f, bit6=g
                val activeSegs = when {
                    bi == LogicLevel.ZERO -> 0 // Blanking input LOW blanks all segments
                    lt == LogicLevel.ZERO -> 0x7F // Lamp Test LOW illuminates all 7 segments
                    bcd == 0 && rbi == LogicLevel.ZERO -> 0 // Ripple Blanking zero suppression
                    else -> when (bcd) {
                        0 -> 0b0111111
                        1 -> 0b0000110
                        2 -> 0b1011011
                        3 -> 0b1001111
                        4 -> 0b1100110
                        5 -> 0b1101101
                        6 -> 0b1111100 // Classic 7447 '6' without top tail
                        7 -> 0b0000111
                        8 -> 0b1111111
                        9 -> 0b1100111 // Classic 7447 '9' without bottom tail
                        10 -> 0b1011000
                        11 -> 0b1001100
                        12 -> 0b1100010
                        13 -> 0b1101001
                        14 -> 0b1111000
                        else -> 0
                    }
                }
                // 7447 outputs are active-LOW (0 = segment ON, 1 = segment OFF)
                val segPins = listOf("sa", "sb", "sc", "sd", "se", "sf", "sg")
                segPins.forEachIndexed { idx, pin ->
                    val on = ((activeSegs shr idx) and 1) == 1
                    drive(pin, if (on) LogicLevel.ZERO else LogicLevel.ONE)
                }
            }
            ComponentType.IC_7483 -> {
                fun b(pin: String): Int = if (inPin(pin) == LogicLevel.ONE) 1 else 0
                val aVal = b("a1") + (b("a2") shl 1) + (b("a3") shl 2) + (b("a4") shl 3)
                val bVal = b("b1") + (b("b2") shl 1) + (b("b3") shl 2) + (b("b4") shl 3)
                val c0 = b("c0")
                val sum = aVal + bVal + c0
                drive("s1", if ((sum and 1) != 0) LogicLevel.ONE else LogicLevel.ZERO)
                drive("s2", if ((sum and 2) != 0) LogicLevel.ONE else LogicLevel.ZERO)
                drive("s3", if ((sum and 4) != 0) LogicLevel.ONE else LogicLevel.ZERO)
                drive("s4", if ((sum and 8) != 0) LogicLevel.ONE else LogicLevel.ZERO)
                drive("c4", if ((sum and 16) != 0) LogicLevel.ONE else LogicLevel.ZERO)
            }

            ComponentType.IC_74138 -> {
                val en = inPin("e1")
                val enabled = en == LogicLevel.ONE || en == LogicLevel.UNKNOWN
                val a0 = if (inPin("a0") == LogicLevel.ONE) 1 else 0
                val a1 = if (inPin("a1") == LogicLevel.ONE) 2 else 0
                val a2 = if (inPin("a2") == LogicLevel.ONE) 4 else 0
                val sel = a0 + a1 + a2
                for (i in 0..7) {
                    drive("y$i", if (enabled && i == sel) LogicLevel.ZERO else LogicLevel.ONE)
                }
            }
            ComponentType.IC_74151 -> {
                val s0 = if (inPin("s0") == LogicLevel.ONE) 1 else 0
                val s1 = if (inPin("s1") == LogicLevel.ONE) 2 else 0
                val s2 = if (inPin("s2") == LogicLevel.ONE) 4 else 0
                val sel = s0 + s1 + s2
                val y = if (sel <= 3) inPin("d$sel") else LogicLevel.ZERO
                drive("y", y)
                drive("wn", y.invert())
            }
            ComponentType.IC_7485 -> {
                fun b(pin: String): Int = if (inPin(pin) == LogicLevel.ONE) 1 else 0
                val aVal = b("a0") + (b("a1") shl 1) + (b("a2") shl 2) + (b("a3") shl 3)
                val bVal = b("b0") + (b("b1") shl 1) + (b("b2") shl 2) + (b("b3") shl 3)
                val (gt, eq, lt) = when {
                    aVal > bVal -> Triple(LogicLevel.ONE, LogicLevel.ZERO, LogicLevel.ZERO)
                    aVal < bVal -> Triple(LogicLevel.ZERO, LogicLevel.ZERO, LogicLevel.ONE)
                    else -> {
                        val igt = inPin("igt") == LogicLevel.ONE
                        val ilt = inPin("ilt") == LogicLevel.ONE
                        val ieq = inPin("ieq") != LogicLevel.ZERO
                        when {
                            ieq && !igt && !ilt -> Triple(LogicLevel.ZERO, LogicLevel.ONE, LogicLevel.ZERO)
                            igt && !ilt -> Triple(LogicLevel.ONE, LogicLevel.ZERO, LogicLevel.ZERO)
                            ilt && !igt -> Triple(LogicLevel.ZERO, LogicLevel.ZERO, LogicLevel.ONE)
                            else -> Triple(LogicLevel.ZERO, LogicLevel.ONE, LogicLevel.ZERO)
                        }
                    }
                }
                drive("ogt", gt)
                drive("oeq", eq)
                drive("olt", lt)
            }
            ComponentType.IC_74595 -> {
                val oeDisabled = inPin("oe") == LogicLevel.ONE
                val pinNames = listOf("qa", "qb", "qc", "qd", "qe", "qf", "qg", "qh")
                pinNames.forEachIndexed { i, pName ->
                    val q = ffQ["${comp.id}:q$i"] ?: LogicLevel.ZERO
                    drive(pName, if (oeDisabled) LogicLevel.HIGH_Z else q)
                }
                drive("qhs", ffQ["${comp.id}:s7"] ?: LogicLevel.ZERO)
            }
            ComponentType.IC_74161 -> {
                val qa = ffQ["${comp.id}:qa"] ?: LogicLevel.ZERO
                val qb = ffQ["${comp.id}:qb"] ?: LogicLevel.ZERO
                val qc = ffQ["${comp.id}:qc"] ?: LogicLevel.ZERO
                val qd = ffQ["${comp.id}:qd"] ?: LogicLevel.ZERO
                drive("qa", qa)
                drive("qb", qb)
                drive("qc", qc)
                drive("qd", qd)
                val ent = inPin("ent") != LogicLevel.ZERO
                val rco = if (ent && qa == LogicLevel.ONE && qb == LogicLevel.ONE && qc == LogicLevel.ONE && qd == LogicLevel.ONE) {
                    LogicLevel.ONE
                } else LogicLevel.ZERO
                drive("rco", rco)
            }
            ComponentType.IC_74154 -> {
                val g1 = inPin("g1")
                val g2 = inPin("g2")
                val enabled = g1 != LogicLevel.ONE && g2 != LogicLevel.ONE
                val sel = (if (inPin("a") == LogicLevel.ONE) 1 else 0) or
                    (if (inPin("b") == LogicLevel.ONE) 2 else 0) or
                    (if (inPin("c") == LogicLevel.ONE) 4 else 0) or
                    (if (inPin("d") == LogicLevel.ONE) 8 else 0)
                val outPins = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15)
                outPins.forEach { idx ->
                    drive("y$idx", if (enabled && idx == sel) LogicLevel.ZERO else LogicLevel.ONE)
                }
            }
            ComponentType.CUSTOM_IC -> {
                val meta = com.example.model.ComponentCatalog.metadataFor(comp)
                val inPins = meta.pins.filter { it.direction == com.example.model.PinDirection.INPUT }
                val outPins = meta.pins.filter { it.direction == com.example.model.PinDirection.OUTPUT }
                val truthMap = com.example.data.CustomIcStore.parseCompactTruthString(comp.properties["truthMap"].orEmpty())
                var mask = 0
                var allKnown = true
                inPins.forEachIndexed { idx, pin ->
                    val lvl = inPin(pin.id)
                    if (!lvl.isKnown()) allKnown = false
                    if (lvl == LogicLevel.ONE) {
                        mask = mask or (1 shl (inPins.size - 1 - idx))
                    }
                }
                val outMask = if (allKnown) truthMap[mask] else null
                outPins.forEachIndexed { idx, pin ->
                    val lvl = if (outMask == null) LogicLevel.UNKNOWN
                    else if (((outMask shr idx) and 1) == 1) LogicLevel.ONE else LogicLevel.ZERO
                    drive(pin.id, lvl)
                }
            }
            ComponentType.IC_555 -> {
                val rst = inPin("reset")
                val out = if (rst == LogicLevel.ZERO) LogicLevel.ZERO else (clockStates[comp.id] ?: LogicLevel.ZERO)
                drive("out", out)
            }
            else -> Unit
        }
    }

    private fun evalAnd(a: LogicLevel, b: LogicLevel): LogicLevel = when {
        a == LogicLevel.ZERO || b == LogicLevel.ZERO -> LogicLevel.ZERO
        a == LogicLevel.ONE && b == LogicLevel.ONE -> LogicLevel.ONE
        else -> LogicLevel.UNKNOWN
    }

    private fun evalAndList(levels: List<LogicLevel>): LogicLevel = when {
        levels.any { it == LogicLevel.ZERO } -> LogicLevel.ZERO
        levels.all { it == LogicLevel.ONE } -> LogicLevel.ONE
        else -> LogicLevel.UNKNOWN
    }

    private fun evalOr(a: LogicLevel, b: LogicLevel): LogicLevel = when {
        a == LogicLevel.ONE || b == LogicLevel.ONE -> LogicLevel.ONE
        a == LogicLevel.ZERO && b == LogicLevel.ZERO -> LogicLevel.ZERO
        else -> LogicLevel.UNKNOWN
    }

    private fun evalOrList(levels: List<LogicLevel>): LogicLevel = when {
        levels.any { it == LogicLevel.ONE } -> LogicLevel.ONE
        levels.all { it == LogicLevel.ZERO } -> LogicLevel.ZERO
        else -> LogicLevel.UNKNOWN
    }

    private fun evalNot(a: LogicLevel): LogicLevel = when (a) {
        LogicLevel.ZERO -> LogicLevel.ONE
        LogicLevel.ONE -> LogicLevel.ZERO
        else -> LogicLevel.UNKNOWN
    }

    private fun evalXor(a: LogicLevel, b: LogicLevel): LogicLevel = when {
        a.isKnown() && b.isKnown() -> if (a != b) LogicLevel.ONE else LogicLevel.ZERO
        else -> LogicLevel.UNKNOWN
    }

    private fun evalXorList(levels: List<LogicLevel>): LogicLevel {
        if (levels.isEmpty() || levels.any { !it.isKnown() }) return LogicLevel.UNKNOWN
        val ones = levels.count { it == LogicLevel.ONE }
        return if ((ones and 1) == 1) LogicLevel.ONE else LogicLevel.ZERO
    }
}

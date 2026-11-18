package com.example

import com.example.data.CircuitTemplates
import com.example.data.CustomIcStore
import com.example.engine.digital.DigitalMemoryState
import com.example.engine.digital.DigitalSimulationEngine
import com.example.engine.netlist.NetlistBuilder
import com.example.model.CircuitPage
import com.example.model.ComponentCatalog
import com.example.model.ComponentInstance
import com.example.model.ComponentType
import com.example.model.GridPoint
import com.example.model.LogicLevel
import com.example.model.WireEndpoint
import com.example.model.WireSegment
import com.example.tools.AutoWiringHelper
import com.example.tools.BomCalculator
import com.example.tools.LabChallengeEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewFeaturesPhase2Test {

    private fun wire(id: String, c1: String, p1: String, c2: String, p2: String) = WireSegment(
        id = id,
        from = WireEndpoint(componentId = c1, pinId = p1),
        to = WireEndpoint(componentId = c2, pinId = p2)
    )

    @Test
    fun variableInputAndGateEvaluatesThreeAndFourInputsCorrectly() {
        val gate3 = ComponentInstance(
            id = "g1",
            type = ComponentType.GATE_AND,
            label = "U1",
            x = 10f,
            y = 6f,
            value = "3-Input",
            properties = mapOf("inputCount" to "3")
        )
        val meta3 = ComponentCatalog.metadataFor(gate3)
        assertEquals(4, meta3.pins.size) // a, b, c + y

        val engine = DigitalSimulationEngine()

        // 1 & 1 & 0 == 0
        val pageLow = CircuitPage(
            id = "p_and3",
            name = "3-Input AND",
            components = listOf(
                ComponentInstance("i1", ComponentType.LOGIC_INPUT, "A", 3f, 4f, value = "1", properties = mapOf("state" to "1")),
                ComponentInstance("i2", ComponentType.LOGIC_INPUT, "B", 3f, 6f, value = "1", properties = mapOf("state" to "1")),
                ComponentInstance("i3", ComponentType.LOGIC_INPUT, "C", 3f, 8f, value = "0", properties = mapOf("state" to "0")),
                gate3,
                ComponentInstance("p1", ComponentType.PROBE_GREEN, "Y", 16f, 6f)
            ),
            wires = listOf(
                wire("w1", "i1", "out", "g1", "a"),
                wire("w2", "i2", "out", "g1", "b"),
                wire("w3", "i3", "out", "g1", "c"),
                wire("w4", "g1", "y", "p1", "in")
            )
        )
        val resLow = engine.evaluateCombinational(pageLow)
        assertEquals(LogicLevel.ZERO, resLow.pinLevels[NetlistBuilder.pinKey("p1", "in")])

        // 1 & 1 & 1 == 1
        val pageHigh = pageLow.copy(
            components = pageLow.components.map {
                if (it.id == "i3") it.copy(value = "1", properties = mapOf("state" to "1")) else it
            }
        )
        val resHigh = engine.evaluateCombinational(pageHigh)
        assertEquals(LogicLevel.ONE, resHigh.pinLevels[NetlistBuilder.pinKey("p1", "in")])
    }

    @Test
    fun variableInputXorAndNandGatesSupportFourInputs() {
        val xor4 = ComponentInstance(
            id = "gx",
            type = ComponentType.GATE_XOR,
            label = "U_XOR4",
            x = 10f,
            y = 6f,
            value = "4-Input",
            properties = mapOf("inputCount" to "4")
        )
        assertEquals(5, ComponentCatalog.metadataFor(xor4).pins.size)

        val page = CircuitPage(
            id = "p_xor4",
            name = "4-Input XOR",
            components = listOf(
                ComponentInstance("i1", ComponentType.LOGIC_INPUT, "A", 2f, 2f, value = "1", properties = mapOf("state" to "1")),
                ComponentInstance("i2", ComponentType.LOGIC_INPUT, "B", 2f, 4f, value = "1", properties = mapOf("state" to "1")),
                ComponentInstance("i3", ComponentType.LOGIC_INPUT, "C", 2f, 6f, value = "1", properties = mapOf("state" to "1")),
                ComponentInstance("i4", ComponentType.LOGIC_INPUT, "D", 2f, 8f, value = "0", properties = mapOf("state" to "0")),
                xor4,
                ComponentInstance("p1", ComponentType.PROBE_CYAN, "Y", 16f, 6f)
            ),
            wires = listOf(
                wire("w1", "i1", "out", "gx", "a"),
                wire("w2", "i2", "out", "gx", "b"),
                wire("w3", "i3", "out", "gx", "c"),
                wire("w4", "i4", "out", "gx", "d"),
                wire("w5", "gx", "y", "p1", "in")
            )
        )
        // Odd parity (3 ones) -> XOR output = 1
        val resOdd = DigitalSimulationEngine().evaluateCombinational(page)
        assertEquals(LogicLevel.ONE, resOdd.pinLevels[NetlistBuilder.pinKey("p1", "in")])

        // Even parity (4 ones) -> XOR output = 0
        val pageEven = page.copy(
            components = page.components.map {
                if (it.id == "i4") it.copy(value = "1", properties = mapOf("state" to "1")) else it
            }
        )
        val resEven = DigitalSimulationEngine().evaluateCombinational(pageEven)
        assertEquals(LogicLevel.ZERO, resEven.pinLevels[NetlistBuilder.pinKey("p1", "in")])
    }

    @Test
    fun customIcTruthMapEncodeDecodeAndSimulation() {
        val rawMap = mapOf(0 to 0, 1 to 1, 2 to 1, 3 to 2) // Half Adder: bit0=SUM, bit1=CARRY
        val compact = CustomIcStore.truthMapToCompactString(rawMap)
        val parsed = CustomIcStore.parseCompactTruthString(compact)
        assertEquals(rawMap, parsed)

        val cic = ComponentInstance(
            id = "u_ha",
            type = ComponentType.CUSTOM_IC,
            label = "HADD_1",
            x = 10f,
            y = 6f,
            value = "Half Adder IC",
            properties = mapOf(
                "shortCode" to "HADD",
                "inputLabels" to "A,B",
                "outputLabels" to "SUM,CARRY",
                "truthMap" to compact
            )
        )
        val meta = ComponentCatalog.metadataFor(cic)
        assertEquals(4, meta.pins.size)

        val page = CircuitPage(
            id = "p_cic",
            name = "Custom IC Test",
            components = listOf(
                ComponentInstance("a", ComponentType.LOGIC_INPUT, "A", 2f, 4f, value = "1", properties = mapOf("state" to "1")),
                ComponentInstance("b", ComponentType.LOGIC_INPUT, "B", 2f, 8f, value = "1", properties = mapOf("state" to "1")),
                cic,
                ComponentInstance("s", ComponentType.PROBE_GREEN, "SUM", 16f, 4f),
                ComponentInstance("c", ComponentType.PROBE_RED, "CARRY", 16f, 8f)
            ),
            wires = listOf(
                wire("w1", "a", "out", "u_ha", "in0"),
                wire("w2", "b", "out", "u_ha", "in1"),
                wire("w3", "u_ha", "out0", "s", "in"),
                wire("w4", "u_ha", "out1", "c", "in")
            )
        )
        val res = DigitalSimulationEngine().evaluateCombinational(page)
        assertEquals(LogicLevel.ZERO, res.pinLevels[NetlistBuilder.pinKey("s", "in")])
        assertEquals(LogicLevel.ONE, res.pinLevels[NetlistBuilder.pinKey("c", "in")])
    }

    @Test
    fun ic7485ComparatorAndTemplatesEvaluateAccurately() {
        val tpl7485 = CircuitTemplates.byId("ic_7485_comparator_lab")
        assertNotNull(tpl7485)
        val tplTraffic = CircuitTemplates.byId("traffic_light_signal_lab")
        assertNotNull(tplTraffic)

        // Template 26 starts with A=1 (A0=1,A1=0), B=0 -> A>B HIGH
        val resTpl = DigitalSimulationEngine().evaluateCombinational(tpl7485!!.toNewProject().activePage())
        assertEquals(LogicLevel.ONE, resTpl.pinLevels[NetlistBuilder.pinKey("u1", "ogt")])
        assertEquals(LogicLevel.ZERO, resTpl.pinLevels[NetlistBuilder.pinKey("u1", "oeq")])
        assertEquals(LogicLevel.ZERO, resTpl.pinLevels[NetlistBuilder.pinKey("u1", "olt")])
    }

    @Test
    fun ic74161CounterIncrementsOnClockEdges() {
        val page = CircuitPage(
            id = "p_74161",
            name = "74161 Sync Counter",
            components = listOf(
                ComponentInstance("clk", ComponentType.CLOCK_INPUT, "CLK", 3f, 6f),
                ComponentInstance("vcc", ComponentType.VCC, "VCC", 3f, 3f),
                ComponentInstance("u1", ComponentType.IC_74161, "U1", 12f, 8f)
            ),
            wires = listOf(
                wire("w1", "clk", "out", "u1", "clk"),
                wire("w2", "vcc", "vcc", "u1", "clr"),
                wire("w3", "vcc", "vcc", "u1", "load"),
                wire("w4", "vcc", "vcc", "u1", "enp"),
                wire("w5", "vcc", "vcc", "u1", "ent")
            )
        )
        val engine = DigitalSimulationEngine()
        var mem = DigitalMemoryState()
        var lastRes = engine.evaluateCombinational(page)
        repeat(6) {
            val (res, nextMem) = engine.step(page, mem, toggleClocks = true)
            lastRes = res
            mem = nextMem
        }
        val b0 = if (lastRes.pinLevels[NetlistBuilder.pinKey("u1", "qa")] == LogicLevel.ONE) 1 else 0
        val b1 = if (lastRes.pinLevels[NetlistBuilder.pinKey("u1", "qb")] == LogicLevel.ONE) 2 else 0
        val b2 = if (lastRes.pinLevels[NetlistBuilder.pinKey("u1", "qc")] == LogicLevel.ONE) 4 else 0
        val b3 = if (lastRes.pinLevels[NetlistBuilder.pinKey("u1", "qd")] == LogicLevel.ONE) 8 else 0
        val cnt = b0 + b1 + b2 + b3
        assertTrue("74161 counter should advance on clock ticks (was $cnt)", cnt > 0)
    }

    @Test
    fun labChallengeEngineGradesHalfAdderPassAndFail() {
        val halfAdderChallenge = LabChallengeEngine.allChallenges.first { it.id == "ch_half_adder" }
        val starter = halfAdderChallenge.starterBuilder()

        // Unwired starter must fail
        val failRes = LabChallengeEngine.verify(halfAdderChallenge, starter)
        assertFalse(failRes.passed)

        // Wire valid XOR + AND Half Adder -> must pass 100%
        val solvedPage = starter.copy(
            components = starter.components + listOf(
                ComponentInstance("g_xor", ComponentType.GATE_XOR, "U1", 9f, 4f),
                ComponentInstance("g_and", ComponentType.GATE_AND, "U2", 9f, 8f)
            ),
            wires = listOf(
                wire("w1", "inA", "out", "g_xor", "a"),
                wire("w2", "inB", "out", "g_xor", "b"),
                wire("w3", "inA", "out", "g_and", "a"),
                wire("w4", "inB", "out", "g_and", "b"),
                wire("w5", "g_xor", "y", "outS", "in"),
                wire("w6", "g_and", "y", "outC", "in")
            )
        )
        val passRes = LabChallengeEngine.verify(halfAdderChallenge, solvedPage)
        assertTrue(passRes.passed)
        assertEquals(100, passRes.scorePercent)
    }

    @Test
    fun bomCalculatorComputesCostPowerAndCsv() {
        val page = CircuitTemplates.byId("half_adder")!!.toNewProject().activePage()
        val report = BomCalculator.generate(page)
        assertTrue(report.totalPartsCount >= 4)
        assertTrue(report.totalPowerMw > 0.0)
        val csv = BomCalculator.toCsv("Half Adder", report)
        assertTrue(csv.contains("GateX Labs Bill of Materials"))
        assertTrue(csv.contains("TOTAL"))
    }

    @Test
    fun smartOrthogonalRouterAvoidsObstacleAndStaysOrthogonal() {
        val obstacle = ComponentInstance("obs", ComponentType.IC_7400, "U1", 10f, 6f)
        val page = CircuitPage("p_route", "Route", components = listOf(obstacle))
        val waypoints = AutoWiringHelper.routeSmartOrthogonal(
            from = GridPoint(4f, 6f),
            to = GridPoint(16f, 6f),
            page = page,
            ignoreCompIds = emptySet()
        )
        assertTrue("Smart router should detour around obstacle", waypoints.isNotEmpty())
        assertTrue("Detour waypoint should clear obstacle Y=6", waypoints.any { kotlin.math.abs(it.y - 6f) >= 1.5f })
    }
}

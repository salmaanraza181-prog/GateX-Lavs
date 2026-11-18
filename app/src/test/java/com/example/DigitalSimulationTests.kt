package com.example

import com.example.data.CircuitTemplates
import com.example.engine.digital.DigitalMemoryState
import com.example.engine.digital.DigitalSimulationEngine
import com.example.engine.netlist.NetlistBuilder
import com.example.model.CircuitPage
import com.example.model.ComponentInstance
import com.example.model.ComponentType
import com.example.model.LogicLevel
import com.example.model.WireEndpoint
import com.example.model.WireJunction
import com.example.model.WireSegment
import com.example.tools.BooleanEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DigitalSimulationTests {

    private val engine = DigitalSimulationEngine()

    private fun testBinaryGate(type: ComponentType, aVal: String, bVal: String): LogicLevel {
        val page = CircuitPage(
            id = "t",
            name = "t",
            components = listOf(
                ComponentInstance("a", ComponentType.LOGIC_INPUT, "A", 2f, 2f, value = aVal, properties = mapOf("state" to aVal)),
                ComponentInstance("b", ComponentType.LOGIC_INPUT, "B", 2f, 6f, value = bVal, properties = mapOf("state" to bVal)),
                ComponentInstance("g", type, "G", 8f, 4f),
                ComponentInstance("out", ComponentType.LOGIC_INDICATOR, "Y", 14f, 4f)
            ),
            wires = listOf(
                WireSegment("w1", WireEndpoint("a", "out"), WireEndpoint("g", "a")),
                WireSegment("w2", WireEndpoint("b", "out"), WireEndpoint("g", "b")),
                WireSegment("w3", WireEndpoint("g", "y"), WireEndpoint("out", "in"))
            )
        )
        val res = engine.evaluateCombinational(page)
        return res.pinLevels[NetlistBuilder.pinKey("out", "in")] ?: LogicLevel.UNKNOWN
    }

    @Test
    fun `all basic logic gates produce accurate truth tables`() {
        val pairs = listOf("0" to "0", "0" to "1", "1" to "0", "1" to "1")
        assertEquals(listOf(LogicLevel.ZERO, LogicLevel.ZERO, LogicLevel.ZERO, LogicLevel.ONE),
            pairs.map { testBinaryGate(ComponentType.GATE_AND, it.first, it.second) })
        assertEquals(listOf(LogicLevel.ZERO, LogicLevel.ONE, LogicLevel.ONE, LogicLevel.ONE),
            pairs.map { testBinaryGate(ComponentType.GATE_OR, it.first, it.second) })
        assertEquals(listOf(LogicLevel.ONE, LogicLevel.ONE, LogicLevel.ONE, LogicLevel.ZERO),
            pairs.map { testBinaryGate(ComponentType.GATE_NAND, it.first, it.second) })
        assertEquals(listOf(LogicLevel.ONE, LogicLevel.ZERO, LogicLevel.ZERO, LogicLevel.ZERO),
            pairs.map { testBinaryGate(ComponentType.GATE_NOR, it.first, it.second) })
        assertEquals(listOf(LogicLevel.ZERO, LogicLevel.ONE, LogicLevel.ONE, LogicLevel.ZERO),
            pairs.map { testBinaryGate(ComponentType.GATE_XOR, it.first, it.second) })
        assertEquals(listOf(LogicLevel.ONE, LogicLevel.ZERO, LogicLevel.ZERO, LogicLevel.ONE),
            pairs.map { testBinaryGate(ComponentType.GATE_XNOR, it.first, it.second) })
    }

    @Test
    fun `not buffer and tristate gates behave accurately`() {
        val page = CircuitPage(
            id = "tri",
            name = "tri",
            components = listOf(
                ComponentInstance("a", ComponentType.LOGIC_INPUT, "A", 2f, 4f, properties = mapOf("state" to "1")),
                ComponentInstance("en", ComponentType.LOGIC_INPUT, "EN", 2f, 8f, properties = mapOf("state" to "0")),
                ComponentInstance("not", ComponentType.GATE_NOT, "N", 8f, 2f),
                ComponentInstance("buf", ComponentType.GATE_BUFFER, "B", 8f, 5f),
                ComponentInstance("tri", ComponentType.GATE_TRISTATE, "T", 8f, 9f),
                ComponentInstance("o1", ComponentType.LOGIC_INDICATOR, "O1", 14f, 2f),
                ComponentInstance("o2", ComponentType.LOGIC_INDICATOR, "O2", 14f, 5f),
                ComponentInstance("o3", ComponentType.LOGIC_INDICATOR, "O3", 14f, 9f)
            ),
            wires = listOf(
                WireSegment("w1", WireEndpoint("a", "out"), WireEndpoint("not", "a")),
                WireSegment("w2", WireEndpoint("a", "out"), WireEndpoint("buf", "a")),
                WireSegment("w3", WireEndpoint("a", "out"), WireEndpoint("tri", "a")),
                WireSegment("w4", WireEndpoint("en", "out"), WireEndpoint("tri", "en")),
                WireSegment("w5", WireEndpoint("not", "y"), WireEndpoint("o1", "in")),
                WireSegment("w6", WireEndpoint("buf", "y"), WireEndpoint("o2", "in")),
                WireSegment("w7", WireEndpoint("tri", "y"), WireEndpoint("o3", "in"))
            )
        )
        val res = engine.evaluateCombinational(page)
        assertEquals(LogicLevel.ZERO, res.pinLevels[NetlistBuilder.pinKey("o1", "in")])
        assertEquals(LogicLevel.ONE, res.pinLevels[NetlistBuilder.pinKey("o2", "in")])
        assertEquals(LogicLevel.HIGH_Z, res.pinLevels[NetlistBuilder.pinKey("o3", "in")])
    }

    @Test
    fun `half adder and full adder truth tables match expected outputs`() {
        val halfTable = engine.generateTruthTable(CircuitTemplates.byId("half_adder")!!.buildPage())
        assertEquals(4, halfTable.rows.size)
        // A=1, B=1 -> SUM=0, CARRY=1
        val lastHalf = halfTable.rows.last().outputs
        assertEquals(listOf(LogicLevel.ONE, LogicLevel.ZERO), lastHalf) // sorted labels: CARRY, SUM

        val fullTable = engine.generateTruthTable(CircuitTemplates.byId("full_adder")!!.buildPage())
        assertEquals(8, fullTable.rows.size)
        // A=1, B=1, Cin=1 -> COUT=1, SUM=1
        assertEquals(listOf(LogicLevel.ONE, LogicLevel.ONE), fullTable.rows.last().outputs)
        // A=0, B=1, Cin=0 -> COUT=0, SUM=1
        assertEquals(listOf(LogicLevel.ZERO, LogicLevel.ONE), fullTable.rows[2].outputs)
    }

    @Test
    fun `half subtractor and full subtractor truth tables match expected outputs`() {
        val halfSub = engine.generateTruthTable(CircuitTemplates.byId("half_subtractor")!!.buildPage())
        assertEquals(4, halfSub.rows.size)
        // Sorted labels: BORROW, DIFF. For A=0, B=1 -> BORROW=1, DIFF=1
        assertEquals(listOf(LogicLevel.ONE, LogicLevel.ONE), halfSub.rows[1].outputs)

        val fullSub = engine.generateTruthTable(CircuitTemplates.byId("full_subtractor")!!.buildPage())
        assertEquals(8, fullSub.rows.size)
        // A=0, B=1, Bin=1 -> BOUT=1, DIFF=0
        assertEquals(listOf(LogicLevel.ONE, LogicLevel.ZERO), fullSub.rows[3].outputs)
    }

    @Test
    fun `sr latch and d latch hold and update state`() {
        val srPage = CircuitTemplates.byId("sr_latch")!!.buildPage() // S=1, R=0 initially
        val srSet = engine.evaluateCombinational(srPage)
        assertEquals(LogicLevel.ONE, srSet.pinLevels[NetlistBuilder.pinKey("outQ", "in")])
        assertEquals(LogicLevel.ZERO, srSet.pinLevels[NetlistBuilder.pinKey("outQn", "in")])

        val dLatch = engine.evaluateCombinational(CircuitTemplates.byId("d_latch")!!.buildPage())
        assertEquals(LogicLevel.ONE, dLatch.pinLevels[NetlistBuilder.pinKey("outQ", "in")])
    }

    @Test
    fun `jk flip flop toggles on rising clock edges`() {
        val jkPage = CircuitTemplates.byId("jk_flip_flop")!!.buildPage()
        var mem = DigitalMemoryState()
        val outputs = mutableListOf<LogicLevel>()
        repeat(6) {
            val (res, nextMem) = engine.step(jkPage, mem, toggleClocks = true)
            mem = nextMem
            outputs.add(res.pinLevels[NetlistBuilder.pinKey("outQ", "in")] ?: LogicLevel.UNKNOWN)
        }
        assertTrue(outputs.contains(LogicLevel.ONE) && outputs.contains(LogicLevel.ZERO))
    }

    @Test
    fun `short circuit contention is detected when opposing drivers share a net`() {
        val page = CircuitPage(
            id = "short",
            name = "short",
            components = listOf(
                ComponentInstance("vcc", ComponentType.VCC, "VCC", 2f, 2f),
                ComponentInstance("gnd", ComponentType.GROUND, "GND", 8f, 2f)
            ),
            junctions = listOf(WireJunction("j1", 5f, 2f)),
            wires = listOf(
                WireSegment("w1", WireEndpoint("vcc", "vcc"), WireEndpoint(junctionId = "j1")),
                WireSegment("w2", WireEndpoint("gnd", "gnd"), WireEndpoint(junctionId = "j1"))
            )
        )
        val res = engine.evaluateCombinational(page)
        assertTrue(res.hasShortCircuit)
        assertEquals(LogicLevel.CONFLICT, res.pinLevels[NetlistBuilder.pinKey("vcc", "vcc")])
    }

    @Test
    fun `boolean expression synthesizer builds working circuit matching truth table`() {
        val analysis = BooleanEngine.parseAndAnalyze("A.B + ~A.C")
        assertNotNull(analysis)
        val page = BooleanEngine.expressionToCircuitPage("A^B")
        assertNotNull(page)
        val tt = engine.generateTruthTable(page!!)
        assertEquals(4, tt.rows.size)
        assertEquals(
            listOf(LogicLevel.ZERO, LogicLevel.ONE, LogicLevel.ONE, LogicLevel.ZERO),
            tt.rows.map { it.outputs.first() }
        )
        assertFalse(engine.evaluateCombinational(page).hasShortCircuit)
    }

    @Test
    fun `ic 7472 has authentic 14-pin dip layout matching reference and toggles on clock edges`() {
        val meta = com.example.model.ComponentCatalog.get(ComponentType.IC_7472)
        assertEquals(14, meta.pins.size)
        val byNum = meta.pins.associateBy { it.pinNumber }
        assertEquals("NC", byNum[1]?.label)
        assertEquals("CLR'", byNum[2]?.label)
        assertTrue(byNum[2]?.activeLow == true)
        assertEquals("J1", byNum[3]?.label)
        assertEquals("J2", byNum[4]?.label)
        assertEquals("J3", byNum[5]?.label)
        assertEquals("Q'", byNum[6]?.label)
        assertTrue(byNum[6]?.activeLow == true)
        assertEquals("GND", byNum[7]?.label)
        assertEquals("Q", byNum[8]?.label)
        assertEquals("K1", byNum[9]?.label)
        assertEquals("K2", byNum[10]?.label)
        assertEquals("K3", byNum[11]?.label)
        assertEquals("CLK", byNum[12]?.label)
        assertTrue(byNum[12]?.isClock == true)
        assertEquals("PRE'", byNum[13]?.label)
        assertTrue(byNum[13]?.activeLow == true)
        assertEquals("VCC", byNum[14]?.label)

        val page = CircuitTemplates.byId("ic_7472_jk_lab")!!.buildPage()
        var mem = DigitalMemoryState()
        val qSeq = mutableListOf<LogicLevel>()
        val qnSeq = mutableListOf<LogicLevel>()
        repeat(6) {
            val (res, nextMem) = engine.step(page, mem, toggleClocks = true)
            mem = nextMem
            qSeq.add(res.pinLevels[NetlistBuilder.pinKey("pQ", "in")] ?: LogicLevel.UNKNOWN)
            qnSeq.add(res.pinLevels[NetlistBuilder.pinKey("pQn", "in")] ?: LogicLevel.UNKNOWN)
        }
        assertTrue(qSeq.contains(LogicLevel.ONE) && qSeq.contains(LogicLevel.ZERO))
        assertTrue(qnSeq.contains(LogicLevel.ONE) && qnSeq.contains(LogicLevel.ZERO))
    }

    @Test
    fun `all 14-pin 74xx gate ics have 14 numbered pins and simulate accurately`() {
        val dip14Types = listOf(
            ComponentType.IC_7400, ComponentType.IC_7402, ComponentType.IC_7404,
            ComponentType.IC_7408, ComponentType.IC_7410, ComponentType.IC_7411,
            ComponentType.IC_7420, ComponentType.IC_7421, ComponentType.IC_7427,
            ComponentType.IC_7430, ComponentType.IC_7432, ComponentType.IC_7472,
            ComponentType.IC_7473, ComponentType.IC_7474, ComponentType.IC_7476,
            ComponentType.IC_7486, ComponentType.IC_7490, ComponentType.IC_7493,
            ComponentType.IC_74125, ComponentType.IC_74266
        )
        for (t in dip14Types) {
            val m = com.example.model.ComponentCatalog.get(t)
            assertEquals("Expected 14 pins on $t", 14, m.pins.size)
            assertEquals("Expected pins 1..14 on $t", (1..14).toSet(), m.pins.mapNotNull { it.pinNumber }.toSet())
        }

        // Verify 7400 Gate 4 (pins 12,13 -> 11) + Red Probe
        val page7400 = CircuitPage(
            id = "p7400",
            name = "7400",
            components = listOf(
                ComponentInstance("a", ComponentType.LOGIC_INPUT, "A", 2f, 2f, properties = mapOf("state" to "1")),
                ComponentInstance("b", ComponentType.LOGIC_INPUT, "B", 2f, 5f, properties = mapOf("state" to "0")),
                ComponentInstance("u1", ComponentType.IC_7400, "U1", 10f, 5f),
                ComponentInstance("pr", ComponentType.PROBE_RED, "P1", 18f, 5f)
            ),
            wires = listOf(
                WireSegment("w1", WireEndpoint("a", "out"), WireEndpoint("u1", "4a")),
                WireSegment("w2", WireEndpoint("b", "out"), WireEndpoint("u1", "4b")),
                WireSegment("w3", WireEndpoint("u1", "4y"), WireEndpoint("pr", "in"))
            )
        )
        val res7400 = engine.evaluateCombinational(page7400)
        assertEquals(LogicLevel.ONE, res7400.pinLevels[NetlistBuilder.pinKey("pr", "in")])
        assertEquals("1", res7400.componentStates["pr"]?.get("state"))
    }

    @Test
    fun `ic 7490 decade counter counts 0 through 9 and drives probes hex and decoded bargraph`() {
        val page = CircuitTemplates.byId("ic_7490_bcd_counter")!!.buildPage()
        var mem = DigitalMemoryState()
        val seenDigits = mutableSetOf<Int>()
        repeat(24) {
            val (res, nextMem) = engine.step(page, mem, toggleClocks = true)
            mem = nextMem
            val hexVal = res.componentStates["hex"]?.get("value")?.toIntOrNull()
            val barVal = res.componentStates["bar"]?.get("value")?.toIntOrNull()
            assertEquals(hexVal, barVal)
            if (hexVal != null) seenDigits.add(hexVal)
        }
        assertEquals((0..9).toSet(), seenDigits)
    }
}

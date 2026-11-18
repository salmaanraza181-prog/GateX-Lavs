package com.example

import com.example.engine.analog.AnalogSimulationEngine
import com.example.engine.digital.DigitalSimulationEngine
import com.example.engine.netlist.NetlistBuilder
import com.example.model.CircuitPage
import com.example.model.ComponentInstance
import com.example.model.ComponentType
import com.example.model.LogicLevel
import com.example.model.WireEndpoint
import com.example.model.WireJunction
import com.example.model.WireSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SwitchSimulationTests {

    private val digitalEngine = DigitalSimulationEngine()
    private val analogEngine = AnalogSimulationEngine()

    @Test
    fun `spdt switch routes common to nc by default and to no when toggled`() {
        fun runWithPos(pos: String): Pair<LogicLevel, LogicLevel> {
            val page = CircuitPage(
                id = "spdt",
                name = "SPDT Test",
                components = listOf(
                    ComponentInstance("in1", ComponentType.LOGIC_INPUT, "IN", 2f, 4f, properties = mapOf("state" to "1")),
                    ComponentInstance("sw", ComponentType.SPDT_SWITCH, "SW1", 6f, 4f, properties = mapOf("position" to pos)),
                    ComponentInstance("outNo", ComponentType.LOGIC_INDICATOR, "NO", 10f, 2f),
                    ComponentInstance("outNc", ComponentType.LOGIC_INDICATOR, "NC", 10f, 6f)
                ),
                wires = listOf(
                    WireSegment("w1", WireEndpoint("in1", "out"), WireEndpoint("sw", "com")),
                    WireSegment("w2", WireEndpoint("sw", "no"), WireEndpoint("outNo", "in")),
                    WireSegment("w3", WireEndpoint("sw", "nc"), WireEndpoint("outNc", "in"))
                )
            )
            val res = digitalEngine.evaluateCombinational(page)
            val noLvl = res.pinLevels[NetlistBuilder.pinKey("outNo", "in")] ?: LogicLevel.UNKNOWN
            val ncLvl = res.pinLevels[NetlistBuilder.pinKey("outNc", "in")] ?: LogicLevel.UNKNOWN
            return noLvl to ncLvl
        }

        val (noAtNc, ncAtNc) = runWithPos("NC")
        assertEquals(LogicLevel.UNKNOWN, noAtNc)
        assertEquals(LogicLevel.ONE, ncAtNc)

        val (noAtNo, ncAtNo) = runWithPos("NO")
        assertEquals(LogicLevel.ONE, noAtNo)
        assertEquals(LogicLevel.UNKNOWN, ncAtNo)
    }

    @Test
    fun `dpst switch simultaneously opens and closes both isolated poles`() {
        fun runClosed(closed: String): Pair<LogicLevel, LogicLevel> {
            val page = CircuitPage(
                id = "dpst",
                name = "DPST Test",
                components = listOf(
                    ComponentInstance("inA", ComponentType.LOGIC_INPUT, "A", 2f, 2f, properties = mapOf("state" to "1")),
                    ComponentInstance("inB", ComponentType.LOGIC_INPUT, "B", 2f, 6f, properties = mapOf("state" to "0")),
                    ComponentInstance("sw", ComponentType.DPST_SWITCH, "DPST1", 6f, 4f, properties = mapOf("closed" to closed)),
                    ComponentInstance("outA", ComponentType.LOGIC_INDICATOR, "OA", 10f, 2f),
                    ComponentInstance("outB", ComponentType.LOGIC_INDICATOR, "OB", 10f, 6f)
                ),
                wires = listOf(
                    WireSegment("w1", WireEndpoint("inA", "out"), WireEndpoint("sw", "a1")),
                    WireSegment("w2", WireEndpoint("sw", "a2"), WireEndpoint("outA", "in")),
                    WireSegment("w3", WireEndpoint("inB", "out"), WireEndpoint("sw", "b1")),
                    WireSegment("w4", WireEndpoint("sw", "b2"), WireEndpoint("outB", "in"))
                )
            )
            val res = digitalEngine.evaluateCombinational(page)
            val aLvl = res.pinLevels[NetlistBuilder.pinKey("outA", "in")] ?: LogicLevel.UNKNOWN
            val bLvl = res.pinLevels[NetlistBuilder.pinKey("outB", "in")] ?: LogicLevel.UNKNOWN
            return aLvl to bLvl
        }

        assertEquals(LogicLevel.UNKNOWN to LogicLevel.UNKNOWN, runClosed("false"))
        assertEquals(LogicLevel.ONE to LogicLevel.ZERO, runClosed("true"))
    }

    @Test
    fun `dpdt switch throws both commons together`() {
        fun runWithPos(pos: String): List<LogicLevel> {
            val page = CircuitPage(
                id = "dpdt",
                name = "DPDT Test",
                components = listOf(
                    ComponentInstance("in1", ComponentType.LOGIC_INPUT, "A", 2f, 2f, properties = mapOf("state" to "1")),
                    ComponentInstance("in2", ComponentType.LOGIC_INPUT, "B", 2f, 6f, properties = mapOf("state" to "0")),
                    ComponentInstance("sw", ComponentType.DPDT_SWITCH, "DPDT1", 6f, 4f, properties = mapOf("position" to pos)),
                    ComponentInstance("oNo1", ComponentType.LOGIC_INDICATOR, "ON1", 10f, 1f),
                    ComponentInstance("oNc1", ComponentType.LOGIC_INDICATOR, "OC1", 10f, 3f),
                    ComponentInstance("oNo2", ComponentType.LOGIC_INDICATOR, "ON2", 10f, 5f),
                    ComponentInstance("oNc2", ComponentType.LOGIC_INDICATOR, "OC2", 10f, 7f)
                ),
                wires = listOf(
                    WireSegment("w1", WireEndpoint("in1", "out"), WireEndpoint("sw", "com1")),
                    WireSegment("w2", WireEndpoint("in2", "out"), WireEndpoint("sw", "com2")),
                    WireSegment("w3", WireEndpoint("sw", "no1"), WireEndpoint("oNo1", "in")),
                    WireSegment("w4", WireEndpoint("sw", "nc1"), WireEndpoint("oNc1", "in")),
                    WireSegment("w5", WireEndpoint("sw", "no2"), WireEndpoint("oNo2", "in")),
                    WireSegment("w6", WireEndpoint("sw", "nc2"), WireEndpoint("oNc2", "in"))
                )
            )
            val res = digitalEngine.evaluateCombinational(page)
            return listOf("oNo1", "oNc1", "oNo2", "oNc2").map { id ->
                res.pinLevels[NetlistBuilder.pinKey(id, "in")] ?: LogicLevel.UNKNOWN
            }
        }

        assertEquals(listOf(LogicLevel.UNKNOWN, LogicLevel.ONE, LogicLevel.UNKNOWN, LogicLevel.ZERO), runWithPos("NC"))
        assertEquals(listOf(LogicLevel.ONE, LogicLevel.UNKNOWN, LogicLevel.ZERO, LogicLevel.UNKNOWN), runWithPos("NO"))
    }

    @Test
    fun `push button nc conducts when released and breaks when pressed`() {
        fun evalPressed(pressed: String): LogicLevel {
            val page = CircuitPage(
                id = "pbnc",
                name = "PB NC",
                components = listOf(
                    ComponentInstance("in1", ComponentType.LOGIC_INPUT, "IN", 2f, 2f, properties = mapOf("state" to "1")),
                    ComponentInstance("pb", ComponentType.PUSH_BUTTON_NC, "PB1", 6f, 2f, properties = mapOf("pressed" to pressed)),
                    ComponentInstance("out", ComponentType.LOGIC_INDICATOR, "OUT", 10f, 2f)
                ),
                wires = listOf(
                    WireSegment("w1", WireEndpoint("in1", "out"), WireEndpoint("pb", "p1")),
                    WireSegment("w2", WireEndpoint("pb", "p2"), WireEndpoint("out", "in"))
                )
            )
            return digitalEngine.evaluateCombinational(page).pinLevels[NetlistBuilder.pinKey("out", "in")] ?: LogicLevel.UNKNOWN
        }

        assertEquals(LogicLevel.ONE, evalPressed("false"))
        assertEquals(LogicLevel.UNKNOWN, evalPressed("true"))
    }

    @Test
    fun `rotary switch connects com exclusively to selected pole p1 to p4`() {
        val page = CircuitPage(
            id = "rot",
            name = "Rotary",
            components = listOf(
                ComponentInstance("in1", ComponentType.LOGIC_INPUT, "IN", 2f, 4f, properties = mapOf("state" to "1")),
                ComponentInstance("sw", ComponentType.ROTARY_SWITCH, "ROT1", 6f, 4f, properties = mapOf("position" to "3")),
                ComponentInstance("o1", ComponentType.LOGIC_INDICATOR, "O1", 10f, 1f),
                ComponentInstance("o2", ComponentType.LOGIC_INDICATOR, "O2", 10f, 3f),
                ComponentInstance("o3", ComponentType.LOGIC_INDICATOR, "O3", 10f, 5f),
                ComponentInstance("o4", ComponentType.LOGIC_INDICATOR, "O4", 10f, 7f)
            ),
            wires = listOf(
                WireSegment("w1", WireEndpoint("in1", "out"), WireEndpoint("sw", "com")),
                WireSegment("w2", WireEndpoint("sw", "p1"), WireEndpoint("o1", "in")),
                WireSegment("w3", WireEndpoint("sw", "p2"), WireEndpoint("o2", "in")),
                WireSegment("w4", WireEndpoint("sw", "p3"), WireEndpoint("o3", "in")),
                WireSegment("w5", WireEndpoint("sw", "p4"), WireEndpoint("o4", "in"))
            )
        )
        val res = digitalEngine.evaluateCombinational(page)
        assertEquals(LogicLevel.UNKNOWN, res.pinLevels[NetlistBuilder.pinKey("o1", "in")])
        assertEquals(LogicLevel.UNKNOWN, res.pinLevels[NetlistBuilder.pinKey("o2", "in")])
        assertEquals(LogicLevel.ONE, res.pinLevels[NetlistBuilder.pinKey("o3", "in")])
        assertEquals(LogicLevel.UNKNOWN, res.pinLevels[NetlistBuilder.pinKey("o4", "in")])
    }

    @Test
    fun `voltage controlled switch closes load path above threshold`() {
        fun solveWithCtrl(ctrlV: Double): Double {
            val page = CircuitPage(
                id = "vsw",
                name = "VSW Test",
                components = listOf(
                    ComponentInstance("vctrl", ComponentType.DC_SOURCE, "Vc", 2f, 2f, properties = mapOf("voltage" to ctrlV.toString())),
                    ComponentInstance("vload", ComponentType.DC_SOURCE, "Vs", 2f, 8f, properties = mapOf("voltage" to "10.0")),
                    ComponentInstance("vsw", ComponentType.VOLTAGE_CONTROLLED_SWITCH, "S1", 6f, 8f, properties = mapOf("von" to "2.5", "ron" to "0.05")),
                    ComponentInstance("rload", ComponentType.RESISTOR, "RL", 10f, 8f, value = "1k", properties = mapOf("resistance" to "1000")),
                    ComponentInstance("gnd", ComponentType.GROUND, "GND", 6f, 12f)
                ),
                junctions = listOf(WireJunction("jg", 6f, 11f)),
                wires = listOf(
                    WireSegment("wc1", WireEndpoint("vctrl", "pos"), WireEndpoint("vsw", "ctrl_pos")),
                    WireSegment("wc2", WireEndpoint("vctrl", "neg"), WireEndpoint(junctionId = "jg")),
                    WireSegment("wc3", WireEndpoint("vsw", "ctrl_neg"), WireEndpoint(junctionId = "jg")),
                    WireSegment("wl1", WireEndpoint("vload", "pos"), WireEndpoint("vsw", "sw1")),
                    WireSegment("wl2", WireEndpoint("vsw", "sw2"), WireEndpoint("rload", "p1")),
                    WireSegment("wl3", WireEndpoint("rload", "p2"), WireEndpoint(junctionId = "jg")),
                    WireSegment("wl4", WireEndpoint("vload", "neg"), WireEndpoint(junctionId = "jg")),
                    WireSegment("wg", WireEndpoint("gnd", "gnd"), WireEndpoint(junctionId = "jg"))
                )
            )
            val res = analogEngine.solveDcAndTransient(page)
            return res.componentCurrents["rload"] ?: 0.0
        }

        val openCurrent = solveWithCtrl(1.0)
        val closedCurrent = solveWithCtrl(5.0)
        assertTrue("Expected open current near 0A, got $openCurrent", openCurrent < 1e-4)
        assertTrue("Expected closed current near 10mA, got $closedCurrent", closedCurrent > 0.009)
    }

    @Test
    fun `spdt relay energizes coil and transfers common from nc to no`() {
        fun solveRelay(coilV: Double): Pair<Double, Double> {
            val page = CircuitPage(
                id = "rly",
                name = "Relay SPDT Test",
                components = listOf(
                    ComponentInstance("vcoil", ComponentType.DC_SOURCE, "Vcoil", 2f, 2f, properties = mapOf("voltage" to coilV.toString())),
                    ComponentInstance("vs", ComponentType.DC_SOURCE, "Vs", 2f, 8f, properties = mapOf("voltage" to "12.0")),
                    ComponentInstance("rly", ComponentType.RELAY_SPDT, "K1", 6f, 5f, properties = mapOf("coilVoltage" to "5.0", "coilResistance" to "100")),
                    ComponentInstance("rNo", ComponentType.RESISTOR, "RNO", 10f, 3f, value = "1k", properties = mapOf("resistance" to "1000")),
                    ComponentInstance("rNc", ComponentType.RESISTOR, "RNC", 10f, 7f, value = "1k", properties = mapOf("resistance" to "1000")),
                    ComponentInstance("gnd", ComponentType.GROUND, "GND", 6f, 11f)
                ),
                junctions = listOf(WireJunction("jg", 6f, 10f)),
                wires = listOf(
                    WireSegment("w1", WireEndpoint("vcoil", "pos"), WireEndpoint("rly", "coil1")),
                    WireSegment("w2", WireEndpoint("rly", "coil2"), WireEndpoint(junctionId = "jg")),
                    WireSegment("w3", WireEndpoint("vcoil", "neg"), WireEndpoint(junctionId = "jg")),
                    WireSegment("w4", WireEndpoint("vs", "pos"), WireEndpoint("rly", "com")),
                    WireSegment("w5", WireEndpoint("vs", "neg"), WireEndpoint(junctionId = "jg")),
                    WireSegment("w6", WireEndpoint("rly", "no"), WireEndpoint("rNo", "p1")),
                    WireSegment("w7", WireEndpoint("rNo", "p2"), WireEndpoint(junctionId = "jg")),
                    WireSegment("w8", WireEndpoint("rly", "nc"), WireEndpoint("rNc", "p1")),
                    WireSegment("w9", WireEndpoint("rNc", "p2"), WireEndpoint(junctionId = "jg")),
                    WireSegment("w10", WireEndpoint("gnd", "gnd"), WireEndpoint(junctionId = "jg"))
                )
            )
            val res = analogEngine.solveDcAndTransient(page)
            return (res.componentCurrents["rNo"] ?: 0.0) to (res.componentCurrents["rNc"] ?: 0.0)
        }

        val (noIdle, ncIdle) = solveRelay(0.0)
        assertTrue("Idle relay NO should be ~0A, got $noIdle", noIdle < 1e-4)
        assertTrue("Idle relay NC should carry ~12mA, got $ncIdle", ncIdle > 0.011)

        val (noEnergized, ncEnergized) = solveRelay(5.0)
        assertTrue("Energized relay NO should carry ~12mA, got $noEnergized", noEnergized > 0.011)
        assertTrue("Energized relay NC should be ~0A, got $ncEnergized", ncEnergized < 1e-4)
    }

    @Test
    fun `spdt changeover and relay motor templates converge cleanly`() {
        val spdtTpl = com.example.data.CircuitTemplates.byId("spdt_changeover_lab")!!
        val spdtPage = spdtTpl.buildPage()
        val spdtRes = analogEngine.solveDcAndTransient(spdtPage)
        assertTrue("SPDT changeover template should converge", spdtRes.converged)
        val iNc = spdtRes.componentCurrents["led2"] ?: 0.0
        val iNo = spdtRes.componentCurrents["led1"] ?: 0.0
        assertTrue("Default NC LED should conduct (>5mA), got $iNc", iNc > 0.005)
        assertTrue("Default NO LED should be off (<0.1mA), got $iNo", iNo < 1e-4)

        val relayTpl = com.example.data.CircuitTemplates.byId("relay_motor_driver")!!
        val relayPage = relayTpl.buildPage()
        val relayRes = analogEngine.solveDcAndTransient(relayPage)
        assertTrue("Relay motor driver template should converge", relayRes.converged)
        val iMotor = relayRes.componentCurrents["m1"] ?: 0.0
        val iBuzzer = relayRes.componentCurrents["bz1"] ?: 0.0
        assertTrue("Energized relay motor should draw current (>0.1A), got $iMotor", iMotor > 0.1)
        assertTrue("Energized relay NC buzzer should be disconnected (<0.1mA), got $iBuzzer", iBuzzer < 1e-4)
    }
}

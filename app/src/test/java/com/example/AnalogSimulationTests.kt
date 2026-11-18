package com.example

import com.example.data.CircuitTemplates
import com.example.engine.analog.AnalogSimulationEngine
import com.example.engine.netlist.NetlistBuilder
import com.example.model.CircuitPage
import com.example.model.ComponentInstance
import com.example.model.ComponentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalogSimulationTests {

    private val engine = AnalogSimulationEngine()

    @Test
    fun `series resistor network computes exact Ohm law current`() {
        val page = CircuitTemplates.byId("series_circuit")!!.buildPage()
        val res = engine.solveDcAndTransient(page)
        assertTrue(res.converged)
        // 9V / (1k + 2k) = 3mA
        val i = res.componentCurrents["r1"] ?: 0.0
        assertEquals(0.003, i, 1e-4)
    }

    @Test
    fun `parallel resistor network splits current evenly`() {
        val page = CircuitTemplates.byId("parallel_circuit")!!.buildPage()
        val res = engine.solveDcAndTransient(page)
        assertTrue(res.converged)
        // 10V across two 1k resistors -> 10mA each, 20mA total source current
        assertEquals(0.01, res.componentCurrents["r1"] ?: 0.0, 1e-4)
        assertEquals(0.01, res.componentCurrents["r2"] ?: 0.0, 1e-4)
        assertEquals(0.02, res.componentCurrents["v1"] ?: 0.0, 1e-4)
    }

    @Test
    fun `voltage divider outputs exact 5V from 10V across equal resistors`() {
        val page = CircuitTemplates.byId("voltage_divider")!!.buildPage()
        val res = engine.solveDcAndTransient(page)
        assertTrue(res.converged)
        val vOut = res.pinVoltages[NetlistBuilder.pinKey("vm", "pos")] ?: 0.0
        assertEquals(5.0, vOut, 0.02)
    }

    @Test
    fun `led with 220 ohm resistor on 5V computes realistic forward current`() {
        val page = CircuitTemplates.byId("led_resistor")!!.buildPage()
        val res = engine.solveDcAndTransient(page)
        assertTrue(res.converged)
        // (5.0 - 2.0) / (220 + 15) ≈ 12.7mA
        val curMa = (res.componentCurrents["led1"] ?: 0.0) * 1000.0
        assertTrue("Expected ~12-14mA, got $curMa", curMa in 11.0..15.0)
    }

    @Test
    fun `rc charging circuit generates monotonic exponential charging curve`() {
        val page = CircuitTemplates.byId("rc_charging")!!.buildPage()
        val res = engine.solveDcAndTransient(page, transientDurationSec = 0.05, steps = 50)
        assertTrue(res.converged)
        assertTrue(res.transientSamples.size >= 40)
        val firstV = res.transientSamples.first().channelValues["CH2"] ?: 0.0
        val midV = res.transientSamples[10].channelValues["CH2"] ?: 0.0
        val endV = res.transientSamples.last().channelValues["CH2"] ?: 0.0
        assertTrue("Expected monotonic rise: first=$firstV, mid=$midV, end=$endV", firstV < midV && midV < endV)
        assertTrue("Expected Vc at 5*tau (~50ms) close to 5V, got $endV", endV > 4.7)
    }

    @Test
    fun `circuit without ground or sources reports diagnostic error instead of fake results`() {
        val page = CircuitPage(
            id = "bad",
            name = "bad",
            components = listOf(ComponentInstance("r1", ComponentType.RESISTOR, "R1", 4f, 4f, value = "1k"))
        )
        val res = engine.solveDcAndTransient(page)
        assertFalse(res.converged)
        assertTrue(res.diagnostics.any { it.code == "NO_GROUND" })
    }
}

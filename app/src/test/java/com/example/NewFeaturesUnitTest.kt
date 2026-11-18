package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CircuitTemplates
import com.example.data.ProjectSerializer
import com.example.engine.analog.AnalogSimulationEngine
import com.example.engine.digital.DigitalMemoryState
import com.example.engine.digital.DigitalSimulationEngine
import com.example.engine.netlist.NetlistBuilder
import com.example.export.ExportManager
import com.example.export.PdfReportMetadata
import com.example.export.PngExportOptions
import com.example.model.CircuitPage
import com.example.model.CircuitProject
import com.example.model.ComponentInstance
import com.example.model.ComponentType
import com.example.model.LogicLevel
import com.example.model.WireEndpoint
import com.example.model.WireSegment
import com.example.tools.BooleanEngine
import com.example.ui.i18n.AppStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NewFeaturesUnitTest {

    @Test
    fun `wireless NET_LABEL joins matching net tags across disconnected canvas regions`() {
        val page = CircuitPage(
            id = "p_net",
            name = "Net Test",
            components = listOf(
                ComponentInstance("in1", ComponentType.LOGIC_INPUT, "A", 2f, 4f, value = "1", properties = mapOf("state" to "1")),
                ComponentInstance("tag1", ComponentType.NET_LABEL, "N1", 6f, 4f, value = "clk"),
                ComponentInstance("tag2", ComponentType.NET_LABEL, "N2", 18f, 10f, value = " CLK "),
                ComponentInstance("probe", ComponentType.PROBE_GREEN, "P1", 22f, 10f)
            ),
            wires = listOf(
                WireSegment("w1", WireEndpoint("in1", "out"), WireEndpoint("tag1", "pin")),
                WireSegment("w2", WireEndpoint("tag2", "pin"), WireEndpoint("probe", "in"))
            )
        )

        val res = DigitalSimulationEngine().evaluateCombinational(page)
        assertEquals(LogicLevel.ONE, res.pinLevels[NetlistBuilder.pinKey("probe", "in")])
    }

    @Test
    fun `IC_7447 decodes BCD digits to active-LOW segments and supports Lamp Test and Blanking`() {
        fun build7447Page(bcd: Int, lt: Int = 1, bi: Int = 1): CircuitPage {
            val comps = mutableListOf(
                ComponentInstance("u1", ComponentType.IC_7447, "U1", 10f, 8f),
                ComponentInstance("in_a", ComponentType.LOGIC_INPUT, "A", 2f, 2f, value = ((bcd shr 0) and 1).toString(), properties = mapOf("state" to ((bcd shr 0) and 1).toString())),
                ComponentInstance("in_b", ComponentType.LOGIC_INPUT, "B", 2f, 4f, value = ((bcd shr 1) and 1).toString(), properties = mapOf("state" to ((bcd shr 1) and 1).toString())),
                ComponentInstance("in_c", ComponentType.LOGIC_INPUT, "C", 2f, 6f, value = ((bcd shr 2) and 1).toString(), properties = mapOf("state" to ((bcd shr 2) and 1).toString())),
                ComponentInstance("in_d", ComponentType.LOGIC_INPUT, "D", 2f, 8f, value = ((bcd shr 3) and 1).toString(), properties = mapOf("state" to ((bcd shr 3) and 1).toString())),
                ComponentInstance("in_lt", ComponentType.LOGIC_INPUT, "LT", 2f, 10f, value = lt.toString(), properties = mapOf("state" to lt.toString())),
                ComponentInstance("in_bi", ComponentType.LOGIC_INPUT, "BI", 2f, 12f, value = bi.toString(), properties = mapOf("state" to bi.toString()))
            )
            val wires = listOf(
                WireSegment("wa", WireEndpoint("in_a", "out"), WireEndpoint("u1", "a")),
                WireSegment("wb", WireEndpoint("in_b", "out"), WireEndpoint("u1", "b")),
                WireSegment("wc", WireEndpoint("in_c", "out"), WireEndpoint("u1", "c")),
                WireSegment("wd", WireEndpoint("in_d", "out"), WireEndpoint("u1", "d")),
                WireSegment("wlt", WireEndpoint("in_lt", "out"), WireEndpoint("u1", "lt")),
                WireSegment("wbi", WireEndpoint("in_bi", "out"), WireEndpoint("u1", "bi"))
            )
            return CircuitPage("p_7447", "7447", comps, wires)
        }

        val engine = DigitalSimulationEngine()
        // Digit 0: segments a..f ON (active-LOW => ZERO), segment g OFF (ONE)
        val r0 = engine.evaluateCombinational(build7447Page(0))
        assertEquals(LogicLevel.ZERO, r0.pinLevels[NetlistBuilder.pinKey("u1", "sa")])
        assertEquals(LogicLevel.ZERO, r0.pinLevels[NetlistBuilder.pinKey("u1", "sf")])
        assertEquals(LogicLevel.ONE, r0.pinLevels[NetlistBuilder.pinKey("u1", "sg")])

        // Digit 8: all segments ON (ZERO)
        val r8 = engine.evaluateCombinational(build7447Page(8))
        listOf("sa", "sb", "sc", "sd", "se", "sf", "sg").forEach { pin ->
            assertEquals("Expected $pin active LOW for digit 8", LogicLevel.ZERO, r8.pinLevels[NetlistBuilder.pinKey("u1", pin)])
        }

        // Lamp Test (LT'=0): all segments ON (ZERO) even for digit 1
        val rLt = engine.evaluateCombinational(build7447Page(1, lt = 0, bi = 1))
        listOf("sa", "sb", "sc", "sd", "se", "sf", "sg").forEach { pin ->
            assertEquals("Expected $pin active LOW during Lamp Test", LogicLevel.ZERO, rLt.pinLevels[NetlistBuilder.pinKey("u1", pin)])
        }

        // Blanking (BI'=0): all segments OFF (ONE)
        val rBi = engine.evaluateCombinational(build7447Page(8, lt = 1, bi = 0))
        listOf("sa", "sb", "sc", "sd", "se", "sf", "sg").forEach { pin ->
            assertEquals("Expected $pin HIGH (blanked) when BI'=0", LogicLevel.ONE, rBi.pinLevels[NetlistBuilder.pinKey("u1", pin)])
        }
    }

    @Test
    fun `IC_7483 computes 4-bit binary addition with carry in and carry out`() {
        fun addPage(a: Int, b: Int, c0: Int): CircuitPage {
            val comps = mutableListOf(ComponentInstance("u1", ComponentType.IC_7483, "U1", 10f, 8f))
            val wires = mutableListOf<WireSegment>()
            fun wireBit(name: String, pinId: String, bitVal: Int) {
                comps.add(ComponentInstance("in_$name", ComponentType.LOGIC_INPUT, name, 2f, 2f, value = bitVal.toString(), properties = mapOf("state" to bitVal.toString())))
                wires.add(WireSegment("w_$name", WireEndpoint("in_$name", "out"), WireEndpoint("u1", pinId)))
            }
            for (i in 0..3) {
                wireBit("a${i + 1}", "a${i + 1}", (a shr i) and 1)
                wireBit("b${i + 1}", "b${i + 1}", (b shr i) and 1)
            }
            wireBit("c0", "c0", c0)
            return CircuitPage("p_7483", "7483", comps, wires)
        }

        val engine = DigitalSimulationEngine()
        // 5 + 6 + 0 = 11 (binary 1011, C4 = 0)
        val r1 = engine.evaluateCombinational(addPage(5, 6, 0))
        val sum1 = (if (r1.pinLevels[NetlistBuilder.pinKey("u1", "s1")] == LogicLevel.ONE) 1 else 0) +
            (if (r1.pinLevels[NetlistBuilder.pinKey("u1", "s2")] == LogicLevel.ONE) 2 else 0) +
            (if (r1.pinLevels[NetlistBuilder.pinKey("u1", "s3")] == LogicLevel.ONE) 4 else 0) +
            (if (r1.pinLevels[NetlistBuilder.pinKey("u1", "s4")] == LogicLevel.ONE) 8 else 0)
        assertEquals(11, sum1)
        assertEquals(LogicLevel.ZERO, r1.pinLevels[NetlistBuilder.pinKey("u1", "c4")])

        // 15 + 1 + 0 = 16 (S = 0, C4 = 1)
        val r2 = engine.evaluateCombinational(addPage(15, 1, 0))
        assertEquals(LogicLevel.ZERO, r2.pinLevels[NetlistBuilder.pinKey("u1", "s1")])
        assertEquals(LogicLevel.ZERO, r2.pinLevels[NetlistBuilder.pinKey("u1", "s4")])
        assertEquals(LogicLevel.ONE, r2.pinLevels[NetlistBuilder.pinKey("u1", "c4")])
    }

    @Test
    fun `IC_74194 supports Parallel Load, Shift Right, and Async Clear`() {
        fun build74194(clk: Int, clr: Int, s1: Int, s0: Int, sr: Int, a: Int, b: Int, c: Int, d: Int): CircuitPage {
            val comps = mutableListOf(ComponentInstance("u1", ComponentType.IC_74194, "U1", 10f, 8f))
            val wires = mutableListOf<WireSegment>()
            fun wireIn(pin: String, v: Int) {
                comps.add(ComponentInstance("in_$pin", ComponentType.LOGIC_INPUT, pin, 2f, 2f, value = v.toString(), properties = mapOf("state" to v.toString())))
                wires.add(WireSegment("w_$pin", WireEndpoint("in_$pin", "out"), WireEndpoint("u1", pin)))
            }
            wireIn("clk", clk); wireIn("clr", clr); wireIn("s1", s1); wireIn("s0", s0)
            wireIn("sr", sr); wireIn("a", a); wireIn("b", b); wireIn("c", c); wireIn("d", d)
            return CircuitPage("p_74194", "74194", comps, wires)
        }

        val engine = DigitalSimulationEngine()
        var mem = DigitalMemoryState()

        // Step 1: CLK=0, S1=1, S0=1 (Parallel Load armed), ABCD = 1010
        val (_, m1) = engine.step(build74194(0, 1, 1, 1, 0, 1, 0, 1, 0), mem, toggleClocks = false)
        mem = m1

        // Step 2: Rising edge CLK=1 -> QA=1, QB=0, QC=1, QD=0
        val (rLoad, m2) = engine.step(build74194(1, 1, 1, 1, 0, 1, 0, 1, 0), mem, toggleClocks = false)
        mem = m2
        assertEquals(LogicLevel.ONE, rLoad.pinLevels[NetlistBuilder.pinKey("u1", "qa")])
        assertEquals(LogicLevel.ZERO, rLoad.pinLevels[NetlistBuilder.pinKey("u1", "qb")])
        assertEquals(LogicLevel.ONE, rLoad.pinLevels[NetlistBuilder.pinKey("u1", "qc")])
        assertEquals(LogicLevel.ZERO, rLoad.pinLevels[NetlistBuilder.pinKey("u1", "qd")])

        // Step 3: CLK=0, then Rising edge Shift Right (S1=0, S0=1) with SR=0 -> 0101
        val (_, m3) = engine.step(build74194(0, 1, 0, 1, 0, 0, 0, 0, 0), mem, toggleClocks = false)
        val (rShift, m4) = engine.step(build74194(1, 1, 0, 1, 0, 0, 0, 0, 0), m3, toggleClocks = false)
        assertEquals(LogicLevel.ZERO, rShift.pinLevels[NetlistBuilder.pinKey("u1", "qa")])
        assertEquals(LogicLevel.ONE, rShift.pinLevels[NetlistBuilder.pinKey("u1", "qb")])
        assertEquals(LogicLevel.ZERO, rShift.pinLevels[NetlistBuilder.pinKey("u1", "qc")])
        assertEquals(LogicLevel.ONE, rShift.pinLevels[NetlistBuilder.pinKey("u1", "qd")])

        // Step 4: Async Clear (CLR'=0) -> all 0
        val (rClr, _) = engine.step(build74194(1, 0, 0, 0, 0, 0, 0, 0, 0), m4, toggleClocks = false)
        assertEquals(LogicLevel.ZERO, rClr.pinLevels[NetlistBuilder.pinKey("u1", "qb")])
        assertEquals(LogicLevel.ZERO, rClr.pinLevels[NetlistBuilder.pinKey("u1", "qd")])
    }

    @Test
    fun `LM741 Op-Amp MNA non-inverting amplifier solves accurate closed-loop gain`() {
        val tpl = CircuitTemplates.byId("opamp_741_amp")
        assertNotNull(tpl)
        val page = tpl!!.toNewProject().activePage()
        val analogRes = AnalogSimulationEngine().solveDcAndTransient(page)

        assertTrue("Expected MNA convergence for LM741 amplifier", analogRes.converged)
        val vOut = analogRes.pinVoltages[NetlistBuilder.pinKey("op1", "out")] ?: Double.NaN
        // Vin = 2V, Rf = 2k, Rg = 1k => Gain = 1 + 2k/1k = 3.0 => Vout ≈ 6.0V
        assertTrue("Expected Vout near 6.0V, got $vOut", abs(vOut - 6.0) < 0.2)
    }

    @Test
    fun `Safety FUSE detects overcurrent and RESISTOR flags overheat above 250mW`() {
        // 5V across 10 Ohm resistor in series with 250mA fuse => I ≈ 500mA (> 250mA), P_R ≈ 2.5W (> 0.25W)
        val page = CircuitPage(
            id = "p_fuse",
            name = "Overcurrent",
            components = listOf(
                ComponentInstance("v1", ComponentType.DC_SOURCE, "V1", 2f, 6f, value = "5V", properties = mapOf("voltage" to "5.0")),
                ComponentInstance("f1", ComponentType.FUSE, "F1", 8f, 4f, value = "250mA", properties = mapOf("ratingMa" to "250", "blown" to "false")),
                ComponentInstance("r1", ComponentType.RESISTOR, "R1", 14f, 6f, value = "10"),
                ComponentInstance("gnd", ComponentType.GROUND, "GND", 8f, 10f)
            ),
            wires = listOf(
                WireSegment("w1", WireEndpoint("v1", "pos"), WireEndpoint("f1", "p1")),
                WireSegment("w2", WireEndpoint("f1", "p2"), WireEndpoint("r1", "p1")),
                WireSegment("w3", WireEndpoint("r1", "p2"), WireEndpoint("gnd", "gnd")),
                WireSegment("w4", WireEndpoint("v1", "neg"), WireEndpoint("gnd", "gnd"))
            )
        )

        val res = AnalogSimulationEngine().solveDcAndTransient(page)
        assertTrue("Expected FUSE_BLOWN diagnostic", res.diagnostics.any { it.code == "FUSE_BLOWN" && it.componentId == "f1" })
        assertTrue("Expected RESISTOR_OVERHEAT diagnostic", res.diagnostics.any { it.code == "RESISTOR_OVERHEAT" && it.componentId == "r1" })
    }

    @Test
    fun `BooleanEngine Logic Converter synthesizes exact NAND-only circuit matching K-Map minterms`() {
        val vars = listOf("A", "B")
        val xorMinterms = listOf(1, 2) // A'B + AB'
        val sop = BooleanEngine.simplifyMinterms(vars, xorMinterms)
        assertTrue(sop.isNotBlank())

        val nandPage = BooleanEngine.mintermsToCircuitPage(vars, xorMinterms, nandOnly = true)
        val gates = nandPage.components.filter { it.type.name.startsWith("GATE_") }
        assertTrue("Expected at least one GATE_NAND", gates.isNotEmpty())
        assertTrue("Expected strictly NAND gates in NAND-Only mode", gates.all { it.type == ComponentType.GATE_NAND })

        // Verify truth table of synthesized NAND-only circuit matches XOR minterms (1, 2)
        val tt = DigitalSimulationEngine().generateTruthTable(nandPage)
        assertEquals(4, tt.rows.size)
        assertEquals(LogicLevel.ZERO, tt.rows[0].outputs.first()) // 00 -> 0
        assertEquals(LogicLevel.ONE, tt.rows[1].outputs.first())  // 01 -> 1
        assertEquals(LogicLevel.ONE, tt.rows[2].outputs.first())  // 10 -> 1
        assertEquals(LogicLevel.ZERO, tt.rows[3].outputs.first()) // 11 -> 0
    }

    @Test
    fun `WireSegment custom colorHex survives ProjectSerializer JSON round-trip`() {
        val page = CircuitPage(
            id = "p1",
            name = "Color Wire",
            components = listOf(
                ComponentInstance("a", ComponentType.LOGIC_INPUT, "A", 2f, 4f, value = "1"),
                ComponentInstance("p", ComponentType.PROBE_RED, "P", 10f, 4f)
            ),
            wires = listOf(
                WireSegment("w1", WireEndpoint("a", "out"), WireEndpoint("p", "in"), colorHex = 0xFFFF3B30L)
            )
        )
        val proj = CircuitProject("id_col", "ColorTest", "", 1L, 1L, listOf(page), "p1")
        val json = ProjectSerializer.toJson(proj)
        val restored = ProjectSerializer.fromJson(json)

        assertNotNull(restored)
        assertEquals(0xFFFF3B30L, restored!!.activePage().wires.first().colorHex)
    }

    @Test
    fun `AppStrings provides distinct translations for English, Urdu, Arabic, and Spanish`() {
        val en = AppStrings.forLanguage("English")
        val ur = AppStrings.forLanguage("Urdu")
        val ar = AppStrings.forLanguage("Arabic")
        val es = AppStrings.forLanguage("Spanish")

        assertEquals("Home", en.navHome)
        assertFalse(en.navHome == ur.navHome)
        assertFalse(en.navHome == ar.navHome)
        assertFalse(en.navHome == es.navHome)
    }

    @Test
    fun `ExportManager renders PNG and JSON report with new components and assignment metadata`() {
        val project = CircuitTemplates.byId("ic_7490_7447_lab")!!.toNewProject(999L)
        val dRes = DigitalSimulationEngine().evaluateCombinational(project.activePage())
        val aRes = AnalogSimulationEngine().solveDcAndTransient(project.activePage())

        val bmp = ExportManager.renderSchematicBitmap(
            page = project.activePage(),
            projectName = project.name,
            digitalResult = dRes,
            analogResult = aRes,
            options = PngExportOptions(widthPx = 800, heightPx = 500)
        )
        assertEquals(800, bmp.width)
        assertEquals(500, bmp.height)
        bmp.recycle()

        val opAmpProj = CircuitTemplates.byId("opamp_741_amp")!!.toNewProject(1000L)
        val opAmpBmp = ExportManager.renderSchematicBitmap(
            page = opAmpProj.activePage(),
            projectName = opAmpProj.name,
            analogResult = AnalogSimulationEngine().solveDcAndTransient(opAmpProj.activePage()),
            options = PngExportOptions(widthPx = 800, heightPx = 500)
        )
        assertEquals(800, opAmpBmp.width)
        opAmpBmp.recycle()

        val meta = PdfReportMetadata(
            studentName = "Ali Raza",
            rollNumber = "EE-2026-042",
            labNotes = "Verified BCD counter driving 7447 active-low 7-segment display."
        )
        assertEquals("Ali Raza", meta.studentName)
        assertEquals("EE-2026-042", meta.rollNumber)

        val json = ExportManager.buildStructuredJsonReport(project, dRes, aRes, includeSimulationTelemetry = true)
        assertTrue(json.contains("IC_7447"))
        assertTrue(json.contains("NET_LABEL"))
        assertTrue(json.contains("TEXT_NOTE"))
    }
}

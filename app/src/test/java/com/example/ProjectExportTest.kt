package com.example

import android.content.Context
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.CircuitTemplates
import com.example.data.ProjectSerializer
import com.example.engine.analog.AnalogSimulationEngine
import com.example.engine.digital.DigitalSimulationEngine
import com.example.export.ExportManager
import com.example.export.ExportTheme
import com.example.export.PngExportOptions
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProjectExportTest {

    @Test
    fun `structured json report contains metadata bom netlist simulation telemetry and re-importable project`() {
        val project = CircuitTemplates.byId("half_adder")!!.toNewProject(555L)
        val digitalRes = DigitalSimulationEngine().evaluateCombinational(project.activePage())
        val analogRes = AnalogSimulationEngine().solveDcAndTransient(project.activePage())

        val jsonStr = ExportManager.buildStructuredJsonReport(
            project = project,
            digitalResult = digitalRes,
            analogResult = analogRes,
            includeSimulationTelemetry = true
        )

        val root = JSONObject(jsonStr)
        assertEquals(ExportManager.REPORT_FORMAT, root.getString("reportFormat"))
        assertEquals("GateX Labs", root.getString("generator"))
        assertTrue(root.has("generatedAtIso"))

        val summary = root.getJSONObject("summary")
        assertEquals(project.name, summary.getString("projectName"))
        assertEquals(project.activePage().components.size, summary.getInt("componentCount"))
        assertEquals(project.activePage().wires.size, summary.getInt("wireCount"))
        assertTrue(summary.getInt("netCount") > 0)

        val bom = root.getJSONArray("billOfMaterials")
        assertEquals(project.activePage().components.size, bom.length())

        val nets = root.getJSONArray("electricalConnections")
        assertTrue(nets.length() > 0)

        val sim = root.getJSONObject("simulationReport")
        assertTrue(sim.has("digital"))
        assertTrue(sim.has("analog"))

        // Verify the structured JSON report can be cleanly re-imported into Salman Digital
        val restored = ProjectSerializer.fromJson(jsonStr)
        assertNotNull(restored)
        assertEquals(project.name, restored!!.name)
        assertEquals(project.activePage().components.size, restored.activePage().components.size)
        assertEquals(project.activePage().wires.size, restored.activePage().wires.size)
    }

    @Test
    fun `renderSchematicBitmap renders both Dark Lab and Light Blueprint themes`() {
        val project = CircuitTemplates.byId("voltage_divider")!!.toNewProject(101L)
        val analogRes = AnalogSimulationEngine().solveDcAndTransient(project.activePage())

        val darkBmp = ExportManager.renderSchematicBitmap(
            page = project.activePage(),
            projectName = project.name,
            analogResult = analogRes,
            options = PngExportOptions(widthPx = 800, heightPx = 500, theme = ExportTheme.DARK_LAB)
        )
        assertEquals(800, darkBmp.width)
        assertEquals(500, darkBmp.height)
        val darkBytes = java.io.ByteArrayOutputStream().use { out ->
            assertTrue(darkBmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out))
            out.toByteArray()
        }
        assertTrue(darkBytes.size > 50)
        darkBmp.recycle()

        val lightBmp = ExportManager.renderSchematicBitmap(
            page = project.activePage(),
            projectName = project.name,
            analogResult = analogRes,
            options = PngExportOptions(widthPx = 800, heightPx = 500, theme = ExportTheme.LIGHT_BLUEPRINT)
        )
        assertEquals(800, lightBmp.width)
        assertEquals(500, lightBmp.height)
        val lightBytes = java.io.ByteArrayOutputStream().use { out ->
            assertTrue(lightBmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out))
            out.toByteArray()
        }
        assertTrue(lightBytes.size > 50)
        lightBmp.recycle()
    }

    @Test
    fun `local archive and uri writers persist valid PNG and JSON report files`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val project = CircuitTemplates.byId("led_resistor")!!.toNewProject(202L)
        val analogRes = AnalogSimulationEngine().solveDcAndTransient(project.activePage())

        // Clean any prior exports in Robolectric sandbox
        ExportManager.listLocalExports(context).forEach { ExportManager.deleteLocalExport(it.file) }

        val pngArtifact = ExportManager.savePngToLocalArchive(context, project, null, analogRes)
        val jsonArtifact = ExportManager.saveJsonReportToLocalArchive(context, project, null, analogRes)

        assertTrue(pngArtifact.file.exists())
        assertTrue(pngArtifact.sizeBytes > 100)
        assertEquals("image/png", pngArtifact.mimeType)

        assertTrue(jsonArtifact.file.exists())
        assertTrue(jsonArtifact.sizeBytes > 100)
        assertEquals("application/json", jsonArtifact.mimeType)

        val listed = ExportManager.listLocalExports(context)
        assertEquals(2, listed.size)

        // Write via SAF-style Uri to temp files
        val outPng = File(context.cacheDir, "test_device_export.png")
        val outJson = File(context.cacheDir, "test_device_export.json")
        val pngWrite = ExportManager.writePngToUri(context, Uri.fromFile(outPng), project, null, analogRes)
        val jsonWrite = ExportManager.writeStructuredJsonReportToUri(context, Uri.fromFile(outJson), project, null, analogRes)

        assertTrue(pngWrite.isSuccess)
        assertTrue(jsonWrite.isSuccess)

        val pngBytes = outPng.readBytes()
        assertTrue(pngBytes.size > 8)
        // PNG magic header: 89 50 4E 47
        assertEquals(0x89.toByte(), pngBytes[0])
        assertEquals(0x50.toByte(), pngBytes[1])
        assertEquals(0x4E.toByte(), pngBytes[2])
        assertEquals(0x47.toByte(), pngBytes[3])

        val parsed = ProjectSerializer.fromJson(outJson.readText())
        assertNotNull(parsed)
        assertEquals(project.name, parsed!!.name)

        // Delete artifacts
        assertTrue(ExportManager.deleteLocalExport(pngArtifact.file))
        assertTrue(ExportManager.deleteLocalExport(jsonArtifact.file))
        assertFalse(pngArtifact.file.exists())
        assertEquals(0, ExportManager.listLocalExports(context).size)
    }
}

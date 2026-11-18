package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.ProjectSerializer
import com.example.engine.netlist.NetlistBuilder
import com.example.model.AnalogSimulationResult
import com.example.model.CircuitPage
import com.example.model.CircuitProject
import com.example.model.ComponentCatalog
import com.example.model.ComponentType
import com.example.model.DigitalSimulationResult
import com.example.model.LogicLevel
import com.example.model.endpointWorldPosition
import com.example.model.pinWorldPosition
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.min

enum class ExportTheme(val label: String) {
    DARK_LAB("Dark Lab"),
    LIGHT_BLUEPRINT("Light Blueprint")
}

data class PngExportOptions(
    val widthPx: Int = 1600,
    val heightPx: Int = 1000,
    val theme: ExportTheme = ExportTheme.DARK_LAB,
    val includeSimulationOverlay: Boolean = true,
    val includeGrid: Boolean = true,
    val includeTitleBlock: Boolean = true
)

data class ExportedArtifact(
    val file: File,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val modifiedAtMs: Long
)

data class PdfReportMetadata(
    val studentName: String = "",
    val rollNumber: String = "",
    val labNotes: String = ""
)

object ExportManager {
    const val REPORT_FORMAT = "salman_digital_project_report_v1"

    fun suggestedFileName(projectName: String, extension: String, suffix: String = ""): String {
        val safe = projectName.trim()
            .replace(Regex("[^a-zA-Z0-9_-]+"), "_")
            .trim('_')
            .ifEmpty { "gatex_labs_circuit" }
        val suffixPart = if (suffix.isNotEmpty()) "_$suffix" else ""
        return "${safe}${suffixPart}.$extension"
    }

    fun renderSchematicBitmap(
        page: CircuitPage,
        projectName: String = "GateX Labs",
        digitalResult: DigitalSimulationResult? = null,
        analogResult: AnalogSimulationResult? = null,
        options: PngExportOptions = PngExportOptions()
    ): Bitmap {
        val widthPx = options.widthPx.coerceAtLeast(320)
        val heightPx = options.heightPx.coerceAtLeast(240)
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val isDark = options.theme == ExportTheme.DARK_LAB
        val bgColor = if (isDark) Color.parseColor("#0B111E") else Color.parseColor("#F8FAFC")
        val gridColor = if (isDark) Color.parseColor("#18253C") else Color.parseColor("#DCE6F2")
        val wireDefaultColor = if (isDark) Color.parseColor("#00E5FF") else Color.parseColor("#0284C7")
        val boxFillColor = if (isDark) Color.parseColor("#192338") else Color.parseColor("#FFFFFF")
        val boxBorderColor = if (isDark) Color.parseColor("#FFB300") else Color.parseColor("#0F172A")
        val labelColor = if (isDark) Color.parseColor("#EAF2FF") else Color.parseColor("#0F172A")
        val valueColor = if (isDark) Color.parseColor("#94A7C6") else Color.parseColor("#475569")
        val pinColor = if (isDark) Color.parseColor("#00E5FF") else Color.parseColor("#0369A1")
        val junctionColor = if (isDark) Color.parseColor("#00E676") else Color.parseColor("#16A34A")
        val titleBgColor = if (isDark) Color.parseColor("#111B2E") else Color.parseColor("#E2E8F0")
        val titleBorderColor = if (isDark) Color.parseColor("#2A3B5C") else Color.parseColor("#94A3B8")

        canvas.drawColor(bgColor)

        val titleBarHeight = if (options.includeTitleBlock) 72f else 0f
        val drawableHeight = heightPx - titleBarHeight

        if (options.includeGrid) {
            val gridPaint = Paint().apply {
                color = gridColor
                strokeWidth = 1.5f
            }
            val step = 40f
            var gx = 0f
            while (gx < widthPx) {
                canvas.drawLine(gx, 0f, gx, drawableHeight, gridPaint)
                gx += step
            }
            var gy = 0f
            while (gy < drawableHeight) {
                canvas.drawLine(0f, gy, widthPx.toFloat(), gy, gridPaint)
                gy += step
            }
        }

        val compsById = page.components.associateBy { it.id }
        val juncsById = page.junctions.associateBy { it.id }

        // Compute world bounding box to cleanly center and scale any schematic
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY

        for (comp in page.components) {
            val meta = ComponentCatalog.metadataFor(comp)
            val hw = meta.widthGrid / 2f + 1.2f
            val hh = meta.heightGrid / 2f + 1.2f
            minX = min(minX, comp.x - hw)
            minY = min(minY, comp.y - hh)
            maxX = max(maxX, comp.x + hw)
            maxY = max(maxY, comp.y + hh)
        }
        for (j in page.junctions) {
            minX = min(minX, j.x - 1f)
            minY = min(minY, j.y - 1f)
            maxX = max(maxX, j.x + 1f)
            maxY = max(maxY, j.y + 1f)
        }

        if (!minX.isFinite()) {
            minX = 0f
            minY = 0f
            maxX = 20f
            maxY = 12f
        }

        val worldW = max(8f, maxX - minX)
        val worldH = max(6f, maxY - minY)
        val pad = 56f
        val availW = max(120f, widthPx - pad * 2f)
        val availH = max(120f, drawableHeight - pad * 2f)
        val scale = min(availW / worldW, availH / worldH).coerceIn(22f, 68f)

        val contentW = worldW * scale
        val contentH = worldH * scale
        val offsetX = (widthPx - contentW) / 2f - minX * scale
        val offsetY = (drawableHeight - contentH) / 2f - minY * scale

        val netGraph = NetlistBuilder.build(page)

        val wirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = wireDefaultColor
            strokeWidth = max(3f, scale * 0.085f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            style = Paint.Style.STROKE
        }

        for (wire in page.wires) {
            val from = endpointWorldPosition(wire.from, compsById, juncsById, ComponentCatalog::get) ?: continue
            val to = endpointWorldPosition(wire.to, compsById, juncsById, ComponentCatalog::get) ?: continue

            val logicLevel = if (options.includeSimulationOverlay) {
                digitalResult?.wireLevels?.get(wire.id)
            } else null

            val customWireColor = wire.colorHex?.toInt()
            wirePaint.color = when (logicLevel) {
                LogicLevel.ONE -> if (isDark) Color.parseColor("#00E676") else Color.parseColor("#16A34A")
                LogicLevel.ZERO -> customWireColor ?: if (isDark) Color.parseColor("#64748B") else Color.parseColor("#94A3B8")
                LogicLevel.CONFLICT -> Color.parseColor("#FF5252")
                LogicLevel.HIGH_Z -> Color.parseColor("#B388FF")
                else -> customWireColor ?: wireDefaultColor
            }

            if (wire.waypoints.isNotEmpty()) {
                var prevX = offsetX + from.x * scale
                var prevY = offsetY + from.y * scale
                for (wp in wire.waypoints) {
                    val nx = offsetX + wp.x * scale
                    val ny = offsetY + wp.y * scale
                    canvas.drawLine(prevX, prevY, nx, ny, wirePaint)
                    prevX = nx
                    prevY = ny
                }
                canvas.drawLine(prevX, prevY, offsetX + to.x * scale, offsetY + to.y * scale, wirePaint)
            } else {
                val midX = (from.x + to.x) / 2f
                canvas.drawLine(
                    offsetX + from.x * scale,
                    offsetY + from.y * scale,
                    offsetX + midX * scale,
                    offsetY + from.y * scale,
                    wirePaint
                )
                canvas.drawLine(
                    offsetX + midX * scale,
                    offsetY + from.y * scale,
                    offsetX + midX * scale,
                    offsetY + to.y * scale,
                    wirePaint
                )
                canvas.drawLine(
                    offsetX + midX * scale,
                    offsetY + to.y * scale,
                    offsetX + to.x * scale,
                    offsetY + to.y * scale,
                    wirePaint
                )
            }
        }

        val juncPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = junctionColor
            style = Paint.Style.FILL
        }
        for (j in page.junctions) {
            canvas.drawCircle(offsetX + j.x * scale, offsetY + j.y * scale, max(5f, scale * 0.14f), juncPaint)
        }

        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = boxFillColor
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = boxBorderColor
            style = Paint.Style.STROKE
            strokeWidth = max(2.2f, scale * 0.06f)
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor
            textSize = (scale * 0.48f).coerceIn(15f, 26f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = valueColor
            textSize = (scale * 0.36f).coerceIn(12f, 20f)
            textAlign = Paint.Align.CENTER
        }
        val telemetryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) Color.parseColor("#00E676") else Color.parseColor("#15803D")
            textSize = (scale * 0.32f).coerceIn(11f, 17f)
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }
        val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pinColor
            style = Paint.Style.FILL
        }

        val pinTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor
            textSize = (scale * 0.28f).coerceIn(10f, 16f)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val pinNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = valueColor
            textSize = (scale * 0.25f).coerceIn(9f, 14f)
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }

        for (comp in page.components) {
            val meta = ComponentCatalog.metadataFor(comp)
            val cx = offsetX + comp.x * scale
            val cy = offsetY + comp.y * scale
            val isVert = comp.rotationDegrees % 180 != 0
            val gridW = if (isVert) meta.heightGrid else meta.widthGrid
            val gridH = if (isVert) meta.widthGrid else meta.heightGrid
            val hw = (gridW * scale) / 2f
            val hh = (gridH * scale) / 2f
            val isDip = meta.pins.any { it.pinNumber != null } && comp.type != ComponentType.OP_AMP_741
            val isProbe = comp.type == ComponentType.PROBE_RED || comp.type == ComponentType.PROBE_GREEN ||
                comp.type == ComponentType.PROBE_YELLOW || comp.type == ComponentType.PROBE_BLUE ||
                comp.type == ComponentType.PROBE_ORANGE || comp.type == ComponentType.PROBE_CYAN

            if (isProbe) {
                val pCol = when (comp.type) {
                    ComponentType.PROBE_RED -> Color.parseColor("#FF2E3B")
                    ComponentType.PROBE_GREEN -> Color.parseColor("#00F076")
                    ComponentType.PROBE_YELLOW -> Color.parseColor("#FFEA00")
                    ComponentType.PROBE_BLUE -> Color.parseColor("#2979FF")
                    ComponentType.PROBE_ORANGE -> Color.parseColor("#FF7A00")
                    else -> Color.parseColor("#00E5FF")
                }
                val lit = options.includeSimulationOverlay &&
                    (digitalResult?.pinLevels?.get(NetlistBuilder.pinKey(comp.id, "in")) == LogicLevel.ONE)
                val domeY = cy - hh * 0.28f
                val rad = min(hw, hh) * 0.72f
                val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = pCol
                    alpha = if (lit) 235 else 75
                    style = Paint.Style.FILL
                }
                val rimP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = pCol
                    strokeWidth = max(2.2f, scale * 0.06f)
                    style = Paint.Style.STROKE
                }
                canvas.drawLine(cx, domeY + rad, cx, cy + hh, borderPaint)
                canvas.drawCircle(cx, domeY, rad, fillP)
                canvas.drawCircle(cx, domeY, rad, rimP)
                canvas.drawText(comp.label, cx, domeY - rad - 6f, subPaint)
            } else if (comp.type == ComponentType.OP_AMP_741) {
                val triPath = android.graphics.Path().apply {
                    moveTo(cx - hw * 0.75f, cy - hh * 0.72f)
                    lineTo(cx + hw * 0.75f, cy)
                    lineTo(cx - hw * 0.75f, cy + hh * 0.72f)
                    close()
                }
                canvas.drawPath(triPath, boxPaint)
                canvas.drawPath(triPath, borderPaint)
                canvas.drawText("+", cx - hw * 0.48f, cy - hh * 0.25f, textPaint)
                canvas.drawText("−", cx - hw * 0.48f, cy + hh * 0.42f, textPaint)
                canvas.drawText("741", cx + hw * 0.05f, cy + textPaint.textSize * 0.35f, subPaint)
                canvas.drawText(comp.label, cx, cy - hh * 0.80f, subPaint)
            } else if (comp.type == ComponentType.FUSE) {
                val blown = comp.properties["blown"] == "true"
                val fuseRect = RectF(cx - hw * 0.65f, cy - hh * 0.42f, cx + hw * 0.65f, cy + hh * 0.42f)
                canvas.drawRoundRect(fuseRect, 6f, 6f, boxPaint)
                val fBorder = Paint(borderPaint).apply {
                    color = if (blown) Color.parseColor("#FF5252") else boxBorderColor
                }
                canvas.drawRoundRect(fuseRect, 6f, 6f, fBorder)
                canvas.drawLine(cx - hw, cy, cx - hw * 0.65f, cy, fBorder)
                canvas.drawLine(cx + hw * 0.65f, cy, cx + hw, cy, fBorder)
                if (blown) {
                    canvas.drawLine(cx - hw * 0.55f, cy, cx - hw * 0.12f, cy - hh * 0.22f, fBorder)
                    canvas.drawLine(cx + hw * 0.12f, cy + hh * 0.22f, cx + hw * 0.55f, cy, fBorder)
                    canvas.drawText("BLOWN", cx, cy - hh * 0.55f, fBorder.apply {
                        style = Paint.Style.FILL
                        textSize = subPaint.textSize
                        textAlign = Paint.Align.CENTER
                    })
                } else {
                    canvas.drawLine(cx - hw * 0.55f, cy, cx + hw * 0.55f, cy, fBorder)
                    canvas.drawText(comp.value.ifEmpty { "250mA" }, cx, cy - hh * 0.55f, subPaint)
                }
            } else if (comp.type == ComponentType.NET_LABEL) {
                val tagPath = android.graphics.Path().apply {
                    moveTo(cx - hw, cy)
                    lineTo(cx - hw * 0.55f, cy - hh * 0.55f)
                    lineTo(cx + hw * 0.92f, cy - hh * 0.55f)
                    lineTo(cx + hw * 0.92f, cy + hh * 0.55f)
                    lineTo(cx - hw * 0.55f, cy + hh * 0.55f)
                    close()
                }
                canvas.drawPath(tagPath, boxPaint)
                canvas.drawPath(tagPath, borderPaint)
                canvas.drawText(comp.value.ifEmpty { "NET" }, cx + hw * 0.18f, cy + subPaint.textSize * 0.35f, textPaint)
            } else if (comp.type == ComponentType.TEXT_NOTE) {
                val noteRect = RectF(cx - hw, cy - hh, cx + hw, cy + hh)
                val noteFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isDark) Color.parseColor("#2A2416") else Color.parseColor("#FEF9C3")
                    style = Paint.Style.FILL
                }
                val noteBorder = Paint(borderPaint).apply {
                    color = Color.parseColor("#EAB308")
                }
                canvas.drawRoundRect(noteRect, 10f, 10f, noteFill)
                canvas.drawRoundRect(noteRect, 10f, 10f, noteBorder)
                canvas.drawText(comp.value.ifEmpty { comp.label }, cx, cy + subPaint.textSize * 0.35f, textPaint)
            } else if (isDip) {
                val bodyHw = hw * 0.62f
                val bodyHh = hh * 0.90f
                val rect = RectF(cx - bodyHw, cy - bodyHh, cx + bodyHw, cy + bodyHh)
                canvas.drawRoundRect(rect, 6f, 6f, boxPaint)
                canvas.drawRoundRect(rect, 6f, 6f, borderPaint)
                canvas.drawText(comp.label, cx, cy - bodyHh - 8f, textPaint)
                canvas.drawText(meta.shortCode, cx, cy + bodyHh + textPaint.textSize + 4f, textPaint)
                for (pin in meta.pins) {
                    val pt = pinWorldPosition(comp, pin)
                    val px = offsetX + pt.x * scale
                    val py = offsetY + pt.y * scale
                    val isLeft = px < cx
                    val edgeX = if (isLeft) cx - bodyHw else cx + bodyHw
                    canvas.drawLine(px, py, edgeX, py, borderPaint)
                    pin.pinNumber?.let { num ->
                        canvas.drawText(num.toString(), (px + edgeX) / 2f, py - 4f, pinNumPaint)
                    }
                    pinTextPaint.textAlign = if (isLeft) Paint.Align.LEFT else Paint.Align.RIGHT
                    val inset = max(6f, scale * 0.2f)
                    val tx = if (isLeft) edgeX + inset else edgeX - inset
                    canvas.drawText(pin.label, tx, py + pinTextPaint.textSize * 0.35f, pinTextPaint)
                }
            } else {
                val rect = RectF(cx - hw, cy - hh, cx + hw, cy + hh)
                canvas.drawRoundRect(rect, 12f, 12f, boxPaint)
                canvas.drawRoundRect(rect, 12f, 12f, borderPaint)

                canvas.drawText(comp.label, cx, cy - 3f, textPaint)
                canvas.drawText(comp.value.ifEmpty { meta.shortCode }, cx, cy + textPaint.textSize * 0.85f, subPaint)
            }

            if (options.includeSimulationOverlay) {
                val current = analogResult?.componentCurrents?.get(comp.id)
                if (current != null) {
                    canvas.drawText(
                        ComponentCatalog.formatEngineeringValue(current, "A"),
                        cx,
                        cy + hh + telemetryPaint.textSize + 2f,
                        telemetryPaint
                    )
                }
            }

            for (pin in meta.pins) {
                val pt = pinWorldPosition(comp, pin)
                val px = offsetX + pt.x * scale
                val py = offsetY + pt.y * scale
                val pinKey = NetlistBuilder.pinKey(comp.id, pin.id)
                val pinLogic = if (options.includeSimulationOverlay) digitalResult?.pinLevels?.get(pinKey) else null
                pinPaint.color = when (pinLogic) {
                    LogicLevel.ONE -> Color.parseColor("#00E676")
                    LogicLevel.ZERO -> Color.parseColor("#64748B")
                    LogicLevel.CONFLICT -> Color.parseColor("#FF5252")
                    else -> pinColor
                }
                canvas.drawCircle(px, py, max(4.5f, scale * 0.12f), pinPaint)
            }
        }

        if (options.includeTitleBlock) {
            val barTop = heightPx - titleBarHeight
            val barPaint = Paint().apply { color = titleBgColor; style = Paint.Style.FILL }
            val barLinePaint = Paint().apply { color = titleBorderColor; strokeWidth = 2f }
            canvas.drawRect(0f, barTop, widthPx.toFloat(), heightPx.toFloat(), barPaint)
            canvas.drawLine(0f, barTop, widthPx.toFloat(), barTop, barLinePaint)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = labelColor
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = valueColor
                textSize = 18f
                typeface = Typeface.MONOSPACE
            }
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
            canvas.drawText("GATEX LABS  |  $projectName", 28f, barTop + 30f, titlePaint)
            canvas.drawText(
                "Page: ${page.name}   •   Parts: ${page.components.size}   •   Wires: ${page.wires.size}   •   Nets: ${netGraph.nets.size}   •   $dateStr",
                28f,
                barTop + 56f,
                metaPaint
            )
        }

        return bmp
    }

    fun buildStructuredJsonReport(
        project: CircuitProject,
        digitalResult: DigitalSimulationResult?,
        analogResult: AnalogSimulationResult?,
        includeSimulationTelemetry: Boolean = true
    ): String {
        val activePage = project.activePage()
        val netGraph = NetlistBuilder.build(activePage)
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val root = JSONObject()
        root.put("reportFormat", REPORT_FORMAT)
        root.put("generator", "GateX Labs")
        root.put("generatedAtIso", isoFormat.format(Date()))

        val summary = JSONObject().apply {
            put("projectId", project.id)
            put("projectName", project.name)
            put("description", project.description)
            put("pageCount", project.pages.size)
            put("activePageId", activePage.id)
            put("activePageName", activePage.name)
            put("componentCount", activePage.components.size)
            put("wireCount", activePage.wires.size)
            put("junctionCount", activePage.junctions.size)
            put("netCount", netGraph.nets.size)
            put("breadboardPlacements", project.breadboard.placements.size)
            put("pcbFootprints", project.pcb.footprints.size)
        }
        root.put("summary", summary)

        val bomArr = JSONArray()
        for (comp in activePage.components) {
            val meta = ComponentCatalog.get(comp.type)
            val propsObj = JSONObject()
            comp.properties.forEach { (k, v) -> propsObj.put(k, v) }
            bomArr.put(
                JSONObject().apply {
                    put("id", comp.id)
                    put("label", comp.label)
                    put("type", comp.type.name)
                    put("displayName", meta.displayName)
                    put("category", meta.category.title)
                    put("value", comp.value.ifEmpty { meta.defaultValue })
                    put("rotationDegrees", comp.rotationDegrees)
                    put("flipped", comp.flipped)
                    put("x", comp.x.toDouble())
                    put("y", comp.y.toDouble())
                    put("properties", propsObj)
                }
            )
        }
        root.put("billOfMaterials", bomArr)

        val netsArr = JSONArray()
        for (net in netGraph.nets) {
            netsArr.put(
                JSONObject().apply {
                    put("netId", net.id)
                    put("pins", JSONArray(net.pinKeys.toList()))
                    put("junctions", JSONArray(net.junctionIds.toList()))
                    put("wires", JSONArray(net.wireIds.toList()))
                }
            )
        }
        root.put("electricalConnections", netsArr)

        if (includeSimulationTelemetry) {
            val simObj = JSONObject()

            val digitalObj = JSONObject().apply {
                val res = digitalResult
                put("tickCount", res?.tickCount ?: 0)
                put("hasShortCircuit", res?.hasShortCircuit ?: false)
                val pinLevelsObj = JSONObject()
                res?.pinLevels?.forEach { (pinKey, level) -> pinLevelsObj.put(pinKey, level.symbol) }
                put("pinLevels", pinLevelsObj)
                val diagArr = JSONArray()
                res?.diagnostics?.forEach { d ->
                    diagArr.put(
                        JSONObject().apply {
                            put("severity", d.severity.name)
                            put("code", d.code)
                            put("message", d.message)
                            d.componentId?.let { put("componentId", it) }
                        }
                    )
                }
                put("diagnostics", diagArr)
            }
            simObj.put("digital", digitalObj)

            val analogObj = JSONObject().apply {
                val res = analogResult
                put("converged", res?.converged ?: true)
                res?.equivalentResistanceOhms?.let { put("equivalentResistanceOhms", it) }
                res?.acSweepSummary?.let { put("acSweepSummary", it) }
                val voltagesObj = JSONObject()
                res?.nodeVoltages?.forEach { (nodeId, v) -> voltagesObj.put(nodeId, v) }
                put("nodeVoltages", voltagesObj)
                val currentsObj = JSONObject()
                res?.componentCurrents?.forEach { (compId, current) -> currentsObj.put(compId, current) }
                put("componentCurrents", currentsObj)
                val powersObj = JSONObject()
                res?.componentPowers?.forEach { (compId, p) -> powersObj.put(compId, p) }
                put("componentPowers", powersObj)
                val diagArr = JSONArray()
                res?.diagnostics?.forEach { d ->
                    diagArr.put(
                        JSONObject().apply {
                            put("severity", d.severity.name)
                            put("code", d.code)
                            put("message", d.message)
                            d.componentId?.let { put("componentId", it) }
                        }
                    )
                }
                put("diagnostics", diagArr)
            }
            simObj.put("analog", analogObj)
            root.put("simulationReport", simObj)
        }

        // Embed full serializable project so exported reports can also be re-imported cleanly
        root.put("project", JSONObject(ProjectSerializer.toJson(project)))
        return root.toString(2)
    }

    fun writePngToUri(
        context: Context,
        uri: Uri,
        project: CircuitProject,
        digitalResult: DigitalSimulationResult?,
        analogResult: AnalogSimulationResult?,
        options: PngExportOptions = PngExportOptions()
    ): Result<Long> = runCatching {
        val bmp = renderSchematicBitmap(
            page = project.activePage(),
            projectName = project.name,
            digitalResult = digitalResult,
            analogResult = analogResult,
            options = options
        )
        val baos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val bytes = baos.toByteArray()
        bmp.recycle()
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(bytes)
            out.flush()
        } ?: error("Could not open destination output stream.")
        bytes.size.toLong()
    }

    fun writeStructuredJsonReportToUri(
        context: Context,
        uri: Uri,
        project: CircuitProject,
        digitalResult: DigitalSimulationResult?,
        analogResult: AnalogSimulationResult?,
        includeSimulationTelemetry: Boolean = true
    ): Result<Long> = runCatching {
        val json = buildStructuredJsonReport(project, digitalResult, analogResult, includeSimulationTelemetry)
        val bytes = json.toByteArray(Charsets.UTF_8)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(bytes)
            out.flush()
        } ?: error("Could not open destination output stream.")
        bytes.size.toLong()
    }

    fun writePdfReportToUri(
        context: Context,
        uri: Uri,
        project: CircuitProject,
        digitalResult: DigitalSimulationResult?,
        analogResult: AnalogSimulationResult?,
        reportMeta: PdfReportMetadata = PdfReportMetadata()
    ): Result<Long> = runCatching {
        val baos = ByteArrayOutputStream()
        writePdfDocumentToStream(project, digitalResult, analogResult, baos, reportMeta)
        val bytes = baos.toByteArray()
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(bytes)
            out.flush()
        } ?: error("Could not open destination output stream.")
        bytes.size.toLong()
    }

    private fun localArchiveDir(context: Context): File =
        File(context.filesDir, "exports").apply { mkdirs() }

    fun savePngToLocalArchive(
        context: Context,
        project: CircuitProject,
        digitalResult: DigitalSimulationResult?,
        analogResult: AnalogSimulationResult?,
        options: PngExportOptions = PngExportOptions()
    ): ExportedArtifact {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = suggestedFileName(project.name, "png", "schematic_$stamp")
        val file = File(localArchiveDir(context), fileName)
        val bmp = renderSchematicBitmap(
            page = project.activePage(),
            projectName = project.name,
            digitalResult = digitalResult,
            analogResult = analogResult,
            options = options
        )
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bmp.recycle()
        return toArtifact(file)
    }

    fun saveJsonReportToLocalArchive(
        context: Context,
        project: CircuitProject,
        digitalResult: DigitalSimulationResult?,
        analogResult: AnalogSimulationResult?,
        includeSimulationTelemetry: Boolean = true
    ): ExportedArtifact {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = suggestedFileName(project.name, "json", "report_$stamp")
        val file = File(localArchiveDir(context), fileName)
        file.writeText(buildStructuredJsonReport(project, digitalResult, analogResult, includeSimulationTelemetry))
        return toArtifact(file)
    }

    fun listLocalExports(context: Context): List<ExportedArtifact> {
        val dir = localArchiveDir(context)
        val files = dir.listFiles()?.filter { it.isFile } ?: emptyList()
        return files.sortedByDescending { it.lastModified() }.map(::toArtifact)
    }

    fun deleteLocalExport(file: File): Boolean = file.exists() && file.delete()

    private fun toArtifact(file: File): ExportedArtifact {
        val mime = when {
            file.name.endsWith(".png", ignoreCase = true) -> "image/png"
            file.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            else -> "application/json"
        }
        return ExportedArtifact(
            file = file,
            name = file.name,
            mimeType = mime,
            sizeBytes = file.length(),
            modifiedAtMs = file.lastModified()
        )
    }

    fun exportPng(
        context: Context,
        project: CircuitProject,
        digitalResult: DigitalSimulationResult? = null,
        analogResult: AnalogSimulationResult? = null,
        options: PngExportOptions = PngExportOptions()
    ): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, suggestedFileName(project.name, "png", "schematic"))
        val bmp = renderSchematicBitmap(
            page = project.activePage(),
            projectName = project.name,
            digitalResult = digitalResult,
            analogResult = analogResult,
            options = options
        )
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bmp.recycle()
        return file
    }

    fun exportPdfReport(
        context: Context,
        project: CircuitProject,
        digitalResult: DigitalSimulationResult?,
        analogResult: AnalogSimulationResult?,
        reportMeta: PdfReportMetadata = PdfReportMetadata()
    ): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, suggestedFileName(project.name, "pdf", "report"))
        FileOutputStream(file).use { out ->
            writePdfDocumentToStream(project, digitalResult, analogResult, out, reportMeta)
        }
        return file
    }

    private fun writePdfDocumentToStream(
        project: CircuitProject,
        digitalResult: DigitalSimulationResult?,
        analogResult: AnalogSimulationResult?,
        outputStream: java.io.OutputStream,
        reportMeta: PdfReportMetadata = PdfReportMetadata()
    ) {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val pdfPage = pdf.startPage(pageInfo)
        val c = pdfPage.canvas

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B111E")
            textSize = 19f
            isFakeBoldText = true
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 10.5f
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 9.5f
        }
        val boxStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        val boxFill = Paint().apply {
            color = Color.parseColor("#F1F5F9")
            style = Paint.Style.FILL
        }

        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        c.drawText("GATEX LABS — Engineering Lab Report", 32f, 36f, titlePaint)
        c.drawText("Project: ${project.name}  |  Generated: $dateStr", 32f, 52f, mutedPaint)

        var headerEndY = 68f
        val hasStudentMeta = reportMeta.studentName.isNotBlank() ||
            reportMeta.rollNumber.isNotBlank() ||
            reportMeta.labNotes.isNotBlank()
        if (hasStudentMeta) {
            val blockHeight = if (reportMeta.labNotes.isNotBlank()) 44f else 26f
            val blockRect = RectF(32f, 60f, 563f, 60f + blockHeight)
            c.drawRoundRect(blockRect, 6f, 6f, boxFill)
            c.drawRoundRect(blockRect, 6f, 6f, boxStroke)
            val studentLine = buildString {
                if (reportMeta.studentName.isNotBlank()) append("Student: ${reportMeta.studentName.trim()}")
                if (reportMeta.rollNumber.isNotBlank()) {
                    if (isNotEmpty()) append("   |   ")
                    append("Roll / ID: ${reportMeta.rollNumber.trim()}")
                }
            }.ifEmpty { "Lab Assignment Submission" }
            c.drawText(studentLine, 42f, 76f, bodyPaint)
            if (reportMeta.labNotes.isNotBlank()) {
                c.drawText("Notes: ${reportMeta.labNotes.trim().take(88)}", 42f, 94f, mutedPaint)
            }
            headerEndY = 60f + blockHeight + 14f
        }

        c.drawText(
            "Pages: ${project.pages.size}  |  Components: ${project.activePage().components.size}  |  Wires: ${project.activePage().wires.size}",
            32f,
            headerEndY,
            bodyPaint
        )

        val schematicTop = headerEndY + 12f
        val bmp = renderSchematicBitmap(
            page = project.activePage(),
            projectName = project.name,
            digitalResult = digitalResult,
            analogResult = analogResult,
            options = PngExportOptions(
                widthPx = 1060,
                heightPx = 540,
                theme = ExportTheme.LIGHT_BLUEPRINT,
                includeTitleBlock = false
            )
        )
        val scaled = Bitmap.createScaledBitmap(bmp, 530, 260, true)
        c.drawBitmap(scaled, 32f, schematicTop, null)
        c.drawRect(32f, schematicTop, 562f, schematicTop + 260f, boxStroke)
        bmp.recycle()
        scaled.recycle()

        var y = schematicTop + 282f
        titlePaint.textSize = 13.5f
        c.drawText("Bill of Materials (Active Page: ${project.activePage().name})", 32f, y, titlePaint)
        y += 16f
        val maxBomRows = if (hasStudentMeta) 14 else 16
        for (comp in project.activePage().components.take(maxBomRows)) {
            if (y > 760f) break
            val meta = ComponentCatalog.metadataFor(comp)
            val currentStr = analogResult?.componentCurrents?.get(comp.id)?.let {
                " | I = ${ComponentCatalog.formatEngineeringValue(it, "A")}"
            } ?: ""
            c.drawText(
                "• ${comp.label} (${meta.displayName}) — ${comp.value.ifEmpty { meta.defaultValue }}$currentStr",
                36f,
                y,
                bodyPaint
            )
            y += 14f
        }

        if (analogResult != null && analogResult.nodeVoltages.isNotEmpty() && y < 780f) {
            y += 8f
            c.drawText("Analog Node Voltages", 32f, y, titlePaint)
            y += 15f
            val summaryLine = analogResult.nodeVoltages.entries.take(6).joinToString("   •   ") { (k, v) ->
                "$k: ${String.format(Locale.US, "%.2fV", v)}"
            }
            c.drawText(summaryLine, 36f, y, bodyPaint)
            y += 14f
        }

        if (digitalResult != null && digitalResult.pinLevels.isNotEmpty() && y < 805f) {
            y += 8f
            c.drawText("Digital Simulation Summary (Tick ${digitalResult.tickCount})", 32f, y, titlePaint)
            y += 15f
            c.drawText("Short-circuit detected: ${digitalResult.hasShortCircuit}", 36f, y, bodyPaint)
        }

        pdf.finishPage(pdfPage)
        pdf.writeTo(outputStream)
        pdf.close()
    }

    fun exportProjectJson(
        context: Context,
        project: CircuitProject,
        digitalResult: DigitalSimulationResult? = null,
        analogResult: AnalogSimulationResult? = null,
        includeSimulationTelemetry: Boolean = true
    ): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, suggestedFileName(project.name, "json", "report"))
        file.writeText(buildStructuredJsonReport(project, digitalResult, analogResult, includeSimulationTelemetry))
        return file
    }

    fun importProjectFromUri(context: Context, uri: Uri): CircuitProject? {
        return try {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return null
            ProjectSerializer.fromJson(text)
        } catch (_: Exception) {
            null
        }
    }

    fun exportGatexProjectFile(context: Context, project: CircuitProject): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, suggestedFileName(project.name, "gatex.json"))
        file.writeText(ProjectSerializer.toJson(project))
        return file
    }

    fun exportBomCsv(
        context: Context,
        project: CircuitProject,
        analogResult: AnalogSimulationResult? = null
    ): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, suggestedFileName(project.name, "csv", "bom"))
        val report = com.example.tools.BomCalculator.generate(project.activePage(), analogResult)
        file.writeText(com.example.tools.BomCalculator.toCsv(project.name, report))
        return file
    }

    fun exportOscilloscopeCsv(
        context: Context,
        project: CircuitProject,
        analogResult: AnalogSimulationResult
    ): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, suggestedFileName(project.name, "csv", "oscilloscope"))
        val samples = analogResult.transientSamples
        val channels = samples.firstOrNull()?.channelValues?.keys?.sorted().orEmpty()
        val sb = StringBuilder()
        sb.append("SampleIndex,TimeSec")
        channels.forEach { sb.append(",\"${it.replace("\"", "\"\"")}\"") }
        sb.appendLine()
        samples.forEachIndexed { i, pt ->
            sb.append(String.format(Locale.US, "%d,%.6f", i, pt.timeSec))
            channels.forEach { ch ->
                sb.append(String.format(Locale.US, ",%.4f", pt.channelValues[ch] ?: 0.0))
            }
            sb.appendLine()
        }
        file.writeText(sb.toString())
        return file
    }

    fun exportLogicAnalyzerCsv(
        context: Context,
        project: CircuitProject,
        digitalResult: DigitalSimulationResult
    ): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, suggestedFileName(project.name, "csv", "logic_analyzer"))
        val history = digitalResult.waveformHistory
        val keys = history.firstOrNull()?.nodeLevels?.keys?.sorted().orEmpty()
        val sb = StringBuilder()
        sb.append("Tick")
        keys.forEach { sb.append(",\"${it.replace("\"", "\"\"")}\"") }
        sb.appendLine()
        history.forEach { sample ->
            sb.append(sample.tick)
            keys.forEach { k ->
                val lvl = sample.nodeLevels[k] ?: LogicLevel.UNKNOWN
                sb.append(",${lvl.symbol}")
            }
            sb.appendLine()
        }
        file.writeText(sb.toString())
        return file
    }

    fun exportWaveformPng(
        context: Context,
        project: CircuitProject,
        isAnalog: Boolean,
        analogResult: AnalogSimulationResult,
        digitalResult: DigitalSimulationResult
    ): File {
        val w = 1400
        val h = 760
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.parseColor("#08101E"))

        val gridPaint = Paint().apply {
            color = Color.parseColor("#1A2A44")
            strokeWidth = 1.5f
        }
        for (x in 0..w step 70) canvas.drawLine(x.toFloat(), 60f, x.toFloat(), h - 40f, gridPaint)
        for (y in 60..(h - 40) step 60) canvas.drawLine(0f, y.toFloat(), w.toFloat(), y.toFloat(), gridPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5FF")
            textSize = 26f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText(
            if (isAnalog) "GateX Labs Oscilloscope — ${project.name}" else "GateX Labs Logic Analyzer — ${project.name}",
            28f,
            40f,
            titlePaint
        )

        if (isAnalog) {
            val samples = analogResult.transientSamples
            val channels = samples.firstOrNull()?.channelValues?.keys?.toList().orEmpty().take(3)
            val colors = listOf(Color.parseColor("#00E5FF"), Color.parseColor("#FFB300"), Color.parseColor("#00E676"))
            val maxV = max(5.0, samples.flatMap { it.channelValues.values }.maxOfOrNull { kotlin.math.abs(it) } ?: 5.0)
            val midY = h / 2f
            val ampPx = (h - 160f) / 2.2f
            channels.forEachIndexed { chIdx, chName ->
                if (samples.size < 2) return@forEachIndexed
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colors[chIdx % colors.size]
                    strokeWidth = 4f
                    style = Paint.Style.STROKE
                }
                for (i in 1 until samples.size) {
                    val v1 = samples[i - 1].channelValues[chName] ?: 0.0
                    val v2 = samples[i].channelValues[chName] ?: 0.0
                    val x1 = 40f + ((i - 1).toFloat() / (samples.size - 1)) * (w - 80f)
                    val y1 = midY - ((v1 / maxV) * ampPx).toFloat()
                    val x2 = 40f + (i.toFloat() / (samples.size - 1)) * (w - 80f)
                    val y2 = midY - ((v2 / maxV) * ampPx).toFloat()
                    canvas.drawLine(x1, y1, x2, y2, p)
                }
            }
        } else {
            val history = digitalResult.waveformHistory
            val channels = history.firstOrNull()?.nodeLevels?.keys?.toList().orEmpty().take(6)
            val rowH = if (channels.isEmpty()) 100f else (h - 120f) / channels.size
            val lblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#EAF2FF")
                textSize = 18f
                typeface = Typeface.MONOSPACE
            }
            val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#00E676")
                strokeWidth = 3.5f
                style = Paint.Style.STROKE
            }
            channels.forEachIndexed { rIdx, chName ->
                val baseY = 90f + rIdx * rowH
                canvas.drawText(chName.take(14), 20f, baseY + rowH * 0.5f, lblPaint)
                if (history.size >= 2) {
                    val startX = 180f
                    val spanW = w - 210f
                    val stepX = spanW / history.size.coerceAtLeast(1)
                    val highY = baseY + rowH * 0.22f
                    val lowY = baseY + rowH * 0.72f
                    var prevY = if (history.first().nodeLevels[chName] == LogicLevel.ONE) highY else lowY
                    for (i in history.indices) {
                        val x1 = startX + i * stepX
                        val x2 = x1 + stepX
                        val curY = if (history[i].nodeLevels[chName] == LogicLevel.ONE) highY else lowY
                        if (i > 0 && curY != prevY) canvas.drawLine(x1, prevY, x1, curY, wavePaint)
                        canvas.drawLine(x1, curY, x2, curY, wavePaint)
                        prevY = curY
                    }
                }
            }
        }

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, suggestedFileName(project.name, "png", if (isAnalog) "scope" else "logic"))
        FileOutputStream(file).use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 95, out) }
        bmp.recycle()
        return file
    }

    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}

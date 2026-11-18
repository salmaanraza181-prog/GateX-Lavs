package com.example.ui.canvas

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin
import com.example.engine.netlist.NetlistBuilder
import com.example.model.AnalogSimulationResult
import com.example.model.ComponentCatalog
import com.example.model.ComponentInstance
import com.example.model.ComponentType
import com.example.model.DigitalSimulationResult
import com.example.model.LogicLevel
import com.example.model.PinDirection
import com.example.model.pinWorldPosition
import com.example.ui.theme.FaultRed
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LogicHighGreen
import com.example.ui.theme.LogicHighZViolet
import com.example.ui.theme.LogicLowSlate
import com.example.ui.theme.LogicUnknownAmber
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SignalCyan

fun logicLevelColor(level: LogicLevel, defaultColor: Color): Color = when (level) {
    LogicLevel.ONE -> LogicHighGreen
    LogicLevel.ZERO -> LogicLowSlate
    LogicLevel.UNKNOWN -> LogicUnknownAmber
    LogicLevel.HIGH_Z -> LogicHighZViolet
    LogicLevel.CONFLICT -> FaultRed
}

fun DrawScope.drawSchematicComponent(
    comp: ComponentInstance,
    gridPx: Float,
    isSelected: Boolean,
    isDark: Boolean,
    simRunning: Boolean,
    pulsePhase: Float,
    digitalResult: DigitalSimulationResult,
    analogResult: AnalogSimulationResult,
    textMeasurer: TextMeasurer,
    showLabel: Boolean,
    showValue: Boolean,
    showProbes: Boolean = true,
    pendingWireComponentPin: Pair<String, String>?,
    selectionAccent: Color = SignalCyan
) {
    val meta = ComponentCatalog.metadataFor(comp)
    val cx = comp.x * gridPx
    val cy = comp.y * gridPx
    val w = meta.widthGrid * gridPx
    val h = meta.heightGrid * gridPx

    val bodyStroke = if (isSelected) selectionAccent else if (isDark) Color(0xFFE2EEFF) else Color(0xFF0F172A)
    val mutedColor = if (isDark) Color(0xFF94A7C6) else Color(0xFF475569)

    val rotNorm = ((comp.rotationDegrees % 180) + 180) % 180
    val boxW = if (rotNorm == 90) h else w
    val boxH = if (rotNorm == 90) w else h

    if (isSelected) {
        val left = cx - boxW / 2f - 11f
        val top = cy - boxH / 2f - 11f
        val bw = boxW + 22f
        val bh = boxH + 22f
        drawRoundRect(
            color = selectionAccent.copy(alpha = 0.13f),
            topLeft = Offset(left, top),
            size = Size(bw, bh),
            cornerRadius = CornerRadius(14f, 14f)
        )
        drawRoundRect(
            color = selectionAccent.copy(alpha = 0.65f),
            topLeft = Offset(left, top),
            size = Size(bw, bh),
            cornerRadius = CornerRadius(14f, 14f),
            style = Stroke(width = 1.8f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 6f)))
        )
        // Precision CAD corner brackets
        val bLen = 10f
        val corners = listOf(
            Offset(left, top) to Pair(Offset(left + bLen, top), Offset(left, top + bLen)),
            Offset(left + bw, top) to Pair(Offset(left + bw - bLen, top), Offset(left + bw, top + bLen)),
            Offset(left, top + bh) to Pair(Offset(left + bLen, top + bh), Offset(left, top + bh - bLen)),
            Offset(left + bw, top + bh) to Pair(Offset(left + bw - bLen, top + bh), Offset(left + bw, top + bh - bLen))
        )
        corners.forEach { (c, ends) ->
            drawLine(selectionAccent, c, ends.first, 3.2f)
            drawLine(selectionAccent, c, ends.second, 3.2f)
        }
    }

    withTransform({
        translate(cx, cy)
        rotate(comp.rotationDegrees.toFloat(), pivot = Offset.Zero)
        if (comp.flipped) scale(-1f, 1f, pivot = Offset.Zero)
    }) {
        drawComponentSymbolBody(
            comp = comp,
            w = w,
            h = h,
            gridPx = gridPx,
            isSelected = isSelected,
            isDark = isDark,
            simRunning = simRunning,
            pulsePhase = pulsePhase,
            digitalResult = digitalResult,
            analogResult = analogResult,
            textMeasurer = textMeasurer,
            compactPreview = false
        )
    }

    val wiringActive = pendingWireComponentPin != null

    // Draw high-contrast solder-pad pins at world coordinates
    for (pin in meta.pins) {
        val wp = pinWorldPosition(comp, pin)
        val px = wp.x * gridPx
        val py = wp.y * gridPx
        val isPendingStart = pendingWireComponentPin?.first == comp.id && pendingWireComponentPin.second == pin.id
        val pinKey = NetlistBuilder.pinKey(comp.id, pin.id)
        val pinLevel = digitalResult.pinLevels[pinKey]
        val defaultDirColor = when (pin.direction) {
            PinDirection.POWER -> SignalAmber
            PinDirection.GROUND -> LogicHighGreen
            else -> SignalCyan
        }
        val pinCol = when {
            isPendingStart -> SignalAmber
            simRunning && pinLevel != null && pinLevel != LogicLevel.UNKNOWN -> logicLevelColor(pinLevel, defaultDirColor)
            else -> defaultDirColor
        }
        if (isPendingStart) {
            drawCircle(SignalAmber.copy(alpha = 0.35f), radius = 15f, center = Offset(px, py))
        } else if (wiringActive) {
            // Subtle magnetic target ring when waiting to complete a wire
            drawCircle(SignalCyan.copy(alpha = 0.28f), radius = 10f, center = Offset(px, py), style = Stroke(1.3f))
        }
        drawCircle(pinCol, radius = if (isPendingStart) 8.5f else 5.5f, center = Offset(px, py))
        drawCircle(if (isDark) Color(0xFF0B111E) else Color.White, radius = 2.2f, center = Offset(px, py))

        // Live Node Voltage Probe badge right next to active analog pins when Probes enabled
        if (showProbes && simRunning && gridPx >= 24f && comp.type != ComponentType.GROUND) {
            val vNode = analogResult.pinVoltages[pinKey]
            if (vNode != null && (pin.id == "pos" || pin.id == "out" || pin.id == "com" || pin.id == "wiper" || pin.id == "collector" || pin.id == "drain")) {
                val badgeText = String.format("%.2fV", vNode)
                val res = textMeasurer.measure(
                    badgeText,
                    TextStyle(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = SignalAmber)
                )
                val bx = px + 6f
                val by = py - res.size.height - 6f
                drawRoundRect(
                    color = Color(0xDD080E1A),
                    topLeft = Offset(bx - 3f, by - 1.5f),
                    size = Size(res.size.width + 6f, res.size.height + 3f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawRoundRect(
                    color = SignalAmber.copy(alpha = 0.55f),
                    topLeft = Offset(bx - 3f, by - 1.5f),
                    size = Size(res.size.width + 6f, res.size.height + 3f),
                    cornerRadius = CornerRadius(4f, 4f),
                    style = Stroke(0.9f)
                )
                drawText(res, topLeft = Offset(bx, by))
            }
        }

        // Render pin designators when zoomed in enough for legibility (numbered DIP ICs draw internal pin names & numbers cleanly)
        if (gridPx >= 26f && meta.pins.size > 1 && pin.label.isNotBlank() && pin.pinNumber == null) {
            val pinLbl = textMeasurer.measure(
                pin.label,
                TextStyle(fontFamily = JetBrainsMonoFamily, fontSize = 8.5.sp, color = mutedColor)
            )
            val dx = if (wp.x < comp.x) -pinLbl.size.width - 7f else if (wp.x > comp.x) 7f else -pinLbl.size.width / 2f
            val dy = if (wp.y < comp.y) -pinLbl.size.height - 5f else if (wp.y > comp.y) 6f else -pinLbl.size.height / 2f
            drawText(pinLbl, topLeft = Offset(px + dx, py + dy))
        }
    }

    val isNumberedDip = meta.pins.any { it.pinNumber != null } && comp.type != ComponentType.OP_AMP_741

    // Safety & Burnout Visual Overlay (LED Overcurrent / Resistor Overheat / Blown Fuse)
    val safetyFault = if (simRunning) {
        analogResult.diagnostics.firstOrNull {
            it.componentId == comp.id &&
                (it.code == "LED_OVERCURRENT" || it.code == "RESISTOR_OVERHEAT" || it.code == "FUSE_BLOWN")
        }
    } else null
    val isFuseBlown = comp.type == ComponentType.FUSE && comp.properties["blown"] == "true"
    if (safetyFault != null || isFuseBlown) {
        val pulse = 0.72f + 0.28f * sin(pulsePhase * 2 * PI).toFloat()
        val auraR = maxOf(boxW, boxH) * (0.72f + 0.14f * pulse)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    FaultRed.copy(alpha = 0.42f * pulse),
                    SignalAmber.copy(alpha = 0.20f * pulse),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = auraR
            ),
            radius = auraR,
            center = Offset(cx, cy)
        )
        // Animated heat / smoke wisps rising above overheated part
        for (i in -1..1) {
            val wx = cx + i * gridPx * 0.45f
            val lift = ((pulsePhase + i * 0.28f) % 1f + 1f) % 1f
            val baseY = cy - boxH / 2f - 4f
            val topY = baseY - gridPx * (0.65f + 0.55f * lift)
            val wisp = Path().apply {
                moveTo(wx, baseY)
                quadraticTo(wx + 6f * sin((pulsePhase * 4 + i) * PI).toFloat(), (baseY + topY) / 2f, wx, topY)
            }
            drawPath(
                wisp,
                color = if (isFuseBlown) Color(0xCC94A3B8) else SignalAmber.copy(alpha = (1f - lift) * 0.85f),
                style = Stroke(width = 2.2f, cap = StrokeCap.Round)
            )
        }
        val badgeLabel = if (isFuseBlown || safetyFault?.code == "FUSE_BLOWN") "BLOWN" else "OVERHEAT"
        val badgeRes = textMeasurer.measure(
            badgeLabel,
            TextStyle(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = Color.White)
        )
        val bx = cx - badgeRes.size.width / 2f
        val by = cy - boxH / 2f - badgeRes.size.height - 20f
        drawRoundRect(
            color = FaultRed,
            topLeft = Offset(bx - 5f, by - 2f),
            size = Size(badgeRes.size.width + 10f, badgeRes.size.height + 4f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        drawText(badgeRes, topLeft = Offset(bx, by))
    }

    // Component Label and Value / Live Current above/below bounding box
    val hideExternalText = comp.type == ComponentType.TEXT_NOTE || comp.type == ComponentType.NET_LABEL
    if (showLabel && !hideExternalText) {
        val lblRes = textMeasurer.measure(
            comp.label,
            TextStyle(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = bodyStroke)
        )
        drawText(lblRes, topLeft = Offset(cx - lblRes.size.width / 2f, cy - boxH / 2f - lblRes.size.height - 5f))
    }
    if (showValue && !hideExternalText && comp.value.isNotBlank() && comp.type != ComponentType.LOGIC_INPUT &&
        !(isNumberedDip && (comp.value == meta.shortCode || comp.value.endsWith(meta.shortCode)))
    ) {
        val valText = if (simRunning && analogResult.componentCurrents[comp.id] != null &&
            (comp.type == ComponentType.RESISTOR || comp.type == ComponentType.LED ||
                comp.type == ComponentType.DIODE || comp.type == ComponentType.MOTOR ||
                comp.type == ComponentType.PILOT_LAMP || comp.type == ComponentType.FUSE)
        ) {
            val ma = (analogResult.componentCurrents[comp.id] ?: 0.0) * 1000.0
            "${comp.value} (${String.format("%.1fmA", ma)})"
        } else comp.value
        val valRes = textMeasurer.measure(
            valText,
            TextStyle(fontFamily = JetBrainsMonoFamily, fontSize = 10.sp, color = mutedColor)
        )
        val yBelow = if (isNumberedDip) cy + boxH / 2f + 19f else cy + boxH / 2f + 5f
        drawText(valRes, topLeft = Offset(cx - valRes.size.width / 2f, yBelow))
    }
}

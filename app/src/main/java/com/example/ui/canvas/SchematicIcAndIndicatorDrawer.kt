package com.example.ui.canvas

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.engine.netlist.NetlistBuilder
import com.example.model.AnalogSimulationResult
import com.example.model.ComponentCatalog
import com.example.model.ComponentInstance
import com.example.model.ComponentType
import com.example.model.DigitalSimulationResult
import com.example.model.LogicLevel
import com.example.model.PinDirection
import com.example.ui.theme.CopperGold
import com.example.ui.theme.FaultRed
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LogicHighGreen
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SignalCyan
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal fun probeBaseColor(type: ComponentType): Color = when (type) {
    ComponentType.PROBE_RED -> Color(0xFFFF2E3B)
    ComponentType.PROBE_GREEN -> Color(0xFF00F076)
    ComponentType.PROBE_YELLOW -> Color(0xFFFFEA00)
    ComponentType.PROBE_BLUE -> Color(0xFF2979FF)
    ComponentType.PROBE_ORANGE -> Color(0xFFFF7A00)
    ComponentType.PROBE_CYAN -> Color(0xFF00E5FF)
    else -> LogicHighGreen
}

internal fun isPinEnergized(
    compId: String,
    pinId: String,
    simRunning: Boolean,
    digitalResult: DigitalSimulationResult,
    analogResult: AnalogSimulationResult,
    thresholdV: Double = 1.5
): Boolean {
    if (!simRunning) return false
    val key = NetlistBuilder.pinKey(compId, pinId)
    if (digitalResult.pinLevels[key] == LogicLevel.ONE) return true
    val v = analogResult.pinVoltages[key]
    return v != null && abs(v) >= thresholdV
}

/**
 * Draws an Electronics Workbench / Multisim style 14-pin (or numbered) DIP IC:
 * - Crisp rectangular body with top DIP notch
 * - Left pins (1..7) and right pins (14..8) with metallic leads
 * - Pin numbers printed clearly above each external lead
 * - Active-low inversion bubbles ('o') outside the body wall
 * - Clock edge triangles ('<' / '>') inside the body wall
 * - Pin function labels aligned neatly inside the package
 * - Bold part number printed cleanly beneath the package
 */
internal fun DrawScope.drawNumberedDipIc(
    comp: ComponentInstance,
    w: Float,
    h: Float,
    gridPx: Float,
    isSelected: Boolean,
    isDark: Boolean,
    simRunning: Boolean,
    digitalResult: DigitalSimulationResult,
    textMeasurer: TextMeasurer,
    compactPreview: Boolean
) {
    val meta = ComponentCatalog.metadataFor(comp)
    val bodyW = w * 0.62f
    val bodyH = h * 0.90f
    val halfBodyW = bodyW / 2f
    val halfBodyH = bodyH / 2f
    val sw = if (compactPreview) 1.6f else 2.5f

    val borderCol = when {
        isSelected -> SignalCyan
        isDark -> Color(0xFFE2EEFF)
        else -> Color(0xFF0F172A)
    }
    val leadDefaultCol = if (isDark) Color(0xFFB4C6E0) else Color(0xFF1E293B)
    val pinNumCol = if (isDark) Color(0xFF94A7C6) else Color(0xFF334155)
    val internalTextCol = if (isDark) Color(0xFFEAF2FF) else Color(0xFF0F172A)
    val bgTop = if (isDark) Color(0xFF18243A) else Color(0xFFFFFFFF)
    val bgBottom = if (isDark) Color(0xFF0D1524) else Color(0xFFF1F5F9)

    if (compactPreview) {
        // Sleek miniature 14-pin DIP icon for Parts Bin
        val miniW = w * 0.56f
        val miniH = h * 0.88f
        val legCount = (meta.pins.size / 2).coerceIn(4, 7)
        for (i in 0 until legCount) {
            val ly = -miniH * 0.40f + (miniH * 0.80f) * (i.toFloat() / (legCount - 1).coerceAtLeast(1))
            drawLine(Color(0xFF94A3B8), Offset(-w * 0.46f, ly), Offset(-miniW / 2f, ly), 1.5f)
            drawLine(Color(0xFF94A3B8), Offset(miniW / 2f, ly), Offset(w * 0.46f, ly), 1.5f)
        }
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(bgTop, bgBottom)),
            topLeft = Offset(-miniW / 2f, -miniH / 2f),
            size = Size(miniW, miniH),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawRoundRect(
            color = CopperGold,
            topLeft = Offset(-miniW / 2f, -miniH / 2f),
            size = Size(miniW, miniH),
            cornerRadius = CornerRadius(4f, 4f),
            style = Stroke(1.6f)
        )
        drawArc(
            color = CopperGold,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(-4f, -miniH / 2f - 2f),
            size = Size(8f, 4f),
            style = Stroke(1.2f)
        )
        val codeRes = textMeasurer.measure(
            meta.shortCode,
            TextStyle(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp,
                color = if (isDark) Color(0xFFFFD54F) else Color(0xFF0F172A)
            )
        )
        drawText(codeRes, topLeft = Offset(-codeRes.size.width / 2f, -codeRes.size.height / 2f))
        return
    }

    val bubbleR = (gridPx * 0.145f).coerceIn(3.2f, 5.5f)

    // 1. Draw metallic leads + external pin numbers + active-low bubbles
    for (pin in meta.pins) {
        val px = pin.relX * gridPx
        val py = pin.relY * gridPx
        val isLeft = pin.relX < 0f
        val edgeX = if (isLeft) -halfBodyW else halfBodyW
        val pinLevel = if (simRunning) digitalResult.pinLevels[NetlistBuilder.pinKey(comp.id, pin.id)] else null
        val leadCol = when {
            pinLevel == LogicLevel.ONE -> LogicHighGreen
            pinLevel == LogicLevel.CONFLICT -> FaultRed
            pin.direction == PinDirection.POWER -> SignalAmber
            pin.direction == PinDirection.GROUND -> Color(0xFF38BDF8)
            else -> leadDefaultCol
        }

        val leadEndX = if (pin.activeLow) {
            if (isLeft) edgeX - bubbleR * 2f else edgeX + bubbleR * 2f
        } else {
            edgeX
        }
        drawLine(leadCol, Offset(px, py), Offset(leadEndX, py), sw * 0.95f, cap = StrokeCap.Round)

        // Pin number printed directly above each lead (matching EWB 7472 reference photo)
        val pNum = pin.pinNumber
        if (pNum != null && gridPx >= 14f) {
            val numSizeSp = (gridPx * 0.30f).coerceIn(7.5f, 11.5f).sp
            val numRes = textMeasurer.measure(
                pNum.toString(),
                TextStyle(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = numSizeSp,
                    color = pinNumCol
                )
            )
            val midLeadX = (px + leadEndX) / 2f
            drawText(
                numRes,
                topLeft = Offset(
                    midLeadX - numRes.size.width / 2f,
                    py - numRes.size.height - 2f
                )
            )
        }
    }

    // 2. Draw crisp rectangular IC body with subtle lab elevation
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(bgTop, bgBottom)),
        topLeft = Offset(-halfBodyW, -halfBodyH),
        size = Size(bodyW, bodyH),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = borderCol,
        topLeft = Offset(-halfBodyW, -halfBodyH),
        size = Size(bodyW, bodyH),
        cornerRadius = CornerRadius(4f, 4f),
        style = Stroke(sw)
    )
    // Subtle top DIP Pin-1 notch & index dot
    val notchW = (bodyW * 0.14f).coerceIn(12f, 22f)
    drawArc(
        color = borderCol.copy(alpha = 0.72f),
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(-notchW / 2f, -halfBodyH - notchW * 0.25f),
        size = Size(notchW, notchW * 0.5f),
        style = Stroke(sw * 0.8f)
    )

    // 3. Draw active-low bubbles on body wall, clock triangles inside wall, and internal pin names
    val labelSizeSp = (gridPx * 0.32f).coerceIn(7.5f, 12f).sp
    for (pin in meta.pins) {
        val py = pin.relY * gridPx
        val isLeft = pin.relX < 0f
        val edgeX = if (isLeft) -halfBodyW else halfBodyW
        val pinLevel = if (simRunning) digitalResult.pinLevels[NetlistBuilder.pinKey(comp.id, pin.id)] else null

        if (pin.activeLow) {
            val bx = if (isLeft) edgeX - bubbleR else edgeX + bubbleR
            drawCircle(bgTop, radius = bubbleR, center = Offset(bx, py))
            drawCircle(borderCol, radius = bubbleR, center = Offset(bx, py), style = Stroke(sw * 0.8f))
        }

        val triDepth = (gridPx * 0.34f).coerceIn(6f, 11f)
        val triHalfH = (gridPx * 0.24f).coerceIn(4.5f, 8f)
        if (pin.isClock) {
            val clkPath = Path().apply {
                if (isLeft) {
                    moveTo(edgeX, py - triHalfH)
                    lineTo(edgeX + triDepth, py)
                    lineTo(edgeX, py + triHalfH)
                } else {
                    moveTo(edgeX, py - triHalfH)
                    lineTo(edgeX - triDepth, py)
                    lineTo(edgeX, py + triHalfH)
                }
            }
            drawPath(
                clkPath,
                color = if (pinLevel == LogicLevel.ONE) LogicHighGreen else borderCol,
                style = Stroke(sw * 0.85f, join = StrokeJoin.Miter)
            )
        }

        if (gridPx >= 15f) {
            val nameColor = when {
                pinLevel == LogicLevel.ONE -> LogicHighGreen
                pinLevel == LogicLevel.CONFLICT -> FaultRed
                pin.direction == PinDirection.POWER -> SignalAmber
                pin.direction == PinDirection.GROUND -> Color(0xFF38BDF8)
                else -> internalTextCol
            }
            val lblRes = textMeasurer.measure(
                pin.label,
                TextStyle(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = labelSizeSp,
                    color = nameColor
                )
            )
            val padInset = (gridPx * 0.20f).coerceIn(4f, 8f) + if (pin.isClock) triDepth + 2f else 0f
            val tx = if (isLeft) {
                -halfBodyW + padInset
            } else {
                halfBodyW - padInset - lblRes.size.width
            }
            drawText(lblRes, topLeft = Offset(tx, py - lblRes.size.height / 2f))
        }
    }

    // 4. Part Number printed crisply underneath the IC box (just like "7472" in reference photo)
    val partRes = textMeasurer.measure(
        meta.shortCode,
        TextStyle(
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (gridPx * 0.42f).coerceIn(10f, 15f).sp,
            color = CopperGold
        )
    )
    drawText(
        partRes,
        topLeft = Offset(-partRes.size.width / 2f, halfBodyH + 4f)
    )
}

/**
 * Draws an Electronics Workbench style 1-input Colored Circular Probe (Red, Green, Yellow, Blue, Orange, Cyan)
 * with vivid multi-ring glow illumination when energized!
 */
internal fun DrawScope.drawColoredProbeIndicator(
    comp: ComponentInstance,
    w: Float,
    h: Float,
    isSelected: Boolean,
    isDark: Boolean,
    simRunning: Boolean,
    pulsePhase: Float,
    digitalResult: DigitalSimulationResult,
    analogResult: AnalogSimulationResult,
    compactPreview: Boolean
) {
    val baseColor = probeBaseColor(comp.type)
    val thresh = comp.properties["thresholdV"]?.toDoubleOrNull() ?: 1.5
    val isLit = if (compactPreview) {
        true
    } else {
        isPinEnergized(comp.id, "in", simRunning, digitalResult, analogResult, thresh)
    }

    val sw = if (compactPreview) 1.8f else 2.6f
    val domeCenter = Offset(0f, -h * 0.14f)
    val domeR = min(w, h) * 0.36f
    val stemColor = if (isSelected) SignalCyan else if (isDark) Color(0xFFCBD5E1) else Color(0xFF1E293B)

    // Bottom single terminal stem (exact EWB probe silhouette)
    drawLine(
        color = if (isLit && !compactPreview) baseColor else stemColor,
        start = Offset(0f, domeCenter.y + domeR),
        end = Offset(0f, h / 2f),
        strokeWidth = sw * 1.05f,
        cap = StrokeCap.Round
    )

    if (isLit && !compactPreview) {
        // Vivid multi-layer light bloom ("light jaalni chahiye"!)
        val pulse = 1f + 0.12f * sin(pulsePhase * 2 * PI).toFloat()
        val outerBloomR = domeR * 2.55f * pulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    baseColor.copy(alpha = 0.72f),
                    baseColor.copy(alpha = 0.30f),
                    baseColor.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = domeCenter,
                radius = outerBloomR
            ),
            radius = outerBloomR,
            center = domeCenter
        )
        // Radiating light beams around energized dome
        val rayInner = domeR * 1.24f
        val rayOuter = domeR * (1.62f + 0.10f * sin(pulsePhase * 2 * PI).toFloat())
        for (i in 0 until 8) {
            val ang = i * (PI / 4.0)
            val cosA = cos(ang).toFloat()
            val sinA = sin(ang).toFloat()
            drawLine(
                color = baseColor.copy(alpha = 0.75f),
                start = Offset(domeCenter.x + rayInner * cosA, domeCenter.y + rayInner * sinA),
                end = Offset(domeCenter.x + rayOuter * cosA, domeCenter.y + rayOuter * sinA),
                strokeWidth = sw * 0.75f,
                cap = StrokeCap.Round
            )
        }
    }

    // Outer metallic bezel ring
    drawCircle(
        color = if (isDark) Color(0xFF1E293B) else Color(0xFF334155),
        radius = domeR * 1.10f,
        center = domeCenter
    )

    // Main glass probe dome (bright white-hot illuminated gradient when ON, dark smoked glass when OFF)
    val domeBrush = if (isLit) {
        Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = if (compactPreview) 0.75f else 0.96f),
                baseColor,
                baseColor.copy(alpha = 0.88f)
            ),
            center = Offset(domeCenter.x - domeR * 0.22f, domeCenter.y - domeR * 0.22f),
            radius = domeR * 1.22f
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                baseColor.copy(alpha = 0.32f),
                baseColor.copy(alpha = 0.14f),
                if (isDark) Color(0xFF0A101C) else Color(0xFF1E293B)
            ),
            center = Offset(domeCenter.x - domeR * 0.22f, domeCenter.y - domeR * 0.22f),
            radius = domeR * 1.15f
        )
    }
    drawCircle(brush = domeBrush, radius = domeR, center = domeCenter)

    // Specular glass dome highlight
    drawCircle(
        color = Color.White.copy(alpha = if (isLit) 0.72f else 0.22f),
        radius = domeR * 0.24f,
        center = Offset(domeCenter.x - domeR * 0.32f, domeCenter.y - domeR * 0.32f)
    )

    // Rim outline
    drawCircle(
        color = if (isSelected) SignalCyan else if (isLit) Color.White.copy(alpha = 0.85f) else baseColor.copy(alpha = 0.72f),
        radius = domeR,
        center = domeCenter,
        style = Stroke(sw * 0.85f)
    )
}

/**
 * Draws an Electronics Workbench incandescent Light Bulb / Lamp indicator with coiled filament
 * and warm golden illumination + light rays when energized.
 */
internal fun DrawScope.drawPilotLamp(
    comp: ComponentInstance,
    w: Float,
    h: Float,
    isSelected: Boolean,
    isDark: Boolean,
    simRunning: Boolean,
    pulsePhase: Float,
    digitalResult: DigitalSimulationResult,
    analogResult: AnalogSimulationResult,
    compactPreview: Boolean
) {
    val sw = if (compactPreview) 1.8f else 2.6f
    val r = min(w, h) * 0.36f
    val leadCol = if (isSelected) SignalCyan else if (isDark) Color(0xFFE2EEFF) else Color(0xFF0F172A)

    val p1Key = NetlistBuilder.pinKey(comp.id, "p1")
    val p2Key = NetlistBuilder.pinKey(comp.id, "p2")
    val dv = abs((analogResult.pinVoltages[p1Key] ?: 0.0) - (analogResult.pinVoltages[p2Key] ?: 0.0))
    val d1 = digitalResult.pinLevels[p1Key]
    val d2 = digitalResult.pinLevels[p2Key]
    val digitalOn = (d1 == LogicLevel.ONE && d2 != LogicLevel.ONE) || (d2 == LogicLevel.ONE && d1 != LogicLevel.ONE)
    val isLit = compactPreview || (simRunning && (dv >= 1.5 || digitalOn))
    val ratedV = (comp.properties["ratedV"]?.toDoubleOrNull() ?: 5.0).coerceAtLeast(1.5)
    val brightness = when {
        compactPreview -> 0.85f
        !isLit -> 0f
        dv >= 1.5 -> (dv / ratedV).toFloat().coerceIn(0.45f, 1.0f)
        else -> 1.0f
    }

    // Horizontal terminal leads
    drawLine(leadCol, Offset(-w / 2f, 0f), Offset(-r, 0f), sw)
    drawLine(leadCol, Offset(r, 0f), Offset(w / 2f, 0f), sw)

    // Warm golden glow & radiating beams when lit
    if (isLit && !compactPreview) {
        val pulse = 1f + 0.10f * sin(pulsePhase * 2 * PI).toFloat()
        val bloomR = r * (2.3f + 0.45f * brightness) * pulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF59D).copy(alpha = 0.80f * brightness),
                    Color(0xFFFFD600).copy(alpha = 0.40f * brightness),
                    Color(0xFFFF9100).copy(alpha = 0.12f * brightness),
                    Color.Transparent
                ),
                center = Offset.Zero,
                radius = bloomR
            ),
            radius = bloomR
        )
        for (i in 0 until 8) {
            val ang = i * (PI / 4.0) + PI / 8.0
            val cA = cos(ang).toFloat()
            val sA = sin(ang).toFloat()
            drawLine(
                color = Color(0xFFFFEA00).copy(alpha = 0.85f * brightness),
                start = Offset(r * 1.18f * cA, r * 1.18f * sA),
                end = Offset(r * 1.62f * cA, r * 1.62f * sA),
                strokeWidth = sw * 0.8f,
                cap = StrokeCap.Round
            )
        }
    }

    // Bulb glass body
    val glassBrush = if (isLit) {
        Brush.radialGradient(
            colors = listOf(
                Color.White,
                Color(0xFFFFF176).copy(alpha = 0.95f),
                Color(0xFFFFB300).copy(alpha = 0.78f)
            ),
            center = Offset(0f, -r * 0.12f),
            radius = r * 1.15f
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFD54F).copy(alpha = 0.16f),
                if (isDark) Color(0xFF131E33) else Color(0xFFE2E8F0)
            ),
            center = Offset.Zero,
            radius = r
        )
    }
    drawCircle(brush = glassBrush, radius = r)
    drawCircle(
        color = if (isLit) Color(0xFFFFD600) else leadCol,
        radius = r,
        style = Stroke(sw)
    )

    // Coiled tungsten filament loop inside bulb (matching EWB bulb icon!)
    val filament = Path().apply {
        moveTo(-r * 0.65f, 0f)
        lineTo(-r * 0.35f, 0f)
        lineTo(-r * 0.22f, -r * 0.35f)
        quadraticTo(-r * 0.08f, -r * 0.58f, 0f, -r * 0.18f)
        quadraticTo(r * 0.08f, -r * 0.58f, r * 0.22f, -r * 0.35f)
        lineTo(r * 0.35f, 0f)
        lineTo(r * 0.65f, 0f)
    }
    drawPath(
        filament,
        color = if (isLit) Color(0xFFFF6D00) else if (isDark) Color(0xFF94A7C6) else Color(0xFF475569),
        style = Stroke(sw * 0.85f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

/**
 * Draws an Electronics Workbench Bargraph Display (4 direct LED bars) or Decoded Bargraph (8-bar level meter).
 */
internal fun DrawScope.drawBarGraphDisplay(
    comp: ComponentInstance,
    w: Float,
    h: Float,
    gridPx: Float,
    isSelected: Boolean,
    isDark: Boolean,
    simRunning: Boolean,
    digitalResult: DigitalSimulationResult,
    analogResult: AnalogSimulationResult,
    compactPreview: Boolean
) {
    val sw = if (compactPreview) 1.6f else 2.5f
    val isDecoded = comp.type == ComponentType.DECODED_BAR_GRAPH
    val bodyW = w * 0.68f
    val bodyH = h * 0.90f
    val borderCol = if (isSelected) SignalCyan else if (isDark) Color(0xFFE2EEFF) else Color(0xFF0F172A)
    val bezelFill = if (isDark) Color(0xFF0A101D) else Color(0xFF1E293B)

    // Input leads on left
    val pinYs = listOf(-1.5f, -0.5f, 0.5f, 1.5f)
    for (ry in pinYs) {
        val py = ry * gridPx
        drawLine(borderCol, Offset(-w / 2f, py), Offset(-bodyW / 2f, py), sw * 0.85f)
    }

    // Dark instrument housing
    drawRoundRect(
        color = bezelFill,
        topLeft = Offset(-bodyW / 2f, -bodyH / 2f),
        size = Size(bodyW, bodyH),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawRoundRect(
        color = if (isDecoded) SignalCyan else CopperGold,
        topLeft = Offset(-bodyW / 2f, -bodyH / 2f),
        size = Size(bodyW, bodyH),
        cornerRadius = CornerRadius(8f, 8f),
        style = Stroke(sw)
    )

    val barPadX = bodyW * 0.16f
    val barW = bodyW - barPadX * 2f

    if (!isDecoded) {
        // 4 direct-driven glowing LED bars (b3 top .. b0 bottom)
        val pins = listOf("b3", "b2", "b1", "b0")
        val barColors = listOf(Color(0xFFFF2E3B), Color(0xFFFF9100), Color(0xFFFFEA00), Color(0xFF00F076))
        val slotH = (bodyH * 0.82f) / 4f
        val barH = slotH * 0.62f
        val startY = -bodyH * 0.41f
        pins.forEachIndexed { idx, pinId ->
            val lit = if (compactPreview) (idx % 2 == 0) else isPinEnergized(comp.id, pinId, simRunning, digitalResult, analogResult)
            val col = barColors[idx]
            val topY = startY + idx * slotH + (slotH - barH) / 2f
            if (lit && !compactPreview) {
                drawRoundRect(
                    color = col.copy(alpha = 0.38f),
                    topLeft = Offset(-barW / 2f - 4f, topY - 3f),
                    size = Size(barW + 8f, barH + 6f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
            drawRoundRect(
                color = if (lit) col else Color(0xFF1F2937),
                topLeft = Offset(-barW / 2f, topY),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(4f, 4f)
            )
            if (lit) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.55f),
                    topLeft = Offset(-barW * 0.36f, topY + barH * 0.22f),
                    size = Size(barW * 0.72f, barH * 0.32f),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
        }
    } else {
        // Decoded 8-bar progressive meter (0..15 fills bottom to top)
        val value = if (compactPreview) {
            10
        } else {
            val b3 = if (isPinEnergized(comp.id, "b3", simRunning, digitalResult, analogResult)) 8 else 0
            val b2 = if (isPinEnergized(comp.id, "b2", simRunning, digitalResult, analogResult)) 4 else 0
            val b1 = if (isPinEnergized(comp.id, "b1", simRunning, digitalResult, analogResult)) 2 else 0
            val b0 = if (isPinEnergized(comp.id, "b0", simRunning, digitalResult, analogResult)) 1 else 0
            b3 + b2 + b1 + b0
        }
        val litBars = if (value == 0) 0 else ((value * 8 + 7) / 15).coerceIn(1, 8)
        val slotH = (bodyH * 0.84f) / 8f
        val barH = slotH * 0.65f
        val bottomY = bodyH * 0.42f
        for (i in 0 until 8) {
            val isOn = i < litBars
            val col = when {
                i >= 6 -> Color(0xFFFF2E3B) // Peak red
                i >= 4 -> Color(0xFFFFEA00) // Mid yellow
                else -> Color(0xFF00F076)   // Nominal green
            }
            val topY = bottomY - (i + 1) * slotH + (slotH - barH) / 2f
            if (isOn && !compactPreview) {
                drawRoundRect(
                    color = col.copy(alpha = 0.34f),
                    topLeft = Offset(-barW / 2f - 3f, topY - 2f),
                    size = Size(barW + 6f, barH + 4f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
            drawRoundRect(
                color = if (isOn) col else Color(0xFF1F2937),
                topLeft = Offset(-barW / 2f, topY),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(3f, 3f)
            )
        }
    }
}

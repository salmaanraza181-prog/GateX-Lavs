package com.example.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.engine.netlist.NetlistBuilder
import com.example.model.AnalogSimulationResult
import com.example.model.ComponentCatalog
import com.example.model.ComponentCategory
import com.example.model.ComponentInstance
import com.example.model.ComponentType
import com.example.model.DigitalSimulationResult
import com.example.model.LogicLevel
import com.example.ui.theme.CopperGold
import com.example.ui.theme.FaultRed
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LogicHighGreen
import com.example.ui.theme.LogicLowSlate
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SignalCyan
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun ComponentSymbolThumbnail(
    type: ComponentType,
    isDark: Boolean = true,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val meta = ComponentCatalog.get(type)
    val dummyComp = ComponentInstance(
        id = "preview_${type.name}",
        type = type,
        label = meta.shortCode,
        x = 0f,
        y = 0f,
        value = meta.defaultValue,
        properties = meta.defaultProperties
    )
    Canvas(modifier = modifier) {
        val gridPx = min(
            (size.width * 0.78f) / meta.widthGrid.coerceAtLeast(2f),
            (size.height * 0.78f) / meta.heightGrid.coerceAtLeast(2f)
        ).coerceIn(6f, 20f)
        val w = meta.widthGrid * gridPx
        val h = meta.heightGrid * gridPx
        withTransform({
            translate(size.width / 2f, size.height / 2f)
        }) {
            drawComponentSymbolBody(
                comp = dummyComp,
                w = w,
                h = h,
                gridPx = gridPx,
                isSelected = false,
                isDark = isDark,
                simRunning = false,
                pulsePhase = 0f,
                digitalResult = DigitalSimulationResult(),
                analogResult = AnalogSimulationResult(),
                textMeasurer = textMeasurer,
                compactPreview = true
            )
        }
    }
}

internal fun DrawScope.drawComponentSymbolBody(
    comp: ComponentInstance,
    w: Float,
    h: Float,
    gridPx: Float,
    isSelected: Boolean,
    isDark: Boolean,
    simRunning: Boolean,
    pulsePhase: Float,
    digitalResult: DigitalSimulationResult,
    analogResult: AnalogSimulationResult,
    textMeasurer: TextMeasurer,
    compactPreview: Boolean = false
) {
    val meta = ComponentCatalog.metadataFor(comp)
    val bodyStroke = if (isSelected) SignalCyan else if (isDark) Color(0xFFE2EEFF) else Color(0xFF0F172A)
    val cardFill = if (isDark) Color(0xFF131E33) else Color(0xFFFFFFFF)
    val recessedFill = if (isDark) Color(0xFF090E19) else Color(0xFFE2E8F0)
    val mutedColor = if (isDark) Color(0xFF94A7C6) else Color(0xFF475569)
    val sw = if (compactPreview) 2.2f else 3f

    when (comp.type) {
        ComponentType.RESISTOR, ComponentType.VARIABLE_RESISTOR -> {
            val p = Path().apply {
                moveTo(-w / 2f, 0f)
                lineTo(-w * 0.28f, 0f)
                lineTo(-w * 0.22f, -h * 0.36f)
                lineTo(-w * 0.11f, h * 0.36f)
                lineTo(0f, -h * 0.36f)
                lineTo(w * 0.11f, h * 0.36f)
                lineTo(w * 0.22f, -h * 0.36f)
                lineTo(w * 0.28f, 0f)
                lineTo(w / 2f, 0f)
            }
            drawPath(p, bodyStroke, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
            if (comp.type == ComponentType.VARIABLE_RESISTOR) {
                drawLine(SignalAmber, Offset(-w * 0.20f, h * 0.44f), Offset(w * 0.20f, -h * 0.44f), sw)
                drawArrowTip(Offset(w * 0.20f, -h * 0.44f), Offset(-w * 0.20f, h * 0.44f), SignalAmber, sw * 2.2f)
            }
        }

        ComponentType.POTENTIOMETER -> {
            drawLine(bodyStroke, Offset(-w / 2f, 0f), Offset(-w * 0.26f, 0f), sw)
            drawLine(bodyStroke, Offset(w * 0.26f, 0f), Offset(w / 2f, 0f), sw)
            val zig = Path().apply {
                moveTo(-w * 0.26f, 0f)
                lineTo(-w * 0.18f, -h * 0.16f)
                lineTo(-w * 0.06f, h * 0.16f)
                lineTo(w * 0.06f, -h * 0.16f)
                lineTo(w * 0.18f, h * 0.16f)
                lineTo(w * 0.26f, 0f)
            }
            drawPath(zig, bodyStroke, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawLine(SignalAmber, Offset(0f, -h / 2f), Offset(0f, -h * 0.14f), sw)
            drawArrowTip(Offset(0f, -h * 0.14f), Offset(0f, -h / 2f), SignalAmber, sw * 2f)
        }

        ComponentType.CAPACITOR, ComponentType.POLARIZED_CAPACITOR -> {
            drawLine(bodyStroke, Offset(-w / 2f, 0f), Offset(-gridPx * 0.20f, 0f), sw)
            drawLine(bodyStroke, Offset(gridPx * 0.20f, 0f), Offset(w / 2f, 0f), sw)
            drawLine(bodyStroke, Offset(-gridPx * 0.20f, -h * 0.46f), Offset(-gridPx * 0.20f, h * 0.46f), sw * 1.25f)
            if (comp.type == ComponentType.POLARIZED_CAPACITOR) {
                val curved = Path().apply {
                    moveTo(gridPx * 0.28f, -h * 0.46f)
                    quadraticTo(gridPx * 0.10f, 0f, gridPx * 0.28f, h * 0.46f)
                }
                drawPath(curved, SignalCyan, style = Stroke(width = sw * 1.2f, cap = StrokeCap.Round))
                drawLine(SignalAmber, Offset(-w * 0.36f, -h * 0.34f), Offset(-w * 0.24f, -h * 0.34f), sw * 0.9f)
                drawLine(SignalAmber, Offset(-w * 0.30f, -h * 0.44f), Offset(-w * 0.30f, -h * 0.24f), sw * 0.9f)
            } else {
                drawLine(bodyStroke, Offset(gridPx * 0.20f, -h * 0.46f), Offset(gridPx * 0.20f, h * 0.46f), sw * 1.25f)
            }
        }

        ComponentType.INDUCTOR -> {
            drawLine(bodyStroke, Offset(-w / 2f, 0f), Offset(-w * 0.32f, 0f), sw)
            drawLine(bodyStroke, Offset(w * 0.32f, 0f), Offset(w / 2f, 0f), sw)
            for (i in 0 until 4) {
                val ax = -w * 0.32f + i * (w * 0.16f)
                drawArc(
                    color = CopperGold,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(ax, -h * 0.30f),
                    size = Size(w * 0.16f, h * 0.56f),
                    style = Stroke(sw, cap = StrokeCap.Round)
                )
            }
        }

        ComponentType.TRANSFORMER -> {
            drawRoundRect(cardFill.copy(alpha = 0.55f), Offset(-w * 0.42f, -h * 0.44f), Size(w * 0.84f, h * 0.88f), CornerRadius(10f, 10f))
            for (i in 0 until 3) {
                val ay = -h * 0.32f + i * (h * 0.21f)
                drawArc(CopperGold, -90f, 180f, false, Offset(-w * 0.34f, ay), Size(w * 0.24f, h * 0.21f), style = Stroke(sw))
                drawArc(SignalCyan, 90f, 180f, false, Offset(w * 0.10f, ay), Size(w * 0.24f, h * 0.21f), style = Stroke(sw))
            }
            drawLine(mutedColor, Offset(-w * 0.04f, -h * 0.36f), Offset(-w * 0.04f, h * 0.36f), sw)
            drawLine(mutedColor, Offset(w * 0.04f, -h * 0.36f), Offset(w * 0.04f, h * 0.36f), sw)
            drawLine(bodyStroke, Offset(-w / 2f, -gridPx), Offset(-w * 0.22f, -gridPx), sw * 0.85f)
            drawLine(bodyStroke, Offset(-w / 2f, gridPx), Offset(-w * 0.22f, gridPx), sw * 0.85f)
            drawLine(bodyStroke, Offset(w * 0.22f, -gridPx), Offset(w / 2f, -gridPx), sw * 0.85f)
            drawLine(bodyStroke, Offset(w * 0.22f, gridPx), Offset(w / 2f, gridPx), sw * 0.85f)
        }

        ComponentType.FUSE -> {
            val isBlown = comp.properties["blown"] == "true" ||
                (simRunning && analogResult.diagnostics.any { it.componentId == comp.id && it.code == "FUSE_BLOWN" })
            drawLine(bodyStroke, Offset(-w / 2f, 0f), Offset(-w * 0.28f, 0f), sw)
            drawLine(bodyStroke, Offset(w * 0.28f, 0f), Offset(w / 2f, 0f), sw)
            // Glass tube
            val tubeFill = if (isBlown) Color(0xFF2B1114) else if (isDark) Color(0x3360A5FA) else Color(0x330EA5E9)
            drawRoundRect(tubeFill, Offset(-w * 0.28f, -h * 0.34f), Size(w * 0.56f, h * 0.68f), CornerRadius(6f, 6f))
            // Metallic end caps
            val capCol = if (isBlown) FaultRed else CopperGold
            drawRoundRect(capCol, Offset(-w * 0.32f, -h * 0.38f), Size(w * 0.12f, h * 0.76f), CornerRadius(3f, 3f))
            drawRoundRect(capCol, Offset(w * 0.20f, -h * 0.38f), Size(w * 0.12f, h * 0.76f), CornerRadius(3f, 3f))
            drawRoundRect(
                if (isBlown) FaultRed else bodyStroke,
                Offset(-w * 0.28f, -h * 0.34f),
                Size(w * 0.56f, h * 0.68f),
                CornerRadius(6f, 6f),
                style = Stroke(sw * 0.85f)
            )
            if (isBlown) {
                // Broken filament with charred center gap
                drawLine(FaultRed, Offset(-w * 0.20f, 0f), Offset(-w * 0.06f, -h * 0.14f), sw)
                drawLine(FaultRed, Offset(w * 0.06f, h * 0.14f), Offset(w * 0.20f, 0f), sw)
                drawLine(SignalAmber, Offset(-w * 0.04f, -h * 0.20f), Offset(w * 0.04f, h * 0.20f), sw * 0.8f)
            } else {
                // Intact S-wave filament
                val wirePath = Path().apply {
                    moveTo(-w * 0.20f, 0f)
                    cubicTo(-w * 0.08f, -h * 0.24f, w * 0.08f, h * 0.24f, w * 0.20f, 0f)
                }
                drawPath(wirePath, SignalAmber, style = Stroke(sw * 0.95f, cap = StrokeCap.Round))
            }
        }

        ComponentType.OP_AMP_741 -> {
            // Input leads (+ at -gridPx, - at +gridPx), Output lead, and optional V+/V- rail leads
            drawLine(bodyStroke, Offset(-w / 2f, -gridPx), Offset(-w * 0.34f, -gridPx), sw)
            drawLine(bodyStroke, Offset(-w / 2f, gridPx), Offset(-w * 0.34f, gridPx), sw)
            drawLine(bodyStroke, Offset(w * 0.38f, 0f), Offset(w / 2f, 0f), sw)
            drawLine(FaultRed.copy(alpha = 0.75f), Offset(0f, -h / 2f), Offset(0f, -h * 0.22f), sw * 0.8f)
            drawLine(SignalCyan.copy(alpha = 0.75f), Offset(0f, h * 0.22f), Offset(0f, h / 2f), sw * 0.8f)

            val tri = Path().apply {
                moveTo(-w * 0.34f, -h * 0.44f)
                lineTo(-w * 0.34f, h * 0.44f)
                lineTo(w * 0.38f, 0f)
                close()
            }
            drawPath(
                tri,
                brush = Brush.verticalGradient(
                    if (isDark) listOf(Color(0xFF1A2942), Color(0xFF0D1628)) else listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0))
                )
            )
            drawPath(tri, if (isSelected) SignalCyan else CopperGold, style = Stroke(sw, join = StrokeJoin.Round))

            // '+' and '-' symbols inside left edge
            val plusY = -gridPx * 0.62f
            val minusY = gridPx * 0.62f
            val signX = -w * 0.22f
            val signR = gridPx * 0.16f
            drawLine(LogicHighGreen, Offset(signX - signR, plusY), Offset(signX + signR, plusY), sw * 0.9f)
            drawLine(LogicHighGreen, Offset(signX, plusY - signR), Offset(signX, plusY + signR), sw * 0.9f)
            drawLine(FaultRed, Offset(signX - signR, minusY), Offset(signX + signR, minusY), sw * 0.9f)

            if (!compactPreview) {
                val res = textMeasurer.measure(
                    "741",
                    TextStyle(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = CopperGold)
                )
                drawText(res, topLeft = Offset(-res.size.width * 0.35f, -res.size.height * 0.85f))
                if (simRunning) {
                    val vOut = analogResult.pinVoltages[NetlistBuilder.pinKey(comp.id, "out")]
                    if (vOut != null) {
                        val vRes = textMeasurer.measure(
                            String.format("%.1fV", vOut),
                            TextStyle(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = SignalAmber)
                        )
                        drawText(vRes, topLeft = Offset(-vRes.size.width * 0.35f, 2f))
                    }
                }
            }
        }

        ComponentType.NET_LABEL -> {
            val netLvl = digitalResult.pinLevels[NetlistBuilder.pinKey(comp.id, "pin")]
            val tagAccent = when {
                isSelected -> SignalCyan
                simRunning && netLvl != null && netLvl != LogicLevel.UNKNOWN -> logicLevelColor(netLvl, SignalCyan)
                else -> SignalCyan
            }
            drawLine(tagAccent, Offset(-w / 2f, 0f), Offset(-w * 0.26f, 0f), sw)
            val pennant = Path().apply {
                moveTo(-w * 0.26f, 0f)
                lineTo(-w * 0.10f, -h * 0.42f)
                lineTo(w * 0.46f, -h * 0.42f)
                lineTo(w * 0.46f, h * 0.42f)
                lineTo(-w * 0.10f, h * 0.42f)
                close()
            }
            drawPath(pennant, tagAccent.copy(alpha = if (simRunning && netLvl == LogicLevel.ONE) 0.32f else 0.16f))
            drawPath(pennant, tagAccent, style = Stroke(sw * 0.95f, join = StrokeJoin.Round))
            if (!compactPreview) {
                val tagText = comp.value.trim().uppercase().ifBlank { "NET" }.take(8)
                val res = textMeasurer.measure(
                    tagText,
                    TextStyle(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        color = if (isDark) Color(0xFFEAF6FF) else Color(0xFF0F172A)
                    )
                )
                drawText(res, topLeft = Offset(w * 0.16f - res.size.width / 2f, -res.size.height / 2f))
            }
        }

        ComponentType.TEXT_NOTE -> {
            val noteBg = if (isDark) Color(0xFF1F273B) else Color(0xFFFFFBEB)
            val noteBorder = if (isSelected) SignalCyan else SignalAmber.copy(alpha = 0.75f)
            drawRoundRect(noteBg, Offset(-w / 2f, -h / 2f), Size(w, h), CornerRadius(8f, 8f))
            drawRoundRect(noteBorder, Offset(-w / 2f, -h / 2f), Size(w, h), CornerRadius(8f, 8f), style = Stroke(sw * 0.85f))
            // Corner fold accent
            val fold = Path().apply {
                moveTo(w / 2f - 12f, -h / 2f)
                lineTo(w / 2f, -h / 2f + 12f)
                lineTo(w / 2f - 12f, -h / 2f + 12f)
                close()
            }
            drawPath(fold, SignalAmber.copy(alpha = 0.55f))
            if (!compactPreview) {
                val txt = comp.value.ifBlank { "Note" }.take(36)
                val res = textMeasurer.measure(
                    txt,
                    TextStyle(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.5.sp,
                        color = if (isDark) Color(0xFFFEF3C7) else Color(0xFF451A03)
                    )
                )
                drawText(res, topLeft = Offset(-res.size.width / 2f, -res.size.height / 2f))
            } else {
                drawLine(SignalAmber, Offset(-w * 0.30f, -h * 0.14f), Offset(w * 0.28f, -h * 0.14f), sw * 0.8f)
                drawLine(mutedColor, Offset(-w * 0.30f, h * 0.14f), Offset(w * 0.18f, h * 0.14f), sw * 0.8f)
            }
        }

        ComponentType.DIODE, ComponentType.ZENER_DIODE, ComponentType.LED, ComponentType.PHOTODIODE -> {
            drawLine(bodyStroke, Offset(-w / 2f, 0f), Offset(w / 2f, 0f), sw * 0.85f)
            val tri = Path().apply {
                moveTo(-gridPx * 0.45f, -h * 0.38f)
                lineTo(-gridPx * 0.45f, h * 0.38f)
                lineTo(gridPx * 0.45f, 0f)
                close()
            }
            val currentMa = (analogResult.componentCurrents[comp.id] ?: 0.0) * 1000.0
            val ledLit = comp.type == ComponentType.LED && simRunning && currentMa > 0.5
            if (ledLit) {
                val rBloom = w * (0.65f + 0.14f * sin(pulsePhase * 2 * PI).toFloat())
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            LogicHighGreen.copy(alpha = 0.55f),
                            LogicHighGreen.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset.Zero,
                        radius = rBloom
                    ),
                    radius = rBloom
                )
            }
            val triFill = when {
                ledLit -> LogicHighGreen
                comp.type == ComponentType.LED -> FaultRed.copy(alpha = 0.35f)
                else -> SignalAmber.copy(alpha = 0.28f)
            }
            val triStroke = if (ledLit) LogicHighGreen else bodyStroke
            drawPath(tri, triFill)
            drawPath(tri, triStroke, style = Stroke(sw))
            drawLine(triStroke, Offset(gridPx * 0.45f, -h * 0.38f), Offset(gridPx * 0.45f, h * 0.38f), sw * 1.15f)
            if (comp.type == ComponentType.ZENER_DIODE) {
                drawLine(bodyStroke, Offset(gridPx * 0.45f, -h * 0.38f), Offset(gridPx * 0.68f, -h * 0.48f), sw)
                drawLine(bodyStroke, Offset(gridPx * 0.45f, h * 0.38f), Offset(gridPx * 0.22f, h * 0.48f), sw)
            }
            if (comp.type == ComponentType.LED) {
                val rayCol = if (ledLit) LogicHighGreen else SignalAmber
                drawLine(rayCol, Offset(0f, -h * 0.44f), Offset(gridPx * 0.36f, -h * 0.72f), sw * 0.8f)
                drawLine(rayCol, Offset(gridPx * 0.32f, -h * 0.36f), Offset(gridPx * 0.68f, -h * 0.64f), sw * 0.8f)
            }
            if (comp.type == ComponentType.PHOTODIODE) {
                drawLine(SignalCyan, Offset(-gridPx * 0.35f, -h * 0.70f), Offset(0f, -h * 0.42f), sw * 0.8f)
                drawLine(SignalCyan, Offset(gridPx * 0.10f, -h * 0.70f), Offset(gridPx * 0.42f, -h * 0.42f), sw * 0.8f)
            }
        }

        ComponentType.RGB_LED -> {
            drawRoundRect(cardFill, Offset(-w / 2f, -h / 2f), Size(w, h), CornerRadius(12f, 12f))
            drawRoundRect(bodyStroke, Offset(-w / 2f, -h / 2f), Size(w, h), CornerRadius(12f, 12f), style = Stroke(sw))
            val rLit = simRunning && digitalResult.pinLevels[NetlistBuilder.pinKey(comp.id, "r")] == LogicLevel.ONE
            val gLit = simRunning && digitalResult.pinLevels[NetlistBuilder.pinKey(comp.id, "g")] == LogicLevel.ONE
            val bLit = simRunning && digitalResult.pinLevels[NetlistBuilder.pinKey(comp.id, "b")] == LogicLevel.ONE
            val blend = Color(
                red = if (rLit) 1f else 0.2f,
                green = if (gLit) 1f else 0.2f,
                blue = if (bLit) 1f else 0.2f
            )
            if (rLit || gLit || bLit) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(blend.copy(alpha = 0.85f), blend.copy(alpha = 0.2f), Color.Transparent),
                        center = Offset.Zero,
                        radius = gridPx * 1.45f
                    ),
                    radius = gridPx * 1.45f
                )
            }
            drawCircle(blend.copy(alpha = if (rLit || gLit || bLit) 0.75f else 0.18f), radius = gridPx * 0.85f)
            drawCircle(FaultRed, radius = gridPx * 0.28f, center = Offset(-gridPx * 0.45f, -gridPx * 0.65f))
            drawCircle(LogicHighGreen, radius = gridPx * 0.28f, center = Offset(-gridPx * 0.45f, 0f))
            drawCircle(SignalCyan, radius = gridPx * 0.28f, center = Offset(-gridPx * 0.45f, gridPx * 0.65f))
        }

        ComponentType.TRANSISTOR_NPN, ComponentType.TRANSISTOR_PNP -> {
            val r = gridPx * 0.95f
            drawCircle(cardFill, radius = r)
            drawCircle(bodyStroke, radius = r, style = Stroke(sw * 0.9f))
            drawLine(bodyStroke, Offset(-w / 2f, 0f), Offset(-gridPx * 0.35f, 0f), sw)
            drawLine(bodyStroke, Offset(-gridPx * 0.35f, -gridPx * 0.55f), Offset(-gridPx * 0.35f, gridPx * 0.55f), sw * 1.3f)
            drawLine(bodyStroke, Offset(-gridPx * 0.35f, -gridPx * 0.25f), Offset(gridPx, -h / 2f), sw)
            drawLine(bodyStroke, Offset(-gridPx * 0.35f, gridPx * 0.25f), Offset(gridPx, h / 2f), sw)
            if (comp.type == ComponentType.TRANSISTOR_NPN) {
                drawArrowTip(Offset(gridPx * 0.45f, h * 0.32f), Offset(-gridPx * 0.35f, gridPx * 0.25f), SignalAmber, sw * 2f)
            } else {
                drawArrowTip(Offset(-gridPx * 0.25f, -gridPx * 0.27f), Offset(gridPx * 0.6f, -h * 0.40f), SignalCyan, sw * 2f)
            }
        }

        ComponentType.MOSFET -> {
            val r = gridPx * 0.95f
            drawCircle(cardFill, radius = r)
            drawCircle(bodyStroke, radius = r, style = Stroke(sw * 0.9f))
            drawLine(bodyStroke, Offset(-w / 2f, 0f), Offset(-gridPx * 0.45f, 0f), sw)
            drawLine(bodyStroke, Offset(-gridPx * 0.45f, -gridPx * 0.50f), Offset(-gridPx * 0.45f, gridPx * 0.50f), sw)
            // Broken channel segments
            for (segY in listOf(-gridPx * 0.45f, 0f, gridPx * 0.45f)) {
                drawLine(SignalCyan, Offset(-gridPx * 0.20f, segY - gridPx * 0.14f), Offset(-gridPx * 0.20f, segY + gridPx * 0.14f), sw * 1.15f)
            }
            drawLine(bodyStroke, Offset(-gridPx * 0.20f, -gridPx * 0.45f), Offset(gridPx, -h / 2f), sw)
            drawLine(bodyStroke, Offset(-gridPx * 0.20f, gridPx * 0.45f), Offset(gridPx, h / 2f), sw)
            drawLine(SignalAmber, Offset(-gridPx * 0.20f, 0f), Offset(gridPx * 0.4f, 0f), sw * 0.9f)
        }

        ComponentType.DC_SOURCE, ComponentType.AC_SOURCE, ComponentType.POWER_SUPPLY, ComponentType.VARIABLE_INPUT, ComponentType.CURRENT_SOURCE -> {
            val r = min(w, h) * 0.40f
            drawCircle(cardFill, radius = r)
            drawCircle(SignalAmber, radius = r, style = Stroke(sw))
            when (comp.type) {
                ComponentType.AC_SOURCE -> {
                    val wave = Path().apply {
                        moveTo(-r * 0.55f, 0f)
                        quadraticTo(-r * 0.28f, -r * 0.55f, 0f, 0f)
                        quadraticTo(r * 0.28f, r * 0.55f, r * 0.55f, 0f)
                    }
                    drawPath(wave, SignalCyan, style = Stroke(sw, cap = StrokeCap.Round))
                }
                ComponentType.CURRENT_SOURCE -> {
                    drawLine(SignalCyan, Offset(0f, r * 0.55f), Offset(0f, -r * 0.50f), sw)
                    drawArrowTip(Offset(0f, -r * 0.55f), Offset(0f, r * 0.55f), SignalCyan, sw * 2.2f)
                }
                else -> {
                    drawLine(SignalAmber, Offset(-r * 0.35f, -r * 0.20f), Offset(r * 0.35f, -r * 0.20f), sw)
                    drawLine(SignalAmber, Offset(0f, -r * 0.50f), Offset(0f, r * 0.10f), sw)
                    drawLine(mutedColor, Offset(-r * 0.30f, r * 0.45f), Offset(r * 0.30f, r * 0.45f), sw)
                }
            }
        }

        ComponentType.BATTERY -> {
            drawLine(bodyStroke, Offset(-w / 2f, 0f), Offset(-gridPx * 0.45f, 0f), sw)
            drawLine(bodyStroke, Offset(gridPx * 0.45f, 0f), Offset(w / 2f, 0f), sw)
            drawLine(SignalAmber, Offset(-gridPx * 0.45f, -h * 0.46f), Offset(-gridPx * 0.45f, h * 0.46f), sw * 1.2f)
            drawLine(bodyStroke, Offset(-gridPx * 0.12f, -h * 0.25f), Offset(-gridPx * 0.12f, h * 0.25f), sw * 1.5f)
            drawLine(SignalAmber, Offset(gridPx * 0.18f, -h * 0.46f), Offset(gridPx * 0.18f, h * 0.46f), sw * 1.2f)
            drawLine(bodyStroke, Offset(gridPx * 0.45f, -h * 0.25f), Offset(gridPx * 0.45f, h * 0.25f), sw * 1.5f)
        }

        ComponentType.GROUND -> {
            drawLine(bodyStroke, Offset(0f, -h / 2f), Offset(0f, 0f), sw)
            drawLine(LogicHighGreen, Offset(-gridPx * 0.68f, 0f), Offset(gridPx * 0.68f, 0f), sw * 1.1f)
            drawLine(LogicHighGreen, Offset(-gridPx * 0.42f, gridPx * 0.32f), Offset(gridPx * 0.42f, gridPx * 0.32f), sw * 1.1f)
            drawLine(LogicHighGreen, Offset(-gridPx * 0.18f, gridPx * 0.64f), Offset(gridPx * 0.18f, gridPx * 0.64f), sw * 1.1f)
        }

        ComponentType.VCC -> {
            drawLine(bodyStroke, Offset(0f, h / 2f), Offset(0f, -gridPx * 0.15f), sw)
            val tri = Path().apply {
                moveTo(-gridPx * 0.55f, 0f)
                lineTo(gridPx * 0.55f, 0f)
                lineTo(0f, -gridPx * 0.65f)
                close()
            }
            drawPath(tri, FaultRed.copy(alpha = 0.25f))
            drawPath(tri, FaultRed, style = Stroke(sw))
        }

        ComponentType.VOLTAGE_REGULATOR -> {
            drawRoundRect(cardFill, Offset(-w * 0.34f, -h * 0.42f), Size(w * 0.68f, h * 0.84f), CornerRadius(8f, 8f))
            drawRoundRect(CopperGold, Offset(-w * 0.34f, -h * 0.42f), Size(w * 0.68f, h * 0.84f), CornerRadius(8f, 8f), style = Stroke(sw))
            drawLine(bodyStroke, Offset(-w / 2f, 0f), Offset(-w * 0.34f, 0f), sw)
            drawLine(bodyStroke, Offset(w * 0.34f, 0f), Offset(w / 2f, 0f), sw)
            drawLine(bodyStroke, Offset(0f, h * 0.42f), Offset(0f, h / 2f), sw)
            if (!compactPreview) {
                val res = textMeasurer.measure("7805", TextStyle(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = CopperGold))
                drawText(res, topLeft = Offset(-res.size.width / 2f, -res.size.height / 2f))
            }
        }

        // Complete EWB Switch Family
        ComponentType.TOGGLE_SWITCH -> {
            val isClosed = comp.properties["closed"] == "true"
            drawLine(bodyStroke, Offset(-w / 2f, 0f), Offset(-w * 0.22f, 0f), sw)
            drawLine(bodyStroke, Offset(w * 0.22f, 0f), Offset(w / 2f, 0f), sw)
            drawCircle(SignalCyan, sw * 1.4f, Offset(-w * 0.22f, 0f))
            drawCircle(SignalCyan, sw * 1.4f, Offset(w * 0.22f, 0f))
            val bladeCol = if (isClosed) LogicHighGreen else SignalAmber
            drawLine(bladeCol, Offset(-w * 0.22f, 0f), Offset(w * 0.22f, if (isClosed) 0f else -h * 0.42f), sw * 1.3f)
        }

        ComponentType.SPDT_SWITCH -> {
            val isNo = comp.properties["position"] == "NO"
            drawLine(bodyStroke, Offset(-w / 2f, 0f), Offset(-w * 0.22f, 0f), sw)
            drawLine(bodyStroke, Offset(w * 0.22f, -gridPx), Offset(w / 2f, -gridPx), sw)
            drawLine(bodyStroke, Offset(w * 0.22f, gridPx), Offset(w / 2f, gridPx), sw)
            drawCircle(SignalCyan, sw * 1.3f, Offset(-w * 0.22f, 0f))
            drawCircle(if (isNo) LogicHighGreen else mutedColor, sw * 1.3f, Offset(w * 0.22f, -gridPx))
            drawCircle(if (!isNo) LogicHighGreen else mutedColor, sw * 1.3f, Offset(w * 0.22f, gridPx))
            val targetY = if (isNo) -gridPx else gridPx
            drawLine(LogicHighGreen, Offset(-w * 0.22f, 0f), Offset(w * 0.20f, targetY), sw * 1.3f)
        }

        ComponentType.DPST_SWITCH -> {
            val isClosed = comp.properties["closed"] == "true"
            val bladeCol = if (isClosed) LogicHighGreen else SignalAmber
            for (poleY in listOf(-gridPx, gridPx)) {
                drawLine(bodyStroke, Offset(-w / 2f, poleY), Offset(-w * 0.22f, poleY), sw)
                drawLine(bodyStroke, Offset(w * 0.22f, poleY), Offset(w / 2f, poleY), sw)
                drawCircle(SignalCyan, sw * 1.2f, Offset(-w * 0.22f, poleY))
                drawCircle(SignalCyan, sw * 1.2f, Offset(w * 0.22f, poleY))
                val endY = if (isClosed) poleY else poleY - gridPx * 0.65f
                drawLine(bladeCol, Offset(-w * 0.22f, poleY), Offset(w * 0.22f, endY), sw * 1.2f)
            }
            drawLine(
                mutedColor,
                Offset(0f, -gridPx * (if (isClosed) 1f else 1.35f)),
                Offset(0f, gridPx * (if (isClosed) 1f else 0.65f)),
                sw * 0.7f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))
            )
        }

        ComponentType.DPDT_SWITCH -> {
            val isNo = comp.properties["position"] == "NO"
            for (baseY in listOf(-gridPx * 1.5f, gridPx * 1.5f)) {
                drawLine(bodyStroke, Offset(-w / 2f, baseY), Offset(-w * 0.22f, baseY), sw)
                drawLine(bodyStroke, Offset(w * 0.22f, baseY - gridPx), Offset(w / 2f, baseY - gridPx), sw)
                drawLine(bodyStroke, Offset(w * 0.22f, baseY + gridPx), Offset(w / 2f, baseY + gridPx), sw)
                drawCircle(SignalCyan, sw * 1.2f, Offset(-w * 0.22f, baseY))
                drawCircle(if (isNo) LogicHighGreen else mutedColor, sw * 1.2f, Offset(w * 0.22f, baseY - gridPx))
                drawCircle(if (!isNo) LogicHighGreen else mutedColor, sw * 1.2f, Offset(w * 0.22f, baseY + gridPx))
                val ty = if (isNo) baseY - gridPx else baseY + gridPx
                drawLine(LogicHighGreen, Offset(-w * 0.22f, baseY), Offset(w * 0.20f, ty), sw * 1.2f)
            }
            drawLine(
                SignalAmber,
                Offset(0f, -gridPx * 1.5f),
                Offset(0f, gridPx * 1.5f),
                sw * 0.7f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))
            )
        }

        ComponentType.PUSH_BUTTON, ComponentType.PUSH_BUTTON_NC -> {
            val isNc = comp.type == ComponentType.PUSH_BUTTON_NC
            val pressed = comp.properties["pressed"] == "true"
            val closed = if (isNc) !pressed else pressed
            drawLine(bodyStroke, Offset(-w / 2f, 0f), Offset(-w * 0.22f, 0f), sw)
            drawLine(bodyStroke, Offset(w * 0.22f, 0f), Offset(w / 2f, 0f), sw)
            drawCircle(SignalCyan, sw * 1.3f, Offset(-w * 0.22f, 0f))
            drawCircle(SignalCyan, sw * 1.3f, Offset(w * 0.22f, 0f))
            val barY = if (closed) -sw * 0.5f else -h * 0.34f
            val col = if (closed) LogicHighGreen else SignalAmber
            drawLine(col, Offset(-w * 0.25f, barY), Offset(w * 0.25f, barY), sw * 1.2f)
            drawLine(col, Offset(0f, barY), Offset(0f, barY - h * 0.28f), sw)
            drawLine(col, Offset(-w * 0.14f, barY - h * 0.28f), Offset(w * 0.14f, barY - h * 0.28f), sw * 1.3f)
        }

        ComponentType.ROTARY_SWITCH -> {
            val pos = (comp.properties["position"]?.toIntOrNull() ?: 1).coerceIn(1, 4)
            drawRoundRect(cardFill, Offset(-w * 0.38f, -h * 0.44f), Size(w * 0.76f, h * 0.88f), CornerRadius(12f, 12f))
            drawRoundRect(CopperGold, Offset(-w * 0.38f, -h * 0.44f), Size(w * 0.76f, h * 0.88f), CornerRadius(12f, 12f), style = Stroke(sw))
            drawLine(bodyStroke, Offset(-w / 2f, 0f), Offset(-w * 0.24f, 0f), sw)
            drawCircle(SignalCyan, sw * 1.3f, Offset(-w * 0.24f, 0f))
            val ys = listOf(-gridPx * 1.5f, -gridPx * 0.5f, gridPx * 0.5f, gridPx * 1.5f)
            ys.forEachIndexed { idx, py ->
                val active = (idx + 1) == pos
                drawLine(bodyStroke, Offset(w * 0.24f, py), Offset(w / 2f, py), sw)
                drawCircle(if (active) LogicHighGreen else mutedColor, sw * 1.3f, Offset(w * 0.24f, py))
                if (active) {
                    drawLine(LogicHighGreen, Offset(-w * 0.24f, 0f), Offset(w * 0.22f, py), sw * 1.3f)
                }
            }
        }

        ComponentType.DIP_SWITCH -> {
            drawRoundRect(Color(0xFF8B1E24), Offset(-w * 0.38f, -h * 0.45f), Size(w * 0.76f, h * 0.90f), CornerRadius(10f, 10f))
            drawRoundRect(bodyStroke, Offset(-w * 0.38f, -h * 0.45f), Size(w * 0.76f, h * 0.90f), CornerRadius(10f, 10f), style = Stroke(sw))
            val bits = (comp.properties["bits"] ?: "0000").padEnd(4, '0')
            val ys = listOf(-gridPx * 1.5f, -gridPx * 0.5f, gridPx * 0.5f, gridPx * 1.5f)
            ys.forEachIndexed { i, py ->
                val isOn = bits[i] == '1'
                drawRoundRect(Color(0xFF0B111E), Offset(-w * 0.24f, py - gridPx * 0.26f), Size(w * 0.48f, gridPx * 0.52f), CornerRadius(4f, 4f))
                val knobX = if (isOn) w * 0.04f else -w * 0.20f
                drawRoundRect(if (isOn) LogicHighGreen else Color(0xFFE2E8F0), Offset(knobX, py - gridPx * 0.22f), Size(w * 0.18f, gridPx * 0.44f), CornerRadius(3f, 3f))
                drawLine(bodyStroke, Offset(w * 0.38f, py), Offset(w / 2f, py), sw * 0.85f)
            }
        }

        ComponentType.VOLTAGE_CONTROLLED_SWITCH -> {
            val vOn = comp.properties["von"]?.toDoubleOrNull() ?: 2.5
            val vCtrl = (analogResult.pinVoltages[NetlistBuilder.pinKey(comp.id, "ctrl_pos")] ?: 0.0) -
                (analogResult.pinVoltages[NetlistBuilder.pinKey(comp.id, "ctrl_neg")] ?: 0.0)
            val closed = simRunning && vCtrl >= vOn
            drawRoundRect(cardFill, Offset(-w * 0.38f, -h * 0.38f), Size(w * 0.76f, h * 0.76f), CornerRadius(10f, 10f))
            drawRoundRect(if (closed) LogicHighGreen else SignalCyan, Offset(-w * 0.38f, -h * 0.38f), Size(w * 0.76f, h * 0.76f), CornerRadius(10f, 10f), style = Stroke(sw))
            drawLine(bodyStroke, Offset(-w / 2f, 0f), Offset(-w * 0.20f, 0f), sw)
            drawLine(bodyStroke, Offset(w * 0.20f, 0f), Offset(w / 2f, 0f), sw)
            drawLine(SignalAmber, Offset(0f, -h / 2f), Offset(0f, -h * 0.22f), sw)
            drawLine(mutedColor, Offset(0f, h * 0.22f), Offset(0f, h / 2f), sw)
            drawLine(
                if (closed) LogicHighGreen else SignalAmber,
                Offset(-w * 0.20f, 0f),
                Offset(w * 0.20f, if (closed) 0f else -h * 0.22f),
                sw * 1.2f
            )
            drawCircle(SignalCyan, sw * 1.1f, Offset(-w * 0.20f, 0f))
            drawCircle(SignalCyan, sw * 1.1f, Offset(w * 0.20f, 0f))
        }

        ComponentType.RELAY, ComponentType.RELAY_SPDT -> {
            val vCoil = abs(
                (analogResult.pinVoltages[NetlistBuilder.pinKey(comp.id, "coil1")] ?: 0.0) -
                    (analogResult.pinVoltages[NetlistBuilder.pinKey(comp.id, "coil2")] ?: 0.0)
            )
            val energized = simRunning && vCoil >= 3.5
            drawRoundRect(cardFill, Offset(-w * 0.42f, -h * 0.44f), Size(w * 0.84f, h * 0.88f), CornerRadius(10f, 10f))
            drawRoundRect(if (energized) LogicHighGreen else CopperGold, Offset(-w * 0.42f, -h * 0.44f), Size(w * 0.84f, h * 0.88f), CornerRadius(10f, 10f), style = Stroke(sw))
            // Relay coil loops on left
            for (i in 0 until 3) {
                val ay = -h * 0.28f + i * (h * 0.18f)
                drawArc(if (energized) LogicHighGreen else SignalAmber, -90f, 180f, false, Offset(-w * 0.32f, ay), Size(w * 0.18f, h * 0.18f), style = Stroke(sw))
            }
            drawLine(bodyStroke, Offset(-w / 2f, -gridPx), Offset(-w * 0.22f, -gridPx), sw * 0.85f)
            drawLine(bodyStroke, Offset(-w / 2f, gridPx), Offset(-w * 0.22f, gridPx), sw * 0.85f)
            // Magnetic coupling dashed line
            drawLine(mutedColor, Offset(-w * 0.04f, -h * 0.30f), Offset(-w * 0.04f, h * 0.30f), sw * 0.7f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f)))
            // Switch blade on right
            val targetY = if (comp.type == ComponentType.RELAY_SPDT) {
                if (energized) -gridPx * 1.15f else gridPx * 1.15f
            } else {
                if (energized) 0f else -gridPx * 0.7f
            }
            drawLine(if (energized) LogicHighGreen else SignalAmber, Offset(w * 0.08f, 0f), Offset(w * 0.36f, targetY), sw * 1.1f)
            drawCircle(SignalCyan, sw, Offset(w * 0.08f, 0f))
            if (comp.type == ComponentType.RELAY_SPDT) {
                drawCircle(if (energized) LogicHighGreen else mutedColor, sw * 0.9f, Offset(w * 0.36f, -gridPx * 1.15f))
                drawCircle(if (!energized) LogicHighGreen else mutedColor, sw * 0.9f, Offset(w * 0.36f, gridPx * 1.15f))
            }
        }

        ComponentType.GATE_AND, ComponentType.GATE_NAND,
        ComponentType.GATE_OR, ComponentType.GATE_NOR,
        ComponentType.GATE_XOR, ComponentType.GATE_XNOR,
        ComponentType.GATE_NOT, ComponentType.GATE_BUFFER, ComponentType.GATE_TRISTATE -> {
            val inputPins = meta.pins.filter { it.direction == com.example.model.PinDirection.INPUT && it.id != "en" }
            drawLogicGateSymbol(comp.type, w, h, gridPx, inputPins, cardFill, bodyStroke, sw)
        }

        ComponentType.LOGIC_INPUT -> {
            val isOne = comp.properties["state"] == "1" || comp.value == "1"
            val col = if (isOne) LogicHighGreen else LogicLowSlate
            drawRoundRect(col.copy(alpha = 0.24f), Offset(-w / 2f, -h / 2f), Size(w, h), CornerRadius(10f, 10f))
            drawRoundRect(col, Offset(-w / 2f, -h / 2f), Size(w, h), CornerRadius(10f, 10f), style = Stroke(sw))
            if (!compactPreview) {
                val res = textMeasurer.measure(
                    if (isOne) "1" else "0",
                    TextStyle(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = col)
                )
                drawText(res, topLeft = Offset(-res.size.width / 2f, -res.size.height / 2f))
            } else {
                drawCircle(col, radius = min(w, h) * 0.18f)
            }
        }

        ComponentType.CLOCK_INPUT -> {
            drawRoundRect(cardFill, Offset(-w / 2f, -h / 2f), Size(w, h), CornerRadius(10f, 10f))
            drawRoundRect(SignalCyan, Offset(-w / 2f, -h / 2f), Size(w, h), CornerRadius(10f, 10f), style = Stroke(sw))
            val sq = Path().apply {
                moveTo(-w * 0.32f, h * 0.22f)
                lineTo(-w * 0.32f, -h * 0.22f)
                lineTo(0f, -h * 0.22f)
                lineTo(0f, h * 0.22f)
                lineTo(w * 0.32f, h * 0.22f)
                lineTo(w * 0.32f, -h * 0.22f)
            }
            drawPath(sq, LogicHighGreen, style = Stroke(sw, cap = StrokeCap.Square))
        }

        ComponentType.PROBE_RED, ComponentType.PROBE_GREEN,
        ComponentType.PROBE_YELLOW, ComponentType.PROBE_BLUE,
        ComponentType.PROBE_ORANGE, ComponentType.PROBE_CYAN -> {
            drawColoredProbeIndicator(
                comp = comp,
                w = w,
                h = h,
                isSelected = isSelected,
                isDark = isDark,
                simRunning = simRunning,
                pulsePhase = pulsePhase,
                digitalResult = digitalResult,
                analogResult = analogResult,
                compactPreview = compactPreview
            )
        }

        ComponentType.PILOT_LAMP -> {
            drawPilotLamp(
                comp = comp,
                w = w,
                h = h,
                isSelected = isSelected,
                isDark = isDark,
                simRunning = simRunning,
                pulsePhase = pulsePhase,
                digitalResult = digitalResult,
                analogResult = analogResult,
                compactPreview = compactPreview
            )
        }

        ComponentType.BAR_GRAPH, ComponentType.DECODED_BAR_GRAPH -> {
            drawBarGraphDisplay(
                comp = comp,
                w = w,
                h = h,
                gridPx = gridPx,
                isSelected = isSelected,
                isDark = isDark,
                simRunning = simRunning,
                digitalResult = digitalResult,
                analogResult = analogResult,
                compactPreview = compactPreview
            )
        }

        ComponentType.LOGIC_INDICATOR, ComponentType.LED_INDICATOR -> {
            val lvl = digitalResult.pinLevels[NetlistBuilder.pinKey(comp.id, "in")] ?: LogicLevel.UNKNOWN
            val analogLit = isPinEnergized(comp.id, "in", simRunning, digitalResult, analogResult)
            val effectiveLit = simRunning && (lvl == LogicLevel.ONE || analogLit)
            val col = if (effectiveLit) LogicHighGreen else logicLevelColor(lvl, mutedColor)
            val rad = min(w, h) * 0.44f
            if (effectiveLit) {
                val pulse = 1f + 0.12f * sin(pulsePhase * 2 * PI).toFloat()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(col.copy(alpha = 0.80f), col.copy(alpha = 0.28f), Color.Transparent),
                        center = Offset.Zero,
                        radius = rad * 2.35f * pulse
                    ),
                    radius = rad * 2.35f * pulse
                )
            }
            val fillBrush = if (effectiveLit) {
                Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.85f), col, col.copy(alpha = 0.82f)),
                    center = Offset(-rad * 0.2f, -rad * 0.2f),
                    radius = rad * 1.2f
                )
            } else {
                Brush.radialGradient(
                    colors = listOf(col.copy(alpha = 0.22f), col.copy(alpha = 0.08f)),
                    center = Offset.Zero,
                    radius = rad
                )
            }
            drawCircle(brush = fillBrush, radius = rad)
            drawCircle(if (effectiveLit) Color.White.copy(alpha = 0.9f) else col, radius = rad, style = Stroke(sw))
            if (!compactPreview) {
                val txt = if (comp.type == ComponentType.LED_INDICATOR && effectiveLit) "ON" else if (effectiveLit) "1" else lvl.symbol
                val res = textMeasurer.measure(
                    txt,
                    TextStyle(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (effectiveLit) Color(0xFF062112) else col
                    )
                )
                drawText(res, topLeft = Offset(-res.size.width / 2f, -res.size.height / 2f))
            }
        }

        ComponentType.SEVEN_SEGMENT, ComponentType.HEX_DISPLAY, ComponentType.DIGITAL_DISPLAY -> {
            drawRoundRect(recessedFill, Offset(-w / 2f, -h / 2f), Size(w, h), CornerRadius(12f, 12f))
            drawRoundRect(CopperGold, Offset(-w / 2f, -h / 2f), Size(w, h), CornerRadius(12f, 12f), style = Stroke(sw))
            if (comp.type == ComponentType.DIGITAL_DISPLAY && !compactPreview) {
                val valueStr = digitalResult.componentStates[comp.id]?.get("value") ?: "0"
                val res = textMeasurer.measure(
                    valueStr,
                    TextStyle(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = SignalCyan)
                )
                drawText(res, topLeft = Offset(-res.size.width / 2f, -res.size.height / 2f))
            } else {
                val segMask = when (comp.type) {
                    ComponentType.SEVEN_SEGMENT -> {
                        val activeLow = comp.properties["activeLow"] == "true"
                        var mask = 0
                        listOf("a", "b", "c", "d", "e", "f", "g").forEachIndexed { idx, pinId ->
                            val isOn = if (compactPreview) {
                                true
                            } else if (activeLow) {
                                simRunning && digitalResult.pinLevels[NetlistBuilder.pinKey(comp.id, pinId)] == LogicLevel.ZERO
                            } else {
                                isPinEnergized(comp.id, pinId, simRunning, digitalResult, analogResult)
                            }
                            if (isOn) mask = mask or (1 shl idx)
                        }
                        if (!simRunning && !compactPreview) 0x3F else mask
                    }
                    ComponentType.HEX_DISPLAY -> {
                        val hexChar = digitalResult.componentStates[comp.id]?.get("hex") ?: if (compactPreview) "8" else "0"
                        hexToSevenSegMask(hexChar.firstOrNull() ?: '0')
                    }
                    else -> 0x7F
                }
                drawSevenSegmentBars(w * 0.52f, h * 0.68f, segMask, Color(0xFFFF2E3B), sw, glow = simRunning && !compactPreview)
            }
        }

        ComponentType.LCD_16X2 -> {
            val isOn = compactPreview || (simRunning && digitalResult.componentStates[comp.id]?.get("en") == "1")
            val pcbCol = Color(0xFF14532D)
            val screenBg = if (isOn) Color(0xFF84CC16) else Color(0xFF3F6212)
            val pixelCol = if (isOn) Color(0xFF0F2912) else Color(0xFF1E3A1E)
            drawRoundRect(pcbCol, Offset(-w / 2f, -h / 2f), Size(w, h), CornerRadius(10f, 10f))
            drawRoundRect(if (isSelected) SignalCyan else CopperGold, Offset(-w / 2f, -h / 2f), Size(w, h), CornerRadius(10f, 10f), style = Stroke(sw))
            // Dark bezel + backlit LCD glass
            drawRoundRect(Color(0xFF0B111E), Offset(-w * 0.42f, -h * 0.36f), Size(w * 0.86f, h * 0.72f), CornerRadius(6f, 6f))
            drawRoundRect(screenBg, Offset(-w * 0.38f, -h * 0.30f), Size(w * 0.78f, h * 0.60f), CornerRadius(4f, 4f))
            if (!compactPreview) {
                val line1 = (digitalResult.componentStates[comp.id]?.get("line1") ?: comp.properties["line1"] ?: "GATEX LABS").take(14)
                val line2 = (digitalResult.componentStates[comp.id]?.get("line2") ?: "EN=1 FOR DATA").take(14)
                val fontSp = (gridPx * 0.33f).coerceIn(7f, 11.5f).sp
                val r1 = textMeasurer.measure(line1, TextStyle(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, fontSize = fontSp, color = pixelCol))
                val r2 = textMeasurer.measure(line2, TextStyle(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Medium, fontSize = fontSp, color = pixelCol))
                drawText(r1, topLeft = Offset(-w * 0.34f, -h * 0.24f))
                drawText(r2, topLeft = Offset(-w * 0.34f, h * 0.03f))
            } else {
                drawLine(pixelCol, Offset(-w * 0.28f, -h * 0.10f), Offset(w * 0.28f, -h * 0.10f), sw * 0.8f)
                drawLine(pixelCol, Offset(-w * 0.28f, h * 0.10f), Offset(w * 0.14f, h * 0.10f), sw * 0.8f)
            }
        }

        ComponentType.DOT_MATRIX_8X8 -> {
            drawRoundRect(Color(0xFF0F172A), Offset(-w / 2f, -h / 2f), Size(w, h), CornerRadius(10f, 10f))
            drawRoundRect(if (isSelected) SignalCyan else CopperGold, Offset(-w / 2f, -h / 2f), Size(w, h), CornerRadius(10f, 10f), style = Stroke(sw))
            val rowMask = digitalResult.componentStates[comp.id]?.get("rows")?.toIntOrNull() ?: if (compactPreview) 0x0F else 0
            val colMask = digitalResult.componentStates[comp.id]?.get("cols")?.toIntOrNull() ?: if (compactPreview) 0x0F else 0
            val padX = w * 0.16f
            val padY = h * 0.14f
            val innerW = w - padX * 2f
            val innerH = h - padY * 2f
            val dotR = (min(innerW, innerH) / 18f).coerceAtLeast(1.6f)
            for (r in 0 until 8) {
                val rowBit = r / 2
                val rowOn = (rowMask and (1 shl rowBit)) != 0
                for (c in 0 until 8) {
                    val colBit = c / 2
                    val colOn = (colMask and (1 shl colBit)) != 0
                    val lit = if (compactPreview) ((r + c) % 2 == 0) else (simRunning && rowOn && colOn)
                    val dx = -innerW / 2f + (c + 0.5f) * (innerW / 8f)
                    val dy = -innerH / 2f + (r + 0.5f) * (innerH / 8f)
                    if (lit && !compactPreview) {
                        drawCircle(Color(0x66FF2E3B), radius = dotR * 1.9f, center = Offset(dx, dy))
                    }
                    drawCircle(
                        color = if (lit) Color(0xFFFF2E3B) else Color(0xFF283548),
                        radius = dotR,
                        center = Offset(dx, dy)
                    )
                }
            }
        }

        ComponentType.TRAFFIC_LIGHT -> {
            val rLit = compactPreview || isPinEnergized(comp.id, "r", simRunning, digitalResult, analogResult)
            val yLit = isPinEnergized(comp.id, "y", simRunning, digitalResult, analogResult)
            val gLit = isPinEnergized(comp.id, "g", simRunning, digitalResult, analogResult)
            drawRoundRect(Color(0xFF111827), Offset(-w * 0.36f, -h * 0.46f), Size(w * 0.72f, h * 0.92f), CornerRadius(12f, 12f))
            drawRoundRect(if (isSelected) SignalCyan else Color(0xFF64748B), Offset(-w * 0.36f, -h * 0.46f), Size(w * 0.72f, h * 0.92f), CornerRadius(12f, 12f), style = Stroke(sw))
            val lensR = (min(w * 0.24f, h * 0.125f)).coerceAtLeast(3f)
            val lenses = listOf(
                Triple(-h * 0.28f, Color(0xFFFF2E3B), rLit),
                Triple(0f, Color(0xFFFFEA00), yLit),
                Triple(h * 0.28f, Color(0xFF00F076), gLit)
            )
            lenses.forEach { (ly, col, lit) ->
                drawLine(bodyStroke, Offset(-w / 2f, ly), Offset(-w * 0.36f, ly), sw * 0.8f)
                if (lit && !compactPreview) {
                    val pulse = 1f + 0.10f * sin(pulsePhase * 2 * PI).toFloat()
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(col.copy(alpha = 0.75f), col.copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(0f, ly),
                            radius = lensR * 2.2f * pulse
                        ),
                        radius = lensR * 2.2f * pulse,
                        center = Offset(0f, ly)
                    )
                }
                drawCircle(
                    color = if (lit) col else col.copy(alpha = 0.18f),
                    radius = lensR,
                    center = Offset(0f, ly)
                )
                drawCircle(
                    color = if (lit) Color.White.copy(alpha = 0.85f) else Color(0xFF334155),
                    radius = lensR,
                    center = Offset(0f, ly),
                    style = Stroke(sw * 0.65f)
                )
            }
            drawLine(bodyStroke, Offset(w * 0.36f, h * 0.28f), Offset(w / 2f, h * 0.28f), sw * 0.8f)
        }

        ComponentType.BUZZER -> {
            val active = simRunning && (
                digitalResult.pinLevels[NetlistBuilder.pinKey(comp.id, "pos")] == LogicLevel.ONE ||
                    abs((analogResult.pinVoltages[NetlistBuilder.pinKey(comp.id, "pos")] ?: 0.0) -
                        (analogResult.pinVoltages[NetlistBuilder.pinKey(comp.id, "neg")] ?: 0.0)) > 1.8
                )
            val rad = min(w, h) * 0.36f
            if (active && !compactPreview) {
                val bloomR = rad * (1.9f + 0.25f * sin(pulsePhase * 2 * PI).toFloat())
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(SignalAmber.copy(alpha = 0.55f), Color.Transparent),
                        center = Offset.Zero,
                        radius = bloomR
                    ),
                    radius = bloomR
                )
            }
            drawCircle(cardFill, radius = rad)
            drawCircle(if (active) LogicHighGreen else SignalAmber, radius = rad, style = Stroke(sw))
            drawLine(bodyStroke, Offset(-w / 2f, 0f), Offset(-rad, 0f), sw)
            drawLine(bodyStroke, Offset(rad, 0f), Offset(w / 2f, 0f), sw)
            val waveCol = if (active) LogicHighGreen else mutedColor
            drawArc(waveCol, -50f, 100f, false, Offset(-min(w, h) * 0.10f, -min(w, h) * 0.20f), Size(min(w, h) * 0.4f, min(w, h) * 0.4f), style = Stroke(sw * 0.9f))
            drawArc(waveCol, -50f, 100f, false, Offset(-min(w, h) * 0.02f, -min(w, h) * 0.28f), Size(min(w, h) * 0.56f, min(w, h) * 0.56f), style = Stroke(sw * 0.8f))
        }

        ComponentType.MOTOR -> {
            val r = min(w, h) * 0.38f
            drawCircle(cardFill, radius = r)
            drawCircle(SignalCyan, radius = r, style = Stroke(sw))
            val v = abs((analogResult.pinVoltages[NetlistBuilder.pinKey(comp.id, "pos")] ?: 0.0) -
                (analogResult.pinVoltages[NetlistBuilder.pinKey(comp.id, "neg")] ?: 0.0))
            if (simRunning && v > 0.2) {
                val angle = pulsePhase * 2 * PI
                drawLine(SignalAmber, Offset.Zero, Offset((r * 0.72f * cos(angle)).toFloat(), (r * 0.72f * sin(angle)).toFloat()), sw)
            } else {
                drawLine(SignalCyan, Offset(-r * 0.4f, r * 0.25f), Offset(0f, -r * 0.3f), sw * 0.9f)
                drawLine(SignalCyan, Offset(0f, -r * 0.3f), Offset(r * 0.4f, r * 0.25f), sw * 0.9f)
            }
        }

        ComponentType.OSCILLOSCOPE -> {
            drawRoundRect(recessedFill, Offset(-w / 2f, -h / 2f), Size(w, h), CornerRadius(12f, 12f))
            drawRoundRect(SignalCyan, Offset(-w / 2f, -h / 2f), Size(w, h), CornerRadius(12f, 12f), style = Stroke(sw))
            drawRect(Color(0xFF0A2229), Offset(-w * 0.38f, -h * 0.32f), Size(w * 0.76f, h * 0.64f))
            val scopeWave = Path().apply {
                moveTo(-w * 0.34f, 0f)
                quadraticTo(-w * 0.17f, -h * 0.36f, 0f, 0f)
                quadraticTo(w * 0.17f, h * 0.36f, w * 0.34f, 0f)
            }
            drawPath(scopeWave, LogicHighGreen, style = Stroke(sw * 0.9f, cap = StrokeCap.Round))
        }

        ComponentType.LOGIC_ANALYZER -> {
            drawRoundRect(recessedFill, Offset(-w / 2f, -h / 2f), Size(w, h), CornerRadius(12f, 12f))
            drawRoundRect(SignalCyan, Offset(-w / 2f, -h / 2f), Size(w, h), CornerRadius(12f, 12f), style = Stroke(sw))
            for (row in 0..2) {
                val ry = -h * 0.25f + row * h * 0.25f
                val pulse = Path().apply {
                    moveTo(-w * 0.34f, ry + 5f)
                    lineTo(-w * 0.15f, ry + 5f)
                    lineTo(-w * 0.15f, ry - 5f)
                    lineTo(w * 0.10f, ry - 5f)
                    lineTo(w * 0.10f, ry + 5f)
                    lineTo(w * 0.34f, ry + 5f)
                }
                drawPath(pulse, if (row == 0) LogicHighGreen else SignalCyan, style = Stroke(sw * 0.8f))
            }
        }

        else -> {
            val isIc = meta.category == ComponentCategory.IC_7400
            if (isIc && meta.pins.any { it.pinNumber != null }) {
                drawNumberedDipIc(
                    comp = comp,
                    w = w,
                    h = h,
                    gridPx = gridPx,
                    isSelected = isSelected,
                    isDark = isDark,
                    simRunning = simRunning,
                    digitalResult = digitalResult,
                    textMeasurer = textMeasurer,
                    compactPreview = compactPreview
                )
                return
            }
            val isInst = meta.category == ComponentCategory.INSTRUMENTS
            val accent = when {
                isIc -> CopperGold
                isInst -> SignalCyan
                else -> bodyStroke
            }
            val bodyW = if (isIc) w * 0.72f else w
            val bodyH = if (isIc) h * 0.92f else h
            drawRoundRect(
                brush = Brush.verticalGradient(
                    if (isIc) listOf(Color(0xFF19243B), Color(0xFF0A101D)) else listOf(cardFill, recessedFill.copy(alpha = 0.45f))
                ),
                topLeft = Offset(-bodyW / 2f, -bodyH / 2f),
                size = Size(bodyW, bodyH),
                cornerRadius = CornerRadius(10f, 10f)
            )
            drawRoundRect(accent, Offset(-bodyW / 2f, -bodyH / 2f), Size(bodyW, bodyH), CornerRadius(10f, 10f), style = Stroke(sw))
            val codeText = when (comp.type) {
                ComponentType.VOLTMETER -> {
                    val v = (analogResult.pinVoltages[NetlistBuilder.pinKey(comp.id, "pos")] ?: 0.0) -
                        (analogResult.pinVoltages[NetlistBuilder.pinKey(comp.id, "neg")] ?: 0.0)
                    if (simRunning) String.format("%.2fV", v) else "V"
                }
                ComponentType.MULTIMETER -> {
                    val v = (analogResult.pinVoltages[NetlistBuilder.pinKey(comp.id, "pos")] ?: 0.0) -
                        (analogResult.pinVoltages[NetlistBuilder.pinKey(comp.id, "com")] ?: 0.0)
                    if (simRunning) String.format("%.2fV", v) else "DMM"
                }
                ComponentType.AMMETER -> {
                    val i = analogResult.componentCurrents[comp.id] ?: 0.0
                    if (simRunning) ComponentCatalog.formatEngineeringValue(i, "A") else "A"
                }
                ComponentType.OHMMETER -> {
                    val r = analogResult.equivalentResistanceOhms
                    if (simRunning && r != null) ComponentCatalog.formatEngineeringValue(r, "Ω") else "Ω"
                }
                else -> meta.shortCode
            }
            if (!compactPreview) {
                val res = textMeasurer.measure(
                    codeText,
                    TextStyle(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = accent)
                )
                drawText(res, topLeft = Offset(-res.size.width / 2f, -res.size.height / 2f))
            } else {
                drawLine(accent, Offset(-bodyW * 0.25f, 0f), Offset(bodyW * 0.25f, 0f), sw)
            }
        }
    }
}

private fun DrawScope.drawArrowTip(tip: Offset, from: Offset, color: Color, sizePx: Float) {
    val angle = kotlin.math.atan2((tip.y - from.y).toDouble(), (tip.x - from.x).toDouble())
    val wing = 0.52
    val p1 = Offset(
        tip.x - (sizePx * cos(angle - wing)).toFloat(),
        tip.y - (sizePx * sin(angle - wing)).toFloat()
    )
    val p2 = Offset(
        tip.x - (sizePx * cos(angle + wing)).toFloat(),
        tip.y - (sizePx * sin(angle + wing)).toFloat()
    )
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        close()
    }
    drawPath(path, color)
}

private fun hexToSevenSegMask(ch: Char): Int = when (ch.uppercaseChar()) {
    '0' -> 0x3F; '1' -> 0x06; '2' -> 0x5B; '3' -> 0x4F; '4' -> 0x66
    '5' -> 0x6D; '6' -> 0x7D; '7' -> 0x07; '8' -> 0x7F; '9' -> 0x6F
    'A' -> 0x77; 'B' -> 0x7C; 'C' -> 0x39; 'D' -> 0x5E; 'E' -> 0x79; 'F' -> 0x71
    else -> 0x40
}

private fun DrawScope.drawSevenSegmentBars(
    bw: Float,
    bh: Float,
    mask: Int,
    litColor: Color,
    strokeW: Float,
    glow: Boolean = false
) {
    val hw = bw / 2f
    val hh = bh / 2f
    val dimColor = Color(0xFF1E293B)
    val segW = strokeW * 1.35f
    fun seg(bit: Int, p1: Offset, p2: Offset) {
        val lit = (mask and (1 shl bit)) != 0
        if (lit && glow) {
            drawLine(litColor.copy(alpha = 0.35f), p1, p2, segW * 2.4f, StrokeCap.Round)
        }
        drawLine(if (lit) litColor else dimColor, p1, p2, segW, StrokeCap.Round)
    }
    seg(0, Offset(-hw * 0.7f, -hh), Offset(hw * 0.7f, -hh)) // a
    seg(1, Offset(hw, -hh * 0.85f), Offset(hw, -hh * 0.12f)) // b
    seg(2, Offset(hw, hh * 0.12f), Offset(hw, hh * 0.85f)) // c
    seg(3, Offset(-hw * 0.7f, hh), Offset(hw * 0.7f, hh)) // d
    seg(4, Offset(-hw, hh * 0.12f), Offset(-hw, hh * 0.85f)) // e
    seg(5, Offset(-hw, -hh * 0.85f), Offset(-hw, -hh * 0.12f)) // f
    seg(6, Offset(-hw * 0.7f, 0f), Offset(hw * 0.7f, 0f)) // g
}

private fun DrawScope.drawLogicGateSymbol(
    type: ComponentType,
    w: Float,
    h: Float,
    gridPx: Float,
    inputPins: List<com.example.model.PinSpec>,
    fill: Color,
    stroke: Color,
    sw: Float
) {
    val left = -w * 0.34f
    val right = w * 0.30f
    val top = -h * 0.42f
    val bottom = h * 0.42f

    if (type == ComponentType.GATE_NOT || type == ComponentType.GATE_BUFFER || type == ComponentType.GATE_TRISTATE) {
        drawLine(stroke, Offset(-w / 2f, 0f), Offset(left, 0f), sw)
    } else if (inputPins.isNotEmpty()) {
        val isCurvedBack = type == ComponentType.GATE_OR || type == ComponentType.GATE_NOR ||
            type == ComponentType.GATE_XOR || type == ComponentType.GATE_XNOR
        inputPins.forEach { pin ->
            val py = pin.relY * gridPx
            val endX = if (isCurvedBack) left + 4f else left
            drawLine(stroke, Offset(-w / 2f, py), Offset(endX, py), sw)
        }
        // Extend vertical input bus bar when > 4 inputs exceed gate body wall
        val minY = inputPins.minOf { it.relY * gridPx }
        val maxY = inputPins.maxOf { it.relY * gridPx }
        if (minY < top || maxY > bottom) {
            drawLine(stroke, Offset(left, minOf(minY, top)), Offset(left, maxOf(maxY, bottom)), sw)
        }
    } else {
        drawLine(stroke, Offset(-w / 2f, -gridPx), Offset(left, -gridPx), sw)
        drawLine(stroke, Offset(-w / 2f, gridPx), Offset(left, gridPx), sw)
    }

    val path = Path()
    when (type) {
        ComponentType.GATE_AND, ComponentType.GATE_NAND -> {
            path.moveTo(left, top)
            path.lineTo(0f, top)
            path.arcTo(
                rect = androidx.compose.ui.geometry.Rect(-right, top, right, bottom),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            path.lineTo(left, bottom)
            path.close()
        }
        ComponentType.GATE_OR, ComponentType.GATE_NOR, ComponentType.GATE_XOR, ComponentType.GATE_XNOR -> {
            val bodyLeft = if (type == ComponentType.GATE_XOR || type == ComponentType.GATE_XNOR) left + 8f else left
            path.moveTo(bodyLeft, top)
            path.quadraticTo(right * 0.5f, top, right, 0f)
            path.quadraticTo(right * 0.5f, bottom, bodyLeft, bottom)
            path.quadraticTo(bodyLeft + 14f, 0f, bodyLeft, top)
            path.close()
            if (type == ComponentType.GATE_XOR || type == ComponentType.GATE_XNOR) {
                val arc = Path().apply {
                    moveTo(left - 2f, top)
                    quadraticTo(left + 12f, 0f, left - 2f, bottom)
                }
                drawPath(arc, stroke, style = Stroke(sw))
            }
        }
        ComponentType.GATE_NOT, ComponentType.GATE_BUFFER, ComponentType.GATE_TRISTATE -> {
            path.moveTo(left, top)
            path.lineTo(right, 0f)
            path.lineTo(left, bottom)
            path.close()
            if (type == ComponentType.GATE_TRISTATE) {
                drawLine(SignalAmber, Offset(0f, -h / 2f), Offset(0f, -h * 0.21f), sw)
            }
        }
        else -> Unit
    }

    drawPath(path, fill)
    drawPath(path, stroke, style = Stroke(sw))

    val hasBubble = type == ComponentType.GATE_NOT || type == ComponentType.GATE_NAND ||
        type == ComponentType.GATE_NOR || type == ComponentType.GATE_XNOR
    val bubbleR = (sw * 1.9f).coerceIn(3.5f, 6.5f)
    if (hasBubble) {
        drawCircle(fill, radius = bubbleR, center = Offset(right + bubbleR, 0f))
        drawCircle(stroke, radius = bubbleR, center = Offset(right + bubbleR, 0f), style = Stroke(sw))
        drawLine(stroke, Offset(right + bubbleR * 2f, 0f), Offset(w / 2f, 0f), sw)
    } else {
        drawLine(stroke, Offset(right, 0f), Offset(w / 2f, 0f), sw)
    }
}

package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay

@Composable
fun PreOpeningSplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logoScale = remember { Animatable(0.76f) }
    val contentAlpha = remember { Animatable(0f) }
    val bootProgress = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_circuit_pulse")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_phase"
    )
    val glowBreath by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_breath"
    )

    LaunchedEffect(Unit) {
        contentAlpha.animateTo(1f, animationSpec = tween(420, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        logoScale.animateTo(1f, animationSpec = tween(620, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        bootProgress.animateTo(1f, animationSpec = tween(2350, easing = FastOutSlowInEasing))
        delay(180)
        onFinished()
    }

    val statusLabel = when {
        bootProgress.value < 0.36f -> "Initializing Digital & Analog Engines..."
        bootProgress.value < 0.74f -> "Loading 74xx ICs & Virtual Instruments..."
        else -> "GateX Labs Workbench Ready"
    }

    val cyanNeon = Color(0xFF00E5FF)
    val goldAccent = Color(0xFFFFB300)
    val deepNavy = Color(0xFF02091A)
    val midNavy = Color(0xFF061B3A)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(midNavy, deepNavy, Color(0xFF010612)),
                    radius = 1200f
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFinished
            )
            .testTag("pre_opening_splash"),
        contentAlignment = Alignment.Center
    ) {
        // Animated PCB Neon Circuit Traces Backdrop
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val traceColor = cyanNeon.copy(alpha = 0.14f)
            val nodeColor = cyanNeon.copy(alpha = 0.28f)

            val tracePaths = listOf(
                listOf(Offset(0f, h * 0.18f), Offset(w * 0.24f, h * 0.18f), Offset(w * 0.34f, h * 0.26f), Offset(w * 0.34f, h * 0.36f)),
                listOf(Offset(w, h * 0.22f), Offset(w * 0.74f, h * 0.22f), Offset(w * 0.65f, h * 0.31f), Offset(w * 0.65f, h * 0.40f)),
                listOf(Offset(0f, h * 0.78f), Offset(w * 0.26f, h * 0.78f), Offset(w * 0.36f, h * 0.68f), Offset(w * 0.36f, h * 0.60f)),
                listOf(Offset(w, h * 0.82f), Offset(w * 0.72f, h * 0.82f), Offset(w * 0.62f, h * 0.72f), Offset(w * 0.62f, h * 0.62f)),
                listOf(Offset(w * 0.12f, 0f), Offset(w * 0.12f, h * 0.12f), Offset(w * 0.22f, h * 0.12f)),
                listOf(Offset(w * 0.88f, h), Offset(w * 0.88f, h * 0.88f), Offset(w * 0.78f, h * 0.88f))
            )

            tracePaths.forEachIndexed { idx, pts ->
                val p = Path().apply {
                    moveTo(pts.first().x, pts.first().y)
                    for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
                }
                drawPath(p, traceColor, style = Stroke(width = 3f, cap = StrokeCap.Round))
                drawCircle(nodeColor, radius = 5.5f, center = pts.last())

                // Animated traveling signal dot along trace
                val t = (pulsePhase + idx * 0.17f) % 1f
                val segCount = (pts.size - 1).coerceAtLeast(1)
                val scaled = t * segCount
                val segIdx = scaled.toInt().coerceIn(0, segCount - 1)
                val segFrac = scaled - segIdx
                val a = pts[segIdx]
                val b = pts[segIdx + 1]
                val dot = Offset(a.x + (b.x - a.x) * segFrac, a.y + (b.y - a.y) * segFrac)
                drawCircle(cyanNeon.copy(alpha = 0.26f), radius = 12f, center = dot)
                drawCircle(cyanNeon.copy(alpha = 0.85f), radius = 4.2f, center = dot)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .padding(horizontal = 28.dp)
                .alpha(contentAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Glowing Logo Frame
            Box(
                modifier = Modifier
                    .scale(logoScale.value)
                    .size(164.dp),
                contentAlignment = Alignment.Center
            ) {
                // Soft outer neon halo
                Box(
                    modifier = Modifier
                        .size(164.dp)
                        .clip(RoundedCornerShape(36.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    cyanNeon.copy(alpha = 0.34f * glowBreath),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Image(
                    painter = painterResource(id = R.drawable.gatex_labs_icon_1794944999367),
                    contentDescription = "GateX Labs Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(144.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(listOf(cyanNeon, goldAccent, cyanNeon)),
                            shape = RoundedCornerShape(30.dp)
                        )
                )
            }

            Spacer(Modifier.height(4.dp))

            // Brand Wordmark
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFFF1F5F9), fontWeight = FontWeight.ExtraBold)) {
                        append("Gate")
                    }
                    withStyle(SpanStyle(color = goldAccent, fontWeight = FontWeight.Black)) {
                        append("X ")
                    }
                    withStyle(SpanStyle(color = cyanNeon, fontWeight = FontWeight.Bold)) {
                        append("Labs")
                    }
                },
                fontSize = 34.sp,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Digital Logic Design & Circuit Simulation",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFBAE6FD),
                textAlign = TextAlign.Center
            )

            // Feature Pill Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SplashBadge("74xx ICs", cyanNeon)
                SplashBadge("MNA Analog", goldAccent)
                SplashBadge("Breadboard", Color(0xFF4ADE80))
            }

            Spacer(Modifier.height(14.dp))

            // Smooth Neon Boot Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0F2547))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(bootProgress.value.coerceIn(0.05f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(listOf(cyanNeon, Color(0xFF38BDF8), goldAccent))
                        )
                )
            }

            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            // Developer & University Credit Card
            Surface(
                color = Color(0xCC061630),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(listOf(cyanNeon.copy(alpha = 0.65f), goldAccent.copy(alpha = 0.75f))),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .testTag("splash_developer_credit")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "DEVELOPED BY STUDENT OF",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.2.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "SBBU UNIVERSITY NAWABSHAH",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = cyanNeon,
                        letterSpacing = 0.8.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "SALMAN 26BSIT-70",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = goldAccent,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Subtle Tap-to-Enter cue at bottom
        Text(
            text = "Tap anywhere to enter workbench",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF64748B),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
                .alpha(contentAlpha.value * 0.85f)
        )
    }
}

@Composable
private fun SplashBadge(label: String, accent: Color) {
    Surface(
        color = accent.copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

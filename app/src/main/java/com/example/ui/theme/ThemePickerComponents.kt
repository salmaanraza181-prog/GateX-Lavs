package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ThemePaletteGrid(
    selectedThemeId: String,
    onSelectTheme: (AppThemePreset) -> Unit,
    presets: List<AppThemePreset> = AppThemePreset.entries,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        presets.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { preset ->
                    ThemePresetCard(
                        preset = preset,
                        selected = preset.id == selectedThemeId,
                        onClick = { onSelectTheme(preset) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ThemePresetCard(
    preset: AppThemePreset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = preset.colorScheme
    val canvasPalette = preset.canvasColors
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
    val borderWidth = if (selected) 2.2.dp else 1.dp

    Surface(
        color = scheme.surface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = if (selected) 4.dp else 1.dp,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("theme_card_${preset.id}")
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            // Live miniature schematic workbench preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, scheme.outline.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(canvasPalette.backdropCenter, canvasPalette.backdropEdge),
                            center = Offset(size.width * 0.5f, size.height * 0.5f),
                            radius = size.maxDimension * 0.85f
                        )
                    )
                    val step = 14f
                    var gx = step
                    while (gx < size.width) {
                        drawLine(canvasPalette.gridMinor, Offset(gx, 0f), Offset(gx, size.height), 0.8f)
                        gx += step
                    }
                    var gy = step
                    while (gy < size.height) {
                        drawLine(canvasPalette.gridMinor, Offset(0f, gy), Offset(size.width, gy), 0.8f)
                        gy += step
                    }

                    // Delicate decorative 5-petal flower watermark for Floral themes
                    if (preset.category == ThemeCategory.FLORAL) {
                        val fc = Offset(size.width * 0.82f, size.height * 0.72f)
                        for (i in 0 until 5) {
                            val angle = (i * 72.0 - 90.0) * PI / 180.0
                            val pc = Offset(
                                x = fc.x + (cos(angle) * 9.5).toFloat(),
                                y = fc.y + (sin(angle) * 9.5).toFloat()
                            )
                            drawCircle(scheme.primary.copy(alpha = 0.34f), radius = 6.2f, center = pc)
                        }
                        drawCircle(scheme.secondary.copy(alpha = 0.8f), radius = 4.2f, center = fc)
                    } else if (preset.category == ThemeCategory.AESTHETIC) {
                        // Soft dreamy bokeh circles for Aesthetic themes
                        drawCircle(scheme.secondary.copy(alpha = 0.22f), radius = 16f, center = Offset(size.width * 0.84f, size.height * 0.76f))
                        drawCircle(scheme.tertiary.copy(alpha = 0.22f), radius = 11f, center = Offset(size.width * 0.18f, size.height * 0.78f))
                    }

                    // Mini routed schematic trace
                    val wirePath = Path().apply {
                        moveTo(size.width * 0.12f, size.height * 0.38f)
                        lineTo(size.width * 0.36f, size.height * 0.38f)
                        lineTo(size.width * 0.36f, size.height * 0.52f)
                        lineTo(size.width * 0.68f, size.height * 0.52f)
                        lineTo(size.width * 0.68f, size.height * 0.34f)
                        lineTo(size.width * 0.84f, size.height * 0.34f)
                    }
                    drawPath(
                        path = wirePath,
                        color = canvasPalette.defaultWire.copy(alpha = 0.30f),
                        style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = wirePath,
                        color = canvasPalette.defaultWire,
                        style = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Mini DIP IC block
                    val chipW = size.width * 0.24f
                    val chipH = size.height * 0.48f
                    val chipLeft = size.width * 0.38f
                    val chipTop = size.height * 0.26f
                    drawRoundRect(
                        color = scheme.surfaceVariant,
                        topLeft = Offset(chipLeft, chipTop),
                        size = Size(chipW, chipH),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = scheme.primary,
                        topLeft = Offset(chipLeft, chipTop),
                        size = Size(chipW, chipH),
                        cornerRadius = CornerRadius(4f, 4f),
                        style = Stroke(1.4f)
                    )

                    // Glowing probe indicator dot on right
                    val probeCenter = Offset(size.width * 0.84f, size.height * 0.34f)
                    drawCircle(scheme.secondary.copy(alpha = 0.35f), radius = 10f, center = probeCenter)
                    drawCircle(scheme.secondary, radius = 5.2f, center = probeCenter)
                    drawCircle(Color.White.copy(alpha = 0.85f), radius = 1.8f, center = Offset(probeCenter.x - 1.5f, probeCenter.y - 1.5f))

                    // Input switch pad on left
                    val inCenter = Offset(size.width * 0.14f, size.height * 0.38f)
                    drawCircle(scheme.tertiary, radius = 4.2f, center = inCenter)
                }

                // Category / Mode Pill
                Surface(
                    color = scheme.surface.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        val badgeIcon = when (preset.category) {
                            ThemeCategory.FLORAL -> Icons.Default.LocalFlorist
                            ThemeCategory.AESTHETIC -> Icons.Default.AutoAwesome
                            ThemeCategory.LAB -> if (preset.isDark) Icons.Default.DarkMode else Icons.Default.LightMode
                        }
                        val badgeText = when (preset.category) {
                            ThemeCategory.FLORAL -> "FLORAL"
                            ThemeCategory.AESTHETIC -> "AESTHETIC"
                            ThemeCategory.LAB -> if (preset.isDark) "DARK" else "LIGHT"
                        }
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = badgeText,
                            tint = scheme.primary,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (selected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Active theme",
                        tint = scheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                            .size(18.dp)
                            .background(scheme.surface, CircleShape)
                    )
                }
            }

            // Title, tagline & accent swatch trio
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = preset.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(scheme.primary)
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(scheme.secondary)
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(scheme.tertiary)
                        )
                    }
                }
                Text(
                    text = preset.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ThemePickerSheet(
    selectedThemeId: String,
    onSelectTheme: (AppThemePreset) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = AppThemePreset.fromId(selectedThemeId)
    var selectedCategory by remember { mutableStateOf<ThemeCategory?>(null) }

    val filteredPresets = remember(selectedCategory) {
        if (selectedCategory == null) AppThemePreset.entries
        else AppThemePreset.entries.filter { it.category == selectedCategory }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "GateX Theme Studio",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Active: ${active.displayName} • ${AppThemePreset.entries.size} Themes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.testTag("theme_sheet_close_btn")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close theme studio")
            }
        }

        // Category Filter Bar: All / Flowers / Aesthetic / Tech & Lab
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("All (${AppThemePreset.entries.size})") },
                modifier = Modifier.testTag("theme_cat_all")
            )
            FilterChip(
                selected = selectedCategory == ThemeCategory.FLORAL,
                onClick = { selectedCategory = ThemeCategory.FLORAL },
                leadingIcon = { Icon(Icons.Default.LocalFlorist, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Flowers (${AppThemePreset.entries.count { it.category == ThemeCategory.FLORAL }})") },
                modifier = Modifier.testTag("theme_cat_floral")
            )
            FilterChip(
                selected = selectedCategory == ThemeCategory.AESTHETIC,
                onClick = { selectedCategory = ThemeCategory.AESTHETIC },
                leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Aesthetic (${AppThemePreset.entries.count { it.category == ThemeCategory.AESTHETIC }})") },
                modifier = Modifier.testTag("theme_cat_aesthetic")
            )
            FilterChip(
                selected = selectedCategory == ThemeCategory.LAB,
                onClick = { selectedCategory = ThemeCategory.LAB },
                leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Tech & Lab (${AppThemePreset.entries.count { it.category == ThemeCategory.LAB }})") },
                modifier = Modifier.testTag("theme_cat_lab")
            )
        }

        ThemePaletteGrid(
            selectedThemeId = selectedThemeId,
            onSelectTheme = onSelectTheme,
            presets = filteredPresets
        )

        Spacer(Modifier.height(6.dp))
    }
}

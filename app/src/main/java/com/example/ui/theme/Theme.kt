package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

val LabShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp),
)

@Composable
fun SalmanDigitalTheme(
    themePreset: AppThemePreset = AppThemePreset.MIDNIGHT_LAB,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalLabCanvasColors provides themePreset.canvasColors,
        LocalAppThemePreset provides themePreset
    ) {
        MaterialTheme(
            colorScheme = themePreset.colorScheme,
            typography = SalmanTypography,
            shapes = LabShapes,
            content = content,
        )
    }
}

@Composable
fun SalmanDigitalTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val preset = if (darkTheme) AppThemePreset.MIDNIGHT_LAB else AppThemePreset.ENGINEERING_BLUEPRINT
    SalmanDigitalTheme(themePreset = preset, content = content)
}

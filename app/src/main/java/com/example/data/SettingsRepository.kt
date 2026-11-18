package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AppThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "salman_digital_settings")

data class AppSettings(
    val themeId: String = AppThemePreset.MIDNIGHT_LAB.id,
    val isDarkTheme: Boolean = true,
    val language: String = "English",
    val screenOrientation: String = "PORTRAIT",
    val showGrid: Boolean = true,
    val snapToGrid: Boolean = true,
    val gridSpacingDp: Float = 24f,
    val simulationSpeedMs: Long = 500L,
    val autosaveEnabled: Boolean = true,
    val showComponentLabels: Boolean = true,
    val showComponentValues: Boolean = true,
    val soundEnabled: Boolean = true
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val THEME_ID = stringPreferencesKey("theme_id")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val LANGUAGE = stringPreferencesKey("language")
        val SCREEN_ORIENTATION = stringPreferencesKey("screen_orientation")
        val SHOW_GRID = booleanPreferencesKey("show_grid")
        val SNAP_TO_GRID = booleanPreferencesKey("snap_to_grid")
        val GRID_SPACING = floatPreferencesKey("grid_spacing")
        val SIM_SPEED_MS = longPreferencesKey("sim_speed_ms")
        val AUTOSAVE = booleanPreferencesKey("autosave")
        val SHOW_LABELS = booleanPreferencesKey("show_labels")
        val SHOW_VALUES = booleanPreferencesKey("show_values")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        val legacyDark = prefs[Keys.DARK_THEME] ?: true
        val preset = AppThemePreset.fromId(prefs[Keys.THEME_ID], fallbackDark = legacyDark)
        AppSettings(
            themeId = preset.id,
            isDarkTheme = preset.isDark,
            language = prefs[Keys.LANGUAGE] ?: "English",
            screenOrientation = prefs[Keys.SCREEN_ORIENTATION] ?: "PORTRAIT",
            showGrid = prefs[Keys.SHOW_GRID] ?: true,
            snapToGrid = prefs[Keys.SNAP_TO_GRID] ?: true,
            gridSpacingDp = prefs[Keys.GRID_SPACING] ?: 24f,
            simulationSpeedMs = prefs[Keys.SIM_SPEED_MS] ?: 500L,
            autosaveEnabled = prefs[Keys.AUTOSAVE] ?: true,
            showComponentLabels = prefs[Keys.SHOW_LABELS] ?: true,
            showComponentValues = prefs[Keys.SHOW_VALUES] ?: true,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsDataStore.edit { prefs ->
            val legacyDark = prefs[Keys.DARK_THEME] ?: true
            val currentPreset = AppThemePreset.fromId(prefs[Keys.THEME_ID], fallbackDark = legacyDark)
            val current = AppSettings(
                themeId = currentPreset.id,
                isDarkTheme = currentPreset.isDark,
                language = prefs[Keys.LANGUAGE] ?: "English",
                screenOrientation = prefs[Keys.SCREEN_ORIENTATION] ?: "PORTRAIT",
                showGrid = prefs[Keys.SHOW_GRID] ?: true,
                snapToGrid = prefs[Keys.SNAP_TO_GRID] ?: true,
                gridSpacingDp = prefs[Keys.GRID_SPACING] ?: 24f,
                simulationSpeedMs = prefs[Keys.SIM_SPEED_MS] ?: 500L,
                autosaveEnabled = prefs[Keys.AUTOSAVE] ?: true,
                showComponentLabels = prefs[Keys.SHOW_LABELS] ?: true,
                showComponentValues = prefs[Keys.SHOW_VALUES] ?: true,
                soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true
            )
            val next = transform(current)
            val resolvedPreset = AppThemePreset.fromId(next.themeId, fallbackDark = next.isDarkTheme)
            prefs[Keys.THEME_ID] = resolvedPreset.id
            prefs[Keys.DARK_THEME] = resolvedPreset.isDark
            prefs[Keys.LANGUAGE] = next.language
            prefs[Keys.SCREEN_ORIENTATION] = next.screenOrientation
            prefs[Keys.SHOW_GRID] = next.showGrid
            prefs[Keys.SNAP_TO_GRID] = next.snapToGrid
            prefs[Keys.GRID_SPACING] = next.gridSpacingDp
            prefs[Keys.SIM_SPEED_MS] = next.simulationSpeedMs
            prefs[Keys.AUTOSAVE] = next.autosaveEnabled
            prefs[Keys.SHOW_LABELS] = next.showComponentLabels
            prefs[Keys.SHOW_VALUES] = next.showComponentValues
            prefs[Keys.SOUND_ENABLED] = next.soundEnabled
        }
    }
}

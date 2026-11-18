package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CircuitTemplates
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.AppThemePreset
import com.example.viewmodel.WorkbenchViewModel

@Composable
fun SettingsScreen(
    vm: WorkbenchViewModel,
    onOpenThemePicker: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val activeTheme = AppThemePreset.fromId(settings.themeId, fallbackDark = settings.isDarkTheme)
    val strings = LocalAppStrings.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(strings.settingsTitle, style = MaterialTheme.typography.headlineMedium)

        SettingsSection(strings.appThemesSection) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenThemePicker)
                    .testTag("settings_open_themes_btn")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Open Theme Studio",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Themes (${AppThemePreset.entries.size} Presets)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Active: ${activeTheme.displayName} • ${activeTheme.category.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Flowers, Aesthetic, Dark & Light Lab themes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(activeTheme.colorScheme.primary)
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(activeTheme.colorScheme.secondary)
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(activeTheme.colorScheme.tertiary)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Browse themes",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            SettingSwitchRow(
                title = "Quick Dark Mode Toggle",
                subtitle = "Instantly flip between Dark and Light workbench presets",
                checked = activeTheme.isDark,
                tag = "setting_dark_mode"
            ) { dark ->
                val target = if (dark) {
                    if (activeTheme.isDark) activeTheme else AppThemePreset.MIDNIGHT_LAB
                } else {
                    if (!activeTheme.isDark) activeTheme else AppThemePreset.ENGINEERING_BLUEPRINT
                }
                vm.selectTheme(target)
            }
        }

        SettingsSection(strings.orientationLanguageSection) {
            Text("Screen Orientation", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "PORTRAIT" to "Vertical (Default)",
                    "AUTO" to "Auto-Rotate",
                    "LANDSCAPE" to "Horizontal"
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = settings.screenOrientation == mode,
                        onClick = { vm.updateSettings { it.copy(screenOrientation = mode) } },
                        label = { Text(label) },
                        modifier = Modifier.testTag("setting_orientation_${mode.lowercase()}")
                    )
                }
            }

            Text("Language", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("English", "Urdu", "Arabic", "Spanish").forEach { lang ->
                    FilterChip(
                        selected = settings.language == lang,
                        onClick = { vm.updateSettings { it.copy(language = lang) } },
                        label = { Text(lang) },
                        modifier = Modifier.testTag("setting_lang_${lang.lowercase()}")
                    )
                }
            }
        }

        SettingsSection(strings.canvasGridSection) {
            SettingSwitchRow("Show Schematic Grid", "Display major/minor engineering grid lines", settings.showGrid, "setting_show_grid") { v ->
                vm.updateSettings { it.copy(showGrid = v) }
            }
            SettingSwitchRow("Snap to Grid", "Align components and junctions to 0.5 grid steps", settings.snapToGrid, "setting_snap_grid") { v ->
                vm.updateSettings { it.copy(snapToGrid = v) }
            }
            SettingSwitchRow("Show Component Labels", "Render reference designators (R1, U1)", settings.showComponentLabels, "setting_labels") { v ->
                vm.updateSettings { it.copy(showComponentLabels = v) }
            }
            SettingSwitchRow("Show Component Values", "Render resistance, capacitance, and live mA", settings.showComponentValues, "setting_values") { v ->
                vm.updateSettings { it.copy(showComponentValues = v) }
            }
        }

        SettingsSection(strings.simStorageSection) {
            Text("Simulation Tick Interval: ${settings.simulationSpeedMs} ms", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = settings.simulationSpeedMs.toFloat(),
                onValueChange = { ms -> vm.updateSettings { it.copy(simulationSpeedMs = ms.toLong()) } },
                valueRange = 150f..1500f
            )
            SettingSwitchRow(
                title = strings.soundEffectsTitle,
                subtitle = strings.soundEffectsSubtitle,
                checked = settings.soundEnabled,
                tag = "setting_sound"
            ) { v ->
                vm.updateSettings { it.copy(soundEnabled = v) }
            }
            SettingSwitchRow("Automatic Project Recovery", "Save unsaved edits in background Room draft", settings.autosaveEnabled, "setting_autosave") { v ->
                vm.updateSettings { it.copy(autosaveEnabled = v) }
            }
        }

        SettingsSection(strings.aboutSection) {
            Text("GATEX LABS — Electronic Workbench & Circuit Simulator", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_developer_credit")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "Developer: Salman (Roll No: 26BSIT-70)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Student of SBBU University Nawabshah",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "Offline-first Android engineering workbench featuring Graph Netlist Analysis, MNA Analog DC/Transient Solver, 7400-Series Digital Simulation, Logic Converter, Word Generator, Op-Amp LM741, Safety Fuse Protection, Virtual Instruments, Breadboard Lab, PCB Designer with DRC, 10 Studio Themes, and optional Gemini AI assistance.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.testTag(tag))
    }
}

@Composable
fun LearningCenterScreen(
    vm: WorkbenchViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lessons = listOf(
        Triple("Ohm's Law & LED Current Limiting", "V = I × R. Always place a series resistor with an LED: R = (Vs - Vf) / If.", "led_resistor"),
        Triple("Voltage Dividers & Potentiometers", "Two series resistors divide voltage proportionally: Vout = Vin × R2 / (R1 + R2).", "voltage_divider"),
        Triple("RC Charging & Time Constants", "A capacitor charges through R with τ = R × C, reaching 63.2% at 1τ and 99.3% at 5τ.", "rc_charging"),
        Triple("Combinational Logic: Half & Full Adder", "XOR computes Sum (A⊕B); AND+OR gates compute Carry propagation.", "full_adder"),
        Triple("Sequential Memory: Latches & Flip-Flops", "Cross-coupled NOR gates form SR Latches; 7474/7476 Flip-Flops sample on rising clock edges.", "jk_flip_flop"),
        Triple("NE555 Astable Timer", "Generates continuous digital clock pulses whose frequency is set by external RC timing.", "timer_555")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column {
                Text("Learning Center", style = MaterialTheme.typography.headlineMedium)
                Text("Interactive electronics curriculum", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        lessons.forEach { (title, body, templateId) ->
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                    Text(body, style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = {
                        CircuitTemplates.byId(templateId)?.let { vm.openTemplate(it) }
                    }) {
                        Text("Open Interactive Lab Circuit")
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

package com.example.ui.canvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ComponentCatalog
import com.example.model.ComponentType
import com.example.ui.theme.CopperGold
import com.example.ui.theme.LogicHighGreen
import com.example.ui.theme.LogicHighZViolet
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SignalCyan

enum class WorkbenchBin(
    val title: String,
    val icon: ImageVector,
    val accent: Color,
    val types: List<ComponentType>
) {
    SOURCES(
        title = "Sources",
        icon = Icons.Default.Bolt,
        accent = SignalAmber,
        types = listOf(
            ComponentType.DC_SOURCE,
            ComponentType.BATTERY,
            ComponentType.AC_SOURCE,
            ComponentType.VCC,
            ComponentType.GROUND,
            ComponentType.POWER_SUPPLY,
            ComponentType.CURRENT_SOURCE,
            ComponentType.VOLTAGE_REGULATOR
        )
    ),
    BASIC(
        title = "Basic",
        icon = Icons.Default.Tune,
        accent = SignalCyan,
        types = listOf(
            ComponentType.RESISTOR,
            ComponentType.VARIABLE_RESISTOR,
            ComponentType.POTENTIOMETER,
            ComponentType.CAPACITOR,
            ComponentType.POLARIZED_CAPACITOR,
            ComponentType.INDUCTOR,
            ComponentType.TRANSFORMER,
            ComponentType.FUSE,
            ComponentType.NET_LABEL,
            ComponentType.TEXT_NOTE
        )
    ),
    SEMIS(
        title = "Diodes & BJTs",
        icon = Icons.Default.ElectricBolt,
        accent = LogicHighGreen,
        types = listOf(
            ComponentType.DIODE,
            ComponentType.ZENER_DIODE,
            ComponentType.LED,
            ComponentType.RGB_LED,
            ComponentType.PHOTODIODE,
            ComponentType.TRANSISTOR_NPN,
            ComponentType.TRANSISTOR_PNP,
            ComponentType.MOSFET
        )
    ),
    SWITCHES(
        title = "Switches",
        icon = Icons.Default.ToggleOn,
        accent = Color(0xFFFFD54F),
        types = listOf(
            ComponentType.TOGGLE_SWITCH,
            ComponentType.SPDT_SWITCH,
            ComponentType.DPST_SWITCH,
            ComponentType.DPDT_SWITCH,
            ComponentType.PUSH_BUTTON,
            ComponentType.PUSH_BUTTON_NC,
            ComponentType.DIP_SWITCH,
            ComponentType.ROTARY_SWITCH,
            ComponentType.VOLTAGE_CONTROLLED_SWITCH,
            ComponentType.RELAY,
            ComponentType.RELAY_SPDT
        )
    ),
    GATES(
        title = "Logic Gates",
        icon = Icons.Default.DeveloperBoard,
        accent = LogicHighZViolet,
        types = listOf(
            ComponentType.GATE_AND,
            ComponentType.GATE_OR,
            ComponentType.GATE_NOT,
            ComponentType.GATE_NAND,
            ComponentType.GATE_NOR,
            ComponentType.GATE_XOR,
            ComponentType.GATE_XNOR,
            ComponentType.GATE_BUFFER,
            ComponentType.GATE_TRISTATE
        )
    ),
    ICS(
        title = "ICs & 74xx",
        icon = Icons.Default.Memory,
        accent = CopperGold,
        types = listOf(
            ComponentType.IC_7472,
            ComponentType.IC_7400,
            ComponentType.IC_7402,
            ComponentType.IC_7404,
            ComponentType.IC_7408,
            ComponentType.IC_7410,
            ComponentType.IC_7411,
            ComponentType.IC_7420,
            ComponentType.IC_7421,
            ComponentType.IC_7427,
            ComponentType.IC_7430,
            ComponentType.IC_7432,
            ComponentType.IC_7447,
            ComponentType.IC_7473,
            ComponentType.IC_7474,
            ComponentType.IC_7476,
            ComponentType.IC_7483,
            ComponentType.IC_7485,
            ComponentType.IC_7486,
            ComponentType.IC_7490,
            ComponentType.IC_7493,
            ComponentType.IC_74125,
            ComponentType.IC_74138,
            ComponentType.IC_74151,
            ComponentType.IC_74154,
            ComponentType.IC_74161,
            ComponentType.IC_74194,
            ComponentType.IC_74266,
            ComponentType.IC_74595,
            ComponentType.OP_AMP_741,
            ComponentType.IC_555,
            ComponentType.CUSTOM_IC
        )
    ),
    INDICATORS(
        title = "Indicators",
        icon = Icons.Default.Lightbulb,
        accent = Color(0xFFFF5252),
        types = listOf(
            ComponentType.VOLTMETER,
            ComponentType.AMMETER,
            ComponentType.PILOT_LAMP,
            ComponentType.TRAFFIC_LIGHT,
            ComponentType.PROBE_RED,
            ComponentType.PROBE_GREEN,
            ComponentType.PROBE_YELLOW,
            ComponentType.PROBE_BLUE,
            ComponentType.PROBE_ORANGE,
            ComponentType.PROBE_CYAN,
            ComponentType.SEVEN_SEGMENT,
            ComponentType.HEX_DISPLAY,
            ComponentType.LCD_16X2,
            ComponentType.DOT_MATRIX_8X8,
            ComponentType.BUZZER,
            ComponentType.BAR_GRAPH,
            ComponentType.DECODED_BAR_GRAPH,
            ComponentType.LOGIC_INDICATOR,
            ComponentType.LED_INDICATOR,
            ComponentType.DIGITAL_DISPLAY,
            ComponentType.MOTOR,
            ComponentType.RELAY_OUTPUT
        )
    ),
    INPUTS(
        title = "Inputs",
        icon = Icons.Default.ToggleOn,
        accent = Color(0xFF38BDF8),
        types = listOf(
            ComponentType.LOGIC_INPUT,
            ComponentType.CLOCK_INPUT,
            ComponentType.KEYPAD,
            ComponentType.VARIABLE_INPUT
        )
    ),
    INSTRUMENTS(
        title = "Instruments",
        icon = Icons.Default.Speed,
        accent = Color(0xFF2DD4BF),
        types = listOf(
            ComponentType.MULTIMETER,
            ComponentType.VOLTMETER,
            ComponentType.AMMETER,
            ComponentType.OHMMETER,
            ComponentType.OSCILLOSCOPE,
            ComponentType.FUNCTION_GENERATOR,
            ComponentType.LOGIC_ANALYZER,
            ComponentType.FREQUENCY_COUNTER
        )
    )
}

@Composable
fun WorkbenchPartsBin(
    selectedBin: WorkbenchBin?,
    isDark: Boolean,
    onSelectBin: (WorkbenchBin?) -> Unit,
    onPickComponent: (ComponentType) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Compartment Tab Row ("Alag Khane")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WorkbenchBin.entries.forEach { bin ->
                    val active = selectedBin == bin
                    Surface(
                        color = if (active) bin.accent.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (active) 1.5.dp else 1.dp,
                                color = if (active) bin.accent else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectBin(if (active) null else bin) }
                            .testTag("ewb_bin_${bin.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = bin.icon,
                                contentDescription = bin.title,
                                tint = bin.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = bin.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) bin.accent else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${bin.types.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Expanded Compartment Tray showing Visual Schematic Symbol Tiles
            AnimatedVisibility(
                visible = selectedBin != null,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                if (selectedBin != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    ) {
                        HorizontalDivider(color = selectedBin.accent.copy(alpha = 0.45f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 6.dp, top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedBin.title} Bin — Tap any part to place on schematic",
                                style = MaterialTheme.typography.labelSmall,
                                color = selectedBin.accent
                            )
                            IconButton(
                                onClick = { onSelectBin(null) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Collapse ${selectedBin.title} bin",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(selectedBin.types, key = { it.name }) { type ->
                                val meta = ComponentCatalog.get(type)
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .width(102.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, selectedBin.accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                        .clickable { onPickComponent(type) }
                                        .testTag("ewb_part_${type.name}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(46.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isDark) Color(0xFF0A101D) else Color(0xFFF1F5F9)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            ComponentSymbolThumbnail(
                                                type = type,
                                                isDark = isDark,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(42.dp)
                                            )
                                        }
                                        Spacer(Modifier.height(5.dp))
                                        Text(
                                            text = meta.displayName,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = meta.defaultValue,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = selectedBin.accent,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

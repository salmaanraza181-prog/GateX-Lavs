package com.example.ui.canvas

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CircuitTemplate
import com.example.data.CircuitTemplates
import com.example.model.AnalogSimulationResult
import com.example.model.ComponentCatalog
import com.example.model.ComponentInstance
import com.example.model.ComponentType
import com.example.ui.theme.LogicHighGreen
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SignalCyan

@Composable
fun QuickComponentPickerSheet(
    isDark: Boolean,
    onSelect: (ComponentType) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedBin by remember { mutableStateOf<WorkbenchBin?>(null) }
    val items = remember(query, selectedBin) {
        val base = ComponentCatalog.search(query)
        if (selectedBin == null) base else base.filter { it.type in selectedBin!!.types }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Add Component to Canvas", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search ${ComponentCatalog.allComponents.size}+ parts (e.g. SPDT, DPDT, 7400, 555)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(selected = selectedBin == null, onClick = { selectedBin = null }, label = { Text("All") })
            WorkbenchBin.entries.forEach { bin ->
                FilterChip(
                    selected = selectedBin == bin,
                    onClick = { selectedBin = if (selectedBin == bin) null else bin },
                    label = { Text(bin.title) }
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.type.name }) { meta ->
                val bin = WorkbenchBin.entries.firstOrNull { meta.type in it.types }
                val accent = bin?.accent ?: SignalCyan
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .width(136.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .clickable { onSelect(meta.type) }
                        .testTag("pick_${meta.type.name}")
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ComponentSymbolThumbnail(
                            type = meta.type,
                            isDark = isDark,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        )
                        Text(meta.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        Text(meta.defaultValue, style = MaterialTheme.typography.bodySmall, color = accent, maxLines = 1)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun ComponentPropertyEditorSheet(
    component: ComponentInstance,
    analogResult: AnalogSimulationResult,
    onSave: (ComponentInstance) -> Unit,
    onToggleDipBit: ((Int) -> Unit)? = null,
    onClose: () -> Unit
) {
    val meta = ComponentCatalog.metadataFor(component)
    var label by remember(component.id) { mutableStateOf(component.label) }
    var value by remember(component.id) { mutableStateOf(component.value) }
    var voltageProp by remember(component.id) { mutableStateOf(component.properties["voltage"] ?: "") }
    var freqProp by remember(component.id) { mutableStateOf(component.properties["frequency"] ?: "") }
    var wiperProp by remember(component.id) { mutableStateOf(component.properties["wiperPct"] ?: component.properties["positionPct"] ?: "") }
    var vonProp by remember(component.id) { mutableStateOf(component.properties["von"] ?: "") }
    var coilVoltProp by remember(component.id) { mutableStateOf(component.properties["coilVoltage"] ?: "") }
    var ratingMaProp by remember(component.id) { mutableStateOf(component.properties["ratingMa"] ?: "") }
    var opAmpGainProp by remember(component.id) { mutableStateOf(component.properties["gain"] ?: "") }
    var opAmpVPosProp by remember(component.id) { mutableStateOf(component.properties["vPosRail"] ?: "") }
    var opAmpVNegProp by remember(component.id) { mutableStateOf(component.properties["vNegRail"] ?: "") }
    var sevenSegActiveLow by remember(component.id) { mutableStateOf(component.properties["activeLow"] == "true") }
    var gateInputCount by remember(component.id) {
        mutableStateOf((component.properties["inputCount"]?.toIntOrNull() ?: 2).coerceIn(2, 8))
    }
    var lcdLine1Prop by remember(component.id) { mutableStateOf(component.properties["line1"] ?: component.value.ifBlank { "GATEX LABS" }) }

    val quickValuePresets = remember(component.type) {
        when (component.type) {
            ComponentType.RESISTOR, ComponentType.VARIABLE_RESISTOR, ComponentType.POTENTIOMETER ->
                listOf("100", "220", "470", "1k", "4.7k", "10k", "100k")
            ComponentType.CAPACITOR, ComponentType.POLARIZED_CAPACITOR ->
                listOf("100n", "1u", "10u", "47u", "100u", "470u")
            ComponentType.INDUCTOR ->
                listOf("1mH", "10mH", "100mH", "1H")
            ComponentType.DC_SOURCE, ComponentType.BATTERY, ComponentType.POWER_SUPPLY, ComponentType.VARIABLE_INPUT ->
                listOf("3.3V", "5V", "9V", "12V", "24V")
            ComponentType.ZENER_DIODE ->
                listOf("3.3V", "5.1V", "9.1V", "12V")
            ComponentType.FUSE ->
                listOf("50mA", "100mA", "250mA", "500mA", "1000mA")
            ComponentType.NET_LABEL ->
                listOf("CLK", "VCC", "GND", "A", "B", "OUT", "RST")
            else -> emptyList()
        }
    }

    val valueFieldLabel = when (component.type) {
        ComponentType.NET_LABEL -> "Wireless Net Name (matching names connect)"
        ComponentType.TEXT_NOTE -> "Annotation Note Text"
        ComponentType.FUSE -> "Fuse Display Label (e.g. 250mA)"
        else -> "Value (e.g. 1k, 10u, 5V)"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("${meta.displayName} (${component.label})", style = MaterialTheme.typography.headlineSmall)
        Text(meta.datasheetSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        analogResult.componentCurrents[component.id]?.let { cur ->
            val pwr = analogResult.componentPowers[component.id] ?: 0.0
            Surface(
                color = LogicHighGreen.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Live Telemetry: I = ${ComponentCatalog.formatEngineeringValue(cur, "A")} | P = ${ComponentCatalog.formatEngineeringValue(pwr, "W")}",
                    style = MaterialTheme.typography.labelLarge,
                    color = LogicHighGreen,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        if (component.type == ComponentType.DIP_SWITCH && onToggleDipBit != null) {
            val bits = (component.properties["bits"] ?: "0000").padEnd(4, '0')
            Text("Individual DIP Channels (Tap to Toggle):", style = MaterialTheme.typography.titleSmall, color = SignalAmber)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 0..3) {
                    val isOn = bits[i] == '1'
                    FilterChip(
                        selected = isOn,
                        onClick = { onToggleDipBit(i) },
                        label = { Text("D$i: ${if (isOn) "1" else "0"}") }
                    )
                }
            }
        }

        if (component.type == ComponentType.SEVEN_SEGMENT) {
            Text("7-Segment Drive Mode:", style = MaterialTheme.typography.titleSmall, color = SignalAmber)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !sevenSegActiveLow,
                    onClick = { sevenSegActiveLow = false },
                    label = { Text("Common Cathode (Active-HIGH)") }
                )
                FilterChip(
                    selected = sevenSegActiveLow,
                    onClick = { sevenSegActiveLow = true },
                    label = { Text("Common Anode / 7447 (Active-LOW)") }
                )
            }
        }

        if (component.type in ComponentCatalog.variableInputGateTypes) {
            Text("Number of Gate Inputs (2–8 Pins):", style = MaterialTheme.typography.titleSmall, color = SignalAmber)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(2, 3, 4, 5, 6, 8).forEach { cnt ->
                    FilterChip(
                        selected = gateInputCount == cnt,
                        onClick = {
                            gateInputCount = cnt
                            value = "$cnt-Input"
                        },
                        label = { Text("$cnt-In") },
                        modifier = Modifier.testTag("gate_inputs_chip_$cnt")
                    )
                }
            }
        }

        if (component.type == ComponentType.LCD_16X2) {
            OutlinedTextField(
                value = lcdLine1Prop,
                onValueChange = { lcdLine1Prop = it.take(16); value = it.take(16) },
                label = { Text("LCD Line 1 Text (max 16 chars)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Component Label") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text(valueFieldLabel) }, modifier = Modifier.fillMaxWidth())

        if (quickValuePresets.isNotEmpty()) {
            Text("Quick Presets:", style = MaterialTheme.typography.labelMedium, color = SignalCyan)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickValuePresets.forEach { preset ->
                    SuggestionChip(
                        onClick = {
                            value = preset
                            if (preset.endsWith("V") && component.type in setOf(
                                    ComponentType.DC_SOURCE, ComponentType.BATTERY,
                                    ComponentType.POWER_SUPPLY, ComponentType.VARIABLE_INPUT
                                )
                            ) {
                                voltageProp = preset.removeSuffix("V")
                            }
                            if (component.type == ComponentType.FUSE && preset.endsWith("mA")) {
                                ratingMaProp = preset.removeSuffix("mA")
                            }
                        },
                        label = { Text(preset) }
                    )
                }
            }
        }

        if (voltageProp.isNotEmpty() || component.type in setOf(ComponentType.DC_SOURCE, ComponentType.BATTERY, ComponentType.POWER_SUPPLY, ComponentType.VARIABLE_INPUT)) {
            OutlinedTextField(value = voltageProp, onValueChange = { voltageProp = it }, label = { Text("Voltage (V)") }, modifier = Modifier.fillMaxWidth())
        }
        if (freqProp.isNotEmpty() || component.type in setOf(ComponentType.AC_SOURCE, ComponentType.FUNCTION_GENERATOR, ComponentType.CLOCK_INPUT, ComponentType.IC_555)) {
            OutlinedTextField(value = freqProp, onValueChange = { freqProp = it }, label = { Text("Frequency (Hz)") }, modifier = Modifier.fillMaxWidth())
        }
        if (wiperProp.isNotEmpty()) {
            OutlinedTextField(value = wiperProp, onValueChange = { wiperProp = it }, label = { Text("Wiper / Position (%)") }, modifier = Modifier.fillMaxWidth())
        }
        if (vonProp.isNotEmpty() || component.type == ComponentType.VOLTAGE_CONTROLLED_SWITCH) {
            OutlinedTextField(value = vonProp, onValueChange = { vonProp = it }, label = { Text("Turn-on Threshold Vth (V)") }, modifier = Modifier.fillMaxWidth())
        }
        if (coilVoltProp.isNotEmpty() || component.type in setOf(ComponentType.RELAY, ComponentType.RELAY_SPDT)) {
            OutlinedTextField(value = coilVoltProp, onValueChange = { coilVoltProp = it }, label = { Text("Nominal Coil Voltage (V)") }, modifier = Modifier.fillMaxWidth())
        }
        if (component.type == ComponentType.FUSE) {
            OutlinedTextField(value = ratingMaProp, onValueChange = { ratingMaProp = it }, label = { Text("Blow Current Rating (mA)") }, modifier = Modifier.fillMaxWidth())
        }
        if (component.type == ComponentType.OP_AMP_741) {
            OutlinedTextField(value = opAmpGainProp, onValueChange = { opAmpGainProp = it }, label = { Text("Open-Loop Gain Aol") }, modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = opAmpVPosProp, onValueChange = { opAmpVPosProp = it }, label = { Text("+V Rail (V)") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = opAmpVNegProp, onValueChange = { opAmpVNegProp = it }, label = { Text("-V Rail (V)") }, modifier = Modifier.weight(1f))
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onClose) { Text("Cancel") }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    val newProps = component.properties.toMutableMap()
                    if (voltageProp.isNotBlank()) newProps["voltage"] = voltageProp
                    if (freqProp.isNotBlank()) newProps["frequency"] = freqProp
                    if (wiperProp.isNotBlank()) {
                        if (component.type == ComponentType.POTENTIOMETER) newProps["wiperPct"] = wiperProp
                        else newProps["positionPct"] = wiperProp
                    }
                    if (vonProp.isNotBlank()) newProps["von"] = vonProp
                    if (coilVoltProp.isNotBlank()) newProps["coilVoltage"] = coilVoltProp
                    if (component.type == ComponentType.FUSE && ratingMaProp.isNotBlank()) {
                        newProps["ratingMa"] = ratingMaProp
                    }
                    if (component.type == ComponentType.OP_AMP_741) {
                        if (opAmpGainProp.isNotBlank()) newProps["gain"] = opAmpGainProp
                        if (opAmpVPosProp.isNotBlank()) newProps["vPosRail"] = opAmpVPosProp
                        if (opAmpVNegProp.isNotBlank()) newProps["vNegRail"] = opAmpVNegProp
                    }
                    if (component.type == ComponentType.SEVEN_SEGMENT) {
                        newProps["activeLow"] = sevenSegActiveLow.toString()
                    }
                    if (component.type in ComponentCatalog.variableInputGateTypes) {
                        newProps["inputCount"] = gateInputCount.toString()
                    }
                    if (component.type == ComponentType.LCD_16X2) {
                        newProps["line1"] = lcdLine1Prop.ifBlank { "GATEX LABS" }
                    }
                    onSave(component.copy(label = label, value = value, properties = newProps))
                },
                colors = ButtonDefaults.buttonColors(containerColor = SignalCyan)
            ) {
                Text("Apply")
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun QuickLabTemplatesSheet(
    onSelectTemplate: (CircuitTemplate) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Ready-to-Simulate Lab Circuits", style = MaterialTheme.typography.headlineSmall)
                Text("${CircuitTemplates.all.size} verified analog, digital & switch labs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onClose) { Text("Close") }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CircuitTemplates.all, key = { it.id }) { tpl ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectTemplate(tpl) }
                        .testTag("ws_tpl_${tpl.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tpl.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(tpl.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { onSelectTemplate(tpl) }) {
                            Text("Load")
                        }
                    }
                }
            }
        }
    }
}

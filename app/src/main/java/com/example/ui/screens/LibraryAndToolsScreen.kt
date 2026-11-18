package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ComponentCatalog
import com.example.model.ComponentCategory
import com.example.model.ComponentMetadata
import com.example.tools.BooleanEngine
import com.example.tools.EngineeringCalculators
import com.example.tools.ResistorColorBand
import com.example.ui.canvas.ComponentSymbolThumbnail
import com.example.ui.canvas.WorkbenchBin
import com.example.ui.theme.LogicHighGreen
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SignalCyan
import com.example.viewmodel.AppTab
import com.example.viewmodel.WorkbenchViewModel

@Composable
fun LibraryAndToolsScreen(
    vm: WorkbenchViewModel,
    modifier: Modifier = Modifier
) {
    var tabIdx by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tabIdx) {
            Tab(
                selected = tabIdx == 0,
                onClick = { tabIdx = 0 },
                text = { Text("Component Library") },
                modifier = Modifier.testTag("lib_tab_components")
            )
            Tab(
                selected = tabIdx == 1,
                onClick = { tabIdx = 1 },
                text = { Text("Engineering Tools") },
                modifier = Modifier.testTag("lib_tab_tools")
            )
        }

        if (tabIdx == 0) {
            ComponentLibraryBrowser(vm)
        } else {
            EngineeringToolsView(vm)
        }
    }
}

@Composable
private fun ComponentLibraryBrowser(vm: WorkbenchViewModel) {
    var query by remember { mutableStateOf("") }
    var selectedBin by remember { mutableStateOf<WorkbenchBin?>(null) }
    var datasheetItem by remember { mutableStateOf<ComponentMetadata?>(null) }

    val filtered = remember(query, selectedBin) {
        val base = if (query.isBlank()) ComponentCatalog.allComponents else ComponentCatalog.search(query)
        if (selectedBin == null) base else base.filter { it.type in selectedBin!!.types }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Search components, switches, 7400 ICs, meters...") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("library_search_input")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedBin == null,
                onClick = { selectedBin = null },
                label = { Text("All (${ComponentCatalog.allComponents.size})") }
            )
            WorkbenchBin.entries.forEach { bin ->
                FilterChip(
                    selected = selectedBin == bin,
                    onClick = { selectedBin = if (selectedBin == bin) null else bin },
                    leadingIcon = {
                        Icon(
                            imageVector = bin.icon,
                            contentDescription = null,
                            tint = bin.accent,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("${bin.title} (${bin.types.size})") }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.type.name }) { meta ->
                val bin = WorkbenchBin.entries.firstOrNull { meta.type in it.types }
                val accent = bin?.accent ?: SignalCyan
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = Color(0xFF0A101D),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .size(width = 62.dp, height = 52.dp)
                                .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        ) {
                            ComponentSymbolThumbnail(
                                type = meta.type,
                                isDark = true,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(meta.displayName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${bin?.title ?: meta.category.title} • ${meta.defaultValue} • ${meta.supportLevel.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = accent
                            )
                            Text(meta.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                        }
                        IconButton(onClick = { datasheetItem = meta }) {
                            Icon(Icons.Default.Info, contentDescription = "Datasheet & pinout")
                        }
                        IconButton(
                            onClick = {
                                vm.addComponent(meta.type)
                                vm.navigateTab(AppTab.DESIGN)
                            },
                            modifier = Modifier.testTag("lib_add_${meta.type.name}")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add to canvas", tint = LogicHighGreen)
                        }
                    }
                }
            }
        }
    }

    datasheetItem?.let { meta ->
        AlertDialog(
            onDismissRequest = { datasheetItem = null },
            title = { Text("${meta.displayName} (${meta.shortCode})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(meta.description, style = MaterialTheme.typography.bodyMedium)
                    Text("Datasheet Summary:", style = MaterialTheme.typography.titleSmall, color = SignalCyan)
                    Text(meta.datasheetSummary, style = MaterialTheme.typography.bodySmall)
                    Text("Pins (${meta.pins.size}): ${meta.pins.joinToString(", ") { it.label }}", style = MaterialTheme.typography.labelMedium)
                    if (meta.pinoutTable.isNotEmpty()) {
                        Text("DIP Pinout:", style = MaterialTheme.typography.titleSmall, color = SignalAmber)
                        meta.pinoutTable.forEach { (left, right) ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(left, style = MaterialTheme.typography.labelSmall)
                                Text(right, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.addComponent(meta.type)
                    datasheetItem = null
                    vm.navigateTab(AppTab.DESIGN)
                }) { Text("Place on Canvas") }
            },
            dismissButton = {
                TextButton(onClick = { datasheetItem = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun EngineeringToolsView(vm: WorkbenchViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Boolean Algebra Simplifier & Circuit Generator
        BooleanToolCard(onBuildCircuit = { expr ->
            vm.openBooleanCircuit(expr)
        })

        // 2. Ohm's Law Calculator
        OhmsLawCard()

        // 3. Resistor Color Code Calculator
        ResistorColorCodeCard()

        // 4. LED Resistor Calculator
        LedResistorCard()

        // 5. RC Time Constant Calculator
        RcTimeConstantCard()

        // 6. Number Base Converter (Binary / Decimal / Hex)
        NumberBaseConverterCard()
    }
}

@Composable
private fun BooleanToolCard(onBuildCircuit: (String) -> Boolean) {
    var expr by remember { mutableStateOf("A.B + A.~B") }
    val analysis = remember(expr) { BooleanEngine.parseAndAnalyze(expr) }

    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Boolean Simplifier & Circuit Synthesizer", style = MaterialTheme.typography.titleMedium, color = SignalCyan)
            OutlinedTextField(
                value = expr,
                onValueChange = { expr = it },
                label = { Text("Expression (e.g. A.B + A'C or A^B)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("bool_expr_input")
            )
            if (analysis != null) {
                Text("Simplified SOP: Y = ${analysis.simplifiedSop}", style = MaterialTheme.typography.labelLarge, color = LogicHighGreen)
                Text("Truth Table (${analysis.variables.joinToString(",")} -> Y):", style = MaterialTheme.typography.bodySmall)
                analysis.rows.take(8).forEach { (inputs, out) ->
                    Text(
                        text = "${inputs.joinToString(" ") { if (it) "1" else "0" }}  |  ${if (out) "1" else "0"}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Button(
                    onClick = { onBuildCircuit(expr) },
                    modifier = Modifier.fillMaxWidth().testTag("bool_to_circuit_btn")
                ) {
                    Text("Convert Expression to Schematic Circuit")
                }
            } else {
                Text("Enter valid Boolean syntax using A–E, +, ., ~, ', ^, ( )", style = MaterialTheme.typography.bodySmall, color = SignalAmber)
            }
        }
    }
}

@Composable
private fun OhmsLawCard() {
    var vStr by remember { mutableStateOf("5") }
    var rStr by remember { mutableStateOf("220") }
    var iStr by remember { mutableStateOf("") }
    val res = remember(vStr, rStr, iStr) {
        EngineeringCalculators.solveOhmsLaw(
            v = vStr.toDoubleOrNull(),
            i = iStr.toDoubleOrNull(),
            r = rStr.toDoubleOrNull(),
            p = null
        )
    }
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ohm's Law & Power Calculator", style = MaterialTheme.typography.titleMedium, color = SignalAmber)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(vStr, { vStr = it }, label = { Text("V (Volts)") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(rStr, { rStr = it }, label = { Text("R (Ohms)") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(iStr, { iStr = it }, label = { Text("I (Amps)") }, modifier = Modifier.weight(1f), singleLine = true)
            }
            if (res != null) {
                Text(
                    text = "V = ${String.format("%.2f V", res.voltageV)} | I = ${ComponentCatalog.formatEngineeringValue(res.currentA, "A")} | R = ${ComponentCatalog.formatEngineeringValue(res.resistanceOhms, "Ω")} | P = ${ComponentCatalog.formatEngineeringValue(res.powerW, "W")}",
                    style = MaterialTheme.typography.labelMedium,
                    color = LogicHighGreen
                )
            }
        }
    }
}

@Composable
private fun ResistorColorCodeCard() {
    val digitBands = ResistorColorBand.entries.filter { it.digit != null }
    val multBands = ResistorColorBand.entries.filter { it.multiplier != null }
    val tolBands = ResistorColorBand.entries.filter { it.tolerancePct != null }

    var b1 by remember { mutableStateOf(ResistorColorBand.BROWN) }
    var b2 by remember { mutableStateOf(ResistorColorBand.BLACK) }
    var mult by remember { mutableStateOf(ResistorColorBand.RED) }
    var tol by remember { mutableStateOf(ResistorColorBand.GOLD) }
    val (ohms, tolPct) = EngineeringCalculators.decode4BandResistor(b1, b2, mult, tol)

    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("4-Band Resistor Color Code", style = MaterialTheme.typography.titleMedium, color = SignalCyan)
            // Visual resistor preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .background(Color(0xFFD2B48C), RoundedCornerShape(16.dp))
                    .padding(horizontal = 28.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(b1, b2, mult, tol).forEach { band ->
                    Box(modifier = Modifier.width(14.dp).height(42.dp).background(Color(band.colorHex)))
                }
            }
            Text(
                text = "${ComponentCatalog.formatEngineeringValue(ohms, "Ω")} ±$tolPct%",
                style = MaterialTheme.typography.titleMedium,
                color = LogicHighGreen
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { b1 = digitBands[(digitBands.indexOf(b1) + 1) % digitBands.size] }, modifier = Modifier.weight(1f)) {
                    Text(b1.label, maxLines = 1)
                }
                OutlinedButton(onClick = { b2 = digitBands[(digitBands.indexOf(b2) + 1) % digitBands.size] }, modifier = Modifier.weight(1f)) {
                    Text(b2.label, maxLines = 1)
                }
                OutlinedButton(onClick = { mult = multBands[(multBands.indexOf(mult) + 1) % multBands.size] }, modifier = Modifier.weight(1f)) {
                    Text(mult.label, maxLines = 1)
                }
                OutlinedButton(onClick = { tol = tolBands[(tolBands.indexOf(tol) + 1) % tolBands.size] }, modifier = Modifier.weight(1f)) {
                    Text(tol.label, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun LedResistorCard() {
    var vs by remember { mutableStateOf("5.0") }
    var vf by remember { mutableStateOf("2.0") }
    var ma by remember { mutableStateOf("15") }
    val res = EngineeringCalculators.calculateLedResistor(
        vs.toDoubleOrNull() ?: 5.0,
        vf.toDoubleOrNull() ?: 2.0,
        ma.toDoubleOrNull() ?: 15.0
    )
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("LED Resistor Calculator", style = MaterialTheme.typography.titleMedium, color = SignalAmber)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(vs, { vs = it }, label = { Text("Vs (V)") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(vf, { vf = it }, label = { Text("Vf (V)") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(ma, { ma = it }, label = { Text("If (mA)") }, modifier = Modifier.weight(1f), singleLine = true)
            }
            if (res != null) {
                Text(
                    text = "Exact: ${String.format("%.1f Ω", res.exactOhms)} | Nearest E24: ${ComponentCatalog.formatEngineeringValue(res.standardE24Ohms, "Ω")} (${res.recommendedWattage})",
                    style = MaterialTheme.typography.labelMedium,
                    color = LogicHighGreen
                )
            }
        }
    }
}

@Composable
private fun RcTimeConstantCard() {
    var rStr by remember { mutableStateOf("1k") }
    var cStr by remember { mutableStateOf("10u") }
    val r = ComponentCatalog.parseEngineeringValue(rStr, 1000.0)
    val c = ComponentCatalog.parseEngineeringValue(cStr, 1e-5)
    val res = EngineeringCalculators.calculateRcTimeConstant(r, c)
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("RC Time Constant & Cutoff Calculator", style = MaterialTheme.typography.titleMedium, color = SignalCyan)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(rStr, { rStr = it }, label = { Text("R (e.g. 1k)") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(cStr, { cStr = it }, label = { Text("C (e.g. 10u)") }, modifier = Modifier.weight(1f), singleLine = true)
            }
            if (res != null) {
                Text(
                    text = "τ = ${ComponentCatalog.formatEngineeringValue(res.tauSeconds, "s")} | 5τ = ${ComponentCatalog.formatEngineeringValue(res.fiveTauSeconds, "s")} | fc = ${ComponentCatalog.formatEngineeringValue(res.cutoffHz, "Hz")}",
                    style = MaterialTheme.typography.labelMedium,
                    color = LogicHighGreen
                )
            }
        }
    }
}

@Composable
private fun NumberBaseConverterCard() {
    var input by remember { mutableStateOf("42") }
    var radix by remember { mutableIntStateOf(10) }
    val converted = EngineeringCalculators.convertNumberBase(input, radix)

    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Binary / Decimal / Hex Converter", style = MaterialTheme.typography.titleMedium, color = SignalAmber)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2 to "BIN", 10 to "DEC", 16 to "HEX").forEach { (r, lbl) ->
                    FilterChip(selected = radix == r, onClick = { radix = r }, label = { Text(lbl) })
                }
            }
            OutlinedTextField(input, { input = it }, label = { Text("Input Value") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            if (converted != null) {
                Text(
                    text = "BIN: ${converted.first}   |   DEC: ${converted.second}   |   HEX: 0x${converted.third}",
                    style = MaterialTheme.typography.labelLarge,
                    color = SignalCyan
                )
            }
        }
    }
}

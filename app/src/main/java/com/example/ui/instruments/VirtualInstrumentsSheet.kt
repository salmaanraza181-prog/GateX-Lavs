package com.example.ui.instruments

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.netlist.NetlistBuilder
import com.example.model.ComponentCatalog
import com.example.model.ComponentType
import com.example.model.LogicLevel
import com.example.tools.BomCalculator
import com.example.ui.theme.FaultRed
import com.example.ui.theme.LogicHighGreen
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SignalCyan
import com.example.viewmodel.WorkbenchViewModel
import kotlin.math.abs

@Composable
fun VirtualInstrumentsSheet(
    vm: WorkbenchViewModel,
    onClose: () -> Unit
) {
    val project by vm.activeProject.collectAsStateWithLifecycle()
    val analogResult by vm.analogResult.collectAsStateWithLifecycle()
    val digitalResult by vm.digitalResult.collectAsStateWithLifecycle()
    val truthTable by vm.truthTable.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Multimeter",
        "Oscilloscope",
        "Logic Analyzer",
        "Func Gen",
        "Truth Table",
        "Logic Converter",
        "Word Gen",
        "BOM & Power"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Virtual Laboratory Instruments", style = MaterialTheme.typography.headlineSmall)
            OutlinedButton(onClick = { vm.stepSimulationOnce(true) }) { Text("Sample Now") }
        }

        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = {
                        selectedTab = index
                        if (index == 4) vm.computeTruthTableForActivePage()
                    },
                    text = { Text(title, style = MaterialTheme.typography.titleSmall) },
                    modifier = Modifier.testTag("inst_tab_$index")
                )
            }
        }

        when (selectedTab) {
            0 -> MultimeterPanel(project.activePage(), analogResult)
            1 -> OscilloscopePanel(
                analogResult = analogResult,
                onExportCsv = { vm.exportWaveformCsvAndShare(isAnalog = true) },
                onExportPng = { vm.exportWaveformPngAndShare(isAnalog = true) }
            )
            2 -> LogicAnalyzerPanel(
                digitalResult = digitalResult,
                onExportCsv = { vm.exportWaveformCsvAndShare(isAnalog = false) },
                onExportPng = { vm.exportWaveformPngAndShare(isAnalog = false) }
            )
            3 -> FunctionGeneratorPanel(project.activePage(), vm)
            4 -> TruthTablePanel(truthTable, onRefresh = { vm.computeTruthTableForActivePage() })
            5 -> LogicConverterPanel(vm = vm, onBuiltCircuit = onClose)
            6 -> WordGeneratorPanel(vm = vm)
            7 -> BomPowerPanel(
                page = project.activePage(),
                analogResult = analogResult,
                onExportBomCsv = { vm.exportBomCsvAndShare() }
            )
        }
    }
}

@Composable
private fun MultimeterPanel(
    page: com.example.model.CircuitPage,
    analogResult: com.example.model.AnalogSimulationResult
) {
    var mode by remember { mutableStateOf("VOLTAGE") }
    val dmm = page.components.firstOrNull { it.type == ComponentType.MULTIMETER }
    val primarySource = page.components.firstOrNull {
        it.type == ComponentType.DC_SOURCE || it.type == ComponentType.BATTERY || it.type == ComponentType.POWER_SUPPLY
    }

    val reading = remember(mode, analogResult, dmm, primarySource) {
        when (mode) {
            "VOLTAGE" -> {
                if (dmm != null) {
                    val vp = analogResult.pinVoltages[NetlistBuilder.pinKey(dmm.id, "pos")] ?: 0.0
                    val vn = analogResult.pinVoltages[NetlistBuilder.pinKey(dmm.id, "com")] ?: 0.0
                    String.format("%.3f V DC", vp - vn)
                } else {
                    val maxV = analogResult.nodeVoltages.values.maxByOrNull { abs(it) } ?: 0.0
                    String.format("%.3f V (Peak Node)", maxV)
                }
            }
            "CURRENT" -> {
                val cur = primarySource?.let { analogResult.componentCurrents[it.id] }
                    ?: analogResult.componentCurrents.values.maxOrNull()
                    ?: 0.0
                ComponentCatalog.formatEngineeringValue(cur, "A")
            }
            "RESISTANCE" -> {
                val r = analogResult.equivalentResistanceOhms
                if (r != null) ComponentCatalog.formatEngineeringValue(r, "Ω")
                else {
                    val v = primarySource?.let {
                        (analogResult.pinVoltages[NetlistBuilder.pinKey(it.id, "pos")] ?: 0.0) -
                            (analogResult.pinVoltages[NetlistBuilder.pinKey(it.id, "neg")] ?: 0.0)
                    } ?: 0.0
                    val i = primarySource?.let { analogResult.componentCurrents[it.id] } ?: 0.0
                    if (i > 1e-9) ComponentCatalog.formatEngineeringValue(abs(v / i), "Ω") else "OL (Open)"
                }
            }
            else -> {
                val r = analogResult.equivalentResistanceOhms
                if (r != null && r < 50.0) "BEEP! (${String.format("%.1f Ω", r)})" else "OPEN (>50Ω)"
            }
        }
    }

    Surface(
        color = Color(0xFF0A121E),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, SignalAmber.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("GATEX DMM-4000", style = MaterialTheme.typography.labelMedium, color = SignalAmber)
            Surface(
                color = Color(0xFF13261E),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = reading,
                    style = MaterialTheme.typography.displaySmall,
                    color = LogicHighGreen,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("VOLTAGE", "CURRENT", "RESISTANCE", "CONTINUITY").forEach { m ->
                    FilterChip(
                        selected = mode == m,
                        onClick = { mode = m },
                        label = { Text(m) }
                    )
                }
            }
            if (analogResult.nodeVoltages.isNotEmpty()) {
                Text("Node Voltages:", style = MaterialTheme.typography.titleSmall, color = Color(0xFF94A7C6))
                analogResult.nodeVoltages.entries.take(6).forEach { (net, v) ->
                    Text(
                        text = "$net : ${String.format("%.3f V", v)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = SignalCyan
                    )
                }
            }
        }
    }
}

@Composable
private fun OscilloscopePanel(
    analogResult: com.example.model.AnalogSimulationResult,
    onExportCsv: () -> Unit,
    onExportPng: () -> Unit
) {
    var voltsPerDiv by remember { mutableFloatStateOf(2f) }
    var timeZoom by remember { mutableFloatStateOf(1f) }
    var triggerV by remember { mutableFloatStateOf(0f) }
    var paused by remember { mutableStateOf(false) }
    var frozenSamples by remember { mutableStateOf(analogResult.transientSamples) }
    if (!paused) frozenSamples = analogResult.transientSamples

    val samples = frozenSamples
    val channels = samples.firstOrNull()?.channelValues?.keys?.toList().orEmpty()
    val chColors = listOf(SignalCyan, SignalAmber, LogicHighGreen)

    // Compute automated scope measurements for CH1
    val ch1Values = remember(samples, channels) {
        val ch1 = channels.firstOrNull() ?: return@remember emptyList()
        samples.mapNotNull { it.channelValues[ch1] }
    }
    val vMax = ch1Values.maxOrNull()
    val vMin = ch1Values.minOrNull()
    val vPp = if (vMax != null && vMin != null) vMax - vMin else null
    val vAvg = if (ch1Values.isNotEmpty()) ch1Values.average() else null

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (vPp != null && vMax != null && vMin != null && vAvg != null) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScopeMetricBadge("Vpp", String.format("%.2f V", vPp), SignalCyan)
                ScopeMetricBadge("Vmax", String.format("%.2f V", vMax), LogicHighGreen)
                ScopeMetricBadge("Vmin", String.format("%.2f V", vMin), SignalAmber)
                ScopeMetricBadge("Avg", String.format("%.2f V", vAvg), Color(0xFFB388FF))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(225.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(Color(0xFF0B1928), Color(0xFF040810))
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                .border(1.5.dp, SignalCyan.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                .padding(8.dp)
        ) {
            if (samples.isEmpty() || channels.isEmpty()) {
                Text(
                    text = "No transient waveform yet. Add an AC Source, Capacitor, Inductor, or Oscilloscope and press Run.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A7C6),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Fine phosphor graticule grid
                    val cols = 10
                    val rows = 6
                    for (c in 0..cols) {
                        val x = size.width * c / cols
                        drawLine(
                            if (c == cols / 2) Color(0xFF284166) else Color(0xFF132238),
                            Offset(x, 0f),
                            Offset(x, size.height),
                            if (c == cols / 2) 1.6f else 1f
                        )
                    }
                    for (r in 0..rows) {
                        val y = size.height * r / rows
                        drawLine(
                            if (r == rows / 2) Color(0xFF284166) else Color(0xFF132238),
                            Offset(0f, y),
                            Offset(size.width, y),
                            if (r == rows / 2) 1.8f else 1f
                        )
                    }
                    // Center cross sub-ticks
                    val midX = size.width / 2f
                    val midY = size.height / 2f
                    for (t in 1 until 20) {
                        val tx = size.width * t / 20f
                        drawLine(Color(0xFF395885), Offset(tx, midY - 4f), Offset(tx, midY + 4f), 1f)
                    }
                    // Trigger level dashed line
                    val pxPerVolt = (size.height / rows) / voltsPerDiv
                    val trigY = (midY - triggerV * pxPerVolt).coerceIn(0f, size.height)
                    drawLine(
                        FaultRed.copy(alpha = 0.7f),
                        Offset(0f, trigY),
                        Offset(size.width, trigY),
                        1.5f,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                    )

                    val visibleCount = (samples.size / timeZoom).toInt().coerceIn(4, samples.size)
                    val slice = samples.take(visibleCount)

                    channels.forEachIndexed { chIdx, chName ->
                        val col = chColors[chIdx % chColors.size]
                        val linePath = Path()
                        val fillPath = Path()
                        slice.forEachIndexed { i, pt ->
                            val x = if (slice.size <= 1) 0f else size.width * i / (slice.size - 1)
                            val v = pt.channelValues[chName] ?: 0.0
                            val y = (midY - v.toFloat() * pxPerVolt).coerceIn(2f, size.height - 2f)
                            if (i == 0) {
                                linePath.moveTo(x, y)
                                fillPath.moveTo(x, midY)
                                fillPath.lineTo(x, y)
                            } else {
                                linePath.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }
                            if (i == slice.lastIndex) {
                                fillPath.lineTo(x, midY)
                                fillPath.close()
                            }
                        }
                        // Translucent vertical fill under trace
                        drawPath(
                            path = fillPath,
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(col.copy(alpha = 0.22f), col.copy(alpha = 0.03f))
                            )
                        )
                        // Wide phosphor bloom + sharp trace core
                        drawPath(linePath, col.copy(alpha = 0.30f), style = Stroke(width = 7.5f))
                        drawPath(linePath, col, style = Stroke(width = 2.6f))
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Channels: ${channels.joinToString(" | ").ifEmpty { "None" }}", style = MaterialTheme.typography.labelMedium, color = SignalCyan)
            OutlinedButton(onClick = { paused = !paused }) {
                Text(if (paused) "Resume" else "Pause Scope")
            }
        }

        analogResult.acSweepSummary?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = SignalAmber)
        }

        Text("Vertical Scale: ${String.format("%.1f", voltsPerDiv)} V/div", style = MaterialTheme.typography.bodySmall)
        Slider(value = voltsPerDiv, onValueChange = { voltsPerDiv = it }, valueRange = 0.5f..10f)
        Text("Time Zoom: ${String.format("%.1fx", timeZoom)}", style = MaterialTheme.typography.bodySmall)
        Slider(value = timeZoom, onValueChange = { timeZoom = it }, valueRange = 1f..4f)
        Text("Trigger Level: ${String.format("%.1f V", triggerV)}", style = MaterialTheme.typography.bodySmall)
        Slider(value = triggerV, onValueChange = { triggerV = it }, valueRange = -5f..5f)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onExportCsv,
                modifier = Modifier.weight(1f).testTag("scope_export_csv_btn")
            ) { Text("Export CSV") }
            OutlinedButton(
                onClick = onExportPng,
                modifier = Modifier.weight(1f).testTag("scope_export_png_btn")
            ) { Text("Export PNG") }
        }
    }
}

@Composable
private fun LogicAnalyzerPanel(
    digitalResult: com.example.model.DigitalSimulationResult,
    onExportCsv: () -> Unit,
    onExportPng: () -> Unit
) {
    val history = digitalResult.waveformHistory
    val channels = history.firstOrNull()?.nodeLevels?.keys?.toList().orEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Captured: ${history.size} | Ticks: ${digitalResult.tickCount}", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = onExportCsv, modifier = Modifier.testTag("la_export_csv_btn")) { Text("CSV") }
                OutlinedButton(onClick = onExportPng, modifier = Modifier.testTag("la_export_png_btn")) { Text("PNG") }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color(0xFF060B14), RoundedCornerShape(12.dp))
                .border(1.dp, LogicHighGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            if (history.isEmpty() || channels.isEmpty()) {
                Text(
                    text = "Run a digital circuit with Logic Inputs, Clocks, Flip-Flops, or Logic Analyzer to capture timing diagrams.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A7C6),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                    channels.take(5).forEach { ch ->
                        Row(modifier = Modifier.fillMaxWidth().height(34.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = ch.take(8),
                                style = MaterialTheme.typography.labelSmall,
                                color = SignalCyan,
                                modifier = Modifier.width(64.dp)
                            )
                            Canvas(modifier = Modifier.weight(1f).height(26.dp)) {
                                val stepW = size.width / history.size.coerceAtLeast(1)
                                var prevY: Float? = null
                                val path = Path()
                                history.forEachIndexed { idx, sample ->
                                    val lvl = sample.nodeLevels[ch] ?: LogicLevel.UNKNOWN
                                    val y = when (lvl) {
                                        LogicLevel.ONE -> 3f
                                        LogicLevel.ZERO -> size.height - 3f
                                        else -> size.height / 2f
                                    }
                                    val x0 = idx * stepW
                                    val x1 = (idx + 1) * stepW
                                    if (idx == 0) {
                                        path.moveTo(x0, y)
                                    } else {
                                        if (prevY != null && prevY != y) path.lineTo(x0, y)
                                    }
                                    path.lineTo(x1, y)
                                    prevY = y
                                }
                                drawPath(path, LogicHighGreen, style = Stroke(2.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FunctionGeneratorPanel(
    page: com.example.model.CircuitPage,
    vm: WorkbenchViewModel
) {
    val existing = page.components.firstOrNull { it.type == ComponentType.FUNCTION_GENERATOR }
    var waveform by remember(existing?.id) { mutableStateOf(existing?.properties?.get("waveform") ?: "SINE") }
    var freq by remember(existing?.id) { mutableFloatStateOf(existing?.properties?.get("frequency")?.toFloatOrNull() ?: 100f) }
    var amp by remember(existing?.id) { mutableFloatStateOf(existing?.properties?.get("amplitude")?.toFloatOrNull() ?: 5f) }
    var offset by remember(existing?.id) { mutableFloatStateOf(existing?.properties?.get("offset")?.toFloatOrNull() ?: 0f) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = if (existing != null) "Editing ${existing.label}" else "No Function Generator on canvas yet",
            style = MaterialTheme.typography.titleSmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("SINE", "SQUARE", "TRIANGLE").forEach { w ->
                FilterChip(selected = waveform == w, onClick = { waveform = w }, label = { Text(w) })
            }
        }
        Text("Frequency: ${freq.toInt()} Hz", style = MaterialTheme.typography.labelMedium)
        Slider(value = freq, onValueChange = { freq = it }, valueRange = 10f..2000f)
        Text("Amplitude: ${String.format("%.1f V", amp)}", style = MaterialTheme.typography.labelMedium)
        Slider(value = amp, onValueChange = { amp = it }, valueRange = 0.5f..12f)
        Text("DC Offset: ${String.format("%.1f V", offset)}", style = MaterialTheme.typography.labelMedium)
        Slider(value = offset, onValueChange = { offset = it }, valueRange = -5f..5f)

        Button(
            onClick = {
                if (existing != null) {
                    vm.updateComponent(
                        existing.copy(
                            value = "${freq.toInt()}Hz ${String.format("%.1f", amp)}V $waveform",
                            properties = existing.properties + mapOf(
                                "waveform" to waveform,
                                "frequency" to freq.toDouble().toString(),
                                "amplitude" to amp.toDouble().toString(),
                                "offset" to offset.toDouble().toString()
                            )
                        )
                    )
                    vm.stepSimulationOnce(true)
                } else {
                    vm.addComponent(ComponentType.FUNCTION_GENERATOR, 5f, 8f)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (existing != null) "Apply Waveform Settings" else "Add Function Generator to Canvas")
        }
    }
}

@Composable
private fun TruthTablePanel(
    table: com.example.engine.digital.TruthTableResult?,
    onRefresh: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Extracted Circuit Truth Table", style = MaterialTheme.typography.titleSmall)
            OutlinedButton(onClick = onRefresh) { Text("Evaluate All") }
        }
        if (table == null || table.rows.isEmpty()) {
            Text(
                text = "Place Logic Input (0/1) and Logic Probe/LED components to extract a combinational truth table.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        table.inputLabels.forEach { lbl ->
                            Text(lbl, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = SignalCyan)
                        }
                        Text("|", modifier = Modifier.padding(horizontal = 6.dp))
                        table.outputLabels.forEach { lbl ->
                            Text(lbl, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = LogicHighGreen)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    table.rows.forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            row.inputs.forEach { v ->
                                Text(v.symbol, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                            }
                            Text("|", modifier = Modifier.padding(horizontal = 6.dp))
                            row.outputs.forEach { v ->
                                Text(
                                    v.symbol,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (v == LogicLevel.ONE) LogicHighGreen else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScopeMetricBadge(label: String, value: String, color: Color) {
    Surface(
        color = Color(0xFF0A1424),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A7C6))
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun BomPowerPanel(
    page: com.example.model.CircuitPage,
    analogResult: com.example.model.AnalogSimulationResult,
    onExportBomCsv: () -> Unit
) {
    val report = remember(page, analogResult) {
        BomCalculator.generate(page = page, analogResult = analogResult)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Bill of Materials & Power", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${report.totalPartsCount} physical part(s) • ${report.items.size} unique line item(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onExportBomCsv,
                modifier = Modifier.testTag("bom_export_csv_btn")
            ) {
                Text("Export CSV")
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                color = SignalCyan.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Est. Cost", style = MaterialTheme.typography.labelSmall, color = SignalCyan)
                    Text(
                        String.format("$%.2f", report.totalEstimatedUsd),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "≈ Rs. ${report.totalEstimatedPkr.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Surface(
                color = SignalAmber.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Est. Power", style = MaterialTheme.typography.labelSmall, color = SignalAmber)
                    Text(
                        String.format("%.1f mW", report.totalPowerMw),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        report.recommendedSupply,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (report.items.isEmpty()) {
            Text(
                "Place physical components on the schematic to compute BOM cost and power dissipation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Part / Value", modifier = Modifier.weight(1.7f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SignalCyan)
                        Text("Qty", modifier = Modifier.weight(0.45f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SignalCyan)
                        Text("USD", modifier = Modifier.weight(0.75f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SignalCyan)
                        Text("Power", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SignalAmber)
                    }
                    report.items.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1.7f)) {
                                Text(item.partName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                Text(
                                    "${item.designators.joinToString(", ")} • ${item.value}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text("×${item.quantity}", modifier = Modifier.weight(0.45f), style = MaterialTheme.typography.bodySmall)
                            Text(String.format("$%.2f", item.totalCostUsd), modifier = Modifier.weight(0.75f), style = MaterialTheme.typography.bodySmall, color = LogicHighGreen)
                            Text(String.format("%.0fmW", item.powerMw), modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodySmall, color = SignalAmber)
                        }
                    }
                }
            }
        }
    }
}


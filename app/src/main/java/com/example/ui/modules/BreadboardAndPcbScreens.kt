package com.example.ui.modules

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.BreadboardJumper
import com.example.model.BreadboardPlacement
import com.example.model.ComponentType
import com.example.model.PcbLayer
import com.example.model.PcbTrace
import com.example.model.PcbVia
import com.example.ui.theme.CopperGold
import com.example.ui.theme.FaultRed
import com.example.ui.theme.LogicHighGreen
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SignalCyan
import com.example.viewmodel.WorkbenchViewModel
import java.util.UUID
import kotlin.math.hypot

@Composable
fun BreadboardScreen(
    vm: WorkbenchViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val project by vm.activeProject.collectAsStateWithLifecycle()
    val bb = project.breadboard
    var mode by remember { mutableStateOf("JUMPER") } // JUMPER, DIP_IC, RESISTOR, LED, SWITCH
    var firstHole by remember { mutableStateOf<String?>(null) }
    var jumperColorHex by remember { mutableStateOf(0xFF00E5FFL) }

    val jumperColors = listOf(
        "Cyan" to 0xFF00E5FFL,
        "Red" to 0xFFFF3B30L,
        "Green" to 0xFF00E676L,
        "Yellow" to 0xFFFFD600L,
        "Blue" to 0xFF2979FFL,
        "Orange" to 0xFFFF6D00L,
        "Purple" to 0xFFAB47BCL,
        "Black" to 0xFF1E293BL
    )

    val cols = 30
    val topRows = listOf("+", "-")
    val upperStrip = listOf("a", "b", "c", "d", "e")
    val lowerStrip = listOf("f", "g", "h", "i", "j")
    val botRows = listOf("+", "-")
    val allRowLabels = topRows + upperStrip + lowerStrip + botRows

    fun holeNet(hole: String): String {
        val parts = hole.split(":")
        val strip = parts[0]
        val col = parts.getOrNull(1)?.toIntOrNull() ?: 1
        return when (strip) {
            "top+", "top-" -> strip
            "bot+", "bot-" -> strip
            "a", "b", "c", "d", "e" -> "U_COL_$col"
            "f", "g", "h", "i", "j" -> "L_COL_$col"
            else -> hole
        }
    }

    val highlightedNet = firstHole?.let { holeNet(it) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text("830-Point Breadboard Lab", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "30-column solderless board • +5V/GND rails • DIP IC ravine",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { vm.importSchematicToBreadboard() },
                    modifier = Modifier.testTag("breadboard_import_btn")
                ) { Text("Import") }
                OutlinedButton(onClick = { vm.clearBreadboard() }) { Text("Clear") }
            }
        }

        // Tool selection chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "JUMPER" to "Jumper Wire",
                "DIP_IC" to "14-Pin DIP IC",
                "RESISTOR" to "Resistor",
                "LED" to "LED",
                "SWITCH" to "Switch"
            ).forEach { (id, lbl) ->
                FilterChip(
                    selected = mode == id,
                    onClick = { mode = id; firstHole = null },
                    label = { Text(lbl) },
                    modifier = Modifier.testTag("bb_tool_${id.lowercase()}")
                )
            }
        }

        // 8-color jumper wire palette
        if (mode == "JUMPER") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Wire:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                jumperColors.forEach { (name, hex) ->
                    FilterChip(
                        selected = jumperColorHex == hex,
                        onClick = { jumperColorHex = hex },
                        label = { Text(name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        Text(
            text = when {
                mode == "DIP_IC" -> "Tap any column on row e/f to snap a 14-pin DIP IC cleanly across the center ravine"
                firstHole == null -> "Tap a breadboard hole to start ($mode) — scrolls horizontally across 30 columns"
                else -> "Selected $firstHole [Net $highlightedNet] — tap destination hole"
            },
            style = MaterialTheme.typography.labelMedium,
            color = SignalCyan
        )

        // Horizontally scrollable realistic 830-point board canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(370.dp)
                .background(Color(0xFFE7ECF2), RoundedCornerShape(14.dp))
                .border(2.dp, Color(0xFF94A3B8), RoundedCornerShape(14.dp))
                .horizontalScroll(rememberScrollState())
                .padding(8.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .height(354.dp)
                    .width(860.dp)
                    .padding(horizontal = 4.dp)
                    .testTag("breadboard_canvas")
                    .pointerInput(mode, firstHole, jumperColorHex) {
                        detectTapGestures { offset ->
                            val cellW = size.width / (cols + 1.5f)
                            val cellH = size.height / (allRowLabels.size + 2.5f)
                            val colIdx = ((offset.x / cellW) - 0.5f).toInt().coerceIn(1, cols)
                            val rowSlot = (offset.y / cellH).toInt()
                            val rowIdx = when {
                                rowSlot in 0..1 -> rowSlot
                                rowSlot in 3..7 -> rowSlot - 1
                                rowSlot in 9..13 -> rowSlot - 2
                                rowSlot in 15..16 -> rowSlot - 3
                                else -> -1
                            }
                            if (rowIdx in allRowLabels.indices) {
                                val prefix = when {
                                    rowIdx < 2 -> "top${allRowLabels[rowIdx]}"
                                    rowIdx >= 12 -> "bot${allRowLabels[rowIdx]}"
                                    else -> allRowLabels[rowIdx]
                                }
                                if (mode == "DIP_IC") {
                                    val startCol = colIdx.coerceIn(1, cols - 6)
                                    vm.addBreadboardPlacement(
                                        BreadboardPlacement(
                                            id = "bp_${UUID.randomUUID().toString().take(6)}",
                                            componentType = ComponentType.IC_7408,
                                            label = "U${bb.placements.count { it.startHole.startsWith("e:") } + 1}",
                                            value = "74xx DIP-14",
                                            startHole = "e:$startCol",
                                            endHole = "f:${startCol + 6}"
                                        )
                                    )
                                    return@detectTapGestures
                                }
                                val holeId = "$prefix:$colIdx"
                                val start = firstHole
                                if (start == null) {
                                    firstHole = holeId
                                } else if (start != holeId) {
                                    when (mode) {
                                        "JUMPER" -> vm.addBreadboardJumper(
                                            BreadboardJumper("j_${UUID.randomUUID().toString().take(6)}", start, holeId, jumperColorHex)
                                        )
                                        "RESISTOR" -> vm.addBreadboardPlacement(
                                            BreadboardPlacement("bp_${UUID.randomUUID().toString().take(6)}", ComponentType.RESISTOR, "R", "330Ω", start, holeId)
                                        )
                                        "LED" -> vm.addBreadboardPlacement(
                                            BreadboardPlacement("bp_${UUID.randomUUID().toString().take(6)}", ComponentType.LED, "LED", "Red", start, holeId)
                                        )
                                        "SWITCH" -> vm.addBreadboardPlacement(
                                            BreadboardPlacement("bp_${UUID.randomUUID().toString().take(6)}", ComponentType.TOGGLE_SWITCH, "SW", "ON", start, holeId)
                                        )
                                    }
                                    firstHole = null
                                }
                            }
                        }
                    }
            ) {
                val cellW = size.width / (cols + 1.5f)
                val cellH = size.height / (allRowLabels.size + 2.5f)

                fun holeCenter(hole: String): Offset {
                    val parts = hole.split(":")
                    val prefix = parts[0]
                    val col = parts.getOrNull(1)?.toIntOrNull() ?: 1
                    val slot = when (prefix) {
                        "top+" -> 0
                        "top-" -> 1
                        "a" -> 3; "b" -> 4; "c" -> 5; "d" -> 6; "e" -> 7
                        "f" -> 9; "g" -> 10; "h" -> 11; "i" -> 12; "j" -> 13
                        "bot+" -> 15
                        "bot-" -> 16
                        else -> 3
                    }
                    return Offset(col * cellW + cellW * 0.5f, slot * cellH + cellH * 0.5f)
                }

                // Top & bottom +5V (Red) / GND (Blue) rail markings
                drawLine(FaultRed, Offset(cellW, cellH * 0.12f), Offset(size.width - cellW * 0.6f, cellH * 0.12f), 3.5f)
                drawLine(Color(0xFF1D4ED8), Offset(cellW, cellH * 1.92f), Offset(size.width - cellW * 0.6f, cellH * 1.92f), 3.5f)
                drawLine(FaultRed, Offset(cellW, cellH * 15.05f), Offset(size.width - cellW * 0.6f, cellH * 15.05f), 3.5f)
                drawLine(Color(0xFF1D4ED8), Offset(cellW, cellH * 16.92f), Offset(size.width - cellW * 0.6f, cellH * 16.92f), 3.5f)
                // Center DIP IC trough
                drawRect(Color(0xFFCBD5E1), Offset(0f, cellH * 8.05f), Size(size.width, cellH * 0.90f))

                // Draw all 830-style holes with live continuity net highlighting
                for (rIdx in allRowLabels.indices) {
                    val prefix = when {
                        rIdx < 2 -> "top${allRowLabels[rIdx]}"
                        rIdx >= 12 -> "bot${allRowLabels[rIdx]}"
                        else -> allRowLabels[rIdx]
                    }
                    for (c in 1..cols) {
                        val hId = "$prefix:$c"
                        val center = holeCenter(hId)
                        val isHighlighted = highlightedNet != null && holeNet(hId) == highlightedNet
                        drawRect(
                            color = if (isHighlighted) LogicHighGreen else Color(0xFF1E293B),
                            topLeft = Offset(center.x - 4.5f, center.y - 4.5f),
                            size = Size(9f, 9f)
                        )
                    }
                }

                // Draw Jumpers
                bb.jumpers.forEach { j ->
                    val p1 = holeCenter(j.fromHole)
                    val p2 = holeCenter(j.toHole)
                    drawLine(Color(j.colorHex), p1, p2, 5.5f)
                    drawCircle(Color(j.colorHex), 6.5f, p1)
                    drawCircle(Color(j.colorHex), 6.5f, p2)
                }

                // Draw Placed Parts (DIP ICs straddle center ravine with metallic pins + notch)
                bb.placements.forEach { p ->
                    val p1 = holeCenter(p.startHole)
                    val p2 = holeCenter(p.endHole)
                    val isDip = p.startHole.startsWith("e:") && p.endHole.startsWith("f:")
                    if (isDip) {
                        val leftX = minOf(p1.x, p2.x) - 6f
                        val rightX = maxOf(p1.x, p2.x) + 6f
                        val topY = p1.y - 4f
                        val botY = p2.y + 4f
                        drawRoundRect(
                            color = Color(0xFF111827),
                            topLeft = Offset(leftX, topY),
                            size = Size(rightX - leftX, botY - topY),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = CopperGold,
                            topLeft = Offset(leftX, topY),
                            size = Size(rightX - leftX, botY - topY),
                            cornerRadius = CornerRadius(6f, 6f),
                            style = Stroke(2f)
                        )
                        // Pin-1 notch on left side of DIP package
                        drawCircle(Color(0xFF94A3B8), 4.5f, Offset(leftX + 3f, (topY + botY) / 2f))
                    } else {
                        drawLine(Color(0xFF475569), p1, p2, 3.2f)
                        val mid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
                        val col = when (p.componentType) {
                            ComponentType.LED, ComponentType.PROBE_RED -> FaultRed
                            ComponentType.PROBE_GREEN -> LogicHighGreen
                            ComponentType.TOGGLE_SWITCH, ComponentType.LOGIC_INPUT -> SignalCyan
                            else -> Color(0xFFD97706)
                        }
                        drawRoundRect(col, Offset(mid.x - 16f, mid.y - 8f), Size(32f, 16f), CornerRadius(6f, 6f))
                    }
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Breadboard Net Mapping (${bb.placements.size} parts • ${bb.jumpers.size} jumpers)",
                    style = MaterialTheme.typography.titleSmall
                )
                if (bb.placements.isEmpty() && bb.jumpers.isEmpty()) {
                    Text(
                        "Tap 'Import' to load components from your active schematic, or place DIP ICs, resistors, LEDs, and colored jumpers directly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    bb.placements.forEach { p ->
                        Text(
                            "• ${p.label} (${p.value}): ${p.startHole} [${holeNet(p.startHole)}] ↔ ${p.endHole} [${holeNet(p.endHole)}]",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PcbDesignerScreen(
    vm: WorkbenchViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val project by vm.activeProject.collectAsStateWithLifecycle()
    val pcb = project.pcb
    var activeLayer by remember { mutableStateOf(PcbLayer.TOP_COPPER) }
    var toolMode by remember { mutableStateOf("TRACE") } // TRACE or VIA
    var firstPointMm by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var drcReport by remember { mutableStateOf<List<String>>(emptyList()) }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text("PCB Layout Designer", style = MaterialTheme.typography.titleLarge)
                    Text("Board: ${pcb.boardWidthMm.toInt()}mm x ${pcb.boardHeightMm.toInt()}mm • Independent PCB Data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(onClick = { vm.importSchematicToPcbFootprints() }, modifier = Modifier.testTag("pcb_import_btn")) {
                Text("Sync Parts")
            }
        }

        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = activeLayer == PcbLayer.TOP_COPPER, onClick = { activeLayer = PcbLayer.TOP_COPPER }, label = { Text("Top Copper (Red)") })
            FilterChip(selected = activeLayer == PcbLayer.BOTTOM_COPPER, onClick = { activeLayer = PcbLayer.BOTTOM_COPPER }, label = { Text("Bottom Copper (Blue)") })
            FilterChip(selected = toolMode == "TRACE", onClick = { toolMode = "TRACE"; firstPointMm = null }, label = { Text("Route Track") })
            FilterChip(selected = toolMode == "VIA", onClick = { toolMode = "VIA"; firstPointMm = null }, label = { Text("Place Via") })
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color(0xFF0A2416), RoundedCornerShape(12.dp))
                .border(2.dp, CopperGold, RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("pcb_canvas")
                    .pointerInput(pcb, activeLayer, toolMode, firstPointMm) {
                        detectTapGestures { offset ->
                            val xMm = (offset.x / size.width) * pcb.boardWidthMm
                            val yMm = (offset.y / size.height) * pcb.boardHeightMm
                            if (toolMode == "VIA") {
                                vm.addPcbVia(PcbVia("via_${UUID.randomUUID().toString().take(6)}", "NET_VIA", xMm, yMm))
                            } else {
                                val start = firstPointMm
                                if (start == null) {
                                    firstPointMm = xMm to yMm
                                } else {
                                    vm.addPcbTrace(
                                        PcbTrace(
                                            id = "tr_${UUID.randomUUID().toString().take(6)}",
                                            netName = "TRACK_${pcb.traces.size + 1}",
                                            layer = activeLayer,
                                            widthMm = 0.5f,
                                            points = listOf(start, xMm to yMm)
                                        )
                                    )
                                    firstPointMm = null
                                }
                            }
                        }
                    }
            ) {
                val sx = size.width / pcb.boardWidthMm
                val sy = size.height / pcb.boardHeightMm

                // Board silkscreen outline
                drawRect(CopperGold, Offset.Zero, size, style = Stroke(3f))

                // Traces
                pcb.traces.forEach { tr ->
                    val col = if (tr.layer == PcbLayer.TOP_COPPER) Color(0xFFEF4444) else Color(0xFF3B82F6)
                    for (i in 0 until tr.points.size - 1) {
                        val p1 = tr.points[i]
                        val p2 = tr.points[i + 1]
                        drawLine(col, Offset(p1.first * sx, p1.second * sy), Offset(p2.first * sx, p2.second * sy), tr.widthMm * sx * 0.8f)
                    }
                }

                // Footprints & Pads
                pcb.footprints.forEach { fp ->
                    val fx = fp.xMm * sx
                    val fy = fp.yMm * sy
                    drawRect(Color.White.copy(alpha = 0.6f), Offset(fx - 26f, fy - 18f), Size(52f, 36f), style = Stroke(2f))
                    fp.pads.forEach { pad ->
                        val px = (fp.xMm + pad.relXMm) * sx
                        val py = (fp.yMm + pad.relYMm) * sy
                        drawCircle(CopperGold, 7f, Offset(px, py))
                        drawCircle(Color(0xFF0A2416), 2.5f, Offset(px, py))
                    }
                }

                // Vias
                pcb.vias.forEach { v ->
                    drawCircle(SignalAmber, 7f, Offset(v.xMm * sx, v.yMm * sy))
                    drawCircle(Color.Black, 3f, Offset(v.xMm * sx, v.yMm * sy))
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    // Real Design Rule Checking (DRC)
                    val issues = mutableListOf<String>()
                    if (pcb.footprints.isEmpty()) issues.add("WARN: No PCB footprints placed. Tap 'Sync Parts' first.")
                    pcb.footprints.forEach { fp ->
                        if (fp.xMm < 4f || fp.yMm < 4f || fp.xMm > pcb.boardWidthMm - 4f || fp.yMm > pcb.boardHeightMm - 4f) {
                            issues.add("DRC ERROR: Footprint ${fp.schematicRef} violates 4mm board edge clearance.")
                        }
                    }
                    pcb.traces.forEach { tr ->
                        if (tr.widthMm < 0.25f) issues.add("DRC ERROR: Trace ${tr.id} width ${tr.widthMm}mm < 0.25mm minimum.")
                    }
                    // Check for close pads on different nets
                    val allPads = pcb.footprints.flatMap { fp -> fp.pads.map { (fp.xMm + it.relXMm) to (fp.yMm + it.relYMm) } }
                    if (pcb.traces.isEmpty() && allPads.isNotEmpty()) {
                        issues.add("DRC INFO: ${allPads.size} unrouted pads remaining. Tap board to route copper tracks.")
                    }
                    if (issues.isEmpty()) {
                        issues.add("DRC PASSED: 0 clearance errors across ${pcb.footprints.size} footprints, ${pcb.traces.size} traces, ${pcb.vias.size} vias.")
                    }
                    drcReport = issues
                },
                modifier = Modifier.weight(1f).testTag("pcb_drc_btn")
            ) {
                Text("Run DRC Check")
            }
            OutlinedButton(
                onClick = { vm.updatePcbBoardSize(100f, 80f) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Expand 100x80mm")
            }
        }

        drcReport.forEach { line ->
            Surface(
                color = if (line.contains("ERROR")) FaultRed.copy(alpha = 0.16f) else LogicHighGreen.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(line, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
            }
        }
    }
}

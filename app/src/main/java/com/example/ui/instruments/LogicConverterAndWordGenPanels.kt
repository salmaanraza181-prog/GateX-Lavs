package com.example.ui.instruments

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ComponentType
import com.example.model.LogicLevel
import com.example.tools.BooleanEngine
import com.example.ui.theme.LogicHighGreen
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SignalCyan
import com.example.viewmodel.WorkbenchViewModel

@Composable
fun LogicConverterPanel(
    vm: WorkbenchViewModel,
    onBuiltCircuit: () -> Unit
) {
    var varCount by remember { mutableIntStateOf(3) }
    var minterms by remember { mutableStateOf(setOf(1, 2, 4, 7)) } // Default 3-var odd parity / full adder Sum
    var infoMessage by remember { mutableStateOf<String?>(null) }

    val vars = remember(varCount) { listOf("A", "B", "C", "D").take(varCount) }
    val validMinterms = remember(minterms, varCount) {
        val maxVal = (1 shl varCount) - 1
        minterms.filter { it in 0..maxVal }.sorted()
    }
    val simplifiedSop = remember(vars, validMinterms) {
        BooleanEngine.simplifyMinterms(vars, validMinterms)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Interactive Karnaugh Map & Gate Synthesizer",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Variable count selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(2 to "2 Vars (A,B)", 3 to "3 Vars (A,B,C)", 4 to "4 Vars (A..D)").forEach { (n, label) ->
                FilterChip(
                    selected = varCount == n,
                    onClick = {
                        varCount = n
                        val maxVal = (1 shl n) - 1
                        minterms = minterms.filter { it <= maxVal }.toSet()
                        infoMessage = null
                    },
                    label = { Text(label) },
                    modifier = Modifier.testTag("lc_vars_$n")
                )
            }
        }

        // Quick actions: Import from active circuit, Clear, Invert
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val table = vm.extractTruthTableForActivePage()
                    if (table.rows.isEmpty() || table.inputLabels.isEmpty()) {
                        infoMessage = "Place 2–4 Logic Inputs and a Probe/LED on canvas to import."
                    } else {
                        val importedVars = table.inputLabels.size.coerceIn(2, 4)
                        varCount = importedVars
                        val extracted = mutableSetOf<Int>()
                        table.rows.forEach { row ->
                            val outHigh = row.outputs.firstOrNull() == LogicLevel.ONE
                            if (outHigh) {
                                var mask = 0
                                row.inputs.take(importedVars).forEach { lvl ->
                                    mask = (mask shl 1) or if (lvl == LogicLevel.ONE) 1 else 0
                                }
                                extracted.add(mask)
                            }
                        }
                        minterms = extracted
                        infoMessage = "Imported ${extracted.size} minterm(s) from active circuit."
                    }
                },
                modifier = Modifier.testTag("lc_import_btn")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Import Circuit")
            }
            OutlinedButton(
                onClick = {
                    minterms = emptySet()
                    infoMessage = null
                },
                modifier = Modifier.testTag("lc_clear_btn")
            ) {
                Text("Clear")
            }
            OutlinedButton(
                onClick = {
                    val all = (0 until (1 shl varCount)).toSet()
                    minterms = all - validMinterms.toSet()
                    infoMessage = null
                },
                modifier = Modifier.testTag("lc_invert_btn")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Invert")
            }
        }

        infoMessage?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color = SignalAmber
            )
        }

        // Interactive K-Map Grid (Gray-code ordered)
        val rowHeaders: List<Pair<String, Int>>
        val colHeaders: List<Pair<String, Int>>
        val cornerTitle: String
        when (varCount) {
            2 -> {
                cornerTitle = "A \\ B"
                rowHeaders = listOf("0" to 0, "1" to 1)
                colHeaders = listOf("0" to 0, "1" to 1)
            }
            3 -> {
                cornerTitle = "A \\ BC"
                rowHeaders = listOf("0" to 0, "1" to 1)
                colHeaders = listOf("00" to 0, "01" to 1, "11" to 3, "10" to 2)
            }
            else -> {
                cornerTitle = "AB \\ CD"
                rowHeaders = listOf("00" to 0, "01" to 1, "11" to 3, "10" to 2)
                colHeaders = listOf("00" to 0, "01" to 1, "11" to 3, "10" to 2)
            }
        }
        val colShift = if (varCount == 2) 1 else 2

        Surface(
            color = Color(0xFF0A121E),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.2.dp, SignalCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = cornerTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = SignalAmber,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(54.dp)
                    )
                    colHeaders.forEach { (colLabel, _) ->
                        Text(
                            text = colLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = SignalCyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Data rows
                rowHeaders.forEach { (rowLabel, rowBits) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = rowLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = SignalCyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(54.dp)
                        )
                        colHeaders.forEach { (_, colBits) ->
                            val minterm = (rowBits shl colShift) or colBits
                            val isOne = minterm in minterms
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isOne) LogicHighGreen.copy(alpha = 0.22f)
                                        else Color(0xFF132033)
                                    )
                                    .border(
                                        width = if (isOne) 1.5.dp else 1.dp,
                                        color = if (isOne) LogicHighGreen else Color(0xFF283A56),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        minterms = if (isOne) minterms - minterm else minterms + minterm
                                    }
                                    .testTag("kmap_cell_$minterm")
                            ) {
                                Text(
                                    text = if (isOne) "1" else "0",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isOne) LogicHighGreen else Color(0xFF8AA0C0)
                                )
                                Text(
                                    text = "m$minterm",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF647896),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 4.dp, bottom = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Simplified SOP Readout Card
        Surface(
            color = Color(0xFF0E1D19),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, LogicHighGreen.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "SIMPLIFIED BOOLEAN SOP (Quine-McCluskey)",
                    style = MaterialTheme.typography.labelSmall,
                    color = SignalAmber
                )
                Text(
                    text = "Y = $simplifiedSop",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = LogicHighGreen,
                    modifier = Modifier.testTag("lc_simplified_text")
                )
                Text(
                    text = "Σm(${validMinterms.joinToString(", ").ifEmpty { "∅" }})",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = SignalCyan
                )
            }
        }

        // Auto-build circuit buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val page = BooleanEngine.mintermsToCircuitPage(
                        vars = vars,
                        minterms = validMinterms,
                        nandOnly = false,
                        pageName = "Logic: $simplifiedSop"
                    )
                    vm.loadSynthesizedPage(page)
                    onBuiltCircuit()
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("lc_build_gates_btn")
            ) {
                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Build Gates")
            }
            OutlinedButton(
                onClick = {
                    val page = BooleanEngine.mintermsToCircuitPage(
                        vars = vars,
                        minterms = validMinterms,
                        nandOnly = true,
                        pageName = "NAND: $simplifiedSop"
                    )
                    vm.loadSynthesizedPage(page)
                    onBuiltCircuit()
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("lc_build_nand_btn")
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("NAND-Only")
            }
        }
    }
}

@Composable
fun WordGeneratorPanel(vm: WorkbenchViewModel) {
    val project by vm.activeProject.collectAsStateWithLifecycle()
    var wordValue by remember { mutableIntStateOf(0x55) }

    val page = project.activePage()
    val logicInputCount = page.components.count { it.type == ComponentType.LOGIC_INPUT }
    val dipCount = page.components.count { it.type == ComponentType.DIP_SWITCH }

    val binaryStr = remember(wordValue) {
        wordValue.toString(2).padStart(8, '0').let { "${it.substring(0, 4)} ${it.substring(4)}" }
    }
    val hexStr = remember(wordValue) {
        "0x" + wordValue.toString(16).uppercase().padStart(2, '0')
    }

    fun currentLsbBits(v: Int): List<Boolean> = List(8) { bit -> ((v shr bit) and 1) == 1 }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "8-Bit Digital Word Generator",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Instrument Readout
        Surface(
            color = Color(0xFF0A121E),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.2.dp, SignalAmber.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = SignalAmber, modifier = Modifier.size(18.dp))
                        Text("GATEX WG-800", style = MaterialTheme.typography.labelMedium, color = SignalAmber)
                    }
                    Text(
                        text = "$hexStr  •  DEC $wordValue",
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = SignalCyan
                    )
                }

                Surface(
                    color = Color(0xFF13261E),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = binaryStr,
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = LogicHighGreen,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .testTag("wg_binary_readout")
                    )
                }

                // 8 interactive bit toggles B7..B0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (bit in 7 downTo 0) {
                        val isHigh = ((wordValue shr bit) and 1) == 1
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isHigh) LogicHighGreen.copy(alpha = 0.24f) else Color(0xFF132033))
                                .border(
                                    width = if (isHigh) 1.5.dp else 1.dp,
                                    color = if (isHigh) LogicHighGreen else Color(0xFF283A56),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { wordValue = wordValue xor (1 shl bit) }
                                .padding(vertical = 10.dp)
                                .testTag("wg_bit_$bit")
                        ) {
                            Text(
                                text = "B$bit",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF8AA0C0),
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (isHigh) "1" else "0",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isHigh) LogicHighGreen else Color(0xFF647896)
                            )
                        }
                    }
                }
            }
        }

        // Pattern presets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { wordValue = (wordValue + 1) and 0xFF }) {
                Text("Counter +1")
            }
            OutlinedButton(onClick = {
                val msb = (wordValue shr 7) and 1
                wordValue = (((wordValue shl 1) or msb) and 0xFF).let { if (it == 0) 1 else it }
            }) {
                Text("Ring ≪")
            }
            OutlinedButton(onClick = { wordValue = if (wordValue == 0xAA) 0x55 else 0xAA }) {
                Text("Alt AA/55")
            }
            OutlinedButton(onClick = { wordValue = 0x00 }) {
                Text("All 0s")
            }
            OutlinedButton(onClick = { wordValue = 0xFF }) {
                Text("All 1s")
            }
        }

        Text(
            text = when {
                logicInputCount > 0 -> "Will drive $logicInputCount Logic Input(s) (B0 = first input) on '${page.name}'."
                dipCount > 0 -> "Will drive $dipCount DIP Switch bank(s) (4 bits each) on '${page.name}'."
                else -> "Add Logic Inputs or a DIP Switch to '${page.name}' to receive word patterns."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { vm.applyWordGeneratorBits(currentLsbBits(wordValue)) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("wg_send_btn")
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Send Pattern")
            }
            OutlinedButton(
                onClick = {
                    val next = (wordValue + 1) and 0xFF
                    wordValue = next
                    vm.applyWordGeneratorBits(currentLsbBits(next))
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("wg_step_send_btn")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Step & Send")
            }
        }
    }
}

package com.example.ui.export

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.netlist.NetlistBuilder
import com.example.export.ExportManager
import com.example.export.ExportTheme
import com.example.export.ExportedArtifact
import com.example.export.PdfReportMetadata
import com.example.export.PngExportOptions
import com.example.ui.theme.LogicHighGreen
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SignalCyan
import com.example.viewmodel.WorkbenchViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProjectExportSheet(
    vm: WorkbenchViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Project Export Module",
                    style = MaterialTheme.typography.titleLarge,
                    color = SignalCyan
                )
                Text(
                    text = "Save schematic PNG or structured JSON report to device storage",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.testTag("close_export_sheet_btn")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close export module")
            }
        }

        ProjectExportModulePanel(vm = vm)
    }
}

@Composable
fun ProjectExportModulePanel(
    vm: WorkbenchViewModel,
    modifier: Modifier = Modifier
) {
    val activeProject by vm.activeProject.collectAsStateWithLifecycle()
    val digitalResult by vm.digitalResult.collectAsStateWithLifecycle()
    val analogResult by vm.analogResult.collectAsStateWithLifecycle()
    val localExports by vm.localExports.collectAsStateWithLifecycle()

    var exportTheme by remember { mutableStateOf(ExportTheme.DARK_LAB) }
    var includeSimOverlay by remember { mutableStateOf(true) }
    var includeGrid by remember { mutableStateOf(true) }
    var includeTitleBlock by remember { mutableStateOf(true) }
    var includeSimTelemetryInJson by remember { mutableStateOf(true) }
    var showJsonPreview by remember { mutableStateOf(false) }
    var studentName by remember { mutableStateOf("") }
    var rollNumber by remember { mutableStateOf("") }
    var labNotes by remember { mutableStateOf("") }
    val pdfReportMeta = remember(studentName, rollNumber, labNotes) {
        PdfReportMetadata(studentName = studentName, rollNumber = rollNumber, labNotes = labNotes)
    }

    val pngOptions = remember(exportTheme, includeSimOverlay, includeGrid, includeTitleBlock) {
        PngExportOptions(
            widthPx = 1600,
            heightPx = 1000,
            theme = exportTheme,
            includeSimulationOverlay = includeSimOverlay,
            includeGrid = includeGrid,
            includeTitleBlock = includeTitleBlock
        )
    }

    val previewBitmap = remember(activeProject, digitalResult, analogResult, pngOptions) {
        ExportManager.renderSchematicBitmap(
            page = activeProject.activePage(),
            projectName = activeProject.name,
            digitalResult = digitalResult,
            analogResult = analogResult,
            options = pngOptions.copy(widthPx = 960, heightPx = 600)
        ).asImageBitmap()
    }

    val activePage = activeProject.activePage()
    val netGraph = remember(activePage) { NetlistBuilder.build(activePage) }

    val savePngLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri != null) {
            vm.savePngToDeviceUri(uri, pngOptions)
        }
    }

    val saveJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            vm.saveJsonReportToDeviceUri(uri, includeSimTelemetryInJson)
        }
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            vm.savePdfReportToDeviceUri(uri, pdfReportMeta)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section 1: PNG Schematic Preview & Save to Local Storage
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = SignalCyan)
                        Column {
                            Text("PNG Schematic Image", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${activeProject.name} • ${activePage.name} (1600×1000 px)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                ) {
                    Image(
                        bitmap = previewBitmap,
                        contentDescription = "Schematic PNG export preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().height(176.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExportTheme.entries.forEach { themeOption ->
                        FilterChip(
                            selected = exportTheme == themeOption,
                            onClick = { exportTheme = themeOption },
                            label = { Text(themeOption.label) },
                            modifier = Modifier.testTag("export_theme_${themeOption.name.lowercase()}")
                        )
                    }
                    FilterChip(
                        selected = includeSimOverlay,
                        onClick = { includeSimOverlay = !includeSimOverlay },
                        label = { Text("Sim Overlay") },
                        modifier = Modifier.testTag("export_sim_overlay_chip")
                    )
                    FilterChip(
                        selected = includeGrid,
                        onClick = { includeGrid = !includeGrid },
                        label = { Text("Grid") },
                        modifier = Modifier.testTag("export_grid_chip")
                    )
                    FilterChip(
                        selected = includeTitleBlock,
                        onClick = { includeTitleBlock = !includeTitleBlock },
                        label = { Text("Title Block") },
                        modifier = Modifier.testTag("export_title_chip")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            savePngLauncher.launch(
                                ExportManager.suggestedFileName(activeProject.name, "png", "schematic")
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("export_save_png_device_btn")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save PNG to Device")
                    }
                    OutlinedButton(
                        onClick = { vm.exportPngAndShare(pngOptions) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("export_png_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share PNG", modifier = Modifier.size(18.dp))
                    }
                }

                OutlinedButton(
                    onClick = { vm.savePngLocally(pngOptions) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("export_save_png_local_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Quick-Save PNG to App Archive")
                }
            }
        }

        // Section 2: Structured JSON Project Report
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = SignalAmber)
                        Column {
                            Text("Structured JSON Project Report", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${activePage.components.size} BOM parts • ${netGraph.nets.size} electrical nets • Re-importable",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Include Simulation Telemetry", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Embeds node voltages, currents, logic levels, and ERC diagnostics",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = includeSimTelemetryInJson,
                        onCheckedChange = { includeSimTelemetryInJson = it },
                        modifier = Modifier.testTag("export_json_sim_switch")
                    )
                }

                TextButton(
                    onClick = { showJsonPreview = !showJsonPreview },
                    modifier = Modifier.testTag("toggle_json_preview_btn")
                ) {
                    Text(if (showJsonPreview) "Hide Structured JSON Preview" else "Preview Structured JSON Report")
                }

                AnimatedVisibility(visible = showJsonPreview) {
                    val previewText = remember(activeProject, digitalResult, analogResult, includeSimTelemetryInJson) {
                        val full = vm.buildStructuredJsonReportPreview(includeSimTelemetryInJson)
                        if (full.length > 2600) full.take(2600) + "\n... (${full.length} chars total)" else full
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = previewText,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            saveJsonLauncher.launch(
                                ExportManager.suggestedFileName(activeProject.name, "json", "report")
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("export_save_json_device_btn")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save JSON Report")
                    }
                    OutlinedButton(
                        onClick = { vm.exportJsonAndShare(includeSimTelemetryInJson) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("export_json_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share JSON report", modifier = Modifier.size(18.dp))
                    }
                }

                OutlinedButton(
                    onClick = { vm.saveJsonReportLocally(includeSimTelemetryInJson) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("export_save_json_local_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Quick-Save JSON Report to App Archive")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { vm.shareGatexProjectFile() },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("export_share_gatex_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Share .gatex.json", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { vm.exportBomCsvAndShare() },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("export_bom_csv_btn")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("BOM CSV", maxLines = 1)
                    }
                }

                HorizontalDivider()

                Text(
                    text = "PDF Lab Report Title Block (Optional)",
                    style = MaterialTheme.typography.titleSmall,
                    color = SignalCyan
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = studentName,
                        onValueChange = { studentName = it },
                        label = { Text("Student Name") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_student_name_input")
                    )
                    OutlinedTextField(
                        value = rollNumber,
                        onValueChange = { rollNumber = it },
                        label = { Text("Roll / ID") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.75f)
                            .testTag("export_roll_number_input")
                    )
                }
                OutlinedTextField(
                    value = labNotes,
                    onValueChange = { labNotes = it },
                    label = { Text("Lab Observation / Notes") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_lab_notes_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            savePdfLauncher.launch(
                                ExportManager.suggestedFileName(activeProject.name, "pdf", "report")
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("export_save_pdf_device_btn")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save PDF Report")
                    }
                    OutlinedButton(
                        onClick = { vm.exportPdfAndShare(pdfReportMeta) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("export_pdf_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share PDF report", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Section 3: Local App Export Archive
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = LogicHighGreen)
                    Column {
                        Text("Saved Local Exports (${localExports.size})", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Stored privately in app local storage for offline access & sharing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (localExports.isEmpty()) {
                    Text(
                        text = "No quick-saved exports yet. Tap Quick-Save PNG or JSON Report above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    localExports.forEach { artifact ->
                        LocalExportRow(
                            artifact = artifact,
                            onShare = { vm.shareLocalExport(artifact) },
                            onDelete = { vm.deleteLocalExport(artifact) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalExportRow(
    artifact: ExportedArtifact,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.US) }
    val sizeLabel = remember(artifact.sizeBytes) {
        if (artifact.sizeBytes >= 1024) {
            String.format(Locale.US, "%.1f KB", artifact.sizeBytes / 1024.0)
        } else {
            "${artifact.sizeBytes} B"
        }
    }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artifact.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$sizeLabel • ${dateFmt.format(Date(artifact.modifiedAtMs))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share ${artifact.name}")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete ${artifact.name}")
                }
            }
        }
    }
}

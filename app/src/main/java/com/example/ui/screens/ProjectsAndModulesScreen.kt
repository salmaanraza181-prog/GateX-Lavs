package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CircuitTemplates
import com.example.data.TemplateDomain
import com.example.model.CircuitProject
import com.example.ui.export.ProjectExportModulePanel
import com.example.ui.theme.LogicHighGreen
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SignalCyan
import com.example.viewmodel.WorkbenchViewModel

@Composable
fun ProjectsAndModulesScreen(
    vm: WorkbenchViewModel,
    onRequestImportFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val projects by vm.savedProjects.collectAsStateWithLifecycle()
    var tabIdx by remember { mutableIntStateOf(0) }
    var renamingProject by remember { mutableStateOf<CircuitProject?>(null) }
    var domainFilter by remember { mutableStateOf<TemplateDomain?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tabIdx) {
            Tab(
                selected = tabIdx == 0,
                onClick = { tabIdx = 0 },
                text = { Text("My Projects (${projects.size})") },
                modifier = Modifier.testTag("proj_tab_saved")
            )
            Tab(
                selected = tabIdx == 1,
                onClick = { tabIdx = 1 },
                text = { Text("Templates (${CircuitTemplates.all.size})") },
                modifier = Modifier.testTag("proj_tab_templates")
            )
            Tab(
                selected = tabIdx == 2,
                onClick = { tabIdx = 2 },
                text = { Text("Export & PCB") },
                modifier = Modifier.testTag("proj_tab_modules")
            )
        }

        when (tabIdx) {
            0 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.createNewProject() }, modifier = Modifier.weight(1f)) {
                                Text("New Circuit")
                            }
                            OutlinedButton(onClick = onRequestImportFile, modifier = Modifier.weight(1f)) {
                                Text("Import .gatex / JSON")
                            }
                        }
                    }
                    item {
                        CloudSyncPanel(vm = vm)
                    }
                    items(projects, key = { it.id }) { proj ->
                        ProjectPreviewCard(
                            project = proj,
                            onOpen = { vm.openProject(proj) },
                            onDuplicate = { vm.duplicateProject(proj) },
                            onRename = { renamingProject = proj },
                            onDelete = { vm.deleteProject(proj.id) }
                        )
                    }
                }
            }
            1 -> {
                val templates = remember(domainFilter) {
                    if (domainFilter == null) CircuitTemplates.all else CircuitTemplates.all.filter { it.domain == domainFilter }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(selected = domainFilter == null, onClick = { domainFilter = null }, label = { Text("All (${CircuitTemplates.all.size})") })
                            FilterChip(selected = domainFilter == TemplateDomain.ANALOG, onClick = { domainFilter = TemplateDomain.ANALOG }, label = { Text("Analog") })
                            FilterChip(selected = domainFilter == TemplateDomain.DIGITAL, onClick = { domainFilter = TemplateDomain.DIGITAL }, label = { Text("Digital") })
                            FilterChip(selected = domainFilter == TemplateDomain.MIXED, onClick = { domainFilter = TemplateDomain.MIXED }, label = { Text("Mixed / 555") })
                        }
                    }
                    items(templates, key = { it.id }) { tpl ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(tpl.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        tpl.domain.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (tpl.domain == TemplateDomain.DIGITAL) SignalCyan else SignalAmber
                                    )
                                }
                                Text(tpl.summary, style = MaterialTheme.typography.bodySmall)
                                Text(tpl.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Button(
                                    onClick = { vm.openTemplate(tpl) },
                                    modifier = Modifier.align(Alignment.End).testTag("tpl_open_${tpl.id}")
                                ) {
                                    Text("Open & Simulate")
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        ProjectExportModulePanel(vm = vm)
                    }

                    item {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Hardware Prototyping Modules", style = MaterialTheme.typography.titleMedium, color = LogicHighGreen)
                                Text(
                                    "Breadboard and PCB data are maintained independently from schematic diagrams.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { vm.openSubScreen("breadboard") },
                                    modifier = Modifier.fillMaxWidth().testTag("mod_breadboard_btn")
                                ) {
                                    Text("Open 830-Point Virtual Breadboard Lab")
                                }
                                Button(
                                    onClick = { vm.openSubScreen("challenges") },
                                    modifier = Modifier.fillMaxWidth().testTag("mod_challenges_btn")
                                ) {
                                    Text("Open Lab Challenges & Practical Exam")
                                }
                                Button(
                                    onClick = { vm.openSubScreen("pcb") },
                                    modifier = Modifier.fillMaxWidth().testTag("mod_pcb_btn")
                                ) {
                                    Text("Open PCB Layout Designer")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    renamingProject?.let { target ->
        var newName by remember(target.id) { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { renamingProject = null },
            title = { Text("Rename Project") },
            text = { OutlinedTextField(newName, { newName = it }, label = { Text("Name") }) },
            confirmButton = {
                Button(onClick = { vm.renameProject(target, newName); renamingProject = null }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renamingProject = null }) { Text("Cancel") }
            }
        )
    }
}

package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.CircuitTemplates
import com.example.data.TemplateDomain
import com.example.model.CircuitPage
import com.example.model.CircuitProject
import com.example.model.ComponentCatalog
import com.example.model.ComponentCategory
import com.example.model.ComponentType
import com.example.model.endpointWorldPosition
import com.example.tools.AutoWiringHelper
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.FaultRed
import com.example.ui.theme.LocalAppThemePreset
import com.example.ui.theme.LocalLabCanvasColors
import com.example.ui.theme.LogicHighGreen
import com.example.ui.theme.LogicHighZViolet
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SignalCyan
import com.example.viewmodel.AppTab
import com.example.viewmodel.WorkbenchViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    vm: WorkbenchViewModel,
    onRequestImportFile: () -> Unit,
    onOpenThemePicker: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val projects by vm.savedProjects.collectAsStateWithLifecycle()
    val recoverableDraft by vm.recoverableDraft.collectAsStateWithLifecycle()
    var renamingProject by remember { mutableStateOf<CircuitProject?>(null) }
    val activeTheme = LocalAppThemePreset.current
    val canvasColors = LocalLabCanvasColors.current
    val strings = LocalAppStrings.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Lab Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_lab_banner),
                    contentDescription = "GateX Labs Workbench Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(canvasColors.bannerOverlayStart, canvasColors.bannerOverlayEnd)
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.gatex_labs_icon_1794944999367),
                                contentDescription = "GateX Labs Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.5.dp, SignalCyan.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "GATEX LABS",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (activeTheme.isDark) MaterialTheme.colorScheme.primary else Color(0xFF7DD3FC)
                                )
                                Text(
                                    text = strings.heroSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFEAF2FF),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Surface(
                            color = Color(0xB808111E),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                .clickable(onClick = onOpenThemePicker)
                                .testTag("dash_theme_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Change app theme",
                                    tint = if (activeTheme.isDark) MaterialTheme.colorScheme.primary else Color(0xFF7DD3FC),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = strings.themeButton,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFEAF2FF),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { vm.createNewProject() },
                            modifier = Modifier.testTag("dash_new_circuit_btn")
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.newCircuit)
                        }
                        OutlinedButton(
                            onClick = onRequestImportFile,
                            modifier = Modifier.testTag("dash_open_project_btn")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp), tint = SignalAmber)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.openFile, color = Color(0xFFEAF2FF))
                        }
                    }
                }
            }
        }

        // Autosave Recovery Banner
        if (recoverableDraft != null) {
            item {
                Surface(
                    color = SignalAmber.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SignalAmber.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Unsaved Draft Detected", style = MaterialTheme.typography.titleSmall, color = SignalAmber)
                            Text("Recover '${recoverableDraft!!.name}'?", style = MaterialTheme.typography.bodySmall)
                        }
                        Row {
                            TextButton(onClick = { vm.dismissAutosaveDraft() }) { Text("Dismiss") }
                            Button(onClick = { vm.recoverAutosaveDraft() }) { Text("Recover") }
                        }
                    }
                }
            }
        }

        // 10-Section Workbench Quick Access Grid
        item {
            Text(strings.workbenchModules, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            val tiles = listOf(
                DashboardTile("New Circuit", "Blank schematic", Icons.Default.AddCircle, SignalCyan, "tile_new") {
                    vm.createNewProject()
                },
                DashboardTile("Open Project", "Import JSON file", Icons.Default.FolderOpen, SignalAmber, "tile_open") {
                    onRequestImportFile()
                },
                DashboardTile("Recent Projects", "${projects.size} saved", Icons.Default.History, LogicHighGreen, "tile_recent") {
                    vm.navigateTab(AppTab.PROJECTS)
                },
                DashboardTile("My Projects", "Manage & export", Icons.Default.Layers, SignalCyan, "tile_my_projects") {
                    vm.navigateTab(AppTab.PROJECTS)
                },
                DashboardTile("Component Library", "${ComponentCatalog.allComponents.size}+ parts, switches & ICs", Icons.Default.Memory, SignalAmber, "tile_library") {
                    vm.navigateTab(AppTab.LIBRARY)
                },
                DashboardTile("Circuit Templates", "${CircuitTemplates.all.size} lab circuits", Icons.Default.ViewModule, LogicHighGreen, "tile_templates") {
                    vm.navigateTab(AppTab.PROJECTS)
                },
                DashboardTile("Digital Electronics", "Gates, FFs & counters", Icons.Default.DeveloperBoard, SignalCyan, "tile_digital") {
                    vm.openTemplate(CircuitTemplates.all.first { it.domain == TemplateDomain.DIGITAL })
                },
                DashboardTile("Analog Electronics", "MNA DC & transient", Icons.Default.Bolt, SignalAmber, "tile_analog") {
                    vm.openTemplate(CircuitTemplates.all.first { it.domain == TemplateDomain.ANALOG })
                },
                DashboardTile("Learning Center", "Guides & formulas", Icons.Default.School, LogicHighGreen, "tile_learning") {
                    vm.openSubScreen("learning_center")
                },
                DashboardTile("Settings", "${com.example.ui.theme.AppThemePreset.entries.size} themes, grid & speed", Icons.Default.Settings, MaterialTheme.colorScheme.primary, "tile_settings") {
                    vm.navigateTab(AppTab.SETTINGS)
                }
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tiles.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { tile ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(onClick = tile.onClick)
                                    .testTag(tile.tag)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        color = tile.accent.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(tile.icon, contentDescription = tile.title, tint = tile.accent, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Column {
                                        Text(tile.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(tile.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Specialized Lab Shortcuts (Breadboard, Lab Challenges, PCB)
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { vm.openSubScreen("breadboard") },
                    modifier = Modifier.weight(1f).testTag("open_breadboard_btn"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Text("Breadboard", maxLines = 1)
                }
                Button(
                    onClick = { vm.openSubScreen("challenges") },
                    modifier = Modifier.weight(1f).testTag("open_challenges_btn"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Text("Lab Exam", maxLines = 1)
                }
                OutlinedButton(
                    onClick = { vm.openSubScreen("pcb") },
                    modifier = Modifier.weight(1f).testTag("open_pcb_btn"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Text("PCB Layout", maxLines = 1)
                }
            }
        }

        // Recent Projects Carousel / List with Circuit Previews
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Projects", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = { vm.navigateTab(AppTab.PROJECTS) }) { Text("View All") }
            }
        }

        if (projects.isEmpty()) {
            item {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No saved projects yet. Tap New Circuit or pick a template to begin.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(projects.take(6), key = { it.id }) { proj ->
                ProjectPreviewCard(
                    project = proj,
                    onOpen = { vm.openProject(proj) },
                    onDuplicate = { vm.duplicateProject(proj) },
                    onRename = { renamingProject = proj },
                    onDelete = { vm.deleteProject(proj.id) }
                )
            }
        }

        // Developer & University Credit Footer
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SignalCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .testTag("dash_developer_badge")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Developed by Salman • 26BSIT-70",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SignalAmber
                    )
                    Text(
                        text = "Student of SBBU University Nawabshah",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    renamingProject?.let { target ->
        var newName by remember(target.id) { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { renamingProject = null },
            title = { Text("Rename Project") },
            text = {
                OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Project Name") })
            },
            confirmButton = {
                Button(onClick = {
                    vm.renameProject(target, newName)
                    renamingProject = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renamingProject = null }) { Text("Cancel") }
            }
        )
    }
}

private data class DashboardTile(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val tag: String,
    val onClick: () -> Unit
)

@Composable
fun ProjectPreviewCard(
    project: CircuitProject,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(project.updatedAt) {
        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(project.updatedAt))
    }
    val page = project.activePage()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("project_card_${project.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live Mini Schematic Preview Canvas
            val previewColors = LocalLabCanvasColors.current
            MiniCircuitPreview(
                page = page,
                modifier = Modifier
                    .size(width = 96.dp, height = 70.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(previewColors.backdropEdge)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(project.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    text = "${page.components.size} parts • ${page.wires.size} wires • $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onOpen, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = "Open", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onRename, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = FaultRed, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MiniCircuitPreview(page: CircuitPage, modifier: Modifier = Modifier) {
    val canvasColors = LocalLabCanvasColors.current
    Canvas(modifier = modifier) {
        // Subtle theme-aware engineering grid lines
        val step = 12f
        var gx = step
        while (gx < size.width) {
            drawLine(canvasColors.gridMajor.copy(alpha = 0.55f), Offset(gx, 0f), Offset(gx, size.height), 0.7f)
            gx += step
        }
        var gy = step
        while (gy < size.height) {
            drawLine(canvasColors.gridMajor.copy(alpha = 0.55f), Offset(0f, gy), Offset(size.width, gy), 0.7f)
            gy += step
        }

        if (page.components.isEmpty()) return@Canvas
        val minX = page.components.minOf { it.x } - 2f
        val maxX = page.components.maxOf { it.x } + 2f
        val minY = page.components.minOf { it.y } - 2f
        val maxY = page.components.maxOf { it.y } + 2f
        val spanX = (maxX - minX).coerceAtLeast(6f)
        val spanY = (maxY - minY).coerceAtLeast(6f)
        val compsById = page.components.associateBy { it.id }
        val juncsById = page.junctions.associateBy { it.id }

        fun mapPt(x: Float, y: Float): Offset = Offset(
            x = ((x - minX) / spanX) * (size.width - 14f) + 7f,
            y = ((y - minY) / spanY) * (size.height - 14f) + 7f
        )

        for (w in page.wires) {
            val f = endpointWorldPosition(w.from, compsById, juncsById, ComponentCatalog::get) ?: continue
            val t = endpointWorldPosition(w.to, compsById, juncsById, ComponentCatalog::get) ?: continue
            val pts = listOf(f) + w.waypoints.ifEmpty { AutoWiringHelper.routeManhattan(f, t) } + listOf(t)
            val path = Path().apply {
                val p0 = mapPt(pts.first().x, pts.first().y)
                moveTo(p0.x, p0.y)
                for (i in 1 until pts.size) {
                    val pi = mapPt(pts[i].x, pts[i].y)
                    lineTo(pi.x, pi.y)
                }
            }
            drawPath(path, canvasColors.defaultWire.copy(alpha = 0.25f), style = Stroke(3.4f, cap = StrokeCap.Round))
            drawPath(path, canvasColors.defaultWire.copy(alpha = 0.92f), style = Stroke(1.3f, cap = StrokeCap.Round))
        }
        for (j in page.junctions) {
            val jp = mapPt(j.x, j.y)
            drawCircle(LogicHighGreen, 2.3f, jp)
        }
        for (c in page.components) {
            val pt = mapPt(c.x, c.y)
            val cat = ComponentCatalog.get(c.type).category
            when (c.type) {
                ComponentType.GROUND -> {
                    drawLine(LogicHighGreen, Offset(pt.x - 4f, pt.y), Offset(pt.x + 4f, pt.y), 1.6f)
                    drawLine(LogicHighGreen, Offset(pt.x - 2.5f, pt.y + 2f), Offset(pt.x + 2.5f, pt.y + 2f), 1.4f)
                }
                ComponentType.DC_SOURCE, ComponentType.AC_SOURCE, ComponentType.BATTERY,
                ComponentType.LED, ComponentType.LED_INDICATOR, ComponentType.PILOT_LAMP,
                ComponentType.PROBE_RED, ComponentType.PROBE_GREEN, ComponentType.PROBE_YELLOW,
                ComponentType.PROBE_BLUE, ComponentType.PROBE_ORANGE, ComponentType.PROBE_CYAN -> {
                    val col = when (c.type) {
                        ComponentType.PROBE_RED -> FaultRed
                        ComponentType.PROBE_GREEN -> LogicHighGreen
                        ComponentType.PROBE_YELLOW, ComponentType.PILOT_LAMP -> Color(0xFFFFEA00)
                        ComponentType.PROBE_BLUE -> Color(0xFF38BDF8)
                        ComponentType.PROBE_ORANGE -> SignalAmber
                        ComponentType.PROBE_CYAN -> SignalCyan
                        else -> if (cat == ComponentCategory.POWER) SignalAmber else LogicHighGreen
                    }
                    drawCircle(col.copy(alpha = 0.34f), 4.5f, pt)
                    drawCircle(col, 4.5f, pt, style = Stroke(1.3f))
                }
                else -> {
                    val col = when (cat) {
                        ComponentCategory.LOGIC_GATES, ComponentCategory.IC_7400 -> LogicHighZViolet
                        ComponentCategory.INPUT -> Color(0xFFFFD54F)
                        ComponentCategory.INSTRUMENTS -> Color(0xFF2DD4BF)
                        else -> SignalAmber
                    }
                    drawRoundRect(
                        color = col.copy(alpha = 0.35f),
                        topLeft = Offset(pt.x - 5f, pt.y - 3.5f),
                        size = Size(10f, 7f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                    drawRoundRect(
                        color = col,
                        topLeft = Offset(pt.x - 5f, pt.y - 3.5f),
                        size = Size(10f, 7f),
                        cornerRadius = CornerRadius(2f, 2f),
                        style = Stroke(1.2f)
                    )
                }
            }
        }
    }
}

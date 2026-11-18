package com.example.ui.canvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.netlist.NetlistBuilder
import com.example.model.CircuitPage
import com.example.model.ComponentCatalog
import com.example.model.ComponentInstance
import com.example.model.ComponentType
import com.example.model.DiagnosticSeverity
import com.example.model.GridPoint
import com.example.model.LogicLevel
import com.example.model.WireEndpoint
import com.example.model.WireJunction
import com.example.model.endpointWorldPosition
import com.example.model.pinWorldPosition
import com.example.tools.AutoWiringHelper
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.FaultRed
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalLabCanvasColors
import com.example.ui.theme.LogicHighGreen
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SignalCyan
import com.example.viewmodel.SimulationRunState
import com.example.viewmodel.WorkbenchViewModel
import com.example.viewmodel.WorkspaceTool
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.round

private val INTERACTIVE_TYPES = setOf(
    ComponentType.LOGIC_INPUT,
    ComponentType.TOGGLE_SWITCH,
    ComponentType.SPDT_SWITCH,
    ComponentType.DPST_SWITCH,
    ComponentType.DPDT_SWITCH,
    ComponentType.PUSH_BUTTON,
    ComponentType.PUSH_BUTTON_NC,
    ComponentType.ROTARY_SWITCH,
    ComponentType.KEYPAD,
    ComponentType.DIP_SWITCH
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircuitWorkspaceScreen(
    vm: WorkbenchViewModel,
    onOpenInstruments: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenExport: () -> Unit = {},
    onOpenThemePicker: () -> Unit = {},
    isFullScreen: Boolean = false,
    onToggleFullScreen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val project by vm.activeProject.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val tool by vm.tool.collectAsStateWithLifecycle()
    val selectedIds by vm.selectedComponentIds.collectAsStateWithLifecycle()
    val pendingWireStart by vm.pendingWireStart.collectAsStateWithLifecycle()
    val simState by vm.simRunState.collectAsStateWithLifecycle()
    val digitalResult by vm.digitalResult.collectAsStateWithLifecycle()
    val analogResult by vm.analogResult.collectAsStateWithLifecycle()
    val ercDiagnostics by vm.ercDiagnostics.collectAsStateWithLifecycle()
    val canUndo by vm.canUndo.collectAsStateWithLifecycle()
    val canRedo by vm.canRedo.collectAsStateWithLifecycle()
    val simSpeed by vm.simSpeedMultiplier.collectAsStateWithLifecycle()
    val customIcs by vm.customIcs.collectAsStateWithLifecycle()
    val canvasColors = LocalLabCanvasColors.current
    val strings = LocalAppStrings.current

    val page = project.activePage()
    var zoom by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(24f) }
    var panY by remember { mutableFloatStateOf(24f) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var lastFittedPageKey by remember { mutableStateOf("") }

    var multiSelectMode by remember { mutableStateOf(false) }
    var showProbes by remember { mutableStateOf(true) }
    var showCurrentFlow by remember { mutableStateOf(true) }
    var activeBin by remember { mutableStateOf<WorkbenchBin?>(WorkbenchBin.SWITCHES) }
    var activeDragCompId by remember { mutableStateOf<String?>(null) }
    var activeDragJunctionId by remember { mutableStateOf<String?>(null) }
    var selectedWireId by remember { mutableStateOf<String?>(null) }

    // Live Drag-to-Connect Wire preview state
    var dragWireStart by remember { mutableStateOf<WireEndpoint?>(null) }
    var dragWireCursorWorld by remember { mutableStateOf<GridPoint?>(null) }
    var dragWireSnapTarget by remember { mutableStateOf<WireEndpoint?>(null) }
    var dragWireSnapBranch by remember { mutableStateOf<Pair<String, GridPoint>?>(null) }

    var showQuickAddSheet by remember { mutableStateOf(false) }
    var showQuickTemplatesSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showSaveCustomIcDialog by remember { mutableStateOf(false) }
    var showMyCustomIcsSheet by remember { mutableStateOf(false) }
    var editingComponent by remember { mutableStateOf<ComponentInstance?>(null) }
    var datasheetComponent by remember { mutableStateOf<ComponentInstance?>(null) }
    var showErcDialog by remember { mutableStateOf(false) }

    val baseGridPx = settings.gridSpacingDp * 2.1f
    val gridPx = baseGridPx * zoom
    val textMeasurer = rememberTextMeasurer()
    val haptic = LocalHapticFeedback.current

    fun fitCircuitToViewport() {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return
        if (page.components.isEmpty()) {
            zoom = 1f
            panX = 24f
            panY = 24f
            return
        }
        val minX = page.components.minOf { it.x } - 3.5f
        val maxX = page.components.maxOf { it.x } + 3.5f
        val minY = page.components.minOf { it.y } - 3.5f
        val maxY = page.components.maxOf { it.y } + 3.5f
        val spanGridX = (maxX - minX).coerceAtLeast(8f)
        val spanGridY = (maxY - minY).coerceAtLeast(8f)
        val fitZoomX = (canvasSize.width * 0.86f) / (spanGridX * baseGridPx)
        val fitZoomY = (canvasSize.height * 0.80f) / (spanGridY * baseGridPx)
        val newZoom = min(fitZoomX, fitZoomY).coerceIn(0.55f, 1.55f)
        val newGridPx = baseGridPx * newZoom
        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f
        zoom = newZoom
        panX = canvasSize.width / 2f - centerX * newGridPx
        panY = canvasSize.height / 2f - centerY * newGridPx
    }

    LaunchedEffect(project.id, page.id, canvasSize) {
        val key = "${project.id}_${page.id}"
        if (canvasSize.width > 0 && canvasSize.height > 0 && lastFittedPageKey != key) {
            lastFittedPageKey = key
            fitCircuitToViewport()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "simPulse")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse"
    )

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (!isFullScreen) {
        // Top Header + Simulation Transport Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showRenameDialog = true }
                            .padding(end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = project.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Rename project",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = "${page.components.size} parts • ${page.wires.size} wires • ${(zoom * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleFullScreen,
                            modifier = Modifier.size(36.dp).testTag("ws_fullscreen_btn")
                        ) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Full screen workspace", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
                        }
                        IconButton(
                            onClick = onOpenThemePicker,
                            modifier = Modifier.size(36.dp).testTag("ws_theme_btn")
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = "Change app theme", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(19.dp))
                        }
                        IconButton(
                            onClick = { showQuickTemplatesSheet = true },
                            modifier = Modifier.size(36.dp).testTag("ws_templates_btn")
                        ) {
                            Icon(Icons.Default.ViewModule, contentDescription = "Lab Templates", tint = LogicHighGreen, modifier = Modifier.size(19.dp))
                        }
                        IconButton(
                            onClick = { vm.saveCurrentProject() },
                            modifier = Modifier.size(36.dp).testTag("save_project_btn")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save project", modifier = Modifier.size(19.dp))
                        }
                        IconButton(
                            onClick = onOpenExport,
                            modifier = Modifier.size(36.dp).testTag("open_export_btn")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Export project", tint = LogicHighGreen, modifier = Modifier.size(19.dp))
                        }
                        IconButton(
                            onClick = { vm.runErcCheck(); showErcDialog = true },
                            modifier = Modifier.size(36.dp).testTag("erc_check_btn")
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = "Check circuit", modifier = Modifier.size(19.dp))
                        }
                        IconButton(
                            onClick = onOpenInstruments,
                            modifier = Modifier.size(36.dp).testTag("open_instruments_btn")
                        ) {
                            Icon(Icons.Default.Science, contentDescription = "Virtual instruments", tint = SignalCyan, modifier = Modifier.size(19.dp))
                        }
                        IconButton(
                            onClick = onOpenAiAssistant,
                            modifier = Modifier.size(36.dp).testTag("open_ai_btn")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Circuit Assistant", tint = SignalAmber, modifier = Modifier.size(19.dp))
                        }
                    }
                }

                // Multi-page tabs + Run/Step/Pause/Stop simulation controls
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        project.pages.forEach { p ->
                            FilterChip(
                                selected = p.id == page.id,
                                onClick = { vm.switchPage(p.id) },
                                label = { Text(p.name, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                        AssistChip(
                            onClick = { vm.addPage() },
                            label = { Text("+ Page") },
                            modifier = Modifier.testTag("add_page_btn")
                        )
                    }

                    Spacer(Modifier.width(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        val speeds = listOf(0.25f, 0.5f, 1.0f, 2.0f, 5.0f)
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    val idx = speeds.indexOfFirst { abs(it - simSpeed) < 0.05f }
                                    val next = speeds[(idx + 1) % speeds.size]
                                    vm.setSimSpeedMultiplier(next)
                                }
                                .testTag("sim_speed_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                val speedLabel = if (simSpeed >= 1f) "${simSpeed.toInt()}×" else "${simSpeed}×"
                                Text(speedLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                        FilledSimIconButton(
                            active = simState == SimulationRunState.RUNNING,
                            activeColor = LogicHighGreen,
                            icon = Icons.Default.PlayArrow,
                            contentDescription = "Run simulation",
                            tag = "sim_run_btn",
                            onClick = { vm.runSimulation() }
                        )
                        FilledSimIconButton(
                            active = false,
                            activeColor = SignalCyan,
                            icon = Icons.Default.SkipNext,
                            contentDescription = "Step simulation 1 tick",
                            tag = "sim_step_btn",
                            onClick = { vm.stepSimulationOnce(toggleClocks = true) }
                        )
                        FilledSimIconButton(
                            active = simState == SimulationRunState.PAUSED,
                            activeColor = SignalAmber,
                            icon = Icons.Default.Pause,
                            contentDescription = "Pause simulation",
                            tag = "sim_pause_btn",
                            onClick = { vm.pauseSimulation() }
                        )
                        FilledSimIconButton(
                            active = false,
                            activeColor = FaultRed,
                            icon = Icons.Default.Stop,
                            contentDescription = "Stop simulation",
                            tag = "sim_stop_btn",
                            onClick = { vm.stopSimulation() }
                        )
                    }
                }
            }
        }

        // Workspace Action Toolbar (Select, Move, Wire, Delete, Rotate, Probes, Fit, Auto-Route, Undo, Redo)
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolToggleChip(strings.selectLabel, Icons.Default.PanTool, tool == WorkspaceTool.SELECT, "tool_select") {
                    vm.setTool(WorkspaceTool.SELECT)
                }
                ToolToggleChip(strings.moveLabel, Icons.Default.OpenWith, tool == WorkspaceTool.MOVE, "tool_move") {
                    vm.setTool(WorkspaceTool.MOVE)
                }
                ToolToggleChip(strings.wireLabel, Icons.Default.Cable, tool == WorkspaceTool.WIRE, "tool_wire") {
                    vm.setTool(WorkspaceTool.WIRE)
                }
                ToolToggleChip(strings.unwireLabel, Icons.Default.LinkOff, tool == WorkspaceTool.UNWIRE, "tool_unwire") {
                    vm.setTool(if (tool == WorkspaceTool.UNWIRE) WorkspaceTool.SELECT else WorkspaceTool.UNWIRE)
                }
                AssistChip(
                    onClick = {
                        if (vm.undoLastWire()) {
                            selectedWireId = null
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    enabled = page.wires.isNotEmpty(),
                    label = { Text(strings.undoWireLabel) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("undo_wire_btn")
                )
                AssistChip(
                    onClick = onToggleFullScreen,
                    label = { Text(strings.fullScreenLabel) },
                    leadingIcon = { Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("toolbar_fullscreen_btn")
                )
                ToolToggleChip(strings.deleteLabel, Icons.Default.Delete, tool == WorkspaceTool.DELETE, "tool_delete") {
                    vm.setTool(WorkspaceTool.DELETE)
                }
                ToolToggleChip(strings.rotateLabel, Icons.AutoMirrored.Filled.RotateRight, tool == WorkspaceTool.ROTATE, "tool_rotate") {
                    if (selectedIds.isNotEmpty()) vm.rotateSelected() else vm.setTool(WorkspaceTool.ROTATE)
                }
                FilterChip(
                    selected = multiSelectMode,
                    onClick = { multiSelectMode = !multiSelectMode },
                    label = { Text("Multi") },
                    leadingIcon = { Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = showProbes,
                    onClick = { showProbes = !showProbes },
                    label = { Text("Probes") },
                    leadingIcon = { Icon(Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("toggle_probes_btn")
                )
                FilterChip(
                    selected = showCurrentFlow,
                    onClick = { showCurrentFlow = !showCurrentFlow },
                    label = { Text("Flow") },
                    modifier = Modifier.testTag("toggle_flow_btn")
                )
                AssistChip(
                    onClick = { showSaveCustomIcDialog = true },
                    label = { Text("Save as IC") },
                    modifier = Modifier.testTag("save_custom_ic_btn")
                )
                if (customIcs.isNotEmpty()) {
                    AssistChip(
                        onClick = { showMyCustomIcsSheet = true },
                        label = { Text("My ICs (${customIcs.size})") },
                        modifier = Modifier.testTag("open_my_ics_btn")
                    )
                }
                AssistChip(
                    onClick = { vm.autoRouteAllWires() },
                    label = { Text("Auto-Route") },
                    modifier = Modifier.testTag("auto_route_btn")
                )
                IconButton(onClick = { fitCircuitToViewport() }, modifier = Modifier.testTag("fit_view_btn")) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = "Fit circuit to screen", tint = SignalCyan)
                }
                IconButton(onClick = { zoom = (zoom * 1.2f).coerceAtMost(2.5f) }, modifier = Modifier.testTag("zoom_in_btn")) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in")
                }
                IconButton(onClick = { zoom = (zoom / 1.2f).coerceAtLeast(0.45f) }, modifier = Modifier.testTag("zoom_out_btn")) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom out")
                }
                IconButton(onClick = { vm.undo() }, enabled = canUndo, modifier = Modifier.testTag("undo_btn")) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                }
                IconButton(onClick = { vm.redo() }, enabled = canRedo, modifier = Modifier.testTag("redo_btn")) {
                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                }
                IconButton(onClick = { showClearConfirm = true }, modifier = Modifier.testTag("clear_sheet_btn")) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear sheet", tint = FaultRed)
                }
            }
        }

        // Electronic Workbench (EWB) Categorized Parts Compartments ("Alag Khane")
        WorkbenchPartsBin(
            selectedBin = activeBin,
            isDark = settings.isDarkTheme,
            onSelectBin = { activeBin = it },
            onPickComponent = { type ->
                val idx = page.components.size
                val centerWorldX = if (canvasSize.width > 0) (canvasSize.width / 2f - panX) / gridPx else 8f
                val centerWorldY = if (canvasSize.height > 0) (canvasSize.height / 2f - panY) / gridPx else 6f
                val offsetCols = ((idx % 3) - 1) * 3.5f
                val offsetRows = (((idx / 3) % 3) - 1) * 3.0f
                vm.addComponent(
                    type = type,
                    gridX = (centerWorldX + offsetCols).coerceAtLeast(2f),
                    gridY = (centerWorldY + offsetRows).coerceAtLeast(2f)
                )
            }
        )

        // Simulation / Wire Guidance Banner
        val combinedDiags = (digitalResult.diagnostics + analogResult.diagnostics).distinctBy { it.code }
        if (simState != SimulationRunState.STOPPED || pendingWireStart != null || tool == WorkspaceTool.WIRE || tool == WorkspaceTool.UNWIRE) {
            Surface(
                color = when {
                    digitalResult.hasShortCircuit || !analogResult.converged || tool == WorkspaceTool.UNWIRE -> FaultRed.copy(alpha = 0.20f)
                    pendingWireStart != null || tool == WorkspaceTool.WIRE -> SignalAmber.copy(alpha = 0.16f)
                    else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val bannerText = when {
                        tool == WorkspaceTool.UNWIRE -> "UNWIRE MODE: Tap any wrong wire or connected pin to disconnect it."
                        pendingWireStart != null -> "WIRE MODE: Tap target pin or existing wire to connect."
                        tool == WorkspaceTool.WIRE -> "WIRE TOOL: Drag from any pin/junction to another pin or wire."
                        combinedDiags.any { it.severity == DiagnosticSeverity.ERROR } ->
                            combinedDiags.first { it.severity == DiagnosticSeverity.ERROR }.message
                        simState == SimulationRunState.RUNNING ->
                            "LIVE • Tick ${digitalResult.tickCount} • Nodes: ${analogResult.nodeVoltages.size} • Tap switches to flip"
                        else -> "PAUSED • Tick ${digitalResult.tickCount} • Tap Step (+1) or Run"
                    }
                    Text(
                        text = bannerText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (digitalResult.hasShortCircuit || tool == WorkspaceTool.UNWIRE) FaultRed else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (pendingWireStart != null) {
                        TextButton(
                            onClick = { pendingWireStart?.let { vm.handlePinOrJunctionTap(it) } },
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Cancel", style = MaterialTheme.typography.labelSmall, color = SignalAmber)
                        }
                    } else if (tool == WorkspaceTool.UNWIRE) {
                        TextButton(
                            onClick = { vm.setTool(WorkspaceTool.SELECT) },
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Done", style = MaterialTheme.typography.labelSmall, color = FaultRed)
                        }
                    }
                }
            }
        }
        } // end if (!isFullScreen)

        // Main Interactive Circuit Canvas with Unified Lag-Free Gesture Engine
        val compsById = remember(page.components) { page.components.associateBy { it.id } }
        val juncsById = remember(page.junctions) { page.junctions.associateBy { it.id } }

        val statePage by rememberUpdatedState(page)
        val stateTool by rememberUpdatedState(tool)
        val stateMulti by rememberUpdatedState(multiSelectMode)
        val stateCompsById by rememberUpdatedState(compsById)
        val stateJuncsById by rememberUpdatedState(juncsById)
        val statePendingWire by rememberUpdatedState(pendingWireStart)
        val stateGridPx by rememberUpdatedState(gridPx)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { canvasSize = it }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("circuit_canvas")
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startScreenPos = down.position
                            var lastSinglePos = startScreenPos
                            var totalMovePx = 0f
                            var exceededSlop = false
                            var didMultiTouch = false

                            val gPx = stateGridPx
                            val worldDownX = (startScreenPos.x - panX) / gPx
                            val worldDownY = (startScreenPos.y - panY) / gPx

                            val downHitJunction = statePage.junctions.firstOrNull { j ->
                                hypot((j.x - worldDownX).toDouble(), (j.y - worldDownY).toDouble()) < 0.85
                            }
                            val downHitPin = findClosestPin(
                                page = statePage,
                                wx = worldDownX,
                                wy = worldDownY,
                                maxDistGrid = if (stateTool == WorkspaceTool.WIRE) 1.05f else 0.56f
                            )
                            val downHitComp = findComponentNearPoint(statePage, worldDownX, worldDownY)

                            // Determine if this touch down is eligible for direct drag-to-connect wiring:
                            // 1) In WIRE mode: touching any junction or pin starts drag-wire.
                            // 2) In SELECT mode: touching an exposed pin on a multi-pin component (outside center body)
                            //    or touching a pin with no body overlap starts drag-wire immediately!
                            val directPinInSelect = stateTool == WorkspaceTool.SELECT && downHitPin != null && run {
                                val pinComp = downHitPin.componentId?.let { stateCompsById[it] }
                                val pinCount = pinComp?.let { ComponentCatalog.metadataFor(it).pins.size } ?: 0
                                val distFromBodyCenter = if (pinComp != null) {
                                    hypot((worldDownX - pinComp.x).toDouble(), (worldDownY - pinComp.y).toDouble())
                                } else 99.0
                                pinCount >= 2 && (downHitComp == null || distFromBodyCenter > 0.65)
                            }
                            val wireStartCandidate: WireEndpoint? = when {
                                stateTool == WorkspaceTool.WIRE -> {
                                    downHitJunction?.let { WireEndpoint(junctionId = it.id) } ?: downHitPin
                                }
                                directPinInSelect -> downHitPin
                                else -> null
                            }

                            var draggingWireEp: WireEndpoint? = null
                            var draggingCompId: String? = null
                            var draggingJuncId: String? = null
                            var dragStartSnapshot: CircuitPage? = null
                            var rawCompX = 0f
                            var rawCompY = 0f
                            var rawJuncX = 0f
                            var rawJuncY = 0f
                            var lastSnapKey: String? = null

                            while (true) {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.isEmpty()) break

                                if (pressed.size >= 2) {
                                    didMultiTouch = true
                                    if (draggingCompId != null || draggingJuncId != null) {
                                        dragStartSnapshot?.let { snap -> vm.commitUndoCheckpoint(snap) }
                                        draggingCompId = null
                                        draggingJuncId = null
                                        dragStartSnapshot = null
                                        activeDragCompId = null
                                        activeDragJunctionId = null
                                    }
                                    if (draggingWireEp != null) {
                                        draggingWireEp = null
                                        dragWireStart = null
                                        dragWireCursorWorld = null
                                        dragWireSnapTarget = null
                                        dragWireSnapBranch = null
                                    }
                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()
                                    val centroid = event.calculateCentroid(useCurrent = true)
                                    val prevZoom = zoom
                                    val nextZoom = (prevZoom * zoomChange).coerceIn(0.45f, 2.5f)
                                    val ratio = nextZoom / prevZoom
                                    zoom = nextZoom
                                    panX = centroid.x - (centroid.x - panX) * ratio + panChange.x
                                    panY = centroid.y - (centroid.y - panY) * ratio + panChange.y
                                    event.changes.forEach { it.consume() }
                                } else {
                                    val ch = pressed.first()
                                    val delta = ch.position - lastSinglePos
                                    lastSinglePos = ch.position
                                    totalMovePx += hypot(delta.x.toDouble(), delta.y.toDouble()).toFloat()

                                    if (!exceededSlop && totalMovePx > viewConfiguration.touchSlop) {
                                        exceededSlop = true
                                        when {
                                            wireStartCandidate != null -> {
                                                draggingWireEp = wireStartCandidate
                                                dragWireStart = wireStartCandidate
                                                selectedWireId = null
                                                lastSnapKey = null
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            downHitJunction != null && stateTool != WorkspaceTool.WIRE && stateTool != WorkspaceTool.UNWIRE && stateTool != WorkspaceTool.DELETE -> {
                                                draggingJuncId = downHitJunction.id
                                                dragStartSnapshot = statePage
                                                rawJuncX = downHitJunction.x
                                                rawJuncY = downHitJunction.y
                                                activeDragJunctionId = downHitJunction.id
                                                selectedWireId = null
                                            }
                                            downHitComp != null && stateTool != WorkspaceTool.WIRE && stateTool != WorkspaceTool.UNWIRE && stateTool != WorkspaceTool.DELETE -> {
                                                draggingCompId = downHitComp.id
                                                dragStartSnapshot = statePage
                                                rawCompX = downHitComp.x
                                                rawCompY = downHitComp.y
                                                activeDragCompId = downHitComp.id
                                                selectedWireId = null
                                                vm.selectComponent(downHitComp.id)
                                            }
                                        }
                                    }

                                    if (exceededSlop && !didMultiTouch) {
                                        when {
                                            draggingWireEp != null -> {
                                                val curWX = (ch.position.x - panX) / stateGridPx
                                                val curWY = (ch.position.y - panY) / stateGridPx
                                                val snapEp = findClosestEndpointExcluding(
                                                    page = statePage,
                                                    wx = curWX,
                                                    wy = curWY,
                                                    exclude = draggingWireEp,
                                                    maxDistGrid = 1.15f
                                                )
                                                if (snapEp != null) {
                                                    val snapPos = endpointWorldPosition(snapEp, stateCompsById, stateJuncsById, ComponentCatalog::get)
                                                    dragWireSnapTarget = snapEp
                                                    dragWireSnapBranch = null
                                                    dragWireCursorWorld = snapPos ?: GridPoint(curWX, curWY)
                                                    val sk = snapEp.key()
                                                    if (sk != lastSnapKey) {
                                                        lastSnapKey = sk
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                } else {
                                                    lastSnapKey = null
                                                    val nearWire = findWireNearPoint(statePage, curWX, curWY, stateCompsById, stateJuncsById)
                                                    val canBranch = nearWire != null &&
                                                        nearWire.from.key() != draggingWireEp.key() &&
                                                        nearWire.to.key() != draggingWireEp.key()
                                                    if (canBranch && nearWire != null) {
                                                        val snappedBranchPt = GridPoint(
                                                            round(curWX * 2f) / 2f,
                                                            round(curWY * 2f) / 2f
                                                        )
                                                        dragWireSnapTarget = null
                                                        dragWireSnapBranch = nearWire.id to snappedBranchPt
                                                        dragWireCursorWorld = snappedBranchPt
                                                    } else {
                                                        dragWireSnapTarget = null
                                                        dragWireSnapBranch = null
                                                        dragWireCursorWorld = GridPoint(curWX, curWY)
                                                    }
                                                }
                                            }
                                            draggingJuncId != null -> {
                                                rawJuncX += delta.x / stateGridPx
                                                rawJuncY += delta.y / stateGridPx
                                                vm.moveJunctionTo(draggingJuncId, rawJuncX, rawJuncY)
                                            }
                                            draggingCompId != null -> {
                                                // Accumulate un-snapped delta so grid snap never traps component!
                                                rawCompX += delta.x / stateGridPx
                                                rawCompY += delta.y / stateGridPx
                                                vm.moveComponentTo(draggingCompId, rawCompX, rawCompY)
                                            }
                                            else -> {
                                                panX += delta.x
                                                panY += delta.y
                                            }
                                        }
                                        ch.consume()
                                    }
                                }
                            }

                            if (draggingWireEp != null) {
                                val targetEp = dragWireSnapTarget
                                val branchTarget = dragWireSnapBranch
                                if (targetEp != null) {
                                    if (vm.connectWire(draggingWireEp, targetEp)) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                } else if (branchTarget != null) {
                                    vm.branchAndConnectWire(
                                        from = draggingWireEp,
                                        targetWireId = branchTarget.first,
                                        gridX = branchTarget.second.x,
                                        gridY = branchTarget.second.y
                                    )
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                dragWireStart = null
                                dragWireCursorWorld = null
                                dragWireSnapTarget = null
                                dragWireSnapBranch = null
                                return@awaitEachGesture
                            }

                            if (draggingCompId != null || draggingJuncId != null) {
                                dragStartSnapshot?.let { snap -> vm.commitUndoCheckpoint(snap) }
                                activeDragCompId = null
                                activeDragJunctionId = null
                                return@awaitEachGesture
                            }

                            if (!exceededSlop && !didMultiTouch) {
                                // Handle clean single Tap
                                val curGridPx = stateGridPx
                                val wx = (startScreenPos.x - panX) / curGridPx
                                val wy = (startScreenPos.y - panY) / curGridPx
                                val curPage = statePage
                                val curTool = stateTool

                                // 0. UNWIRE mode: safely disconnect wire, junction, or wires on tapped pin without deleting components
                                if (curTool == WorkspaceTool.UNWIRE) {
                                    val hitWireUnwire = findWireNearPoint(curPage, wx, wy, stateCompsById, stateJuncsById)
                                    if (hitWireUnwire != null) {
                                        vm.deleteWire(hitWireUnwire.id)
                                        selectedWireId = null
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        return@awaitEachGesture
                                    }
                                    val hitJUnwire = curPage.junctions.firstOrNull { j ->
                                        hypot((j.x - wx).toDouble(), (j.y - wy).toDouble()) < 0.95
                                    }
                                    if (hitJUnwire != null) {
                                        vm.deleteJunction(hitJUnwire.id)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        return@awaitEachGesture
                                    }
                                    val hitPinUnwire = findClosestPin(curPage, wx, wy, maxDistGrid = 1.05f)
                                    if (hitPinUnwire != null && vm.disconnectWiresAtPin(hitPinUnwire) > 0) {
                                        selectedWireId = null
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        return@awaitEachGesture
                                    }
                                    return@awaitEachGesture
                                }

                                // 0b. Delete Junction tap in Delete mode
                                if (curTool == WorkspaceTool.DELETE) {
                                    val hitJDel = curPage.junctions.firstOrNull { j ->
                                        hypot((j.x - wx).toDouble(), (j.y - wy).toDouble()) < 0.90
                                    }
                                    if (hitJDel != null) {
                                        vm.deleteJunction(hitJDel.id)
                                        return@awaitEachGesture
                                    }
                                }

                                // 1. Wire mode or completing pending wire: check pins/junctions/wires
                                if (curTool == WorkspaceTool.WIRE || statePendingWire != null) {
                                    val hitJ = curPage.junctions.firstOrNull { j ->
                                        hypot((j.x - wx).toDouble(), (j.y - wy).toDouble()) < 1.05
                                    }
                                    if (hitJ != null) {
                                        vm.handlePinOrJunctionTap(WireEndpoint(junctionId = hitJ.id))
                                        return@awaitEachGesture
                                    }
                                    val hitPin = findClosestPin(curPage, wx, wy, maxDistGrid = 1.05f)
                                    if (hitPin != null) {
                                        vm.handlePinOrJunctionTap(hitPin)
                                        return@awaitEachGesture
                                    }
                                    val hitWireForBranch = findWireNearPoint(curPage, wx, wy, stateCompsById, stateJuncsById)
                                    if (hitWireForBranch != null) {
                                        vm.branchWireWithJunction(hitWireForBranch.id, wx, wy)
                                        return@awaitEachGesture
                                    }
                                }

                                // 2. Component Tap
                                val tappedComp = findComponentNearPoint(curPage, wx, wy)
                                if (tappedComp != null) {
                                    selectedWireId = null
                                    when (curTool) {
                                        WorkspaceTool.DELETE -> {
                                            vm.selectComponent(tappedComp.id)
                                            vm.deleteSelected()
                                        }
                                        WorkspaceTool.ROTATE -> {
                                            vm.selectComponent(tappedComp.id)
                                            vm.rotateSelected()
                                        }
                                        else -> {
                                            vm.selectComponent(tappedComp.id, stateMulti)
                                            if (tappedComp.type in INTERACTIVE_TYPES) {
                                                vm.toggleInteractiveInput(tappedComp.id)
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        }
                                    }
                                    return@awaitEachGesture
                                }

                                // 3. Wire Tap
                                val tappedWire = findWireNearPoint(curPage, wx, wy, stateCompsById, stateJuncsById)
                                if (tappedWire != null) {
                                    vm.selectComponent(null)
                                    if (curTool == WorkspaceTool.DELETE) {
                                        vm.deleteWire(tappedWire.id)
                                        selectedWireId = null
                                    } else {
                                        selectedWireId = tappedWire.id
                                    }
                                    return@awaitEachGesture
                                }

                                // 4. Empty Canvas Tap
                                selectedWireId = null
                                vm.selectComponent(null)
                                statePendingWire?.let { vm.handlePinOrJunctionTap(it) }
                            }
                        }
                    }
            ) {
                // High-Contrast Theme-Synced Laboratory Backdrop
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(canvasColors.backdropCenter, canvasColors.backdropEdge),
                        center = Offset(size.width * 0.5f, size.height * 0.45f),
                        radius = size.maxDimension * 0.85f
                    )
                )

                // Engineering Grid + CAD Crosshair Reticles
                if (settings.showGrid) {
                    val minorColor = canvasColors.gridMinor
                    val majorColor = canvasColors.gridMajor
                    val crossColor = canvasColors.gridCross
                    val startX = (panX % gridPx) - gridPx
                    val startY = (panY % gridPx) - gridPx
                    var x = startX
                    var col = 0
                    while (x < size.width + gridPx) {
                        drawLine(
                            color = if (col % 4 == 0) majorColor else minorColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = if (col % 4 == 0) 1.3f else 0.7f
                        )
                        x += gridPx
                        col++
                    }
                    var y = startY
                    var row = 0
                    while (y < size.height + gridPx) {
                        drawLine(
                            color = if (row % 4 == 0) majorColor else minorColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = if (row % 4 == 0) 1.3f else 0.7f
                        )
                        y += gridPx
                        row++
                    }
                    // Precision intersection crosshairs on major 4x4 vertices
                    var mx = startX
                    var mc = 0
                    while (mx < size.width + gridPx) {
                        if (mc % 4 == 0) {
                            var my = startY
                            var mr = 0
                            while (my < size.height + gridPx) {
                                if (mr % 4 == 0) {
                                    drawLine(crossColor, Offset(mx - 4f, my), Offset(mx + 4f, my), 1.4f)
                                    drawLine(crossColor, Offset(mx, my - 4f), Offset(mx, my + 4f), 1.4f)
                                }
                                my += gridPx
                                mr++
                            }
                        }
                        mx += gridPx
                        mc++
                    }
                }

                // Alignment Crosshair Guides while dragging a component or junction
                activeDragCompId?.let { dragId ->
                    page.components.firstOrNull { it.id == dragId }?.let { dc ->
                        val sx = panX + dc.x * gridPx
                        val sy = panY + dc.y * gridPx
                        val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                        drawLine(SignalCyan.copy(alpha = 0.45f), Offset(sx, 0f), Offset(sx, size.height), 1.3f, pathEffect = dash)
                        drawLine(SignalCyan.copy(alpha = 0.45f), Offset(0f, sy), Offset(size.width, sy), 1.3f, pathEffect = dash)
                    }
                }
                activeDragJunctionId?.let { jId ->
                    page.junctions.firstOrNull { it.id == jId }?.let { dj ->
                        val sx = panX + dj.x * gridPx
                        val sy = panY + dj.y * gridPx
                        val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                        drawLine(LogicHighGreen.copy(alpha = 0.45f), Offset(sx, 0f), Offset(sx, size.height), 1.3f, pathEffect = dash)
                        drawLine(LogicHighGreen.copy(alpha = 0.45f), Offset(0f, sy), Offset(size.width, sy), 1.3f, pathEffect = dash)
                    }
                }

                val activeWireAnchor = dragWireStart ?: pendingWireStart
                val pendingPinPair = if (activeWireAnchor?.isComponentPin == true) {
                    activeWireAnchor.componentId!! to activeWireAnchor.pinId!!
                } else null

                // Draw Wires with outer neon glow + sharp core + digital/analog energy pulse dots
                for (wire in page.wires) {
                    val from = endpointWorldPosition(wire.from, compsById, juncsById, ComponentCatalog::get) ?: continue
                    val to = endpointWorldPosition(wire.to, compsById, juncsById, ComponentCatalog::get) ?: continue
                    val pts = listOf(from) + wire.waypoints.ifEmpty { AutoWiringHelper.routeManhattan(from, to) } + listOf(to)

                    val wireLevel = digitalResult.wireLevels[wire.id]
                    val analogV = if (simState != SimulationRunState.STOPPED) {
                        val vFrom = if (wire.from.isComponentPin) {
                            analogResult.pinVoltages[NetlistBuilder.pinKey(wire.from.componentId!!, wire.from.pinId!!)]
                        } else null
                        val vTo = if (wire.to.isComponentPin) {
                            analogResult.pinVoltages[NetlistBuilder.pinKey(wire.to.componentId!!, wire.to.pinId!!)]
                        } else null
                        vFrom ?: vTo
                    } else null
                    val isAnalogEnergized = analogV != null && abs(analogV) >= 0.4
                    val isWireSelected = wire.id == selectedWireId
                    val isBranchHighlight = dragWireSnapBranch?.first == wire.id
                    val customWireColor = wire.colorHex?.let { Color(it) }
                    val wireColor = when {
                        isBranchHighlight -> SignalAmber
                        isWireSelected -> SignalAmber
                        simState != SimulationRunState.STOPPED && wireLevel != null && wireLevel != LogicLevel.UNKNOWN ->
                            if (wireLevel == LogicLevel.ZERO && customWireColor != null) customWireColor
                            else logicLevelColor(wireLevel, customWireColor ?: canvasColors.defaultWire)
                        isAnalogEnergized -> customWireColor ?: SignalAmber
                        else -> customWireColor ?: canvasColors.defaultWire
                    }

                    val path = Path().apply {
                        moveTo(panX + pts.first().x * gridPx, panY + pts.first().y * gridPx)
                        for (i in 1 until pts.size) {
                            lineTo(panX + pts[i].x * gridPx, panY + pts[i].y * gridPx)
                        }
                    }
                    drawPath(
                        path = path,
                        color = wireColor.copy(
                            alpha = when {
                                isWireSelected || isBranchHighlight -> 0.44f
                                simState == SimulationRunState.RUNNING && (wireLevel == LogicLevel.ONE || isAnalogEnergized) -> 0.36f
                                else -> 0.17f
                            }
                        ),
                        style = Stroke(width = if (isWireSelected || isBranchHighlight) 11.5f else 8.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = path,
                        color = wireColor,
                        style = Stroke(width = if (isWireSelected || isBranchHighlight) 4.4f else 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    val showPulse = showCurrentFlow && simState == SimulationRunState.RUNNING && pts.size >= 2 &&
                        (wireLevel == LogicLevel.ONE || isAnalogEnergized)
                    if (showPulse) {
                        val pulseCol = if (wireLevel == LogicLevel.ONE) LogicHighGreen else SignalAmber
                        val dotOffsets = listOf(0f, 0.33f, 0.66f)
                        dotOffsets.forEach { off ->
                            val t = (pulsePhase + off) % 1f
                            val pulsePt = samplePolylinePoint(pts, t)
                            val ax = panX + pulsePt.x * gridPx
                            val ay = panY + pulsePt.y * gridPx
                            drawCircle(pulseCol.copy(alpha = 0.45f), radius = 7.5f, center = Offset(ax, ay))
                            drawCircle(Color.White, radius = 3.2f, center = Offset(ax, ay))
                        }
                    }
                }

                // Draw Solder-Pad Junctions
                for (j in page.junctions) {
                    val jx = panX + j.x * gridPx
                    val jy = panY + j.y * gridPx
                    val isPending = activeWireAnchor?.junctionId == j.id || dragWireSnapTarget?.junctionId == j.id
                    val jCol = if (isPending) SignalAmber else LogicHighGreen
                    drawCircle(jCol.copy(alpha = 0.28f), radius = if (isPending) 15f else 9.5f, center = Offset(jx, jy))
                    drawCircle(jCol, radius = if (isPending) 8.5f else 5.5f, center = Offset(jx, jy))
                }

                // Draw Components
                for (comp in page.components) {
                    val shifted = comp.copy(
                        x = comp.x + panX / gridPx,
                        y = comp.y + panY / gridPx
                    )
                    drawSchematicComponent(
                        comp = shifted,
                        gridPx = gridPx,
                        isSelected = comp.id in selectedIds,
                        isDark = settings.isDarkTheme,
                        simRunning = simState != SimulationRunState.STOPPED,
                        pulsePhase = pulsePhase,
                        digitalResult = digitalResult,
                        analogResult = analogResult,
                        textMeasurer = textMeasurer,
                        showLabel = settings.showComponentLabels,
                        showValue = settings.showComponentValues,
                        showProbes = showProbes,
                        pendingWireComponentPin = pendingPinPair,
                        selectionAccent = canvasColors.selectionAccent
                    )
                }

                // Luminous Live Drag-to-Connect Preview Wire + Magnetic Snap Target Reticle
                val dStart = dragWireStart
                val dCursor = dragWireCursorWorld
                if (dStart != null && dCursor != null) {
                    val startWorld = endpointWorldPosition(dStart, compsById, juncsById, ComponentCatalog::get)
                    if (startWorld != null) {
                        val endWorld = when {
                            dragWireSnapTarget != null ->
                                endpointWorldPosition(dragWireSnapTarget!!, compsById, juncsById, ComponentCatalog::get) ?: dCursor
                            dragWireSnapBranch != null -> dragWireSnapBranch!!.second
                            else -> dCursor
                        }
                        val previewPts = listOf(startWorld) + AutoWiringHelper.routeManhattan(startWorld, endWorld) + listOf(endWorld)
                        val previewColor = when {
                            dragWireSnapTarget != null -> LogicHighGreen
                            dragWireSnapBranch != null -> SignalAmber
                            else -> SignalCyan
                        }
                        val previewPath = Path().apply {
                            moveTo(panX + previewPts.first().x * gridPx, panY + previewPts.first().y * gridPx)
                            for (i in 1 until previewPts.size) {
                                lineTo(panX + previewPts[i].x * gridPx, panY + previewPts[i].y * gridPx)
                            }
                        }

                        // Layer 1: Wide glowing aura
                        drawPath(
                            path = previewPath,
                            color = previewColor.copy(alpha = 0.28f),
                            style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        // Layer 2: Crisp solid core conductor
                        drawPath(
                            path = previewPath,
                            color = previewColor,
                            style = Stroke(width = 4.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        // Layer 3: Animated directional signal flow dashes
                        drawPath(
                            path = previewPath,
                            color = Color.White.copy(alpha = 0.78f),
                            style = Stroke(
                                width = 2.0f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(11f, 11f), phase = -pulsePhase * 44f)
                            )
                        )

                        // Start & Target endpoint reticles + floating HUD badge
                        val sx = panX + startWorld.x * gridPx
                        val sy = panY + startWorld.y * gridPx
                        drawCircle(previewColor.copy(alpha = 0.35f), radius = 15f, center = Offset(sx, sy))
                        drawCircle(previewColor, radius = 7.5f, center = Offset(sx, sy))

                        val tx = panX + endWorld.x * gridPx
                        val ty = panY + endWorld.y * gridPx
                        if (dragWireSnapTarget != null || dragWireSnapBranch != null) {
                            drawCircle(previewColor.copy(alpha = 0.25f), radius = 21f, center = Offset(tx, ty))
                            drawCircle(previewColor, radius = 15f, center = Offset(tx, ty), style = Stroke(2.4f))
                            drawCircle(previewColor, radius = 7f, center = Offset(tx, ty))
                        } else {
                            drawCircle(previewColor.copy(alpha = 0.20f), radius = 14f, center = Offset(tx, ty))
                            drawCircle(previewColor, radius = 10f, center = Offset(tx, ty), style = Stroke(1.8f))
                            drawCircle(Color.White, radius = 4f, center = Offset(tx, ty))
                        }

                        val hudText = when {
                            dragWireSnapTarget != null -> {
                                val ep = dragWireSnapTarget!!
                                if (ep.isComponentPin) {
                                    val c = compsById[ep.componentId]
                                    val pLabel = c?.let { ComponentCatalog.metadataFor(it).pins.firstOrNull { p -> p.id == ep.pinId }?.label }
                                    "SNAP • ${c?.label ?: ""}:${pLabel ?: ep.pinId}"
                                } else "SNAP • JUNCTION"
                            }
                            dragWireSnapBranch != null -> "BRANCH + SOLDER DOT"
                            else -> "DRAG TO PIN"
                        }
                        val hudMeasured = textMeasurer.measure(
                            hudText,
                            TextStyle(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color.White
                            )
                        )
                        val bx = (tx - hudMeasured.size.width / 2f).coerceIn(10f, (size.width - hudMeasured.size.width - 14f).coerceAtLeast(10f))
                        val by = (ty - hudMeasured.size.height - 28f).coerceAtLeast(8f)
                        drawRoundRect(
                            color = Color(0xE6081120),
                            topLeft = Offset(bx - 7f, by - 4f),
                            size = Size(hudMeasured.size.width + 14f, hudMeasured.size.height + 8f),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                        drawRoundRect(
                            color = previewColor,
                            topLeft = Offset(bx - 7f, by - 4f),
                            size = Size(hudMeasured.size.width + 14f, hudMeasured.size.height + 8f),
                            cornerRadius = CornerRadius(8f, 8f),
                            style = Stroke(1.4f)
                        )
                        drawText(hudMeasured, topLeft = Offset(bx, by))
                    }
                }
            }

            if (isFullScreen) {
                // Distraction-Free Full-Screen Minimal Floating Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 10.dp, top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
                        shape = RoundedCornerShape(14.dp),
                        tonalElevation = 4.dp,
                        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (simState == SimulationRunState.RUNNING) vm.stopSimulation() else vm.runSimulation()
                                },
                                modifier = Modifier.size(38.dp).testTag("fs_sim_toggle_btn")
                            ) {
                                Icon(
                                    imageVector = if (simState == SimulationRunState.RUNNING) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (simState == SimulationRunState.RUNNING) "Stop simulation" else "Run simulation",
                                    tint = if (simState == SimulationRunState.RUNNING) FaultRed else LogicHighGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Surface(
                                color = if (tool == WorkspaceTool.WIRE) SignalAmber.copy(alpha = 0.24f) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = if (tool == WorkspaceTool.WIRE) 1.dp else 0.dp,
                                        color = if (tool == WorkspaceTool.WIRE) SignalAmber else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                            ) {
                                IconButton(
                                    onClick = {
                                        val next = if (tool == WorkspaceTool.WIRE) WorkspaceTool.SELECT else WorkspaceTool.WIRE
                                        vm.setTool(next)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                    modifier = Modifier.size(38.dp).testTag("fs_wire_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cable,
                                        contentDescription = "Connect wire tool",
                                        tint = if (tool == WorkspaceTool.WIRE) SignalAmber else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                            Surface(
                                color = if (tool == WorkspaceTool.UNWIRE) FaultRed.copy(alpha = 0.22f) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = if (tool == WorkspaceTool.UNWIRE) 1.dp else 0.dp,
                                        color = if (tool == WorkspaceTool.UNWIRE) FaultRed else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                            ) {
                                IconButton(
                                    onClick = {
                                        vm.setTool(if (tool == WorkspaceTool.UNWIRE) WorkspaceTool.SELECT else WorkspaceTool.UNWIRE)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                    modifier = Modifier.size(38.dp).testTag("fs_unwire_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LinkOff,
                                        contentDescription = "Disconnect wire tool",
                                        tint = if (tool == WorkspaceTool.UNWIRE) FaultRed else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    if (vm.undoLastWire()) {
                                        selectedWireId = null
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                                enabled = page.wires.isNotEmpty(),
                                modifier = Modifier.size(38.dp).testTag("fs_undo_wire_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "Undo last wire",
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            IconButton(
                                onClick = { fitCircuitToViewport() },
                                modifier = Modifier.size(38.dp).testTag("fs_fit_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CenterFocusStrong,
                                    contentDescription = "Fit circuit",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }

                    if (tool == WorkspaceTool.WIRE || pendingWireStart != null) {
                        Surface(
                            color = SignalAmber.copy(alpha = 0.90f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    pendingWireStart?.let { vm.handlePinOrJunctionTap(it) }
                                    vm.setTool(WorkspaceTool.SELECT)
                                }
                                .testTag("fs_wire_banner")
                        ) {
                            Text(
                                text = if (pendingWireStart != null) {
                                    "WIRE • Tap target pin or wire (Tap to cancel)"
                                } else {
                                    "WIRE ON • Drag or tap pins to connect (Tap to stop)"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF08101E),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    } else if (tool == WorkspaceTool.UNWIRE) {
                        Surface(
                            color = FaultRed.copy(alpha = 0.88f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { vm.setTool(WorkspaceTool.SELECT) }
                        ) {
                            Text(
                                text = "UNWIRE ON • Tap wire/pin (Tap to stop)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Exit Full-Screen Pill (Top-End)
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(14.dp),
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f), RoundedCornerShape(14.dp))
                        .clickable(onClick = onToggleFullScreen)
                        .testTag("ws_exit_fullscreen_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Exit full screen",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            text = strings.exitLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                // Top-Right Quick Status & ERC HUD Badge
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 12.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            width = 1.dp,
                            color = when {
                                digitalResult.hasShortCircuit || tool == WorkspaceTool.UNWIRE -> FaultRed.copy(alpha = 0.7f)
                                simState == SimulationRunState.RUNNING -> LogicHighGreen.copy(alpha = 0.65f)
                                else -> MaterialTheme.colorScheme.outlineVariant
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { vm.runErcCheck(); showErcDialog = true }
                        .testTag("canvas_hud_pill")
                ) {
                    val maxV = analogResult.pinVoltages.values.maxOfOrNull { abs(it) }
                    val hudLabel = when {
                        digitalResult.hasShortCircuit -> "SHORT CIRCUIT"
                        tool == WorkspaceTool.UNWIRE -> "UNWIRE • TAP WIRE/PIN"
                        simState == SimulationRunState.RUNNING && maxV != null ->
                            "LIVE • ${String.format("%.1fV", maxV)} peak"
                        simState == SimulationRunState.RUNNING -> "LIVE • SIM ON"
                        tool == WorkspaceTool.WIRE -> "DRAG PIN → PIN"
                        else -> "DRAG PIN TO WIRE"
                    }
                    val hudColor = when {
                        digitalResult.hasShortCircuit || tool == WorkspaceTool.UNWIRE -> FaultRed
                        simState == SimulationRunState.RUNNING -> LogicHighGreen
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Text(
                        text = hudLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = hudColor,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }

                // Blueprint Watermark Title Pill (Bottom-Left)
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = if (selectedIds.isNotEmpty() || selectedWireId != null) 78.dp else 12.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                ) {
                    Text(
                        text = "GATEX LABS • ${page.name} • ${(zoom * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }

                // Floating Add Component FAB
                FloatingActionButton(
                    onClick = { showQuickAddSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = if (selectedIds.isNotEmpty() || selectedWireId != null) 76.dp else 16.dp)
                        .testTag("add_component_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add component")
                }
            }

            // Bottom Wire Inspector Bar when a wire is tapped
            androidx.compose.animation.AnimatedVisibility(
                visible = selectedWireId != null && selectedIds.isEmpty(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                selectedWireId?.let { wId ->
                    val activeWire = page.wires.firstOrNull { it.id == wId }
                    val wireSummary = if (activeWire != null) {
                        fun epLabel(ep: WireEndpoint): String = if (ep.isComponentPin) {
                            val c = compsById[ep.componentId]
                            val p = c?.let { ComponentCatalog.metadataFor(it).pins.firstOrNull { pin -> pin.id == ep.pinId }?.label }
                            "${c?.label ?: "?"}:${p ?: ep.pinId}"
                        } else "Node"
                        "${epLabel(activeWire.from)} ↔ ${epLabel(activeWire.to)}"
                    } else "Wire Selected"
                    val wireSwatches = listOf<Pair<String, Long?>>(
                        "Default" to null,
                        "Red" to 0xFFFF3B30L,
                        "Black" to 0xFF1E293BL,
                        "Green" to 0xFF22C55EL,
                        "Blue" to 0xFF3B82F6L,
                        "Yellow" to 0xFFFACC15L,
                        "Orange" to 0xFFFF7A00L,
                        "Violet" to 0xFFA855F7L
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 6.dp,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text("Wire Connection", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(wireSummary, style = MaterialTheme.typography.titleSmall, color = SignalAmber, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AssistChip(
                                        onClick = {
                                            if (activeWire != null) {
                                                val f = endpointWorldPosition(activeWire.from, compsById, juncsById, ComponentCatalog::get)
                                                val t = endpointWorldPosition(activeWire.to, compsById, juncsById, ComponentCatalog::get)
                                                if (f != null && t != null) {
                                                    vm.branchWireWithJunction(wId, (f.x + t.x) / 2f, (f.y + t.y) / 2f)
                                                    vm.setTool(WorkspaceTool.WIRE)
                                                }
                                            }
                                            selectedWireId = null
                                        },
                                        label = { Text("Branch") }
                                    )
                                    Button(
                                        onClick = {
                                            vm.deleteWire(wId)
                                            selectedWireId = null
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = FaultRed),
                                        modifier = Modifier.testTag("disconnect_wire_btn")
                                    ) {
                                        Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text(strings.disconnectLabel)
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Color:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                wireSwatches.forEachIndexed { idx, (name, hex) ->
                                    val isChosen = activeWire?.colorHex == hex
                                    val dotColor = hex?.let { Color(it) } ?: canvasColors.defaultWire
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                            .border(
                                                width = if (isChosen) 2.5.dp else 1.dp,
                                                color = if (isChosen) Color.White else MaterialTheme.colorScheme.outline,
                                                shape = CircleShape
                                            )
                                            .clickable { vm.setWireColor(wId, hex) }
                                            .testTag("wire_color_swatch_$idx")
                                    ) {
                                        if (hex == null) {
                                            Text("A", style = MaterialTheme.typography.labelSmall, color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Selection Inspector Bar when component(s) are selected
            val primarySelected = page.components.firstOrNull { it.id in selectedIds }
            androidx.compose.animation.AnimatedVisibility(
                visible = primarySelected != null,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                if (primarySelected != null) {
                    val hasAttachedWires = page.wires.any {
                        it.from.componentId == primarySelected.id || it.to.componentId == primarySelected.id
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 6.dp,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.padding(end = 6.dp)) {
                                Text(primarySelected.label, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    primarySelected.value.ifBlank { ComponentCatalog.get(primarySelected.type).shortCode },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (primarySelected.type in ComponentCatalog.variableInputGateTypes) {
                                val curIn = (primarySelected.properties["inputCount"]?.toIntOrNull() ?: 2).coerceIn(2, 8)
                                val steps = listOf(2, 3, 4, 5, 6, 8)
                                val nextIn = steps[(steps.indexOf(curIn).let { if (it < 0) 0 else it } + 1) % steps.size]
                                AssistChip(
                                    onClick = { vm.setGateInputCount(primarySelected.id, nextIn) },
                                    label = { Text("Inputs: $curIn → $nextIn") },
                                    modifier = Modifier.testTag("sel_gate_inputs_btn")
                                )
                            }
                            if (primarySelected.type in INTERACTIVE_TYPES) {
                                AssistChip(
                                    onClick = { vm.toggleInteractiveInput(primarySelected.id) },
                                    label = { Text("Flip") },
                                    leadingIcon = {
                                        Icon(Icons.Default.ToggleOn, contentDescription = null, tint = LogicHighGreen, modifier = Modifier.size(16.dp))
                                    },
                                    modifier = Modifier.testTag("toggle_selected_btn")
                                )
                            }
                            if (primarySelected.type == ComponentType.FUSE && primarySelected.properties["blown"] == "true") {
                                AssistChip(
                                    onClick = { vm.resetFuse(primarySelected.id) },
                                    label = { Text("Reset Fuse", color = FaultRed) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = FaultRed, modifier = Modifier.size(16.dp))
                                    },
                                    modifier = Modifier.testTag("reset_fuse_btn")
                                )
                            }
                            if (hasAttachedWires) {
                                AssistChip(
                                    onClick = {
                                        vm.disconnectComponentWires(primarySelected.id)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                    label = { Text(strings.unwireLabel) },
                                    leadingIcon = {
                                        Icon(Icons.Default.LinkOff, contentDescription = null, tint = FaultRed, modifier = Modifier.size(16.dp))
                                    },
                                    modifier = Modifier.testTag("sel_unwire_part_btn")
                                )
                            }
                            AssistChip(
                                onClick = { datasheetComponent = primarySelected },
                                label = { Text(strings.datasheetLabel) },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = SignalCyan, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.testTag("component_datasheet_btn")
                            )
                            AssistChip(
                                onClick = { editingComponent = primarySelected },
                                label = { Text("Edit") },
                                leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.testTag("edit_component_btn")
                            )
                            IconButton(onClick = { vm.rotateSelected() }, modifier = Modifier.testTag("sel_rotate_btn")) {
                                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate")
                            }
                            IconButton(onClick = { vm.flipSelected() }, modifier = Modifier.testTag("sel_flip_btn")) {
                                Icon(Icons.Default.Flip, contentDescription = "Flip")
                            }
                            IconButton(onClick = { vm.duplicateSelected() }, modifier = Modifier.testTag("sel_duplicate_btn")) {
                                Icon(Icons.Default.Add, contentDescription = "Duplicate")
                            }
                            IconButton(onClick = { vm.copySelected() }, modifier = Modifier.testTag("sel_copy_btn")) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            }
                            IconButton(onClick = { vm.pasteClipboard() }, modifier = Modifier.testTag("sel_paste_btn")) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                            }
                            IconButton(onClick = { vm.deleteSelected() }, modifier = Modifier.testTag("sel_delete_btn")) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = FaultRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQuickAddSheet) {
        ModalBottomSheet(onDismissRequest = { showQuickAddSheet = false }) {
            QuickComponentPickerSheet(
                isDark = settings.isDarkTheme,
                onSelect = { type ->
                    val idx = page.components.size
                    val centerWorldX = if (canvasSize.width > 0) (canvasSize.width / 2f - panX) / gridPx else 8f
                    val centerWorldY = if (canvasSize.height > 0) (canvasSize.height / 2f - panY) / gridPx else 6f
                    vm.addComponent(
                        type = type,
                        gridX = (centerWorldX + ((idx % 3) - 1) * 3.5f).coerceAtLeast(2f),
                        gridY = (centerWorldY + (((idx / 3) % 3) - 1) * 3.0f).coerceAtLeast(2f)
                    )
                    showQuickAddSheet = false
                }
            )
        }
    }

    if (showQuickTemplatesSheet) {
        ModalBottomSheet(onDismissRequest = { showQuickTemplatesSheet = false }) {
            QuickLabTemplatesSheet(
                onSelectTemplate = { tpl ->
                    vm.openTemplate(tpl)
                    showQuickTemplatesSheet = false
                },
                onClose = { showQuickTemplatesSheet = false }
            )
        }
    }

    editingComponent?.let { target ->
        val latest = page.components.firstOrNull { it.id == target.id } ?: target
        ModalBottomSheet(onDismissRequest = { editingComponent = null }) {
            ComponentPropertyEditorSheet(
                component = latest,
                analogResult = analogResult,
                onSave = { updated ->
                    vm.updateComponent(updated)
                    editingComponent = null
                },
                onToggleDipBit = { bitIdx -> vm.toggleDipBit(latest.id, bitIdx) },
                onClose = { editingComponent = null }
            )
        }
    }

    datasheetComponent?.let { target ->
        val latest = page.components.firstOrNull { it.id == target.id } ?: target
        ComponentDatasheetSheet(
            component = latest,
            isDark = settings.isDarkTheme,
            onDismiss = { datasheetComponent = null }
        )
    }

    if (showRenameDialog) {
        var newTitle by remember(project.name) { mutableStateOf(project.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Active Project") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    vm.renameActiveProject(newTitle)
                    showRenameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Schematic Sheet?") },
            text = { Text("Remove all components, wires, and junctions from '${page.name}'? You can still Undo right after.") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.clearActivePage()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FaultRed)
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showErcDialog) {
        ModalBottomSheet(onDismissRequest = { showErcDialog = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Electrical Rules & Connection Check", style = MaterialTheme.typography.headlineSmall)
                ercDiagnostics.forEach { d ->
                    Surface(
                        color = when (d.severity) {
                            DiagnosticSeverity.ERROR -> FaultRed.copy(alpha = 0.15f)
                            DiagnosticSeverity.WARNING -> SignalAmber.copy(alpha = 0.15f)
                            DiagnosticSeverity.INFO -> LogicHighGreen.copy(alpha = 0.15f)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "[${d.severity.name}] ${d.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showSaveCustomIcDialog) {
        var icName by remember { mutableStateOf(project.name.take(18)) }
        var icCode by remember { mutableStateOf("MYIC") }
        AlertDialog(
            onDismissRequest = { showSaveCustomIcDialog = false },
            title = { Text("Package Active Circuit as Custom IC") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Captures the active schematic's truth table (up to 4 Logic Inputs × 4 Output Probes) into a reusable chip.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = icName,
                        onValueChange = { icName = it },
                        label = { Text("Custom IC Name (e.g. Half Adder IC)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("custom_ic_name_input")
                    )
                    OutlinedTextField(
                        value = icCode,
                        onValueChange = { icCode = it.uppercase().take(6) },
                        label = { Text("Chip Code (max 6 chars, e.g. HADD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("custom_ic_code_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (vm.packageActiveCircuitAsCustomIc(icName, icCode)) {
                            showSaveCustomIcDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_save_custom_ic_btn")
                ) { Text("Save IC") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveCustomIcDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showMyCustomIcsSheet) {
        ModalBottomSheet(onDismissRequest = { showMyCustomIcsSheet = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("My Custom ICs (${customIcs.size})", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Tap any custom chip to place it directly on your active schematic.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                customIcs.forEach { def ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val centerWorldX = if (canvasSize.width > 0) (canvasSize.width / 2f - panX) / gridPx else 10f
                                val centerWorldY = if (canvasSize.height > 0) (canvasSize.height / 2f - panY) / gridPx else 8f
                                vm.placeCustomIc(def, centerWorldX.coerceAtLeast(3f), centerWorldY.coerceAtLeast(3f))
                                showMyCustomIcsSheet = false
                            }
                            .testTag("place_custom_ic_${def.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${def.name} [${def.shortCode}]", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "${def.inputLabels.size} In (${def.inputLabels.joinToString(",")}) → ${def.outputLabels.size} Out (${def.outputLabels.joinToString(",")})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SignalCyan
                                )
                            }
                            IconButton(onClick = { vm.deleteCustomIc(def.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete custom IC", tint = FaultRed)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun FilledSimIconButton(
    active: Boolean,
    activeColor: Color,
    icon: ImageVector,
    contentDescription: String,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        color = if (active) activeColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, if (active) activeColor else Color.Transparent, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = if (active) activeColor else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ToolToggleChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
        modifier = Modifier.testTag(tag)
    )
}

private fun findComponentNearPoint(
    page: CircuitPage,
    wx: Float,
    wy: Float
): ComponentInstance? = page.components.lastOrNull { comp ->
    val meta = ComponentCatalog.metadataFor(comp)
    val rotNorm = ((comp.rotationDegrees % 180) + 180) % 180
    val rawHalfW = (if (rotNorm == 90) meta.heightGrid else meta.widthGrid) / 2f
    val rawHalfH = (if (rotNorm == 90) meta.widthGrid else meta.heightGrid) / 2f
    val halfW = (rawHalfW + 0.7f).coerceAtLeast(1.75f)
    val halfH = (rawHalfH + 0.7f).coerceAtLeast(1.55f)
    abs(wx - comp.x) <= halfW && abs(wy - comp.y) <= halfH
}

private fun findClosestPin(
    page: CircuitPage,
    wx: Float,
    wy: Float,
    maxDistGrid: Float
): WireEndpoint? {
    var bestEndpoint: WireEndpoint? = null
    var bestDist = maxDistGrid.toDouble()
    for (comp in page.components) {
        val meta = ComponentCatalog.metadataFor(comp)
        for (pin in meta.pins) {
            val wp = pinWorldPosition(comp, pin)
            val d = hypot((wp.x - wx).toDouble(), (wp.y - wy).toDouble())
            if (d < bestDist) {
                bestDist = d
                bestEndpoint = WireEndpoint(componentId = comp.id, pinId = pin.id)
            }
        }
    }
    return bestEndpoint
}

private fun findClosestEndpointExcluding(
    page: CircuitPage,
    wx: Float,
    wy: Float,
    exclude: WireEndpoint,
    maxDistGrid: Float
): WireEndpoint? {
    var bestEndpoint: WireEndpoint? = null
    var bestDist = maxDistGrid.toDouble()
    for (j in page.junctions) {
        val ep = WireEndpoint(junctionId = j.id)
        if (ep.key() == exclude.key()) continue
        val d = hypot((j.x - wx).toDouble(), (j.y - wy).toDouble())
        if (d < bestDist) {
            bestDist = d
            bestEndpoint = ep
        }
    }
    for (comp in page.components) {
        val meta = ComponentCatalog.metadataFor(comp)
        for (pin in meta.pins) {
            val ep = WireEndpoint(componentId = comp.id, pinId = pin.id)
            if (ep.key() == exclude.key()) continue
            val wp = pinWorldPosition(comp, pin)
            val d = hypot((wp.x - wx).toDouble(), (wp.y - wy).toDouble())
            if (d < bestDist) {
                bestDist = d
                bestEndpoint = ep
            }
        }
    }
    return bestEndpoint
}

private fun findWireNearPoint(
    page: CircuitPage,
    wx: Float,
    wy: Float,
    compsById: Map<String, ComponentInstance>,
    juncsById: Map<String, WireJunction>
) = page.wires.firstOrNull { wire ->
    val from = endpointWorldPosition(wire.from, compsById, juncsById, ComponentCatalog::get) ?: return@firstOrNull false
    val to = endpointWorldPosition(wire.to, compsById, juncsById, ComponentCatalog::get) ?: return@firstOrNull false
    val pts = listOf(from) + wire.waypoints.ifEmpty { AutoWiringHelper.routeManhattan(from, to) } + listOf(to)
    (0 until pts.size - 1).any { i ->
        distancePointToSegment(wx, wy, pts[i], pts[i + 1]) < 0.80f
    }
}

private fun samplePolylinePoint(pts: List<GridPoint>, t: Float): GridPoint {
    if (pts.size < 2) return pts.firstOrNull() ?: GridPoint(0f, 0f)
    val segLens = FloatArray(pts.size - 1)
    var total = 0f
    for (i in 0 until pts.size - 1) {
        val len = hypot((pts[i + 1].x - pts[i].x).toDouble(), (pts[i + 1].y - pts[i].y).toDouble()).toFloat()
        segLens[i] = len
        total += len
    }
    if (total < 1e-4f) return pts.first()
    var target = t.coerceIn(0f, 1f) * total
    for (i in segLens.indices) {
        val len = segLens[i]
        if (target <= len || i == segLens.lastIndex) {
            val frac = if (len > 1e-4f) (target / len).coerceIn(0f, 1f) else 0f
            return GridPoint(
                x = pts[i].x + (pts[i + 1].x - pts[i].x) * frac,
                y = pts[i].y + (pts[i + 1].y - pts[i].y) * frac
            )
        }
        target -= len
    }
    return pts.last()
}

private fun distancePointToSegment(px: Float, py: Float, a: GridPoint, b: GridPoint): Float {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val lenSq = dx * dx + dy * dy
    if (lenSq < 1e-6f) return hypot((px - a.x).toDouble(), (py - a.y).toDouble()).toFloat()
    val t = (((px - a.x) * dx + (py - a.y) * dy) / lenSq).coerceIn(0f, 1f)
    val projX = a.x + t * dx
    val projY = a.y + t * dy
    return hypot((px - projX).toDouble(), (py - projY).toDouble()).toFloat()
}

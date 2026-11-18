package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiAssistantState
import com.example.ai.AiCircuitRepository
import com.example.data.AppSettings
import com.example.data.CircuitTemplate
import com.example.data.CircuitTemplates
import com.example.data.CloudSyncRepository
import com.example.data.CustomIcStore
import com.example.data.ProjectRepository
import com.example.data.SalmanDatabase
import com.example.data.SettingsRepository
import com.example.engine.analog.AnalogSimulationEngine
import com.example.engine.digital.DigitalMemoryState
import com.example.engine.digital.DigitalSimulationEngine
import com.example.engine.digital.TruthTableResult
import com.example.engine.netlist.NetlistBuilder
import com.example.export.ExportManager
import com.example.export.ExportedArtifact
import com.example.export.PdfReportMetadata
import com.example.export.PngExportOptions
import com.example.model.CustomIcDefinition
import com.example.tools.ChallengeVerificationResult
import com.example.tools.LabChallenge
import com.example.tools.LabChallengeEngine
import com.example.model.AnalogSimulationResult
import com.example.model.BreadboardJumper
import com.example.model.BreadboardPlacement
import com.example.model.CircuitPage
import com.example.model.CircuitProject
import com.example.model.ComponentCatalog
import com.example.model.ComponentInstance
import com.example.model.ComponentType
import com.example.model.DigitalSimulationResult
import com.example.model.PcbBoardState
import com.example.model.PcbFootprint
import com.example.model.PcbLayer
import com.example.model.PcbPad
import com.example.model.PcbTrace
import com.example.model.PcbVia
import com.example.model.SimulationDiagnostic
import com.example.model.WireEndpoint
import com.example.model.WireJunction
import com.example.model.WireSegment
import com.example.tools.AutoWiringHelper
import com.example.ui.theme.AppThemePreset
import com.example.tools.BooleanEngine
import com.example.tools.CircuitConnectionChecker
import com.example.tools.LabAudioFeedback
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

enum class WorkspaceTool { SELECT, MOVE, WIRE, UNWIRE, DELETE, ROTATE }
enum class SimulationRunState { STOPPED, RUNNING, PAUSED }
enum class AppTab(val label: String) {
    HOME("Home"),
    DESIGN("Design"),
    LIBRARY("Library"),
    PROJECTS("Projects"),
    SETTINGS("Settings")
}

class WorkbenchViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SalmanDatabase.getInstance(application)
    private val projectRepo = ProjectRepository(db.projectDao())
    private val settingsRepo = SettingsRepository(application)
    private val customIcStore = CustomIcStore(application)
    val cloudSyncRepo = CloudSyncRepository(application)
    private val digitalEngine = DigitalSimulationEngine()
    private val analogEngine = AnalogSimulationEngine()
    private val aiRepo = AiCircuitRepository()

    val customIcs: StateFlow<List<CustomIcDefinition>> = customIcStore.customIcs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val settings: StateFlow<AppSettings> = settingsRepo.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    val savedProjects: StateFlow<List<CircuitProject>> = projectRepo.savedProjects.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _currentTab = MutableStateFlow(AppTab.HOME)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _activeProject = MutableStateFlow(
        CircuitTemplates.byId("half_adder")!!.toNewProject()
    )
    val activeProject: StateFlow<CircuitProject> = _activeProject.asStateFlow()

    private val _recoverableDraft = MutableStateFlow<CircuitProject?>(null)
    val recoverableDraft: StateFlow<CircuitProject?> = _recoverableDraft.asStateFlow()

    private val _tool = MutableStateFlow(WorkspaceTool.SELECT)
    val tool: StateFlow<WorkspaceTool> = _tool.asStateFlow()

    private val _selectedComponentIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedComponentIds: StateFlow<Set<String>> = _selectedComponentIds.asStateFlow()

    private val _clipboard = MutableStateFlow<List<ComponentInstance>>(emptyList())
    val clipboard: StateFlow<List<ComponentInstance>> = _clipboard.asStateFlow()

    private val _pendingWireStart = MutableStateFlow<WireEndpoint?>(null)
    val pendingWireStart: StateFlow<WireEndpoint?> = _pendingWireStart.asStateFlow()

    private val _simRunState = MutableStateFlow(SimulationRunState.STOPPED)
    val simRunState: StateFlow<SimulationRunState> = _simRunState.asStateFlow()

    private val _digitalResult = MutableStateFlow(DigitalSimulationResult())
    val digitalResult: StateFlow<DigitalSimulationResult> = _digitalResult.asStateFlow()

    private val _analogResult = MutableStateFlow(AnalogSimulationResult())
    val analogResult: StateFlow<AnalogSimulationResult> = _analogResult.asStateFlow()

    private val _ercDiagnostics = MutableStateFlow<List<SimulationDiagnostic>>(emptyList())
    val ercDiagnostics: StateFlow<List<SimulationDiagnostic>> = _ercDiagnostics.asStateFlow()

    private val _truthTable = MutableStateFlow<TruthTableResult?>(null)
    val truthTable: StateFlow<TruthTableResult?> = _truthTable.asStateFlow()

    private val _aiState = MutableStateFlow<AiAssistantState>(AiAssistantState.Idle)
    val aiState: StateFlow<AiAssistantState> = _aiState.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _subScreen = MutableStateFlow<String?>(null)
    val subScreen: StateFlow<String?> = _subScreen.asStateFlow()

    private val _localExports = MutableStateFlow<List<ExportedArtifact>>(emptyList())
    val localExports: StateFlow<List<ExportedArtifact>> = _localExports.asStateFlow()

    private val _simSpeedMultiplier = MutableStateFlow(1.0f)
    val simSpeedMultiplier: StateFlow<Float> = _simSpeedMultiplier.asStateFlow()

    private val _completedChallengeIds = MutableStateFlow<Set<String>>(emptySet())
    val completedChallengeIds: StateFlow<Set<String>> = _completedChallengeIds.asStateFlow()

    private val _activeChallenge = MutableStateFlow<LabChallenge?>(null)
    val activeChallenge: StateFlow<LabChallenge?> = _activeChallenge.asStateFlow()

    private val _lastChallengeResult = MutableStateFlow<ChallengeVerificationResult?>(null)
    val lastChallengeResult: StateFlow<ChallengeVerificationResult?> = _lastChallengeResult.asStateFlow()

    private val _cloudSyncBusy = MutableStateFlow(false)
    val cloudSyncBusy: StateFlow<Boolean> = _cloudSyncBusy.asStateFlow()

    private var digitalMemory = DigitalMemoryState()
    private val undoStack = ArrayDeque<CircuitPage>()
    private val redoStack = ArrayDeque<CircuitPage>()
    private var simJob: Job? = null
    private var aiJob: Job? = null

    val canUndo: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val canRedo: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {
        refreshLocalExports()
        viewModelScope.launch {
            projectRepo.ensureStarterProjectsSeeded()
            _recoverableDraft.value = projectRepo.getRecoverableDraft()
        }
    }

    fun navigateTab(tab: AppTab) {
        _currentTab.value = tab
        _subScreen.value = null
    }

    fun openSubScreen(screenId: String?) {
        _subScreen.value = screenId
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun recoverAutosaveDraft() {
        val draft = _recoverableDraft.value ?: return
        _activeProject.value = draft
        _recoverableDraft.value = null
        _currentTab.value = AppTab.DESIGN
        resetSimulationState()
        _statusBanner.value = "Recovered unsaved draft: ${draft.name}"
    }

    fun dismissAutosaveDraft() {
        viewModelScope.launch {
            projectRepo.clearAutosaveDraft()
            _recoverableDraft.value = null
        }
    }

    fun createNewProject(name: String = "New Circuit") {
        val now = System.currentTimeMillis()
        val page = CircuitPage("p1", "Sheet 1")
        val proj = CircuitProject(
            id = "proj_$now",
            name = name,
            description = "Custom schematic workspace",
            createdAt = now,
            updatedAt = now,
            pages = listOf(page),
            activePageId = page.id
        )
        loadProjectIntoEditor(proj)
    }

    fun openTemplate(template: CircuitTemplate) {
        loadProjectIntoEditor(template.toNewProject())
        _statusBanner.value = "Loaded template: ${template.title}"
    }

    fun openBooleanCircuit(expression: String): Boolean {
        val page = BooleanEngine.expressionToCircuitPage(expression, "Boolean: $expression") ?: return false
        val now = System.currentTimeMillis()
        val proj = CircuitProject(
            id = "proj_bool_$now",
            name = "Boolean ($expression)",
            description = "Synthesized from $expression",
            createdAt = now,
            updatedAt = now,
            pages = listOf(page),
            activePageId = page.id
        )
        loadProjectIntoEditor(proj)
        return true
    }

    fun loadSynthesizedPage(page: CircuitPage) {
        val now = System.currentTimeMillis()
        val proj = CircuitProject(
            id = "proj_lc_$now",
            name = page.name,
            description = "Synthesized by GateX Logic Converter",
            createdAt = now,
            updatedAt = now,
            pages = listOf(page),
            activePageId = page.id
        )
        loadProjectIntoEditor(proj)
        _statusBanner.value = "Built '${page.name}' — press Run to simulate."
    }

    fun applyWordGeneratorBits(lsbFirstBits: List<Boolean>) {
        val page = _activeProject.value.activePage()
        val logicInputs = page.components
            .filter { it.type == ComponentType.LOGIC_INPUT }
            .sortedBy { it.label }
        val dipSwitches = page.components
            .filter { it.type == ComponentType.DIP_SWITCH }
            .sortedBy { it.label }

        if (logicInputs.isEmpty() && dipSwitches.isEmpty()) {
            _statusBanner.value = "Add Logic Inputs or a DIP Switch to drive patterns."
            return
        }

        val updates = HashMap<String, ComponentInstance>()
        logicInputs.forEachIndexed { idx, comp ->
            val bitVal = if (lsbFirstBits.getOrElse(idx) { false }) "1" else "0"
            updates[comp.id] = comp.copy(value = bitVal, properties = comp.properties + ("state" to bitVal))
        }
        dipSwitches.forEachIndexed { dipIdx, comp ->
            val bitString = (0 until 4).map { b ->
                if (lsbFirstBits.getOrElse(dipIdx * 4 + b) { false }) '1' else '0'
            }.joinToString("")
            updates[comp.id] = comp.copy(value = bitString, properties = comp.properties + ("bits" to bitString))
        }

        mutateActivePageNoUndo { p ->
            p.copy(components = p.components.map { updates[it.id] ?: it })
        }
        LabAudioFeedback.playSwitchClick(settings.value.soundEnabled)
        if (_simRunState.value == SimulationRunState.STOPPED) {
            runSimulation()
        } else {
            stepSimulationOnce(toggleClocks = false)
        }
        val driven = logicInputs.size + dipSwitches.size * 4
        _statusBanner.value = "Word Generator sent pattern to $driven input bit(s)."
    }

    fun setWireColor(wireId: String, colorHex: Long?) {
        mutateActivePage { page ->
            page.copy(
                wires = page.wires.map { w ->
                    if (w.id == wireId) w.copy(colorHex = colorHex) else w
                }
            )
        }
        _statusBanner.value = if (colorHex == null) "Wire color reset to default." else "Wire color updated."
    }

    fun resetFuse(componentId: String) {
        mutateActivePage { page ->
            page.copy(
                components = page.components.map { comp ->
                    if (comp.id == componentId && comp.type == ComponentType.FUSE) {
                        comp.copy(properties = comp.properties + ("blown" to "false"))
                    } else comp
                }
            )
        }
        LabAudioFeedback.playSwitchClick(settings.value.soundEnabled)
        if (_simRunState.value != SimulationRunState.STOPPED) {
            stepSimulationOnce(toggleClocks = false)
        }
        _statusBanner.value = "Safety fuse reset."
    }

    fun openProject(project: CircuitProject) {
        loadProjectIntoEditor(project)
    }

    private fun loadProjectIntoEditor(project: CircuitProject) {
        stopSimulation()
        undoStack.clear()
        redoStack.clear()
        syncUndoFlags()
        _selectedComponentIds.value = emptySet()
        _pendingWireStart.value = null
        _activeProject.value = project
        _currentTab.value = AppTab.DESIGN
        _subScreen.value = null
    }

    fun saveCurrentProject() {
        viewModelScope.launch {
            projectRepo.saveProject(_activeProject.value)
            projectRepo.clearAutosaveDraft()
            _statusBanner.value = "Project '${_activeProject.value.name}' saved."
        }
    }

    fun renameProject(project: CircuitProject, newName: String) {
        viewModelScope.launch {
            val updated = projectRepo.renameProject(project, newName)
            if (_activeProject.value.id == project.id) {
                _activeProject.value = updated
            }
        }
    }

    fun duplicateProject(project: CircuitProject) {
        viewModelScope.launch {
            projectRepo.duplicateProject(project)
            _statusBanner.value = "Duplicated '${project.name}'."
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            projectRepo.deleteProject(projectId)
            _statusBanner.value = "Project deleted."
        }
    }

    // Multi-page management
    fun addPage() {
        val proj = _activeProject.value
        val newPage = CircuitPage(
            id = "p_${System.currentTimeMillis()}",
            name = "Sheet ${proj.pages.size + 1}"
        )
        _activeProject.value = proj.copy(
            pages = proj.pages + newPage,
            activePageId = newPage.id
        )
        resetSimulationState()
        triggerAutosave()
    }

    fun switchPage(pageId: String) {
        if (_activeProject.value.activePageId == pageId) return
        stopSimulation()
        undoStack.clear()
        redoStack.clear()
        syncUndoFlags()
        _selectedComponentIds.value = emptySet()
        _pendingWireStart.value = null
        _activeProject.value = _activeProject.value.copy(activePageId = pageId)
    }

    fun deletePage(pageId: String) {
        val proj = _activeProject.value
        if (proj.pages.size <= 1) return
        val remaining = proj.pages.filterNot { it.id == pageId }
        val nextActive = if (proj.activePageId == pageId) remaining.first().id else proj.activePageId
        _activeProject.value = proj.copy(pages = remaining, activePageId = nextActive)
        triggerAutosave()
    }

    fun setTool(newTool: WorkspaceTool) {
        _tool.value = newTool
        if (newTool != WorkspaceTool.WIRE) {
            _pendingWireStart.value = null
        }
    }

    fun addComponent(type: ComponentType, gridX: Float = 10f, gridY: Float = 8f) {
        val page = _activeProject.value.activePage()
        val meta = ComponentCatalog.get(type)
        val count = page.components.count { it.type == type } + 1
        val snappedX = snapCoord(gridX)
        val snappedY = snapCoord(gridY)
        val newComp = ComponentInstance(
            id = "c_${UUID.randomUUID().toString().take(8)}",
            type = type,
            label = "${meta.shortCode}$count",
            x = snappedX,
            y = snappedY,
            value = meta.defaultValue,
            properties = meta.defaultProperties
        )
        mutateActivePage { it.copy(components = it.components + newComp) }
        _selectedComponentIds.value = setOf(newComp.id)
        _pendingWireStart.value = null
        if (_tool.value != WorkspaceTool.SELECT && _tool.value != WorkspaceTool.MOVE) {
            _tool.value = WorkspaceTool.SELECT
        }
    }

    fun selectComponent(componentId: String?, multiSelect: Boolean = false) {
        if (componentId == null) {
            _selectedComponentIds.value = emptySet()
            return
        }
        if (multiSelect) {
            val curr = _selectedComponentIds.value
            _selectedComponentIds.value = if (componentId in curr) curr - componentId else curr + componentId
        } else {
            _selectedComponentIds.value = setOf(componentId)
        }
    }

    fun moveSelectedComponents(dxGrid: Float, dyGrid: Float, recordUndo: Boolean = false) {
        val selected = _selectedComponentIds.value
        if (selected.isEmpty()) return
        val transform: (CircuitPage) -> CircuitPage = { page ->
            page.copy(
                components = page.components.map { c ->
                    if (c.id in selected) {
                        c.copy(x = snapCoord(c.x + dxGrid), y = snapCoord(c.y + dyGrid))
                    } else c
                }
            )
        }
        if (recordUndo) mutateActivePage(transform) else mutateActivePageNoUndo(transform)
    }

    fun moveComponentTo(componentId: String, newX: Float, newY: Float) {
        mutateActivePageNoUndo { page ->
            val movedComps = page.components.map { c ->
                if (c.id == componentId) c.copy(x = snapCoord(newX), y = snapCoord(newY)) else c
            }
            val compsById = movedComps.associateBy { it.id }
            val juncsById = page.junctions.associateBy { it.id }
            val updatedWires = page.wires.map { wire ->
                if (wire.from.componentId == componentId || wire.to.componentId == componentId) {
                    val p1 = com.example.model.endpointWorldPosition(wire.from, compsById, juncsById, ComponentCatalog::get)
                    val p2 = com.example.model.endpointWorldPosition(wire.to, compsById, juncsById, ComponentCatalog::get)
                    if (p1 != null && p2 != null) {
                        wire.copy(waypoints = AutoWiringHelper.routeManhattan(p1, p2))
                    } else wire
                } else wire
            }
            page.copy(components = movedComps, wires = updatedWires)
        }
    }

    fun moveJunctionTo(junctionId: String, newX: Float, newY: Float) {
        mutateActivePageNoUndo { page ->
            val movedJuncs = page.junctions.map { j ->
                if (j.id == junctionId) j.copy(x = snapCoord(newX), y = snapCoord(newY)) else j
            }
            val compsById = page.components.associateBy { it.id }
            val juncsById = movedJuncs.associateBy { it.id }
            val updatedWires = page.wires.map { wire ->
                if (wire.from.junctionId == junctionId || wire.to.junctionId == junctionId) {
                    val p1 = com.example.model.endpointWorldPosition(wire.from, compsById, juncsById, ComponentCatalog::get)
                    val p2 = com.example.model.endpointWorldPosition(wire.to, compsById, juncsById, ComponentCatalog::get)
                    if (p1 != null && p2 != null) {
                        wire.copy(waypoints = AutoWiringHelper.routeManhattan(p1, p2))
                    } else wire
                } else wire
            }
            page.copy(junctions = movedJuncs, wires = updatedWires)
        }
    }

    fun commitUndoCheckpoint(previousPage: CircuitPage) {
        undoStack.addLast(previousPage)
        if (undoStack.size > 40) undoStack.removeFirst()
        redoStack.clear()
        syncUndoFlags()
        triggerAutosave()
    }

    fun rotateSelected() {
        val selected = _selectedComponentIds.value
        if (selected.isEmpty()) return
        mutateActivePage { page ->
            page.copy(
                components = page.components.map { c ->
                    if (c.id in selected) c.copy(rotationDegrees = (c.rotationDegrees + 90) % 360) else c
                }
            )
        }
    }

    fun flipSelected() {
        val selected = _selectedComponentIds.value
        if (selected.isEmpty()) return
        mutateActivePage { page ->
            page.copy(
                components = page.components.map { c ->
                    if (c.id in selected) c.copy(flipped = !c.flipped) else c
                }
            )
        }
    }

    fun deleteSelected() {
        val selected = _selectedComponentIds.value
        if (selected.isEmpty()) return
        mutateActivePage { page ->
            page.copy(
                components = page.components.filterNot { it.id in selected },
                wires = page.wires.filterNot {
                    it.from.componentId in selected || it.to.componentId in selected
                }
            )
        }
        _selectedComponentIds.value = emptySet()
    }

    fun duplicateSelected() {
        val selected = _selectedComponentIds.value
        if (selected.isEmpty()) return
        val page = _activeProject.value.activePage()
        val clones = page.components.filter { it.id in selected }.map { orig ->
            orig.copy(
                id = "c_${UUID.randomUUID().toString().take(8)}",
                label = "${orig.label}_cp",
                x = orig.x + 2f,
                y = orig.y + 2f
            )
        }
        mutateActivePage { it.copy(components = it.components + clones) }
        _selectedComponentIds.value = clones.map { it.id }.toSet()
    }

    fun copySelected() {
        val selected = _selectedComponentIds.value
        val page = _activeProject.value.activePage()
        _clipboard.value = page.components.filter { it.id in selected }
        _statusBanner.value = "Copied ${_clipboard.value.size} component(s)."
    }

    fun pasteClipboard() {
        val items = _clipboard.value
        if (items.isEmpty()) return
        val clones = items.map { orig ->
            orig.copy(
                id = "c_${UUID.randomUUID().toString().take(8)}",
                label = "${orig.label}_p",
                x = orig.x + 3f,
                y = orig.y + 3f
            )
        }
        mutateActivePage { it.copy(components = it.components + clones) }
        _selectedComponentIds.value = clones.map { it.id }.toSet()
    }

    fun updateComponent(updated: ComponentInstance) {
        mutateActivePage { page ->
            val old = page.components.firstOrNull { it.id == updated.id }
            val nextMeta = ComponentCatalog.metadataFor(updated)
            val validPinIds = nextMeta.pins.map { it.id }.toSet()
            val cleanedWires = if (old != null && ComponentCatalog.hasDynamicPins(updated)) {
                page.wires.filterNot { w ->
                    (w.from.componentId == updated.id && w.from.pinId != null && w.from.pinId !in validPinIds) ||
                        (w.to.componentId == updated.id && w.to.pinId != null && w.to.pinId !in validPinIds)
                }
            } else page.wires
            page.copy(
                components = page.components.map { if (it.id == updated.id) updated else it },
                wires = cleanedWires
            )
        }
        if (_simRunState.value == SimulationRunState.RUNNING) {
            stepSimulationOnce(toggleClocks = false)
        }
    }

    /** Changes the number of inputs (2..8) on a variable-input logic gate (AND/OR/NAND/NOR/XOR/XNOR). */
    fun setGateInputCount(componentId: String, inputCount: Int) {
        val clamped = inputCount.coerceIn(2, 8)
        val page = _activeProject.value.activePage()
        val comp = page.components.firstOrNull { it.id == componentId } ?: return
        if (comp.type !in ComponentCatalog.variableInputGateTypes) return
        val updated = comp.copy(
            value = "$clamped-Input",
            properties = comp.properties + ("inputCount" to clamped.toString())
        )
        val validPinIds = ComponentCatalog.metadataFor(updated).pins.map { it.id }.toSet()
        mutateActivePage { p ->
            val nextComps = p.components.map { if (it.id == componentId) updated else it }
            val compsById = nextComps.associateBy { it.id }
            val juncsById = p.junctions.associateBy { it.id }
            val nextPageTmp = p.copy(components = nextComps)
            val cleanedWires = p.wires
                .filterNot { w ->
                    (w.from.componentId == componentId && w.from.pinId != null && w.from.pinId !in validPinIds) ||
                        (w.to.componentId == componentId && w.to.pinId != null && w.to.pinId !in validPinIds)
                }
                .map { w ->
                    if (w.from.componentId == componentId || w.to.componentId == componentId) {
                        val p1 = com.example.model.endpointWorldPosition(w.from, compsById, juncsById, ComponentCatalog::get)
                        val p2 = com.example.model.endpointWorldPosition(w.to, compsById, juncsById, ComponentCatalog::get)
                        if (p1 != null && p2 != null) {
                            w.copy(waypoints = AutoWiringHelper.routeSmartOrthogonal(p1, p2, nextPageTmp, setOfNotNull(w.from.componentId, w.to.componentId)))
                        } else w
                    } else w
                }
            p.copy(components = nextComps, wires = cleanedWires)
        }
        if (_simRunState.value != SimulationRunState.STOPPED) {
            stepSimulationOnce(toggleClocks = false)
        }
        _statusBanner.value = "${comp.label} set to $clamped inputs."
    }

    /** Packages the active combinational schematic (up to 4 inputs x 4 outputs) into a reusable Custom IC. */
    fun packageActiveCircuitAsCustomIc(name: String, shortCode: String): Boolean {
        val cleanName = name.trim().ifEmpty { "My Custom IC" }
        val cleanCode = shortCode.trim().uppercase().take(6).ifEmpty { "MYIC" }
        val page = _activeProject.value.activePage()
        val tt = digitalEngine.generateTruthTable(page)
        if (tt.inputLabels.isEmpty() || tt.outputLabels.isEmpty() || tt.rows.isEmpty()) {
            _statusBanner.value = "Add at least 1 Logic Input and 1 Output Probe/Indicator before saving as Custom IC."
            return false
        }
        if (tt.inputLabels.size > 4 || tt.outputLabels.size > 4) {
            _statusBanner.value = "Custom IC supports up to 4 inputs and 4 outputs (current: ${tt.inputLabels.size} in, ${tt.outputLabels.size} out)."
            return false
        }
        val truthMap = LinkedHashMap<Int, Int>()
        val inCount = tt.inputLabels.size.coerceAtMost(4)
        tt.rows.forEach { row ->
            var inMask = 0
            row.inputs.take(inCount).forEachIndexed { idx, lvl ->
                if (lvl == com.example.model.LogicLevel.ONE) {
                    inMask = inMask or (1 shl (inCount - 1 - idx))
                }
            }
            var outMask = 0
            row.outputs.take(4).forEachIndexed { idx, lvl ->
                if (lvl == com.example.model.LogicLevel.ONE) {
                    outMask = outMask or (1 shl idx)
                }
            }
            truthMap[inMask] = outMask
        }
        val def = CustomIcDefinition(
            id = "cic_${UUID.randomUUID().toString().take(8)}",
            name = cleanName,
            shortCode = cleanCode,
            description = "Packaged from '${page.name}' (${tt.inputLabels.joinToString(",")} -> ${tt.outputLabels.joinToString(",")})",
            inputLabels = tt.inputLabels.take(4),
            outputLabels = tt.outputLabels.take(4),
            truthMap = truthMap
        )
        viewModelScope.launch {
            customIcStore.saveCustomIc(def)
        }
        _statusBanner.value = "Saved Custom IC '$cleanName' ($cleanCode) — ready to place on any schematic!"
        return true
    }

    fun placeCustomIc(def: CustomIcDefinition, gridX: Float = 12f, gridY: Float = 8f) {
        val page = _activeProject.value.activePage()
        val count = page.components.count { it.type == ComponentType.CUSTOM_IC } + 1
        val props = mapOf(
            "customIcId" to def.id,
            "shortCode" to def.shortCode,
            "inputLabels" to def.inputLabels.joinToString(","),
            "outputLabels" to def.outputLabels.joinToString(","),
            "truthMap" to CustomIcStore.truthMapToCompactString(def.truthMap)
        )
        val newComp = ComponentInstance(
            id = "c_${UUID.randomUUID().toString().take(8)}",
            type = ComponentType.CUSTOM_IC,
            label = "${def.shortCode}_$count",
            x = snapCoord(gridX),
            y = snapCoord(gridY),
            value = def.name,
            properties = props
        )
        mutateActivePage { it.copy(components = it.components + newComp) }
        _selectedComponentIds.value = setOf(newComp.id)
        _statusBanner.value = "Placed Custom IC '${def.name}' (${def.shortCode})."
    }

    fun deleteCustomIc(icId: String) {
        viewModelScope.launch {
            customIcStore.deleteCustomIc(icId)
            _statusBanner.value = "Removed Custom IC from local library."
        }
    }

    fun setSimSpeedMultiplier(multiplier: Float) {
        _simSpeedMultiplier.value = multiplier.coerceIn(0.25f, 5.0f)
        _statusBanner.value = "Simulation speed: ${_simSpeedMultiplier.value}×"
    }

    fun renameActiveProject(newName: String) {
        val clean = newName.trim().ifEmpty { return }
        val updated = _activeProject.value.copy(name = clean, updatedAt = System.currentTimeMillis())
        _activeProject.value = updated
        viewModelScope.launch {
            projectRepo.saveProject(updated)
        }
        _statusBanner.value = "Renamed project to '$clean'."
    }

    fun clearActivePage() {
        stopSimulation()
        mutateActivePage { page ->
            page.copy(components = emptyList(), wires = emptyList(), junctions = emptyList())
        }
        _selectedComponentIds.value = emptySet()
        _pendingWireStart.value = null
        _statusBanner.value = "Cleared active schematic sheet."
    }

    fun toggleDipBit(componentId: String, bitIndex: Int) {
        val page = _activeProject.value.activePage()
        val comp = page.components.firstOrNull { it.id == componentId && it.type == ComponentType.DIP_SWITCH } ?: return
        val chars = (comp.properties["bits"] ?: "0000").padEnd(4, '0').toCharArray()
        val idx = bitIndex.coerceIn(0, 3)
        chars[idx] = if (chars[idx] == '1') '0' else '1'
        val nextBits = String(chars)
        val updated = comp.copy(value = nextBits, properties = comp.properties + ("bits" to nextBits))
        mutateActivePageNoUndo { p ->
            p.copy(components = p.components.map { if (it.id == componentId) updated else it })
        }
        LabAudioFeedback.playSwitchClick(settings.value.soundEnabled)
        if (_simRunState.value != SimulationRunState.STOPPED) {
            stepSimulationOnce(toggleClocks = false)
        }
    }

    fun toggleInteractiveInput(componentId: String) {
        val page = _activeProject.value.activePage()
        val comp = page.components.firstOrNull { it.id == componentId } ?: return
        val updated = when (comp.type) {
            ComponentType.LOGIC_INPUT -> {
                val next = if (comp.properties["state"] == "1" || comp.value == "1") "0" else "1"
                comp.copy(value = next, properties = comp.properties + ("state" to next))
            }
            ComponentType.TOGGLE_SWITCH, ComponentType.DPST_SWITCH -> {
                val nextClosed = comp.properties["closed"] != "true"
                comp.copy(
                    value = if (nextClosed) "ON" else "OFF",
                    properties = comp.properties + ("closed" to nextClosed.toString())
                )
            }
            ComponentType.SPDT_SWITCH, ComponentType.DPDT_SWITCH -> {
                val nextPos = if (comp.properties["position"] == "NO") "NC" else "NO"
                comp.copy(
                    value = nextPos,
                    properties = comp.properties + ("position" to nextPos)
                )
            }
            ComponentType.PUSH_BUTTON -> {
                val nextPressed = comp.properties["pressed"] != "true"
                comp.copy(
                    value = if (nextPressed) "CLOSED" else "OPEN",
                    properties = comp.properties + ("pressed" to nextPressed.toString())
                )
            }
            ComponentType.PUSH_BUTTON_NC -> {
                val nextPressed = comp.properties["pressed"] != "true"
                comp.copy(
                    value = if (nextPressed) "OPEN" else "CLOSED",
                    properties = comp.properties + ("pressed" to nextPressed.toString())
                )
            }
            ComponentType.ROTARY_SWITCH -> {
                val curr = (comp.properties["position"]?.toIntOrNull() ?: 1).coerceIn(1, 4)
                val next = (curr % 4) + 1
                comp.copy(
                    value = "POS $next",
                    properties = comp.properties + ("position" to next.toString())
                )
            }
            ComponentType.KEYPAD -> {
                val curr = (comp.properties["hexDigit"] ?: "0").toIntOrNull(16) ?: 0
                val next = ((curr + 1) and 0xF).toString(16).uppercase()
                comp.copy(value = "0x$next", properties = comp.properties + ("hexDigit" to next))
            }
            ComponentType.DIP_SWITCH -> {
                val curr = (comp.properties["bits"] ?: "0000").toIntOrNull(2) ?: 0
                val next = ((curr + 1) and 0xF).toString(2).padStart(4, '0')
                comp.copy(value = next, properties = comp.properties + ("bits" to next))
            }
            else -> return
        }
        mutateActivePageNoUndo { p ->
            p.copy(components = p.components.map { if (it.id == componentId) updated else it })
        }
        LabAudioFeedback.playSwitchClick(settings.value.soundEnabled)
        if (_simRunState.value != SimulationRunState.STOPPED) {
            stepSimulationOnce(toggleClocks = false)
        }
    }

    fun handlePinOrJunctionTap(endpoint: WireEndpoint) {
        val start = _pendingWireStart.value
        if (start == null) {
            _pendingWireStart.value = endpoint
            return
        }
        if (start.key() == endpoint.key()) {
            _pendingWireStart.value = null
            return
        }
        connectWire(start, endpoint)
    }

    fun connectWire(from: WireEndpoint, to: WireEndpoint): Boolean {
        if (from.key() == to.key()) {
            _pendingWireStart.value = null
            return false
        }
        val page = _activeProject.value.activePage()
        val duplicate = page.wires.any { w ->
            (w.from.key() == from.key() && w.to.key() == to.key()) ||
                (w.from.key() == to.key() && w.to.key() == from.key())
        }
        if (duplicate) {
            _pendingWireStart.value = null
            return false
        }
        val compsById = page.components.associateBy { it.id }
        val juncsById = page.junctions.associateBy { it.id }
        val fromPos = com.example.model.endpointWorldPosition(from, compsById, juncsById, ComponentCatalog::get)
        val toPos = com.example.model.endpointWorldPosition(to, compsById, juncsById, ComponentCatalog::get)
        val waypoints = if (fromPos != null && toPos != null) {
            AutoWiringHelper.routeSmartOrthogonal(
                from = fromPos,
                to = toPos,
                page = page,
                ignoreCompIds = setOfNotNull(from.componentId, to.componentId)
            )
        } else emptyList()

        val wire = WireSegment(
            id = "w_${UUID.randomUUID().toString().take(8)}",
            from = from,
            to = to,
            waypoints = waypoints
        )
        mutateActivePage { it.copy(wires = it.wires + wire) }
        _pendingWireStart.value = null
        LabAudioFeedback.playSwitchClick(settings.value.soundEnabled)
        if (_simRunState.value != SimulationRunState.STOPPED) {
            stepSimulationOnce(toggleClocks = false)
        }
        return true
    }

    /** Creates a branch junction on an existing wire and connects an incoming wire if supplied. */
    fun branchAndConnectWire(from: WireEndpoint, targetWireId: String, gridX: Float, gridY: Float) {
        val page = _activeProject.value.activePage()
        val wire = page.wires.firstOrNull { it.id == targetWireId } ?: return
        val jId = "j_${UUID.randomUUID().toString().take(8)}"
        val jx = snapCoord(gridX)
        val jy = snapCoord(gridY)
        val junction = WireJunction(jId, jx, jy)
        val compsById = page.components.associateBy { it.id }
        val juncsById = (page.junctions + junction).associateBy { it.id }
        val jEp = WireEndpoint(junctionId = jId)

        fun routed(a: WireEndpoint, b: WireEndpoint): WireSegment {
            val p1 = com.example.model.endpointWorldPosition(a, compsById, juncsById, ComponentCatalog::get)
            val p2 = com.example.model.endpointWorldPosition(b, compsById, juncsById, ComponentCatalog::get)
            val wp = if (p1 != null && p2 != null) AutoWiringHelper.routeManhattan(p1, p2) else emptyList()
            return WireSegment("w_${UUID.randomUUID().toString().take(8)}", a, b, wp)
        }

        val w1 = routed(wire.from, jEp)
        val w2 = routed(jEp, wire.to)
        val w3 = routed(from, jEp)

        mutateActivePage {
            it.copy(
                junctions = it.junctions + junction,
                wires = it.wires.filterNot { w -> w.id == targetWireId } + w1 + w2 + w3
            )
        }
        _pendingWireStart.value = null
        if (_simRunState.value != SimulationRunState.STOPPED) {
            stepSimulationOnce(toggleClocks = false)
        }
    }

    /** Creates a branch junction on an existing wire so multi-way wiring works smoothly. */
    fun branchWireWithJunction(wireId: String, gridX: Float, gridY: Float) {
        val page = _activeProject.value.activePage()
        val wire = page.wires.firstOrNull { it.id == wireId } ?: return
        val jId = "j_${UUID.randomUUID().toString().take(8)}"
        val junction = WireJunction(jId, snapCoord(gridX), snapCoord(gridY))
        val w1 = WireSegment("w_${UUID.randomUUID().toString().take(8)}", wire.from, WireEndpoint(junctionId = jId))
        val w2 = WireSegment("w_${UUID.randomUUID().toString().take(8)}", WireEndpoint(junctionId = jId), wire.to)

        mutateActivePage {
            it.copy(
                junctions = it.junctions + junction,
                wires = it.wires.filterNot { w -> w.id == wireId } + w1 + w2
            )
        }
        _pendingWireStart.value = WireEndpoint(junctionId = jId)
    }

    fun deleteWire(wireId: String) {
        mutateActivePage { page ->
            page.copy(wires = page.wires.filterNot { it.id == wireId })
        }
        if (_simRunState.value != SimulationRunState.STOPPED) {
            stepSimulationOnce(toggleClocks = false)
        }
        _statusBanner.value = "Wire disconnected."
    }

    /** Disconnects the most recently connected wire on the active page. */
    fun undoLastWire(): Boolean {
        val page = _activeProject.value.activePage()
        val lastWire = page.wires.lastOrNull() ?: return false
        mutateActivePage { p ->
            p.copy(wires = p.wires.filterNot { it.id == lastWire.id })
        }
        if (_simRunState.value != SimulationRunState.STOPPED) {
            stepSimulationOnce(toggleClocks = false)
        }
        _statusBanner.value = "Undid last wire connection."
        return true
    }

    /** Disconnects all wires attached to a specific component pin or junction. */
    fun disconnectWiresAtPin(endpoint: WireEndpoint): Int {
        val page = _activeProject.value.activePage()
        val key = endpoint.key()
        val matching = page.wires.filter { it.from.key() == key || it.to.key() == key }
        if (matching.isEmpty()) return 0
        val removeIds = matching.map { it.id }.toSet()
        mutateActivePage { p ->
            p.copy(wires = p.wires.filterNot { it.id in removeIds })
        }
        if (_simRunState.value != SimulationRunState.STOPPED) {
            stepSimulationOnce(toggleClocks = false)
        }
        _statusBanner.value = if (matching.size == 1) "Disconnected 1 wire from pin." else "Disconnected ${matching.size} wires from pin."
        return matching.size
    }

    /** Disconnects all wires connected to a component while keeping the component on canvas. */
    fun disconnectComponentWires(componentId: String): Int {
        val page = _activeProject.value.activePage()
        val matching = page.wires.filter {
            it.from.componentId == componentId || it.to.componentId == componentId
        }
        if (matching.isEmpty()) return 0
        val removeIds = matching.map { it.id }.toSet()
        mutateActivePage { p ->
            p.copy(wires = p.wires.filterNot { it.id in removeIds })
        }
        if (_simRunState.value != SimulationRunState.STOPPED) {
            stepSimulationOnce(toggleClocks = false)
        }
        _statusBanner.value = "Disconnected ${matching.size} wire(s) from component."
        return matching.size
    }

    fun deleteJunction(junctionId: String) {
        mutateActivePage { page ->
            page.copy(
                junctions = page.junctions.filterNot { it.id == junctionId },
                wires = page.wires.filterNot { it.from.junctionId == junctionId || it.to.junctionId == junctionId }
            )
        }
        if (_simRunState.value != SimulationRunState.STOPPED) {
            stepSimulationOnce(toggleClocks = false)
        }
    }

    fun autoRouteAllWires() {
        mutateActivePage { page ->
            val compsById = page.components.associateBy { it.id }
            val juncsById = page.junctions.associateBy { it.id }
            val routed = page.wires.map { wire ->
                val p1 = com.example.model.endpointWorldPosition(wire.from, compsById, juncsById, ComponentCatalog::get)
                val p2 = com.example.model.endpointWorldPosition(wire.to, compsById, juncsById, ComponentCatalog::get)
                if (p1 != null && p2 != null) {
                    wire.copy(
                        waypoints = AutoWiringHelper.routeSmartOrthogonal(
                            from = p1,
                            to = p2,
                            page = page,
                            ignoreCompIds = setOfNotNull(wire.from.componentId, wire.to.componentId)
                        )
                    )
                } else wire
            }
            page.copy(wires = routed)
        }
        _statusBanner.value = "Smart obstacle-aware auto-routed all schematic wires."
    }

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return
        val curr = _activeProject.value.activePage()
        redoStack.addLast(curr)
        replaceActivePage(prev)
        syncUndoFlags()
        if (_simRunState.value != SimulationRunState.STOPPED) {
            stepSimulationOnce(toggleClocks = false)
        }
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        val curr = _activeProject.value.activePage()
        undoStack.addLast(curr)
        replaceActivePage(next)
        syncUndoFlags()
        if (_simRunState.value != SimulationRunState.STOPPED) {
            stepSimulationOnce(toggleClocks = false)
        }
    }

    // Simulation Lifecycle
    fun runSimulation() {
        if (_simRunState.value == SimulationRunState.RUNNING) return
        _simRunState.value = SimulationRunState.RUNNING
        stepSimulationOnce(toggleClocks = false)
        simJob?.cancel()
        simJob = viewModelScope.launch {
            while (_simRunState.value == SimulationRunState.RUNNING) {
                val baseMs = settings.value.simulationSpeedMs.coerceIn(150L, 2000L)
                val scaledMs = (baseMs / _simSpeedMultiplier.value).toLong().coerceIn(75L, 4000L)
                delay(scaledMs)
                stepSimulationOnce(toggleClocks = true)
            }
        }
    }

    fun pauseSimulation() {
        _simRunState.value = SimulationRunState.PAUSED
        simJob?.cancel()
    }

    fun stopSimulation() {
        _simRunState.value = SimulationRunState.STOPPED
        simJob?.cancel()
        resetSimulationState()
    }

    fun stepSimulationOnce(toggleClocks: Boolean = true) {
        var page = _activeProject.value.activePage()
        val (dRes, nextMem) = digitalEngine.step(page, digitalMemory, toggleClocks = toggleClocks)
        digitalMemory = nextMem
        _digitalResult.value = dRes

        var aRes = analogEngine.solveDcAndTransient(page)

        // Safety protection: trip any intact FUSE that exceeded its rated current
        val newlyBlownIds = aRes.diagnostics
            .filter { it.code == "FUSE_BLOWN" }
            .mapNotNull { it.componentId }
            .filter { id ->
                page.components.firstOrNull { it.id == id && it.type == ComponentType.FUSE }
                    ?.properties?.get("blown") != "true"
            }
            .toSet()

        if (newlyBlownIds.isNotEmpty()) {
            mutateActivePageNoUndo { p ->
                p.copy(
                    components = p.components.map { comp ->
                        if (comp.id in newlyBlownIds) {
                            comp.copy(properties = comp.properties + ("blown" to "true"))
                        } else comp
                    }
                )
            }
            page = _activeProject.value.activePage()
            aRes = analogEngine.solveDcAndTransient(page)
            LabAudioFeedback.playWarningTone(settings.value.soundEnabled)
            _statusBanner.value = "Safety Fuse blown due to overcurrent! Select fuse to Reset."
        }
        _analogResult.value = aRes

        // Live Piezo Buzzer audio feedback while simulation is running
        if (_simRunState.value == SimulationRunState.RUNNING && settings.value.soundEnabled) {
            val buzzerActive = page.components.any { comp ->
                if (comp.type != ComponentType.BUZZER) return@any false
                val digitalOn = dRes.componentStates[comp.id]?.get("state") == "1"
                val vp = aRes.pinVoltages[NetlistBuilder.pinKey(comp.id, "pos")] ?: 0.0
                val vn = aRes.pinVoltages[NetlistBuilder.pinKey(comp.id, "neg")] ?: 0.0
                digitalOn || abs(vp - vn) > 1.8
            }
            if (buzzerActive) {
                LabAudioFeedback.playBuzzerBeep(true)
            }
        }
    }

    fun runErcCheck() {
        _ercDiagnostics.value = CircuitConnectionChecker.check(_activeProject.value.activePage())
    }

    fun computeTruthTableForActivePage() {
        _truthTable.value = digitalEngine.generateTruthTable(_activeProject.value.activePage())
    }

    fun extractTruthTableForActivePage(): TruthTableResult =
        digitalEngine.generateTruthTable(_activeProject.value.activePage())

    // AI Assistant
    fun askAiAssistant(query: String) {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            aiRepo.streamEngineeringAdvice(query, _activeProject.value.activePage()).collect { state ->
                _aiState.value = state
            }
        }
    }

    fun generateCircuitWithAi(description: String) {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _aiState.value = AiAssistantState.Loading
            val res = aiRepo.generateStructuredCircuit(description)
            _aiState.value = res
            if (res is AiAssistantState.Success && res.generatedPage != null) {
                val now = System.currentTimeMillis()
                val proj = CircuitProject(
                    id = "proj_ai_$now",
                    name = res.generatedPage.name,
                    description = "AI-generated schematic (unverified)",
                    createdAt = now,
                    updatedAt = now,
                    pages = listOf(res.generatedPage),
                    activePageId = res.generatedPage.id
                )
                loadProjectIntoEditor(proj)
                _statusBanner.value = "AI circuit loaded — tap Run to simulate and verify."
            }
        }
    }

    // Breadboard & PCB Updates
    fun addBreadboardPlacement(placement: BreadboardPlacement) {
        val proj = _activeProject.value
        _activeProject.value = proj.copy(
            breadboard = proj.breadboard.copy(placements = proj.breadboard.placements + placement)
        )
        triggerAutosave()
    }

    fun addBreadboardJumper(jumper: BreadboardJumper) {
        val proj = _activeProject.value
        _activeProject.value = proj.copy(
            breadboard = proj.breadboard.copy(jumpers = proj.breadboard.jumpers + jumper)
        )
        triggerAutosave()
    }

    fun clearBreadboard() {
        val proj = _activeProject.value
        _activeProject.value = proj.copy(
            breadboard = com.example.model.BreadboardState()
        )
        triggerAutosave()
    }

    /** Maps active schematic parts (DIP ICs, resistors, LEDs, switches) onto the 830-point virtual breadboard. */
    fun importSchematicToBreadboard() {
        val page = _activeProject.value.activePage()
        val placements = mutableListOf<BreadboardPlacement>()
        val jumpers = mutableListOf<BreadboardJumper>()
        var nextCol = 3

        // Place DIP ICs & Logic Gates straddling center ravine (rows e & f)
        page.components.forEach { comp ->
            val meta = ComponentCatalog.metadataFor(comp)
            when {
                meta.category == com.example.model.ComponentCategory.IC_7400 ||
                    meta.category == com.example.model.ComponentCategory.LOGIC_GATES -> {
                    val span = (meta.pins.size / 2).coerceIn(4, 8)
                    if (nextCol + span <= 29) {
                        placements.add(
                            BreadboardPlacement(
                                id = "bp_${comp.id}",
                                componentType = comp.type,
                                label = comp.label,
                                value = meta.shortCode,
                                startHole = "e:$nextCol",
                                endHole = "f:${nextCol + span - 1}"
                            )
                        )
                        // Auto-power rail jumper wires for DIP IC
                        jumpers.add(BreadboardJumper("jb_vcc_${comp.id}", "top+:$nextCol", "a:$nextCol", 0xFFFF3B30))
                        jumpers.add(BreadboardJumper("jb_gnd_${comp.id}", "bot-:${nextCol + span - 1}", "j:${nextCol + span - 1}", 0xFF2563EB))
                        nextCol += span + 2
                    }
                }
                comp.type == ComponentType.RESISTOR || comp.type == ComponentType.LED ||
                    comp.type == ComponentType.PROBE_RED || comp.type == ComponentType.PROBE_GREEN ||
                    comp.type == ComponentType.LOGIC_INPUT || comp.type == ComponentType.TOGGLE_SWITCH -> {
                    if (nextCol + 3 <= 29) {
                        val row = if (comp.type == ComponentType.LOGIC_INPUT || comp.type == ComponentType.TOGGLE_SWITCH) "b" else "h"
                        placements.add(
                            BreadboardPlacement(
                                id = "bp_${comp.id}",
                                componentType = comp.type,
                                label = comp.label,
                                value = comp.value.ifBlank { meta.shortCode },
                                startHole = "$row:$nextCol",
                                endHole = "$row:${nextCol + 3}"
                            )
                        )
                        nextCol += 4
                    }
                }
            }
        }
        val proj = _activeProject.value
        _activeProject.value = proj.copy(
            breadboard = com.example.model.BreadboardState(placements = placements, jumpers = jumpers)
        )
        triggerAutosave()
        _statusBanner.value = "Imported ${placements.size} part(s) & ${jumpers.size} rail jumper(s) to 830-point breadboard."
    }

    // Lab Challenges & Exam Mode
    fun openChallengeStarter(challenge: LabChallenge) {
        _activeChallenge.value = challenge
        _lastChallengeResult.value = null
        val page = challenge.starterBuilder()
        val now = System.currentTimeMillis()
        val proj = CircuitProject(
            id = "challenge_${challenge.id}_$now",
            name = "Lab: ${challenge.title}",
            description = challenge.objective,
            createdAt = now,
            updatedAt = now,
            pages = listOf(page),
            activePageId = page.id
        )
        loadProjectIntoEditor(proj)
        _statusBanner.value = "Challenge loaded: ${challenge.title}. Build & tap Verify in Lab Challenges!"
    }

    fun verifyActiveChallenge(challenge: LabChallenge): ChallengeVerificationResult {
        _activeChallenge.value = challenge
        val result = LabChallengeEngine.verify(challenge, _activeProject.value.activePage())
        _lastChallengeResult.value = result
        if (result.passed) {
            _completedChallengeIds.value = _completedChallengeIds.value + challenge.id
            LabAudioFeedback.playSwitchClick(settings.value.soundEnabled)
        }
        _statusBanner.value = if (result.passed) {
            "PASS (${result.scorePercent}%) — ${challenge.title} verified!"
        } else {
            "Score ${result.scorePercent}% — ${result.summary}"
        }
        return result
    }

    // BOM, Waveform CSV/PNG & .gatex.json File Share
    fun exportBomCsvAndShare() {
        val ctx = getApplication<Application>()
        val file = ExportManager.exportBomCsv(ctx, _activeProject.value, _analogResult.value)
        ExportManager.shareFile(ctx, file, "text/csv", "Share BOM CSV — ${_activeProject.value.name}")
    }

    fun exportWaveformCsvAndShare(isAnalog: Boolean) {
        val ctx = getApplication<Application>()
        val file = if (isAnalog) {
            ExportManager.exportOscilloscopeCsv(ctx, _activeProject.value, _analogResult.value)
        } else {
            ExportManager.exportLogicAnalyzerCsv(ctx, _activeProject.value, _digitalResult.value)
        }
        ExportManager.shareFile(ctx, file, "text/csv", "Share Waveform CSV — ${_activeProject.value.name}")
    }

    fun exportWaveformPngAndShare(isAnalog: Boolean) {
        val ctx = getApplication<Application>()
        val file = ExportManager.exportWaveformPng(ctx, _activeProject.value, isAnalog, _analogResult.value, _digitalResult.value)
        ExportManager.shareFile(ctx, file, "image/png", "Share Waveform PNG — ${_activeProject.value.name}")
    }

    fun shareGatexProjectFile() {
        val ctx = getApplication<Application>()
        val file = ExportManager.exportGatexProjectFile(ctx, _activeProject.value)
        ExportManager.shareFile(ctx, file, "application/json", "Share ${_activeProject.value.name}.gatex.json")
    }

    // Cloud Sync (Firestore — strictly gated by signed-in user)
    fun uploadActiveProjectToCloud() {
        viewModelScope.launch {
            _cloudSyncBusy.value = true
            val res = cloudSyncRepo.uploadProject(_activeProject.value)
            _cloudSyncBusy.value = false
            _statusBanner.value = res.fold(
                onSuccess = { "Backed up '${_activeProject.value.name}' to GateX Cloud." },
                onFailure = { err -> "Cloud backup failed: ${err.localizedMessage ?: "Sign in required"}" }
            )
        }
    }

    fun uploadCustomIcToCloud(def: CustomIcDefinition) {
        viewModelScope.launch {
            _cloudSyncBusy.value = true
            val res = cloudSyncRepo.uploadCustomIc(def)
            _cloudSyncBusy.value = false
            _statusBanner.value = res.fold(
                onSuccess = { "Synced Custom IC '${def.name}' to Cloud." },
                onFailure = { err -> "Custom IC sync failed: ${err.localizedMessage ?: "Error"}" }
            )
        }
    }

    fun restoreCloudProject(project: CircuitProject) {
        viewModelScope.launch {
            val restored = project.copy(updatedAt = System.currentTimeMillis())
            projectRepo.saveProject(restored)
            loadProjectIntoEditor(restored)
            _statusBanner.value = "Restored '${restored.name}' from GateX Cloud."
        }
    }

    fun deleteCloudProject(projectId: String) {
        viewModelScope.launch {
            val res = cloudSyncRepo.deleteCloudProject(projectId)
            _statusBanner.value = res.fold(
                onSuccess = { "Deleted project from GateX Cloud." },
                onFailure = { err -> "Cloud delete failed: ${err.localizedMessage ?: "Error"}" }
            )
        }
    }

    fun syncCloudCustomIcsToLocal(cloudDefs: List<CustomIcDefinition>) {
        viewModelScope.launch {
            customIcStore.mergeCloudCustomIcs(cloudDefs)
            _statusBanner.value = "Synced ${cloudDefs.size} Custom IC(s) to local library."
        }
    }

    fun importSchematicToPcbFootprints() {
        val page = _activeProject.value.activePage()
        val footprints = page.components.mapIndexed { idx, comp ->
            val meta = ComponentCatalog.get(comp.type)
            val col = idx % 3
            val row = idx / 3
            val cx = 14f + col * 24f
            val cy = 14f + row * 18f
            val pads = meta.pins.mapIndexed { pIdx, pin ->
                PcbPad(
                    id = "${comp.id}_${pin.id}",
                    name = pin.label,
                    relXMm = if (pIdx % 2 == 0) -4f else 4f,
                    relYMm = ((pIdx / 2) - 0.5f) * 3.5f,
                    netName = "NET_${comp.label}_${pin.label}"
                )
            }
            PcbFootprint(
                id = "fp_${comp.id}",
                schematicRef = comp.label,
                footprintName = if (meta.pins.size > 4) "DIP-${meta.pins.size}" else "TH-${meta.pins.size}",
                xMm = cx,
                yMm = cy,
                pads = pads
            )
        }
        val proj = _activeProject.value
        _activeProject.value = proj.copy(pcb = PcbBoardState(footprints = footprints))
        triggerAutosave()
        _statusBanner.value = "Generated ${footprints.size} PCB footprints separately from schematic."
    }

    fun addPcbTrace(trace: PcbTrace) {
        val proj = _activeProject.value
        _activeProject.value = proj.copy(pcb = proj.pcb.copy(traces = proj.pcb.traces + trace))
        triggerAutosave()
    }

    fun addPcbVia(via: PcbVia) {
        val proj = _activeProject.value
        _activeProject.value = proj.copy(pcb = proj.pcb.copy(vias = proj.pcb.vias + via))
        triggerAutosave()
    }

    fun updatePcbBoardSize(widthMm: Float, heightMm: Float) {
        val proj = _activeProject.value
        _activeProject.value = proj.copy(pcb = proj.pcb.copy(boardWidthMm = widthMm, boardHeightMm = heightMm))
        triggerAutosave()
    }

    // Export & Import (Project Export Module)
    fun refreshLocalExports() {
        _localExports.value = ExportManager.listLocalExports(getApplication())
    }

    fun buildStructuredJsonReportPreview(includeSimTelemetry: Boolean = true): String =
        ExportManager.buildStructuredJsonReport(
            project = _activeProject.value,
            digitalResult = _digitalResult.value,
            analogResult = _analogResult.value,
            includeSimulationTelemetry = includeSimTelemetry
        )

    fun savePngToDeviceUri(uri: Uri, options: PngExportOptions = PngExportOptions()) {
        val ctx = getApplication<Application>()
        val result = ExportManager.writePngToUri(
            context = ctx,
            uri = uri,
            project = _activeProject.value,
            digitalResult = _digitalResult.value,
            analogResult = _analogResult.value,
            options = options
        )
        _statusBanner.value = result.fold(
            onSuccess = { bytes -> "Saved PNG schematic (${bytes / 1024} KB) to device storage." },
            onFailure = { err -> "Failed to save PNG: ${err.localizedMessage ?: "I/O error"}" }
        )
    }

    fun saveJsonReportToDeviceUri(uri: Uri, includeSimTelemetry: Boolean = true) {
        val ctx = getApplication<Application>()
        val result = ExportManager.writeStructuredJsonReportToUri(
            context = ctx,
            uri = uri,
            project = _activeProject.value,
            digitalResult = _digitalResult.value,
            analogResult = _analogResult.value,
            includeSimulationTelemetry = includeSimTelemetry
        )
        _statusBanner.value = result.fold(
            onSuccess = { bytes -> "Saved JSON project report (${(bytes / 1024.0).coerceAtLeast(0.1).let { String.format(java.util.Locale.US, "%.1f", it) }} KB) to device storage." },
            onFailure = { err -> "Failed to save JSON report: ${err.localizedMessage ?: "I/O error"}" }
        )
    }

    fun savePdfReportToDeviceUri(uri: Uri, reportMeta: PdfReportMetadata = PdfReportMetadata()) {
        val ctx = getApplication<Application>()
        val result = ExportManager.writePdfReportToUri(
            context = ctx,
            uri = uri,
            project = _activeProject.value,
            digitalResult = _digitalResult.value,
            analogResult = _analogResult.value,
            reportMeta = reportMeta
        )
        _statusBanner.value = result.fold(
            onSuccess = { bytes -> "Saved PDF engineering report (${bytes / 1024} KB) to device storage." },
            onFailure = { err -> "Failed to save PDF report: ${err.localizedMessage ?: "I/O error"}" }
        )
    }

    fun savePngLocally(options: PngExportOptions = PngExportOptions()) {
        val ctx = getApplication<Application>()
        val artifact = ExportManager.savePngToLocalArchive(
            context = ctx,
            project = _activeProject.value,
            digitalResult = _digitalResult.value,
            analogResult = _analogResult.value,
            options = options
        )
        refreshLocalExports()
        _statusBanner.value = "Saved ${artifact.name} (${artifact.sizeBytes / 1024} KB) to app exports."
    }

    fun saveJsonReportLocally(includeSimTelemetry: Boolean = true) {
        val ctx = getApplication<Application>()
        val artifact = ExportManager.saveJsonReportToLocalArchive(
            context = ctx,
            project = _activeProject.value,
            digitalResult = _digitalResult.value,
            analogResult = _analogResult.value,
            includeSimulationTelemetry = includeSimTelemetry
        )
        refreshLocalExports()
        _statusBanner.value = "Saved ${artifact.name} to app exports."
    }

    fun shareLocalExport(artifact: ExportedArtifact) {
        val ctx = getApplication<Application>()
        ExportManager.shareFile(ctx, artifact.file, artifact.mimeType, "Share ${artifact.name}")
    }

    fun deleteLocalExport(artifact: ExportedArtifact) {
        ExportManager.deleteLocalExport(artifact.file)
        refreshLocalExports()
        _statusBanner.value = "Deleted ${artifact.name}."
    }

    fun exportPngAndShare(options: PngExportOptions = PngExportOptions()) {
        val ctx = getApplication<Application>()
        val file = ExportManager.exportPng(
            context = ctx,
            project = _activeProject.value,
            digitalResult = _digitalResult.value,
            analogResult = _analogResult.value,
            options = options
        )
        ExportManager.shareFile(ctx, file, "image/png", "Share Circuit PNG — ${_activeProject.value.name}")
    }

    fun exportPdfAndShare(reportMeta: PdfReportMetadata = PdfReportMetadata()) {
        val ctx = getApplication<Application>()
        val file = ExportManager.exportPdfReport(
            ctx,
            _activeProject.value,
            _digitalResult.value,
            _analogResult.value,
            reportMeta
        )
        ExportManager.shareFile(ctx, file, "application/pdf", "Share Engineering Report — ${_activeProject.value.name}")
    }

    fun exportJsonAndShare(includeSimTelemetry: Boolean = true) {
        val ctx = getApplication<Application>()
        val file = ExportManager.exportProjectJson(
            context = ctx,
            project = _activeProject.value,
            digitalResult = _digitalResult.value,
            analogResult = _analogResult.value,
            includeSimulationTelemetry = includeSimTelemetry
        )
        ExportManager.shareFile(ctx, file, "application/json", "Share GateX Labs Report — ${_activeProject.value.name}")
    }

    fun importProjectUri(uri: Uri) {
        val ctx = getApplication<Application>()
        val imported = ExportManager.importProjectFromUri(ctx, uri)
        if (imported != null) {
            viewModelScope.launch {
                val unique = imported.copy(
                    id = "proj_imp_${System.currentTimeMillis()}",
                    updatedAt = System.currentTimeMillis()
                )
                projectRepo.saveProject(unique)
                loadProjectIntoEditor(unique)
                _statusBanner.value = "Imported '${unique.name}'."
            }
        } else {
            _statusBanner.value = "Could not parse selected project file."
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepo.update(transform) }
    }

    fun selectTheme(preset: AppThemePreset) {
        viewModelScope.launch {
            settingsRepo.update {
                it.copy(themeId = preset.id, isDarkTheme = preset.isDark)
            }
        }
        _statusBanner.value = "Theme: ${preset.displayName}"
    }

    private fun snapCoord(v: Float): Float {
        return if (settings.value.snapToGrid) {
            (v * 2f).roundToInt() / 2f
        } else v
    }

    private fun mutateActivePage(transform: (CircuitPage) -> CircuitPage) {
        val curr = _activeProject.value.activePage()
        undoStack.addLast(curr)
        if (undoStack.size > 40) undoStack.removeFirst()
        redoStack.clear()
        syncUndoFlags()
        replaceActivePage(transform(curr))
        triggerAutosave()
    }

    private fun mutateActivePageNoUndo(transform: (CircuitPage) -> CircuitPage) {
        val curr = _activeProject.value.activePage()
        replaceActivePage(transform(curr))
        triggerAutosave()
    }

    private fun replaceActivePage(updatedPage: CircuitPage) {
        val proj = _activeProject.value
        _activeProject.value = proj.copy(
            updatedAt = System.currentTimeMillis(),
            pages = proj.pages.map { if (it.id == updatedPage.id) updatedPage else it }
        )
    }

    private fun syncUndoFlags() {
        canUndo.value = undoStack.isNotEmpty()
        canRedo.value = redoStack.isNotEmpty()
    }

    private fun resetSimulationState() {
        digitalMemory = DigitalMemoryState()
        _digitalResult.value = DigitalSimulationResult()
        _analogResult.value = AnalogSimulationResult()
    }

    private fun triggerAutosave() {
        if (!settings.value.autosaveEnabled) return
        val snapshot = _activeProject.value
        viewModelScope.launch {
            projectRepo.saveAutosaveDraft(snapshot)
        }
    }

    override fun onCleared() {
        super.onCleared()
        simJob?.cancel()
        aiJob?.cancel()
    }
}

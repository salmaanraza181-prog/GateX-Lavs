package com.example.model

import kotlin.math.roundToInt

enum class LogicLevel(val symbol: String) {
    ZERO("0"),
    ONE("1"),
    UNKNOWN("X"),
    HIGH_Z("Z"),
    CONFLICT("ERR");

    fun invert(): LogicLevel = when (this) {
        ZERO -> ONE
        ONE -> ZERO
        UNKNOWN -> UNKNOWN
        HIGH_Z -> UNKNOWN
        CONFLICT -> CONFLICT
    }

    fun isHigh(): Boolean = this == ONE
    fun isLow(): Boolean = this == ZERO
    fun isKnown(): Boolean = this == ZERO || this == ONE
}

enum class PinDirection {
    INPUT,
    OUTPUT,
    BIDIRECTIONAL,
    POWER,
    GROUND
}

data class PinSpec(
    val id: String,
    val name: String,
    val label: String,
    val direction: PinDirection,
    /** Offset in grid units relative to component center */
    val relX: Float,
    val relY: Float,
    val pinNumber: Int? = null,
    val activeLow: Boolean = false,
    val isClock: Boolean = false
)

enum class ComponentCategory(val title: String) {
    BASIC("Basic Components"),
    POWER("Power Sources"),
    LOGIC_GATES("Digital Logic Gates"),
    IC_7400("7400 Series & ICs"),
    INPUT("Input Controls"),
    OUTPUT("Output & Displays"),
    INSTRUMENTS("Virtual Instruments")
}

enum class SimulationSupportLevel(val label: String) {
    FULL_DIGITAL("Digital Engine"),
    FULL_ANALOG("Analog MNA Engine"),
    MIXED_SIGNAL("Digital + Analog"),
    INSTRUMENT("Virtual Instrument")
}

enum class ComponentType {
    // A. Basic Components
    RESISTOR,
    VARIABLE_RESISTOR,
    POTENTIOMETER,
    CAPACITOR,
    POLARIZED_CAPACITOR,
    INDUCTOR,
    DIODE,
    ZENER_DIODE,
    LED,
    RGB_LED,
    PHOTODIODE,
    TRANSISTOR_NPN,
    TRANSISTOR_PNP,
    MOSFET,
    RELAY,
    TRANSFORMER,
    FUSE,
    OP_AMP_741,
    NET_LABEL,
    TEXT_NOTE,

    // B. Power Components
    DC_SOURCE,
    AC_SOURCE,
    BATTERY,
    GROUND,
    VCC,
    CURRENT_SOURCE,
    POWER_SUPPLY,
    VOLTAGE_REGULATOR,

    // C. Digital Logic Gates
    GATE_AND,
    GATE_OR,
    GATE_NOT,
    GATE_NAND,
    GATE_NOR,
    GATE_XOR,
    GATE_XNOR,
    GATE_BUFFER,
    GATE_TRISTATE,

    // D. 7400 Series 14-Pin DIP IC Library
    IC_7400,
    IC_7402,
    IC_7404,
    IC_7408,
    IC_7410,
    IC_7411,
    IC_7420,
    IC_7421,
    IC_7427,
    IC_7430,
    IC_7432,
    IC_7472,
    IC_7473,
    IC_7474,
    IC_7476,
    IC_7486,
    IC_7490,
    IC_7493,
    IC_74125,
    IC_74266,
    IC_74138,
    IC_74151,
    IC_7447,
    IC_7483,
    IC_74194,
    IC_7485,
    IC_74595,
    IC_74161,
    IC_74154,
    IC_555,
    CUSTOM_IC,

    // E. Input & Switch Components
    TOGGLE_SWITCH,
    SPDT_SWITCH,
    DPST_SWITCH,
    DPDT_SWITCH,
    PUSH_BUTTON,
    PUSH_BUTTON_NC,
    DIP_SWITCH,
    ROTARY_SWITCH,
    VOLTAGE_CONTROLLED_SWITCH,
    RELAY_SPDT,
    CLOCK_INPUT,
    LOGIC_INPUT,
    KEYPAD,
    VARIABLE_INPUT,

    // F. Output & Indicator Components
    PROBE_RED,
    PROBE_GREEN,
    PROBE_YELLOW,
    PROBE_BLUE,
    PROBE_ORANGE,
    PROBE_CYAN,
    PILOT_LAMP,
    BAR_GRAPH,
    DECODED_BAR_GRAPH,
    LOGIC_INDICATOR,
    LED_INDICATOR,
    SEVEN_SEGMENT,
    HEX_DISPLAY,
    DIGITAL_DISPLAY,
    LCD_16X2,
    DOT_MATRIX_8X8,
    TRAFFIC_LIGHT,
    BUZZER,
    MOTOR,
    RELAY_OUTPUT,

    // G. Instruments
    MULTIMETER,
    VOLTMETER,
    AMMETER,
    OHMMETER,
    OSCILLOSCOPE,
    FUNCTION_GENERATOR,
    LOGIC_ANALYZER,
    FREQUENCY_COUNTER
}

data class ComponentMetadata(
    val type: ComponentType,
    val displayName: String,
    val shortCode: String,
    val category: ComponentCategory,
    val widthGrid: Float,
    val heightGrid: Float,
    val pins: List<PinSpec>,
    val defaultValue: String,
    val defaultProperties: Map<String, String> = emptyMap(),
    val supportLevel: SimulationSupportLevel,
    val description: String,
    val datasheetSummary: String,
    val pinoutTable: List<Pair<String, String>> = emptyList()
)

data class ComponentInstance(
    val id: String,
    val type: ComponentType,
    val label: String,
    val x: Float,
    val y: Float,
    val rotationDegrees: Int = 0,
    val flipped: Boolean = false,
    val value: String = "",
    val properties: Map<String, String> = emptyMap()
)

data class WireEndpoint(
    val componentId: String? = null,
    val pinId: String? = null,
    val junctionId: String? = null
) {
    val isComponentPin: Boolean get() = componentId != null && pinId != null
    val isJunction: Boolean get() = junctionId != null
    fun key(): String = when {
        componentId != null && pinId != null -> "pin:$componentId:$pinId"
        junctionId != null -> "junc:$junctionId"
        else -> "invalid"
    }
}

data class WireJunction(
    val id: String,
    val x: Float,
    val y: Float
)

data class GridPoint(
    val x: Float,
    val y: Float
)

data class WireSegment(
    val id: String,
    val from: WireEndpoint,
    val to: WireEndpoint,
    val waypoints: List<GridPoint> = emptyList(),
    val colorHex: Long? = null
)

data class CircuitPage(
    val id: String,
    val name: String,
    val components: List<ComponentInstance> = emptyList(),
    val wires: List<WireSegment> = emptyList(),
    val junctions: List<WireJunction> = emptyList()
)

data class CustomIcDefinition(
    val id: String,
    val name: String,
    val shortCode: String,
    val description: String = "",
    val inputLabels: List<String> = listOf("A", "B"),
    val outputLabels: List<String> = listOf("Y"),
    val truthMap: Map<Int, Int> = emptyMap()
)

// Breadboard Module Models (kept separate from schematic)
data class BreadboardPlacement(
    val id: String,
    val componentType: ComponentType,
    val label: String,
    val value: String,
    val startHole: String,
    val endHole: String,
    val properties: Map<String, String> = emptyMap()
)

data class BreadboardJumper(
    val id: String,
    val fromHole: String,
    val toHole: String,
    val colorHex: Long
)

data class BreadboardState(
    val placements: List<BreadboardPlacement> = emptyList(),
    val jumpers: List<BreadboardJumper> = emptyList()
)

// PCB Layout Module Models (strictly separate from schematic data)
enum class PcbLayer { TOP_COPPER, BOTTOM_COPPER }

data class PcbPad(
    val id: String,
    val name: String,
    val relXMm: Float,
    val relYMm: Float,
    val netName: String
)

data class PcbFootprint(
    val id: String,
    val schematicRef: String,
    val footprintName: String,
    val xMm: Float,
    val yMm: Float,
    val rotationDegrees: Int = 0,
    val pads: List<PcbPad>
)

data class PcbTrace(
    val id: String,
    val netName: String,
    val layer: PcbLayer,
    val widthMm: Float,
    val points: List<Pair<Float, Float>>
)

data class PcbVia(
    val id: String,
    val netName: String,
    val xMm: Float,
    val yMm: Float,
    val drillMm: Float = 0.4f
)

data class PcbBoardState(
    val boardWidthMm: Float = 80f,
    val boardHeightMm: Float = 60f,
    val footprints: List<PcbFootprint> = emptyList(),
    val traces: List<PcbTrace> = emptyList(),
    val vias: List<PcbVia> = emptyList()
)

data class CircuitProject(
    val id: String,
    val name: String,
    val description: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val pages: List<CircuitPage>,
    val activePageId: String,
    val breadboard: BreadboardState = BreadboardState(),
    val pcb: PcbBoardState = PcbBoardState()
) {
    fun activePage(): CircuitPage = pages.firstOrNull { it.id == activePageId } ?: pages.first()
}

enum class DiagnosticSeverity { ERROR, WARNING, INFO }

data class SimulationDiagnostic(
    val severity: DiagnosticSeverity,
    val code: String,
    val message: String,
    val componentId: String? = null
)

data class DigitalWaveformSample(
    val tick: Int,
    val nodeLevels: Map<String, LogicLevel>
)

data class DigitalSimulationResult(
    val pinLevels: Map<String, LogicLevel> = emptyMap(),
    val wireLevels: Map<String, LogicLevel> = emptyMap(),
    val componentStates: Map<String, Map<String, String>> = emptyMap(),
    val diagnostics: List<SimulationDiagnostic> = emptyList(),
    val waveformHistory: List<DigitalWaveformSample> = emptyList(),
    val tickCount: Int = 0,
    val hasShortCircuit: Boolean = false
)

data class AnalogWaveformPoint(
    val timeSec: Double,
    val channelValues: Map<String, Double>
)

data class AnalogSimulationResult(
    val nodeVoltages: Map<String, Double> = emptyMap(),
    val pinVoltages: Map<String, Double> = emptyMap(),
    val componentCurrents: Map<String, Double> = emptyMap(),
    val componentPowers: Map<String, Double> = emptyMap(),
    val equivalentResistanceOhms: Double? = null,
    val transientSamples: List<AnalogWaveformPoint> = emptyList(),
    val acSweepSummary: String? = null,
    val diagnostics: List<SimulationDiagnostic> = emptyList(),
    val converged: Boolean = true
)

fun pinWorldPosition(component: ComponentInstance, pin: PinSpec): GridPoint {
    val rx = if (component.flipped) -pin.relX else pin.relX
    val ry = pin.relY
    val rot = ((component.rotationDegrees % 360) + 360) % 360
    val (tx, ty) = when (rot) {
        90 -> -ry to rx
        180 -> -rx to -ry
        270 -> ry to -rx
        else -> rx to ry
    }
    return GridPoint(
        x = ((component.x + tx) * 2f).roundToInt() / 2f,
        y = ((component.y + ty) * 2f).roundToInt() / 2f
    )
}

fun endpointWorldPosition(
    endpoint: WireEndpoint,
    componentsById: Map<String, ComponentInstance>,
    junctionsById: Map<String, WireJunction>,
    metadataProvider: (ComponentType) -> ComponentMetadata
): GridPoint? {
    if (endpoint.junctionId != null) {
        val j = junctionsById[endpoint.junctionId] ?: return null
        return GridPoint(j.x, j.y)
    }
    if (endpoint.componentId != null && endpoint.pinId != null) {
        val comp = componentsById[endpoint.componentId] ?: return null
        val meta = if (ComponentCatalog.hasDynamicPins(comp)) {
            ComponentCatalog.metadataFor(comp)
        } else {
            metadataProvider(comp.type)
        }
        val pin = meta.pins.firstOrNull { it.id == endpoint.pinId } ?: return null
        return pinWorldPosition(comp, pin)
    }
    return null
}

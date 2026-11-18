package com.example.model

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

object ComponentCatalog {
    private val metadataMap: Map<ComponentType, ComponentMetadata> by lazy {
        (basicAndPowerMetadata() + digitalAndIcMetadata() + ioAndInstrumentMetadata())
            .associateBy { it.type }
    }

    val allComponents: List<ComponentMetadata>
        get() = ComponentType.entries.map { get(it) }

    val variableInputGateTypes: Set<ComponentType> = setOf(
        ComponentType.GATE_AND,
        ComponentType.GATE_OR,
        ComponentType.GATE_NAND,
        ComponentType.GATE_NOR,
        ComponentType.GATE_XOR,
        ComponentType.GATE_XNOR
    )

    fun hasDynamicPins(comp: ComponentInstance): Boolean =
        comp.type == ComponentType.CUSTOM_IC ||
            (comp.type in variableInputGateTypes && (comp.properties["inputCount"]?.toIntOrNull() ?: 2) != 2)

    fun get(type: ComponentType): ComponentMetadata =
        metadataMap[type] ?: error("Missing metadata for $type")

    fun metadataFor(comp: ComponentInstance): ComponentMetadata {
        val base = get(comp.type)
        if (comp.type in variableInputGateTypes) {
            val n = (comp.properties["inputCount"]?.toIntOrNull() ?: 2).coerceIn(2, 8)
            if (n == 2) return base
            val inputs = (0 until n).map { idx ->
                val id = ('a' + idx).toString()
                val lbl = ('A' + idx).toString()
                val relY = idx - (n - 1) / 2f
                PinSpec(id, lbl, lbl, PinDirection.INPUT, -2f, relY)
            }
            val output = PinSpec("y", "Y", "Y", PinDirection.OUTPUT, 2f, 0f)
            val newH = maxOf(3f, (n - 1) * 1f + 1.5f)
            return base.copy(
                heightGrid = newH,
                pins = inputs + output,
                defaultValue = "$n-Input"
            )
        }
        if (comp.type == ComponentType.CUSTOM_IC) {
            val inLabels = comp.properties["inputLabels"]
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.take(4)
                .orEmpty()
                .ifEmpty { listOf("A", "B") }
            val outLabels = comp.properties["outputLabels"]
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.take(4)
                .orEmpty()
                .ifEmpty { listOf("Y") }
            val maxSide = maxOf(inLabels.size, outLabels.size).coerceAtLeast(2)
            val newH = maxOf(3.5f, (maxSide - 1) * 1.0f + 2.0f)
            val leftPins = inLabels.mapIndexed { idx, lbl ->
                val ry = idx - (inLabels.size - 1) / 2f
                PinSpec("in$idx", lbl, lbl, PinDirection.INPUT, -2.5f, ry, pinNumber = idx + 1)
            }
            val rightPins = outLabels.mapIndexed { idx, lbl ->
                val ry = idx - (outLabels.size - 1) / 2f
                PinSpec("out$idx", lbl, lbl, PinDirection.OUTPUT, 2.5f, ry, pinNumber = inLabels.size + outLabels.size - idx)
            }
            val code = comp.properties["shortCode"]?.ifBlank { null } ?: comp.value.ifBlank { "MY-IC" }
            return base.copy(
                displayName = comp.properties["icName"]?.ifBlank { null } ?: base.displayName,
                shortCode = code,
                heightGrid = newH,
                pins = leftPins + rightPins
            )
        }
        return base
    }

    fun byCategory(category: ComponentCategory): List<ComponentMetadata> =
        allComponents.filter { it.category == category }

    fun search(query: String): List<ComponentMetadata> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return allComponents
        return allComponents.filter {
            it.displayName.lowercase().contains(q) ||
                it.shortCode.lowercase().contains(q) ||
                it.category.title.lowercase().contains(q) ||
                it.description.lowercase().contains(q)
        }
    }

    fun parseEngineeringValue(raw: String, defaultVal: Double): Double {
        val clean = raw.trim()
            .replace("Ω", "")
            .replace("ohm", "", ignoreCase = true)
            .replace("V", "", ignoreCase = true)
            .replace("A", "", ignoreCase = true)
            .replace("F", "", ignoreCase = true)
            .replace("Hz", "", ignoreCase = true)
            .replace("H", "", ignoreCase = true)
            .replace(" ", "")
        if (clean.isEmpty()) return defaultVal
        val match = Regex("^([+-]?\\d*\\.?\\d+(?:[eE][+-]?\\d+)?)([a-zA-Zµμ]*)$").find(clean)
            ?: return defaultVal
        val number = match.groupValues[1].toDoubleOrNull() ?: return defaultVal
        val suffix = match.groupValues[2]
        val multiplier = when (suffix) {
            "p" -> 1e-12
            "n" -> 1e-9
            "u", "µ", "μ" -> 1e-6
            "m" -> 1e-3
            "k", "K" -> 1e3
            "M", "Meg", "MEG" -> 1e6
            "G" -> 1e9
            "" -> 1.0
            else -> 1.0
        }
        return number * multiplier
    }

    fun formatEngineeringValue(value: Double, unit: String): String {
        if (!value.isFinite()) return "---"
        if (abs(value) < 1e-15) return "0 $unit"
        val absVal = abs(value)
        val exp = (log10(absVal) / 3.0).let {
            if (it < 0) kotlin.math.floor(it).toInt() else kotlin.math.floor(it).toInt()
        }.coerceIn(-4, 3)
        val scaled = value / 10.0.pow(exp * 3)
        val prefix = when (exp) {
            -4 -> "p"
            -3 -> "n"
            -2 -> "µ"
            -1 -> "m"
            0 -> ""
            1 -> "k"
            2 -> "M"
            3 -> "G"
            else -> ""
        }
        val formatted = if (abs(scaled) >= 100) String.format("%.1f", scaled)
        else if (abs(scaled) >= 10) String.format("%.2f", scaled)
        else String.format("%.3f", scaled)
        return "$formatted $prefix$unit".trim()
    }
}

internal fun basicAndPowerMetadata(): List<ComponentMetadata> = listOf(
    ComponentMetadata(
        type = ComponentType.RESISTOR,
        displayName = "Resistor",
        shortCode = "R",
        category = ComponentCategory.BASIC,
        widthGrid = 4f,
        heightGrid = 2f,
        pins = listOf(
            PinSpec("p1", "1", "1", PinDirection.BIDIRECTIONAL, -2f, 0f),
            PinSpec("p2", "2", "2", PinDirection.BIDIRECTIONAL, 2f, 0f)
        ),
        defaultValue = "1k",
        defaultProperties = mapOf("tolerance" to "5%", "powerRatingW" to "0.25"),
        supportLevel = SimulationSupportLevel.FULL_ANALOG,
        description = "Linear passive resistor obeying Ohm's Law (V = I * R).",
        datasheetSummary = "Two-terminal bilateral resistive element. Supports DC, AC, and transient MNA simulation."
    ),
    ComponentMetadata(
        type = ComponentType.VARIABLE_RESISTOR,
        displayName = "Variable Resistor",
        shortCode = "VR",
        category = ComponentCategory.BASIC,
        widthGrid = 4f,
        heightGrid = 2f,
        pins = listOf(
            PinSpec("p1", "1", "1", PinDirection.BIDIRECTIONAL, -2f, 0f),
            PinSpec("p2", "2", "2", PinDirection.BIDIRECTIONAL, 2f, 0f)
        ),
        defaultValue = "10k",
        defaultProperties = mapOf("positionPct" to "50"),
        supportLevel = SimulationSupportLevel.FULL_ANALOG,
        description = "Rheostat whose effective resistance scales with wiper percentage (1%–100%).",
        datasheetSummary = "Adjustable two-terminal resistance: R_eff = R_total * (positionPct / 100)."
    ),
    ComponentMetadata(
        type = ComponentType.POTENTIOMETER,
        displayName = "Potentiometer",
        shortCode = "POT",
        category = ComponentCategory.BASIC,
        widthGrid = 4f,
        heightGrid = 3f,
        pins = listOf(
            PinSpec("p1", "1", "1", PinDirection.BIDIRECTIONAL, -2f, 0f),
            PinSpec("p2", "2", "2", PinDirection.BIDIRECTIONAL, 2f, 0f),
            PinSpec("wiper", "W", "W", PinDirection.BIDIRECTIONAL, 0f, -1.5f)
        ),
        defaultValue = "10k",
        defaultProperties = mapOf("wiperPct" to "50"),
        supportLevel = SimulationSupportLevel.FULL_ANALOG,
        description = "Three-terminal resistive voltage divider with adjustable wiper.",
        datasheetSummary = "Pin 1 to W = R * (1 - wiperPct), W to Pin 2 = R * wiperPct."
    ),
    ComponentMetadata(
        type = ComponentType.CAPACITOR,
        displayName = "Capacitor",
        shortCode = "C",
        category = ComponentCategory.BASIC,
        widthGrid = 4f,
        heightGrid = 2f,
        pins = listOf(
            PinSpec("p1", "1", "1", PinDirection.BIDIRECTIONAL, -2f, 0f),
            PinSpec("p2", "2", "2", PinDirection.BIDIRECTIONAL, 2f, 0f)
        ),
        defaultValue = "100u",
        defaultProperties = mapOf("initialV" to "0"),
        supportLevel = SimulationSupportLevel.FULL_ANALOG,
        description = "Non-polarized capacitor storing energy in an electric field (I = C dV/dt).",
        datasheetSummary = "Open-circuit at DC steady state; modeled with Backward Euler companion conductance in transient analysis."
    ),
    ComponentMetadata(
        type = ComponentType.POLARIZED_CAPACITOR,
        displayName = "Electrolytic Cap",
        shortCode = "CP",
        category = ComponentCategory.BASIC,
        widthGrid = 4f,
        heightGrid = 2f,
        pins = listOf(
            PinSpec("pos", "+", "+", PinDirection.BIDIRECTIONAL, -2f, 0f),
            PinSpec("neg", "-", "-", PinDirection.BIDIRECTIONAL, 2f, 0f)
        ),
        defaultValue = "470u",
        defaultProperties = mapOf("maxVoltage" to "25V", "initialV" to "0"),
        supportLevel = SimulationSupportLevel.FULL_ANALOG,
        description = "Polarized electrolytic capacitor. Reverse polarity >0.5V triggers warning.",
        datasheetSummary = "Observe + and - polarity. Reverse-bias exceeding 0.5V is flagged by simulation diagnostics."
    ),
    ComponentMetadata(
        type = ComponentType.INDUCTOR,
        displayName = "Inductor",
        shortCode = "L",
        category = ComponentCategory.BASIC,
        widthGrid = 4f,
        heightGrid = 2f,
        pins = listOf(
            PinSpec("p1", "1", "1", PinDirection.BIDIRECTIONAL, -2f, 0f),
            PinSpec("p2", "2", "2", PinDirection.BIDIRECTIONAL, 2f, 0f)
        ),
        defaultValue = "10m",
        defaultProperties = mapOf("dcrOhms" to "0.5"),
        supportLevel = SimulationSupportLevel.FULL_ANALOG,
        description = "Magnetic coil opposing current changes (V = L dI/dt).",
        datasheetSummary = "Low-resistance short at DC; Backward Euler companion model in RL/RLC transient analysis."
    ),
    ComponentMetadata(
        type = ComponentType.DIODE,
        displayName = "PN Diode",
        shortCode = "D",
        category = ComponentCategory.BASIC,
        widthGrid = 4f,
        heightGrid = 2f,
        pins = listOf(
            PinSpec("anode", "A", "A", PinDirection.BIDIRECTIONAL, -2f, 0f),
            PinSpec("cathode", "K", "K", PinDirection.BIDIRECTIONAL, 2f, 0f)
        ),
        defaultValue = "1N4148",
        defaultProperties = mapOf("vf" to "0.7"),
        supportLevel = SimulationSupportLevel.FULL_ANALOG,
        description = "Silicon PN junction rectifier conducting forward current above ~0.7V.",
        datasheetSummary = "Piecewise-linear forward model (Vf = 0.7V, Ron = 10Ω, Roff = 10MΩ)."
    ),
    ComponentMetadata(
        type = ComponentType.ZENER_DIODE,
        displayName = "Zener Diode",
        shortCode = "ZD",
        category = ComponentCategory.BASIC,
        widthGrid = 4f,
        heightGrid = 2f,
        pins = listOf(
            PinSpec("anode", "A", "A", PinDirection.BIDIRECTIONAL, -2f, 0f),
            PinSpec("cathode", "K", "K", PinDirection.BIDIRECTIONAL, 2f, 0f)
        ),
        defaultValue = "5.1V",
        defaultProperties = mapOf("vf" to "0.7", "vz" to "5.1"),
        supportLevel = SimulationSupportLevel.FULL_ANALOG,
        description = "Voltage reference diode clamping reverse breakdown at Vz.",
        datasheetSummary = "Conducts forward at 0.7V and regulates reverse voltage at Vz (default 5.1V)."
    ),
    ComponentMetadata(
        type = ComponentType.LED,
        displayName = "LED",
        shortCode = "LED",
        category = ComponentCategory.BASIC,
        widthGrid = 4f,
        heightGrid = 2f,
        pins = listOf(
            PinSpec("anode", "A", "A", PinDirection.BIDIRECTIONAL, -2f, 0f),
            PinSpec("cathode", "K", "K", PinDirection.BIDIRECTIONAL, 2f, 0f)
        ),
        defaultValue = "Red 2.0V",
        defaultProperties = mapOf("color" to "Red", "vf" to "2.0", "maxCurrentMa" to "25"),
        supportLevel = SimulationSupportLevel.MIXED_SIGNAL,
        description = "Light-emitting diode glowing proportionally to forward current (2–25 mA).",
        datasheetSummary = "Forward voltage ~2.0V (Red/Amber) or ~3.0V (Blue/White). Flags over-current >30mA."
    ),
    ComponentMetadata(
        type = ComponentType.RGB_LED,
        displayName = "RGB LED",
        shortCode = "RGB",
        category = ComponentCategory.BASIC,
        widthGrid = 4f,
        heightGrid = 4f,
        pins = listOf(
            PinSpec("r", "R", "R", PinDirection.INPUT, -2f, -1f),
            PinSpec("g", "G", "G", PinDirection.INPUT, -2f, 0f),
            PinSpec("b", "B", "B", PinDirection.INPUT, -2f, 1f),
            PinSpec("com", "COM", "K", PinDirection.GROUND, 2f, 0f)
        ),
        defaultValue = "Common Cathode",
        defaultProperties = mapOf("vf" to "2.1"),
        supportLevel = SimulationSupportLevel.MIXED_SIGNAL,
        description = "Common-cathode RGB indicator blending Red, Green, and Blue channels.",
        datasheetSummary = "Apply logic HIGH or >2.1V to R, G, B with COM connected to GND."
    ),
    ComponentMetadata(
        type = ComponentType.PHOTODIODE,
        displayName = "Photodiode",
        shortCode = "PD",
        category = ComponentCategory.BASIC,
        widthGrid = 4f,
        heightGrid = 2f,
        pins = listOf(
            PinSpec("anode", "A", "A", PinDirection.BIDIRECTIONAL, -2f, 0f),
            PinSpec("cathode", "K", "K", PinDirection.BIDIRECTIONAL, 2f, 0f)
        ),
        defaultValue = "500 lux",
        defaultProperties = mapOf("lux" to "500"),
        supportLevel = SimulationSupportLevel.FULL_ANALOG,
        description = "Light-sensitive diode generating photocurrent proportional to illuminance.",
        datasheetSummary = "Photocurrent modeled as I_photo = lux * 0.2µA from cathode to anode."
    ),
    ComponentMetadata(
        type = ComponentType.TRANSISTOR_NPN,
        displayName = "NPN Transistor",
        shortCode = "Q_NPN",
        category = ComponentCategory.BASIC,
        widthGrid = 4f,
        heightGrid = 4f,
        pins = listOf(
            PinSpec("base", "B", "B", PinDirection.INPUT, -2f, 0f),
            PinSpec("collector", "C", "C", PinDirection.BIDIRECTIONAL, 1f, -2f),
            PinSpec("emitter", "E", "E", PinDirection.BIDIRECTIONAL, 1f, 2f)
        ),
        defaultValue = "2N2222 (β=100)",
        defaultProperties = mapOf("beta" to "100", "vbeOn" to "0.7"),
        supportLevel = SimulationSupportLevel.FULL_ANALOG,
        description = "NPN bipolar junction transistor supporting cutoff, active, and saturation switching.",
        datasheetSummary = "Conducts C->E when V_BE >= 0.7V with current gain β (hFE = 100)."
    ),
    ComponentMetadata(
        type = ComponentType.TRANSISTOR_PNP,
        displayName = "PNP Transistor",
        shortCode = "Q_PNP",
        category = ComponentCategory.BASIC,
        widthGrid = 4f,
        heightGrid = 4f,
        pins = listOf(
            PinSpec("base", "B", "B", PinDirection.INPUT, -2f, 0f),
            PinSpec("emitter", "E", "E", PinDirection.BIDIRECTIONAL, 1f, -2f),
            PinSpec("collector", "C", "C", PinDirection.BIDIRECTIONAL, 1f, 2f)
        ),
        defaultValue = "2N3906 (β=100)",
        defaultProperties = mapOf("beta" to "100", "vebOn" to "0.7"),
        supportLevel = SimulationSupportLevel.FULL_ANALOG,
        description = "PNP bipolar junction transistor conducting E->C when base is >=0.7V below emitter.",
        datasheetSummary = "High-side switching BJT. Conducts when V_EB >= 0.7V."
    ),
    ComponentMetadata(
        type = ComponentType.MOSFET,
        displayName = "N-MOSFET",
        shortCode = "M_NMOS",
        category = ComponentCategory.BASIC,
        widthGrid = 4f,
        heightGrid = 4f,
        pins = listOf(
            PinSpec("gate", "G", "G", PinDirection.INPUT, -2f, 0f),
            PinSpec("drain", "D", "D", PinDirection.BIDIRECTIONAL, 1f, -2f),
            PinSpec("source", "S", "S", PinDirection.BIDIRECTIONAL, 1f, 2f)
        ),
        defaultValue = "IRF540 (Vth=2V)",
        defaultProperties = mapOf("vth" to "2.0", "rdsOn" to "0.1"),
        supportLevel = SimulationSupportLevel.FULL_ANALOG,
        description = "Enhancement-mode N-Channel MOSFET voltage-controlled switch.",
        datasheetSummary = "High-impedance gate (>10MΩ). Turns ON (Rds_on = 0.1Ω) when V_GS >= Vth."
    ),
    ComponentMetadata(
        type = ComponentType.RELAY,
        displayName = "SPST Relay",
        shortCode = "RLY",
        category = ComponentCategory.BASIC,
        widthGrid = 4f,
        heightGrid = 4f,
        pins = listOf(
            PinSpec("coil1", "C1", "C+", PinDirection.INPUT, -2f, -1f),
            PinSpec("coil2", "C2", "C-", PinDirection.INPUT, -2f, 1f),
            PinSpec("com", "COM", "COM", PinDirection.BIDIRECTIONAL, 2f, -1f),
            PinSpec("no", "NO", "NO", PinDirection.BIDIRECTIONAL, 2f, 1f)
        ),
        defaultValue = "5V Coil",
        defaultProperties = mapOf("coilVoltage" to "5.0", "coilResistance" to "100"),
        supportLevel = SimulationSupportLevel.MIXED_SIGNAL,
        description = "Electromechanical relay closing COM-NO contacts when coil voltage exceeds threshold.",
        datasheetSummary = "Coil resistance 100Ω; closes NO contact when |V_coil| >= 3.5V."
    ),
    ComponentMetadata(
        type = ComponentType.TRANSFORMER,
        displayName = "Transformer",
        shortCode = "XFMR",
        category = ComponentCategory.BASIC,
        widthGrid = 4f,
        heightGrid = 4f,
        pins = listOf(
            PinSpec("p1", "P1", "P1", PinDirection.BIDIRECTIONAL, -2f, -1f),
            PinSpec("p2", "P2", "P2", PinDirection.BIDIRECTIONAL, -2f, 1f),
            PinSpec("s1", "S1", "S1", PinDirection.BIDIRECTIONAL, 2f, -1f),
            PinSpec("s2", "S2", "S2", PinDirection.BIDIRECTIONAL, 2f, 1f)
        ),
        defaultValue = "10:1",
        defaultProperties = mapOf("turnsRatio" to "0.1"),
        supportLevel = SimulationSupportLevel.FULL_ANALOG,
        description = "Ideal step-down/step-up AC transformer isolating primary and secondary windings.",
        datasheetSummary = "AC coupled: V_sec = V_pri * turnsRatio. DC steady state is isolated."
    ),
    ComponentMetadata(
        type = ComponentType.FUSE,
        displayName = "Safety Fuse",
        shortCode = "FUSE",
        category = ComponentCategory.BASIC,
        widthGrid = 4f,
        heightGrid = 2f,
        pins = listOf(
            PinSpec("p1", "1", "1", PinDirection.BIDIRECTIONAL, -2f, 0f),
            PinSpec("p2", "2", "2", PinDirection.BIDIRECTIONAL, 2f, 0f)
        ),
        defaultValue = "250mA",
        defaultProperties = mapOf("ratingMa" to "250", "blown" to "false"),
        supportLevel = SimulationSupportLevel.FULL_ANALOG,
        description = "Resettable glass cartridge fuse that blows open when current exceeds its mA rating.",
        datasheetSummary = "Intact: ~0.05Ω short. Blows open if |I| > ratingMa; tap Reset Fuse to restore.",
        pinoutTable = listOf("1: Line In" to "2: Load Out")
    ),
    ComponentMetadata(
        type = ComponentType.OP_AMP_741,
        displayName = "LM741 Op-Amp",
        shortCode = "741",
        category = ComponentCategory.BASIC,
        widthGrid = 5f,
        heightGrid = 4f,
        pins = listOf(
            PinSpec("in_pos", "+", "+", PinDirection.INPUT, -2.5f, -1f, pinNumber = 3),
            PinSpec("in_neg", "-", "-", PinDirection.INPUT, -2.5f, 1f, pinNumber = 2),
            PinSpec("out", "OUT", "OUT", PinDirection.OUTPUT, 2.5f, 0f, pinNumber = 6),
            PinSpec("vcc", "V+", "V+", PinDirection.POWER, 0f, -2f, pinNumber = 7),
            PinSpec("vee", "V-", "V-", PinDirection.POWER, 0f, 2f, pinNumber = 4)
        ),
        defaultValue = "LM741",
        defaultProperties = mapOf("gain" to "1000", "vPosRail" to "12.0", "vNegRail" to "-12.0"),
        supportLevel = SimulationSupportLevel.FULL_ANALOG,
        description = "General-purpose operational amplifier for inverting/non-inverting amplifiers, followers & comparators.",
        datasheetSummary = "Vout = clamp(Aol·(V+ - V-), V-, V+). Default ±12V rails; supports negative feedback loops.",
        pinoutTable = listOf(
            "2: Inverting (-)" to "6: Output (OUT)",
            "3: Non-Inverting (+)" to "7: Positive Rail (V+)",
            "4: Negative Rail (V-)" to "Aol ≈ 1000 V/V"
        )
    ),
    ComponentMetadata(
        type = ComponentType.NET_LABEL,
        displayName = "Net Label Tag",
        shortCode = "TAG",
        category = ComponentCategory.BASIC,
        widthGrid = 4f,
        heightGrid = 2f,
        pins = listOf(
            PinSpec("pin", "NET", "NET", PinDirection.BIDIRECTIONAL, -2f, 0f)
        ),
        defaultValue = "CLK",
        defaultProperties = mapOf("netName" to "CLK"),
        supportLevel = SimulationSupportLevel.MIXED_SIGNAL,
        description = "Wireless net connector tag — all Net Labels with the same name (e.g. CLK, A, BUS0) are electrically connected.",
        datasheetSummary = "Edit the value to rename the net (case-insensitive). Works for both Digital & Analog nets.",
        pinoutTable = listOf("NET: Wireless Bus Node" to "Matches any identical tag name")
    ),
    ComponentMetadata(
        type = ComponentType.TEXT_NOTE,
        displayName = "Canvas Note / Title",
        shortCode = "NOTE",
        category = ComponentCategory.BASIC,
        widthGrid = 6f,
        heightGrid = 2f,
        pins = emptyList(),
        defaultValue = "Lab 1: GateX Circuit",
        defaultProperties = mapOf("noteText" to "Lab 1: GateX Circuit"),
        supportLevel = SimulationSupportLevel.MIXED_SIGNAL,
        description = "Editable schematic annotation or assignment title block placed directly on the canvas.",
        datasheetSummary = "Non-electrical visual note rendered on schematic and exported in PNG/PDF lab reports."
    ),

    // B. Power Components
    ComponentMetadata(
        type = ComponentType.DC_SOURCE,
        displayName = "DC Voltage Source",
        shortCode = "VDC",
        category = ComponentCategory.POWER,
        widthGrid = 4f,
        heightGrid = 3f,
        pins = listOf(
            PinSpec("pos", "+", "+", PinDirection.POWER, 0f, -1.5f),
            PinSpec("neg", "-", "-", PinDirection.GROUND, 0f, 1.5f)
        ),
        defaultValue = "5V",
        defaultProperties = mapOf("voltage" to "5.0"),
        supportLevel = SimulationSupportLevel.FULL_ANALOG,
        description = "Ideal independent DC voltage supply.",
        datasheetSummary = "Maintains fixed DC potential V(+) - V(-) = V_dc."
    ),
    ComponentMetadata(
        type = ComponentType.AC_SOURCE,
        displayName = "AC Voltage Source",
        shortCode = "VAC",
        category = ComponentCategory.POWER,
        widthGrid = 4f,
        heightGrid = 3f,
        pins = listOf(
            PinSpec("pos", "+", "~+", PinDirection.POWER, 0f, -1.5f),
            PinSpec("neg", "-", "~-", PinDirection.GROUND, 0f, 1.5f)
        ),
        defaultValue = "5V 50Hz",
        defaultProperties = mapOf("amplitude" to "5.0", "frequency" to "50.0"),
        supportLevel = SimulationSupportLevel.FULL_ANALOG,
        description = "Sinusoidal alternating voltage source for AC & rectifier circuits.",
        datasheetSummary = "v(t) = A * sin(2π * f * t)."
    ),
    ComponentMetadata(
        type = ComponentType.BATTERY,
        displayName = "Battery Cell",
        shortCode = "BAT",
        category = ComponentCategory.POWER,
        widthGrid = 4f,
        heightGrid = 2f,
        pins = listOf(
            PinSpec("pos", "+", "+", PinDirection.POWER, -2f, 0f),
            PinSpec("neg", "-", "-", PinDirection.GROUND, 2f, 0f)
        ),
        defaultValue = "9V",
        defaultProperties = mapOf("voltage" to "9.0", "internalRes" to "0.2"),
        supportLevel = SimulationSupportLevel.FULL_ANALOG,
        description = "DC battery cell with realistic internal resistance.",
        datasheetSummary = "Default 9V cell with 0.2Ω ESR."
    ),
    ComponentMetadata(
        type = ComponentType.GROUND,
        displayName = "Ground (GND)",
        shortCode = "GND",
        category = ComponentCategory.POWER,
        widthGrid = 2f,
        heightGrid = 2f,
        pins = listOf(
            PinSpec("gnd", "GND", "0V", PinDirection.GROUND, 0f, -1f)
        ),
        defaultValue = "0V",
        supportLevel = SimulationSupportLevel.MIXED_SIGNAL,
        description = "0V reference node required for analog MNA simulation and digital ground.",
        datasheetSummary = "Forces attached net to V = 0.0V (analog) and Logic 0 (digital ground)."
    ),
    ComponentMetadata(
        type = ComponentType.VCC,
        displayName = "VCC Rail (+5V)",
        shortCode = "VCC",
        category = ComponentCategory.POWER,
        widthGrid = 2f,
        heightGrid = 2f,
        pins = listOf(
            PinSpec("vcc", "VCC", "+5V", PinDirection.POWER, 0f, 1f)
        ),
        defaultValue = "5V",
        defaultProperties = mapOf("voltage" to "5.0"),
        supportLevel = SimulationSupportLevel.MIXED_SIGNAL,
        description = "Positive DC logic supply rail (+5V / Logic 1).",
        datasheetSummary = "Drives attached net to +5V relative to GND (and Logic 1 in digital nets)."
    ),
    ComponentMetadata(
        type = ComponentType.CURRENT_SOURCE,
        displayName = "DC Current Source",
        shortCode = "IDC",
        category = ComponentCategory.POWER,
        widthGrid = 4f,
        heightGrid = 3f,
        pins = listOf(
            PinSpec("out", "+", "I+", PinDirection.POWER, 0f, -1.5f),
            PinSpec("in", "-", "I-", PinDirection.GROUND, 0f, 1.5f)
        ),
        defaultValue = "10mA",
        defaultProperties = mapOf("current" to "0.01"),
        supportLevel = SimulationSupportLevel.FULL_ANALOG,
        description = "Ideal constant current source injecting fixed current from - to +.",
        datasheetSummary = "Direct MNA current stamp."
    ),
    ComponentMetadata(
        type = ComponentType.POWER_SUPPLY,
        displayName = "Bench PSU",
        shortCode = "PSU",
        category = ComponentCategory.POWER,
        widthGrid = 4f,
        heightGrid = 3f,
        pins = listOf(
            PinSpec("pos", "+", "+", PinDirection.POWER, 2f, -1f),
            PinSpec("neg", "-", "-", PinDirection.GROUND, 2f, 1f)
        ),
        defaultValue = "12V",
        defaultProperties = mapOf("voltage" to "12.0", "currentLimitA" to "2.0"),
        supportLevel = SimulationSupportLevel.FULL_ANALOG,
        description = "Regulated lab bench DC power supply with current limit protection.",
        datasheetSummary = "Selectable DC output voltage with over-current diagnostic alert."
    ),
    ComponentMetadata(
        type = ComponentType.VOLTAGE_REGULATOR,
        displayName = "7805 Regulator",
        shortCode = "7805",
        category = ComponentCategory.POWER,
        widthGrid = 4f,
        heightGrid = 3f,
        pins = listOf(
            PinSpec("in", "IN", "IN", PinDirection.INPUT, -2f, 0f),
            PinSpec("gnd", "GND", "GND", PinDirection.GROUND, 0f, 1.5f),
            PinSpec("out", "OUT", "OUT", PinDirection.OUTPUT, 2f, 0f)
        ),
        defaultValue = "5V",
        defaultProperties = mapOf("vout" to "5.0", "dropout" to "2.0"),
        supportLevel = SimulationSupportLevel.FULL_ANALOG,
        description = "Linear positive voltage regulator (LM78xx series).",
        datasheetSummary = "Outputs regulated Vout when Vin >= Vout + 2.0V dropout."
    )
)

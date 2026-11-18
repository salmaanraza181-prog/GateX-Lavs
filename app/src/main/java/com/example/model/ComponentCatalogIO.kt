package com.example.model

internal fun ioAndInstrumentMetadata(): List<ComponentMetadata> = listOf(
    // E. Input & Switch Components
    ComponentMetadata(
        type = ComponentType.TOGGLE_SWITCH,
        displayName = "SPST Toggle Switch",
        shortCode = "SPST",
        category = ComponentCategory.INPUT,
        widthGrid = 4f,
        heightGrid = 2f,
        pins = listOf(
            PinSpec("p1", "1", "1", PinDirection.BIDIRECTIONAL, -2f, 0f),
            PinSpec("p2", "2", "2", PinDirection.BIDIRECTIONAL, 2f, 0f)
        ),
        defaultValue = "OFF",
        defaultProperties = mapOf("closed" to "false"),
        supportLevel = SimulationSupportLevel.MIXED_SIGNAL,
        description = "Interactive single-pole single-throw (SPST) blade switch.",
        datasheetSummary = "Tap during simulation to toggle Open (OFF) / Closed (ON)."
    ),
    ComponentMetadata(
        type = ComponentType.SPDT_SWITCH,
        displayName = "SPDT Changeover",
        shortCode = "SPDT",
        category = ComponentCategory.INPUT,
        widthGrid = 4f,
        heightGrid = 3f,
        pins = listOf(
            PinSpec("com", "COM", "C", PinDirection.BIDIRECTIONAL, -2f, 0f),
            PinSpec("no", "NO", "NO", PinDirection.BIDIRECTIONAL, 2f, -1f),
            PinSpec("nc", "NC", "NC", PinDirection.BIDIRECTIONAL, 2f, 1f)
        ),
        defaultValue = "NC",
        defaultProperties = mapOf("position" to "NC"),
        supportLevel = SimulationSupportLevel.MIXED_SIGNAL,
        description = "Single-pole double-throw (SPDT) switch routing COM to NC or NO.",
        datasheetSummary = "Tap on canvas to flip common wiper between NC (normally closed) and NO."
    ),
    ComponentMetadata(
        type = ComponentType.DPST_SWITCH,
        displayName = "DPST Dual Switch",
        shortCode = "DPST",
        category = ComponentCategory.INPUT,
        widthGrid = 4f,
        heightGrid = 4f,
        pins = listOf(
            PinSpec("a1", "A1", "A1", PinDirection.BIDIRECTIONAL, -2f, -1f),
            PinSpec("a2", "A2", "A2", PinDirection.BIDIRECTIONAL, 2f, -1f),
            PinSpec("b1", "B1", "B1", PinDirection.BIDIRECTIONAL, -2f, 1f),
            PinSpec("b2", "B2", "B2", PinDirection.BIDIRECTIONAL, 2f, 1f)
        ),
        defaultValue = "OFF",
        defaultProperties = mapOf("closed" to "false"),
        supportLevel = SimulationSupportLevel.MIXED_SIGNAL,
        description = "Double-pole single-throw (DPST) switch simultaneously switching two isolated lines.",
        datasheetSummary = "Tap to open/close Pole A (A1-A2) and Pole B (B1-B2) together."
    ),
    ComponentMetadata(
        type = ComponentType.DPDT_SWITCH,
        displayName = "DPDT Reversing Switch",
        shortCode = "DPDT",
        category = ComponentCategory.INPUT,
        widthGrid = 4f,
        heightGrid = 6f,
        pins = listOf(
            PinSpec("com1", "C1", "C1", PinDirection.BIDIRECTIONAL, -2f, -1.5f),
            PinSpec("no1", "NO1", "NO1", PinDirection.BIDIRECTIONAL, 2f, -2.5f),
            PinSpec("nc1", "NC1", "NC1", PinDirection.BIDIRECTIONAL, 2f, -0.5f),
            PinSpec("com2", "C2", "C2", PinDirection.BIDIRECTIONAL, -2f, 1.5f),
            PinSpec("no2", "NO2", "NO2", PinDirection.BIDIRECTIONAL, 2f, 0.5f),
            PinSpec("nc2", "NC2", "NC2", PinDirection.BIDIRECTIONAL, 2f, 2.5f)
        ),
        defaultValue = "NC",
        defaultProperties = mapOf("position" to "NC"),
        supportLevel = SimulationSupportLevel.MIXED_SIGNAL,
        description = "Double-pole double-throw (DPDT) switch ideal for motor reversing and dual signal routing.",
        datasheetSummary = "Tap to flip both C1 and C2 simultaneously between NC and NO throws."
    ),
    ComponentMetadata(
        type = ComponentType.PUSH_BUTTON,
        displayName = "Push Button (NO)",
        shortCode = "PB_NO",
        category = ComponentCategory.INPUT,
        widthGrid = 4f,
        heightGrid = 2f,
        pins = listOf(
            PinSpec("p1", "1", "1", PinDirection.BIDIRECTIONAL, -2f, 0f),
            PinSpec("p2", "2", "2", PinDirection.BIDIRECTIONAL, 2f, 0f)
        ),
        defaultValue = "OPEN",
        defaultProperties = mapOf("pressed" to "false"),
        supportLevel = SimulationSupportLevel.MIXED_SIGNAL,
        description = "Momentary normally-open (NO) push button switch.",
        datasheetSummary = "Tap to toggle pressed (CLOSED) or released (OPEN)."
    ),
    ComponentMetadata(
        type = ComponentType.PUSH_BUTTON_NC,
        displayName = "Push Button (NC)",
        shortCode = "PB_NC",
        category = ComponentCategory.INPUT,
        widthGrid = 4f,
        heightGrid = 2f,
        pins = listOf(
            PinSpec("p1", "1", "1", PinDirection.BIDIRECTIONAL, -2f, 0f),
            PinSpec("p2", "2", "2", PinDirection.BIDIRECTIONAL, 2f, 0f)
        ),
        defaultValue = "CLOSED",
        defaultProperties = mapOf("pressed" to "false"),
        supportLevel = SimulationSupportLevel.MIXED_SIGNAL,
        description = "Normally-closed (NC) push-to-break switch that opens when pressed.",
        datasheetSummary = "Closed by default; tap to press and break the circuit."
    ),
    ComponentMetadata(
        type = ComponentType.ROTARY_SWITCH,
        displayName = "4-Way Rotary Switch",
        shortCode = "ROT4",
        category = ComponentCategory.INPUT,
        widthGrid = 4f,
        heightGrid = 5f,
        pins = listOf(
            PinSpec("com", "COM", "COM", PinDirection.BIDIRECTIONAL, -2f, 0f),
            PinSpec("p1", "P1", "1", PinDirection.BIDIRECTIONAL, 2f, -1.5f),
            PinSpec("p2", "P2", "2", PinDirection.BIDIRECTIONAL, 2f, -0.5f),
            PinSpec("p3", "P3", "3", PinDirection.BIDIRECTIONAL, 2f, 0.5f),
            PinSpec("p4", "P4", "4", PinDirection.BIDIRECTIONAL, 2f, 1.5f)
        ),
        defaultValue = "POS 1",
        defaultProperties = mapOf("position" to "1"),
        supportLevel = SimulationSupportLevel.MIXED_SIGNAL,
        description = "4-position rotary selector switch routing COM to P1, P2, P3, or P4.",
        datasheetSummary = "Tap on canvas to step selector dial 1 -> 2 -> 3 -> 4."
    ),
    ComponentMetadata(
        type = ComponentType.VOLTAGE_CONTROLLED_SWITCH,
        displayName = "Voltage-Ctrl Switch",
        shortCode = "VSW",
        category = ComponentCategory.INPUT,
        widthGrid = 4f,
        heightGrid = 4f,
        pins = listOf(
            PinSpec("ctrl_pos", "C+", "C+", PinDirection.INPUT, 0f, -2f),
            PinSpec("ctrl_neg", "C-", "C-", PinDirection.INPUT, 0f, 2f),
            PinSpec("sw1", "S1", "S1", PinDirection.BIDIRECTIONAL, -2f, 0f),
            PinSpec("sw2", "S2", "S2", PinDirection.BIDIRECTIONAL, 2f, 0f)
        ),
        defaultValue = "Vth=2.5V",
        defaultProperties = mapOf("von" to "2.5", "ron" to "0.05"),
        supportLevel = SimulationSupportLevel.FULL_ANALOG,
        description = "EWB voltage-controlled analog switch closing S1-S2 when V(C+) - V(C-) >= Vth.",
        datasheetSummary = "High-impedance control input; closes S1-S2 at Ron = 0.05Ω when triggered."
    ),
    ComponentMetadata(
        type = ComponentType.RELAY_SPDT,
        displayName = "SPDT Relay",
        shortCode = "RLY_C",
        category = ComponentCategory.INPUT,
        widthGrid = 4f,
        heightGrid = 4f,
        pins = listOf(
            PinSpec("coil1", "L1", "L+", PinDirection.INPUT, -2f, -1f),
            PinSpec("coil2", "L2", "L-", PinDirection.INPUT, -2f, 1f),
            PinSpec("com", "COM", "COM", PinDirection.BIDIRECTIONAL, 2f, 0f),
            PinSpec("no", "NO", "NO", PinDirection.BIDIRECTIONAL, 2f, -1.5f),
            PinSpec("nc", "NC", "NC", PinDirection.BIDIRECTIONAL, 2f, 1.5f)
        ),
        defaultValue = "5V SPDT",
        defaultProperties = mapOf("coilVoltage" to "5.0", "coilResistance" to "100"),
        supportLevel = SimulationSupportLevel.FULL_ANALOG,
        description = "Electromagnetic SPDT changeover relay switching COM from NC to NO when energized.",
        datasheetSummary = "100Ω coil; switches COM->NO when |V_coil| >= 3.5V, otherwise COM->NC."
    ),
    ComponentMetadata(
        type = ComponentType.DIP_SWITCH,
        displayName = "4-Bit DIP Switch",
        shortCode = "DIP4",
        category = ComponentCategory.INPUT,
        widthGrid = 4f,
        heightGrid = 5f,
        pins = listOf(
            PinSpec("d0", "D0", "0", PinDirection.OUTPUT, 2f, -1.5f),
            PinSpec("d1", "D1", "1", PinDirection.OUTPUT, 2f, -0.5f),
            PinSpec("d2", "D2", "2", PinDirection.OUTPUT, 2f, 0.5f),
            PinSpec("d3", "D3", "3", PinDirection.OUTPUT, 2f, 1.5f)
        ),
        defaultValue = "0000",
        defaultProperties = mapOf("bits" to "0000"),
        supportLevel = SimulationSupportLevel.FULL_DIGITAL,
        description = "4-channel binary DIP switch outputting 4 independent logic bits.",
        datasheetSummary = "Tap individual bits in Inspector or quick-cycle to set 4-bit binary nibble."
    ),
    ComponentMetadata(
        type = ComponentType.CLOCK_INPUT,
        displayName = "Clock Generator",
        shortCode = "CLK",
        category = ComponentCategory.INPUT,
        widthGrid = 4f,
        heightGrid = 2f,
        pins = listOf(
            PinSpec("out", "CLK", "CLK", PinDirection.OUTPUT, 2f, 0f)
        ),
        defaultValue = "1 Hz",
        defaultProperties = mapOf("frequency" to "1.0", "state" to "0"),
        supportLevel = SimulationSupportLevel.FULL_DIGITAL,
        description = "Square-wave digital clock source for flip-flops, counters, and shift registers.",
        datasheetSummary = "Alternates 0 and 1 each clock cycle; supports rising/falling edge detection."
    ),
    ComponentMetadata(
        type = ComponentType.LOGIC_INPUT,
        displayName = "Logic Input (0/1)",
        shortCode = "IN",
        category = ComponentCategory.INPUT,
        widthGrid = 3f,
        heightGrid = 2f,
        pins = listOf(
            PinSpec("out", "OUT", "OUT", PinDirection.OUTPUT, 1.5f, 0f)
        ),
        defaultValue = "0",
        defaultProperties = mapOf("state" to "0"),
        supportLevel = SimulationSupportLevel.FULL_DIGITAL,
        description = "Interactive logic source toggling cleanly between Logic 0 and Logic 1.",
        datasheetSummary = "Tap directly on canvas during Run or Edit mode to flip 0 <-> 1."
    ),
    ComponentMetadata(
        type = ComponentType.KEYPAD,
        displayName = "Hex Keypad (4-bit)",
        shortCode = "KEY",
        category = ComponentCategory.INPUT,
        widthGrid = 4f,
        heightGrid = 5f,
        pins = listOf(
            PinSpec("b0", "B0", "B0", PinDirection.OUTPUT, 2f, -1.5f),
            PinSpec("b1", "B1", "B1", PinDirection.OUTPUT, 2f, -0.5f),
            PinSpec("b2", "B2", "B2", PinDirection.OUTPUT, 2f, 0.5f),
            PinSpec("b3", "B3", "B3", PinDirection.OUTPUT, 2f, 1.5f)
        ),
        defaultValue = "0x0",
        defaultProperties = mapOf("hexDigit" to "0"),
        supportLevel = SimulationSupportLevel.FULL_DIGITAL,
        description = "0–F encoded keypad outputting 4-bit binary (B3..B0).",
        datasheetSummary = "Tap to step hex digit 0..F or select digit in inspector."
    ),
    ComponentMetadata(
        type = ComponentType.VARIABLE_INPUT,
        displayName = "Variable DC Input",
        shortCode = "VIN",
        category = ComponentCategory.INPUT,
        widthGrid = 4f,
        heightGrid = 2f,
        pins = listOf(
            PinSpec("out", "+", "+", PinDirection.POWER, 2f, 0f),
            PinSpec("ref", "-", "-", PinDirection.GROUND, -2f, 0f)
        ),
        defaultValue = "3.3V",
        defaultProperties = mapOf("voltage" to "3.3"),
        supportLevel = SimulationSupportLevel.FULL_ANALOG,
        description = "Adjustable 0–15V analog reference voltage source.",
        datasheetSummary = "Ideal adjustable voltage input."
    ),

    // F. Output & Indicator Components (EWB Indicators Family)
    ComponentMetadata(
        type = ComponentType.PROBE_RED,
        displayName = "Red Probe",
        shortCode = "P_RED",
        category = ComponentCategory.OUTPUT,
        widthGrid = 2.5f,
        heightGrid = 3f,
        pins = listOf(
            PinSpec("in", "IN", "IN", PinDirection.INPUT, 0f, 1.5f)
        ),
        defaultValue = "Red",
        defaultProperties = mapOf("color" to "Red", "thresholdV" to "1.5"),
        supportLevel = SimulationSupportLevel.MIXED_SIGNAL,
        description = "1-input Red indicator probe lighting up bright crimson on Logic 1 or >=1.5V.",
        datasheetSummary = "Single-terminal EWB-style red probe lamp. Lights up vividly when energized."
    ),
    ComponentMetadata(
        type = ComponentType.PROBE_GREEN,
        displayName = "Green Probe",
        shortCode = "P_GRN",
        category = ComponentCategory.OUTPUT,
        widthGrid = 2.5f,
        heightGrid = 3f,
        pins = listOf(
            PinSpec("in", "IN", "IN", PinDirection.INPUT, 0f, 1.5f)
        ),
        defaultValue = "Green",
        defaultProperties = mapOf("color" to "Green", "thresholdV" to "1.5"),
        supportLevel = SimulationSupportLevel.MIXED_SIGNAL,
        description = "1-input Green indicator probe illuminating bright emerald on Logic 1 or >=1.5V.",
        datasheetSummary = "Single-terminal EWB-style green probe lamp. Lights up vividly when energized."
    ),
    ComponentMetadata(
        type = ComponentType.PROBE_YELLOW,
        displayName = "Yellow Probe",
        shortCode = "P_YEL",
        category = ComponentCategory.OUTPUT,
        widthGrid = 2.5f,
        heightGrid = 3f,
        pins = listOf(
            PinSpec("in", "IN", "IN", PinDirection.INPUT, 0f, 1.5f)
        ),
        defaultValue = "Yellow",
        defaultProperties = mapOf("color" to "Yellow", "thresholdV" to "1.5"),
        supportLevel = SimulationSupportLevel.MIXED_SIGNAL,
        description = "1-input Yellow indicator probe glowing bright gold on Logic 1 or >=1.5V.",
        datasheetSummary = "Single-terminal EWB-style yellow probe lamp. Lights up vividly when energized."
    ),
    ComponentMetadata(
        type = ComponentType.PROBE_BLUE,
        displayName = "Blue Probe",
        shortCode = "P_BLU",
        category = ComponentCategory.OUTPUT,
        widthGrid = 2.5f,
        heightGrid = 3f,
        pins = listOf(
            PinSpec("in", "IN", "IN", PinDirection.INPUT, 0f, 1.5f)
        ),
        defaultValue = "Blue",
        defaultProperties = mapOf("color" to "Blue", "thresholdV" to "1.5"),
        supportLevel = SimulationSupportLevel.MIXED_SIGNAL,
        description = "1-input Blue indicator probe shining electric blue on Logic 1 or >=1.5V.",
        datasheetSummary = "Single-terminal EWB-style blue probe lamp. Lights up vividly when energized."
    ),
    ComponentMetadata(
        type = ComponentType.PROBE_ORANGE,
        displayName = "Orange Probe",
        shortCode = "P_ORG",
        category = ComponentCategory.OUTPUT,
        widthGrid = 2.5f,
        heightGrid = 3f,
        pins = listOf(
            PinSpec("in", "IN", "IN", PinDirection.INPUT, 0f, 1.5f)
        ),
        defaultValue = "Orange",
        defaultProperties = mapOf("color" to "Orange", "thresholdV" to "1.5"),
        supportLevel = SimulationSupportLevel.MIXED_SIGNAL,
        description = "1-input Orange indicator probe glowing vivid amber-orange on Logic 1 or >=1.5V.",
        datasheetSummary = "Single-terminal EWB-style orange probe lamp. Lights up vividly when energized."
    ),
    ComponentMetadata(
        type = ComponentType.PROBE_CYAN,
        displayName = "Cyan Probe",
        shortCode = "P_CYN",
        category = ComponentCategory.OUTPUT,
        widthGrid = 2.5f,
        heightGrid = 3f,
        pins = listOf(
            PinSpec("in", "IN", "IN", PinDirection.INPUT, 0f, 1.5f)
        ),
        defaultValue = "Cyan",
        defaultProperties = mapOf("color" to "Cyan", "thresholdV" to "1.5"),
        supportLevel = SimulationSupportLevel.MIXED_SIGNAL,
        description = "1-input Cyan indicator probe illuminating neon cyan on Logic 1 or >=1.5V.",
        datasheetSummary = "Single-terminal EWB-style cyan probe lamp. Lights up vividly when energized."
    ),
    ComponentMetadata(
        type = ComponentType.PILOT_LAMP,
        displayName = "Light Bulb / Lamp",
        shortCode = "LAMP",
        category = ComponentCategory.OUTPUT,
        widthGrid = 4f,
        heightGrid = 3f,
        pins = listOf(
            PinSpec("p1", "1", "1", PinDirection.BIDIRECTIONAL, -2f, 0f),
            PinSpec("p2", "2", "2", PinDirection.BIDIRECTIONAL, 2f, 0f)
        ),
        defaultValue = "5V 50Ω",
        defaultProperties = mapOf("resistance" to "50.0", "ratedV" to "5.0"),
        supportLevel = SimulationSupportLevel.MIXED_SIGNAL,
        description = "EWB incandescent filament bulb illuminating warm golden light when energized (>=1.5V or Logic 1).",
        datasheetSummary = "50Ω filament resistance; brightness scales with applied voltage."
    ),
    ComponentMetadata(
        type = ComponentType.BAR_GRAPH,
        displayName = "Bargraph Display",
        shortCode = "BAR4",
        category = ComponentCategory.OUTPUT,
        widthGrid = 4f,
        heightGrid = 5f,
        pins = listOf(
            PinSpec("b3", "B3", "B3", PinDirection.INPUT, -2f, -1.5f),
            PinSpec("b2", "B2", "B2", PinDirection.INPUT, -2f, -0.5f),
            PinSpec("b1", "B1", "B1", PinDirection.INPUT, -2f, 0.5f),
            PinSpec("b0", "B0", "B0", PinDirection.INPUT, -2f, 1.5f)
        ),
        defaultValue = "4-LED Bar",
        supportLevel = SimulationSupportLevel.MIXED_SIGNAL,
        description = "4-channel LED bargraph indicator illuminating each horizontal bar independently.",
        datasheetSummary = "Direct-drive LED bar array (B3 top to B0 bottom)."
    ),
    ComponentMetadata(
        type = ComponentType.DECODED_BAR_GRAPH,
        displayName = "Decoded Bargraph",
        shortCode = "DBAR",
        category = ComponentCategory.OUTPUT,
        widthGrid = 4f,
        heightGrid = 5f,
        pins = listOf(
            PinSpec("b3", "8", "8", PinDirection.INPUT, -2f, -1.5f),
            PinSpec("b2", "4", "4", PinDirection.INPUT, -2f, -0.5f),
            PinSpec("b1", "2", "2", PinDirection.INPUT, -2f, 0.5f),
            PinSpec("b0", "1", "1", PinDirection.INPUT, -2f, 1.5f)
        ),
        defaultValue = "0-15 Level",
        supportLevel = SimulationSupportLevel.FULL_DIGITAL,
        description = "Decoded 4-bit binary bargraph meter lighting progressive LED segments from 0 to 15.",
        datasheetSummary = "Reads 4-bit BCD/binary (B3..B0) and fills the vertical LED bargraph proportionally."
    ),
    ComponentMetadata(
        type = ComponentType.LOGIC_INDICATOR,
        displayName = "Logic Probe",
        shortCode = "PROBE",
        category = ComponentCategory.OUTPUT,
        widthGrid = 3f,
        heightGrid = 2f,
        pins = listOf(
            PinSpec("in", "IN", "IN", PinDirection.INPUT, -1.5f, 0f)
        ),
        defaultValue = "X",
        supportLevel = SimulationSupportLevel.MIXED_SIGNAL,
        description = "Digital logic indicator displaying 0 (Low), 1 (High), X (Unknown), Z (High-Z).",
        datasheetSummary = "Non-loading high-impedance digital probe."
    ),
    ComponentMetadata(
        type = ComponentType.LED_INDICATOR,
        displayName = "Logic LED",
        shortCode = "L_LED",
        category = ComponentCategory.OUTPUT,
        widthGrid = 3f,
        heightGrid = 2f,
        pins = listOf(
            PinSpec("in", "IN", "IN", PinDirection.INPUT, -1.5f, 0f)
        ),
        defaultValue = "Green",
        defaultProperties = mapOf("color" to "Green"),
        supportLevel = SimulationSupportLevel.MIXED_SIGNAL,
        description = "Digital indicator LED illuminating bright green when input is Logic 1.",
        datasheetSummary = "Lights up when driven by Logic 1."
    ),
    ComponentMetadata(
        type = ComponentType.SEVEN_SEGMENT,
        displayName = "7-Segment Display",
        shortCode = "7SEG",
        category = ComponentCategory.OUTPUT,
        widthGrid = 6f,
        heightGrid = 8f,
        pins = listOf(
            PinSpec("a", "A", "A", PinDirection.INPUT, -3f, -3f),
            PinSpec("b", "B", "B", PinDirection.INPUT, -3f, -2f),
            PinSpec("c", "C", "C", PinDirection.INPUT, -3f, -1f),
            PinSpec("d", "D", "D", PinDirection.INPUT, -3f, 0f),
            PinSpec("e", "E", "E", PinDirection.INPUT, -3f, 1f),
            PinSpec("f", "F", "F", PinDirection.INPUT, -3f, 2f),
            PinSpec("g", "G", "G", PinDirection.INPUT, -3f, 3f)
        ),
        defaultValue = "Common Cathode",
        supportLevel = SimulationSupportLevel.FULL_DIGITAL,
        description = "7-segment LED display with individual A–G segment inputs.",
        datasheetSummary = "Active-high segments a..g."
    ),
    ComponentMetadata(
        type = ComponentType.HEX_DISPLAY,
        displayName = "Hex 7-Seg Display",
        shortCode = "HEX",
        category = ComponentCategory.OUTPUT,
        widthGrid = 5f,
        heightGrid = 5f,
        pins = listOf(
            PinSpec("b3", "B3", "8", PinDirection.INPUT, -2.5f, -1.5f),
            PinSpec("b2", "B2", "4", PinDirection.INPUT, -2.5f, -0.5f),
            PinSpec("b1", "B1", "2", PinDirection.INPUT, -2.5f, 0.5f),
            PinSpec("b0", "B0", "1", PinDirection.INPUT, -2.5f, 1.5f)
        ),
        defaultValue = "0-F",
        supportLevel = SimulationSupportLevel.FULL_DIGITAL,
        description = "Decoded 4-bit hexadecimal 7-segment display (0–F).",
        datasheetSummary = "B3(MSB)..B0(LSB) decoded directly to hex digit 0..F."
    ),
    ComponentMetadata(
        type = ComponentType.DIGITAL_DISPLAY,
        displayName = "4-Bit Binary Display",
        shortCode = "DIG",
        category = ComponentCategory.OUTPUT,
        widthGrid = 5f,
        heightGrid = 5f,
        pins = listOf(
            PinSpec("b3", "B3", "B3", PinDirection.INPUT, -2.5f, -1.5f),
            PinSpec("b2", "B2", "B2", PinDirection.INPUT, -2.5f, -0.5f),
            PinSpec("b1", "B1", "B1", PinDirection.INPUT, -2.5f, 0.5f),
            PinSpec("b0", "B0", "B0", PinDirection.INPUT, -2.5f, 1.5f)
        ),
        defaultValue = "DEC",
        supportLevel = SimulationSupportLevel.FULL_DIGITAL,
        description = "4-bit binary/decimal digital numeric readout.",
        datasheetSummary = "Displays 4-bit binary value and decimal equivalent."
    ),
    ComponentMetadata(
        type = ComponentType.BUZZER,
        displayName = "Piezo Buzzer",
        shortCode = "BUZ",
        category = ComponentCategory.OUTPUT,
        widthGrid = 4f,
        heightGrid = 3f,
        pins = listOf(
            PinSpec("pos", "+", "+", PinDirection.INPUT, -2f, 0f),
            PinSpec("neg", "-", "-", PinDirection.GROUND, 2f, 0f)
        ),
        defaultValue = "1 kHz",
        defaultProperties = mapOf("resistance" to "1000"),
        supportLevel = SimulationSupportLevel.MIXED_SIGNAL,
        description = "Audio transducer activating when energized by logic HIGH or >2.0V.",
        datasheetSummary = "Visual acoustic pulse indicator + real ToneGenerator audio option when active."
    ),
    ComponentMetadata(
        type = ComponentType.MOTOR,
        displayName = "DC Motor",
        shortCode = "MTR",
        category = ComponentCategory.OUTPUT,
        widthGrid = 4f,
        heightGrid = 3f,
        pins = listOf(
            PinSpec("pos", "+", "+", PinDirection.BIDIRECTIONAL, -2f, 0f),
            PinSpec("neg", "-", "-", PinDirection.BIDIRECTIONAL, 2f, 0f)
        ),
        defaultValue = "12V (24Ω)",
        defaultProperties = mapOf("resistance" to "24.0"),
        supportLevel = SimulationSupportLevel.FULL_ANALOG,
        description = "Brushed DC motor load modeled with armature resistance and animated RPM.",
        datasheetSummary = "Spinning rotor animation speed scales with |V_pos - V_neg|."
    ),
    ComponentMetadata(
        type = ComponentType.RELAY_OUTPUT,
        displayName = "Relay Indicator",
        shortCode = "RLY_OUT",
        category = ComponentCategory.OUTPUT,
        widthGrid = 4f,
        heightGrid = 3f,
        pins = listOf(
            PinSpec("in", "IN", "IN", PinDirection.INPUT, -2f, 0f)
        ),
        defaultValue = "NO",
        supportLevel = SimulationSupportLevel.FULL_DIGITAL,
        description = "Logic-controlled relay actuator indicator.",
        datasheetSummary = "Energizes when IN = Logic 1."
    ),
    ComponentMetadata(
        type = ComponentType.LCD_16X2,
        displayName = "16×2 LCD Display",
        shortCode = "LCD",
        category = ComponentCategory.OUTPUT,
        widthGrid = 8f,
        heightGrid = 5f,
        pins = listOf(
            PinSpec("en", "EN", "EN", PinDirection.INPUT, -4f, -2f),
            PinSpec("d0", "D0", "D0", PinDirection.INPUT, -4f, -1f),
            PinSpec("d1", "D1", "D1", PinDirection.INPUT, -4f, 0f),
            PinSpec("d2", "D2", "D2", PinDirection.INPUT, -4f, 1f),
            PinSpec("d3", "D3", "D3", PinDirection.INPUT, -4f, 2f)
        ),
        defaultValue = "GATEX LABS",
        defaultProperties = mapOf("line1" to "GATEX LABS"),
        supportLevel = SimulationSupportLevel.FULL_DIGITAL,
        description = "Backlit 16×2 alphanumeric character LCD showing custom title on Line 1 and live 4-bit binary/hex value on Line 2 when EN=1.",
        datasheetSummary = "Tie EN HIGH (or pulse) to enable backlight; D3..D0 display live decimal & hex readout."
    ),
    ComponentMetadata(
        type = ComponentType.DOT_MATRIX_8X8,
        displayName = "8×8 LED Matrix",
        shortCode = "MAT8",
        category = ComponentCategory.OUTPUT,
        widthGrid = 7f,
        heightGrid = 6f,
        pins = listOf(
            PinSpec("r0", "R0", "R0", PinDirection.INPUT, -3.5f, -1.5f),
            PinSpec("r1", "R1", "R1", PinDirection.INPUT, -3.5f, -0.5f),
            PinSpec("r2", "R2", "R2", PinDirection.INPUT, -3.5f, 0.5f),
            PinSpec("r3", "R3", "R3", PinDirection.INPUT, -3.5f, 1.5f),
            PinSpec("c0", "C0", "C0", PinDirection.INPUT, 3.5f, -1.5f),
            PinSpec("c1", "C1", "C1", PinDirection.INPUT, 3.5f, -0.5f),
            PinSpec("c2", "C2", "C2", PinDirection.INPUT, 3.5f, 0.5f),
            PinSpec("c3", "C3", "C3", PinDirection.INPUT, 3.5f, 1.5f)
        ),
        defaultValue = "Red 8×8",
        supportLevel = SimulationSupportLevel.FULL_DIGITAL,
        description = "8×8 multiplexed LED dot-matrix display driven by 4 row pairs (R0–R3) and 4 column pairs (C0–C3).",
        datasheetSummary = "An LED dot lights when both its row pin (R0–R3) and column pin (C0–C3) are Logic 1."
    ),
    ComponentMetadata(
        type = ComponentType.TRAFFIC_LIGHT,
        displayName = "Traffic Light Module",
        shortCode = "TFL",
        category = ComponentCategory.OUTPUT,
        widthGrid = 4f,
        heightGrid = 6f,
        pins = listOf(
            PinSpec("r", "RED", "R", PinDirection.INPUT, -2f, -2f),
            PinSpec("y", "YEL", "Y", PinDirection.INPUT, -2f, 0f),
            PinSpec("g", "GRN", "G", PinDirection.INPUT, -2f, 2f),
            PinSpec("gnd", "GND", "GND", PinDirection.GROUND, 2f, 2f)
        ),
        defaultValue = "R/Y/G",
        supportLevel = SimulationSupportLevel.MIXED_SIGNAL,
        description = "3-lens vertical traffic signal module with independent Red, Yellow, and Green inputs.",
        datasheetSummary = "Apply Logic 1 (or >1.5V) to R, Y, or G to illuminate each corresponding traffic lens."
    ),

    // G. Virtual Instruments
    ComponentMetadata(
        type = ComponentType.MULTIMETER,
        displayName = "Digital Multimeter",
        shortCode = "DMM",
        category = ComponentCategory.INSTRUMENTS,
        widthGrid = 5f,
        heightGrid = 4f,
        pins = listOf(
            PinSpec("pos", "V/Ω+", "+", PinDirection.INPUT, -2.5f, 1f),
            PinSpec("com", "COM", "-", PinDirection.INPUT, 2.5f, 1f)
        ),
        defaultValue = "Voltage (DC)",
        defaultProperties = mapOf("mode" to "VOLTAGE"),
        supportLevel = SimulationSupportLevel.INSTRUMENT,
        description = "4-mode Digital Multimeter measuring Voltage, Current, Resistance, and Continuity.",
        datasheetSummary = "Connect + and COM across any two nodes; open Instruments drawer for full DMM readout."
    ),
    ComponentMetadata(
        type = ComponentType.VOLTMETER,
        displayName = "DC Voltmeter",
        shortCode = "V",
        category = ComponentCategory.INSTRUMENTS,
        widthGrid = 4f,
        heightGrid = 3f,
        pins = listOf(
            PinSpec("pos", "+", "+", PinDirection.INPUT, -2f, 0f),
            PinSpec("neg", "-", "-", PinDirection.INPUT, 2f, 0f)
        ),
        defaultValue = "10MΩ",
        supportLevel = SimulationSupportLevel.INSTRUMENT,
        description = "Parallel voltmeter measuring differential node voltage V(+) - V(-).",
        datasheetSummary = "High input impedance (10MΩ) prevents circuit loading."
    ),
    ComponentMetadata(
        type = ComponentType.AMMETER,
        displayName = "DC Ammeter",
        shortCode = "A",
        category = ComponentCategory.INSTRUMENTS,
        widthGrid = 4f,
        heightGrid = 3f,
        pins = listOf(
            PinSpec("pos", "+", "+", PinDirection.BIDIRECTIONAL, -2f, 0f),
            PinSpec("neg", "-", "-", PinDirection.BIDIRECTIONAL, 2f, 0f)
        ),
        defaultValue = "1mΩ",
        supportLevel = SimulationSupportLevel.INSTRUMENT,
        description = "Series current meter measuring branch current from + to -.",
        datasheetSummary = "Low shunt impedance (1mΩ). Connect in series with load."
    ),
    ComponentMetadata(
        type = ComponentType.OHMMETER,
        displayName = "Ohmmeter",
        shortCode = "Ω",
        category = ComponentCategory.INSTRUMENTS,
        widthGrid = 4f,
        heightGrid = 3f,
        pins = listOf(
            PinSpec("pos", "+", "+", PinDirection.INPUT, -2f, 0f),
            PinSpec("neg", "-", "-", PinDirection.INPUT, 2f, 0f)
        ),
        defaultValue = "Auto",
        supportLevel = SimulationSupportLevel.INSTRUMENT,
        description = "Passive network resistance meter between two probes.",
        datasheetSummary = "Computes Thévenin equivalent resistance across + and -."
    ),
    ComponentMetadata(
        type = ComponentType.OSCILLOSCOPE,
        displayName = "2-Ch Oscilloscope",
        shortCode = "OSC",
        category = ComponentCategory.INSTRUMENTS,
        widthGrid = 5f,
        heightGrid = 4f,
        pins = listOf(
            PinSpec("ch1", "CH1", "CH1", PinDirection.INPUT, -2.5f, -1f),
            PinSpec("ch2", "CH2", "CH2", PinDirection.INPUT, -2.5f, 1f),
            PinSpec("gnd", "GND", "GND", PinDirection.GROUND, 2.5f, 1f)
        ),
        defaultValue = "1ms/div",
        supportLevel = SimulationSupportLevel.INSTRUMENT,
        description = "Dual-channel real-time analog/transient waveform oscilloscope.",
        datasheetSummary = "Tap Instruments -> Oscilloscope for timebase, V/div, trigger, pause & zoom."
    ),
    ComponentMetadata(
        type = ComponentType.FUNCTION_GENERATOR,
        displayName = "Function Generator",
        shortCode = "FGEN",
        category = ComponentCategory.INSTRUMENTS,
        widthGrid = 5f,
        heightGrid = 4f,
        pins = listOf(
            PinSpec("out", "OUT", "OUT", PinDirection.POWER, 2.5f, -1f),
            PinSpec("gnd", "COM", "COM", PinDirection.GROUND, 2.5f, 1f)
        ),
        defaultValue = "1kHz 5V Sine",
        defaultProperties = mapOf(
            "waveform" to "SINE",
            "frequency" to "1000.0",
            "amplitude" to "5.0",
            "offset" to "0.0"
        ),
        supportLevel = SimulationSupportLevel.INSTRUMENT,
        description = "Signal generator producing Sine, Square, and Triangle waves with adjustable f, Vpp, offset.",
        datasheetSummary = "Drives analog transient & digital square-wave circuits."
    ),
    ComponentMetadata(
        type = ComponentType.LOGIC_ANALYZER,
        displayName = "Logic Analyzer",
        shortCode = "LA4",
        category = ComponentCategory.INSTRUMENTS,
        widthGrid = 5f,
        heightGrid = 5f,
        pins = listOf(
            PinSpec("ch0", "D0", "D0", PinDirection.INPUT, -2.5f, -1.5f),
            PinSpec("ch1", "D1", "D1", PinDirection.INPUT, -2.5f, -0.5f),
            PinSpec("ch2", "D2", "D2", PinDirection.INPUT, -2.5f, 0.5f),
            PinSpec("ch3", "D3", "D3", PinDirection.INPUT, -2.5f, 1.5f)
        ),
        defaultValue = "4-Channel",
        supportLevel = SimulationSupportLevel.INSTRUMENT,
        description = "Multi-channel digital timing analyzer recording Logic 0/1 transitions over time.",
        datasheetSummary = "Inspect timing diagrams and clock edges in the Instruments drawer."
    ),
    ComponentMetadata(
        type = ComponentType.FREQUENCY_COUNTER,
        displayName = "Frequency Counter",
        shortCode = "FREQ",
        category = ComponentCategory.INSTRUMENTS,
        widthGrid = 4f,
        heightGrid = 3f,
        pins = listOf(
            PinSpec("in", "IN", "IN", PinDirection.INPUT, -2f, 0f)
        ),
        defaultValue = "0 Hz",
        supportLevel = SimulationSupportLevel.INSTRUMENT,
        description = "Measures signal switching frequency from rising edges or AC sources.",
        datasheetSummary = "Counts rising logic transitions or AC frequency attached to IN."
    )
)

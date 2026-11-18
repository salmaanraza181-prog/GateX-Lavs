package com.example.model

private data class DipPinDef(
    val pinNumber: Int,
    val id: String,
    val label: String,
    val direction: PinDirection,
    val activeLow: Boolean = false,
    val isClock: Boolean = false
)

/**
 * Builds an authentic 14-pin DIP schematic pin layout matching Electronics Workbench / Multisim:
 * - Left side (top to bottom): Pins 1..7 at relX = -4f, relY = -3f..+3f
 * - Right side (top to bottom): Pins 14..8 at relX = +4f, relY = -3f..+3f
 */
private fun dip14Pins(
    left1To7: List<DipPinDef>,
    right14To8: List<DipPinDef>
): List<PinSpec> {
    val leftSpecs = left1To7.mapIndexed { idx, p ->
        PinSpec(
            id = p.id,
            name = p.label,
            label = p.label,
            direction = p.direction,
            relX = -4f,
            relY = -3f + idx.toFloat(),
            pinNumber = p.pinNumber,
            activeLow = p.activeLow,
            isClock = p.isClock
        )
    }
    val rightSpecs = right14To8.mapIndexed { idx, p ->
        PinSpec(
            id = p.id,
            name = p.label,
            label = p.label,
            direction = p.direction,
            relX = 4f,
            relY = -3f + idx.toFloat(),
            pinNumber = p.pinNumber,
            activeLow = p.activeLow,
            isClock = p.isClock
        )
    }
    return leftSpecs + rightSpecs
}

private fun dip14PinoutTable(
    left1To7: List<DipPinDef>,
    right14To8: List<DipPinDef>
): List<Pair<String, String>> = left1To7.indices.map { i ->
    val l = left1To7[i]
    val r = right14To8[i]
    "${l.pinNumber}: ${l.label}" to "${r.pinNumber}: ${r.label}"
}

private fun buildQuad2InDip14(
    type: ComponentType,
    displayName: String,
    shortCode: String,
    defaultValue: String,
    description: String,
    datasheetSummary: String,
    invertedOutput: Boolean = false
): ComponentMetadata {
    val left = listOf(
        DipPinDef(1, "1a", "1A", PinDirection.INPUT),
        DipPinDef(2, "1b", "1B", PinDirection.INPUT),
        DipPinDef(3, "1y", "1Y", PinDirection.OUTPUT, activeLow = invertedOutput),
        DipPinDef(4, "2a", "2A", PinDirection.INPUT),
        DipPinDef(5, "2b", "2B", PinDirection.INPUT),
        DipPinDef(6, "2y", "2Y", PinDirection.OUTPUT, activeLow = invertedOutput),
        DipPinDef(7, "gnd", "GND", PinDirection.GROUND)
    )
    val right = listOf(
        DipPinDef(14, "vcc", "VCC", PinDirection.POWER),
        DipPinDef(13, "4b", "4B", PinDirection.INPUT),
        DipPinDef(12, "4a", "4A", PinDirection.INPUT),
        DipPinDef(11, "4y", "4Y", PinDirection.OUTPUT, activeLow = invertedOutput),
        DipPinDef(10, "3b", "3B", PinDirection.INPUT),
        DipPinDef(9, "3a", "3A", PinDirection.INPUT),
        DipPinDef(8, "3y", "3Y", PinDirection.OUTPUT, activeLow = invertedOutput)
    )
    return ComponentMetadata(
        type = type,
        displayName = displayName,
        shortCode = shortCode,
        category = ComponentCategory.IC_7400,
        widthGrid = 8f,
        heightGrid = 8f,
        pins = dip14Pins(left, right),
        defaultValue = defaultValue,
        supportLevel = SimulationSupportLevel.FULL_DIGITAL,
        description = description,
        datasheetSummary = datasheetSummary,
        pinoutTable = dip14PinoutTable(left, right)
    )
}

private fun buildTriple3InDip14(
    type: ComponentType,
    displayName: String,
    shortCode: String,
    defaultValue: String,
    description: String,
    datasheetSummary: String,
    invertedOutput: Boolean = false
): ComponentMetadata {
    val left = listOf(
        DipPinDef(1, "1a", "1A", PinDirection.INPUT),
        DipPinDef(2, "1b", "1B", PinDirection.INPUT),
        DipPinDef(3, "2a", "2A", PinDirection.INPUT),
        DipPinDef(4, "2b", "2B", PinDirection.INPUT),
        DipPinDef(5, "2c", "2C", PinDirection.INPUT),
        DipPinDef(6, "2y", "2Y", PinDirection.OUTPUT, activeLow = invertedOutput),
        DipPinDef(7, "gnd", "GND", PinDirection.GROUND)
    )
    val right = listOf(
        DipPinDef(14, "vcc", "VCC", PinDirection.POWER),
        DipPinDef(13, "1c", "1C", PinDirection.INPUT),
        DipPinDef(12, "1y", "1Y", PinDirection.OUTPUT, activeLow = invertedOutput),
        DipPinDef(11, "3c", "3C", PinDirection.INPUT),
        DipPinDef(10, "3b", "3B", PinDirection.INPUT),
        DipPinDef(9, "3a", "3A", PinDirection.INPUT),
        DipPinDef(8, "3y", "3Y", PinDirection.OUTPUT, activeLow = invertedOutput)
    )
    return ComponentMetadata(
        type = type,
        displayName = displayName,
        shortCode = shortCode,
        category = ComponentCategory.IC_7400,
        widthGrid = 8f,
        heightGrid = 8f,
        pins = dip14Pins(left, right),
        defaultValue = defaultValue,
        supportLevel = SimulationSupportLevel.FULL_DIGITAL,
        description = description,
        datasheetSummary = datasheetSummary,
        pinoutTable = dip14PinoutTable(left, right)
    )
}

private fun buildDual4InDip14(
    type: ComponentType,
    displayName: String,
    shortCode: String,
    defaultValue: String,
    description: String,
    datasheetSummary: String,
    invertedOutput: Boolean = false
): ComponentMetadata {
    val left = listOf(
        DipPinDef(1, "1a", "1A", PinDirection.INPUT),
        DipPinDef(2, "1b", "1B", PinDirection.INPUT),
        DipPinDef(3, "nc1", "NC", PinDirection.INPUT),
        DipPinDef(4, "1c", "1C", PinDirection.INPUT),
        DipPinDef(5, "1d", "1D", PinDirection.INPUT),
        DipPinDef(6, "1y", "1Y", PinDirection.OUTPUT, activeLow = invertedOutput),
        DipPinDef(7, "gnd", "GND", PinDirection.GROUND)
    )
    val right = listOf(
        DipPinDef(14, "vcc", "VCC", PinDirection.POWER),
        DipPinDef(13, "2d", "2D", PinDirection.INPUT),
        DipPinDef(12, "2c", "2C", PinDirection.INPUT),
        DipPinDef(11, "nc2", "NC", PinDirection.INPUT),
        DipPinDef(10, "2b", "2B", PinDirection.INPUT),
        DipPinDef(9, "2a", "2A", PinDirection.INPUT),
        DipPinDef(8, "2y", "2Y", PinDirection.OUTPUT, activeLow = invertedOutput)
    )
    return ComponentMetadata(
        type = type,
        displayName = displayName,
        shortCode = shortCode,
        category = ComponentCategory.IC_7400,
        widthGrid = 8f,
        heightGrid = 8f,
        pins = dip14Pins(left, right),
        defaultValue = defaultValue,
        supportLevel = SimulationSupportLevel.FULL_DIGITAL,
        description = description,
        datasheetSummary = datasheetSummary,
        pinoutTable = dip14PinoutTable(left, right)
    )
}

internal fun digitalAndIcMetadata(): List<ComponentMetadata> {
    // 7402 Quad 2-in NOR (outputs on pins 1, 4, 10, 13)
    val left7402 = listOf(
        DipPinDef(1, "1y", "1Y", PinDirection.OUTPUT, activeLow = true),
        DipPinDef(2, "1a", "1A", PinDirection.INPUT),
        DipPinDef(3, "1b", "1B", PinDirection.INPUT),
        DipPinDef(4, "2y", "2Y", PinDirection.OUTPUT, activeLow = true),
        DipPinDef(5, "2a", "2A", PinDirection.INPUT),
        DipPinDef(6, "2b", "2B", PinDirection.INPUT),
        DipPinDef(7, "gnd", "GND", PinDirection.GROUND)
    )
    val right7402 = listOf(
        DipPinDef(14, "vcc", "VCC", PinDirection.POWER),
        DipPinDef(13, "4y", "4Y", PinDirection.OUTPUT, activeLow = true),
        DipPinDef(12, "4b", "4B", PinDirection.INPUT),
        DipPinDef(11, "4a", "4A", PinDirection.INPUT),
        DipPinDef(10, "3y", "3Y", PinDirection.OUTPUT, activeLow = true),
        DipPinDef(9, "3b", "3B", PinDirection.INPUT),
        DipPinDef(8, "3a", "3A", PinDirection.INPUT)
    )

    // 7404 Hex Inverter
    val left7404 = listOf(
        DipPinDef(1, "1a", "1A", PinDirection.INPUT),
        DipPinDef(2, "1y", "1Y", PinDirection.OUTPUT, activeLow = true),
        DipPinDef(3, "2a", "2A", PinDirection.INPUT),
        DipPinDef(4, "2y", "2Y", PinDirection.OUTPUT, activeLow = true),
        DipPinDef(5, "3a", "3A", PinDirection.INPUT),
        DipPinDef(6, "3y", "3Y", PinDirection.OUTPUT, activeLow = true),
        DipPinDef(7, "gnd", "GND", PinDirection.GROUND)
    )
    val right7404 = listOf(
        DipPinDef(14, "vcc", "VCC", PinDirection.POWER),
        DipPinDef(13, "6a", "6A", PinDirection.INPUT),
        DipPinDef(12, "6y", "6Y", PinDirection.OUTPUT, activeLow = true),
        DipPinDef(11, "5a", "5A", PinDirection.INPUT),
        DipPinDef(10, "5y", "5Y", PinDirection.OUTPUT, activeLow = true),
        DipPinDef(9, "4a", "4A", PinDirection.INPUT),
        DipPinDef(8, "4y", "4Y", PinDirection.OUTPUT, activeLow = true)
    )

    // 7430 8-Input NAND
    val left7430 = listOf(
        DipPinDef(1, "a", "A", PinDirection.INPUT),
        DipPinDef(2, "b", "B", PinDirection.INPUT),
        DipPinDef(3, "c", "C", PinDirection.INPUT),
        DipPinDef(4, "d", "D", PinDirection.INPUT),
        DipPinDef(5, "e", "E", PinDirection.INPUT),
        DipPinDef(6, "f", "F", PinDirection.INPUT),
        DipPinDef(7, "gnd", "GND", PinDirection.GROUND)
    )
    val right7430 = listOf(
        DipPinDef(14, "vcc", "VCC", PinDirection.POWER),
        DipPinDef(13, "nc1", "NC", PinDirection.INPUT),
        DipPinDef(12, "h", "H", PinDirection.INPUT),
        DipPinDef(11, "g", "G", PinDirection.INPUT),
        DipPinDef(10, "nc2", "NC", PinDirection.INPUT),
        DipPinDef(9, "nc3", "NC", PinDirection.INPUT),
        DipPinDef(8, "y", "Y", PinDirection.OUTPUT, activeLow = true)
    )

    // 7472 JK Master-Slave Flip-Flop (Exact DIP-14 pinout matching schematic standard)
    val left7472 = listOf(
        DipPinDef(1, "nc", "NC", PinDirection.INPUT),
        DipPinDef(2, "clr", "CLR'", PinDirection.INPUT, activeLow = true),
        DipPinDef(3, "j1", "J1", PinDirection.INPUT),
        DipPinDef(4, "j2", "J2", PinDirection.INPUT),
        DipPinDef(5, "j3", "J3", PinDirection.INPUT),
        DipPinDef(6, "qn", "Q'", PinDirection.OUTPUT, activeLow = true),
        DipPinDef(7, "gnd", "GND", PinDirection.GROUND)
    )
    val right7472 = listOf(
        DipPinDef(14, "vcc", "VCC", PinDirection.POWER),
        DipPinDef(13, "pre", "PRE'", PinDirection.INPUT, activeLow = true),
        DipPinDef(12, "clk", "CLK", PinDirection.INPUT, isClock = true),
        DipPinDef(11, "k3", "K3", PinDirection.INPUT),
        DipPinDef(10, "k2", "K2", PinDirection.INPUT),
        DipPinDef(9, "k1", "K1", PinDirection.INPUT),
        DipPinDef(8, "q", "Q", PinDirection.OUTPUT)
    )

    // 7473 Dual JK Flip-Flop with Clear (DIP-14)
    val left7473 = listOf(
        DipPinDef(1, "1clk", "1CLK", PinDirection.INPUT, isClock = true),
        DipPinDef(2, "1clr", "1CLR'", PinDirection.INPUT, activeLow = true),
        DipPinDef(3, "1k", "1K", PinDirection.INPUT),
        DipPinDef(4, "vcc", "VCC", PinDirection.POWER),
        DipPinDef(5, "2clk", "2CLK", PinDirection.INPUT, isClock = true),
        DipPinDef(6, "2clr", "2CLR'", PinDirection.INPUT, activeLow = true),
        DipPinDef(7, "2j", "2J", PinDirection.INPUT)
    )
    val right7473 = listOf(
        DipPinDef(14, "1j", "1J", PinDirection.INPUT),
        DipPinDef(13, "1qn", "1Q'", PinDirection.OUTPUT, activeLow = true),
        DipPinDef(12, "1q", "1Q", PinDirection.OUTPUT),
        DipPinDef(11, "gnd", "GND", PinDirection.GROUND),
        DipPinDef(10, "2k", "2K", PinDirection.INPUT),
        DipPinDef(9, "2q", "2Q", PinDirection.OUTPUT),
        DipPinDef(8, "2qn", "2Q'", PinDirection.OUTPUT, activeLow = true)
    )

    // 7474 Dual D Flip-Flop (DIP-14; preserves pin IDs d/clk/pre/clr/q/qn for FF1)
    val left7474 = listOf(
        DipPinDef(1, "clr", "1CLR'", PinDirection.INPUT, activeLow = true),
        DipPinDef(2, "d", "1D", PinDirection.INPUT),
        DipPinDef(3, "clk", "1CLK", PinDirection.INPUT, isClock = true),
        DipPinDef(4, "pre", "1PRE'", PinDirection.INPUT, activeLow = true),
        DipPinDef(5, "q", "1Q", PinDirection.OUTPUT),
        DipPinDef(6, "qn", "1Q'", PinDirection.OUTPUT, activeLow = true),
        DipPinDef(7, "gnd", "GND", PinDirection.GROUND)
    )
    val right7474 = listOf(
        DipPinDef(14, "vcc", "VCC", PinDirection.POWER),
        DipPinDef(13, "2clr", "2CLR'", PinDirection.INPUT, activeLow = true),
        DipPinDef(12, "2d", "2D", PinDirection.INPUT),
        DipPinDef(11, "2clk", "2CLK", PinDirection.INPUT, isClock = true),
        DipPinDef(10, "2pre", "2PRE'", PinDirection.INPUT, activeLow = true),
        DipPinDef(9, "2q", "2Q", PinDirection.OUTPUT),
        DipPinDef(8, "2qn", "2Q'", PinDirection.OUTPUT, activeLow = true)
    )

    // 7476 Dual JK Flip-Flop (DIP-14 layout; preserves pin IDs j/k/clk/pre/clr/q/qn for FF1)
    val left7476 = listOf(
        DipPinDef(1, "clk", "1CLK", PinDirection.INPUT, isClock = true),
        DipPinDef(2, "pre", "1PRE'", PinDirection.INPUT, activeLow = true),
        DipPinDef(3, "clr", "1CLR'", PinDirection.INPUT, activeLow = true),
        DipPinDef(4, "j", "1J", PinDirection.INPUT),
        DipPinDef(5, "k", "1K", PinDirection.INPUT),
        DipPinDef(6, "qn", "1Q'", PinDirection.OUTPUT, activeLow = true),
        DipPinDef(7, "gnd", "GND", PinDirection.GROUND)
    )
    val right7476 = listOf(
        DipPinDef(14, "vcc", "VCC", PinDirection.POWER),
        DipPinDef(13, "2pre", "2PRE'", PinDirection.INPUT, activeLow = true),
        DipPinDef(12, "2clk", "2CLK", PinDirection.INPUT, isClock = true),
        DipPinDef(11, "2j", "2J", PinDirection.INPUT),
        DipPinDef(10, "2k", "2K", PinDirection.INPUT),
        DipPinDef(9, "2q", "2Q", PinDirection.OUTPUT),
        DipPinDef(8, "q", "1Q", PinDirection.OUTPUT)
    )

    // 7490 Decade BCD Counter (DIP-14)
    val left7490 = listOf(
        DipPinDef(1, "clkb", "CLKB", PinDirection.INPUT, isClock = true),
        DipPinDef(2, "r01", "R0(1)", PinDirection.INPUT),
        DipPinDef(3, "r02", "R0(2)", PinDirection.INPUT),
        DipPinDef(4, "nc1", "NC", PinDirection.INPUT),
        DipPinDef(5, "vcc", "VCC", PinDirection.POWER),
        DipPinDef(6, "r91", "R9(1)", PinDirection.INPUT),
        DipPinDef(7, "r92", "R9(2)", PinDirection.INPUT)
    )
    val right7490 = listOf(
        DipPinDef(14, "clka", "CLKA", PinDirection.INPUT, isClock = true),
        DipPinDef(13, "nc2", "NC", PinDirection.INPUT),
        DipPinDef(12, "qa", "QA", PinDirection.OUTPUT),
        DipPinDef(11, "qd", "QD", PinDirection.OUTPUT),
        DipPinDef(10, "gnd", "GND", PinDirection.GROUND),
        DipPinDef(9, "qb", "QB", PinDirection.OUTPUT),
        DipPinDef(8, "qc", "QC", PinDirection.OUTPUT)
    )

    // 7493 4-Bit Binary Counter (DIP-14)
    val left7493 = listOf(
        DipPinDef(1, "clkb", "CLKB", PinDirection.INPUT, isClock = true),
        DipPinDef(2, "r01", "R0(1)", PinDirection.INPUT),
        DipPinDef(3, "r02", "R0(2)", PinDirection.INPUT),
        DipPinDef(4, "nc1", "NC", PinDirection.INPUT),
        DipPinDef(5, "vcc", "VCC", PinDirection.POWER),
        DipPinDef(6, "nc2", "NC", PinDirection.INPUT),
        DipPinDef(7, "nc3", "NC", PinDirection.INPUT)
    )
    val right7493 = listOf(
        DipPinDef(14, "clka", "CLKA", PinDirection.INPUT, isClock = true),
        DipPinDef(13, "nc4", "NC", PinDirection.INPUT),
        DipPinDef(12, "qa", "QA", PinDirection.OUTPUT),
        DipPinDef(11, "qd", "QD", PinDirection.OUTPUT),
        DipPinDef(10, "gnd", "GND", PinDirection.GROUND),
        DipPinDef(9, "qb", "QB", PinDirection.OUTPUT),
        DipPinDef(8, "qc", "QC", PinDirection.OUTPUT)
    )

    // 74125 Quad Tri-State Bus Buffer (DIP-14, Active-Low OE')
    val left74125 = listOf(
        DipPinDef(1, "1oe", "1OE'", PinDirection.INPUT, activeLow = true),
        DipPinDef(2, "1a", "1A", PinDirection.INPUT),
        DipPinDef(3, "1y", "1Y", PinDirection.OUTPUT),
        DipPinDef(4, "2oe", "2OE'", PinDirection.INPUT, activeLow = true),
        DipPinDef(5, "2a", "2A", PinDirection.INPUT),
        DipPinDef(6, "2y", "2Y", PinDirection.OUTPUT),
        DipPinDef(7, "gnd", "GND", PinDirection.GROUND)
    )
    val right74125 = listOf(
        DipPinDef(14, "vcc", "VCC", PinDirection.POWER),
        DipPinDef(13, "4oe", "4OE'", PinDirection.INPUT, activeLow = true),
        DipPinDef(12, "4a", "4A", PinDirection.INPUT),
        DipPinDef(11, "4y", "4Y", PinDirection.OUTPUT),
        DipPinDef(10, "3oe", "3OE'", PinDirection.INPUT, activeLow = true),
        DipPinDef(9, "3a", "3A", PinDirection.INPUT),
        DipPinDef(8, "3y", "3Y", PinDirection.OUTPUT)
    )

    return listOf(
        // C. Digital Logic Gates
        ComponentMetadata(
            type = ComponentType.GATE_AND,
            displayName = "AND Gate",
            shortCode = "AND",
            category = ComponentCategory.LOGIC_GATES,
            widthGrid = 4f,
            heightGrid = 3f,
            pins = listOf(
                PinSpec("a", "A", "A", PinDirection.INPUT, -2f, -1f),
                PinSpec("b", "B", "B", PinDirection.INPUT, -2f, 1f),
                PinSpec("y", "Y", "Y", PinDirection.OUTPUT, 2f, 0f)
            ),
            defaultValue = "2-Input",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "Outputs Logic 1 only when all inputs are Logic 1 (Y = A · B).",
            datasheetSummary = "Boolean AND: 0·0=0, 0·1=0, 1·0=0, 1·1=1."
        ),
        ComponentMetadata(
            type = ComponentType.GATE_OR,
            displayName = "OR Gate",
            shortCode = "OR",
            category = ComponentCategory.LOGIC_GATES,
            widthGrid = 4f,
            heightGrid = 3f,
            pins = listOf(
                PinSpec("a", "A", "A", PinDirection.INPUT, -2f, -1f),
                PinSpec("b", "B", "B", PinDirection.INPUT, -2f, 1f),
                PinSpec("y", "Y", "Y", PinDirection.OUTPUT, 2f, 0f)
            ),
            defaultValue = "2-Input",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "Outputs Logic 1 when at least one input is Logic 1 (Y = A + B).",
            datasheetSummary = "Boolean OR: 0+0=0, 0+1=1, 1+0=1, 1+1=1."
        ),
        ComponentMetadata(
            type = ComponentType.GATE_NOT,
            displayName = "NOT Gate (Inverter)",
            shortCode = "NOT",
            category = ComponentCategory.LOGIC_GATES,
            widthGrid = 4f,
            heightGrid = 2f,
            pins = listOf(
                PinSpec("a", "A", "A", PinDirection.INPUT, -2f, 0f),
                PinSpec("y", "Y", "Y", PinDirection.OUTPUT, 2f, 0f)
            ),
            defaultValue = "Inverter",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "Inverts the digital input signal (Y = A').",
            datasheetSummary = "Boolean NOT: 0->1, 1->0."
        ),
        ComponentMetadata(
            type = ComponentType.GATE_NAND,
            displayName = "NAND Gate",
            shortCode = "NAND",
            category = ComponentCategory.LOGIC_GATES,
            widthGrid = 4f,
            heightGrid = 3f,
            pins = listOf(
                PinSpec("a", "A", "A", PinDirection.INPUT, -2f, -1f),
                PinSpec("b", "B", "B", PinDirection.INPUT, -2f, 1f),
                PinSpec("y", "Y", "Y", PinDirection.OUTPUT, 2f, 0f)
            ),
            defaultValue = "2-Input",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "Universal logic gate outputting 0 only when both inputs are 1 (Y = (A·B)').",
            datasheetSummary = "Boolean NAND: 00->1, 01->1, 10->1, 11->0."
        ),
        ComponentMetadata(
            type = ComponentType.GATE_NOR,
            displayName = "NOR Gate",
            shortCode = "NOR",
            category = ComponentCategory.LOGIC_GATES,
            widthGrid = 4f,
            heightGrid = 3f,
            pins = listOf(
                PinSpec("a", "A", "A", PinDirection.INPUT, -2f, -1f),
                PinSpec("b", "B", "B", PinDirection.INPUT, -2f, 1f),
                PinSpec("y", "Y", "Y", PinDirection.OUTPUT, 2f, 0f)
            ),
            defaultValue = "2-Input",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "Universal gate outputting 1 only when all inputs are 0 (Y = (A+B)').",
            datasheetSummary = "Boolean NOR: 00->1, 01->0, 10->0, 11->0."
        ),
        ComponentMetadata(
            type = ComponentType.GATE_XOR,
            displayName = "XOR Gate",
            shortCode = "XOR",
            category = ComponentCategory.LOGIC_GATES,
            widthGrid = 4f,
            heightGrid = 3f,
            pins = listOf(
                PinSpec("a", "A", "A", PinDirection.INPUT, -2f, -1f),
                PinSpec("b", "B", "B", PinDirection.INPUT, -2f, 1f),
                PinSpec("y", "Y", "Y", PinDirection.OUTPUT, 2f, 0f)
            ),
            defaultValue = "2-Input",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "Outputs Logic 1 when inputs differ (Y = A ⊕ B).",
            datasheetSummary = "Boolean XOR: 00->0, 01->1, 10->1, 11->0."
        ),
        ComponentMetadata(
            type = ComponentType.GATE_XNOR,
            displayName = "XNOR Gate",
            shortCode = "XNOR",
            category = ComponentCategory.LOGIC_GATES,
            widthGrid = 4f,
            heightGrid = 3f,
            pins = listOf(
                PinSpec("a", "A", "A", PinDirection.INPUT, -2f, -1f),
                PinSpec("b", "B", "B", PinDirection.INPUT, -2f, 1f),
                PinSpec("y", "Y", "Y", PinDirection.OUTPUT, 2f, 0f)
            ),
            defaultValue = "2-Input",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "Equality comparator outputting Logic 1 when inputs match.",
            datasheetSummary = "Boolean XNOR: 00->1, 01->0, 10->0, 11->1."
        ),
        ComponentMetadata(
            type = ComponentType.GATE_BUFFER,
            displayName = "Buffer",
            shortCode = "BUF",
            category = ComponentCategory.LOGIC_GATES,
            widthGrid = 4f,
            heightGrid = 2f,
            pins = listOf(
                PinSpec("a", "A", "A", PinDirection.INPUT, -2f, 0f),
                PinSpec("y", "Y", "Y", PinDirection.OUTPUT, 2f, 0f)
            ),
            defaultValue = "1-Bit",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "Non-inverting digital signal driver / delay buffer.",
            datasheetSummary = "Y = A with 1 propagation step delay."
        ),
        ComponentMetadata(
            type = ComponentType.GATE_TRISTATE,
            displayName = "Tri-State Buffer",
            shortCode = "TRI",
            category = ComponentCategory.LOGIC_GATES,
            widthGrid = 4f,
            heightGrid = 3f,
            pins = listOf(
                PinSpec("a", "A", "A", PinDirection.INPUT, -2f, 0f),
                PinSpec("en", "EN", "EN", PinDirection.INPUT, 0f, -1.5f),
                PinSpec("y", "Y", "Y", PinDirection.OUTPUT, 2f, 0f)
            ),
            defaultValue = "Active-High EN",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "Bus driver outputting A when EN=1, or High-Impedance (Z) when EN=0.",
            datasheetSummary = "If EN=1 then Y=A else Y=Z (High-Z)."
        ),

        // D. 7400 Series 14-Pin DIP IC Library (Authentic Numbered Pinouts)
        ComponentMetadata(
            type = ComponentType.IC_7472,
            displayName = "7472 JK Master-Slave FF",
            shortCode = "7472",
            category = ComponentCategory.IC_7400,
            widthGrid = 8f,
            heightGrid = 8f,
            pins = dip14Pins(left7472, right7472),
            defaultValue = "7472",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "14-pin DIP JK Master-Slave Flip-Flop with 3-input ANDed J (J1·J2·J3) & K (K1·K2·K3), active-low PRE' & CLR'.",
            datasheetSummary = "J = J1·J2·J3, K = K1·K2·K3 (unconnected J/K inputs pull HIGH). Active-low PRE'/CLR', edge-triggered CLK.",
            pinoutTable = dip14PinoutTable(left7472, right7472)
        ),
        buildQuad2InDip14(
            type = ComponentType.IC_7400,
            displayName = "7400 Quad 2-In NAND",
            shortCode = "7400",
            defaultValue = "7400",
            description = "14-pin DIP Quad 2-input positive NAND gates.",
            datasheetSummary = "Four independent 2-input NAND gates: 1Y=(1A·1B)', 2Y=(2A·2B)', 3Y=(3A·3B)', 4Y=(4A·4B)'.",
            invertedOutput = true
        ),
        ComponentMetadata(
            type = ComponentType.IC_7402,
            displayName = "7402 Quad 2-In NOR",
            shortCode = "7402",
            category = ComponentCategory.IC_7400,
            widthGrid = 8f,
            heightGrid = 8f,
            pins = dip14Pins(left7402, right7402),
            defaultValue = "7402",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "14-pin DIP Quad 2-input positive NOR gates.",
            datasheetSummary = "Four independent 2-input NOR gates: 1Y=(1A+1B)', 2Y=(2A+2B)', 3Y=(3A+3B)', 4Y=(4A+4B)'.",
            pinoutTable = dip14PinoutTable(left7402, right7402)
        ),
        ComponentMetadata(
            type = ComponentType.IC_7404,
            displayName = "7404 Hex Inverter",
            shortCode = "7404",
            category = ComponentCategory.IC_7400,
            widthGrid = 8f,
            heightGrid = 8f,
            pins = dip14Pins(left7404, right7404),
            defaultValue = "7404",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "14-pin DIP Hex Inverter containing six independent NOT gates.",
            datasheetSummary = "Six independent inverters: 1Y=1A' through 6Y=6A'.",
            pinoutTable = dip14PinoutTable(left7404, right7404)
        ),
        buildQuad2InDip14(
            type = ComponentType.IC_7408,
            displayName = "7408 Quad 2-In AND",
            shortCode = "7408",
            defaultValue = "7408",
            description = "14-pin DIP Quad 2-input positive AND gates.",
            datasheetSummary = "Four independent 2-input AND gates: 1Y=1A·1B .. 4Y=4A·4B."
        ),
        buildTriple3InDip14(
            type = ComponentType.IC_7410,
            displayName = "7410 Triple 3-In NAND",
            shortCode = "7410",
            defaultValue = "7410",
            description = "14-pin DIP Triple 3-input positive NAND gates.",
            datasheetSummary = "Three independent 3-input NAND gates: Y = (A·B·C)'.",
            invertedOutput = true
        ),
        buildTriple3InDip14(
            type = ComponentType.IC_7411,
            displayName = "7411 Triple 3-In AND",
            shortCode = "7411",
            defaultValue = "7411",
            description = "14-pin DIP Triple 3-input positive AND gates.",
            datasheetSummary = "Three independent 3-input AND gates: Y = A·B·C."
        ),
        buildDual4InDip14(
            type = ComponentType.IC_7420,
            displayName = "7420 Dual 4-In NAND",
            shortCode = "7420",
            defaultValue = "7420",
            description = "14-pin DIP Dual 4-input positive NAND gates.",
            datasheetSummary = "Two independent 4-input NAND gates: 1Y=(1A·1B·1C·1D)', 2Y=(2A·2B·2C·2D)'.",
            invertedOutput = true
        ),
        buildDual4InDip14(
            type = ComponentType.IC_7421,
            displayName = "7421 Dual 4-In AND",
            shortCode = "7421",
            defaultValue = "7421",
            description = "14-pin DIP Dual 4-input positive AND gates.",
            datasheetSummary = "Two independent 4-input AND gates: 1Y=1A·1B·1C·1D, 2Y=2A·2B·2C·2D."
        ),
        buildTriple3InDip14(
            type = ComponentType.IC_7427,
            displayName = "7427 Triple 3-In NOR",
            shortCode = "7427",
            defaultValue = "7427",
            description = "14-pin DIP Triple 3-input positive NOR gates.",
            datasheetSummary = "Three independent 3-input NOR gates: Y = (A+B+C)'.",
            invertedOutput = true
        ),
        ComponentMetadata(
            type = ComponentType.IC_7430,
            displayName = "7430 8-Input NAND",
            shortCode = "7430",
            category = ComponentCategory.IC_7400,
            widthGrid = 8f,
            heightGrid = 8f,
            pins = dip14Pins(left7430, right7430),
            defaultValue = "7430",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "14-pin DIP Single 8-input positive NAND gate.",
            datasheetSummary = "Y = (A·B·C·D·E·F·G·H)'. Outputs 0 only when all 8 inputs are 1.",
            pinoutTable = dip14PinoutTable(left7430, right7430)
        ),
        buildQuad2InDip14(
            type = ComponentType.IC_7432,
            displayName = "7432 Quad 2-In OR",
            shortCode = "7432",
            defaultValue = "7432",
            description = "14-pin DIP Quad 2-input positive OR gates.",
            datasheetSummary = "Four independent 2-input OR gates: 1Y=1A+1B .. 4Y=4A+4B."
        ),
        ComponentMetadata(
            type = ComponentType.IC_7473,
            displayName = "7473 Dual JK Flip-Flop",
            shortCode = "7473",
            category = ComponentCategory.IC_7400,
            widthGrid = 8f,
            heightGrid = 8f,
            pins = dip14Pins(left7473, right7473),
            defaultValue = "7473",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "14-pin DIP Dual JK Flip-Flop with individual active-low Clear (1CLR', 2CLR').",
            datasheetSummary = "Two independent JK Flip-Flops (VCC pin 4, GND pin 11). 00=Hold, 01=Reset, 10=Set, 11=Toggle.",
            pinoutTable = dip14PinoutTable(left7473, right7473)
        ),
        ComponentMetadata(
            type = ComponentType.IC_7474,
            displayName = "7474 Dual D Flip-Flop",
            shortCode = "7474",
            category = ComponentCategory.IC_7400,
            widthGrid = 8f,
            heightGrid = 8f,
            pins = dip14Pins(left7474, right7474),
            defaultValue = "7474",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "14-pin DIP Dual positive-edge-triggered D Flip-Flop with active-low Preset & Clear.",
            datasheetSummary = "Two independent D Flip-Flops. Captures D on rising CLK edge; PRE'=0 sets Q=1, CLR'=0 resets Q=0.",
            pinoutTable = dip14PinoutTable(left7474, right7474)
        ),
        ComponentMetadata(
            type = ComponentType.IC_7476,
            displayName = "7476 Dual JK Flip-Flop",
            shortCode = "7476",
            category = ComponentCategory.IC_7400,
            widthGrid = 8f,
            heightGrid = 8f,
            pins = dip14Pins(left7476, right7476),
            defaultValue = "7476",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "14-pin DIP Dual JK Flip-Flop with Preset, Clear, and J=K=1 toggle mode.",
            datasheetSummary = "On clock trigger: 00=Hold, 01=Reset, 10=Set, 11=Toggle.",
            pinoutTable = dip14PinoutTable(left7476, right7476)
        ),
        buildQuad2InDip14(
            type = ComponentType.IC_7486,
            displayName = "7486 Quad 2-In XOR",
            shortCode = "7486",
            defaultValue = "7486",
            description = "14-pin DIP Quad 2-input Exclusive-OR gates.",
            datasheetSummary = "Four independent 2-input XOR gates ideal for adders & parity checkers."
        ),
        ComponentMetadata(
            type = ComponentType.IC_7490,
            displayName = "7490 Decade BCD Counter",
            shortCode = "7490",
            category = ComponentCategory.IC_7400,
            widthGrid = 8f,
            heightGrid = 8f,
            pins = dip14Pins(left7490, right7490),
            defaultValue = "7490",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "14-pin DIP 4-Bit Decade (BCD 0–9) Ripple Counter with R0 Reset & R9 Set-to-9.",
            datasheetSummary = "Divide-by-2 (CLKA->QA) + Divide-by-5 (CLKB->QB,QC,QD). Wire QA->CLKB for 0..9 BCD counting.",
            pinoutTable = dip14PinoutTable(left7490, right7490)
        ),
        ComponentMetadata(
            type = ComponentType.IC_7493,
            displayName = "7493 4-Bit Binary Counter",
            shortCode = "7493",
            category = ComponentCategory.IC_7400,
            widthGrid = 8f,
            heightGrid = 8f,
            pins = dip14Pins(left7493, right7493),
            defaultValue = "7493",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "14-pin DIP 4-Bit Binary (0–15) Ripple Counter with R0(1)·R0(2) Master Reset.",
            datasheetSummary = "Divide-by-2 (CLKA->QA) + Divide-by-8 (CLKB->QB,QC,QD). Wire QA->CLKB for 0..15 binary counting.",
            pinoutTable = dip14PinoutTable(left7493, right7493)
        ),
        ComponentMetadata(
            type = ComponentType.IC_74125,
            displayName = "74125 Quad Tri-State Buf",
            shortCode = "74125",
            category = ComponentCategory.IC_7400,
            widthGrid = 8f,
            heightGrid = 8f,
            pins = dip14Pins(left74125, right74125),
            defaultValue = "74125",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "14-pin DIP Quad Bus Buffer Gates with active-low 3-State Output Enables (OE').",
            datasheetSummary = "When nOE'=0, nY=nA; when nOE'=1, nY=High-Z (Z).",
            pinoutTable = dip14PinoutTable(left74125, right74125)
        ),
        buildQuad2InDip14(
            type = ComponentType.IC_74266,
            displayName = "74266 Quad 2-In XNOR",
            shortCode = "74266",
            defaultValue = "74266",
            description = "14-pin DIP Quad 2-input Exclusive-NOR (equality) gates.",
            datasheetSummary = "Four independent 2-input XNOR gates: Y = (A ⊕ B)'.",
            invertedOutput = true
        ),
        ComponentMetadata(
            type = ComponentType.IC_74138,
            displayName = "74138 3-to-8 Decoder",
            shortCode = "74138",
            category = ComponentCategory.IC_7400,
            widthGrid = 8f,
            heightGrid = 9f,
            pins = listOf(
                PinSpec("a0", "A0", "A0", PinDirection.INPUT, -4f, -3.5f, pinNumber = 1),
                PinSpec("a1", "A1", "A1", PinDirection.INPUT, -4f, -2.5f, pinNumber = 2),
                PinSpec("a2", "A2", "A2", PinDirection.INPUT, -4f, -1.5f, pinNumber = 3),
                PinSpec("e1", "E1", "E1", PinDirection.INPUT, -4f, -0.5f, pinNumber = 6),
                PinSpec("y7", "Y7'", "Y7'", PinDirection.OUTPUT, -4f, 1.5f, pinNumber = 7, activeLow = true),
                PinSpec("gnd", "GND", "GND", PinDirection.GROUND, -4f, 3.5f, pinNumber = 8),
                PinSpec("vcc", "VCC", "VCC", PinDirection.POWER, 4f, -3.5f, pinNumber = 16),
                PinSpec("y0", "Y0'", "Y0'", PinDirection.OUTPUT, 4f, -2.5f, pinNumber = 15, activeLow = true),
                PinSpec("y1", "Y1'", "Y1'", PinDirection.OUTPUT, 4f, -1.5f, pinNumber = 14, activeLow = true),
                PinSpec("y2", "Y2'", "Y2'", PinDirection.OUTPUT, 4f, -0.5f, pinNumber = 13, activeLow = true),
                PinSpec("y3", "Y3'", "Y3'", PinDirection.OUTPUT, 4f, 0.5f, pinNumber = 12, activeLow = true),
                PinSpec("y4", "Y4'", "Y4'", PinDirection.OUTPUT, 4f, 1.5f, pinNumber = 11, activeLow = true),
                PinSpec("y5", "Y5'", "Y5'", PinDirection.OUTPUT, 4f, 2.5f, pinNumber = 10, activeLow = true),
                PinSpec("y6", "Y6'", "Y6'", PinDirection.OUTPUT, 4f, 3.5f, pinNumber = 9, activeLow = true)
            ),
            defaultValue = "74138",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "3-line to 8-line decoder/demultiplexer with active-low outputs.",
            datasheetSummary = "When enabled (E1=1 or unconnected), selected output Yi' goes LOW (0), others HIGH (1).",
            pinoutTable = listOf(
                "1..3: A0..A2" to "15..9: Y0'..Y6'",
                "6: E1, 7: Y7', 8: GND" to "16: VCC"
            )
        ),
        ComponentMetadata(
            type = ComponentType.IC_74151,
            displayName = "74151 8-to-1 MUX",
            shortCode = "74151",
            category = ComponentCategory.IC_7400,
            widthGrid = 8f,
            heightGrid = 8f,
            pins = listOf(
                PinSpec("d3", "D3", "D3", PinDirection.INPUT, -4f, -3f, pinNumber = 1),
                PinSpec("d2", "D2", "D2", PinDirection.INPUT, -4f, -2f, pinNumber = 2),
                PinSpec("d1", "D1", "D1", PinDirection.INPUT, -4f, -1f, pinNumber = 3),
                PinSpec("d0", "D0", "D0", PinDirection.INPUT, -4f, 0f, pinNumber = 4),
                PinSpec("y", "Y", "Y", PinDirection.OUTPUT, -4f, 1f, pinNumber = 5),
                PinSpec("wn", "W'", "W'", PinDirection.OUTPUT, -4f, 2f, pinNumber = 6, activeLow = true),
                PinSpec("gnd", "GND", "GND", PinDirection.GROUND, -4f, 3f, pinNumber = 8),
                PinSpec("vcc", "VCC", "VCC", PinDirection.POWER, 4f, -3f, pinNumber = 16),
                PinSpec("s0", "S0", "S0", PinDirection.INPUT, 4f, -1f, pinNumber = 11),
                PinSpec("s1", "S1", "S1", PinDirection.INPUT, 4f, 0f, pinNumber = 10),
                PinSpec("s2", "S2", "S2", PinDirection.INPUT, 4f, 1f, pinNumber = 9)
            ),
            defaultValue = "74151",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "8-input digital multiplexer selecting Di onto Y via 3-bit address S2:S0.",
            datasheetSummary = "Y = D[S], W' = Y'.",
            pinoutTable = listOf(
                "1..4: D3..D0" to "16: VCC",
                "5: Y, 6: W', 8: GND" to "11..9: S0..S2"
            )
        ),
        ComponentMetadata(
            type = ComponentType.IC_7447,
            displayName = "7447 BCD-to-7-Seg Decoder",
            shortCode = "7447",
            category = ComponentCategory.IC_7400,
            widthGrid = 8f,
            heightGrid = 9f,
            pins = listOf(
                PinSpec("b", "B", "B", PinDirection.INPUT, -4f, -3.5f, pinNumber = 1),
                PinSpec("c", "C", "C", PinDirection.INPUT, -4f, -2.5f, pinNumber = 2),
                PinSpec("lt", "LT'", "LT'", PinDirection.INPUT, -4f, -1.5f, pinNumber = 3, activeLow = true),
                PinSpec("bi", "BI'", "BI'", PinDirection.INPUT, -4f, -0.5f, pinNumber = 4, activeLow = true),
                PinSpec("rbi", "RBI'", "RBI'", PinDirection.INPUT, -4f, 0.5f, pinNumber = 5, activeLow = true),
                PinSpec("d", "D", "D", PinDirection.INPUT, -4f, 1.5f, pinNumber = 6),
                PinSpec("a", "A", "A", PinDirection.INPUT, -4f, 2.5f, pinNumber = 7),
                PinSpec("gnd", "GND", "GND", PinDirection.GROUND, -4f, 3.5f, pinNumber = 8),
                PinSpec("vcc", "VCC", "VCC", PinDirection.POWER, 4f, -3.5f, pinNumber = 16),
                PinSpec("sf", "f'", "f'", PinDirection.OUTPUT, 4f, -2.5f, pinNumber = 15, activeLow = true),
                PinSpec("sg", "g'", "g'", PinDirection.OUTPUT, 4f, -1.5f, pinNumber = 14, activeLow = true),
                PinSpec("sa", "a'", "a'", PinDirection.OUTPUT, 4f, -0.5f, pinNumber = 13, activeLow = true),
                PinSpec("sb", "b'", "b'", PinDirection.OUTPUT, 4f, 0.5f, pinNumber = 12, activeLow = true),
                PinSpec("sc", "c'", "c'", PinDirection.OUTPUT, 4f, 1.5f, pinNumber = 11, activeLow = true),
                PinSpec("sd", "d'", "d'", PinDirection.OUTPUT, 4f, 2.5f, pinNumber = 10, activeLow = true),
                PinSpec("se", "e'", "e'", PinDirection.OUTPUT, 4f, 3.5f, pinNumber = 9, activeLow = true)
            ),
            defaultValue = "7447",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "16-pin DIP BCD-to-7-Segment Decoder/Driver with Lamp Test (LT'), Blanking (BI'), and active-low segment outputs.",
            datasheetSummary = "Decodes 4-bit BCD (D C B A) to active-low segments a'–g'. LT'=0 turns all segments ON; BI'=0 blanks display.",
            pinoutTable = listOf(
                "1: B, 2: C, 6: D, 7: A" to "16: VCC, 8: GND",
                "3: LT', 4: BI', 5: RBI'" to "13..9: a'..e', 15: f', 14: g'"
            )
        ),
        ComponentMetadata(
            type = ComponentType.IC_7483,
            displayName = "7483 4-Bit Binary Adder",
            shortCode = "7483",
            category = ComponentCategory.IC_7400,
            widthGrid = 8f,
            heightGrid = 9f,
            pins = listOf(
                PinSpec("a4", "A4", "A4", PinDirection.INPUT, -4f, -3.5f, pinNumber = 1),
                PinSpec("s3", "S3", "S3", PinDirection.OUTPUT, -4f, -2.5f, pinNumber = 2),
                PinSpec("a3", "A3", "A3", PinDirection.INPUT, -4f, -1.5f, pinNumber = 3),
                PinSpec("b3", "B3", "B3", PinDirection.INPUT, -4f, -0.5f, pinNumber = 4),
                PinSpec("vcc", "VCC", "VCC", PinDirection.POWER, -4f, 0.5f, pinNumber = 5),
                PinSpec("s2", "S2", "S2", PinDirection.OUTPUT, -4f, 1.5f, pinNumber = 6),
                PinSpec("b2", "B2", "B2", PinDirection.INPUT, -4f, 2.5f, pinNumber = 7),
                PinSpec("a2", "A2", "A2", PinDirection.INPUT, -4f, 3.5f, pinNumber = 8),
                PinSpec("b4", "B4", "B4", PinDirection.INPUT, 4f, -3.5f, pinNumber = 16),
                PinSpec("s4", "S4", "S4", PinDirection.OUTPUT, 4f, -2.5f, pinNumber = 15),
                PinSpec("c4", "C4", "C4", PinDirection.OUTPUT, 4f, -1.5f, pinNumber = 14),
                PinSpec("c0", "C0", "C0", PinDirection.INPUT, 4f, -0.5f, pinNumber = 13),
                PinSpec("gnd", "GND", "GND", PinDirection.GROUND, 4f, 0.5f, pinNumber = 12),
                PinSpec("b1", "B1", "B1", PinDirection.INPUT, 4f, 1.5f, pinNumber = 11),
                PinSpec("a1", "A1", "A1", PinDirection.INPUT, 4f, 2.5f, pinNumber = 10),
                PinSpec("s1", "S1", "S1", PinDirection.OUTPUT, 4f, 3.5f, pinNumber = 9)
            ),
            defaultValue = "7483",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "16-pin DIP 4-Bit Binary Full Adder with fast carry-lookahead (A4..A1 + B4..B1 + C0 → C4, S4..S1).",
            datasheetSummary = "Computes 5-bit binary sum: {C4, S4, S3, S2, S1} = A[4:1] + B[4:1] + C0.",
            pinoutTable = listOf(
                "10,8,3,1: A1..A4" to "11,7,4,16: B1..B4",
                "9,6,2,15: S1..S4" to "13: C0 (Cin), 14: C4 (Cout)",
                "5: VCC" to "12: GND"
            )
        ),
        ComponentMetadata(
            type = ComponentType.IC_74194,
            displayName = "74194 4-Bit Shift Register",
            shortCode = "74194",
            category = ComponentCategory.IC_7400,
            widthGrid = 8f,
            heightGrid = 9f,
            pins = listOf(
                PinSpec("clr", "CLR'", "CLR'", PinDirection.INPUT, -4f, -3.5f, pinNumber = 1, activeLow = true),
                PinSpec("sr", "SR", "SR", PinDirection.INPUT, -4f, -2.5f, pinNumber = 2),
                PinSpec("a", "A", "A", PinDirection.INPUT, -4f, -1.5f, pinNumber = 3),
                PinSpec("b", "B", "B", PinDirection.INPUT, -4f, -0.5f, pinNumber = 4),
                PinSpec("c", "C", "C", PinDirection.INPUT, -4f, 0.5f, pinNumber = 5),
                PinSpec("d", "D", "D", PinDirection.INPUT, -4f, 1.5f, pinNumber = 6),
                PinSpec("sl", "SL", "SL", PinDirection.INPUT, -4f, 2.5f, pinNumber = 7),
                PinSpec("gnd", "GND", "GND", PinDirection.GROUND, -4f, 3.5f, pinNumber = 8),
                PinSpec("vcc", "VCC", "VCC", PinDirection.POWER, 4f, -3.5f, pinNumber = 16),
                PinSpec("qa", "QA", "QA", PinDirection.OUTPUT, 4f, -2.5f, pinNumber = 15),
                PinSpec("qb", "QB", "QB", PinDirection.OUTPUT, 4f, -1.5f, pinNumber = 14),
                PinSpec("qc", "QC", "QC", PinDirection.OUTPUT, 4f, -0.5f, pinNumber = 13),
                PinSpec("qd", "QD", "QD", PinDirection.OUTPUT, 4f, 0.5f, pinNumber = 12),
                PinSpec("clk", "CLK", "CLK", PinDirection.INPUT, 4f, 1.5f, pinNumber = 11, isClock = true),
                PinSpec("s1", "S1", "S1", PinDirection.INPUT, 4f, 2.5f, pinNumber = 10),
                PinSpec("s0", "S0", "S0", PinDirection.INPUT, 4f, 3.5f, pinNumber = 9)
            ),
            defaultValue = "74194",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "16-pin DIP 4-Bit Bidirectional Universal Shift Register supporting Hold, Shift Right, Shift Left, and Parallel Load.",
            datasheetSummary = "Mode S1:S0 on rising CLK: 00=Hold, 01=Shift Right (SR→QA..QD), 10=Shift Left (SL→QD..QA), 11=Parallel Load (A..D). CLR'=0 resets all Q=0.",
            pinoutTable = listOf(
                "1: CLR', 2: SR, 7: SL" to "16: VCC, 8: GND",
                "3..6: Parallel In A..D" to "15..12: QA..QD",
                "11: CLK (Rising)" to "10: S1, 9: S0"
            )
        ),
        ComponentMetadata(
            type = ComponentType.IC_7485,
            displayName = "7485 4-Bit Magnitude Comparator",
            shortCode = "7485",
            category = ComponentCategory.IC_7400,
            widthGrid = 8f,
            heightGrid = 9f,
            pins = listOf(
                PinSpec("b3", "B3", "B3", PinDirection.INPUT, -4f, -3.5f, pinNumber = 1),
                PinSpec("ilt", "I(A<B)", "I<", PinDirection.INPUT, -4f, -2.5f, pinNumber = 2),
                PinSpec("ieq", "I(A=B)", "I=", PinDirection.INPUT, -4f, -1.5f, pinNumber = 3),
                PinSpec("igt", "I(A>B)", "I>", PinDirection.INPUT, -4f, -0.5f, pinNumber = 4),
                PinSpec("ogt", "A>B", "A>B", PinDirection.OUTPUT, -4f, 0.5f, pinNumber = 5),
                PinSpec("oeq", "A=B", "A=B", PinDirection.OUTPUT, -4f, 1.5f, pinNumber = 6),
                PinSpec("olt", "A<B", "A<B", PinDirection.OUTPUT, -4f, 2.5f, pinNumber = 7),
                PinSpec("gnd", "GND", "GND", PinDirection.GROUND, -4f, 3.5f, pinNumber = 8),
                PinSpec("vcc", "VCC", "VCC", PinDirection.POWER, 4f, -3.5f, pinNumber = 16),
                PinSpec("a3", "A3", "A3", PinDirection.INPUT, 4f, -2.5f, pinNumber = 15),
                PinSpec("b2", "B2", "B2", PinDirection.INPUT, 4f, -1.5f, pinNumber = 14),
                PinSpec("a2", "A2", "A2", PinDirection.INPUT, 4f, -0.5f, pinNumber = 13),
                PinSpec("a1", "A1", "A1", PinDirection.INPUT, 4f, 0.5f, pinNumber = 12),
                PinSpec("b1", "B1", "B1", PinDirection.INPUT, 4f, 1.5f, pinNumber = 11),
                PinSpec("a0", "A0", "A0", PinDirection.INPUT, 4f, 2.5f, pinNumber = 10),
                PinSpec("b0", "B0", "B0", PinDirection.INPUT, 4f, 3.5f, pinNumber = 9)
            ),
            defaultValue = "7485",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "16-pin DIP 4-Bit Magnitude Comparator comparing unsigned words A[3:0] and B[3:0] with cascade inputs.",
            datasheetSummary = "Drives A>B, A=B, or A<B HIGH. Leave I(A=B) HIGH or floating for single-stage 4-bit comparison.",
            pinoutTable = listOf(
                "10,12,13,15: A0..A3" to "9,11,14,1: B0..B3",
                "2,3,4: I<, I=, I>" to "5: A>B, 6: A=B, 7: A<B",
                "16: VCC" to "8: GND"
            )
        ),
        ComponentMetadata(
            type = ComponentType.IC_74595,
            displayName = "74595 8-Bit Shift Register",
            shortCode = "74595",
            category = ComponentCategory.IC_7400,
            widthGrid = 8f,
            heightGrid = 9f,
            pins = listOf(
                PinSpec("qb", "QB", "QB", PinDirection.OUTPUT, -4f, -3.5f, pinNumber = 1),
                PinSpec("qc", "QC", "QC", PinDirection.OUTPUT, -4f, -2.5f, pinNumber = 2),
                PinSpec("qd", "QD", "QD", PinDirection.OUTPUT, -4f, -1.5f, pinNumber = 3),
                PinSpec("qe", "QE", "QE", PinDirection.OUTPUT, -4f, -0.5f, pinNumber = 4),
                PinSpec("qf", "QF", "QF", PinDirection.OUTPUT, -4f, 0.5f, pinNumber = 5),
                PinSpec("qg", "QG", "QG", PinDirection.OUTPUT, -4f, 1.5f, pinNumber = 6),
                PinSpec("qh", "QH", "QH", PinDirection.OUTPUT, -4f, 2.5f, pinNumber = 7),
                PinSpec("gnd", "GND", "GND", PinDirection.GROUND, -4f, 3.5f, pinNumber = 8),
                PinSpec("vcc", "VCC", "VCC", PinDirection.POWER, 4f, -3.5f, pinNumber = 16),
                PinSpec("qa", "QA", "QA", PinDirection.OUTPUT, 4f, -2.5f, pinNumber = 15),
                PinSpec("ser", "SER", "SER", PinDirection.INPUT, 4f, -1.5f, pinNumber = 14),
                PinSpec("oe", "OE'", "OE'", PinDirection.INPUT, 4f, -0.5f, pinNumber = 13, activeLow = true),
                PinSpec("rclk", "RCLK", "RCLK", PinDirection.INPUT, 4f, 0.5f, pinNumber = 12, isClock = true),
                PinSpec("srclk", "SRCLK", "SRCLK", PinDirection.INPUT, 4f, 1.5f, pinNumber = 11, isClock = true),
                PinSpec("srclr", "SRCLR'", "SRCLR'", PinDirection.INPUT, 4f, 2.5f, pinNumber = 10, activeLow = true),
                PinSpec("qhs", "QH'", "QH'", PinDirection.OUTPUT, 4f, 3.5f, pinNumber = 9)
            ),
            defaultValue = "74595",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "16-pin DIP 8-Bit Serial-In Parallel-Out (SIPO) Shift Register with Storage Latch and Tri-State Outputs.",
            datasheetSummary = "Rising SRCLK shifts SER into 8-bit register; rising RCLK latches shift register to QA..QH. OE'=1 sets High-Z.",
            pinoutTable = listOf(
                "14: SER, 11: SRCLK, 12: RCLK" to "10: SRCLR', 13: OE'",
                "15,1..7: QA..QH" to "9: QH' (Cascade), 16: VCC, 8: GND"
            )
        ),
        ComponentMetadata(
            type = ComponentType.IC_74161,
            displayName = "74161 4-Bit Sync Counter",
            shortCode = "74161",
            category = ComponentCategory.IC_7400,
            widthGrid = 8f,
            heightGrid = 9f,
            pins = listOf(
                PinSpec("clr", "CLR'", "CLR'", PinDirection.INPUT, -4f, -3.5f, pinNumber = 1, activeLow = true),
                PinSpec("clk", "CLK", "CLK", PinDirection.INPUT, -4f, -2.5f, pinNumber = 2, isClock = true),
                PinSpec("a", "A", "A", PinDirection.INPUT, -4f, -1.5f, pinNumber = 3),
                PinSpec("b", "B", "B", PinDirection.INPUT, -4f, -0.5f, pinNumber = 4),
                PinSpec("c", "C", "C", PinDirection.INPUT, -4f, 0.5f, pinNumber = 5),
                PinSpec("d", "D", "D", PinDirection.INPUT, -4f, 1.5f, pinNumber = 6),
                PinSpec("enp", "ENP", "ENP", PinDirection.INPUT, -4f, 2.5f, pinNumber = 7),
                PinSpec("gnd", "GND", "GND", PinDirection.GROUND, -4f, 3.5f, pinNumber = 8),
                PinSpec("vcc", "VCC", "VCC", PinDirection.POWER, 4f, -3.5f, pinNumber = 16),
                PinSpec("rco", "RCO", "RCO", PinDirection.OUTPUT, 4f, -2.5f, pinNumber = 15),
                PinSpec("qa", "QA", "QA", PinDirection.OUTPUT, 4f, -1.5f, pinNumber = 14),
                PinSpec("qb", "QB", "QB", PinDirection.OUTPUT, 4f, -0.5f, pinNumber = 13),
                PinSpec("qc", "QC", "QC", PinDirection.OUTPUT, 4f, 0.5f, pinNumber = 12),
                PinSpec("qd", "QD", "QD", PinDirection.OUTPUT, 4f, 1.5f, pinNumber = 11),
                PinSpec("ent", "ENT", "ENT", PinDirection.INPUT, 4f, 2.5f, pinNumber = 10),
                PinSpec("load", "LOAD'", "LOAD'", PinDirection.INPUT, 4f, 3.5f, pinNumber = 9, activeLow = true)
            ),
            defaultValue = "74161",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "16-pin DIP 4-Bit Synchronous Binary Counter with Async Clear (CLR'), Sync Load (LOAD'), and Ripple Carry Out (RCO).",
            datasheetSummary = "Counts 0..15 on rising CLK when ENP=ENT=1. RCO = ENT · QA · QB · QC · QD (HIGH at 15).",
            pinoutTable = listOf(
                "1: CLR', 2: CLK, 9: LOAD'" to "7: ENP, 10: ENT, 15: RCO",
                "3..6: Parallel Load A..D" to "14..11: QA..QD, 16: VCC, 8: GND"
            )
        ),
        ComponentMetadata(
            type = ComponentType.IC_74154,
            displayName = "74154 4-to-16 Decoder",
            shortCode = "74154",
            category = ComponentCategory.IC_7400,
            widthGrid = 9f,
            heightGrid = 11f,
            pins = listOf(
                PinSpec("a", "A0", "A0", PinDirection.INPUT, -4.5f, -4.5f, pinNumber = 23),
                PinSpec("b", "A1", "A1", PinDirection.INPUT, -4.5f, -3.5f, pinNumber = 22),
                PinSpec("c", "A2", "A2", PinDirection.INPUT, -4.5f, -2.5f, pinNumber = 21),
                PinSpec("d", "A3", "A3", PinDirection.INPUT, -4.5f, -1.5f, pinNumber = 20),
                PinSpec("g1", "G1'", "G1'", PinDirection.INPUT, -4.5f, -0.5f, pinNumber = 18, activeLow = true),
                PinSpec("g2", "G2'", "G2'", PinDirection.INPUT, -4.5f, 0.5f, pinNumber = 19, activeLow = true),
                PinSpec("y8", "Y8'", "Y8'", PinDirection.OUTPUT, -4.5f, 1.5f, pinNumber = 9, activeLow = true),
                PinSpec("y9", "Y9'", "Y9'", PinDirection.OUTPUT, -4.5f, 2.5f, pinNumber = 10, activeLow = true),
                PinSpec("y10", "Y10'", "Y10'", PinDirection.OUTPUT, -4.5f, 3.5f, pinNumber = 11, activeLow = true),
                PinSpec("gnd", "GND", "GND", PinDirection.GROUND, -4.5f, 4.5f, pinNumber = 12),
                PinSpec("vcc", "VCC", "VCC", PinDirection.POWER, 4.5f, -4.5f, pinNumber = 24),
                PinSpec("y0", "Y0'", "Y0'", PinDirection.OUTPUT, 4.5f, -3.5f, pinNumber = 1, activeLow = true),
                PinSpec("y1", "Y1'", "Y1'", PinDirection.OUTPUT, 4.5f, -2.5f, pinNumber = 2, activeLow = true),
                PinSpec("y2", "Y2'", "Y2'", PinDirection.OUTPUT, 4.5f, -1.5f, pinNumber = 3, activeLow = true),
                PinSpec("y3", "Y3'", "Y3'", PinDirection.OUTPUT, 4.5f, -0.5f, pinNumber = 4, activeLow = true),
                PinSpec("y4", "Y4'", "Y4'", PinDirection.OUTPUT, 4.5f, 0.5f, pinNumber = 5, activeLow = true),
                PinSpec("y5", "Y5'", "Y5'", PinDirection.OUTPUT, 4.5f, 1.5f, pinNumber = 6, activeLow = true),
                PinSpec("y6", "Y6'", "Y6'", PinDirection.OUTPUT, 4.5f, 2.5f, pinNumber = 7, activeLow = true),
                PinSpec("y7", "Y7'", "Y7'", PinDirection.OUTPUT, 4.5f, 3.5f, pinNumber = 8, activeLow = true),
                PinSpec("y15", "Y15'", "Y15'", PinDirection.OUTPUT, 4.5f, 4.5f, pinNumber = 17, activeLow = true)
            ),
            defaultValue = "74154",
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "4-to-16 Line Decoder/Demultiplexer with active-low strobe enables G1' and G2' and active-low outputs.",
            datasheetSummary = "When G1'=0 & G2'=0 (or floating), selected output Yi' goes LOW (0) for 4-bit address A3..A0.",
            pinoutTable = listOf(
                "23..20: A0..A3" to "18: G1', 19: G2' (Active Low)",
                "1..11,17: Y0'..Y15' (Active Low)" to "24: VCC, 12: GND"
            )
        ),
        ComponentMetadata(
            type = ComponentType.IC_555,
            displayName = "NE555 Timer",
            shortCode = "555",
            category = ComponentCategory.IC_7400,
            widthGrid = 7f,
            heightGrid = 5f,
            pins = listOf(
                PinSpec("gnd", "GND", "GND", PinDirection.GROUND, -3.5f, -1.5f, pinNumber = 1),
                PinSpec("trig", "TRIG", "TRIG", PinDirection.INPUT, -3.5f, -0.5f, pinNumber = 2),
                PinSpec("out", "OUT", "OUT", PinDirection.OUTPUT, -3.5f, 0.5f, pinNumber = 3),
                PinSpec("reset", "RST'", "RST'", PinDirection.INPUT, -3.5f, 1.5f, pinNumber = 4, activeLow = true),
                PinSpec("vcc", "VCC", "VCC", PinDirection.POWER, 3.5f, -1.5f, pinNumber = 8),
                PinSpec("disch", "DIS", "DIS", PinDirection.OUTPUT, 3.5f, -0.5f, pinNumber = 7),
                PinSpec("thres", "THR", "THR", PinDirection.INPUT, 3.5f, 0.5f, pinNumber = 6),
                PinSpec("ctrl", "CTRL", "CTRL", PinDirection.INPUT, 3.5f, 1.5f, pinNumber = 5)
            ),
            defaultValue = "Astable 2Hz",
            defaultProperties = mapOf("mode" to "Astable", "frequency" to "2.0", "dutyPct" to "50"),
            supportLevel = SimulationSupportLevel.MIXED_SIGNAL,
            description = "8-pin DIP precision timing IC generating astable square pulses or monostable triggers.",
            datasheetSummary = "Pin 3 OUT oscillates in Astable mode when RST' != 0. Supports digital & LED blink circuits.",
            pinoutTable = listOf(
                "1: GND" to "8: VCC",
                "2: TRIG" to "7: DISCH",
                "3: OUT" to "6: THRES",
                "4: RESET'" to "5: CTRL"
            )
        ),
        ComponentMetadata(
            type = ComponentType.CUSTOM_IC,
            displayName = "Custom Subcircuit IC",
            shortCode = "MY-IC",
            category = ComponentCategory.IC_7400,
            widthGrid = 5f,
            heightGrid = 4f,
            pins = listOf(
                PinSpec("in0", "A", "A", PinDirection.INPUT, -2.5f, -0.5f, pinNumber = 1),
                PinSpec("in1", "B", "B", PinDirection.INPUT, -2.5f, 0.5f, pinNumber = 2),
                PinSpec("out0", "Y", "Y", PinDirection.OUTPUT, 2.5f, 0f, pinNumber = 3)
            ),
            defaultValue = "MY-IC",
            defaultProperties = mapOf(
                "icName" to "Custom Subcircuit",
                "shortCode" to "MY-IC",
                "inputLabels" to "A,B",
                "outputLabels" to "Y",
                "truthMap" to "0:0,1:1,2:1,3:0"
            ),
            supportLevel = SimulationSupportLevel.FULL_DIGITAL,
            description = "User-packaged reusable Custom IC block built from any combinational circuit in GateX Labs.",
            datasheetSummary = "Use 'Save as My IC' in the Workspace to package your own subcircuits into custom chips!"
        )
    )
}

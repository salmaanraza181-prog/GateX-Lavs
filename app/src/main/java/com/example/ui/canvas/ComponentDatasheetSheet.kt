package com.example.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ComponentCatalog
import com.example.model.ComponentInstance
import com.example.model.ComponentType
import com.example.model.PinDirection
import com.example.ui.theme.CopperGold
import com.example.ui.theme.FaultRed
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LogicHighGreen
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SignalCyan

private data class DatasheetDetails(
    val packageLabel: String,
    val truthTableHeader: List<String>,
    val truthTableRows: List<List<String>>,
    val wiringTip: String
)

private fun datasheetFor(type: ComponentType): DatasheetDetails = when (type) {
    ComponentType.GATE_AND, ComponentType.IC_7408 -> DatasheetDetails(
        packageLabel = if (type == ComponentType.IC_7408) "14-Pin DIP (Quad 2-In AND)" else "2-Input Logic Gate",
        truthTableHeader = listOf("A", "B", "Y = A·B"),
        truthTableRows = listOf(listOf("0", "0", "0"), listOf("0", "1", "0"), listOf("1", "0", "0"), listOf("1", "1", "1")),
        wiringTip = "Connect Pin 14 to VCC (+5V) and Pin 7 to GND. Output is HIGH only when both inputs are HIGH."
    )
    ComponentType.GATE_OR, ComponentType.IC_7432 -> DatasheetDetails(
        packageLabel = if (type == ComponentType.IC_7432) "14-Pin DIP (Quad 2-In OR)" else "2-Input Logic Gate",
        truthTableHeader = listOf("A", "B", "Y = A+B"),
        truthTableRows = listOf(listOf("0", "0", "0"), listOf("0", "1", "1"), listOf("1", "0", "1"), listOf("1", "1", "1")),
        wiringTip = "Output goes HIGH whenever at least one input is HIGH."
    )
    ComponentType.GATE_NOT, ComponentType.IC_7404 -> DatasheetDetails(
        packageLabel = if (type == ComponentType.IC_7404) "14-Pin DIP (Hex Inverter)" else "1-Input Inverter",
        truthTableHeader = listOf("A", "Y = A'"),
        truthTableRows = listOf(listOf("0", "1"), listOf("1", "0")),
        wiringTip = "Six independent inverters in one 7404 DIP package (Pins 1→2, 3→4, 5→6, 9→8, 11→10, 13→12)."
    )
    ComponentType.GATE_NAND, ComponentType.IC_7400 -> DatasheetDetails(
        packageLabel = if (type == ComponentType.IC_7400) "14-Pin DIP (Quad 2-In NAND)" else "Universal NAND Gate",
        truthTableHeader = listOf("A", "B", "Y = (A·B)'"),
        truthTableRows = listOf(listOf("0", "0", "1"), listOf("0", "1", "1"), listOf("1", "0", "1"), listOf("1", "1", "0")),
        wiringTip = "Universal gate: tie both inputs together to make a NOT gate, or cascade NANDs to build any Boolean function."
    )
    ComponentType.GATE_NOR, ComponentType.IC_7402 -> DatasheetDetails(
        packageLabel = if (type == ComponentType.IC_7402) "14-Pin DIP (Quad 2-In NOR)" else "Universal NOR Gate",
        truthTableHeader = listOf("A", "B", "Y = (A+B)'"),
        truthTableRows = listOf(listOf("0", "0", "1"), listOf("0", "1", "0"), listOf("1", "0", "0"), listOf("1", "1", "0")),
        wiringTip = "Note 7402 pinout: output 1Y is on Pin 1 and inputs 1A/1B are Pins 2 & 3 (reversed compared to 7400)."
    )
    ComponentType.GATE_XOR, ComponentType.IC_7486 -> DatasheetDetails(
        packageLabel = if (type == ComponentType.IC_7486) "14-Pin DIP (Quad 2-In XOR)" else "2-Input XOR Gate",
        truthTableHeader = listOf("A", "B", "Y = A⊕B"),
        truthTableRows = listOf(listOf("0", "0", "0"), listOf("0", "1", "1"), listOf("1", "0", "1"), listOf("1", "1", "0")),
        wiringTip = "Ideal for half-adder sum bits, parity generators, and controlled inverters."
    )
    ComponentType.GATE_XNOR, ComponentType.IC_74266 -> DatasheetDetails(
        packageLabel = if (type == ComponentType.IC_74266) "14-Pin DIP (Quad 2-In XNOR)" else "2-Input XNOR Gate",
        truthTableHeader = listOf("A", "B", "Y = (A⊕B)'"),
        truthTableRows = listOf(listOf("0", "0", "1"), listOf("0", "1", "0"), listOf("1", "0", "0"), listOf("1", "1", "1")),
        wiringTip = "Outputs HIGH when both inputs are identical — great for 1-bit equality comparators."
    )
    ComponentType.IC_7472, ComponentType.IC_7473, ComponentType.IC_7476 -> DatasheetDetails(
        packageLabel = "14-Pin DIP (JK Flip-Flop)",
        truthTableHeader = listOf("PRE'/CLR'", "CLK", "J", "K", "Q(next)"),
        truthTableRows = listOf(
            listOf("0 / 1", "X", "X", "X", "1 (Set)"),
            listOf("1 / 0", "X", "X", "X", "0 (Clear)"),
            listOf("1 / 1", "↑", "0", "0", "Q (Hold)"),
            listOf("1 / 1", "↑", "0", "1", "0 (Reset)"),
            listOf("1 / 1", "↑", "1", "0", "1 (Set)"),
            listOf("1 / 1", "↑", "1", "1", "Q' (Toggle)")
        ),
        wiringTip = "Tie PRE' and CLR' to VCC for normal clocked operation. With J=K=1, Q toggles on every clock pulse (÷2)."
    )
    ComponentType.IC_7474 -> DatasheetDetails(
        packageLabel = "14-Pin DIP (Dual D Flip-Flop)",
        truthTableHeader = listOf("PRE'", "CLR'", "CLK", "D", "Q(next)"),
        truthTableRows = listOf(
            listOf("0", "1", "X", "X", "1"),
            listOf("1", "0", "X", "X", "0"),
            listOf("1", "1", "↑", "0", "0"),
            listOf("1", "1", "↑", "1", "1"),
            listOf("1", "1", "0/1", "X", "Q (Hold)")
        ),
        wiringTip = "Positive edge-triggered: D is latched into Q on the 0→1 rising edge of CLK."
    )
    ComponentType.IC_7490, ComponentType.IC_7493 -> DatasheetDetails(
        packageLabel = if (type == ComponentType.IC_7490) "14-Pin DIP (BCD Decade Counter)" else "14-Pin DIP (4-Bit Binary Counter)",
        truthTableHeader = listOf("R0(1) R0(2)", "CLKA", "Wire QA→CLKB", "Sequence"),
        truthTableRows = listOf(
            listOf("1  1", "X", "Any", "0000 (Reset)"),
            listOf("0  X", "↓", "Connected", if (type == ComponentType.IC_7490) "0→1..→9→0" else "0→1..→15→0")
        ),
        wiringTip = "Connect QA (Pin 12) to CLKB (Pin 1) and tie R01/R02 to GND to count continuously from 0."
    )
    ComponentType.IC_7447 -> DatasheetDetails(
        packageLabel = "16-Pin DIP (BCD-to-7-Seg Driver)",
        truthTableHeader = listOf("LT'", "BI'", "D C B A", "Digit (Active-LOW a'–g')"),
        truthTableRows = listOf(
            listOf("X", "0", "X X X X", "Blank (all HIGH)"),
            listOf("0", "1", "X X X X", "8 (Lamp Test, all LOW)"),
            listOf("1", "1", "0 0 0 0", "0 (a'..f'=0, g'=1)"),
            listOf("1", "1", "0 1 0 1", "5 (a',c',d',f',g'=0)"),
            listOf("1", "1", "1 0 0 1", "9 (a',b',c',f',g'=0)")
        ),
        wiringTip = "Outputs a'–g' are active-LOW (0 = ON). Pair with a 7-Segment Display set to Active-LOW mode."
    )
    ComponentType.IC_7483 -> DatasheetDetails(
        packageLabel = "16-Pin DIP (4-Bit Binary Adder)",
        truthTableHeader = listOf("A[4:1]", "B[4:1]", "C0", "Result {C4, S4..S1}"),
        truthTableRows = listOf(
            listOf("0011 (3)", "0100 (4)", "0", "0 0111 (7)"),
            listOf("0101 (5)", "0110 (6)", "0", "0 1011 (11)"),
            listOf("1111 (15)", "0001 (1)", "0", "1 0000 (16)")
        ),
        wiringTip = "A1/B1/S1 are LSB (2⁰) and A4/B4/S4 are MSB (2³). Chain C4 to the next chip's C0 for 8-bit addition."
    )
    ComponentType.IC_74194 -> DatasheetDetails(
        packageLabel = "16-Pin DIP (4-Bit Universal Shift Reg)",
        truthTableHeader = listOf("CLR'", "S1", "S0", "CLK", "Operation"),
        truthTableRows = listOf(
            listOf("0", "X", "X", "X", "Clear QA..QD = 0000"),
            listOf("1", "0", "0", "↑", "Hold state"),
            listOf("1", "0", "1", "↑", "Shift Right (SR→QA→QD)"),
            listOf("1", "1", "0", "↑", "Shift Left (SL→QD→QA)"),
            listOf("1", "1", "1", "↑", "Parallel Load (A..D→QA..QD)")
        ),
        wiringTip = "Tie CLR' (Pin 1) HIGH. Set S1=S0=1 to parallel-load bits A–D, then S1=0, S0=1 to shift right each clock."
    )
    ComponentType.OP_AMP_741 -> DatasheetDetails(
        packageLabel = "8-Pin DIP / Analog Triangle (LM741)",
        truthTableHeader = listOf("Topology", "Formula", "Typical Setup"),
        truthTableRows = listOf(
            listOf("Voltage Follower", "Vout = Vin", "Wire OUT directly to (-)"),
            listOf("Non-Inverting", "Vout = (1 + Rf/Rg)·Vin", "Vin→(+), Rf: OUT→(-), Rg: (-)→GND"),
            listOf("Inverting", "Vout = -(Rf/Rin)·Vin", "(+)→GND, Rin: Vin→(-), Rf: OUT→(-)")
        ),
        wiringTip = "Defaults to ±12V supply rails if V+/V- pins are left unconnected, or wire custom DC supply rails to Pins 7 & 4."
    )
    ComponentType.FUSE -> DatasheetDetails(
        packageLabel = "Glass Cartridge Overcurrent Fuse",
        truthTableHeader = listOf("Condition", "Resistance", "Action"),
        truthTableRows = listOf(
            listOf("|I| ≤ Rating", "0.05 Ω (Intact)", "Passes current normally"),
            listOf("|I| > Rating", "1 GΩ (Blown)", "Opens circuit + triggers alarm")
        ),
        wiringTip = "Place in series with DC power sources. If it blows, lower load current and tap 'Reset Fuse' in the bottom bar."
    )
    ComponentType.NET_LABEL -> DatasheetDetails(
        packageLabel = "Wireless Schematic Net Tag",
        truthTableHeader = listOf("Tag Name", "Behavior"),
        truthTableRows = listOf(
            listOf("Identical (e.g. CLK & CLK)", "Electrically shorted across canvas"),
            listOf("Different (e.g. A & B)", "Electrically isolated nets")
        ),
        wiringTip = "Tap 'Edit' on a Net Label to rename it (case-insensitive). Keeps large 74xx IC circuits clean without tangled wires."
    )
    ComponentType.IC_7485 -> DatasheetDetails(
        packageLabel = "16-Pin DIP (4-Bit Magnitude Comparator)",
        truthTableHeader = listOf("A vs B", "A>B Out", "A=B Out", "A<B Out"),
        truthTableRows = listOf(
            listOf("A > B", "1", "0", "0"),
            listOf("A < B", "0", "0", "1"),
            listOf("A == B (EQ_IN=1)", "0", "1", "0")
        ),
        wiringTip = "Tie I(A=B) HIGH and I(A>B)/I(A<B) LOW for standalone 4-bit comparison, or cascade from a lower-order 7485."
    )
    ComponentType.IC_74595 -> DatasheetDetails(
        packageLabel = "16-Pin DIP (8-Bit SIPO Shift Register)",
        truthTableHeader = listOf("SRCLR'", "SRCLK", "RCLK", "Action"),
        truthTableRows = listOf(
            listOf("0", "X", "X", "Clear shift register to 0x00"),
            listOf("1", "↑", "0", "Shift SER into QA..QH"),
            listOf("1", "X", "↑", "Latch shift reg to output pins")
        ),
        wiringTip = "Keep OE' LOW and SRCLR' HIGH. Pulse SRCLK to shift bits in, then pulse RCLK to display them on QA–QH."
    )
    ComponentType.IC_74161 -> DatasheetDetails(
        packageLabel = "16-Pin DIP (4-Bit Synchronous Counter)",
        truthTableHeader = listOf("CLR'", "LOAD'", "ENP·ENT", "CLK", "Operation"),
        truthTableRows = listOf(
            listOf("0", "X", "X", "X", "Async Clear QA..QD = 0"),
            listOf("1", "0", "X", "↑", "Sync Parallel Load A..D"),
            listOf("1", "1", "1", "↑", "Increment (0..15, RCO=1 at 15)")
        ),
        wiringTip = "Tie CLR', LOAD', ENP, and ENT HIGH to count 0→15 synchronously on every rising CLK edge."
    )
    ComponentType.IC_74154 -> DatasheetDetails(
        packageLabel = "24-Pin DIP (4-to-16 Active-LOW Decoder)",
        truthTableHeader = listOf("G1' / G2'", "D C B A", "Selected Output"),
        truthTableRows = listOf(
            listOf("1 / X", "X X X X", "All Y0'..Y15' = 1 (Disabled)"),
            listOf("0 / 0", "0 0 0 0", "Y0' = 0, others = 1"),
            listOf("0 / 0", "1 1 1 1", "Y15' = 0, others = 1")
        ),
        wiringTip = "Tie G1' and G2' LOW to enable decoding. Exactly one of Y0'–Y15' pulls LOW matching binary D..A."
    )
    ComponentType.CUSTOM_IC -> DatasheetDetails(
        packageLabel = "Custom User Subcircuit IC",
        truthTableHeader = listOf("Pin Side", "Signal Role"),
        truthTableRows = listOf(
            listOf("Left (I1..I4)", "Subcircuit Logic Inputs"),
            listOf("Right (O1..O4)", "Truth-Table Evaluated Outputs")
        ),
        wiringTip = "Created with 'Save as IC'. Re-usable in any schematic with live combinational evaluation."
    )
    ComponentType.LCD_16X2 -> DatasheetDetails(
        packageLabel = "16×2 Character LCD Module",
        truthTableHeader = listOf("EN", "D3..D0", "Display Output"),
        truthTableRows = listOf(
            listOf("0", "Any", "Backlight Standby"),
            listOf("1", "0000..1111", "Line 1: Title, Line 2: DEC & HEX")
        ),
        wiringTip = "Edit Line 1 text in Properties! Connect EN to VCC/Logic 1 and drive D0..D3 from a counter or switches."
    )
    ComponentType.DOT_MATRIX_8X8 -> DatasheetDetails(
        packageLabel = "8×8 LED Dot Matrix",
        truthTableHeader = listOf("R0..R3", "C0..C3", "LED State"),
        truthTableRows = listOf(
            listOf("1", "1", "2×2 Pixel Block Illuminated"),
            listOf("0", "Any", "Off")
        ),
        wiringTip = "Drive rows R0–R3 and columns C0–C3 HIGH to illuminate corresponding dot quadrants."
    )
    ComponentType.TRAFFIC_LIGHT -> DatasheetDetails(
        packageLabel = "3-Lens Traffic Signal Module",
        truthTableHeader = listOf("Pin", "Color", "Active Condition"),
        truthTableRows = listOf(
            listOf("R", "Red Stop", "Logic 1 or >1.5V"),
            listOf("Y", "Yellow Caution", "Logic 1 or >1.5V"),
            listOf("G", "Green Go", "Logic 1 or >1.5V")
        ),
        wiringTip = "Great for sequential state machines, 7490 counters, and digital lab projects."
    )
    else -> DatasheetDetails(
        packageLabel = ComponentCatalog.get(type).category.title,
        truthTableHeader = listOf("Property", "Default"),
        truthTableRows = listOf(
            listOf("Code", ComponentCatalog.get(type).shortCode),
            listOf("Default Value", ComponentCatalog.get(type).defaultValue.ifBlank { "—" }),
            listOf("Engine Support", ComponentCatalog.get(type).supportLevel.name.replace('_', ' '))
        ),
        wiringTip = ComponentCatalog.get(type).datasheetSummary
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentDatasheetSheet(
    component: ComponentInstance,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val meta = ComponentCatalog.metadataFor(component)
    val details = datasheetFor(component.type)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("component_datasheet_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF0A101D) else Color(0xFFF1F5F9))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        ComponentSymbolThumbnail(
                            type = component.type,
                            isDark = isDark,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "${component.label} — ${meta.displayName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        AssistChip(
                            onClick = {},
                            label = { Text(details.packageLabel, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("datasheet_close_btn")) {
                    Icon(Icons.Default.Close, contentDescription = "Close datasheet")
                }
            }

            // Summary Card
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(meta.description, style = MaterialTheme.typography.bodyMedium)
                    if (meta.datasheetSummary.isNotBlank()) {
                        Text(
                            meta.datasheetSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Pinout Table
            if (meta.pins.isNotEmpty()) {
                Text(
                    text = "Pinout Reference (${meta.pins.size} Pins)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Pin", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(46.dp))
                            Text("Signal", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("Role", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(72.dp))
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        val orderedPins = meta.pins.sortedBy { it.pinNumber ?: 99 }
                        orderedPins.forEachIndexed { idx, pin ->
                            val dirColor = when (pin.direction) {
                                PinDirection.POWER -> FaultRed
                                PinDirection.GROUND -> LogicHighGreen
                                PinDirection.OUTPUT -> SignalAmber
                                PinDirection.INPUT -> SignalCyan
                                PinDirection.BIDIRECTIONAL -> CopperGold
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = pin.pinNumber?.let { "#$it" } ?: "${idx + 1}",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(46.dp)
                                )
                                Text(
                                    text = buildString {
                                        append(pin.name)
                                        if (pin.activeLow) append(" (Active-LOW)")
                                        if (pin.isClock) append(" (Clock)")
                                    },
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = pin.direction.name.take(6),
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = dirColor,
                                    modifier = Modifier.width(72.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Truth Table / Operating Behavior
            if (details.truthTableRows.isNotEmpty()) {
                Text(
                    text = "Truth Table & Characteristics",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            details.truthTableHeader.forEach { header ->
                                Text(
                                    text = header,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        details.truthTableRows.forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                row.forEach { cell ->
                                    Text(
                                        text = cell,
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Practical Lab Tip
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.TipsAndUpdates,
                        contentDescription = null,
                        tint = SignalAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text("Lab Wiring Tip", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text(details.wiringTip, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}

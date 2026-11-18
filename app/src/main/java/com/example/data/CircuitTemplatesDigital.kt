package com.example.data

import com.example.model.CircuitPage
import com.example.model.ComponentInstance
import com.example.model.ComponentType
import com.example.model.WireJunction

internal fun digitalTemplates(): List<CircuitTemplate> = listOf(
    CircuitTemplate(
        id = "half_adder",
        title = "5. Half Adder",
        domain = TemplateDomain.DIGITAL,
        summary = "1-bit Half Adder: Sum = A ⊕ B, Carry = A · B using XOR + AND gates.",
        explanation = "Adds two single bits A and B. Toggle inputs A and B to verify Sum (0,1,1,0) and Carry (0,0,0,1).",
        buildPage = {
            CircuitPage(
                id = "p1",
                name = "Half Adder",
                components = listOf(
                    ComponentInstance("inA", ComponentType.LOGIC_INPUT, "A", 4f, 6f, value = "1", properties = mapOf("state" to "1")),
                    ComponentInstance("inB", ComponentType.LOGIC_INPUT, "B", 4f, 11f, value = "1", properties = mapOf("state" to "1")),
                    ComponentInstance("xor1", ComponentType.GATE_XOR, "U1", 12f, 6f),
                    ComponentInstance("and1", ComponentType.GATE_AND, "U2", 12f, 11f),
                    ComponentInstance("outS", ComponentType.LOGIC_INDICATOR, "SUM", 19f, 6f),
                    ComponentInstance("outC", ComponentType.LED_INDICATOR, "CARRY", 19f, 11f)
                ),
                junctions = listOf(
                    WireJunction("jA", 8f, 6f),
                    WireJunction("jB", 8f, 11f)
                ),
                wires = listOf(
                    wJ("w1", "inA", "out", "jA"),
                    wJ("w2", "xor1", "a", "jA"),
                    wJ("w3", "and1", "a", "jA"),
                    wJ("w4", "inB", "out", "jB"),
                    wJ("w5", "xor1", "b", "jB"),
                    wJ("w6", "and1", "b", "jB"),
                    w("w7", "xor1", "y", "outS", "in"),
                    w("w8", "and1", "y", "outC", "in")
                )
            )
        }
    ),
    CircuitTemplate(
        id = "full_adder",
        title = "6. Full Adder",
        domain = TemplateDomain.DIGITAL,
        summary = "Full Adder with 2 XOR, 2 AND, and 1 OR gate computing Sum and Carry-Out.",
        explanation = "Adds A + B + Cin: Sum = (A⊕B)⊕Cin, Cout = (A·B) + ((A⊕B)·Cin).",
        buildPage = {
            CircuitPage(
                id = "p1",
                name = "Full Adder",
                components = listOf(
                    ComponentInstance("inA", ComponentType.LOGIC_INPUT, "A", 3f, 5f, value = "1", properties = mapOf("state" to "1")),
                    ComponentInstance("inB", ComponentType.LOGIC_INPUT, "B", 3f, 9f, value = "1", properties = mapOf("state" to "1")),
                    ComponentInstance("inCin", ComponentType.LOGIC_INPUT, "Cin", 3f, 13f, value = "0", properties = mapOf("state" to "0")),
                    ComponentInstance("xor1", ComponentType.GATE_XOR, "XOR1", 10f, 6f),
                    ComponentInstance("xor2", ComponentType.GATE_XOR, "XOR2", 18f, 6f),
                    ComponentInstance("and1", ComponentType.GATE_AND, "AND1", 10f, 11f),
                    ComponentInstance("and2", ComponentType.GATE_AND, "AND2", 18f, 11f),
                    ComponentInstance("or1", ComponentType.GATE_OR, "OR1", 25f, 11f),
                    ComponentInstance("outS", ComponentType.LOGIC_INDICATOR, "SUM", 29f, 6f),
                    ComponentInstance("outC", ComponentType.LED_INDICATOR, "COUT", 31f, 11f)
                ),
                junctions = listOf(
                    WireJunction("jA", 6.5f, 5f),
                    WireJunction("jB", 6.5f, 9f),
                    WireJunction("jCin", 14f, 13f),
                    WireJunction("jX1", 14f, 6f)
                ),
                wires = listOf(
                    wJ("w1", "inA", "out", "jA"),
                    wJ("w2", "xor1", "a", "jA"),
                    wJ("w3", "and1", "a", "jA"),
                    wJ("w4", "inB", "out", "jB"),
                    wJ("w5", "xor1", "b", "jB"),
                    wJ("w6", "and1", "b", "jB"),
                    wJ("w7", "xor1", "y", "jX1"),
                    wJ("w8", "xor2", "a", "jX1"),
                    wJ("w9", "and2", "a", "jX1"),
                    wJ("w10", "inCin", "out", "jCin"),
                    wJ("w11", "xor2", "b", "jCin"),
                    wJ("w12", "and2", "b", "jCin"),
                    w("w13", "xor2", "y", "outS", "in"),
                    w("w14", "and2", "y", "or1", "a"),
                    w("w15", "and1", "y", "or1", "b"),
                    w("w16", "or1", "y", "outC", "in")
                )
            )
        }
    ),
    CircuitTemplate(
        id = "half_subtractor",
        title = "7. Half Subtractor",
        domain = TemplateDomain.DIGITAL,
        summary = "Computes Difference (A ⊕ B) and Borrow (A' · B).",
        explanation = "Diff = A ⊕ B; Borrow = (NOT A) AND B. Borrow = 1 only when A=0 and B=1.",
        buildPage = {
            CircuitPage(
                id = "p1",
                name = "Half Subtractor",
                components = listOf(
                    ComponentInstance("inA", ComponentType.LOGIC_INPUT, "A", 4f, 6f, value = "0", properties = mapOf("state" to "0")),
                    ComponentInstance("inB", ComponentType.LOGIC_INPUT, "B", 4f, 11f, value = "1", properties = mapOf("state" to "1")),
                    ComponentInstance("xor1", ComponentType.GATE_XOR, "U1", 12f, 6f),
                    ComponentInstance("not1", ComponentType.GATE_NOT, "U2", 10f, 10f),
                    ComponentInstance("and1", ComponentType.GATE_AND, "U3", 16f, 11f),
                    ComponentInstance("outD", ComponentType.LOGIC_INDICATOR, "DIFF", 22f, 6f),
                    ComponentInstance("outBo", ComponentType.LED_INDICATOR, "BORROW", 22f, 11f)
                ),
                junctions = listOf(WireJunction("jA", 7f, 6f), WireJunction("jB", 7f, 11f)),
                wires = listOf(
                    wJ("w1", "inA", "out", "jA"),
                    wJ("w2", "xor1", "a", "jA"),
                    wJ("w3", "not1", "a", "jA"),
                    wJ("w4", "inB", "out", "jB"),
                    wJ("w5", "xor1", "b", "jB"),
                    wJ("w6", "and1", "b", "jB"),
                    w("w7", "not1", "y", "and1", "a"),
                    w("w8", "xor1", "y", "outD", "in"),
                    w("w9", "and1", "y", "outBo", "in")
                )
            )
        }
    ),
    CircuitTemplate(
        id = "full_subtractor",
        title = "8. Full Subtractor",
        domain = TemplateDomain.DIGITAL,
        summary = "Computes A - B - Bin outputting Difference and Borrow-Out.",
        explanation = "Cascades two half-subtractors with an OR gate for final Borrow-Out.",
        buildPage = {
            CircuitPage(
                id = "p1",
                name = "Full Subtractor",
                components = listOf(
                    ComponentInstance("inA", ComponentType.LOGIC_INPUT, "A", 3f, 5f, value = "0", properties = mapOf("state" to "0")),
                    ComponentInstance("inB", ComponentType.LOGIC_INPUT, "B", 3f, 9f, value = "1", properties = mapOf("state" to "1")),
                    ComponentInstance("inBin", ComponentType.LOGIC_INPUT, "Bin", 3f, 13f, value = "1", properties = mapOf("state" to "1")),
                    ComponentInstance("xor1", ComponentType.GATE_XOR, "XOR1", 10f, 6f),
                    ComponentInstance("xor2", ComponentType.GATE_XOR, "XOR2", 19f, 6f),
                    ComponentInstance("not1", ComponentType.GATE_NOT, "INV1", 9f, 10f),
                    ComponentInstance("and1", ComponentType.GATE_AND, "AND1", 14f, 10f),
                    ComponentInstance("not2", ComponentType.GATE_NOT, "INV2", 14f, 13f),
                    ComponentInstance("and2", ComponentType.GATE_AND, "AND2", 20f, 13f),
                    ComponentInstance("or1", ComponentType.GATE_OR, "OR1", 26f, 11f),
                    ComponentInstance("outD", ComponentType.LOGIC_INDICATOR, "DIFF", 31f, 6f),
                    ComponentInstance("outBo", ComponentType.LED_INDICATOR, "BOUT", 31f, 11f)
                ),
                junctions = listOf(
                    WireJunction("jA", 6f, 5f),
                    WireJunction("jB", 6f, 9f),
                    WireJunction("jX1", 13f, 6f),
                    WireJunction("jBin", 17f, 13f)
                ),
                wires = listOf(
                    wJ("w1", "inA", "out", "jA"),
                    wJ("w2", "xor1", "a", "jA"),
                    wJ("w3", "not1", "a", "jA"),
                    wJ("w4", "inB", "out", "jB"),
                    wJ("w5", "xor1", "b", "jB"),
                    wJ("w6", "and1", "b", "jB"),
                    w("w7", "not1", "y", "and1", "a"),
                    wJ("w8", "xor1", "y", "jX1"),
                    wJ("w9", "xor2", "a", "jX1"),
                    wJ("w10", "not2", "a", "jX1"),
                    wJ("w11", "inBin", "out", "jBin"),
                    wJ("w12", "xor2", "b", "jBin"),
                    wJ("w13", "and2", "b", "jBin"),
                    w("w14", "not2", "y", "and2", "a"),
                    w("w15", "and1", "y", "or1", "a"),
                    w("w16", "and2", "y", "or1", "b"),
                    w("w17", "xor2", "y", "outD", "in"),
                    w("w18", "or1", "y", "outBo", "in")
                )
            )
        }
    ),
    CircuitTemplate(
        id = "sr_latch",
        title = "9. SR Latch (Cross-Coupled NOR)",
        domain = TemplateDomain.DIGITAL,
        summary = "1-bit memory latch built with feedback-connected NOR gates.",
        explanation = "Cross-coupled NOR gates store 1 bit: S=1,R=0 sets Q=1; S=0,R=1 resets Q=0; S=0,R=0 holds state.",
        buildPage = {
            CircuitPage(
                id = "p1",
                name = "SR Latch",
                components = listOf(
                    ComponentInstance("inR", ComponentType.LOGIC_INPUT, "R", 4f, 5f, value = "0", properties = mapOf("state" to "0")),
                    ComponentInstance("inS", ComponentType.LOGIC_INPUT, "S", 4f, 11f, value = "1", properties = mapOf("state" to "1")),
                    ComponentInstance("nor1", ComponentType.GATE_NOR, "NOR1", 12f, 5f),
                    ComponentInstance("nor2", ComponentType.GATE_NOR, "NOR2", 12f, 11f),
                    ComponentInstance("outQ", ComponentType.LOGIC_INDICATOR, "Q", 20f, 5f),
                    ComponentInstance("outQn", ComponentType.LOGIC_INDICATOR, "Q'", 20f, 11f)
                ),
                junctions = listOf(WireJunction("jQ", 16f, 5f), WireJunction("jQn", 16f, 11f)),
                wires = listOf(
                    w("w1", "inR", "out", "nor1", "a"),
                    w("w2", "inS", "out", "nor2", "b"),
                    wJ("w3", "nor1", "y", "jQ"),
                    wJ("w4", "outQ", "in", "jQ"),
                    wJ("w5", "nor2", "a", "jQ"),
                    wJ("w6", "nor2", "y", "jQn"),
                    wJ("w7", "outQn", "in", "jQn"),
                    wJ("w8", "nor1", "b", "jQn")
                )
            )
        }
    ),
    CircuitTemplate(
        id = "d_latch",
        title = "10. D Latch (Gated)",
        domain = TemplateDomain.DIGITAL,
        summary = "Transparent D Latch using NOT, AND, and cross-coupled NOR gates.",
        explanation = "When Enable EN=1, Q follows D. When EN=0, Q holds the last captured bit.",
        buildPage = {
            CircuitPage(
                id = "p1",
                name = "D Latch",
                components = listOf(
                    ComponentInstance("inD", ComponentType.LOGIC_INPUT, "D", 3f, 6f, value = "1", properties = mapOf("state" to "1")),
                    ComponentInstance("inEn", ComponentType.LOGIC_INPUT, "EN", 3f, 11f, value = "1", properties = mapOf("state" to "1")),
                    ComponentInstance("inv", ComponentType.GATE_NOT, "INV", 8f, 6f),
                    ComponentInstance("andR", ComponentType.GATE_AND, "AND_R", 14f, 6f),
                    ComponentInstance("andS", ComponentType.GATE_AND, "AND_S", 14f, 11f),
                    ComponentInstance("nor1", ComponentType.GATE_NOR, "NOR1", 21f, 6f),
                    ComponentInstance("nor2", ComponentType.GATE_NOR, "NOR2", 21f, 11f),
                    ComponentInstance("outQ", ComponentType.LOGIC_INDICATOR, "Q", 28f, 6f),
                    ComponentInstance("outQn", ComponentType.LOGIC_INDICATOR, "Q'", 28f, 11f)
                ),
                junctions = listOf(
                    WireJunction("jD", 5.5f, 6f),
                    WireJunction("jEn", 10f, 11f),
                    WireJunction("jQ", 25f, 6f),
                    WireJunction("jQn", 25f, 11f)
                ),
                wires = listOf(
                    wJ("w1", "inD", "out", "jD"),
                    wJ("w2", "inv", "a", "jD"),
                    wJ("w3", "andS", "b", "jD"),
                    w("w4", "inv", "y", "andR", "a"),
                    wJ("w5", "inEn", "out", "jEn"),
                    wJ("w6", "andR", "b", "jEn"),
                    wJ("w7", "andS", "a", "jEn"),
                    w("w8", "andR", "y", "nor1", "a"),
                    w("w9", "andS", "y", "nor2", "b"),
                    wJ("w10", "nor1", "y", "jQ"),
                    wJ("w11", "outQ", "in", "jQ"),
                    wJ("w12", "nor2", "a", "jQ"),
                    wJ("w13", "nor2", "y", "jQn"),
                    wJ("w14", "outQn", "in", "jQn"),
                    wJ("w15", "nor1", "b", "jQn")
                )
            )
        }
    ),
    CircuitTemplate(
        id = "d_flip_flop",
        title = "11. D Flip-Flop (7474)",
        domain = TemplateDomain.DIGITAL,
        summary = "Rising-edge triggered 74LS74 D Flip-Flop with Clock & Preset/Clear.",
        explanation = "Data D is sampled strictly on rising 0->1 clock transitions. Connect Logic Analyzer to observe timing.",
        buildPage = {
            CircuitPage(
                id = "p1",
                name = "D Flip-Flop",
                components = listOf(
                    ComponentInstance("inD", ComponentType.LOGIC_INPUT, "D", 4f, 6f, value = "1", properties = mapOf("state" to "1")),
                    ComponentInstance("clk", ComponentType.CLOCK_INPUT, "CLK", 4f, 10f, value = "1 Hz"),
                    ComponentInstance("vcc", ComponentType.VCC, "VCC", 12f, 3f),
                    ComponentInstance("ff", ComponentType.IC_7474, "U1", 12f, 8f),
                    ComponentInstance("outQ", ComponentType.LOGIC_INDICATOR, "Q", 20f, 7f),
                    ComponentInstance("outQn", ComponentType.LED_INDICATOR, "Q'", 20f, 10f)
                ),
                junctions = listOf(WireJunction("jv", 12f, 5f)),
                wires = listOf(
                    w("w1", "inD", "out", "ff", "d"),
                    w("w2", "clk", "out", "ff", "clk"),
                    wJ("w3", "vcc", "vcc", "jv"),
                    wJ("w4", "ff", "pre", "jv"),
                    wJ("w5", "ff", "clr", "jv"),
                    w("w6", "ff", "q", "outQ", "in"),
                    w("w7", "ff", "qn", "outQn", "in")
                )
            )
        }
    ),
    CircuitTemplate(
        id = "jk_flip_flop",
        title = "12. JK Flip-Flop (7476)",
        domain = TemplateDomain.DIGITAL,
        summary = "74LS76 JK Flip-Flop configured in Toggle mode (J=1, K=1) dividing clock by 2.",
        explanation = "When J=1 and K=1, Q toggles on every rising clock edge, functioning as a divide-by-2 frequency divider.",
        buildPage = {
            CircuitPage(
                id = "p1",
                name = "JK Flip-Flop",
                components = listOf(
                    ComponentInstance("inJ", ComponentType.LOGIC_INPUT, "J", 4f, 5f, value = "1", properties = mapOf("state" to "1")),
                    ComponentInstance("clk", ComponentType.CLOCK_INPUT, "CLK", 4f, 9f, value = "1 Hz"),
                    ComponentInstance("inK", ComponentType.LOGIC_INPUT, "K", 4f, 13f, value = "1", properties = mapOf("state" to "1")),
                    ComponentInstance("vcc", ComponentType.VCC, "VCC", 12f, 3f),
                    ComponentInstance("ff", ComponentType.IC_7476, "U1", 12f, 9f),
                    ComponentInstance("outQ", ComponentType.LOGIC_INDICATOR, "Q", 20f, 8f),
                    ComponentInstance("outQn", ComponentType.LED_INDICATOR, "Q'", 20f, 11f)
                ),
                junctions = listOf(WireJunction("jv", 12f, 5f)),
                wires = listOf(
                    w("w1", "inJ", "out", "ff", "j"),
                    w("w2", "clk", "out", "ff", "clk"),
                    w("w3", "inK", "out", "ff", "k"),
                    wJ("w4", "vcc", "vcc", "jv"),
                    wJ("w5", "ff", "pre", "jv"),
                    wJ("w6", "ff", "clr", "jv"),
                    w("w7", "ff", "q", "outQ", "in"),
                    w("w8", "ff", "qn", "outQn", "in")
                )
            )
        }
    ),
    CircuitTemplate(
        id = "binary_counter",
        title = "13. 2-Bit Binary Ripple Counter",
        domain = TemplateDomain.DIGITAL,
        summary = "Two cascaded JK Flip-Flops counting 00 -> 01 -> 10 -> 11 on clock pulses.",
        explanation = "FF0 toggles on input CLK; FF0.Q' clocks FF1 to produce a 2-bit synchronous/ripple binary count.",
        buildPage = {
            CircuitPage(
                id = "p1",
                name = "Binary Counter",
                components = listOf(
                    ComponentInstance("vcc", ComponentType.VCC, "VCC", 4f, 3f),
                    ComponentInstance("clk", ComponentType.CLOCK_INPUT, "CLK", 4f, 9f, value = "1 Hz"),
                    ComponentInstance("ff0", ComponentType.IC_7476, "FF0", 11f, 9f),
                    ComponentInstance("ff1", ComponentType.IC_7476, "FF1", 21f, 9f),
                    ComponentInstance("disp", ComponentType.DIGITAL_DISPLAY, "COUNT", 29f, 9f)
                ),
                junctions = listOf(WireJunction("j1", 7f, 4f)),
                wires = listOf(
                    wJ("w1", "vcc", "vcc", "j1"),
                    wJ("w2", "ff0", "j", "j1"),
                    wJ("w3", "ff0", "k", "j1"),
                    wJ("w4", "ff0", "pre", "j1"),
                    wJ("w5", "ff0", "clr", "j1"),
                    wJ("w6", "ff1", "j", "j1"),
                    wJ("w7", "ff1", "k", "j1"),
                    wJ("w8", "ff1", "pre", "j1"),
                    wJ("w9", "ff1", "clr", "j1"),
                    w("w10", "clk", "out", "ff0", "clk"),
                    w("w11", "ff0", "qn", "ff1", "clk"),
                    w("w12", "ff0", "q", "disp", "b0"),
                    w("w13", "ff1", "q", "disp", "b1")
                )
            )
        }
    ),
    CircuitTemplate(
        id = "seven_segment",
        title = "14. Seven-Segment / Hex Display",
        domain = TemplateDomain.DIGITAL,
        summary = "4-bit Hex Keypad driving a decoded Hexadecimal 7-Segment Display.",
        explanation = "Tap the Keypad to cycle digits 0–F and observe live 4-bit binary decoding on the 7-segment display.",
        buildPage = {
            CircuitPage(
                id = "p1",
                name = "7-Seg Display",
                components = listOf(
                    ComponentInstance("key", ComponentType.KEYPAD, "KEY1", 5f, 8f, value = "0x5", properties = mapOf("hexDigit" to "5")),
                    ComponentInstance("hex", ComponentType.HEX_DISPLAY, "HEX1", 15f, 8f)
                ),
                wires = listOf(
                    w("w1", "key", "b0", "hex", "b0"),
                    w("w2", "key", "b1", "hex", "b1"),
                    w("w3", "key", "b2", "hex", "b2"),
                    w("w4", "key", "b3", "hex", "b3")
                )
            )
        }
    ),
    CircuitTemplate(
        id = "timer_555",
        title = "15. 555 Timer Oscillator",
        domain = TemplateDomain.MIXED,
        summary = "NE555 in Astable mode flashing a Logic LED and feeding a Logic Analyzer.",
        explanation = "With Reset tied HIGH to VCC, Pin 3 OUT toggles continuously, blinking the LED and logging square waves.",
        buildPage = {
            CircuitPage(
                id = "p1",
                name = "555 Timer",
                components = listOf(
                    ComponentInstance("vcc", ComponentType.VCC, "VCC", 11f, 3f),
                    ComponentInstance("gnd", ComponentType.GROUND, "GND", 11f, 13f),
                    ComponentInstance("ic555", ComponentType.IC_555, "U1", 11f, 8f, value = "Astable 2Hz"),
                    ComponentInstance("led", ComponentType.LED_INDICATOR, "LED1", 20f, 7f),
                    ComponentInstance("la", ComponentType.LOGIC_ANALYZER, "LA1", 20f, 11f)
                ),
                junctions = listOf(WireJunction("jv", 6f, 5f), WireJunction("jout", 16f, 8f)),
                wires = listOf(
                    wJ("w1", "vcc", "vcc", "jv"),
                    wJ("w2", "ic555", "vcc", "jv"),
                    wJ("w3", "ic555", "reset", "jv"),
                    w("w4", "ic555", "gnd", "gnd", "gnd"),
                    wJ("w5", "ic555", "out", "jout"),
                    wJ("w6", "led", "in", "jout"),
                    wJ("w7", "la", "ch0", "jout")
                )
            )
        }
    ),
    CircuitTemplate(
        id = "ic_7472_jk_lab",
        title = "21. 7472 Master-Slave JK FF + Probes",
        domain = TemplateDomain.DIGITAL,
        summary = "14-pin DIP 7472 JK Flip-Flop wired in toggle mode driving glowing Green & Red indicator probes.",
        explanation = "Full 14-pin 7472 package with Pin 14 VCC, Pin 7 GND, active-low PRE' & CLR', CLK, J1–J3, K1–K3, and Q / Q' probes.",
        buildPage = {
            CircuitPage(
                id = "p1",
                name = "7472 JK Lab",
                components = listOf(
                    ComponentInstance("vcc", ComponentType.VCC, "VCC", 18f, 3f),
                    ComponentInstance("gnd", ComponentType.GROUND, "GND", 6f, 15f),
                    ComponentInstance("clk", ComponentType.CLOCK_INPUT, "CLK", 24f, 8f, value = "1 Hz"),
                    ComponentInstance("ic7472", ComponentType.IC_7472, "U1", 14f, 9f, value = "7472"),
                    ComponentInstance("pQ", ComponentType.PROBE_GREEN, "Q", 24f, 12f),
                    ComponentInstance("pQn", ComponentType.PROBE_RED, "Q'", 6f, 11f)
                ),
                junctions = listOf(WireJunction("jvcc", 20f, 5f)),
                wires = listOf(
                    wJ("w1", "vcc", "vcc", "jvcc"),
                    wJ("w2", "ic7472", "vcc", "jvcc"),
                    wJ("w3", "ic7472", "pre", "jvcc"),
                    wJ("w4", "ic7472", "clr", "jvcc"),
                    w("w5", "ic7472", "gnd", "gnd", "gnd"),
                    w("w6", "clk", "out", "ic7472", "clk"),
                    w("w7", "ic7472", "q", "pQ", "in"),
                    w("w8", "ic7472", "qn", "pQn", "in")
                )
            )
        }
    ),
    CircuitTemplate(
        id = "ic_7490_bcd_counter",
        title = "22. 7490 Decade Counter + Probes",
        domain = TemplateDomain.DIGITAL,
        summary = "14-pin DIP 7490 BCD Decade Counter driving colored Probes, Decoded Bargraph & Hex Display.",
        explanation = "Counts 0–9 continuously with QA linked to CLKB. Watch the Red/Yellow/Green/Cyan probes and Bargraph light up in sync!",
        buildPage = {
            CircuitPage(
                id = "p1",
                name = "7490 Counter",
                components = listOf(
                    ComponentInstance("clk", ComponentType.CLOCK_INPUT, "CLK", 4f, 6f, value = "1 Hz"),
                    ComponentInstance("gnd", ComponentType.GROUND, "GND", 4f, 14f),
                    ComponentInstance("vcc", ComponentType.VCC, "VCC", 4f, 10f),
                    ComponentInstance("u1", ComponentType.IC_7490, "U1", 14f, 9f, value = "7490"),
                    ComponentInstance("pA", ComponentType.PROBE_GREEN, "QA", 22f, 5f),
                    ComponentInstance("pB", ComponentType.PROBE_YELLOW, "QB", 25f, 5f),
                    ComponentInstance("pC", ComponentType.PROBE_ORANGE, "QC", 28f, 5f),
                    ComponentInstance("pD", ComponentType.PROBE_RED, "QD", 31f, 5f),
                    ComponentInstance("hex", ComponentType.HEX_DISPLAY, "BCD", 26f, 11f),
                    ComponentInstance("bar", ComponentType.DECODED_BAR_GRAPH, "LVL", 33f, 11f)
                ),
                junctions = listOf(
                    WireJunction("jg", 8f, 13f),
                    WireJunction("jqa", 20f, 8f),
                    WireJunction("jqb", 21f, 10f),
                    WireJunction("jqc", 22f, 11f),
                    WireJunction("jqd", 20f, 9f)
                ),
                wires = listOf(
                    w("w1", "clk", "out", "u1", "clka"),
                    w("w2", "vcc", "vcc", "u1", "vcc"),
                    wJ("w3", "gnd", "gnd", "jg"),
                    wJ("w4", "u1", "gnd", "jg"),
                    wJ("w5", "u1", "r01", "jg"),
                    wJ("w6", "u1", "r02", "jg"),
                    wJ("w7", "u1", "r91", "jg"),
                    wJ("w8", "u1", "r92", "jg"),
                    wJ("w9", "u1", "qa", "jqa"),
                    wJ("w10", "u1", "clkb", "jqa"),
                    wJ("w11", "pA", "in", "jqa"),
                    wJ("w12", "hex", "b0", "jqa"),
                    wJ("w13", "bar", "b0", "jqa"),
                    wJ("w14", "u1", "qb", "jqb"),
                    wJ("w15", "pB", "in", "jqb"),
                    wJ("w16", "hex", "b1", "jqb"),
                    wJ("w17", "bar", "b1", "jqb"),
                    wJ("w18", "u1", "qc", "jqc"),
                    wJ("w19", "pC", "in", "jqc"),
                    wJ("w20", "hex", "b2", "jqc"),
                    wJ("w21", "bar", "b2", "jqc"),
                    wJ("w22", "u1", "qd", "jqd"),
                    wJ("w23", "pD", "in", "jqd"),
                    wJ("w24", "hex", "b3", "jqd"),
                    wJ("w25", "bar", "b3", "jqd")
                )
            )
        }
    ),
    CircuitTemplate(
        id = "ic_7490_7447_lab",
        title = "23. 7490 + 7447 BCD 7-Seg Lab",
        domain = TemplateDomain.DIGITAL,
        summary = "Classic university lab: 7490 Decade Counter driving 16-pin 7447 Decoder & 7-Segment Display via wireless CLK Net Labels.",
        explanation = "Demonstrates 16-pin DIP 7447 active-LOW segment decoding (0–9) plus wireless 'CLK' Net Label tags that connect across the schematic.",
        buildPage = {
            CircuitPage(
                id = "p1",
                name = "7490 + 7447 Lab",
                components = listOf(
                    ComponentInstance("note1", ComponentType.TEXT_NOTE, "NOTE1", 18f, 1.5f, value = "Lab: 7490 Counter -> 7447 BCD Decoder"),
                    ComponentInstance("clk", ComponentType.CLOCK_INPUT, "CLK", 3f, 5f, value = "1 Hz"),
                    ComponentInstance("tagClkTx", ComponentType.NET_LABEL, "TAG1", 8f, 5f, value = "CLK"),
                    ComponentInstance("tagClkRx", ComponentType.NET_LABEL, "TAG2", 8f, 8f, value = "CLK"),
                    ComponentInstance("gnd", ComponentType.GROUND, "GND", 4f, 15f),
                    ComponentInstance("vcc", ComponentType.VCC, "VCC", 15f, 3f),
                    ComponentInstance("u1", ComponentType.IC_7490, "U1", 14f, 9f, value = "7490"),
                    ComponentInstance("u2", ComponentType.IC_7447, "U2", 26f, 9f, value = "7447"),
                    ComponentInstance("seg", ComponentType.SEVEN_SEGMENT, "DISP1", 36f, 9f, value = "Common Anode", properties = mapOf("activeLow" to "true"))
                ),
                junctions = listOf(
                    WireJunction("jg", 8f, 14f),
                    WireJunction("jqa", 20f, 8f)
                ),
                wires = listOf(
                    w("w1", "clk", "out", "tagClkTx", "pin"),
                    w("w2", "tagClkRx", "pin", "u1", "clka"),
                    w("w3", "vcc", "vcc", "u1", "vcc"),
                    wJ("w4", "gnd", "gnd", "jg"),
                    wJ("w5", "u1", "gnd", "jg"),
                    wJ("w6", "u1", "r01", "jg"),
                    wJ("w7", "u1", "r02", "jg"),
                    wJ("w8", "u1", "r91", "jg"),
                    wJ("w9", "u1", "r92", "jg"),
                    wJ("w10", "u1", "qa", "jqa"),
                    wJ("w11", "u1", "clkb", "jqa"),
                    wJ("w12", "u2", "a", "jqa"),
                    w("w13", "u1", "qb", "u2", "b"),
                    w("w14", "u1", "qc", "u2", "c"),
                    w("w15", "u1", "qd", "u2", "d"),
                    w("w16", "u2", "sa", "seg", "a"),
                    w("w17", "u2", "sb", "seg", "b"),
                    w("w18", "u2", "sc", "seg", "c"),
                    w("w19", "u2", "sd", "seg", "d"),
                    w("w20", "u2", "se", "seg", "e"),
                    w("w21", "u2", "sf", "seg", "f"),
                    w("w22", "u2", "sg", "seg", "g")
                )
            )
        }
    ),
    CircuitTemplate(
        id = "opamp_741_amp",
        title = "24. LM741 Op-Amp Amplifier",
        domain = TemplateDomain.ANALOG,
        summary = "Non-inverting LM741 operational amplifier (Av = 1 + 2k/1k = 3×) with 2V input → +6.0V output & Safety Fuse.",
        explanation = "Demonstrates real MNA negative feedback solving with LM741: Vin = 2.0V is amplified 3× to Vout = +6.00V across the load.",
        buildPage = {
            CircuitPage(
                id = "p1",
                name = "LM741 Amplifier",
                components = listOf(
                    ComponentInstance("note1", ComponentType.TEXT_NOTE, "NOTE1", 16f, 2f, value = "LM741 Non-Inverting Amp (Gain = 3x)"),
                    ComponentInstance("vin", ComponentType.DC_SOURCE, "Vin", 4f, 8f, value = "2V", properties = mapOf("voltage" to "2.0")),
                    ComponentInstance("fuse1", ComponentType.FUSE, "F1", 9f, 7f, value = "250mA", properties = mapOf("ratingMa" to "250", "blown" to "false")),
                    ComponentInstance("op1", ComponentType.OP_AMP_741, "U1", 16f, 8f, value = "LM741"),
                    ComponentInstance("rf", ComponentType.RESISTOR, "Rf", 16f, 13f, value = "2k"),
                    ComponentInstance("rg", ComponentType.RESISTOR, "Rg", 10f, 13f, value = "1k"),
                    ComponentInstance("rload", ComponentType.RESISTOR, "RL", 24f, 10f, value = "1k", rotationDegrees = 90),
                    ComponentInstance("vm", ComponentType.VOLTMETER, "VM1", 28f, 10f),
                    ComponentInstance("gnd", ComponentType.GROUND, "GND", 16f, 17f)
                ),
                junctions = listOf(
                    WireJunction("jNeg", 13f, 13f),
                    WireJunction("jOut", 22f, 8f),
                    WireJunction("jGnd", 16f, 15.5f)
                ),
                wires = listOf(
                    w("w1", "vin", "pos", "fuse1", "p1"),
                    w("w2", "fuse1", "p2", "op1", "in_pos"),
                    wJ("w3", "op1", "in_neg", "jNeg"),
                    wJ("w4", "rf", "p1", "jNeg"),
                    wJ("w5", "rg", "p2", "jNeg"),
                    wJ("w6", "op1", "out", "jOut"),
                    wJ("w7", "rf", "p2", "jOut"),
                    wJ("w8", "rload", "p1", "jOut"),
                    wJ("w9", "vm", "pos", "jOut"),
                    wJ("w10", "vin", "neg", "jGnd"),
                    wJ("w11", "rg", "p1", "jGnd"),
                    wJ("w12", "rload", "p2", "jGnd"),
                    wJ("w13", "vm", "neg", "jGnd"),
                    wJ("w14", "gnd", "gnd", "jGnd")
                )
            )
        }
    ),
    CircuitTemplate(
        id = "traffic_light_signal_lab",
        title = "25. Traffic Light & LCD Lab",
        domain = TemplateDomain.DIGITAL,
        summary = "Interactive 3-lens Traffic Light + 16×2 LCD Display driven by manual switches and logic gates.",
        explanation = "Toggle switches R, Y, and G to control the traffic light module while the 16×2 LCD displays the live 4-bit signal code.",
        buildPage = {
            CircuitPage(
                id = "p1",
                name = "Traffic Light Lab",
                components = listOf(
                    ComponentInstance("note1", ComponentType.TEXT_NOTE, "NOTE1", 15f, 1.5f, value = "Traffic Signal + 16x2 LCD Monitor"),
                    ComponentInstance("swR", ComponentType.LOGIC_INPUT, "RED_SW", 4f, 5f, value = "1", properties = mapOf("state" to "1")),
                    ComponentInstance("swY", ComponentType.LOGIC_INPUT, "YEL_SW", 4f, 8f, value = "0", properties = mapOf("state" to "0")),
                    ComponentInstance("swG", ComponentType.LOGIC_INPUT, "GRN_SW", 4f, 11f, value = "0", properties = mapOf("state" to "0")),
                    ComponentInstance("vcc", ComponentType.VCC, "VCC", 4f, 14f),
                    ComponentInstance("tfl", ComponentType.TRAFFIC_LIGHT, "SIG1", 13f, 8f),
                    ComponentInstance("lcd", ComponentType.LCD_16X2, "LCD1", 25f, 8f, value = "TRAFFIC CTRL", properties = mapOf("line1" to "TRAFFIC CTRL"))
                ),
                junctions = listOf(
                    WireJunction("jr", 8f, 6f),
                    WireJunction("jy", 8f, 8f),
                    WireJunction("jg", 8f, 10f)
                ),
                wires = listOf(
                    wJ("w1", "swR", "out", "jr"),
                    wJ("w2", "tfl", "r", "jr"),
                    wJ("w3", "lcd", "d2", "jr"),
                    wJ("w4", "swY", "out", "jy"),
                    wJ("w5", "tfl", "y", "jy"),
                    wJ("w6", "lcd", "d1", "jy"),
                    wJ("w7", "swG", "out", "jg"),
                    wJ("w8", "tfl", "g", "jg"),
                    wJ("w9", "lcd", "d0", "jg"),
                    w("w10", "vcc", "vcc", "lcd", "en")
                )
            )
        }
    ),
    CircuitTemplate(
        id = "ic_7485_comparator_lab",
        title = "26. 7485 4-Bit Magnitude Comparator",
        domain = TemplateDomain.DIGITAL,
        summary = "16-pin 7485 DIP IC comparing two 4-bit binary numbers A and B with live Red/Yellow/Green probes.",
        explanation = "Toggle A0/A1 and B0/B1 inputs to watch A>B (Green), A=B (Yellow), and A<B (Red) probes respond instantaneously.",
        buildPage = {
            CircuitPage(
                id = "p1",
                name = "7485 Comparator",
                components = listOf(
                    ComponentInstance("a0", ComponentType.LOGIC_INPUT, "A0", 4f, 4f, value = "1", properties = mapOf("state" to "1")),
                    ComponentInstance("a1", ComponentType.LOGIC_INPUT, "A1", 4f, 7f, value = "0", properties = mapOf("state" to "0")),
                    ComponentInstance("b0", ComponentType.LOGIC_INPUT, "B0", 4f, 10f, value = "0", properties = mapOf("state" to "0")),
                    ComponentInstance("b1", ComponentType.LOGIC_INPUT, "B1", 4f, 13f, value = "0", properties = mapOf("state" to "0")),
                    ComponentInstance("vcc", ComponentType.VCC, "VCC", 14f, 2f),
                    ComponentInstance("gnd", ComponentType.GROUND, "GND", 14f, 17f),
                    ComponentInstance("u1", ComponentType.IC_7485, "U1", 15f, 9f, value = "7485"),
                    ComponentInstance("pGt", ComponentType.PROBE_GREEN, "A>B", 26f, 6f),
                    ComponentInstance("pEq", ComponentType.PROBE_YELLOW, "A=B", 26f, 9f),
                    ComponentInstance("pLt", ComponentType.PROBE_RED, "A<B", 26f, 12f)
                ),
                junctions = listOf(WireJunction("jg", 9f, 15f)),
                wires = listOf(
                    w("w1", "a0", "out", "u1", "a0"),
                    w("w2", "a1", "out", "u1", "a1"),
                    w("w3", "b0", "out", "u1", "b0"),
                    w("w4", "b1", "out", "u1", "b1"),
                    w("w5", "vcc", "vcc", "u1", "vcc"),
                    w("w6", "vcc", "vcc", "u1", "ieq"),
                    wJ("w7", "gnd", "gnd", "jg"),
                    wJ("w8", "u1", "gnd", "jg"),
                    wJ("w9", "u1", "igt", "jg"),
                    wJ("w10", "u1", "ilt", "jg"),
                    w("w11", "u1", "ogt", "pGt", "in"),
                    w("w12", "u1", "oeq", "pEq", "in"),
                    w("w13", "u1", "olt", "pLt", "in")
                )
            )
        }
    )
)

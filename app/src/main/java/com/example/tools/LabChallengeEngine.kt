package com.example.tools

import com.example.engine.analog.AnalogSimulationEngine
import com.example.engine.digital.DigitalSimulationEngine
import com.example.engine.netlist.NetlistBuilder
import com.example.model.CircuitPage
import com.example.model.ComponentInstance
import com.example.model.ComponentType
import com.example.model.LogicLevel
import kotlin.math.abs

enum class ChallengeDifficulty(val label: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    UNIVERSITY_EXAM("Lab Exam")
}

data class ChallengeCheckRow(
    val inputSummary: String,
    val expected: String,
    val actual: String,
    val passed: Boolean
)

data class ChallengeVerificationResult(
    val challengeId: String,
    val passed: Boolean,
    val scorePercent: Int,
    val summary: String,
    val rows: List<ChallengeCheckRow>
)

data class LabChallenge(
    val id: String,
    val title: String,
    val difficulty: ChallengeDifficulty,
    val objective: String,
    val hint: String,
    val requiredInputs: List<String>,
    val requiredOutputs: List<String>,
    val isAnalog: Boolean = false,
    val onlyNandGates: Boolean = false,
    val starterBuilder: () -> CircuitPage
)

object LabChallengeEngine {

    val allChallenges: List<LabChallenge> = listOf(
        LabChallenge(
            id = "ch_half_adder",
            title = "1. Half Adder (Sum & Carry)",
            difficulty = ChallengeDifficulty.BEGINNER,
            objective = "Connect inputs A and B so output SUM = A ⊕ B and CARRY = A · B.",
            hint = "Use one XOR gate for SUM and one AND gate for CARRY. Connect A and B to both gates.",
            requiredInputs = listOf("A", "B"),
            requiredOutputs = listOf("SUM", "CARRY"),
            starterBuilder = {
                CircuitPage(
                    id = "ch_ha",
                    name = "Challenge: Half Adder",
                    components = listOf(
                        ComponentInstance("inA", ComponentType.LOGIC_INPUT, "A", 3f, 4f, value = "0"),
                        ComponentInstance("inB", ComponentType.LOGIC_INPUT, "B", 3f, 8f, value = "0"),
                        ComponentInstance("outS", ComponentType.PROBE_GREEN, "SUM", 16f, 4f),
                        ComponentInstance("outC", ComponentType.PROBE_RED, "CARRY", 16f, 8f)
                    )
                )
            }
        ),
        LabChallenge(
            id = "ch_xor_from_nand",
            title = "2. XOR Using NAND Gates Only",
            difficulty = ChallengeDifficulty.UNIVERSITY_EXAM,
            objective = "Build Y = A ⊕ B using ONLY NAND gates (no XOR/OR/AND/NOT gates allowed).",
            hint = "Classic 4-NAND XOR: N1 = NAND(A,B), N2 = NAND(A,N1), N3 = NAND(B,N1), Y = NAND(N2,N3).",
            requiredInputs = listOf("A", "B"),
            requiredOutputs = listOf("Y"),
            onlyNandGates = true,
            starterBuilder = {
                CircuitPage(
                    id = "ch_xnand",
                    name = "Challenge: NAND-Only XOR",
                    components = listOf(
                        ComponentInstance("inA", ComponentType.LOGIC_INPUT, "A", 2f, 4f, value = "0"),
                        ComponentInstance("inB", ComponentType.LOGIC_INPUT, "B", 2f, 9f, value = "0"),
                        ComponentInstance("outY", ComponentType.PROBE_CYAN, "Y", 18f, 6.5f)
                    )
                )
            }
        ),
        LabChallenge(
            id = "ch_full_adder",
            title = "3. Full Adder (A, B, Cin)",
            difficulty = ChallengeDifficulty.INTERMEDIATE,
            objective = "Build a 1-bit Full Adder: SUM = A ⊕ B ⊕ Cin, COUT = (A·B) + (Cin·(A⊕B)).",
            hint = "Cascade two XOR gates for SUM, then combine two AND gates with an OR gate for COUT.",
            requiredInputs = listOf("A", "B", "Cin"),
            requiredOutputs = listOf("SUM", "COUT"),
            starterBuilder = {
                CircuitPage(
                    id = "ch_fa",
                    name = "Challenge: Full Adder",
                    components = listOf(
                        ComponentInstance("inA", ComponentType.LOGIC_INPUT, "A", 2f, 3f, value = "0"),
                        ComponentInstance("inB", ComponentType.LOGIC_INPUT, "B", 2f, 6.5f, value = "0"),
                        ComponentInstance("inC", ComponentType.LOGIC_INPUT, "Cin", 2f, 10f, value = "0"),
                        ComponentInstance("outS", ComponentType.PROBE_GREEN, "SUM", 19f, 4.5f),
                        ComponentInstance("outCo", ComponentType.PROBE_ORANGE, "COUT", 19f, 8.5f)
                    )
                )
            }
        ),
        LabChallenge(
            id = "ch_mux_2to1",
            title = "4. 2-to-1 Multiplexer",
            difficulty = ChallengeDifficulty.INTERMEDIATE,
            objective = "Route D0 to Y when SEL=0, and D1 to Y when SEL=1: Y = (SEL'·D0) + (SEL·D1).",
            hint = "Use NOT(SEL) AND D0, SEL AND D1, then combine both outputs into an OR gate.",
            requiredInputs = listOf("D0", "D1", "SEL"),
            requiredOutputs = listOf("Y"),
            starterBuilder = {
                CircuitPage(
                    id = "ch_mux",
                    name = "Challenge: 2-to-1 MUX",
                    components = listOf(
                        ComponentInstance("d0", ComponentType.LOGIC_INPUT, "D0", 2f, 3f, value = "0"),
                        ComponentInstance("d1", ComponentType.LOGIC_INPUT, "D1", 2f, 6.5f, value = "0"),
                        ComponentInstance("sel", ComponentType.LOGIC_INPUT, "SEL", 2f, 10f, value = "0"),
                        ComponentInstance("outY", ComponentType.PROBE_YELLOW, "Y", 18f, 6.5f)
                    )
                )
            }
        ),
        LabChallenge(
            id = "ch_2bit_eq",
            title = "5. 2-Bit Equality Comparator",
            difficulty = ChallengeDifficulty.UNIVERSITY_EXAM,
            objective = "Output EQ = 1 ONLY when 2-bit number (A1,A0) equals (B1,B0).",
            hint = "Use XNOR(A1,B1) and XNOR(A0,B0), then AND their two outputs together.",
            requiredInputs = listOf("A1", "A0", "B1", "B0"),
            requiredOutputs = listOf("EQ"),
            starterBuilder = {
                CircuitPage(
                    id = "ch_eq",
                    name = "Challenge: 2-Bit Comparator",
                    components = listOf(
                        ComponentInstance("a1", ComponentType.LOGIC_INPUT, "A1", 2f, 2.5f, value = "0"),
                        ComponentInstance("a0", ComponentType.LOGIC_INPUT, "A0", 2f, 5.5f, value = "0"),
                        ComponentInstance("b1", ComponentType.LOGIC_INPUT, "B1", 2f, 8.5f, value = "0"),
                        ComponentInstance("b0", ComponentType.LOGIC_INPUT, "B0", 2f, 11.5f, value = "0"),
                        ComponentInstance("eq", ComponentType.PROBE_GREEN, "EQ", 18f, 7f)
                    )
                )
            }
        ),
        LabChallenge(
            id = "ch_majority_3",
            title = "6. 3-Input Majority Voter",
            difficulty = ChallengeDifficulty.INTERMEDIATE,
            objective = "Output VOTE = 1 whenever at least 2 of the 3 inputs (A, B, C) are 1.",
            hint = "VOTE = (A·B) + (B·C) + (A·C). Use 3 AND gates and 2 OR gates (or Logic Converter!).",
            requiredInputs = listOf("A", "B", "C"),
            requiredOutputs = listOf("VOTE"),
            starterBuilder = {
                CircuitPage(
                    id = "ch_maj",
                    name = "Challenge: Majority Voter",
                    components = listOf(
                        ComponentInstance("inA", ComponentType.LOGIC_INPUT, "A", 2f, 3f, value = "0"),
                        ComponentInstance("inB", ComponentType.LOGIC_INPUT, "B", 2f, 6.5f, value = "0"),
                        ComponentInstance("inC", ComponentType.LOGIC_INPUT, "C", 2f, 10f, value = "0"),
                        ComponentInstance("outV", ComponentType.PROBE_BLUE, "VOTE", 18f, 6.5f)
                    )
                )
            }
        ),
        LabChallenge(
            id = "ch_voltage_divider",
            title = "7. 5V-to-2.5V Voltage Divider (Analog)",
            difficulty = ChallengeDifficulty.BEGINNER,
            objective = "Build a 5V DC resistive divider with GND so the midpoint Voltmeter or Resistor node sits at 2.5V (±0.25V).",
            hint = "Connect a 5V DC Source -> R1 (1kΩ) -> mid node -> R2 (1kΩ) -> GND. Place a Voltmeter across R2.",
            requiredInputs = listOf("5V DC"),
            requiredOutputs = listOf("2.5V Node"),
            isAnalog = true,
            starterBuilder = {
                CircuitPage(
                    id = "ch_vdiv",
                    name = "Challenge: 2.5V Divider",
                    components = listOf(
                        ComponentInstance("v1", ComponentType.DC_SOURCE, "V1", 3f, 6f, value = "5V"),
                        ComponentInstance("gnd", ComponentType.GROUND, "GND", 3f, 11f, value = "0V"),
                        ComponentInstance("r1", ComponentType.RESISTOR, "R1", 10f, 4f, value = "1k"),
                        ComponentInstance("r2", ComponentType.RESISTOR, "R2", 10f, 8.5f, value = "1k")
                    )
                )
            }
        ),
        LabChallenge(
            id = "ch_safe_led",
            title = "8. Safe LED Driver (No Overcurrent)",
            difficulty = ChallengeDifficulty.BEGINNER,
            objective = "Light an LED from a 5V–9V supply with safe current (5mA–25mA) and ZERO burnout warnings.",
            hint = "Put a 220Ω–470Ω current-limiting resistor in series with the LED between DC Source and GND.",
            requiredInputs = listOf("DC Supply"),
            requiredOutputs = listOf("Safe LED Current"),
            isAnalog = true,
            starterBuilder = {
                CircuitPage(
                    id = "ch_led",
                    name = "Challenge: Safe LED Driver",
                    components = listOf(
                        ComponentInstance("v1", ComponentType.DC_SOURCE, "V1", 3f, 6f, value = "5V"),
                        ComponentInstance("r1", ComponentType.RESISTOR, "R1", 9f, 4f, value = "220"),
                        ComponentInstance("d1", ComponentType.LED, "D1", 14f, 6f, value = "Red"),
                        ComponentInstance("gnd", ComponentType.GROUND, "GND", 3f, 10.5f, value = "0V")
                    )
                )
            }
        )
    )

    fun verify(challenge: LabChallenge, page: CircuitPage): ChallengeVerificationResult {
        if (challenge.isAnalog) {
            return verifyAnalog(challenge, page)
        }
        return verifyDigital(challenge, page)
    }

    private fun verifyDigital(challenge: LabChallenge, page: CircuitPage): ChallengeVerificationResult {
        if (challenge.onlyNandGates) {
            val forbiddenGates = page.components.filter {
                it.type in setOf(
                    ComponentType.GATE_AND, ComponentType.GATE_OR, ComponentType.GATE_NOT,
                    ComponentType.GATE_NOR, ComponentType.GATE_XOR, ComponentType.GATE_XNOR,
                    ComponentType.IC_7402, ComponentType.IC_7404, ComponentType.IC_7408,
                    ComponentType.IC_7432, ComponentType.IC_7486, ComponentType.IC_74266
                )
            }
            if (forbiddenGates.isNotEmpty()) {
                return ChallengeVerificationResult(
                    challengeId = challenge.id,
                    passed = false,
                    scorePercent = 0,
                    summary = "Rule violation: Only NAND gates (or IC 7400) are permitted! Found ${forbiddenGates.first().label}.",
                    rows = emptyList()
                )
            }
        }

        val inputComps = challenge.requiredInputs.map { req ->
            page.components.firstOrNull {
                it.type == ComponentType.LOGIC_INPUT && it.label.equals(req, ignoreCase = true)
            }
        }
        if (inputComps.any { it == null }) {
            val missing = challenge.requiredInputs.filterIndexed { i, _ -> inputComps[i] == null }
            return ChallengeVerificationResult(
                challengeId = challenge.id,
                passed = false,
                scorePercent = 0,
                summary = "Missing Logic Input(s): ${missing.joinToString(", ")}. Load the Starter Bench or label your inputs cleanly.",
                rows = emptyList()
            )
        }
        val resolvedInputs = inputComps.filterNotNull()

        val probeTypes = setOf(
            ComponentType.LOGIC_INDICATOR, ComponentType.LED_INDICATOR, ComponentType.PROBE_RED,
            ComponentType.PROBE_GREEN, ComponentType.PROBE_YELLOW, ComponentType.PROBE_BLUE,
            ComponentType.PROBE_ORANGE, ComponentType.PROBE_CYAN
        )
        val outputComps = challenge.requiredOutputs.map { req ->
            page.components.firstOrNull {
                it.type in probeTypes && it.label.equals(req, ignoreCase = true)
            }
        }
        if (outputComps.any { it == null }) {
            val missing = challenge.requiredOutputs.filterIndexed { i, _ -> outputComps[i] == null }
            return ChallengeVerificationResult(
                challengeId = challenge.id,
                passed = false,
                scorePercent = 0,
                summary = "Missing Output Probe(s): ${missing.joinToString(", ")}.",
                rows = emptyList()
            )
        }
        val resolvedOutputs = outputComps.filterNotNull()

        val engine = DigitalSimulationEngine()
        val totalVectors = 1 shl resolvedInputs.size
        val rows = mutableListOf<ChallengeCheckRow>()
        var passedCount = 0

        for (mask in 0 until totalVectors) {
            val bits = resolvedInputs.indices.map { i ->
                ((mask shr (resolvedInputs.size - 1 - i)) and 1) == 1
            }
            val updatedComps = page.components.map { comp ->
                val idx = resolvedInputs.indexOfFirst { it.id == comp.id }
                if (idx >= 0) {
                    val v = if (bits[idx]) "1" else "0"
                    comp.copy(value = v, properties = comp.properties + ("state" to v))
                } else comp
            }
            val sim = engine.evaluateCombinational(page.copy(components = updatedComps))
            val actualLevels = resolvedOutputs.map { out ->
                sim.pinLevels[NetlistBuilder.pinKey(out.id, "in")] ?: LogicLevel.UNKNOWN
            }
            val expectedBits = expectedDigitalOutputs(challenge.id, bits)
            val rowPass = expectedBits.indices.all { i ->
                val expLevel = if (expectedBits[i]) LogicLevel.ONE else LogicLevel.ZERO
                actualLevels[i] == expLevel
            }
            if (rowPass) passedCount++
            rows.add(
                ChallengeCheckRow(
                    inputSummary = resolvedInputs.indices.joinToString(" ") { i ->
                        "${challenge.requiredInputs[i]}=${if (bits[i]) 1 else 0}"
                    },
                    expected = expectedBits.joinToString("") { if (it) "1" else "0" },
                    actual = actualLevels.joinToString("") { it.symbol },
                    passed = rowPass
                )
            )
        }

        val score = ((passedCount * 100.0) / totalVectors).toInt()
        val allPass = passedCount == totalVectors
        return ChallengeVerificationResult(
            challengeId = challenge.id,
            passed = allPass,
            scorePercent = score,
            summary = if (allPass) {
                "PASSED ($passedCount/$totalVectors vectors verified) — Perfect circuit!"
            } else {
                "$passedCount/$totalVectors test vectors matched ($score%). Check highlighted rows below."
            },
            rows = rows
        )
    }

    private fun expectedDigitalOutputs(challengeId: String, bits: List<Boolean>): List<Boolean> {
        fun b(i: Int) = if (bits.getOrElse(i) { false }) 1 else 0
        return when (challengeId) {
            "ch_half_adder" -> listOf((b(0) xor b(1)) == 1, (b(0) and b(1)) == 1)
            "ch_xor_from_nand" -> listOf((b(0) xor b(1)) == 1)
            "ch_full_adder" -> {
                val sum = b(0) + b(1) + b(2)
                listOf((sum and 1) == 1, sum >= 2)
            }
            "ch_mux_2to1" -> {
                // D0, D1, SEL
                listOf(if (b(2) == 0) bits[0] else bits[1])
            }
            "ch_2bit_eq" -> {
                // A1, A0, B1, B0
                listOf(bits[0] == bits[2] && bits[1] == bits[3])
            }
            "ch_majority_3" -> {
                listOf((b(0) + b(1) + b(2)) >= 2)
            }
            else -> listOf(false)
        }
    }

    private fun verifyAnalog(challenge: LabChallenge, page: CircuitPage): ChallengeVerificationResult {
        val analog = AnalogSimulationEngine().solveDcAndTransient(page)
        return when (challenge.id) {
            "ch_voltage_divider" -> {
                val midVoltages = analog.nodeVoltages.values.filter { abs(it - 2.5) <= 0.25 }
                val hasHalfV = midVoltages.isNotEmpty()
                val hasTwoResistors = page.components.count { it.type == ComponentType.RESISTOR } >= 2
                val ok = analog.converged && hasTwoResistors && hasHalfV
                val closest = analog.nodeVoltages.values.minByOrNull { abs(it - 2.5) }
                ChallengeVerificationResult(
                    challengeId = challenge.id,
                    passed = ok,
                    scorePercent = if (ok) 100 else if (hasTwoResistors) 40 else 0,
                    summary = if (ok) {
                        "PASSED! Midpoint node measured at ${"%.2f".format(midVoltages.first())}V (target 2.50V)."
                    } else {
                        "Target 2.50V midpoint not reached. Closest node: ${closest?.let { "%.2fV".format(it) } ?: "N/A"}. Check series wiring and GND."
                    },
                    rows = listOf(
                        ChallengeCheckRow(
                            inputSummary = "DC Supply + 2 Resistors",
                            expected = "2.50V ±0.25V",
                            actual = closest?.let { "%.2fV".format(it) } ?: "Open",
                            passed = ok
                        )
                    )
                )
            }
            "ch_safe_led" -> {
                val led = page.components.firstOrNull { it.type == ComponentType.LED }
                val ledI = led?.let { abs(analog.componentCurrents[it.id] ?: 0.0) * 1000.0 } ?: 0.0
                val overcurrent = analog.diagnostics.any { it.code == "LED_OVERCURRENT" || it.code == "FUSE_BLOWN" }
                val safeRange = ledI in 4.0..28.0 && !overcurrent
                ChallengeVerificationResult(
                    challengeId = challenge.id,
                    passed = safeRange,
                    scorePercent = if (safeRange) 100 else if (ledI > 0.5) 50 else 0,
                    summary = when {
                        safeRange -> "PASSED! LED safely illuminated at ${"%.1f".format(ledI)} mA with zero burnout warnings."
                        overcurrent -> "LED Overcurrent detected (${"%.1f".format(ledI)} mA)! Increase your series resistor."
                        else -> "LED current is ${"%.1f".format(ledI)} mA (target 5–25 mA). Wire supply -> resistor -> LED -> GND."
                    },
                    rows = listOf(
                        ChallengeCheckRow(
                            inputSummary = "LED Branch Current",
                            expected = "5–25 mA, Safe",
                            actual = "${"%.1f".format(ledI)} mA" + if (overcurrent) " (OVER!)" else "",
                            passed = safeRange
                        )
                    )
                )
            }
            else -> ChallengeVerificationResult(challenge.id, false, 0, "Unknown challenge", emptyList())
        }
    }
}

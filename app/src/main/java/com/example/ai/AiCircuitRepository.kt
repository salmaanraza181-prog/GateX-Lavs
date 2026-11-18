package com.example.ai

import android.util.Log
import com.example.model.CircuitPage
import com.example.model.ComponentCatalog
import com.example.model.ComponentInstance
import com.example.model.ComponentType
import com.example.model.WireEndpoint
import com.example.model.WireSegment
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiCircuitRepository"

sealed interface AiAssistantState {
    data object Idle : AiAssistantState
    data object Loading : AiAssistantState
    data class Streaming(val text: String) : AiAssistantState
    data class Success(val text: String, val generatedPage: CircuitPage? = null) : AiAssistantState
    data class Error(val message: String) : AiAssistantState
}

class AiCircuitRepository {

    private val chatModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val circuitSchema = Schema.obj(
        properties = mapOf(
            "pageName" to Schema.string(description = "Short descriptive circuit name"),
            "explanation" to Schema.string(description = "Concise engineering explanation of how the circuit works and reminder to run simulation to verify"),
            "components" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "id" to Schema.string(description = "Unique component ID like u1, r1, in1"),
                        "type" to Schema.enumeration(
                            values = ComponentType.entries.map { it.name },
                            description = "Exact ComponentType enum name"
                        ),
                        "label" to Schema.string(description = "Short schematic label like R1, U1, A, SUM"),
                        "x" to Schema.double(description = "Grid X coordinate between 3 and 30"),
                        "y" to Schema.double(description = "Grid Y coordinate between 3 and 20"),
                        "value" to Schema.string(description = "Component value like 1k, 5V, 10u, or 0")
                    )
                ),
                description = "List of circuit components"
            ),
            "wires" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "fromComponentId" to Schema.string(description = "Source component ID"),
                        "fromPinId" to Schema.string(description = "Source pin ID from component catalog"),
                        "toComponentId" to Schema.string(description = "Target component ID"),
                        "toPinId" to Schema.string(description = "Target pin ID from component catalog")
                    )
                ),
                description = "List of electrical wire connections between valid component pins"
            )
        )
    )

    private val structuredCircuitModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = circuitSchema
            }
        )
    }

    fun streamEngineeringAdvice(userQuery: String, activePage: CircuitPage): Flow<AiAssistantState> = flow {
        emit(AiAssistantState.Loading)
        val contextSummary = buildCircuitSummary(activePage)
        val prompt = """
            You are an expert electronics engineering lab assistant inside GateX Labs.
            Active circuit schematic summary:
            $contextSummary

            User request:
            $userQuery

            Provide clear, accurate electronics engineering guidance (components, pinouts, Boolean equations, truth tables, or wiring checks).
            Never claim that a circuit has passed simulation unless the simulation engine output was explicitly provided.
        """.trimIndent()

        val sb = StringBuilder()
        try {
            withTimeout(60_000L) {
                chatModel.generateContentStream(prompt).collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    sb.append(chunkText)
                    emit(AiAssistantState.Streaming(sb.toString()))
                }
            }
            if (sb.isNotBlank()) {
                emit(AiAssistantState.Success(sb.toString()))
            } else {
                emit(AiAssistantState.Error("Gemini returned an empty response."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI advice timed out", e)
            emit(AiAssistantState.Error("AI request timed out after 60 seconds. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI advice failed", e)
            emit(AiAssistantState.Error(e.localizedMessage ?: "AI request failed."))
        }
    }

    suspend fun generateStructuredCircuit(description: String): AiAssistantState {
        val pinCheatSheet = """
            Valid pins by component type:
            - LOGIC_INPUT: out
            - CLOCK_INPUT: out
            - LOGIC_INDICATOR, LED_INDICATOR, PROBE_RED, PROBE_GREEN, PROBE_YELLOW, PROBE_BLUE, PROBE_ORANGE, PROBE_CYAN: in
            - PILOT_LAMP: p1, p2
            - BAR_GRAPH, DECODED_BAR_GRAPH: b3, b2, b1, b0
            - GATE_AND, GATE_OR, GATE_NAND, GATE_NOR, GATE_XOR, GATE_XNOR: a, b, y
            - GATE_NOT, GATE_BUFFER: a, y
            - RESISTOR, CAPACITOR, INDUCTOR, TOGGLE_SWITCH: p1, p2
            - DIODE, ZENER_DIODE, LED: anode, cathode
            - DC_SOURCE, AC_SOURCE, BATTERY: pos, neg
            - GROUND: gnd
            - VCC: vcc
            - IC_7472 (DIP-14 JK Master-Slave FF): nc, clr, j1, j2, j3, qn, gnd, q, k1, k2, k3, clk, pre, vcc
            - IC_7474 (DIP-14 Dual D FF): clr, d, clk, pre, q, qn, gnd, 2qn, 2q, 2pre, 2clk, 2d, 2clr, vcc
            - IC_7476 (DIP-14 Dual JK FF): clk, pre, clr, j, k, q, qn, gnd, vcc, 2clk, 2pre, 2clr, 2j, 2k, 2q, 2qn
            - IC_555: gnd, trig, out, reset, vcc
        """.trimIndent()

        val prompt = """
            Design a valid, well-spaced electronic circuit schematic for:
            "$description"

            $pinCheatSheet
            Lay out inputs on the left (x ~ 4), intermediate stages in the middle (x ~ 11..19), and outputs on the right (x ~ 25..29).
            Use exact pin IDs from the list above. Do NOT claim the circuit has been simulated.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                structuredCircuitModel.generateContent(prompt)
            }
            val raw = response.text ?: return AiAssistantState.Error("Model returned empty JSON.")
            val root = JSONObject(raw)
            val pageName = root.optString("pageName", "AI Circuit")
            val explanation = root.optString("explanation", "Generated circuit.")

            val compArr = root.getJSONArray("components")
            val components = mutableListOf<ComponentInstance>()
            val validIds = HashSet<String>()
            for (i in 0 until compArr.length()) {
                val co = compArr.getJSONObject(i)
                val typeName = co.getString("type")
                val type = ComponentType.entries.firstOrNull { it.name == typeName } ?: continue
                val id = co.getString("id")
                val meta = ComponentCatalog.get(type)
                val value = co.optString("value", meta.defaultValue).ifBlank { meta.defaultValue }
                val props = meta.defaultProperties.toMutableMap()
                if (type == ComponentType.DC_SOURCE || type == ComponentType.BATTERY) {
                    val v = ComponentCatalog.parseEngineeringValue(value, 5.0)
                    props["voltage"] = v.toString()
                } else if (type == ComponentType.LOGIC_INPUT) {
                    props["state"] = if (value == "1") "1" else "0"
                }
                components.add(
                    ComponentInstance(
                        id = id,
                        type = type,
                        label = co.optString("label", meta.shortCode),
                        x = co.optDouble("x", 10.0).toFloat().coerceIn(2f, 40f),
                        y = co.optDouble("y", 8.0).toFloat().coerceIn(2f, 30f),
                        value = value,
                        properties = props
                    )
                )
                validIds.add(id)
            }

            val wireArr = root.getJSONArray("wires")
            val wires = mutableListOf<WireSegment>()
            val compMap = components.associateBy { it.id }
            for (i in 0 until wireArr.length()) {
                val wo = wireArr.getJSONObject(i)
                val fromId = wo.getString("fromComponentId")
                val fromPin = wo.getString("fromPinId")
                val toId = wo.getString("toComponentId")
                val toPin = wo.getString("toPinId")
                val fromComp = compMap[fromId] ?: continue
                val toComp = compMap[toId] ?: continue
                val fromValid = ComponentCatalog.get(fromComp.type).pins.any { it.id == fromPin }
                val toValid = ComponentCatalog.get(toComp.type).pins.any { it.id == toPin }
                if (fromValid && toValid) {
                    wires.add(
                        WireSegment(
                            id = "ai_w_${i + 1}",
                            from = WireEndpoint(fromId, fromPin),
                            to = WireEndpoint(toId, toPin)
                        )
                    )
                }
            }

            if (components.isEmpty()) {
                return AiAssistantState.Error("Generated circuit contained no valid components.")
            }

            val page = CircuitPage(
                id = "ai_page_${System.currentTimeMillis()}",
                name = pageName,
                components = components,
                wires = wires
            )
            AiAssistantState.Success(
                text = "$explanation\n\nNote: Generated schematic loaded into workspace. Press Run to verify behavior with the GateX Labs simulation engine.",
                generatedPage = page
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Circuit generation timed out", e)
            AiAssistantState.Error("Circuit generation timed out after 60 seconds.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Circuit generation failed", e)
            AiAssistantState.Error(e.localizedMessage ?: "Failed to generate circuit.")
        }
    }

    private fun buildCircuitSummary(page: CircuitPage): String {
        if (page.components.isEmpty()) return "Empty canvas (0 components)."
        val comps = page.components.joinToString("\n") {
            "- ${it.label} (${it.type.name}, value=${it.value}, id=${it.id})"
        }
        val wires = page.wires.joinToString("\n") {
            "- ${it.from.key()} -> ${it.to.key()}"
        }
        return "Page '${page.name}'\nComponents:\n$comps\nWires:\n$wires"
    }
}

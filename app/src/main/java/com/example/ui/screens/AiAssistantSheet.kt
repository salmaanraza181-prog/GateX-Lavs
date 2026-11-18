package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.AiAssistantState
import com.example.ui.theme.FaultRed
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SignalCyan
import com.example.viewmodel.WorkbenchViewModel

@Composable
fun AiAssistantSheet(
    vm: WorkbenchViewModel,
    onClose: () -> Unit
) {
    val aiState by vm.aiState.collectAsStateWithLifecycle()
    var prompt by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SignalAmber)
            Column {
                Text("AI Circuit Assistant", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Powered by Gemini via Firebase AI Logic (Optional)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = { vm.askAiAssistant("Explain how the current circuit operates and check for potential wiring mistakes.") },
                label = { Text("Explain Circuit") }
            )
            AssistChip(
                onClick = { vm.askAiAssistant("Generate the truth table and Boolean expression for this circuit.") },
                label = { Text("Boolean & Truth Table") }
            )
            AssistChip(
                onClick = {
                    val sample = "Create a full adder circuit using two XOR gates, two AND gates and one OR gate."
                    prompt = sample
                    vm.generateCircuitWithAi(sample)
                },
                label = { Text("Build Full Adder") }
            )
        }

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Ask a question or describe a circuit to build...") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth().testTag("ai_prompt_input")
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { if (prompt.isNotBlank()) vm.askAiAssistant(prompt) },
                modifier = Modifier.weight(1f).testTag("ai_ask_btn")
            ) {
                Text("Ask Assistant")
            }
            Button(
                onClick = { if (prompt.isNotBlank()) vm.generateCircuitWithAi(prompt) },
                modifier = Modifier.weight(1f).testTag("ai_generate_btn")
            ) {
                Text("Generate Circuit")
            }
        }

        when (val state = aiState) {
            is AiAssistantState.Idle -> {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                    Text(
                        text = "Tip: Try \"Create a full adder circuit using two XOR gates, two AND gates and one OR gate\" or ask for component recommendations. Generated circuits are loaded into the canvas for you to verify with the simulation engine.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            is AiAssistantState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator()
                    Text("Analyzing with Gemini...", style = MaterialTheme.typography.bodyMedium)
                }
            }
            is AiAssistantState.Streaming -> {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                    Text(state.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                }
            }
            is AiAssistantState.Success -> {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(state.text, style = MaterialTheme.typography.bodyMedium)
                        if (state.generatedPage != null) {
                            Text(
                                text = "Loaded '${state.generatedPage.name}' (${state.generatedPage.components.size} components). Tap Run in Workspace to verify.",
                                style = MaterialTheme.typography.labelMedium,
                                color = SignalCyan
                            )
                        }
                    }
                }
            }
            is AiAssistantState.Error -> {
                Surface(color = FaultRed.copy(alpha = 0.16f), shape = RoundedCornerShape(12.dp)) {
                    Text(
                        text = "AI Error: ${state.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FaultRed,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

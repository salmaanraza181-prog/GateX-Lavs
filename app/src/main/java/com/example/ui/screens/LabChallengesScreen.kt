package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tools.ChallengeDifficulty
import com.example.tools.ChallengeVerificationResult
import com.example.tools.LabChallenge
import com.example.tools.LabChallengeEngine
import com.example.ui.theme.FaultRed
import com.example.ui.theme.LogicHighGreen
import com.example.ui.theme.LogicHighZViolet
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SignalCyan
import com.example.viewmodel.WorkbenchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabChallengesScreen(
    vm: WorkbenchViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val completedIds by vm.completedChallengeIds.collectAsStateWithLifecycle()
    val lastResult by vm.lastChallengeResult.collectAsStateWithLifecycle()
    val activeProject by vm.activeProject.collectAsStateWithLifecycle()
    val challenges = remember { LabChallengeEngine.allChallenges }

    val progress = if (challenges.isEmpty()) 0f else completedIds.size.toFloat() / challenges.size.toFloat()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lab Challenges & Practical Exam") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("challenges_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Exam Progress Header Card
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SignalCyan.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = SignalAmber)
                                Column {
                                    Text(
                                        text = "University DLD & Analog Auto-Grader",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Active Workspace: ${activeProject.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                color = LogicHighGreen.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "${completedIds.size}/${challenges.size}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = LogicHighGreen,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = LogicHighGreen,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                        Text(
                            text = "Tap 'Load Starter' to open a pre-labeled testbench, wire your solution in Design, then return here and tap 'Verify Circuit'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(challenges, key = { it.id }) { challenge ->
                val isPassed = challenge.id in completedIds
                val matchingResult = lastResult?.takeIf { it.challengeId == challenge.id }
                ChallengeCard(
                    challenge = challenge,
                    isPassed = isPassed,
                    verificationResult = matchingResult,
                    onLoadStarter = { vm.openChallengeStarter(challenge) },
                    onVerifyActive = { vm.verifyActiveChallenge(challenge) }
                )
            }
        }
    }
}

@Composable
private fun ChallengeCard(
    challenge: LabChallenge,
    isPassed: Boolean,
    verificationResult: ChallengeVerificationResult?,
    onLoadStarter: () -> Unit,
    onVerifyActive: () -> Unit
) {
    var showHint by remember(challenge.id) { mutableStateOf(false) }

    val badgeColor = when (challenge.difficulty) {
        ChallengeDifficulty.BEGINNER -> LogicHighGreen
        ChallengeDifficulty.INTERMEDIATE -> SignalAmber
        ChallengeDifficulty.UNIVERSITY_EXAM -> LogicHighZViolet
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isPassed) 1.5.dp else 1.dp,
                color = if (isPassed) LogicHighGreen.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("challenge_card_${challenge.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isPassed) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Challenge passed",
                            tint = LogicHighGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = challenge.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    color = badgeColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = challenge.difficulty.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = challenge.objective,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Inputs: ${challenge.requiredInputs.joinToString(", ")}  •  Outputs: ${challenge.requiredOutputs.joinToString(", ")}",
                style = MaterialTheme.typography.labelSmall,
                color = SignalCyan
            )

            AnimatedVisibility(visible = showHint) {
                Surface(
                    color = SignalAmber.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Hint: ${challenge.hint}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SignalAmber,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onLoadStarter,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("challenge_load_${challenge.id}")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Load Starter")
                }
                OutlinedButton(
                    onClick = onVerifyActive,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("challenge_verify_${challenge.id}")
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(18.dp), tint = LogicHighGreen)
                    Spacer(Modifier.width(4.dp))
                    Text("Verify Circuit")
                }
                TextButton(
                    onClick = { showHint = !showHint },
                    modifier = Modifier.testTag("challenge_hint_${challenge.id}")
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = "Toggle hint", tint = SignalAmber, modifier = Modifier.size(18.dp))
                }
            }

            if (verificationResult != null) {
                val resColor = if (verificationResult.passed) LogicHighGreen else FaultRed
                Surface(
                    color = resColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, resColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (verificationResult.passed) "PASSED" else "NEEDS FIX",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = resColor
                            )
                            Text(
                                text = "Score: ${verificationResult.scorePercent}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = resColor
                            )
                        }
                        Text(
                            text = verificationResult.summary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (verificationResult.rows.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                verificationResult.rows.take(8).forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = row.inputSummary,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.weight(1.4f)
                                        )
                                        Text(
                                            text = "Exp: ${row.expected}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SignalCyan,
                                            modifier = Modifier.weight(0.8f)
                                        )
                                        Text(
                                            text = "Got: ${row.actual} ${if (row.passed) "✓" else "✗"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (row.passed) LogicHighGreen else FaultRed,
                                            modifier = Modifier.weight(0.9f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

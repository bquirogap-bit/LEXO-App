package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.*
import com.example.service.GoldenReport

@Composable
fun DevDiagnosticsScreen(
    config: LearningConfig,
    contentPack: ContentPack?,
    attempts: List<AttemptRecord>,
    goldenReport: GoldenReport?,
    replayDiffs: Map<String, Pair<KuState, KuState>>?,
    onRunGolden: () -> Unit,
    onReplay: () -> Unit,
    onValidateAndImportPack: (String) -> ValidationReport,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Golden Set", "Replay", "KU Manager", "Attempts", "Config")

    var customPackJson by remember { mutableStateOf("") }
    var validationReport by remember { mutableStateOf<ValidationReport?>(null) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Developer Diagnostics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 16.dp) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Golden Set Runner
                        item {
                            Button(
                                onClick = onRunGolden,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("run_golden_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Run Golden Set Test (24 cases)")
                            }
                        }

                        if (goldenReport != null) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("EVALUATOR GOLDEN REPORT", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                        Text("Total cases: ${goldenReport.totalCases}")
                                        Text("False error rate: ${(goldenReport.falseErrorRate * 100).toInt()}% (${goldenReport.falseErrorCount}/${goldenReport.totalCases})")
                                        Text("Missed error rate: ${(goldenReport.missedErrorRate * 100).toInt()}% (${goldenReport.missedErrorCount}/${goldenReport.totalCases})")
                                        Text("Uncertain rate: ${(goldenReport.uncertainRate * 100).toInt()}% (${goldenReport.uncertainCount}/${goldenReport.totalCases})")
                                    }
                                }
                            }

                            item {
                                Text("Per-Case Details:", fontWeight = FontWeight.Bold)
                            }

                            items(goldenReport.caseResults) { cr ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(cr.caseId, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                            Text(if (cr.passed) "PASS" else "FAIL", color = if (cr.passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                                        }
                                        Text("Input: «${cr.response}»", style = MaterialTheme.typography.bodySmall)
                                        Text("Expected: ${cr.expectedOutcome} · Actual: ${cr.actualOutcome}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Replay View
                        item {
                            Button(
                                onClick = onReplay,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("run_replay_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Replay All Evidence Events Through Reducer")
                            }
                        }

                        if (replayDiffs != null) {
                            item {
                                Text("Replay Results (Deterministic State Check):", fontWeight = FontWeight.Bold)
                            }

                            items(replayDiffs.entries.toList()) { (kuId, pair) ->
                                val (oldState, newState) = pair
                                val isIdentical = oldState.status == newState.status && oldState.validErrors == newState.validErrors

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("KU: $kuId", fontWeight = FontWeight.Bold)
                                        Text("Status: ${oldState.status} -> ${newState.status} (${if (isIdentical) "Deterministic Match" else "Changed"})")
                                        Text("Errors: ${oldState.validErrors} · Opportunities: ${oldState.opportunities}")
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // KU Manager & Validator
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("CURRENT INVENTORY", fontWeight = FontWeight.Bold)
                                    Text("Active KUs: ${contentPack?.knowledge_units?.size ?: 0}")
                                    Text("Active Exercises: ${contentPack?.exercises?.size ?: 0}")
                                    Text("Active Error Signatures: ${contentPack?.error_signatures?.size ?: 0}")
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("ADMISSION RULES (Section 5.4 & 5.5)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Text("• Every KU must have ≥6 exercises across ≥3 distinct contexts.", style = MaterialTheme.typography.bodySmall)
                                    Text("• At least 2 exercise types and ≥2 constructed exercises per KU.", style = MaterialTheme.typography.bodySmall)
                                    Text("• Fill-in-blank prompts must contain exactly one '___'.", style = MaterialTheme.typography.bodySmall)
                                    Text("• Deterministic exercises must have non-empty accepted answers.", style = MaterialTheme.typography.bodySmall)
                                    Text("• Every error signature must resolve to its KU.", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Import / Replace Content Pack (JSON):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                                OutlinedTextField(
                                    value = customPackJson,
                                    onValueChange = { customPackJson = it },
                                    placeholder = { Text("Paste Content Pack JSON here...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Button(
                                    onClick = {
                                        validationReport = onValidateAndImportPack(customPackJson)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = customPackJson.isNotBlank(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Validate & Load Content Pack")
                                }
                            }
                        }

                        if (validationReport != null) {
                            item {
                                val rep = validationReport!!
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (rep.isValid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = if (rep.isValid) "VALIDATION PASSED ✓" else "VALIDATION FAILED ✗",
                                            fontWeight = FontWeight.Bold,
                                            color = if (rep.isValid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        if (rep.isValid) {
                                            Text("Loaded ${rep.totalKus} KUs, ${rep.totalExercises} exercises, and ${rep.totalSignatures} signatures into active session runtime.")
                                        } else {
                                            for (err in rep.errors) {
                                                Text("• $err", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // Attempts Log Inspector
                        if (attempts.isEmpty()) {
                            item {
                                Text("No attempts recorded yet.")
                            }
                        } else {
                            items(attempts) { att ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(att.attemptId, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                            Text(att.source, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Text("Exercise: ${att.exerciseId}", style = MaterialTheme.typography.bodySmall)
                                        Text("Response: «${att.response}»", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                        if (att.guardrailLog.isNotEmpty()) {
                                            Text("Guardrails: ${att.guardrailLog.joinToString { it.rule }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (att.kuChanges.isNotEmpty()) {
                                            Text("State Transitions: ${att.kuChanges.map { "${it.kuId}: ${it.from}->${it.to}" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    4 -> {
                        // LearningConfig Viewer
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("ACTIVE CONFIG: ${config.version}", fontWeight = FontWeight.Bold)
                                    Text("Evidence errorMinConfidence: ${config.evidence.errorMinConfidence}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                    Text("Evidence halfLifeDays: ${config.evidence.halfLifeDays}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                    Text("Recurrence windowDays: ${config.recurrence.windowDays}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                    Text("Consolidation minSuccesses: ${config.consolidation.minSuccesses}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                    Text("Consolidation delayHours: ${config.consolidation.delayHours}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                    Text("Scheduler ladderDays: ${config.scheduler.ladderDays}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                    Text("Planner sessionSize: ${config.planner.sessionSize}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                    Text("Planner problem/due/explore: ${config.planner.problemQuota}/${config.planner.dueQuota}/${config.planner.exploreQuota}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

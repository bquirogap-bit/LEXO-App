package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.ContentPack
import com.example.domain.KuState
import com.example.domain.KuStatus

@Composable
fun PatternsScreen(
    contentPack: ContentPack?,
    kuStates: Map<String, KuState>,
    onSelectKu: (String) -> Unit,
    onBack: () -> Unit
) {
    val now = System.currentTimeMillis()
    var selectedLevel by remember { mutableStateOf("All") }
    val levels = listOf("All", "A1", "A2", "B1", "B2")

    fun matchesLevel(kuId: String): Boolean {
        if (selectedLevel == "All") return true
        val ku = contentPack?.knowledge_units?.firstOrNull { it.id == kuId }
        return ku?.band_introduced?.equals(selectedLevel, ignoreCase = true) == true
    }

    val focusGroup = kuStates.values.filter {
        matchesLevel(it.kuId) && it.status in listOf(KuStatus.RECURRENT, KuStatus.UNSTABLE)
    }

    val recoveringGroup = kuStates.values.filter {
        matchesLevel(it.kuId) && it.status in listOf(KuStatus.RECOVERY, KuStatus.SLIP)
    }

    val consolidatedGroup = kuStates.values.filter {
        matchesLevel(it.kuId) && it.status == KuStatus.CONSOLIDATED
    }

    val notYetExploredGroup = kuStates.values.filter {
        matchesLevel(it.kuId) && (it.status == KuStatus.UNTESTED || (it.opportunities == 0 && it.status == KuStatus.OBSERVED))
    }

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
                Column {
                    Text(
                        text = "Linguistic Patterns",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "A1 to B2 Behavioral Inventory",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Level Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(levels) { lvl ->
                        FilterChip(
                            selected = selectedLevel == lvl,
                            onClick = { selectedLevel = lvl },
                            label = { Text(if (lvl == "All") "All Levels (32)" else "Level $lvl") },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Focus section
            if (focusGroup.isNotEmpty()) {
                item {
                    Text(
                        text = "Focus (stabilizing)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                items(focusGroup) { state ->
                    val ku = contentPack?.knowledge_units?.firstOrNull { it.id == state.kuId }
                    PatternRowCard(ku?.name ?: state.kuId, ku?.band_introduced ?: "B1", state, false) { onSelectKu(state.kuId) }
                }
            }

            // Recovering section
            if (recoveringGroup.isNotEmpty()) {
                item {
                    Text(
                        text = "Recovering",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                items(recoveringGroup) { state ->
                    val ku = contentPack?.knowledge_units?.firstOrNull { it.id == state.kuId }
                    PatternRowCard(ku?.name ?: state.kuId, ku?.band_introduced ?: "B1", state, false) { onSelectKu(state.kuId) }
                }
            }

            // Consolidated section
            if (consolidatedGroup.isNotEmpty()) {
                item {
                    Text(
                        text = "Consolidated (Automated)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                items(consolidatedGroup) { state ->
                    val ku = contentPack?.knowledge_units?.firstOrNull { it.id == state.kuId }
                    val isDue = state.dueAt != null && state.dueAt <= now
                    PatternRowCard(ku?.name ?: state.kuId, ku?.band_introduced ?: "B1", state, isDue) { onSelectKu(state.kuId) }
                }
            }

            // Not yet explored count
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Not yet explored",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (selectedLevel == "All") "In inventory: A1, A2, B1, B2" else "Level $selectedLevel inventory",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${notYetExploredGroup.size} areas",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun PatternRowCard(
    name: String,
    level: String,
    state: KuState,
    isDueForCheck: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("pattern_row_${state.kuId}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = level,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isDueForCheck) {
                        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                            Text(
                                "due for a check",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Text(
                        text = state.status.plainNameEs,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            val evidenceLine = if (state.validErrors > 0) {
                "${state.validErrors} errors observed across ${state.opportunities} opportunities"
            } else if (state.opportunities > 0) {
                "${state.successRun} consecutive successes across ${state.contextsSucceeded.size} contexts"
            } else {
                "No opportunities observed yet"
            }

            Text(
                text = evidenceLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.LexoRepository
import com.example.domain.Consolidation
import com.example.domain.EvidenceEvent
import com.example.domain.KnowledgeUnit
import com.example.domain.KuState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PatternDetailScreen(
    kuId: String,
    ku: KnowledgeUnit?,
    state: KuState?,
    repository: LexoRepository,
    onDisputeEvent: (String) -> Unit,
    onBack: () -> Unit
) {
    val eventsFlow = repository.getEvidenceForKuFlow(kuId)
    val events by eventsFlow.collectAsState(initial = emptyList())

    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US)

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
                    text = ku?.name ?: kuId,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
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
            // Behavior summary card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "TARGET BEHAVIOR",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = ku?.behavior ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                        ku?.l1_note?.get("es")?.let { note ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Sensor L1 (Español): $note",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Status and contexts summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Current status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(state?.status?.plainNameEs ?: "no explorado", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        Column {
                            Text("Contexts seen", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${state?.contextsSucceeded?.size ?: 0} contexts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        Column {
                            Text("Next review", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val reviewStr = state?.dueAt?.let { sdf.format(Date(it)) } ?: "None scheduled"
                            Text(reviewStr, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Timeline of learner's own sentences
            item {
                Text(
                    text = "Timeline of your sentences",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (events.isEmpty()) {
                item {
                    Text(
                        text = "No production evidence recorded for this pattern yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(events) { ev ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val mark = if (ev.outcome == "correct") "✓" else if (ev.outcome == "error") "•" else "?"
                                    Text(
                                        text = mark,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (ev.outcome == "correct") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "${ev.context} · ${sdf.format(Date(ev.at))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (ev.status == "disputed") {
                                    Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                                        Text("disputed", style = MaterialTheme.typography.labelSmall)
                                    }
                                } else {
                                    TextButton(
                                        onClick = { onDisputeEvent(ev.id) },
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.height(28.dp).testTag("dispute_event_${ev.id}")
                                    ) {
                                        Text("Dispute", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            if (!ev.quote.isNullOrBlank()) {
                                Text(
                                    text = "«${ev.quote}»",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

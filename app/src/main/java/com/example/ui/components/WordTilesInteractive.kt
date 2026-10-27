package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.Exercise
import java.util.UUID

data class TileItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isDistractor: Boolean = false
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordTilesInteractive(
    exercise: Exercise,
    onSentenceConstructed: (sentence: String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Generate tiles with target tokens + pedagogical distractors
    val initialPool = remember(exercise.id) {
        val canonical = exercise.accepted.firstOrNull() ?: exercise.prompt
        val cleanWords = canonical
            .replace(Regex("[.,!?;:]"), "")
            .split(" ")
            .filter { it.isNotBlank() }

        val tiles = cleanWords.map { TileItem(text = it) }.toMutableList()

        // Pedagogical distractors based on target KU
        val distractors = when {
            exercise.ku.contains("agree") -> listOf("am", "are")
            exercise.ku.contains("depend") -> listOf("of")
            exercise.ku.contains("since_for") -> if (canonical.contains("for", ignoreCase = true)) listOf("since") else listOf("for")
            exercise.ku.contains("future_be_going_to") -> listOf("will", "be")
            exercise.ku.contains("past_simple_aux") -> listOf("went", "saw", "did")
            exercise.ku.contains("tps_s") -> listOf("like", "work", "agree")
            exercise.ku.contains("subject_explicit") -> listOf("is", "was")
            exercise.ku.contains("connectors") -> listOf("despite", "although")
            exercise.ku.contains("modal") -> listOf("to")
            exercise.ku.contains("discuss") -> listOf("about")
            else -> listOf("the", "at")
        }

        for (d in distractors.take(2)) {
            if (!cleanWords.any { it.equals(d, ignoreCase = true) }) {
                tiles.add(TileItem(text = d, isDistractor = true))
            }
        }

        tiles.shuffled()
    }

    val selectedTiles = remember(exercise.id) { mutableStateListOf<TileItem>() }
    val availableTiles = remember(exercise.id) {
        mutableStateListOf<TileItem>().apply { addAll(initialPool) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Answer Area (Constructed sentence slot)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 90.dp)
                .testTag("tiles_answer_slot"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tu oración:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (selectedTiles.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                availableTiles.addAll(selectedTiles)
                                selectedTiles.clear()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).testTag("reset_tiles_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Restablecer", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (selectedTiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Toca las palabras para formar la frase en orden",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (tile in selectedTiles) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .clickable {
                                        selectedTiles.remove(tile)
                                        availableTiles.add(tile)
                                    }
                            ) {
                                Text(
                                    text = tile.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Available Bank Tiles
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Palabras disponibles:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (tile in availableTiles) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .clickable {
                                availableTiles.remove(tile)
                                selectedTiles.add(tile)
                            }
                            .testTag("tile_${tile.text}")
                    ) {
                        Text(
                            text = tile.text,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Submit Button
        Button(
            onClick = {
                val sentence = selectedTiles.joinToString(" ") { it.text }
                onSentenceConstructed(sentence)
            },
            enabled = selectedTiles.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("submit_tiles_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Comprobar oración construida", fontWeight = FontWeight.SemiBold)
        }
    }
}

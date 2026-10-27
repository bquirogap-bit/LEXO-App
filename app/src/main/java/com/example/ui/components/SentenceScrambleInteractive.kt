package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.Exercise
import java.util.UUID

data class ScrambleChunk(
    val id: String = UUID.randomUUID().toString(),
    val text: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SentenceScrambleInteractive(
    exercise: Exercise,
    onSentenceReconstructed: (sentence: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialChunks = remember(exercise.id) {
        val canonical = exercise.accepted.firstOrNull() ?: exercise.prompt
        val tokens = canonical.split(" ").filter { it.isNotBlank() }

        // Chunk logically (pairing small prepositions with subsequent noun or verb where natural)
        val chunks = mutableListOf<ScrambleChunk>()
        var i = 0
        while (i < tokens.size) {
            if (i < tokens.size - 1 && tokens[i].length <= 3 && !tokens[i].contains("'")) {
                chunks.add(ScrambleChunk(text = "${tokens[i]} ${tokens[i + 1]}"))
                i += 2
            } else {
                chunks.add(ScrambleChunk(text = tokens[i]))
                i += 1
            }
        }

        // Shuffle ensuring it's not identical to original
        var shuffled = chunks.shuffled()
        if (shuffled == chunks && chunks.size > 1) {
            shuffled = chunks.reversed()
        }
        shuffled
    }

    val selectedChunks = remember(exercise.id) { mutableStateListOf<ScrambleChunk>() }
    val availableChunks = remember(exercise.id) {
        mutableStateListOf<ScrambleChunk>().apply { addAll(initialChunks) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Reconstructed sentence slot
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 90.dp)
                .testTag("scramble_answer_slot"),
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
                        text = "Oración reordenada:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (selectedChunks.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                availableChunks.addAll(selectedChunks)
                                selectedChunks.clear()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).testTag("reset_scramble_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Restablecer", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (selectedChunks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Toca los fragmentos en el orden correcto para reconstruir la frase",
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
                        for (chunk in selectedChunks) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                                modifier = Modifier
                                    .clickable {
                                        selectedChunks.remove(chunk)
                                        availableChunks.add(chunk)
                                    }
                            ) {
                                Text(
                                    text = chunk.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Available Bank Chunks
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Fragmentos a ordenar:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (chunk in availableChunks) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .clickable {
                                availableChunks.remove(chunk)
                                selectedChunks.add(chunk)
                            }
                            .testTag("scramble_chunk_${chunk.text}")
                    ) {
                        Text(
                            text = chunk.text,
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
                val sentence = selectedChunks.joinToString(" ") { it.text }
                onSentenceReconstructed(sentence)
            },
            enabled = selectedChunks.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("submit_scramble_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Comprobar orden sintáctico", fontWeight = FontWeight.SemiBold)
        }
    }
}

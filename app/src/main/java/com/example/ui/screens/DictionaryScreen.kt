package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ContentPack
import com.example.domain.VocabularyWord
import com.example.domain.WordKnowledgeMatcher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(
    words: List<VocabularyWord>,
    contentPack: ContentPack?,
    onAddWord: (word: String, defEs: String, defEn: String, partOfSpeech: String, relatedKuId: String?, contexts: List<String>, example: String) -> Unit,
    onDeleteWord: (wordId: String) -> Unit,
    onOpenPatternDetail: (kuId: String) -> Unit,
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var filterPart by remember { mutableStateOf("All") }

    val partsOfSpeech = listOf("All", "verb", "noun", "adjective", "connector", "preposition")

    val filteredWords = if (filterPart == "All") {
        words
    } else {
        words.filter { it.partOfSpeech.equals(filterPart, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Word Review Bucket",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Word → Context → KU → SRS → Transfer",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_word_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add word")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Category filter row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(partsOfSpeech) { part ->
                    FilterChip(
                        selected = filterPart == part,
                        onClick = { filterPart = part },
                        label = { Text(part.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            if (filteredWords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Your Word Bucket is empty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Add vocabulary terms you want to master in active production. Words automatically link to relevant KUs and appear in your spaced practice sessions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { showAddDialog = true }) {
                            Text("Add your first word")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = "${filteredWords.size} words in active repetition",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(filteredWords, key = { it.id }) { word ->
                        val relatedKu = contentPack?.knowledge_units?.firstOrNull { it.id == word.relatedKuId }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("word_card_${word.word}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = word.word,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = word.partOfSpeech,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDeleteWord(word.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete word",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = word.definitionEs,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "“${word.exampleSentence}”",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }

                                // Related KU & SRS Level
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (relatedKu != null) {
                                        Text(
                                            text = "Linked KU: ${relatedKu.name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.clickable { onOpenPatternDetail(relatedKu.id) }
                                        )
                                    } else {
                                        Text(
                                            text = "Vocabulary term",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = "SRS Step ${word.stepIndex} · ${word.reviewCount} reviews",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddWordDialog(
            contentPack = contentPack,
            onDismiss = { showAddDialog = false },
            onConfirm = { word, defEs, defEn, part, kuId, contexts, ex ->
                onAddWord(word, defEs, defEn, part, kuId, contexts, ex)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddWordDialog(
    contentPack: ContentPack?,
    onDismiss: () -> Unit,
    onConfirm: (word: String, defEs: String, defEn: String, partOfSpeech: String, relatedKuId: String?, contexts: List<String>, example: String) -> Unit
) {
    var wordInput by remember { mutableStateOf("") }
    var defEsInput by remember { mutableStateOf("") }
    var defEnInput by remember { mutableStateOf("") }
    var partOfSpeech by remember { mutableStateOf("verb") }
    var selectedKuId by remember { mutableStateOf<String?>(null) }
    var exampleInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Add to Word Bucket")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = wordInput,
                    onValueChange = { input ->
                        wordInput = input
                        if (input.isNotBlank()) {
                            val analysis = WordKnowledgeMatcher.analyzeWord(input, contentPack)
                            defEsInput = analysis.definitionEs
                            defEnInput = analysis.definitionEn
                            partOfSpeech = analysis.partOfSpeech
                            selectedKuId = analysis.relatedKuId
                            exampleInput = analysis.exampleSentence
                        }
                    },
                    label = { Text("Word (e.g. negotiate, schedule, depend)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("word_input_field"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = defEsInput,
                    onValueChange = { defEsInput = it },
                    label = { Text("Significado en español") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = exampleInput,
                    onValueChange = { exampleInput = it },
                    label = { Text("Example sentence") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedKuId != null) {
                    val matchingKu = contentPack?.knowledge_units?.firstOrNull { it.id == selectedKuId }
                    Text(
                        text = "Linked to KU: ${matchingKu?.name ?: selectedKuId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (wordInput.isNotBlank()) {
                        onConfirm(
                            wordInput.trim(),
                            defEsInput.ifBlank { "Significado en estudio" },
                            defEnInput.ifBlank { "Meaning in context" },
                            partOfSpeech,
                            selectedKuId,
                            listOf("Business", "Meetings"),
                            exampleInput.ifBlank { "Used in professional communication." }
                        )
                    }
                },
                enabled = wordInput.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_word_button")
            ) {
                Text("Add word")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

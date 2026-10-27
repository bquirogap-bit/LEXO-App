package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.*
import com.example.ui.components.*
import com.example.ui.theme.LexoHighlightSpan
import com.example.ui.theme.LexoHighlightSpanBg

@Composable
fun SessionScreen(
    session: PlannedSession?,
    currentIndex: Int,
    contentPack: ContentPack?,
    feedback: Feedback?,
    isEvaluating: Boolean,
    vocabularyWords: List<VocabularyWord> = emptyList(),
    onAddVocabularyWord: (word: String, defEs: String, defEn: String, pos: String, kuId: String?, contexts: List<String>, ex: String) -> Unit = { _, _, _, _, _, _, _ -> },
    onDeleteVocabularyWord: (wordId: String) -> Unit = {},
    onSubmit: (response: String, latencyMs: Long, hintsUsed: Int, modality: String) -> Unit,
    onSubmitRewrite: (response: String) -> Unit,
    onDispute: () -> Unit,
    onNextItem: () -> Unit,
    onQuitSession: () -> Unit
) {
    if (session == null || session.items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val item = session.items.getOrNull(currentIndex) ?: return
    val exercise = contentPack?.exercises?.firstOrNull { it.id == item.exerciseId } ?: return
    val ku = contentPack?.knowledge_units?.firstOrNull { it.id == item.kuId }

    var inputText by remember(currentIndex) { mutableStateOf("") }
    var inputModality by remember(currentIndex) {
        mutableStateOf(
            when (item.exerciseFormat) {
                "word_tiles" -> "word_tiles"
                "sentence_scramble" -> "scramble"
                "speaking" -> "spoken"
                else -> if (item.modality == "speaking") "spoken" else "typed"
            }
        )
    }
    var startTime by remember(currentIndex) { mutableLongStateOf(System.currentTimeMillis()) }
    var showWhyThis by remember(currentIndex) { mutableStateOf(false) }
    var showClue by remember(currentIndex) { mutableStateOf(false) }
    var hintsUsedCount by remember(currentIndex) { mutableIntStateOf(0) }

    var rewriteText by remember(currentIndex) { mutableStateOf("") }
    var showRewriteInput by remember(currentIndex) { mutableStateOf(false) }

    // Contextual Word Lookup state
    var currentWordLookup by remember(currentIndex) { mutableStateOf<WordKnowledgeMatcher.ContextualWordLookup?>(null) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onQuitSession) {
                    Icon(Icons.Default.Close, contentDescription = "Close session")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${currentIndex + 1} of ${session.items.size}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (session.isFocusedPractice) {
                        Text(
                            text = "Focused Practice · ${ku?.name ?: item.kuId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.width(48.dp)) // balance layout
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Badges row: Modality + Format + Word + "Why this?"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Modality switch badge
                val modalityLabel = when (inputModality) {
                    "spoken" -> "Speaking"
                    "word_tiles" -> "Word Tiles"
                    "scramble" -> "Scramble"
                    else -> "Writing"
                }

                AssistChip(
                    onClick = {
                        // Toggle between writing and speaking if standard format
                        if (item.exerciseFormat !in listOf("word_tiles", "sentence_scramble")) {
                            inputModality = if (inputModality == "spoken") "typed" else "spoken"
                        }
                    },
                    label = {
                        Text(
                            text = modalityLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = when (inputModality) {
                                "spoken" -> Icons.Default.Mic
                                "word_tiles" -> Icons.Default.Extension
                                "scramble" -> Icons.Default.Shuffle
                                else -> Icons.Default.Edit
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (inputModality == "spoken")
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                // Format badge
                val formatLabel = when (item.exerciseFormat) {
                    "speaking" -> "Voz activa"
                    "word_tiles" -> "Word Tiles"
                    "sentence_scramble" -> "Sentence Scramble"
                    "translation" -> "Traducción"
                    "open_production" -> "Producción abierta"
                    "error_correction" -> "Detección de error"
                    "pattern_retrieval" -> "Recuperación de patrón"
                    else -> "En contexto"
                }
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(text = formatLabel, style = MaterialTheme.typography.labelSmall)
                    }
                )

                // Associated word badge if applicable
                if (item.associatedWord != null) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(text = "Word: ${item.associatedWord}", style = MaterialTheme.typography.labelSmall)
                        },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    )
                }
            }

            // "Why this?" rationale chip
            SuggestionChip(
                onClick = { showWhyThis = !showWhyThis },
                label = {
                    Text(
                        text = if (showWhyThis) item.reasonTextEs else "Why this task?",
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                icon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("why_this_chip")
            )

            // Context brief & action descriptor
            val contextBrief = remember(currentIndex, inputModality) {
                PedagogicalContextHelper.getContext(
                    ku = ku,
                    exercise = exercise,
                    format = item.exerciseFormat,
                    modality = inputModality
                )
            }

            // Expected Action Banner
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expected_action_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = when {
                            inputModality == "spoken" -> Icons.Default.Mic
                            item.exerciseFormat == "word_tiles" -> Icons.Default.Extension
                            item.exerciseFormat == "sentence_scramble" -> Icons.Default.Shuffle
                            exercise.type == "translation" -> Icons.Default.Translate
                            exercise.type == "fill_in_blank" -> Icons.Default.EditNote
                            else -> Icons.Default.Create
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Acción: ${contextBrief.expectedActionLabel}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = contextBrief.expectedActionDetail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // "Entender antes de empezar" optional expandable brief card
            PedagogicalBriefCard(
                contextBrief = contextBrief,
                initiallyExpanded = false,
                modifier = Modifier.fillMaxWidth()
            )

            // Context and situation card with word tap support
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = exercise.context.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    ClickableContextualText(
                        text = exercise.situation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Normal,
                        onWordClick = { tappedWord ->
                            val lookup = WordKnowledgeMatcher.lookupContextualWord(
                                rawWord = tappedWord,
                                sentence = exercise.situation,
                                kuId = item.kuId,
                                pack = contentPack
                            )
                            currentWordLookup = lookup
                            // Auto-save into Word Bucket
                            val existing = vocabularyWords.firstOrNull { it.word.equals(lookup.word, ignoreCase = true) }
                            if (existing == null) {
                                onAddVocabularyWord(
                                    lookup.word,
                                    lookup.translationEs,
                                    lookup.contextualMeaningEs,
                                    lookup.partOfSpeech,
                                    lookup.relatedKuId,
                                    listOf(exercise.context),
                                    exercise.situation
                                )
                            }
                        }
                    )
                }
            }

            // Exercise Prompt with word tap support (tap any word for contextual dictionary)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Toca cualquier palabra para ver su traducción y guardarla:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ClickableContextualText(
                    text = exercise.prompt,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    onWordClick = { tappedWord ->
                        val lookup = WordKnowledgeMatcher.lookupContextualWord(
                            rawWord = tappedWord,
                            sentence = exercise.prompt,
                            kuId = item.kuId,
                            pack = contentPack
                        )
                        currentWordLookup = lookup
                        // Auto-save into Word Bucket
                        val existing = vocabularyWords.firstOrNull { it.word.equals(lookup.word, ignoreCase = true) }
                        if (existing == null) {
                            onAddVocabularyWord(
                                lookup.word,
                                lookup.translationEs,
                                lookup.contextualMeaningEs,
                                lookup.partOfSpeech,
                                lookup.relatedKuId,
                                listOf(exercise.context),
                                exercise.prompt
                            )
                        }
                    }
                )
            }

            // Compact Contextual Word Card (pops up when user taps any word in the exercise)
            AnimatedVisibility(visible = currentWordLookup != null) {
                currentWordLookup?.let { lookup ->
                    val existingWord = vocabularyWords.firstOrNull { it.word.equals(lookup.word, ignoreCase = true) }
                    val isSaved = existingWord != null

                    ContextualWordCard(
                        lookup = lookup,
                        isSavedInBucket = isSaved,
                        onToggleSave = {
                            if (existingWord != null) {
                                onDeleteVocabularyWord(existingWord.id)
                            } else {
                                onAddVocabularyWord(
                                    lookup.word,
                                    lookup.translationEs,
                                    lookup.contextualMeaningEs,
                                    lookup.partOfSpeech,
                                    lookup.relatedKuId,
                                    listOf(exercise.context),
                                    lookup.originSentence
                                )
                            }
                        },
                        onClose = { currentWordLookup = null }
                    )
                }
            }

            // Input / Interaction Area (if no feedback yet)
            if (feedback == null) {
                // Scaffolding: "Need a clue?" button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            if (!showClue) {
                                showClue = true
                                hintsUsedCount++
                            } else {
                                showClue = false
                            }
                        },
                        modifier = Modifier.testTag("need_clue_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (showClue) "Ocultar pista" else "¿Necesitas una pista?",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    if (hintsUsedCount > 0) {
                        Text(
                            text = "Modo asistido con pista",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Scaffolding card (Pedagogical Bullets)
                AnimatedVisibility(visible = showClue) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Andamiaje Pedagógico (Pistas de regla y foco)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            val clues = ScaffoldingHelper.getClues(ku, exercise, "es")
                            for (bullet in clues) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "• ",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = bullet,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Interaction Mode Rendering: Word Tiles, Sentence Scramble, Integrated Speaking, or Typed
                when {
                    item.exerciseFormat == "word_tiles" -> {
                        WordTilesInteractive(
                            exercise = exercise,
                            onSentenceConstructed = { constructed ->
                                inputText = constructed
                                val latency = System.currentTimeMillis() - startTime
                                onSubmit(constructed, latency, hintsUsedCount, "word_tiles")
                            }
                        )
                    }

                    item.exerciseFormat == "sentence_scramble" -> {
                        SentenceScrambleInteractive(
                            exercise = exercise,
                            onSentenceReconstructed = { reconstructed ->
                                inputText = reconstructed
                                val latency = System.currentTimeMillis() - startTime
                                onSubmit(reconstructed, latency, hintsUsedCount, "scramble")
                            }
                        )
                    }

                    inputModality == "spoken" -> {
                        IntegratedSpeakingController(
                            onTranscriptionConfirmed = { transcription ->
                                inputText = transcription
                                val latency = System.currentTimeMillis() - startTime
                                onSubmit(transcription, latency, hintsUsedCount, "spoken")
                            },
                            onFallbackToTyping = {
                                inputModality = "typed"
                            }
                        )
                    }

                    else -> {
                        // Standard Writing Mode
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Escribe tu respuesta en inglés...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("response_input_field"),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isEvaluating,
                            singleLine = false,
                            maxLines = 4,
                            trailingIcon = {
                                IconButton(
                                    onClick = { inputModality = "spoken" },
                                    modifier = Modifier.testTag("switch_to_speaking_icon")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Cambiar a voz",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )

                        Button(
                            onClick = {
                                val latency = System.currentTimeMillis() - startTime
                                onSubmit(inputText, latency, hintsUsedCount, "typed")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_button"),
                            enabled = inputText.isNotBlank() && !isEvaluating,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isEvaluating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Enviar respuesta", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                // FEEDBACK VIEW
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("feedback_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Title / message
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = feedback.message,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (hintsUsedCount > 0) {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Asistido con pista",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Highlighted learner production
                        if (feedback.quote != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Tu respuesta ($inputModality):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                val annotatedResponse = buildAnnotatedString {
                                    val quote = feedback.quote
                                    val full = inputText
                                    val idx = full.indexOf(quote, ignoreCase = true)
                                    if (idx >= 0) {
                                        append(full.substring(0, idx))
                                        withStyle(
                                            SpanStyle(
                                                background = LexoHighlightSpanBg,
                                                color = LexoHighlightSpan,
                                                fontWeight = FontWeight.Bold
                                            )
                                        ) {
                                            append(full.substring(idx, idx + quote.length))
                                        }
                                        append(full.substring(idx + quote.length))
                                    } else {
                                        append(full)
                                    }
                                }

                                Text(
                                    text = annotatedResponse,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        // What fits
                        if (feedback.correction != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Forma adecuada:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = feedback.correction,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Authored pattern explanation
                        if (feedback.patternNote != null) {
                            Text(
                                text = feedback.patternNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Orthographic note (e.g. typo)
                        if (feedback.orthographicNote != null) {
                            Text(
                                text = feedback.orthographicNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Recurrence alert notice
                        if (feedback.isRecurrenceAlert) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Este calco recurrente volvió a aparecer en otro momento. Trabajaremos en estabilizarlo con práctica dirigida.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        // Optional correction rewrite input
                        AnimatedVisibility(visible = showRewriteInput) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = rewriteText,
                                    onValueChange = { rewriteText = it },
                                    placeholder = { Text("Escribe la oración corregida...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Button(
                                    onClick = {
                                        onSubmitRewrite(rewriteText)
                                        showRewriteInput = false
                                    },
                                    enabled = rewriteText.isNotBlank()
                                ) {
                                    Text("Guardar reescritura")
                                }
                            }
                        }

                        // Actions row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (feedback.quote != null) {
                                TextButton(
                                    onClick = { showRewriteInput = !showRewriteInput },
                                    modifier = Modifier.testTag("try_corrected_button")
                                ) {
                                    Text("Probar corregida", style = MaterialTheme.typography.labelMedium)
                                }

                                TextButton(
                                    onClick = onDispute,
                                    modifier = Modifier.testTag("dispute_button")
                                ) {
                                    Text(
                                        "Considero correcta mi respuesta",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Next Button
                Button(
                    onClick = onNextItem,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("next_item_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (currentIndex + 1 >= session.items.size) "Finalizar sesión" else "Siguiente ejercicio",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

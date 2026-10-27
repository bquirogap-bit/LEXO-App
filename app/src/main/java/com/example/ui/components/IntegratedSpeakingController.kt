package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class SpeakingState {
    READY,
    RECORDING,
    LOCKED,
    PROCESSING,
    RESULT,
    ERROR
}

@Composable
fun IntegratedSpeakingController(
    onTranscriptionConfirmed: (transcription: String) -> Unit,
    onFallbackToTyping: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var state by remember { mutableStateOf(SpeakingState.READY) }
    var recognizedText by remember { mutableStateOf("") }
    var partialText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var recordDurationSeconds by remember { mutableIntStateOf(0) }
    var isEditingResult by remember { mutableStateOf(false) }
    var editableText by remember { mutableStateOf("") }

    // Pulsing animation for recording state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Elapsed timer during RECORDING / LOCKED
    LaunchedEffect(state) {
        if (state == SpeakingState.RECORDING || state == SpeakingState.LOCKED) {
            recordDurationSeconds = 0
            while (state == SpeakingState.RECORDING || state == SpeakingState.LOCKED) {
                delay(1000)
                recordDurationSeconds++
            }
        }
    }

    // Android SpeechRecognizer instance
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    DisposableEffect(Unit) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            val sr = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = sr
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    if (state == SpeakingState.RECORDING) {
                        state = SpeakingState.PROCESSING
                    }
                }
                override fun onError(error: Int) {
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No se detectaron palabras en inglés. Intenta hablar más claro o revisa el audio."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tiempo de espera agotado sin detectar voz."
                        SpeechRecognizer.ERROR_AUDIO -> "Error de captura de audio."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permiso de micrófono no disponible."
                        else -> "Reconocimiento de voz no disponible en este momento."
                    }
                    errorMessage = msg
                    state = SpeakingState.ERROR
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val result = matches?.firstOrNull() ?: partialText
                    if (result.isNotBlank()) {
                        recognizedText = result
                        editableText = result
                        state = SpeakingState.RESULT
                    } else {
                        errorMessage = "No se pudo transcribir con claridad. Puedes editar el texto o reintentar."
                        state = SpeakingState.ERROR
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.firstOrNull()?.let {
                        partialText = it
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        onDispose {
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startListening() {
        errorMessage = null
        partialText = ""
        recognizedText = ""
        val sr = speechRecognizer
        if (sr != null) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
                sr.startListening(intent)
                state = SpeakingState.RECORDING
            } catch (e: Exception) {
                errorMessage = "No se pudo iniciar el reconocedor de voz."
                state = SpeakingState.ERROR
            }
        } else {
            // SpeechRecognizer unavailable on container/emulator
            errorMessage = "Servicio de reconocimiento de voz no disponible en este dispositivo. Puedes escribir tu respuesta."
            state = SpeakingState.ERROR
        }
    }

    fun stopListeningAndProcess() {
        state = SpeakingState.PROCESSING
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelListening() {
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        state = SpeakingState.READY
        partialText = ""
        recognizedText = ""
        errorMessage = null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (state) {
            SpeakingState.READY -> {
                Text(
                    text = "Mantén presionado para hablar o desliza ↑ para bloquear",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // WhatsApp-style hold & swipe-to-lock button
                var totalDragY by remember { mutableFloatStateOf(0f) }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("mic_hold_button")
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    startListening()
                                    totalDragY = 0f
                                    val released = tryAwaitRelease()
                                    if (released && state == SpeakingState.RECORDING) {
                                        stopListeningAndProcess()
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { totalDragY = 0f },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDragY += dragAmount.y
                                    // Swipe up past threshold locks recording
                                    if (totalDragY < -100f && state == SpeakingState.RECORDING) {
                                        state = SpeakingState.LOCKED
                                    }
                                },
                                onDragEnd = {
                                    if (state == SpeakingState.RECORDING) {
                                        stopListeningAndProcess()
                                    }
                                },
                                onDragCancel = {
                                    cancelListening()
                                }
                            )
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Hold to record",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                TextButton(
                    onClick = onFallbackToTyping,
                    modifier = Modifier.testTag("switch_to_typing_button")
                ) {
                    Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Prefiero escribir con teclado", style = MaterialTheme.typography.labelMedium)
                }
            }

            SpeakingState.RECORDING -> {
                // Recording with live pulsation & swipe up hint
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                        Text(
                            text = "Grabando: ${recordDurationSeconds}s",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (partialText.isNotBlank()) {
                        Text(
                            text = "“$partialText”",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "↑ Desliza hacia arriba para bloquear",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .scale(pulseScale)
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Recording",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
            }

            SpeakingState.LOCKED -> {
                // WhatsApp-style locked controls: Cancel, Stop, Send
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Grabación bloqueada (${recordDurationSeconds}s)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (partialText.isNotBlank()) {
                            Text(
                                text = "“$partialText”",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cancel button
                            FilledTonalIconButton(
                                onClick = { cancelListening() },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                modifier = Modifier.testTag("cancel_speech_button")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Cancelar")
                            }

                            // Stop button
                            FilledTonalIconButton(
                                onClick = { stopListeningAndProcess() },
                                modifier = Modifier.testTag("stop_speech_button")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Detener")
                            }

                            // Send button
                            Button(
                                onClick = { stopListeningAndProcess() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("send_speech_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Procesar")
                            }
                        }
                    }
                }
            }

            SpeakingState.PROCESSING -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    Text(
                        text = "Transcribiendo audio en inglés...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SpeakingState.RESULT -> {
                // Result card allowing user to verify, edit transcription before submitting, or re-record
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("speaking_result_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Transcripción de voz",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            TextButton(
                                onClick = { isEditingResult = !isEditingResult },
                                modifier = Modifier.testTag("edit_transcription_toggle")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (isEditingResult) "Ver texto" else "Editar")
                            }
                        }

                        if (isEditingResult) {
                            OutlinedTextField(
                                value = editableText,
                                onValueChange = { editableText = it },
                                label = { Text("Corrige si el reconocimiento tuvo algún desliz") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("transcription_edit_field"),
                                shape = RoundedCornerShape(10.dp)
                            )
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "“${editableText.ifBlank { recognizedText }}”",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    state = SpeakingState.READY
                                },
                                modifier = Modifier.testTag("rerecord_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Volver a grabar")
                            }

                            Button(
                                onClick = {
                                    val finalAnswer = editableText.ifBlank { recognizedText }
                                    if (finalAnswer.isNotBlank()) {
                                        onTranscriptionConfirmed(finalAnswer)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                enabled = editableText.isNotBlank() || recognizedText.isNotBlank(),
                                modifier = Modifier.testTag("confirm_transcription_button")
                            ) {
                                Text("Confirmar y Enviar")
                            }
                        }
                    }
                }
            }

            SpeakingState.ERROR -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Aviso de Reconocimiento",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = errorMessage ?: "No se pudo transcribir el audio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { state = SpeakingState.READY }
                            ) {
                                Text("Reintentar hablar")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = onFallbackToTyping,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Escribir respuesta")
                            }
                        }
                    }
                }
            }
        }
    }
}

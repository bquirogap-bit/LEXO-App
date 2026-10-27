package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LexoDatabase
import com.example.data.LexoRepository
import com.example.data.UserPreferencesEntity
import com.example.domain.*
import com.example.service.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppScreen {
    HOME,
    SESSION,
    SESSION_SUMMARY,
    PATTERNS,
    PATTERN_DETAIL,
    SETTINGS,
    DEV_DIAGNOSTICS,
    IMPROVEMENT_HUD,
    DICTIONARY,
    MASTERY
}

class LexoViewModel(application: Application) : AndroidViewModel(application) {

    private val db = LexoDatabase.getDatabase(application)
    val config = LearningConfig()
    val repository = LexoRepository(db, config)

    private val _contentPack = MutableStateFlow<ContentPack?>(null)
    val contentPack: StateFlow<ContentPack?> = _contentPack.asStateFlow()

    private val _goldenCases = MutableStateFlow<List<GoldenCase>>(emptyList())
    val goldenCases: StateFlow<List<GoldenCase>> = _goldenCases.asStateFlow()

    private val _demoEvents = MutableStateFlow<List<EvidenceEvent>>(emptyList())

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedKuId = MutableStateFlow<String?>(null)
    val selectedKuId: StateFlow<String?> = _selectedKuId.asStateFlow()

    private val _activeSession = MutableStateFlow<PlannedSession?>(null)
    val activeSession: StateFlow<PlannedSession?> = _activeSession.asStateFlow()

    private val _currentItemIndex = MutableStateFlow(0)
    val currentItemIndex: StateFlow<Int> = _currentItemIndex.asStateFlow()

    private val _currentFeedback = MutableStateFlow<Feedback?>(null)
    val currentFeedback: StateFlow<Feedback?> = _currentFeedback.asStateFlow()

    private val _currentAttemptRecord = MutableStateFlow<AttemptRecord?>(null)
    val currentAttemptRecord: StateFlow<AttemptRecord?> = _currentAttemptRecord.asStateFlow()

    private val _isEvaluating = MutableStateFlow(false)
    val isEvaluating: StateFlow<Boolean> = _isEvaluating.asStateFlow()

    private val _kuStates = MutableStateFlow<Map<String, KuState>>(emptyMap())
    val kuStates: StateFlow<Map<String, KuState>> = _kuStates.asStateFlow()

    val vocabularyWords: StateFlow<List<VocabularyWord>> = repository.vocabularyWordsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _improvementIssues = MutableStateFlow<List<ImprovementIssue>>(emptyList())
    val improvementIssues: StateFlow<List<ImprovementIssue>> = _improvementIssues.asStateFlow()

    private val _masteryMilestones = MutableStateFlow<List<MasteryMilestone>>(emptyList())
    val masteryMilestones: StateFlow<List<MasteryMilestone>> = _masteryMilestones.asStateFlow()

    val userPreferences: StateFlow<UserPreferencesEntity> = repository.userPreferencesFlow
        .map { it ?: UserPreferencesEntity() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferencesEntity())

    val allAttempts: StateFlow<List<AttemptRecord>> = repository.allAttemptsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _goldenReport = MutableStateFlow<GoldenReport?>(null)
    val goldenReport: StateFlow<GoldenReport?> = _goldenReport.asStateFlow()

    private val _replayDiffs = MutableStateFlow<Map<String, Pair<KuState, KuState>>?>(null)
    val replayDiffs: StateFlow<Map<String, Pair<KuState, KuState>>?> = _replayDiffs.asStateFlow()

    private val _sessionSummary = MutableStateFlow<List<String>>(emptyList())
    val sessionSummary: StateFlow<List<String>> = _sessionSummary.asStateFlow()

    // Pipeline
    private lateinit var pipeline: AttemptPipeline
    private lateinit var evaluator: EvaluatorPort

    init {
        loadAssetsAndInit()
    }

    private fun loadAssetsAndInit() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            try {
                val seedJson = context.assets.open("lexo_seed_mvp.json").bufferedReader().use { it.readText() }
                val pack = ContentLoader.parseContentPack(seedJson)
                _contentPack.value = pack

                val goldenJson = context.assets.open("eval_golden.json").bufferedReader().use { it.readText() }
                _goldenCases.value = ContentLoader.parseGoldenCases(goldenJson)

                val demoJson = context.assets.open("demo_events.json").bufferedReader().use { it.readText() }
                _demoEvents.value = ContentLoader.parseDemoEvents(demoJson)

                evaluator = GeminiEvaluator(fallbackEvaluator = ScriptedEvaluator(_goldenCases.value))
                pipeline = AttemptPipeline(repository, pack, evaluator, config)

                // Initialize starter words if empty
                repository.getAllWords()

                refreshKuStates()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun refreshKuStates() {
        val pack = _contentPack.value ?: return
        val states = repository.getAllKuStates(pack)
        _kuStates.value = states
        refreshAnalytics()
    }

    private suspend fun refreshAnalytics() {
        val pack = _contentPack.value ?: return
        val allEvents = repository.getAllEvidence()
        val now = System.currentTimeMillis()
        val words = repository.getAllWords()

        _improvementIssues.value = ImprovementDetector.detectIssues(
            pack = pack,
            kuStates = _kuStates.value,
            allEvents = allEvents,
            config = config,
            now = now
        )

        _masteryMilestones.value = MasteryCalculator.calculateMilestones(
            pack = pack,
            kuStates = _kuStates.value,
            allEvents = allEvents,
            vocabularyWords = words
        )
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun openPatternDetail(kuId: String) {
        _selectedKuId.value = kuId
        _currentScreen.value = AppScreen.PATTERN_DETAIL
    }

    fun backToHome() {
        _selectedKuId.value = null
        _currentScreen.value = AppScreen.HOME
    }

    fun startSession() {
        viewModelScope.launch {
            val pack = _contentPack.value ?: return@launch
            refreshKuStates()
            val allEvents = repository.getAllEvidence()
            val now = System.currentTimeMillis()
            val words = repository.getAllWords()

            val session = Planner.planSession(
                pack = pack,
                kuStates = _kuStates.value,
                allEvents = allEvents,
                config = config,
                now = now,
                vocabularyWords = words
            )

            _activeSession.value = session
            _currentItemIndex.value = 0
            _currentFeedback.value = null
            _currentAttemptRecord.value = null
            repository.saveSession(session, currentIndex = 0, isFinished = false)
            _currentScreen.value = AppScreen.SESSION
        }
    }

    fun startFocusedPractice(kuId: String) {
        viewModelScope.launch {
            val pack = _contentPack.value ?: return@launch
            refreshKuStates()
            val allEvents = repository.getAllEvidence()
            val now = System.currentTimeMillis()

            val session = Planner.planFocusedSession(
                kuId = kuId,
                pack = pack,
                kuStates = _kuStates.value,
                allEvents = allEvents,
                config = config,
                now = now
            )

            _activeSession.value = session
            _currentItemIndex.value = 0
            _currentFeedback.value = null
            _currentAttemptRecord.value = null
            repository.saveSession(session, currentIndex = 0, isFinished = false)
            _currentScreen.value = AppScreen.SESSION
        }
    }

    fun submitAnswer(response: String, latencyMs: Long, hintsUsed: Int = 0, modality: String = "typed") {
        val session = _activeSession.value ?: return
        val index = _currentItemIndex.value
        val item = session.items.getOrNull(index) ?: return

        viewModelScope.launch {
            _isEvaluating.value = true
            try {
                val attemptId = "att_${UUID.randomUUID().toString().take(10)}"
                val record = pipeline.processAttempt(
                    attemptId = attemptId,
                    sessionId = session.sessionId,
                    exerciseId = item.exerciseId,
                    rawResponse = response,
                    latencyMs = latencyMs,
                    hintsUsed = hintsUsed,
                    kind = "scored",
                    modality = modality
                )
                _currentAttemptRecord.value = record
                _currentFeedback.value = record.feedback

                // If this item was linked to a vocabulary word, update word SRS
                if (item.associatedWord != null) {
                    val matchingWord = repository.getAllWords().firstOrNull { it.word.equals(item.associatedWord, ignoreCase = true) }
                    if (matchingWord != null) {
                        val isCorrect = record.feedback?.tone != "error" && record.feedback?.message?.contains("correct", ignoreCase = true) == true
                        repository.advanceWordReview(matchingWord.id, isCorrect)
                    }
                }

                refreshKuStates()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isEvaluating.value = false
            }
        }
    }

    fun addVocabularyWord(
        word: String,
        definitionEs: String,
        definitionEn: String,
        partOfSpeech: String,
        relatedKuId: String?,
        contexts: List<String>,
        exampleSentence: String
    ) {
        viewModelScope.launch {
            val newWord = VocabularyWord(
                id = "word_${UUID.randomUUID().toString().take(8)}",
                word = word.trim().lowercase(),
                definitionEs = definitionEs.ifBlank { "Término en aprendizaje" },
                definitionEn = definitionEn.ifBlank { "Vocabulary item" },
                partOfSpeech = partOfSpeech,
                relatedKuId = relatedKuId,
                naturalContexts = if (contexts.isEmpty()) listOf("Business", "Meetings") else contexts,
                exampleSentence = exampleSentence.ifBlank { "Used in professional communication." },
                stepIndex = 0,
                dueAt = System.currentTimeMillis() + 86400_000L,
                reviewCount = 0,
                lastReviewedAt = null,
                createdAt = System.currentTimeMillis()
            )
            repository.saveWord(newWord)
            refreshAnalytics()
        }
    }

    fun deleteVocabularyWord(wordId: String) {
        viewModelScope.launch {
            repository.deleteWord(wordId)
            refreshAnalytics()
        }
    }

    fun submitCorrectionRewrite(response: String) {
        val session = _activeSession.value ?: return
        val index = _currentItemIndex.value
        val item = session.items.getOrNull(index) ?: return

        viewModelScope.launch {
            try {
                val attemptId = "att_rewrite_${UUID.randomUUID().toString().take(8)}"
                pipeline.processAttempt(
                    attemptId = attemptId,
                    sessionId = session.sessionId,
                    exerciseId = item.exerciseId,
                    rawResponse = response,
                    kind = "correction_rewrite"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun disputeCurrentAttempt() {
        viewModelScope.launch {
            val record = _currentAttemptRecord.value ?: return@launch
            val allEv = repository.getAllEvidence().filter { it.attemptId == record.attemptId }
            for (ev in allEv) {
                repository.disputeEvidence(ev.id)
            }
            refreshKuStates()
            _currentFeedback.value = _currentFeedback.value?.copy(
                message = "Respuesta disputada y excluida de tu estado."
            )
        }
    }

    fun disputeSpecificEvidence(evidenceId: String) {
        viewModelScope.launch {
            repository.disputeEvidence(evidenceId)
            refreshKuStates()
        }
    }

    fun nextSessionItem() {
        val session = _activeSession.value ?: return
        val nextIdx = _currentItemIndex.value + 1

        if (nextIdx < session.items.size) {
            _currentItemIndex.value = nextIdx
            _currentFeedback.value = null
            _currentAttemptRecord.value = null
            viewModelScope.launch {
                repository.saveSession(session, currentIndex = nextIdx)
            }
        } else {
            // Finish session
            viewModelScope.launch {
                repository.finishSession(session.sessionId)
                refreshKuStates()

                val summaryLines = if (session.isFocusedPractice && session.focusedKuId != null) {
                    val targetKu = _contentPack.value?.knowledge_units?.firstOrNull { it.id == session.focusedKuId }
                    val targetKuName = targetKu?.name ?: session.focusedKuId
                    val allEv = repository.getAllEvidence().filter { it.sessionId == session.sessionId }
                    val targetEvents = allEv.filter { it.kuId == session.focusedKuId }
                    val targetErrors = targetEvents.filter { it.outcome == "error" }
                    val targetCorrect = targetEvents.filter { it.outcome == "correct" }

                    val diagnosticStatus = when {
                        targetErrors.isEmpty() && targetCorrect.isNotEmpty() ->
                            "Resultado: El patrón objetivo '$targetKuName' muestra mejora consistente en esta sesión. Se mantiene en seguimiento activo para consolidar en distintos días y contextos (no se declara dominio con una sola sesión breve)."
                        targetErrors.isNotEmpty() && targetCorrect.isEmpty() ->
                            "Resultado: El calco/error objetivo '$targetKuName' persiste en los contextos evaluados. Necesita nueva revisión y práctica con andamiaje guiado."
                        else ->
                            "Resultado: Progreso parcial sobre '$targetKuName' con aciertos y deslices residuales. Se programará una nueva revisión para su estabilización."
                    }

                    listOf(
                        "Práctica enfocada completada (${session.items.size} ejercicios)",
                        "Patrón objetivo evaluado: $targetKuName",
                        diagnosticStatus,
                        "Los resultados se han registrado en tu modelo de estado y diagnóstico de errores."
                    )
                } else {
                    val practicedKus = session.items.mapNotNull { item ->
                        _contentPack.value?.knowledge_units?.firstOrNull { it.id == item.kuId }?.name
                    }.distinct()

                    listOf(
                        "Practicaste: ${practicedKus.joinToString(" · ")}",
                        "Sesión completada con evaluación en base a evidencia de tu producción.",
                        "Siguiente revisión programada según consolidación de cada patrón."
                    )
                }
                _sessionSummary.value = summaryLines
                _currentScreen.value = AppScreen.SESSION_SUMMARY
            }
        }
    }

    fun setFeedbackLanguage(lang: String) {
        viewModelScope.launch {
            repository.updateFeedbackLang(lang)
        }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            val pack = _contentPack.value ?: return@launch
            repository.resetDemo(_demoEvents.value, pack)
            refreshKuStates()
            _currentScreen.value = AppScreen.HOME
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            refreshKuStates()
            _currentScreen.value = AppScreen.HOME
        }
    }

    fun runGoldenSet() {
        viewModelScope.launch {
            val pack = _contentPack.value ?: return@launch
            val report = GoldenRunner.runGoldenSet(_goldenCases.value, pack, evaluator)
            _goldenReport.value = report
        }
    }

    fun validateAndImportPack(jsonString: String): ValidationReport {
        return try {
            val parsedPack = ContentLoader.parseContentPack(jsonString)
            val report = ContentValidator.validate(parsedPack)
            if (report.isValid) {
                _contentPack.value = parsedPack
                pipeline = AttemptPipeline(repository, parsedPack, evaluator, config)
                viewModelScope.launch {
                    refreshKuStates()
                }
            }
            report
        } catch (e: Exception) {
            ValidationReport(
                isValid = false,
                errors = listOf("JSON parsing error: ${e.message}"),
                warnings = emptyList(),
                totalKus = 0,
                totalExercises = 0,
                totalSignatures = 0
            )
        }
    }

    fun replayAllEvidence() {
        viewModelScope.launch {
            val pack = _contentPack.value ?: return@launch
            val diffs = repository.replayAllEvents(pack)
            _replayDiffs.value = diffs
            refreshKuStates()
        }
    }
}

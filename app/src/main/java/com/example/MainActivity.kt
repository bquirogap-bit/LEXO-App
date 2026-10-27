package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.AppScreen
import com.example.ui.LexoViewModel
import com.example.ui.screens.*
import com.example.ui.theme.LexoTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LexoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemStatusBar()

        setContent {
            LexoTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.navigationBars
                ) { innerPadding ->
                    LexoApp(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemStatusBar()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemStatusBar()
        }
    }

    private fun hideSystemStatusBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.statusBars())
    }
}

@Composable
fun LexoApp(
    viewModel: LexoViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val contentPack by viewModel.contentPack.collectAsState()
    val kuStates by viewModel.kuStates.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val currentIndex by viewModel.currentItemIndex.collectAsState()
    val feedback by viewModel.currentFeedback.collectAsState()
    val isEvaluating by viewModel.isEvaluating.collectAsState()
    val selectedKuId by viewModel.selectedKuId.collectAsState()
    val userPrefs by viewModel.userPreferences.collectAsState()
    val allAttempts by viewModel.allAttempts.collectAsState()
    val goldenReport by viewModel.goldenReport.collectAsState()
    val replayDiffs by viewModel.replayDiffs.collectAsState()
    val sessionSummary by viewModel.sessionSummary.collectAsState()
    val improvementIssues by viewModel.improvementIssues.collectAsState()
    val vocabularyWords by viewModel.vocabularyWords.collectAsState()
    val masteryMilestones by viewModel.masteryMilestones.collectAsState()

    // Handle Back Press for sub-screens
    BackHandler(enabled = currentScreen != AppScreen.HOME) {
        when (currentScreen) {
            AppScreen.PATTERN_DETAIL -> viewModel.navigateTo(AppScreen.PATTERNS)
            AppScreen.SESSION -> viewModel.navigateTo(AppScreen.HOME)
            else -> viewModel.backToHome()
        }
    }

    when (currentScreen) {
        AppScreen.HOME -> {
            HomeScreen(
                contentPack = contentPack,
                kuStates = kuStates,
                improvementIssues = improvementIssues,
                onStartSession = { viewModel.startSession() },
                onStartFocusedPractice = { kuId -> viewModel.startFocusedPractice(kuId) },
                onOpenImprovementHud = { viewModel.navigateTo(AppScreen.IMPROVEMENT_HUD) },
                onOpenDictionary = { viewModel.navigateTo(AppScreen.DICTIONARY) },
                onOpenMastery = { viewModel.navigateTo(AppScreen.MASTERY) },
                onOpenPatterns = { viewModel.navigateTo(AppScreen.PATTERNS) },
                onOpenPatternDetail = { kuId -> viewModel.openPatternDetail(kuId) },
                onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) },
                onOpenDev = { viewModel.navigateTo(AppScreen.DEV_DIAGNOSTICS) }
            )
        }

        AppScreen.IMPROVEMENT_HUD -> {
            ImprovementHudScreen(
                issues = improvementIssues,
                onStartFocusedPractice = { kuId -> viewModel.startFocusedPractice(kuId) },
                onOpenPatternDetail = { kuId -> viewModel.openPatternDetail(kuId) },
                onBack = { viewModel.backToHome() }
            )
        }

        AppScreen.DICTIONARY -> {
            DictionaryScreen(
                words = vocabularyWords,
                contentPack = contentPack,
                onAddWord = { word, defEs, defEn, part, kuId, contexts, ex ->
                    viewModel.addVocabularyWord(word, defEs, defEn, part, kuId, contexts, ex)
                },
                onDeleteWord = { wordId -> viewModel.deleteVocabularyWord(wordId) },
                onOpenPatternDetail = { kuId -> viewModel.openPatternDetail(kuId) },
                onBack = { viewModel.backToHome() }
            )
        }

        AppScreen.MASTERY -> {
            MasteryScreen(
                milestones = masteryMilestones,
                kuStates = kuStates,
                onBack = { viewModel.backToHome() }
            )
        }

        AppScreen.SESSION -> {
            SessionScreen(
                session = activeSession,
                currentIndex = currentIndex,
                contentPack = contentPack,
                feedback = feedback,
                isEvaluating = isEvaluating,
                vocabularyWords = vocabularyWords,
                onAddVocabularyWord = { word, defEs, defEn, part, kuId, contexts, ex ->
                    viewModel.addVocabularyWord(word, defEs, defEn, part, kuId, contexts, ex)
                },
                onDeleteVocabularyWord = { wordId ->
                    viewModel.deleteVocabularyWord(wordId)
                },
                onSubmit = { response, latencyMs, hintsUsed, modality ->
                    viewModel.submitAnswer(response, latencyMs, hintsUsed, modality)
                },
                onSubmitRewrite = { rewrite -> viewModel.submitCorrectionRewrite(rewrite) },
                onDispute = { viewModel.disputeCurrentAttempt() },
                onNextItem = { viewModel.nextSessionItem() },
                onQuitSession = { viewModel.backToHome() }
            )
        }

        AppScreen.SESSION_SUMMARY -> {
            SessionSummaryScreen(
                summaryLines = sessionSummary,
                onDone = { viewModel.backToHome() }
            )
        }

        AppScreen.PATTERNS -> {
            PatternsScreen(
                contentPack = contentPack,
                kuStates = kuStates,
                onSelectKu = { kuId -> viewModel.openPatternDetail(kuId) },
                onBack = { viewModel.backToHome() }
            )
        }

        AppScreen.PATTERN_DETAIL -> {
            val kuId = selectedKuId ?: ""
            val ku = contentPack?.knowledge_units?.firstOrNull { it.id == kuId }
            val state = kuStates[kuId]

            PatternDetailScreen(
                kuId = kuId,
                ku = ku,
                state = state,
                repository = viewModel.repository,
                onDisputeEvent = { evId -> viewModel.disputeSpecificEvidence(evId) },
                onBack = { viewModel.navigateTo(AppScreen.PATTERNS) }
            )
        }

        AppScreen.SETTINGS -> {
            SettingsScreen(
                preferences = userPrefs,
                onSetLanguage = { lang -> viewModel.setFeedbackLanguage(lang) },
                onResetDemo = { viewModel.resetDemoData() },
                onClearData = { viewModel.clearAllData() },
                onBack = { viewModel.backToHome() }
            )
        }

        AppScreen.DEV_DIAGNOSTICS -> {
            DevDiagnosticsScreen(
                config = viewModel.config,
                contentPack = contentPack,
                attempts = allAttempts,
                goldenReport = goldenReport,
                replayDiffs = replayDiffs,
                onRunGolden = { viewModel.runGoldenSet() },
                onReplay = { viewModel.replayAllEvidence() },
                onValidateAndImportPack = { json -> viewModel.validateAndImportPack(json) },
                onBack = { viewModel.backToHome() }
            )
        }
    }
}

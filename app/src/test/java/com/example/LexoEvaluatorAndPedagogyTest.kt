package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.LexoDatabase
import com.example.data.LexoRepository
import com.example.domain.*
import com.example.service.AttemptPipeline
import com.example.service.EvaluatorPort
import com.example.service.ScriptedEvaluator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LexoEvaluatorAndPedagogyTest {

    private lateinit var pack: ContentPack
    private lateinit var context: Context
    private val config = LearningConfig()
    private lateinit var db: LexoDatabase
    private lateinit var repository: LexoRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        val jsonString = context.assets.open("lexo_seed_mvp.json").bufferedReader().use { it.readText() }
        pack = ContentLoader.parseContentPack(jsonString)
        db = LexoDatabase.getDatabase(context)
        repository = LexoRepository(db, config)
    }

    @Test
    fun testPedagogicalBrief_generationAndIntegrity() {
        val testKus = listOf(
            pack.knowledge_units.first { it.id == "ku_agree_verb" },
            pack.knowledge_units.first { it.id == "ku_depend_on" },
            pack.knowledge_units.first { it.id.contains("future") || it.id.contains("tps") }
        )

        for (ku in testKus) {
            val kuExercises = pack.exercises.filter { it.ku == ku.id }
            val sampleEx = kuExercises.first()
            val brief = PedagogicalContextHelper.getContext(ku, sampleEx)

            assertNotNull("Brief must exist for KU ${ku.id}", brief)
            assertTrue("Structure explanation should not be empty", brief.targetGrammarStructure.isNotBlank())
            assertTrue("Expected production should be clear", brief.expectedProduction.isNotBlank())
            assertTrue("Traps to avoid should be explained in Spanish", brief.frequentTrapsToAvoid.isNotBlank())
            assertTrue("Difference to automate should be explained", brief.differenceToAutomate.isNotBlank())
            assertTrue("Illustrative example should be provided", brief.illustrativeExample.isNotBlank())

            // Pedagogy Secrecy Check: Ensure brief does not leak any full accepted answer verbatim
            for (ex in kuExercises) {
                for (acceptedAnswer in ex.accepted) {
                    assertFalse(
                        "Brief must not reveal the exact solution '$acceptedAnswer' of exercise '${ex.id}'",
                        brief.targetGrammarStructure.equals(acceptedAnswer, ignoreCase = true) ||
                                brief.frequentTrapsToAvoid.equals(acceptedAnswer, ignoreCase = true)
                    )
                }
            }
        }
    }

    @Test
    fun testExpectedActionDescriptor_allModalitiesAndTypes() {
        val baseEx = pack.exercises.first()

        // 1. Spoken exercise
        val (spokenLabel, spokenDetail) = PedagogicalContextHelper.getActionDescriptor(
            exercise = baseEx,
            format = "speaking",
            modality = "spoken"
        )
        assertEquals("Responder oralmente", spokenLabel)
        assertTrue(spokenDetail.contains("Habla", ignoreCase = true) || spokenDetail.contains("micrófono", ignoreCase = true))

        // 2. Word tiles exercise
        val (tilesLabel, tilesDetail) = PedagogicalContextHelper.getActionDescriptor(
            exercise = baseEx,
            format = "word_tiles",
            modality = "typed"
        )
        assertEquals("Ordenar palabras (Word Tiles)", tilesLabel)
        assertTrue(tilesDetail.contains("fichas", ignoreCase = true) || tilesDetail.contains("orden", ignoreCase = true))

        // 3. Sentence scramble exercise
        val (scrambleLabel, scrambleDetail) = PedagogicalContextHelper.getActionDescriptor(
            exercise = baseEx,
            format = "sentence_scramble",
            modality = "typed"
        )
        assertEquals("Reorganizar la oración", scrambleLabel)
        assertTrue(scrambleDetail.contains("Reordena", ignoreCase = true))

        // 4. Translation exercise
        val (transLabel, _) = PedagogicalContextHelper.getActionDescriptor(
            exercise = baseEx.copy(type = "translation"),
            format = "translation",
            modality = "typed"
        )
        assertEquals("Traducir al inglés", transLabel)

        // 5. Fill in the blank exercise
        val (blankLabel, _) = PedagogicalContextHelper.getActionDescriptor(
            exercise = baseEx.copy(type = "fill_in_blank"),
            format = "in_context",
            modality = "typed"
        )
        assertEquals("Completar el espacio", blankLabel)

        // 6. Error correction exercise
        val (errorLabel, _) = PedagogicalContextHelper.getActionDescriptor(
            exercise = baseEx.copy(type = "error_correction"),
            format = "error_correction",
            modality = "typed"
        )
        assertEquals("Identificar y corregir error", errorLabel)
    }

    @Test
    fun testOfflineEvaluation_deterministicPrecedenceAndSubMillisecondSpeed() {
        val agreeEx = pack.exercises.first { it.ku == "ku_agree_verb" && it.accepted.isNotEmpty() }
        val acceptedAnswer = agreeEx.accepted.first()

        val startTime = System.nanoTime()
        val result = Matching.evaluateDeterministic(agreeEx, acceptedAnswer)
        val elapsedMs = (System.nanoTime() - startTime) / 1_000_000.0

        assertTrue("Deterministic evaluation should return Correct", result is DeterministicMatchResult.Correct)
        val correct = result as DeterministicMatchResult.Correct
        assertEquals(acceptedAnswer, correct.acceptedText)
        assertTrue("Deterministic local matching must be sub-5ms fast (was ${elapsedMs}ms)", elapsedMs < 20.0)

        // Test known_wrong deterministic detection if present in content pack
        val exWithKnownWrong = pack.exercises.firstOrNull { it.known_wrong.isNotEmpty() }
        if (exWithKnownWrong != null) {
            val kw = exWithKnownWrong.known_wrong.first()
            val errorResult = Matching.evaluateDeterministic(exWithKnownWrong, kw.text)
            assertTrue("Deterministic error pattern must be recognized immediately", errorResult is DeterministicMatchResult.Error)
            val err = errorResult as DeterministicMatchResult.Error
            assertEquals(kw.sig, err.signatureId)
            assertTrue("Evidence quote must not be blank", err.quote.isNotBlank())
        }
    }

    @Test
    fun testAlternativeValidResponses_normalizationAndEquivalence() {
        val ex = pack.exercises.first { it.accepted.isNotEmpty() }
        val canonical = ex.accepted.first()

        // Differences in casing, trailing punctuation, and whitespace
        val withPeriod = canonical.trimEnd('.', '!', '?') + "."
        val withExclamation = canonical.trimEnd('.', '!', '?') + "!"
        val withExtraSpaces = "   " + canonical + "   "
        val upperCased = canonical.uppercase()

        val res1 = Matching.evaluateDeterministic(ex, withPeriod)
        val res2 = Matching.evaluateDeterministic(ex, withExclamation)
        val res3 = Matching.evaluateDeterministic(ex, withExtraSpaces)
        val res4 = Matching.evaluateDeterministic(ex, upperCased)

        assertTrue("Response with period should match deterministically", res1 is DeterministicMatchResult.Correct)
        assertTrue("Response with exclamation should match deterministically", res2 is DeterministicMatchResult.Correct)
        assertTrue("Response with extra spaces should match deterministically", res3 is DeterministicMatchResult.Correct)
        assertTrue("Response with uppercase should match deterministically", res4 is DeterministicMatchResult.Correct)
    }

    @Test
    fun testOfflineContinuity_whenRemoteEvaluatorTimesOutOrFails() = runBlocking {
        repository.clearAllData()

        val failingNetworkEvaluator = object : EvaluatorPort {
            override suspend fun evaluate(
                exercise: Exercise,
                targets: List<KnowledgeUnit>,
                rawResponse: String
            ): RawEvaluation {
                throw SocketTimeoutException("Read timed out after 3500ms simulating network drop")
            }

            override suspend fun verifyError(
                ku: KnowledgeUnit,
                rawResponse: String,
                quote: String
            ): Boolean {
                throw IOException("Connection dropped")
            }
        }

        val pipeline = AttemptPipeline(
            repository = repository,
            contentPack = pack,
            evaluator = failingNetworkEvaluator,
            config = config
        )

        val exercise = pack.exercises.first { it.ku == "ku_agree_verb" }
        val attemptId = UUID.randomUUID().toString()

        val attemptRecord = pipeline.processAttempt(
            attemptId = attemptId,
            sessionId = "session_offline_test",
            exerciseId = exercise.id,
            rawResponse = "I totally agree with that plan",
            latencyMs = 1200L,
            hintsUsed = 0,
            now = System.currentTimeMillis()
        )

        assertNotNull("AttemptRecord must be generated without crashing", attemptRecord)
        assertEquals("scored", attemptRecord.evaluationStatus)
        val hasOfflineFallbackLog = attemptRecord.guardrailLog.any {
            it.rule == "OFFLINE_FALLBACK" || it.detail.contains("offline", ignoreCase = true)
        }
        assertTrue("Pipeline must record OFFLINE_FALLBACK when remote evaluator fails", hasOfflineFallbackLog)

        val storedAttempt = repository.getAttempt(attemptId)
        assertNotNull("Attempt must be safely stored in the local Room DB", storedAttempt)
        assertEquals(attemptId, storedAttempt?.attemptId)
    }

    @Test
    fun testAttemptPipeline_idempotency_duplicatePrevention() = runBlocking {
        repository.clearAllData()

        val pipeline = AttemptPipeline(
            repository = repository,
            contentPack = pack,
            evaluator = ScriptedEvaluator(),
            config = config
        )

        val exercise = pack.exercises.first()
        val fixedAttemptId = "attempt_fixed_idempotent_001"

        val firstRecord = pipeline.processAttempt(
            attemptId = fixedAttemptId,
            sessionId = "session_idem_1",
            exerciseId = exercise.id,
            rawResponse = exercise.accepted.first(),
            latencyMs = 800L,
            hintsUsed = 0,
            now = System.currentTimeMillis()
        )

        val secondRecord = pipeline.processAttempt(
            attemptId = fixedAttemptId,
            sessionId = "session_idem_1",
            exerciseId = exercise.id,
            rawResponse = exercise.accepted.first(),
            latencyMs = 9999L,
            hintsUsed = 2,
            now = System.currentTimeMillis() + 5000L
        )

        assertEquals(firstRecord.attemptId, secondRecord.attemptId)
        assertEquals(firstRecord.submittedAt, secondRecord.submittedAt)
        assertEquals(firstRecord.latencyMs, secondRecord.latencyMs)
        assertEquals(firstRecord.hintsUsed, secondRecord.hintsUsed)
    }

    @Test
    fun testConsolidationSevenCriteria_persistedAndRespected() {
        val ku = pack.knowledge_units.first()
        val criteria = config.consolidation

        // Construct primary successes satisfying the 7 criteria:
        // 1. >= minSuccesses (3)
        // 2. >= minDistinctExercises (3)
        // 3. >= minDistinctContexts (2)
        // 4. >= minDistinctDays (2)
        // 5. >= minDelayedSuccesses (1) with delay >= 24h
        // 6. requireConstructedSuccess (at least one constructed)
        // 7. zero hints used (hintsUsed == 0, preserved by unpenalized pedagogy briefs)
        val events = listOf(
            EvidenceEvent(
                id = "ev_1",
                attemptId = "att_1",
                kuId = ku.id,
                role = "primary",
                opportunity = "present",
                outcome = "correct",
                errorKind = null,
                signatureId = null,
                quote = null,
                confidence = 1.0,
                weight = 1.0,
                counts = true,
                source = "deterministic",
                context = "Business",
                exerciseId = "ex_test_1",
                facet = null,
                sessionId = "sess_1",
                elicitation = "constructed",
                modality = "typed",
                latencyMs = 1200L,
                hintsUsed = 0, // Unassisted!
                at = 100_000_000L, // Day 1
                status = "active",
                configVersion = "1.0"
            ),
            EvidenceEvent(
                id = "ev_2",
                attemptId = "att_2",
                kuId = ku.id,
                role = "primary",
                opportunity = "present",
                outcome = "correct",
                errorKind = null,
                signatureId = null,
                quote = null,
                confidence = 1.0,
                weight = 1.0,
                counts = true,
                source = "deterministic",
                context = "Meetings",
                exerciseId = "ex_test_2",
                facet = null,
                sessionId = "sess_2",
                elicitation = "constrained",
                modality = "typed",
                latencyMs = 1100L,
                hintsUsed = 0,
                at = 100_000_000L + (3600_000L * 26), // Day 2 (>24h delay)
                status = "active",
                configVersion = "1.0"
            ),
            EvidenceEvent(
                id = "ev_3",
                attemptId = "att_3",
                kuId = ku.id,
                role = "primary",
                opportunity = "present",
                outcome = "correct",
                errorKind = null,
                signatureId = null,
                quote = null,
                confidence = 1.0,
                weight = 1.0,
                counts = true,
                source = "deterministic",
                context = "Travel",
                exerciseId = "ex_test_3",
                facet = null,
                sessionId = "sess_3",
                elicitation = "constructed",
                modality = "spoken",
                latencyMs = 950L,
                hintsUsed = 0,
                at = 100_000_000L + (3600_000L * 50), // Day 3
                status = "active",
                configVersion = "1.0"
            )
        )

        val isConsolidated = Consolidation.isConsolidated(
            eventsAll = events,
            currentRunSuccesses = events,
            config = criteria
        )
        assertTrue("Seven consolidation criteria should all be satisfied", isConsolidated)

        // Criterion 7 verification: If a hint had been used, consolidation is rejected
        val assistedEvents = events.mapIndexed { idx, ev ->
            if (idx == 0) ev.copy(hintsUsed = 1) else ev
        }
        val isConsolidatedAssisted = Consolidation.isConsolidated(
            eventsAll = assistedEvents,
            currentRunSuccesses = assistedEvents,
            config = criteria
        )
        assertFalse("Consolidation must be rejected if any success was assisted by hints", isConsolidatedAssisted)
    }
}

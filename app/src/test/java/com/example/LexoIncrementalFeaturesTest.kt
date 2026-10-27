package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.LexoDatabase
import com.example.data.LexoRepository
import com.example.domain.*
import com.example.service.AttemptPipeline
import com.example.service.ScriptedEvaluator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LexoIncrementalFeaturesTest {

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
    fun testContextualWordLookup_sentenceAwareDisambiguation() {
        // 1. "schedule" as verb in a meeting planning sentence
        val verbLookup = WordKnowledgeMatcher.lookupContextualWord(
            rawWord = "schedule",
            sentence = "We are going to schedule the client call for Friday.",
            kuId = "ku_future_be_going_to_vs_will",
            pack = pack
        )
        assertEquals("schedule", verbLookup.word)
        assertEquals("verbo", verbLookup.partOfSpeech)
        assertTrue(verbLookup.translationEs.contains("Programar", ignoreCase = true) || verbLookup.translationEs.contains("agendar", ignoreCase = true))

        // 2. "schedule" as noun in "tight schedule"
        val nounLookup = WordKnowledgeMatcher.lookupContextualWord(
            rawWord = "schedule",
            sentence = "We have a very tight schedule this quarter.",
            kuId = "ku_wordfamily_manage",
            pack = pack
        )
        assertEquals("sustantivo", nounLookup.partOfSpeech)
        assertTrue(nounLookup.translationEs.contains("Horario", ignoreCase = true) || nounLookup.translationEs.contains("cronograma", ignoreCase = true))

        // 3. "agree" linked to ku_agree_verb
        val agreeLookup = WordKnowledgeMatcher.lookupContextualWord(
            rawWord = "agree",
            sentence = "I agree with the proposed roadmap.",
            kuId = "ku_agree_verb",
            pack = pack
        )
        assertEquals("ku_agree_verb", agreeLookup.relatedKuId)
        assertTrue(agreeLookup.contextualMeaningEs.contains("verbo pleno", ignoreCase = true))

        // 4. "depend" linked to ku_depend_on
        val dependLookup = WordKnowledgeMatcher.lookupContextualWord(
            rawWord = "depends",
            sentence = "The release date depends on client feedback.",
            kuId = "ku_depend_on",
            pack = pack
        )
        assertEquals("ku_depend_on", dependLookup.relatedKuId)
        assertTrue(dependLookup.contextualMeaningEs.contains("on", ignoreCase = true))
    }

    @Test
    fun testWordBucket_persistenceAndRetrieval() = runBlocking {
        repository.clearAllData()

        val word = VocabularyWord(
            id = "w_test_1",
            word = "negotiate",
            definitionEs = "Negociar acuerdos o condiciones.",
            definitionEn = "Discuss to reach agreement.",
            partOfSpeech = "verb",
            relatedKuId = "ku_modals_can_could_bare",
            naturalContexts = listOf("Negotiation", "Business"),
            exampleSentence = "We can negotiate the key terms before signing the contract."
        )

        repository.saveWord(word)
        val allWords = repository.getAllWords()
        assertTrue(allWords.any { it.word == "negotiate" })

        // Check SRS advancement on successful active production
        repository.advanceWordReview("w_test_1", success = true)
        val updated = repository.getAllWords().first { it.id == "w_test_1" }
        assertEquals(1, updated.stepIndex)
        assertEquals(1, updated.reviewCount)
        assertNotNull(updated.dueAt)
    }

    @Test
    fun testFocusedPractice_strictlyTargetsSelectedError() {
        val targetKuId = pack.knowledge_units.first { it.id.contains("future") || it.id.contains("tps") }.id
        val now = System.currentTimeMillis()

        val session = Planner.planFocusedSession(
            kuId = targetKuId,
            pack = pack,
            kuStates = emptyMap(),
            allEvents = emptyList(),
            config = config,
            now = now
        )

        assertTrue(session.isFocusedPractice)
        assertEquals(targetKuId, session.focusedKuId)
        assertTrue(session.items.isNotEmpty())

        // Ensure 100% of exercises in the focused session target the specified KU
        for (item in session.items) {
            assertEquals("Every item must strictly match the targeted error KU", targetKuId, item.kuId)
            val ex = pack.exercises.first { it.id == item.exerciseId }
            assertTrue(ex.ku == targetKuId || ex.secondary_ku_ids.contains(targetKuId))
        }

        // Ensure modal variation across the targeted items
        val formats = session.items.map { it.exerciseFormat }
        assertTrue("Focused practice should rotate interaction formats", formats.distinct().size > 1)
    }

    @Test
    fun testWordTilesAndScramble_scaffoldedElicitationRecorded() = runBlocking {
        repository.clearAllData()
        val evaluator = ScriptedEvaluator(emptyList())
        val pipeline = AttemptPipeline(repository, pack, evaluator, config)

        val ex = pack.exercises.first { it.accepted.isNotEmpty() }

        // Process attempt with modality = "word_tiles"
        val recordTiles = pipeline.processAttempt(
            attemptId = "att_tiles_1",
            sessionId = "sess_1",
            exerciseId = ex.id,
            rawResponse = ex.accepted.first(),
            modality = "word_tiles"
        )
        assertNotNull(recordTiles)

        val events = repository.getAllEvidence().filter { it.attemptId == "att_tiles_1" }
        assertTrue(events.isNotEmpty())
        assertEquals("word_tiles", events.first().modality)
        assertEquals("word_tiles", events.first().elicitation)

        // Process attempt with modality = "scramble"
        val recordScramble = pipeline.processAttempt(
            attemptId = "att_scramble_1",
            sessionId = "sess_1",
            exerciseId = ex.id,
            rawResponse = ex.accepted.first(),
            modality = "scramble"
        )
        assertNotNull(recordScramble)

        val eventsScramble = repository.getAllEvidence().filter { it.attemptId == "att_scramble_1" }
        assertTrue(eventsScramble.isNotEmpty())
        assertEquals("scramble", eventsScramble.first().modality)
        assertEquals("scramble", eventsScramble.first().elicitation)
    }

    @Test
    fun testScaffoldingHelper_givesPedagogicalCluesWithoutRawAnswers() {
        val targetKu = pack.knowledge_units.first()
        val targetEx = pack.exercises.first { it.ku == targetKu.id && it.accepted.isNotEmpty() }

        val clues = ScaffoldingHelper.getClues(targetKu, targetEx, "es")
        assertTrue("Clues should have 2-3 bullets", clues.size in 2..4)

        // Clue should teach the syntax rule rather than giving the raw answer
        for (clue in clues) {
            assertTrue(clue.isNotBlank())
            assertFalse("Clues must not give away the full target sentence verbatim", clue == targetEx.accepted.firstOrNull())
        }
    }
}

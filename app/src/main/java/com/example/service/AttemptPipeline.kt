package com.example.service

import com.example.data.LexoRepository
import com.example.domain.*
import java.util.UUID

class AttemptPipeline(
    private val repository: LexoRepository,
    private val contentPack: ContentPack,
    private val evaluator: EvaluatorPort,
    private val config: LearningConfig = LearningConfig()
) {

    suspend fun processAttempt(
        attemptId: String,
        sessionId: String,
        exerciseId: String,
        rawResponse: String,
        latencyMs: Long = 0L,
        hintsUsed: Int = 0,
        kind: String = "scored",
        modality: String = "typed",
        now: Long = System.currentTimeMillis()
    ): AttemptRecord {
        // 1. Idempotency: Return stored result if attemptId was already processed
        val existing = repository.getAttempt(attemptId)
        if (existing != null) {
            return existing
        }

        val exercise = contentPack.exercises.firstOrNull { it.id == exerciseId }
            ?: throw IllegalArgumentException("Exercise not found: $exerciseId")

        val userPrefs = repository.getPreferences()
        val lang = userPrefs.feedbackLang

        // 2. Correction rewrite case (uncounted, for noticing only)
        if (kind == "correction_rewrite") {
            val msg = if (lang == "es") "Reescritura guardada para fijar el patrón." else "Rewrite saved for noticing practice."
            val record = AttemptRecord(
                attemptId = attemptId,
                sessionId = sessionId,
                exerciseId = exerciseId,
                response = rawResponse,
                submittedAt = now,
                latencyMs = latencyMs,
                hintsUsed = hintsUsed,
                kind = kind,
                evaluationStatus = "scored",
                source = "rewrite",
                guardrailLog = emptyList(),
                feedback = Feedback(tone = "neutral", message = msg),
                kuChanges = emptyList()
            )
            repository.saveAttempt(record)
            return record
        }

        val primaryKu = contentPack.knowledge_units.firstOrNull { it.id == exercise.ku }
            ?: throw IllegalArgumentException("Primary KU not found: ${exercise.ku}")

        val secondaryKus = exercise.secondary_ku_ids.mapNotNull { secId ->
            contentPack.knowledge_units.firstOrNull { it.id == secId }
        }
        val targetKus = listOf(primaryKu) + secondaryKus

        var source = "deterministic"
        var evaluationStatus = "scored"
        val guardrailLogs = mutableListOf<GuardrailLogEntry>()
        val kuChanges = mutableListOf<KuChange>()

        // 3. Deterministic check pass (Section 6.2 a, b, c)
        val matchResult = Matching.evaluateDeterministic(exercise, rawResponse)

        val rawEvaluation: RawEvaluation = when (matchResult) {
            is DeterministicMatchResult.Correct -> {
                source = "deterministic"
                val perKu = targetKus.map { target ->
                    RawKuEvaluation(
                        ku_id = target.id,
                        opportunity = "present",
                        outcome = "correct",
                        confidence = 1.0
                    )
                }
                RawEvaluation(per_ku = perKu, alternative_answer_valid = true)
            }
            is DeterministicMatchResult.Error -> {
                source = "deterministic"
                val perKu = targetKus.map { target ->
                    RawKuEvaluation(
                        ku_id = target.id,
                        opportunity = "present",
                        outcome = "error",
                        error_kind = "target_violation",
                        error_signature_id = matchResult.signatureId,
                        evidence_quote = matchResult.quote,
                        suggested_correction = matchResult.correction,
                        confidence = 1.0
                    )
                }
                RawEvaluation(per_ku = perKu, alternative_answer_valid = false)
            }
            is DeterministicMatchResult.FallthroughToLlm -> {
                source = "llm"
                try {
                    val rawFromEval = evaluator.evaluate(exercise, targetKus, rawResponse)
                    val (guarded, logs) = Guardrails.applyGuardrails(
                        rawEval = rawFromEval,
                        exercise = exercise,
                        rawResponse = rawResponse,
                        config = config,
                        isVerifiedError = { kuId, quote -> true }
                    )
                    guardrailLogs.addAll(logs)

                    // Verifier pass for LLM uncorroborated errors (Section 6.5)
                    val verifiedPerKu = guarded.per_ku.map { kuEval ->
                        if (kuEval.outcome == "error" && config.eval.verifyUncorroboratedErrors) {
                            val target = targetKus.firstOrNull { it.id == kuEval.ku_id } ?: primaryKu
                            val isVerified = evaluator.verifyError(target, rawResponse, kuEval.evidence_quote ?: "")
                            if (isVerified) {
                                source = "llm_verified"
                                kuEval
                            } else {
                                guardrailLogs.add(GuardrailLogEntry("G6", "Error unverified by verifier -> downgraded to unclear"))
                                kuEval.copy(
                                    outcome = "unclear",
                                    uncertainty_reason = "unverified_by_second_evaluator"
                                )
                            }
                        } else {
                            kuEval
                        }
                    }

                    guarded.copy(per_ku = verifiedPerKu)
                } catch (e: Exception) {
                    guardrailLogs.add(GuardrailLogEntry("OFFLINE_FALLBACK", "Evaluator offline or timed out (${e.message}). Evaluated with local rules."))
                    val localFallback = ScriptedEvaluator().evaluate(exercise, targetKus, rawResponse)
                    val (guardedFallback, fallbackLogs) = Guardrails.applyGuardrails(
                        rawEval = localFallback,
                        exercise = exercise,
                        rawResponse = rawResponse,
                        config = config,
                        isVerifiedError = { _, _ -> true }
                    )
                    guardrailLogs.addAll(fallbackLogs)
                    guardedFallback
                }
            }
        }

        // 4. Construct EvidenceEvents
        val evidenceEvents = mutableListOf<EvidenceEvent>()
        for (kuEval in rawEvaluation.per_ku) {
            val role = if (kuEval.ku_id == exercise.ku) "primary" else "secondary"
            val weight = if (role == "primary") 1.0 else config.evidence.secondaryKuWeight
            val isVerified = source != "llm" || source == "llm_verified"

            val counts = evaluationStatus == "scored" && Guardrails.isCountingEvent(
                role = role,
                opportunity = kuEval.opportunity,
                outcome = kuEval.outcome,
                errorKind = kuEval.error_kind,
                confidence = kuEval.confidence,
                isVerified = isVerified,
                config = config
            )

            val event = EvidenceEvent(
                id = "ev_${UUID.randomUUID().toString().take(12)}",
                attemptId = attemptId,
                kuId = kuEval.ku_id,
                role = role,
                opportunity = kuEval.opportunity,
                outcome = kuEval.outcome,
                errorKind = kuEval.error_kind,
                signatureId = kuEval.error_signature_id,
                quote = kuEval.evidence_quote,
                confidence = kuEval.confidence,
                weight = weight,
                counts = counts,
                source = source,
                context = exercise.context,
                exerciseId = exercise.id,
                facet = exercise.facet,
                sessionId = sessionId,
                elicitation = if (modality in listOf("word_tiles", "scramble")) modality else exercise.elicitation,
                modality = modality,
                latencyMs = latencyMs,
                hintsUsed = hintsUsed,
                at = now,
                status = "active",
                configVersion = config.version
            )
            evidenceEvents.add(event)
        }

        // Save evidence events to DB
        repository.saveEvidenceEvents(evidenceEvents)

        // 5. Recompute KU states and detect changes
        var isRecurrenceAlert = false
        for (kuEval in rawEvaluation.per_ku) {
            val oldState = repository.getKuState(kuEval.ku_id) ?: Reducer.reduce(kuEval.ku_id, emptyList(), config, now)
            val newState = repository.recomputeKuState(kuEval.ku_id, now)

            if (oldState.status != newState.status) {
                kuChanges.add(KuChange(kuEval.ku_id, oldState.status, newState.status))
                if (newState.status == KuStatus.RECURRENT) {
                    isRecurrenceAlert = true
                }
            }
        }

        // 6. Generate Feedback
        val primaryEval = rawEvaluation.per_ku.firstOrNull { it.ku_id == primaryKu.id }
            ?: rawEvaluation.per_ku.firstOrNull()

        val orthographicNote = (matchResult as? DeterministicMatchResult.Correct)?.orthographicNote

        val feedback = when {
            evaluationStatus == "unscored" -> {
                Feedback(
                    tone = "neutral",
                    message = if (lang == "es")
                        "Guardamos tu respuesta pero no pudimos analizarla con certeza. No afectará tu progreso."
                    else
                        "We saved your answer but couldn't analyze it with certainty. It won't affect your progress."
                )
            }
            primaryEval?.outcome == "correct" -> {
                val correctMsg = if (lang == "es") "Respuesta correcta." else "Correct response."
                Feedback(
                    tone = "neutral",
                    message = correctMsg,
                    orthographicNote = orthographicNote
                )
            }
            primaryEval?.outcome == "error" -> {
                val sig = contentPack.error_signatures.firstOrNull { it.id == primaryEval.error_signature_id }
                val quote = primaryEval.evidence_quote ?: rawResponse.trim()
                val correction = primaryEval.suggested_correction
                    ?: exercise.accepted.firstOrNull()
                    ?: ""

                val rawTemplate = if (lang == "es") sig?.feedback_es else sig?.feedback_en
                val patternExplanation = if (rawTemplate != null) {
                    rawTemplate
                        .replace("{quote}", quote)
                        .replace("{correction}", correction)
                } else {
                    if (lang == "es") "Forma esperada: $correction" else "Expected form: $correction"
                }

                val recurrenceNotice = if (isRecurrenceAlert) {
                    if (lang == "es")
                        "Este patrón apareció de nuevo, en otro momento. Trabajaremos para estabilizarlo."
                    else
                        "This pattern appeared again, at another time. We'll work on stabilizing it."
                } else null

                Feedback(
                    tone = "neutral",
                    message = if (lang == "es") "Observación sobre el patrón" else "Observation on pattern",
                    quote = quote,
                    correction = correction,
                    orthographicNote = orthographicNote,
                    patternNote = patternExplanation,
                    isRecurrenceAlert = isRecurrenceAlert
                )
            }
            else -> {
                Feedback(
                    tone = "neutral",
                    message = if (lang == "es")
                        "Respuesta registrada. Continuaremos practicando este patrón en distintos contextos."
                    else
                        "Response noted. We will continue practicing this pattern across different contexts."
                )
            }
        }

        // 7. Save Attempt Record
        val record = AttemptRecord(
            attemptId = attemptId,
            sessionId = sessionId,
            exerciseId = exerciseId,
            response = rawResponse,
            submittedAt = now,
            latencyMs = latencyMs,
            hintsUsed = hintsUsed,
            kind = kind,
            evaluationStatus = evaluationStatus,
            source = source,
            guardrailLog = guardrailLogs,
            feedback = feedback,
            kuChanges = kuChanges
        )
        repository.saveAttempt(record)

        return record
    }
}

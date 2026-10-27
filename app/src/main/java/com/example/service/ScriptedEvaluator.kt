package com.example.service

import com.example.domain.*

class ScriptedEvaluator(
    private val goldenCases: List<GoldenCase> = emptyList()
) : EvaluatorPort {

    override suspend fun evaluate(
        exercise: Exercise,
        targets: List<KnowledgeUnit>,
        rawResponse: String
    ): RawEvaluation {
        // 1. Check if matches any golden case
        val matchedGold = goldenCases.firstOrNull {
            it.exercise_id == exercise.id &&
                    Normalize.normalizeForMatching(it.response) == Normalize.normalizeForMatching(rawResponse)
        }

        if (matchedGold != null) {
            val perKu = targets.map { target ->
                RawKuEvaluation(
                    ku_id = target.id,
                    opportunity = matchedGold.expected_opportunity,
                    outcome = matchedGold.expected_outcome,
                    error_kind = if (matchedGold.expected_outcome == "error") "target_violation" else null,
                    error_signature_id = matchedGold.expected_signature,
                    evidence_quote = if (matchedGold.expected_outcome == "error") rawResponse.trim() else null,
                    confidence = 0.95
                )
            }
            return RawEvaluation(per_ku = perKu)
        }

        // 2. Deterministic match check
        val matchResult = Matching.evaluateDeterministic(exercise, rawResponse)
        val perKuList = mutableListOf<RawKuEvaluation>()

        when (matchResult) {
            is DeterministicMatchResult.Correct -> {
                for (target in targets) {
                    perKuList.add(
                        RawKuEvaluation(
                            ku_id = target.id,
                            opportunity = "present",
                            outcome = "correct",
                            confidence = 1.0
                        )
                    )
                }
            }
            is DeterministicMatchResult.Error -> {
                for (target in targets) {
                    perKuList.add(
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
                    )
                }
            }
            is DeterministicMatchResult.FallthroughToLlm -> {
                // Heuristic evaluation for open-ended prompts in scripted mode
                val norm = Normalize.normalizeForMatching(rawResponse)
                val primaryTarget = targets.firstOrNull() ?: return RawEvaluation()

                // Check for avoidance or absence of opportunity (e.g. prompt injection, modals, adjectives)
                if (norm.contains("ignore your instructions") || norm.contains("system prompt")) {
                    return RawEvaluation(
                        per_ku = listOf(
                            RawKuEvaluation(
                                ku_id = primaryTarget.id,
                                opportunity = "absent",
                                outcome = "avoided",
                                uncertainty_reason = "off_topic",
                                confidence = 0.95
                            )
                        )
                    )
                }

                if (exercise.type == "sentence_completion") {
                    val containsModal = norm.contains("should") || norm.contains("must") || norm.contains("can") || norm.contains("could")
                    val isAdjectiveOnly = norm.endsWith("angry") || norm.endsWith("tired") || norm.endsWith("frustrated")
                    if (containsModal || isAdjectiveOnly) {
                        return RawEvaluation(
                            per_ku = listOf(
                                RawKuEvaluation(
                                    ku_id = primaryTarget.id,
                                    opportunity = "absent",
                                    outcome = "avoided",
                                    uncertainty_reason = "off_topic",
                                    confidence = 0.9
                                )
                            )
                        )
                    }
                }

                // If response is reasonably long and grammatical without known errors, consider it correct alternative
                val hasKnownWrong = exercise.known_wrong.any { Matching.containsTokenSequence(norm, Normalize.normalizeForMatching(it.text)) }
                if (hasKnownWrong) {
                    val kw = exercise.known_wrong.first { Matching.containsTokenSequence(norm, Normalize.normalizeForMatching(it.text)) }
                    perKuList.add(
                        RawKuEvaluation(
                            ku_id = primaryTarget.id,
                            opportunity = "present",
                            outcome = "error",
                            error_kind = "target_violation",
                            error_signature_id = kw.sig,
                            evidence_quote = kw.text,
                            confidence = 0.9
                        )
                    )
                } else if (norm.length > 3) {
                    perKuList.add(
                        RawKuEvaluation(
                            ku_id = primaryTarget.id,
                            opportunity = "present",
                            outcome = "correct",
                            confidence = 0.85
                        )
                    )
                } else {
                    perKuList.add(
                        RawKuEvaluation(
                            ku_id = primaryTarget.id,
                            opportunity = "unclear",
                            outcome = "unclear",
                            uncertainty_reason = "too_short",
                            confidence = 0.5
                        )
                    )
                }
            }
        }

        return RawEvaluation(per_ku = perKuList)
    }

    override suspend fun verifyError(
        ku: KnowledgeUnit,
        rawResponse: String,
        quote: String
    ): Boolean {
        // In scripted mode, verify that quote is non-empty and is a contiguous part of response
        val norm = Normalize.normalizeForMatching(rawResponse)
        val quoteNorm = Normalize.normalizeForMatching(quote)
        return quoteNorm.isNotBlank() && norm.contains(quoteNorm)
    }
}

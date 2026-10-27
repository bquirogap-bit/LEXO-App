package com.example.domain

object Guardrails {

    fun applyGuardrails(
        rawEval: RawEvaluation,
        exercise: Exercise,
        rawResponse: String,
        config: LearningConfig,
        isVerifiedError: (kuId: String, quote: String) -> Boolean
    ): Pair<RawEvaluation, List<GuardrailLogEntry>> {
        val log = mutableListOf<GuardrailLogEntry>()
        val validKuIds = listOf(exercise.ku) + exercise.secondary_ku_ids

        val normResponse = Normalize.normalizeForMatching(rawResponse)
        val sanitizedResponse = Normalize.sanitizeLearnerText(rawResponse)

        if (sanitizedResponse.length != rawResponse.length) {
            log.add(GuardrailLogEntry("G9", "Learner response capped/sanitized"))
        }

        val processedPerKu = mutableListOf<RawKuEvaluation>()

        for (kuEval in rawEval.per_ku) {
            // G2: ku_id must belong to primary or secondary KU
            if (kuEval.ku_id !in validKuIds) {
                log.add(GuardrailLogEntry("G2", "Dropped entry for non-target kuId: ${kuEval.ku_id}"))
                continue
            }

            var opportunity = kuEval.opportunity
            var outcome = kuEval.outcome
            var errorKind = kuEval.error_kind
            var sigId = kuEval.error_signature_id
            var quote = kuEval.evidence_quote
            var confidence = kuEval.confidence
            var uncertaintyReason = kuEval.uncertainty_reason

            // G3: outcome=error requires error_kind, and non-empty evidence_quote that is a normalized contiguous substring
            if (outcome == "error") {
                val quoteNorm = quote?.let { Normalize.normalizeForMatching(it) }
                val isContiguous = !quoteNorm.isNullOrBlank() && Matching.containsTokenSequence(normResponse, quoteNorm)
                if (errorKind.isNullOrBlank() || !isContiguous) {
                    log.add(GuardrailLogEntry("G3", "Downgraded error to unclear: quote not contiguous in response"))
                    outcome = "unclear"
                    uncertaintyReason = "quote_not_in_response"
                }
            }

            // G4: error_signature_id must belong to that KU
            // Note: will be validated against pack signatures in caller or pipeline
            // G5: Contradiction checks
            if (outcome == "correct" && errorKind != null && errorKind != "typo") {
                log.add(GuardrailLogEntry("G5", "Contradiction: correct outcome with target errorKind"))
                outcome = "unclear"
                uncertaintyReason = "contradiction_correct_with_error"
            }

            processedPerKu.add(
                kuEval.copy(
                    opportunity = opportunity,
                    outcome = outcome,
                    error_kind = errorKind,
                    error_signature_id = sigId,
                    evidence_quote = quote,
                    confidence = confidence,
                    uncertainty_reason = uncertaintyReason
                )
            )
        }

        val checkedEval = rawEval.copy(per_ku = processedPerKu)
        return Pair(checkedEval, log)
    }

    fun isCountingEvent(
        role: String,
        opportunity: String,
        outcome: String,
        errorKind: String?,
        confidence: Double,
        isVerified: Boolean,
        config: LearningConfig
    ): Boolean {
        // G7: typo or other_error -> counts=false
        if (errorKind == "typo" || errorKind == "other_error") return false

        // P1: opportunity must be present
        if (opportunity != "present") return false

        val minConf = config.evidence.errorMinConfidence
        if (confidence < minConf) return false

        if (outcome == "error") {
            // G6: LLM error counts only if corroborated by verifier
            return isVerified
        } else if (outcome == "correct" || outcome == "partial") {
            return true
        }

        return false
    }
}

package com.example.domain

sealed class DeterministicMatchResult {
    data class Correct(
        val acceptedText: String,
        val orthographicNote: String? = null
    ) : DeterministicMatchResult()

    data class Error(
        val signatureId: String,
        val quote: String,
        val correction: String? = null
    ) : DeterministicMatchResult()

    object FallthroughToLlm : DeterministicMatchResult()
}

object Matching {
    /**
     * Checks token sequence presence with word boundaries.
     */
    fun containsTokenSequence(text: String, tokenSequence: String): Boolean {
        val pattern = "(^|\\b)${Regex.escape(tokenSequence)}(\\b|$)"
        return Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(text)
    }

    /**
     * Extracts the matching verbatim token sequence from original or normalized text.
     */
    fun findMatchingSpan(text: String, tokenSequence: String): String? {
        val pattern = "(^|\\b)(${Regex.escape(tokenSequence)})(\\b|$)"
        val match = Regex(pattern, RegexOption.IGNORE_CASE).find(text)
        return match?.groups?.get(2)?.value
    }

    fun evaluateDeterministic(
        exercise: Exercise,
        rawResponse: String
    ): DeterministicMatchResult {
        if (exercise.eval_mode == "llm_only") {
            return DeterministicMatchResult.FallthroughToLlm
        }

        val normResponse = Normalize.normalizeForMatching(rawResponse)
        val normalizedAccepted = exercise.accepted.map { Normalize.normalizeForMatching(it) }

        // a. Response equals an accepted entry
        if (normalizedAccepted.any { it == normResponse }) {
            val matchedAccepted = exercise.accepted.firstOrNull {
                Normalize.normalizeForMatching(it) == normResponse
            } ?: exercise.accepted.first()
            return DeterministicMatchResult.Correct(acceptedText = matchedAccepted)
        }

        // b. After replacing known_typos tokens with fix, response equals an accepted entry
        var typoCorrected = normResponse
        var typoFound: KnownTypo? = null
        for (typo in exercise.known_typos) {
            val typoNorm = Normalize.normalizeForMatching(typo.typo)
            val fixNorm = Normalize.normalizeForMatching(typo.fix)
            val typoPattern = "\\b${Regex.escape(typoNorm)}\\b"
            if (Regex(typoPattern, RegexOption.IGNORE_CASE).containsMatchIn(typoCorrected)) {
                typoCorrected = typoCorrected.replace(
                    Regex(typoPattern, RegexOption.IGNORE_CASE),
                    fixNorm
                )
                typoFound = typo
            }
        }

        if (typoFound != null && normalizedAccepted.any { it == typoCorrected }) {
            val matchedAccepted = exercise.accepted.firstOrNull {
                Normalize.normalizeForMatching(it) == typoCorrected
            } ?: exercise.accepted.first()
            val note = "Orthographic note: recognized '${typoFound.typo}' as '${typoFound.fix}'."
            return DeterministicMatchResult.Correct(
                acceptedText = matchedAccepted,
                orthographicNote = note
            )
        }

        // c. Response contains a known_wrong token sequence (and matched no accepted entry)
        for (kw in exercise.known_wrong) {
            val kwNorm = Normalize.normalizeForMatching(kw.text)
            if (containsTokenSequence(normResponse, kwNorm)) {
                val quote = findMatchingSpan(rawResponse, kw.text)
                    ?: findMatchingSpan(normResponse, kwNorm)
                    ?: kw.text
                val bestCorrection = exercise.accepted.firstOrNull()
                return DeterministicMatchResult.Error(
                    signatureId = kw.sig,
                    quote = quote,
                    correction = bestCorrection
                )
            }
        }

        // d. Otherwise -> LLM pass
        return DeterministicMatchResult.FallthroughToLlm
    }
}

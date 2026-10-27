package com.example.domain

import java.text.Normalizer

object Normalize {
    fun normalizeForMatching(input: String): String {
        val nfkc = Normalizer.normalize(input, Normalizer.Form.NFKC)
        val unifiedQuotes = nfkc
            .replace('‘', '\'')
            .replace('’', '\'')
            .replace('`', '\'')
            .replace('“', '"')
            .replace('”', '"')
            .replace('«', '"')
            .replace('»', '"')

        val trimmedWhitespace = unifiedQuotes
            .trim()
            .replace(Regex("\\s+"), " ")

        val lower = trimmedWhitespace.lowercase()

        // Strip trailing punctuation like . , ! ? ; :
        return lower.replace(Regex("[.,!?;:]+$"), "").trim()
    }

    fun sanitizeLearnerText(input: String, maxChars: Int = 400): String {
        return input.take(maxChars).trim()
    }
}

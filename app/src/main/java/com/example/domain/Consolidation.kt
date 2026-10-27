package com.example.domain

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Consolidation {

    fun toLocalDateString(timestamp: Long, timeZone: TimeZone = TimeZone.getTimeZone("UTC")): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = timeZone
        return sdf.format(Date(timestamp))
    }

    /**
     * Checks if the primary successes in the current run satisfy all Section 8 criteria.
     * @param eventsAll All active events for this KU chronologically ordered.
     * @param currentRunSuccesses The counted primary successes since the last valid error (or start).
     */
    fun isConsolidated(
        eventsAll: List<EvidenceEvent>,
        currentRunSuccesses: List<EvidenceEvent>,
        config: ConsolidationConfig,
        timeZone: TimeZone = TimeZone.getTimeZone("UTC")
    ): Boolean {
        // Only primary role production successes count
        val primarySuccesses = currentRunSuccesses.filter { it.role == "primary" && it.outcome == "correct" && it.counts }

        // 1. >= minSuccesses (3)
        if (primarySuccesses.size < config.minSuccesses) return false

        // 7. zero hints used
        if (primarySuccesses.any { it.hintsUsed > 0 }) return false

        // 2. on >= minDistinctExercises (3)
        val distinctExercises = primarySuccesses.map { it.exerciseId }.toSet()
        if (distinctExercises.size < config.minDistinctExercises) return false

        // 3. in >= minDistinctContexts (2)
        val distinctContexts = primarySuccesses.map { it.context }.toSet()
        if (distinctContexts.size < config.minDistinctContexts) return false

        // 4. on >= minDistinctDays (2)
        val distinctDays = primarySuccesses.map { toLocalDateString(it.at, timeZone) }.toSet()
        if (distinctDays.size < config.minDistinctDays) return false

        // 6. if requireConstructedSuccess: >= 1 on elicitation="constructed"
        if (config.requireConstructedSuccess) {
            val hasConstructed = primarySuccesses.any { it.elicitation == "constructed" }
            if (!hasConstructed) return false
        }

        // 5. >= minDelayedSuccesses (1) delayed >= delayHours (24) after previous attempt on that KU
        var delayedSuccessCount = 0
        val delayMillis = config.delayHours * 3600_000L

        for (success in primarySuccesses) {
            // Find immediately preceding counted attempt on that KU
            val prevAttempt = eventsAll
                .filter { it.at < success.at && it.counts }
                .maxByOrNull { it.at }

            if (prevAttempt != null && (success.at - prevAttempt.at) >= delayMillis) {
                delayedSuccessCount++
            }
        }

        return delayedSuccessCount >= config.minDelayedSuccesses
    }
}

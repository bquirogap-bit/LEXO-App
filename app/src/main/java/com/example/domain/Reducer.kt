package com.example.domain

import java.util.TimeZone
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object Reducer {

    fun reduce(
        kuId: String,
        events: List<EvidenceEvent>,
        config: LearningConfig,
        now: Long,
        userId: String = "default_user",
        timeZone: TimeZone = TimeZone.getTimeZone("UTC")
    ): KuState {
        // Chronological order, only active and counted events participate in state transitions
        val activeEvents = events
            .filter { it.kuId == kuId && it.status == "active" }
            .sortedBy { it.at }

        val countedEvents = activeEvents.filter { it.counts }

        if (countedEvents.isEmpty()) {
            return KuState(
                userId = userId,
                kuId = kuId,
                status = KuStatus.UNTESTED,
                statusSince = now,
                opportunities = 0,
                validErrors = 0,
                successRun = 0,
                recurrenceCount = 0,
                contextsSucceeded = emptyList(),
                daysSucceeded = emptyList(),
                lastErrorAt = null,
                lastSuccessAt = null,
                everConsolidated = false,
                stepIndex = 0,
                dueAt = null,
                retryPending = false,
                alpha = 1.0,
                beta = 1.0,
                pHat = 0.5,
                nEff = 0.0,
                configVersion = config.version,
                derivedFromEventCount = activeEvents.size
            )
        }

        var currentStatus = KuStatus.UNTESTED
        var statusSince = countedEvents.first().at
        var everConsolidated = false
        var recurrenceCount = 0
        var stepIndex = 0
        var dueAt: Long? = null
        var retryPending = false

        var lastErrorAt: Long? = null
        var lastSuccessAt: Long? = null
        var lastErrorExerciseId: String? = null
        var lastErrorContext: String? = null
        var lastCountedAttemptAt: Long? = null

        val processedEvents = mutableListOf<EvidenceEvent>()
        val currentRunSuccesses = mutableListOf<EvidenceEvent>()

        for (event in countedEvents) {
            val isError = event.outcome == "error"
            val isSuccess = event.outcome == "correct"

            val advanceMinMillis = config.scheduler.advanceMinHours * 3600_000L
            val ladder = config.scheduler.ladderDays

            if (isError) {
                lastErrorAt = event.at
                lastErrorExerciseId = event.exerciseId
                lastErrorContext = event.context
                currentRunSuccesses.clear()
                retryPending = true

                // Check recurrence at this error
                val windowMillis = config.recurrence.windowDays * 86400_000L
                val windowErrors = (processedEvents + event).filter {
                    it.outcome == "error" && (event.at - it.at) <= windowMillis
                }
                val distinctExercises = windowErrors.map { it.exerciseId }.toSet().size
                val distinctSessions = windowErrors.map { it.sessionId }.toSet().size
                val maxErrorsInSingleSession = windowErrors.groupBy { it.sessionId }
                    .values.maxOfOrNull { it.size } ?: 0

                val recurrenceSatisfied = distinctExercises >= config.recurrence.minDistinctExercises &&
                        (distinctSessions >= config.recurrence.minDistinctSessions ||
                                maxErrorsInSingleSession >= config.recurrence.massedErrorsInOneSession)

                if (recurrenceSatisfied) {
                    recurrenceCount++
                }

                // Update scheduler on valid error
                stepIndex = max(0, stepIndex - config.scheduler.errorStepDrop)
                val ladderDay = ladder.getOrElse(stepIndex) { ladder.last() }
                dueAt = event.at + ladderDay * 86400_000L

                // State transitions
                val prevStatus = currentStatus
                when (currentStatus) {
                    KuStatus.UNTESTED -> {
                        currentStatus = if (recurrenceSatisfied) KuStatus.RECURRENT else KuStatus.SLIP
                    }
                    KuStatus.OBSERVED -> {
                        currentStatus = if (recurrenceSatisfied) KuStatus.RECURRENT else KuStatus.SLIP
                    }
                    KuStatus.SLIP -> {
                        if (recurrenceSatisfied) currentStatus = KuStatus.RECURRENT
                    }
                    KuStatus.RECOVERY -> {
                        currentStatus = KuStatus.RECURRENT // Relapse
                    }
                    KuStatus.CONSOLIDATED -> {
                        currentStatus = KuStatus.UNSTABLE
                        everConsolidated = true
                        dueAt = event.at + config.unstable.retestDays * 86400_000L
                    }
                    KuStatus.UNSTABLE -> {
                        currentStatus = KuStatus.RECURRENT
                    }
                    KuStatus.RECURRENT -> {
                        // Remains recurrent
                    }
                }

                if (prevStatus != currentStatus) {
                    statusSince = event.at
                }
            } else if (isSuccess) {
                lastSuccessAt = event.at
                currentRunSuccesses.add(event)

                // Scheduler step logic
                val timeSincePrevAttempt = lastCountedAttemptAt?.let { event.at - it } ?: Long.MAX_VALUE
                val isNewExercise = processedEvents.none { it.exerciseId == event.exerciseId }
                val isRetryInSameSession = processedEvents.any {
                    it.sessionId == event.sessionId && it.outcome == "error"
                }

                if (currentStatus == KuStatus.UNTESTED) {
                    stepIndex = 0
                    val ladderDay = ladder.getOrElse(stepIndex) { ladder.last() }
                    dueAt = event.at + ladderDay * 86400_000L
                } else if (!isRetryInSameSession && isNewExercise && timeSincePrevAttempt >= advanceMinMillis) {
                    stepIndex = min(stepIndex + 1, ladder.size - 1)
                    val ladderDay = ladder[stepIndex]
                    dueAt = event.at + ladderDay * 86400_000L
                }

                val prevStatus = currentStatus
                when (currentStatus) {
                    KuStatus.UNTESTED -> {
                        currentStatus = KuStatus.OBSERVED
                    }
                    KuStatus.SLIP -> {
                        val resolveMillis = config.slip.resolveAfterHours * 3600_000L
                        val errorEvent = processedEvents.filter { it.outcome == "error" }.maxByOrNull { it.at }
                        val isDiffExercise = errorEvent == null || errorEvent.exerciseId != event.exerciseId
                        val isDelayed = errorEvent == null || (event.at - errorEvent.at) >= resolveMillis

                        if (isDiffExercise && isDelayed) {
                            currentStatus = KuStatus.OBSERVED
                        }
                    }
                    KuStatus.RECURRENT -> {
                        val errorEvent = processedEvents.filter { it.outcome == "error" }.maxByOrNull { it.at }
                        val isDiffExercise = errorEvent == null || errorEvent.exerciseId != event.exerciseId
                        if (isDiffExercise) {
                            currentStatus = KuStatus.RECOVERY
                        }
                    }
                    KuStatus.UNSTABLE -> {
                        val delayMillis = config.consolidation.delayHours * 3600_000L
                        val isNewEx = lastErrorExerciseId == null || lastErrorExerciseId != event.exerciseId
                        val isDiffContext = lastErrorContext == null || lastErrorContext != event.context
                        val isDelayed = lastErrorAt == null || (event.at - lastErrorAt) >= delayMillis

                        if (isNewEx && isDiffContext && isDelayed) {
                            currentStatus = KuStatus.CONSOLIDATED
                            everConsolidated = true
                        }
                    }
                    else -> {}
                }

                // Check consolidation criteria (section 8) for RECOVERY or OBSERVED
                if (currentStatus == KuStatus.RECOVERY || currentStatus == KuStatus.OBSERVED) {
                    if (Consolidation.isConsolidated(processedEvents + event, currentRunSuccesses, config.consolidation, timeZone)) {
                        currentStatus = KuStatus.CONSOLIDATED
                        everConsolidated = true
                    }
                }

                if (prevStatus != currentStatus) {
                    statusSince = event.at
                }
            }

            lastCountedAttemptAt = event.at
            processedEvents.add(event)
        }

        // Aggregate statistics
        val validErrors = countedEvents.count { it.outcome == "error" }
        val opportunities = countedEvents.size
        val successRun = currentRunSuccesses.size

        val contextsSucceeded = countedEvents
            .filter { it.outcome == "correct" }
            .map { it.context }
            .distinct()

        val daysSucceeded = countedEvents
            .filter { it.outcome == "correct" }
            .map { Consolidation.toLocalDateString(it.at, timeZone) }
            .distinct()

        // pHat calculation (Section 7.3)
        // alpha = 1 + \sum w * s * d
        // beta = 1 + \sum w * (1 - s) * d
        // d = 0.5^(ageDays / halfLifeDays)
        var alpha = 1.0
        var beta = 1.0

        for (event in countedEvents) {
            val ageDays = max(0.0, (now - event.at).toDouble() / 86400_000.0)
            val decay = 0.5.pow(ageDays / config.evidence.halfLifeDays)
            val score = when (event.outcome) {
                "correct" -> 1.0
                "partial" -> config.evidence.partialCredit
                else -> 0.0
            }
            alpha += event.weight * score * decay
            beta += event.weight * (1.0 - score) * decay
        }

        val nEff = max(0.0, alpha + beta - 2.0)
        val pHat = if (alpha + beta > 0) alpha / (alpha + beta) else 0.5

        return KuState(
            userId = userId,
            kuId = kuId,
            status = currentStatus,
            statusSince = statusSince,
            opportunities = opportunities,
            validErrors = validErrors,
            successRun = successRun,
            recurrenceCount = recurrenceCount,
            contextsSucceeded = contextsSucceeded,
            daysSucceeded = daysSucceeded,
            lastErrorAt = lastErrorAt,
            lastSuccessAt = lastSuccessAt,
            everConsolidated = everConsolidated,
            stepIndex = stepIndex,
            dueAt = dueAt,
            retryPending = retryPending,
            alpha = alpha,
            beta = beta,
            pHat = pHat,
            nEff = nEff,
            configVersion = config.version,
            derivedFromEventCount = activeEvents.size
        )
    }
}

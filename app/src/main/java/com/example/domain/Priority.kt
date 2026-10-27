package com.example.domain

import kotlin.math.max
import kotlin.math.min

data class PriorityBreakdown(
    val baseScore: Double,
    val overdueBonus: Double,
    val recurrenceBonus: Double,
    val transferGapBonus: Double,
    val instabilityBonus: Double,
    val recentPenalty: Double,
    val totalScore: Double,
    val reasonCode: ReasonCode
)

object Priority {

    fun computePriority(
        state: KuState,
        config: LearningConfig,
        now: Long,
        recentKuIdsInSession: List<String> = emptyList()
    ): PriorityBreakdown {
        val prioCfg = config.priority
        val base = prioCfg.statusBase[state.status] ?: 10.0

        // Overdue calculation
        val currentStepDays = config.scheduler.ladderDays.getOrElse(state.stepIndex) {
            config.scheduler.ladderDays.last()
        }
        val currentStepMillis = currentStepDays * 86400_000.0

        val overdueRatio = if (state.dueAt != null && now > state.dueAt) {
            (now - state.dueAt).toDouble() / currentStepMillis
        } else {
            0.0
        }
        val overdueBonus = prioCfg.overdueWeight * min(overdueRatio, prioCfg.overdueCap)

        // Recurrence bonus
        val recurrenceBonus = prioCfg.recurrenceWeight * min(
            state.recurrenceCount.toDouble(),
            prioCfg.recurrenceCap
        )

        // Transfer gap
        val hasTransferGap = (state.status == KuStatus.OBSERVED || state.status == KuStatus.RECOVERY) &&
                state.contextsSucceeded.size < config.consolidation.minDistinctContexts
        val transferBonus = if (hasTransferGap) prioCfg.transferGapBonus else 0.0

        // Instability bonus
        val instabilityBonus = prioCfg.instabilityWeight * (1.0 - state.pHat)

        // Recently shown in session
        val isRecentlyShown = recentKuIdsInSession.takeLast(2).contains(state.kuId)
        val recentPenalty = if (isRecentlyShown) prioCfg.recentlyShownPenalty else 0.0

        val total = base + overdueBonus + recurrenceBonus + transferBonus + instabilityBonus - recentPenalty

        // Determine reason code
        val reasonCode = when {
            state.status == KuStatus.RECURRENT -> ReasonCode.RECURRING_PATTERN
            state.status == KuStatus.UNSTABLE -> ReasonCode.UNSTABLE_AFTER_CONSOLIDATION
            state.status == KuStatus.RECOVERY -> ReasonCode.IN_RECOVERY
            state.status == KuStatus.SLIP -> ReasonCode.RECENT_SLIP
            overdueRatio > 0 -> ReasonCode.REVIEW_DUE
            hasTransferGap -> ReasonCode.TRANSFER_CHECK
            state.status == KuStatus.UNTESTED -> ReasonCode.NEW_AREA
            else -> ReasonCode.REVIEW_DUE
        }

        return PriorityBreakdown(
            baseScore = base,
            overdueBonus = overdueBonus,
            recurrenceBonus = recurrenceBonus,
            transferGapBonus = transferBonus,
            instabilityBonus = instabilityBonus,
            recentPenalty = recentPenalty,
            totalScore = total,
            reasonCode = reasonCode
        )
    }
}

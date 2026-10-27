package com.example.domain

data class EvidenceConfig(
    val errorMinConfidence: Double = 0.7,
    val halfLifeDays: Double = 21.0,
    val partialCredit: Double = 0.5,
    val secondaryKuWeight: Double = 0.5
)

data class RecurrenceConfig(
    val windowDays: Int = 30,
    val minDistinctExercises: Int = 2,
    val minDistinctSessions: Int = 2,
    val massedErrorsInOneSession: Int = 3
)

data class SlipConfig(
    val resolveAfterHours: Int = 24
)

data class ConsolidationConfig(
    val minSuccesses: Int = 3,
    val minDistinctExercises: Int = 3,
    val minDistinctContexts: Int = 2,
    val minDistinctDays: Int = 2,
    val minDelayedSuccesses: Int = 1,
    val delayHours: Int = 24,
    val requireConstructedSuccess: Boolean = true
)

data class UnstableConfig(
    val retestDays: Int = 3
)

data class SchedulerConfig(
    val ladderDays: List<Int> = listOf(1, 3, 7, 14, 30, 60),
    val errorStepDrop: Int = 2,
    val advanceMinHours: Int = 12,
    val retryMinGapItems: Int = 3
)

data class PlannerConfig(
    val sessionSize: Int = 8,
    val problemQuota: Int = 4,
    val dueQuota: Int = 2,
    val exploreQuota: Int = 2,
    val maxConsecutiveSameKu: Int = 2,
    val maxShareSingleKu: Double = 0.4,
    val coldStartMinOpportunities: Int = 2
)

data class PriorityConfig(
    val statusBase: Map<KuStatus, Double> = mapOf(
        KuStatus.RECURRENT to 100.0,
        KuStatus.UNSTABLE to 80.0,
        KuStatus.RECOVERY to 60.0,
        KuStatus.SLIP to 40.0,
        KuStatus.UNTESTED to 30.0,
        KuStatus.OBSERVED to 20.0,
        KuStatus.CONSOLIDATED to 5.0
    ),
    val overdueWeight: Double = 20.0,
    val overdueCap: Double = 2.0,
    val recurrenceWeight: Double = 10.0,
    val recurrenceCap: Double = 3.0,
    val transferGapBonus: Double = 15.0,
    val instabilityWeight: Double = 10.0,
    val recentlyShownPenalty: Double = 30.0
)

data class EvalConfig(
    val timeoutMs: Long = 8000L,
    val retries: Int = 1,
    val verifyUncorroboratedErrors: Boolean = true
)

data class LearningConfig(
    val version: String = "cfg-1",
    val evidence: EvidenceConfig = EvidenceConfig(),
    val recurrence: RecurrenceConfig = RecurrenceConfig(),
    val slip: SlipConfig = SlipConfig(),
    val consolidation: ConsolidationConfig = ConsolidationConfig(),
    val unstable: UnstableConfig = UnstableConfig(),
    val scheduler: SchedulerConfig = SchedulerConfig(),
    val planner: PlannerConfig = PlannerConfig(),
    val priority: PriorityConfig = PriorityConfig(),
    val eval: EvalConfig = EvalConfig()
)

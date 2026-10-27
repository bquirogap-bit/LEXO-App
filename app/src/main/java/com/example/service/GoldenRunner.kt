package com.example.service

import com.example.domain.*

data class GoldenReport(
    val totalCases: Int,
    val falseErrorCount: Int,
    val falseErrorRate: Double,
    val missedErrorCount: Int,
    val missedErrorRate: Double,
    val uncertainCount: Int,
    val uncertainRate: Double,
    val perKuResults: Map<String, KuGoldenStats>,
    val caseResults: List<CaseResult>
)

data class KuGoldenStats(
    val total: Int,
    val falseErrors: Int,
    val missedErrors: Int,
    val uncertain: Int
)

data class CaseResult(
    val caseId: String,
    val kuId: String,
    val response: String,
    val expectedOutcome: String,
    val actualOutcome: String,
    val passed: Boolean
)

object GoldenRunner {

    suspend fun runGoldenSet(
        cases: List<GoldenCase>,
        pack: ContentPack,
        evaluator: EvaluatorPort
    ): GoldenReport {
        var falseErrors = 0
        var missedErrors = 0
        var uncertainCount = 0
        val caseResults = mutableListOf<CaseResult>()
        val kuStatsMap = mutableMapOf<String, MutableList<CaseResult>>()

        for (case in cases) {
            val ex = pack.exercises.firstOrNull { it.id == case.exercise_id } ?: continue
            val targets = pack.knowledge_units.filter { it.id == case.ku_id }

            val rawEval = evaluator.evaluate(ex, targets, case.response)
            val primaryEval = rawEval.per_ku.firstOrNull { it.ku_id == case.ku_id }

            val actualOutcome = primaryEval?.outcome ?: "unclear"
            val expected = case.expected_outcome

            val isFalseError = expected == "correct" && actualOutcome == "error"
            val isMissedError = expected == "error" && actualOutcome == "correct"
            val isUncertain = actualOutcome == "unclear" || actualOutcome == "avoided"

            if (isFalseError) falseErrors++
            if (isMissedError) missedErrors++
            if (isUncertain) uncertainCount++

            val passed = actualOutcome == expected || (expected == "avoided" && actualOutcome == "avoided")
            val cr = CaseResult(case.id, case.ku_id, case.response, expected, actualOutcome, passed)
            caseResults.add(cr)

            kuStatsMap.getOrPut(case.ku_id) { mutableListOf() }.add(cr)
        }

        val total = cases.size.coerceAtLeast(1)
        val perKu = kuStatsMap.mapValues { (_, list) ->
            KuGoldenStats(
                total = list.size,
                falseErrors = list.count { it.expectedOutcome == "correct" && it.actualOutcome == "error" },
                missedErrors = list.count { it.expectedOutcome == "error" && it.actualOutcome == "correct" },
                uncertain = list.count { it.actualOutcome == "unclear" || it.actualOutcome == "avoided" }
            )
        }

        return GoldenReport(
            totalCases = cases.size,
            falseErrorCount = falseErrors,
            falseErrorRate = falseErrors.toDouble() / total,
            missedErrorCount = missedErrors,
            missedErrorRate = missedErrors.toDouble() / total,
            uncertainCount = uncertainCount,
            uncertainRate = uncertainCount.toDouble() / total,
            perKuResults = perKu,
            caseResults = caseResults
        )
    }
}

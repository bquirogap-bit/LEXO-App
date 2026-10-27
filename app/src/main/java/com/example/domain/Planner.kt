package com.example.domain

import java.util.UUID
import kotlin.math.floor

object Planner {

    fun planSession(
        pack: ContentPack,
        kuStates: Map<String, KuState>,
        allEvents: List<EvidenceEvent>,
        config: LearningConfig,
        now: Long,
        sessionId: String = "sess_${UUID.randomUUID().toString().take(8)}",
        vocabularyWords: List<VocabularyWord> = emptyList()
    ): PlannedSession {
        val sessionSize = config.planner.sessionSize
        val maxPerKu = floor(sessionSize * config.planner.maxShareSingleKu).toInt().coerceAtLeast(1)

        val isColdStart = allEvents.isEmpty() || kuStates.values.all {
            it.status == KuStatus.UNTESTED || (it.opportunities < config.planner.coldStartMinOpportunities && it.status == KuStatus.OBSERVED)
        }

        // Priority breakdown for each KU
        val priorityMap = kuStates.mapValues { (_, state) ->
            Priority.computePriority(state, config, now)
        }

        // Bucket separation
        val problemPool = kuStates.values
            .filter { it.status in listOf(KuStatus.RECURRENT, KuStatus.UNSTABLE, KuStatus.RECOVERY, KuStatus.SLIP) }
            .sortedByDescending { priorityMap[it.kuId]?.totalScore ?: 0.0 }
            .map { it.kuId }
            .toMutableList()

        val duePool = kuStates.values
            .filter { (it.dueAt != null && it.dueAt <= now) && it.status !in listOf(KuStatus.RECURRENT, KuStatus.UNSTABLE, KuStatus.RECOVERY, KuStatus.SLIP) }
            .sortedByDescending { priorityMap[it.kuId]?.totalScore ?: 0.0 }
            .map { it.kuId }
            .toMutableList()

        val explorePool = pack.knowledge_units
            .filter { (kuStates[it.id]?.status ?: KuStatus.UNTESTED) == KuStatus.UNTESTED }
            .map { it.id }
            .toMutableList()

        val chosenKuIds = mutableListOf<String>()
        val kuCounts = mutableMapOf<String, Int>()

        fun canAddKu(kuId: String): Boolean {
            val count = kuCounts.getOrDefault(kuId, 0)
            if (count >= maxPerKu) return false
            // Max consecutive same KU check
            val lastConsecutive = chosenKuIds.takeLast(config.planner.maxConsecutiveSameKu)
            if (lastConsecutive.size == config.planner.maxConsecutiveSameKu && lastConsecutive.all { it == kuId }) {
                return false
            }
            return true
        }

        if (isColdStart) {
            // Cold start: explore across all available KUs in pack
            var round = 0
            while (chosenKuIds.size < sessionSize && round < 10) {
                for (ku in pack.knowledge_units) {
                    if (chosenKuIds.size < sessionSize && canAddKu(ku.id)) {
                        chosenKuIds.add(ku.id)
                        kuCounts[ku.id] = kuCounts.getOrDefault(ku.id, 0) + 1
                    }
                }
                round++
            }
        } else {
            // Standard Quota allocation: problem (4), due (2), explore (2)
            fun takeFromPool(pool: MutableList<String>, count: Int) {
                var taken = 0
                val it = pool.iterator()
                while (it.hasNext() && taken < count && chosenKuIds.size < sessionSize) {
                    val candidate = it.next()
                    if (canAddKu(candidate)) {
                        chosenKuIds.add(candidate)
                        kuCounts[candidate] = kuCounts.getOrDefault(candidate, 0) + 1
                        taken++
                    }
                }
            }

            takeFromPool(problemPool, config.planner.problemQuota)
            takeFromPool(duePool, config.planner.dueQuota)
            takeFromPool(explorePool, config.planner.exploreQuota)

            // Backfill from remaining priority-sorted KUs if quotas not filled
            val allSortedKus = pack.knowledge_units
                .map { it.id }
                .sortedByDescending { priorityMap[it]?.totalScore ?: 0.0 }

            for (kuId in allSortedKus) {
                while (chosenKuIds.size < sessionSize && canAddKu(kuId)) {
                    chosenKuIds.add(kuId)
                    kuCounts[kuId] = kuCounts.getOrDefault(kuId, 0) + 1
                }
            }
        }

        // Now select exercises for the chosen KUs
        val plannedItems = mutableListOf<PlannedItem>()
        val usedExerciseIdsInSession = mutableSetOf<String>()

        val formatRotation = listOf(
            "writing" to "in_context",
            "speaking" to "speaking",
            "word_tiles" to "word_tiles",
            "writing" to "translation",
            "scramble" to "sentence_scramble",
            "writing" to "open_production",
            "speaking" to "speaking",
            "word_tiles" to "word_tiles"
        )

        for ((idx, kuId) in chosenKuIds.withIndex()) {
            val state = kuStates[kuId]
            val kuEvents = allEvents.filter { it.kuId == kuId && it.status == "active" }
            val priority = priorityMap[kuId]

            val exercise = selectExerciseForKu(
                kuId = kuId,
                pack = pack,
                kuEvents = kuEvents,
                state = state,
                sessionUsedExercises = usedExerciseIdsInSession
            )

            if (exercise != null) {
                usedExerciseIdsInSession.add(exercise.id)
                val code = priority?.reasonCode ?: (if (isColdStart) ReasonCode.NEW_AREA else ReasonCode.REVIEW_DUE)

                val (preferredModality, preferredFormat) = formatRotation.getOrElse(idx % formatRotation.size) {
                    "writing" to "standard"
                }

                // Match with word if available
                val associatedWord = vocabularyWords.firstOrNull { it.relatedKuId == kuId && (it.dueAt == null || it.dueAt <= now) }?.word

                val item = PlannedItem(
                    itemId = "item_${UUID.randomUUID().toString().take(8)}",
                    exerciseId = exercise.id,
                    kuId = kuId,
                    reasonCode = code,
                    reasonTextEs = code.textEs,
                    reasonTextEn = code.textEn,
                    modality = preferredModality,
                    exerciseFormat = if (exercise.type == "translation") "translation" else preferredFormat,
                    associatedWord = associatedWord
                )
                plannedItems.add(item)
            }
        }

        val rationale = if (isColdStart) {
            "Exploring foundation areas to map your patterns"
        } else {
            val problemCount = kuCounts.filterKeys { k ->
                kuStates[k]?.status in listOf(KuStatus.RECURRENT, KuStatus.UNSTABLE, KuStatus.RECOVERY, KuStatus.SLIP)
            }.values.sum()
            val dueCount = kuCounts.filterKeys { k ->
                val s = kuStates[k]
                s?.dueAt != null && s.dueAt <= now
            }.values.sum()
            val exploreCount = plannedItems.size - problemCount - dueCount
            "$problemCount patterns to stabilize · $dueCount review due · $exploreCount new areas"
        }

        return PlannedSession(
            sessionId = sessionId,
            items = plannedItems,
            rationale = rationale,
            isFocusedPractice = false
        )
    }

    /**
     * Generates a targeted, varied mini-session for Focused Practice Mode on a specific issue.
     * Prevents mechanical repetition by varying context, superficial structure, and modality (Writing/Speaking).
     */
    fun planFocusedSession(
        kuId: String,
        pack: ContentPack,
        kuStates: Map<String, KuState>,
        allEvents: List<EvidenceEvent>,
        config: LearningConfig,
        now: Long,
        sessionId: String = "focus_${UUID.randomUUID().toString().take(8)}"
    ): PlannedSession {
        val candidates = pack.exercises.filter { it.ku == kuId || it.secondary_ku_ids.contains(kuId) }
        val kuEvents = allEvents.filter { it.kuId == kuId && it.status == "active" }
        val ku = pack.knowledge_units.firstOrNull { it.id == kuId }
        val kuName = ku?.name ?: kuId

        // Pick up to 4 exercises with maximum variety across contexts
        val distinctContextExercises = candidates
            .groupBy { it.context }
            .values
            .map { it.first() }
            .take(4)
            .toMutableList()

        if (distinctContextExercises.size < 4 && candidates.size > distinctContextExercises.size) {
            val remaining = candidates.filter { it !in distinctContextExercises }
            distinctContextExercises.addAll(remaining.take(4 - distinctContextExercises.size))
        }

        val formats = listOf("writing", "speaking", "word_tiles", "sentence_scramble")
        val finalExercises = mutableListOf<Exercise>()
        if (candidates.isNotEmpty()) {
            while (finalExercises.size < 4) {
                for (cand in distinctContextExercises) {
                    if (finalExercises.size < 4) finalExercises.add(cand)
                }
                for (cand in candidates.filter { it !in distinctContextExercises }) {
                    if (finalExercises.size < 4) finalExercises.add(cand)
                }
            }
        }

        val items = finalExercises.mapIndexed { idx, ex ->
            val format = formats.getOrElse(idx % formats.size) { "writing" }
            val modality = when (format) {
                "speaking" -> "speaking"
                "word_tiles" -> "word_tiles"
                "sentence_scramble" -> "scramble"
                else -> "writing"
            }
            PlannedItem(
                itemId = "item_f_${UUID.randomUUID().toString().take(8)}",
                exerciseId = ex.id,
                kuId = kuId,
                reasonCode = ReasonCode.RECURRING_PATTERN,
                reasonTextEs = "Práctica enfocada: Variación de contexto y producción activa para romper el error recurrente.",
                reasonTextEn = "Focused practice: Context variation and active production to overcome recurring error.",
                modality = modality,
                exerciseFormat = format
            )
        }

        val rationale = if (items.isEmpty()) {
            "No hay ejercicios disponibles para el patrón $kuName en este paquete de contenido."
        } else {
            "Focused Practice on $kuName · Context & format variation"
        }

        return PlannedSession(
            sessionId = sessionId,
            items = items,
            rationale = rationale,
            isFocusedPractice = true,
            focusedKuId = kuId
        )
    }

    private fun selectExerciseForKu(
        kuId: String,
        pack: ContentPack,
        kuEvents: List<EvidenceEvent>,
        state: KuState?,
        sessionUsedExercises: Set<String>
    ): Exercise? {
        val candidates = pack.exercises.filter { (it.ku == kuId || it.secondary_ku_ids.contains(kuId)) && !sessionUsedExercises.contains(it.id) }
        if (candidates.isEmpty()) {
            return pack.exercises.firstOrNull { it.ku == kuId }
        }

        val attemptedExerciseIds = kuEvents.map { it.exerciseId }.toSet()
        val unused = candidates.filter { !attemptedExerciseIds.contains(it.id) }

        val pool = if (unused.isNotEmpty()) unused else candidates

        // Context preference: prefer different from last 2 attempts
        val recentContexts = kuEvents.takeLast(2).map { it.context }.toSet()

        // Elicitation preference
        val preferConstrained = state?.status in listOf(KuStatus.RECURRENT, KuStatus.SLIP, KuStatus.RECOVERY)
        val preferConstructed = state?.status in listOf(KuStatus.OBSERVED, KuStatus.CONSOLIDATED)

        return pool.sortedWith(
            compareBy<Exercise> { ex ->
                // Avoid recent contexts if possible
                if (recentContexts.contains(ex.context)) 1 else 0
            }.thenBy { ex ->
                // Match preferred elicitation
                if (preferConstrained && ex.elicitation == "constrained") 0
                else if (preferConstructed && ex.elicitation == "constructed") 0
                else 1
            }
        ).firstOrNull() ?: pool.first()
    }
}

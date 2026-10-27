package com.example.domain

object MasteryCalculator {

    fun calculateMilestones(
        pack: ContentPack?,
        kuStates: Map<String, KuState>,
        allEvents: List<EvidenceEvent>,
        vocabularyWords: List<VocabularyWord>
    ): List<MasteryMilestone> {
        val activeEvents = allEvents.filter { it.status == "active" }
        val consolidatedCount = kuStates.values.count { it.status == KuStatus.CONSOLIDATED }
        val recoveringCount = kuStates.values.count { it.status == KuStatus.RECOVERY }
        val totalOpportunities = kuStates.values.sumOf { it.opportunities }

        // 1. Dual Modality: Check how many KUs have counted successes in BOTH spoken and typed
        val typedKus = activeEvents
            .filter { it.modality == "typed" && it.outcome == "correct" }
            .map { it.kuId }.toSet()
        val spokenKus = activeEvents
            .filter { it.modality == "spoken" && it.outcome == "correct" }
            .map { it.kuId }.toSet()
        val dualModalityKus = typedKus.intersect(spokenKus)

        // 2. Multicontext Transfer: KUs with successful production in >= 2 distinct contexts
        val transferKus = kuStates.values.filter { it.contextsSucceeded.size >= 2 }

        // 3. Overcome Recurrences: KUs that were recurrent but have advanced to RECOVERY or CONSOLIDATED
        val overcomeRecurrences = kuStates.values.filter {
            it.recurrenceCount > 0 && (it.status == KuStatus.RECOVERY || it.status == KuStatus.CONSOLIDATED)
        }

        // 4. Vocabulary Integrated: Words that have been reviewed at least once
        val wordsPracticed = vocabularyWords.count { it.reviewCount > 0 }

        // 5. Independent Autonomous Production: Count of consecutive unassisted successes
        val unassistedSuccesses = activeEvents.count { it.outcome == "correct" && it.hintsUsed == 0 }

        return listOf(
            MasteryMilestone(
                id = "m_first_consolidation",
                title = "Primer Patrón Automatizado",
                description = "Consolidaste un patrón lingüístico en múltiples contextos y días sin requerir pistas.",
                category = "consolidation",
                isUnlocked = consolidatedCount >= 1,
                currentProgress = consolidatedCount,
                targetProgress = 1,
                evidenceDetail = if (consolidatedCount >= 1) "Alcanzado: $consolidatedCount patrón(es) consolidados" else "Completa 3 éxitos en 2 contextos y días distintos"
            ),
            MasteryMilestone(
                id = "m_five_consolidated",
                title = "Base de Competencia Inconsciente (5 KUs)",
                description = "Cinco patrones gramaticales se producen ahora con automatismo reflejo.",
                category = "consolidation",
                isUnlocked = consolidatedCount >= 5,
                currentProgress = consolidatedCount,
                targetProgress = 5,
                evidenceDetail = "$consolidatedCount de 5 patrones consolidados"
            ),
            MasteryMilestone(
                id = "m_error_overcome",
                title = "Calco Superado",
                description = "Un error recurrente fue detectado, practicado y llevado exitosamente a fase de recuperación.",
                category = "recovery",
                isUnlocked = overcomeRecurrences.isNotEmpty(),
                currentProgress = overcomeRecurrences.size,
                targetProgress = 1,
                evidenceDetail = "${overcomeRecurrences.size} patrón(es) recurrentes en recuperación activa"
            ),
            MasteryMilestone(
                id = "m_multicontext_transfer",
                title = "Transferencia Multicontexto",
                description = "Aplicaste un mismo patrón lingüístico en 2 o más contextos profesionales distintos.",
                category = "transfer",
                isUnlocked = transferKus.isNotEmpty(),
                currentProgress = transferKus.size,
                targetProgress = 2,
                evidenceDetail = "${transferKus.size} patrón(es) transferidos a través de contextos"
            ),
            MasteryMilestone(
                id = "m_dual_modality",
                title = "Dominio Bimodal (Speaking + Writing)",
                description = "Demostraste el mismo dominio lingüístico tanto al hablar como al escribir.",
                category = "modality",
                isUnlocked = dualModalityKus.isNotEmpty(),
                currentProgress = dualModalityKus.size,
                targetProgress = 1,
                evidenceDetail = if (dualModalityKus.isNotEmpty()) "Verificado en: ${dualModalityKus.size} patrón(es)" else "Produce el patrón de forma hablada y escrita"
            ),
            MasteryMilestone(
                id = "m_vocab_integrated",
                title = "Vocabulario en Producción Activa",
                description = "Palabras del diccionario integradas orgánicamente dentro de tus sesiones de práctica.",
                category = "vocabulary",
                isUnlocked = wordsPracticed >= 3,
                currentProgress = wordsPracticed,
                targetProgress = 3,
                evidenceDetail = "$wordsPracticed de 3 palabras practicadas en contexto"
            ),
            MasteryMilestone(
                id = "m_independent_production",
                title = "Producción Autónoma (Sin Pistas)",
                description = "Alcanzaste 10 producciones correctas consecutivas sin necesidad de andamiaje.",
                category = "independence",
                isUnlocked = unassistedSuccesses >= 10,
                currentProgress = unassistedSuccesses.coerceAtMost(10),
                targetProgress = 10,
                evidenceDetail = "$unassistedSuccesses respuestas autónomas correctas registradas"
            )
        )
    }
}

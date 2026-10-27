package com.example.domain

object ImprovementDetector {

    fun detectIssues(
        pack: ContentPack?,
        kuStates: Map<String, KuState>,
        allEvents: List<EvidenceEvent>,
        config: LearningConfig,
        now: Long
    ): List<ImprovementIssue> {
        if (pack == null) return emptyList()

        val activeEvents = allEvents.filter { it.status == "active" }
        val issues = mutableListOf<ImprovementIssue>()

        for (ku in pack.knowledge_units) {
            val state = kuStates[ku.id] ?: continue
            val kuEvents = activeEvents.filter { it.kuId == ku.id }
            val errorEvents = kuEvents.filter { it.outcome == "error" }

            // Include if status is problem-oriented or has registered valid errors
            val isProblematic = state.status in listOf(
                KuStatus.RECURRENT,
                KuStatus.UNSTABLE,
                KuStatus.SLIP,
                KuStatus.RECOVERY
            ) || state.validErrors > 0

            if (isProblematic) {
                val priority = Priority.computePriority(state, config, now)

                // Recent quotes from learner production
                val recentQuotes = errorEvents
                    .takeLast(3)
                    .mapNotNull { it.quote }
                    .filter { it.isNotBlank() }
                    .distinct()

                // Last signature or prominent error
                val lastSigId = errorEvents.lastOrNull()?.signatureId
                val sig = pack.error_signatures.firstOrNull { it.id == lastSigId }

                val friendlyTitle = when {
                    ku.id.contains("agree") -> "Agree — calco con verbo 'to be' (I am agree)"
                    ku.id.contains("depend") -> "Depend — preposición incorrecta (depend of)"
                    ku.id.contains("tps_s") || ku.id.contains("third_person") -> "Tercera persona singular — omisión de -s / -es"
                    ku.id.contains("future_be_going_to") -> "Going to — omisión de 'be' (I going to)"
                    ku.id.contains("subject_explicit") || ku.id.contains("dummy") -> "Sujeto obligatorio — omisión de 'it' en impersonales"
                    ku.id.contains("past_simple_aux") || ku.id.contains("did") -> "Auxiliar de pasado — doble marcación (didn't went)"
                    ku.id.contains("since_for") -> "Duración vs Inicio — confusión de 'for' y 'since'"
                    ku.id.contains("modal") -> "Modales básicos — intercalación indebida de 'to'"
                    ku.id.contains("discuss") -> "Discuss — uso innecesario de 'about'"
                    ku.id.contains("connectors") -> "Conectores — confusión de cláusula con despite / although"
                    ku.id.contains("present_perf_vs_past") -> "Present Perfect vs Past Simple — tiempo cerrado"
                    else -> ku.name
                }

                val action = when (state.status) {
                    KuStatus.RECURRENT -> "Práctica enfocada para romper el calco recurrente"
                    KuStatus.UNSTABLE -> "Revisión inmediata para re-estabilizar el patrón"
                    KuStatus.SLIP -> "Verificación para asegurar que fue un desliz aislado"
                    KuStatus.RECOVERY -> "Práctica de consolidación en contextos variados"
                    else -> "Práctica espaciada de retención"
                }

                val explanation = if (sig != null) {
                    sig.feedback_es.replace("{quote}", recentQuotes.firstOrNull() ?: "tu respuesta")
                        .replace("{correction}", "la forma correcta")
                } else {
                    ku.explanation["es"] ?: ku.behavior
                }

                issues.add(
                    ImprovementIssue(
                        id = "issue_${ku.id}",
                        title = friendlyTitle,
                        kuId = ku.id,
                        kuName = ku.name,
                        signatureId = lastSigId,
                        status = state.status,
                        frequency = state.validErrors.coerceAtLeast(errorEvents.size),
                        recentQuotes = recentQuotes,
                        priorityScore = priority.totalScore,
                        dueAt = state.dueAt,
                        recommendedAction = action,
                        explanation = explanation
                    )
                )
            }
        }

        // Sort by priority score descending
        return issues.sortedByDescending { it.priorityScore }
    }
}

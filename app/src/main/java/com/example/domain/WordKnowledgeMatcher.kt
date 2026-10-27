package com.example.domain

import java.util.UUID

object WordKnowledgeMatcher {

    data class SuggestedWordAnalysis(
        val word: String,
        val definitionEs: String,
        val definitionEn: String,
        val partOfSpeech: String,
        val relatedKuId: String?,
        val naturalContexts: List<String>,
        val exampleSentence: String
    )

    data class ContextualWordLookup(
        val word: String,
        val translationEs: String,
        val contextualMeaningEs: String,
        val partOfSpeech: String,
        val relatedKuId: String?,
        val relatedKuName: String?,
        val originSentence: String
    )

    // Pre-curated catalog of high-frequency professional words linked to LEXO's KUs
    private val curatedWords = listOf(
        SuggestedWordAnalysis(
            word = "negotiate",
            definitionEs = "Negociar acuerdos, contratos o condiciones.",
            definitionEn = "Discuss something with someone to reach an agreement.",
            partOfSpeech = "verb",
            relatedKuId = "ku_modals_can_could_bare",
            naturalContexts = listOf("Negotiation", "Business", "Leadership"),
            exampleSentence = "We can negotiate the key terms before signing the contract."
        ),
        SuggestedWordAnalysis(
            word = "schedule",
            definitionEs = "Planificar, programar una reunión o entrega.",
            definitionEn = "Arrange or plan an event or activity for a certain time.",
            partOfSpeech = "verb",
            relatedKuId = "ku_future_be_going_to_vs_will",
            naturalContexts = listOf("ProjectManagement", "Meetings", "Business"),
            exampleSentence = "We are going to schedule a review meeting for Friday."
        ),
        SuggestedWordAnalysis(
            word = "depend",
            definitionEs = "Depender de factores, personas o decisiones.",
            definitionEn = "Be controlled or determined by.",
            partOfSpeech = "verb",
            relatedKuId = "ku_depend_on",
            naturalContexts = listOf("Business", "Technology", "Negotiation"),
            exampleSentence = "The project timeline depends on the client's approval."
        ),
        SuggestedWordAnalysis(
            word = "manage",
            definitionEs = "Gestionar, administrar recursos o lograr un objetivo.",
            definitionEn = "Be in charge of or succeed in achieving something.",
            partOfSpeech = "verb",
            relatedKuId = "ku_wordfamily_manage",
            naturalContexts = listOf("Leadership", "ProjectManagement", "Business"),
            exampleSentence = "Effective management ensures team productivity."
        ),
        SuggestedWordAnalysis(
            word = "discuss",
            definitionEs = "Debatir o tratar un asunto formalmente.",
            definitionEn = "Talk about something with another person or group.",
            partOfSpeech = "verb",
            relatedKuId = "ku_discuss_no_about",
            naturalContexts = listOf("Meetings", "Presentations", "Business"),
            exampleSentence = "Let's discuss the strategic proposal during the call."
        ),
        SuggestedWordAnalysis(
            word = "avoid",
            definitionEs = "Evitar riesgos, retrasos o errores.",
            definitionEn = "Keep away from or prevent from happening.",
            partOfSpeech = "verb",
            relatedKuId = "ku_verb_pattern_gerund_infinitive",
            naturalContexts = listOf("Technology", "ProjectManagement", "Leadership"),
            exampleSentence = "We must avoid making assumptions without validated data."
        ),
        SuggestedWordAnalysis(
            word = "agree",
            definitionEs = "Estar de acuerdo o consentir con una postura.",
            definitionEn = "Have the same opinion or reach consensus.",
            partOfSpeech = "verb",
            relatedKuId = "ku_agree_verb",
            naturalContexts = listOf("Meetings", "Negotiation", "Leadership"),
            exampleSentence = "I agree with your recommendation regarding the budget."
        ),
        SuggestedWordAnalysis(
            word = "since",
            definitionEs = "Desde un momento puntual específico en el pasado.",
            definitionEn = "From a particular time in the past until now.",
            partOfSpeech = "preposition",
            relatedKuId = "ku_since_for",
            naturalContexts = listOf("Business", "Travel", "Technology"),
            exampleSentence = "We have been developing this product since January."
        ),
        SuggestedWordAnalysis(
            word = "although",
            definitionEs = "Aunque; conector de contraste con cláusula completa.",
            definitionEn = "In spite of the fact that; even though.",
            partOfSpeech = "connector",
            relatedKuId = "ku_connectors_despite_although",
            naturalContexts = listOf("Presentations", "Business", "Negotiation"),
            exampleSentence = "Although the deadline was tight, the team delivered quality work."
        ),
        SuggestedWordAnalysis(
            word = "responsible",
            definitionEs = "Responsable de una tarea o área (responsible for).",
            definitionEn = "Having an obligation to do something, or having control over or care for someone.",
            partOfSpeech = "adjective",
            relatedKuId = "ku_prep_responsible_for",
            naturalContexts = listOf("Leadership", "ProjectManagement", "Business"),
            exampleSentence = "She is responsible for coordinating international stakeholders."
        ),
        SuggestedWordAnalysis(
            word = "deadline",
            definitionEs = "Fecha límite de entrega o vencimiento.",
            definitionEn = "The latest time or date by which something should be completed.",
            partOfSpeech = "noun",
            relatedKuId = "ku_prep_responsible_for",
            naturalContexts = listOf("ProjectManagement", "Business"),
            exampleSentence = "We have to deliver the report before the deadline."
        ),
        SuggestedWordAnalysis(
            word = "budget",
            definitionEs = "Presupuesto asignado a un proyecto o departamento.",
            definitionEn = "An estimate of income and expenditure for a set period.",
            partOfSpeech = "noun",
            relatedKuId = "ku_wordfamily_manage",
            naturalContexts = listOf("Business", "Finance"),
            exampleSentence = "The budget for Q3 has been approved."
        ),
        SuggestedWordAnalysis(
            word = "stakeholders",
            definitionEs = "Partes interesadas o inversores clave.",
            definitionEn = "People with an interest or concern in something.",
            partOfSpeech = "noun",
            relatedKuId = "ku_prep_responsible_for",
            naturalContexts = listOf("Leadership", "Business"),
            exampleSentence = "We presented the quarterly results to the stakeholders."
        ),
        SuggestedWordAnalysis(
            word = "timeline",
            definitionEs = "Cronograma o calendario de fases.",
            definitionEn = "A graphic representation or schedule of events.",
            partOfSpeech = "noun",
            relatedKuId = "ku_future_be_going_to_vs_will",
            naturalContexts = listOf("ProjectManagement", "Technology"),
            exampleSentence = "The project timeline was adjusted after the review."
        )
    )

    fun getDefaultStarterWords(): List<VocabularyWord> {
        val now = System.currentTimeMillis()
        return curatedWords.mapIndexed { idx, sug ->
            VocabularyWord(
                id = "word_${UUID.randomUUID().toString().take(8)}",
                word = sug.word,
                definitionEs = sug.definitionEs,
                definitionEn = sug.definitionEn,
                partOfSpeech = sug.partOfSpeech,
                relatedKuId = sug.relatedKuId,
                naturalContexts = sug.naturalContexts,
                exampleSentence = sug.exampleSentence,
                stepIndex = 0,
                dueAt = now + (idx * 3600_000L), // Staggered initial review
                reviewCount = 0,
                lastReviewedAt = null,
                createdAt = now
            )
        }
    }

    /**
     * Attempts to automatically match an input word to a related KU and linguistic structure.
     */
    fun analyzeWord(input: String, pack: ContentPack?): SuggestedWordAnalysis {
        val clean = input.trim().lowercase()

        // 1. Direct match in curated dictionary
        val curated = curatedWords.firstOrNull { it.word.equals(clean, ignoreCase = true) }
        if (curated != null) return curated

        // 2. Intelligent inference from clean word and available KUs
        val matchingKu = pack?.knowledge_units?.firstOrNull { ku ->
            ku.name.contains(clean, ignoreCase = true) ||
                    ku.behavior.contains(clean, ignoreCase = true) ||
                    ku.obligatory_context.contains(clean, ignoreCase = true)
        }

        val partOfSpeech = when {
            clean.endsWith("ly") -> "adverb"
            clean.endsWith("tion") || clean.endsWith("ment") || clean.endsWith("ness") -> "noun"
            clean.endsWith("able") || clean.endsWith("ive") || clean.endsWith("ful") -> "adjective"
            else -> "verb"
        }

        val defaultContexts = listOf("Business", "Meetings", "Technology")
        val relatedKuId = matchingKu?.id ?: "ku_wordfamily_manage"

        return SuggestedWordAnalysis(
            word = clean,
            definitionEs = "Término incorporado para automatizar su uso en contexto.",
            definitionEn = "Vocabulary item added to practice active production.",
            partOfSpeech = partOfSpeech,
            relatedKuId = relatedKuId,
            naturalContexts = defaultContexts,
            exampleSentence = "We use “$clean” regularly in professional communication."
        )
    }

    /**
     * Provides sentence-aware contextual translations when user taps any word in an exercise sentence.
     * Prioritizes the specific contextual meaning within the origin sentence.
     */
    fun lookupContextualWord(
        rawWord: String,
        sentence: String,
        kuId: String?,
        pack: ContentPack?
    ): ContextualWordLookup {
        val clean = rawWord.lowercase().trim().trim { !it.isLetterOrDigit() }
        val sentLower = sentence.lowercase()

        val matchingKu = pack?.knowledge_units?.firstOrNull { it.id == kuId }
            ?: pack?.knowledge_units?.firstOrNull { ku ->
                ku.name.contains(clean, ignoreCase = true) || ku.behavior.contains(clean, ignoreCase = true)
            }

        // Sentence-aware contextual disambiguations:
        val (translation, contextMeaning, pos, assignedKu) = when (clean) {
            "schedule" -> {
                if (sentLower.contains("a schedule") || sentLower.contains("tight schedule") || sentLower.contains("on schedule")) {
                    Tuple4("Horario / cronograma", "Plan organizado de tiempos y entregas.", "sustantivo", "ku_future_be_going_to_vs_will")
                } else {
                    Tuple4("Programar / agendar", "Acción de planificar una reunión o entrega.", "verbo", "ku_future_be_going_to_vs_will")
                }
            }
            "agree" -> Tuple4("Estar de acuerdo / concordar", "Funciona como verbo pleno en inglés. No uses 'to be' (I agree, no I am agree).", "verbo", "ku_agree_verb")
            "agrees" -> Tuple4("Está de acuerdo", "Forma en 3ª persona singular del presente simple (he/she agrees).", "verbo", "ku_tps_s")
            "depend", "depends" -> Tuple4("Depender de", "En inglés requiere obligatoriamente la preposición 'on' (nunca 'of').", "verbo", "ku_depend_on")
            "since" -> {
                if (sentLower.contains("yesterday") || sentLower.contains("last") || sentLower.contains("20") || sentLower.contains("monday")) {
                    Tuple4("Desde", "Indica el punto de inicio exacto de una acción continuada.", "preposición", "ku_since_for")
                } else {
                    Tuple4("Ya que / dado que", "Conector explicativo de causa.", "conector", "ku_connectors_despite_although")
                }
            }
            "for" -> {
                if (sentLower.contains("year") || sentLower.contains("month") || sentLower.contains("hour") || sentLower.contains("day") || sentLower.contains("time")) {
                    Tuple4("Durante / por", "Expresa duración de tiempo acumulado (for three years).", "preposición", "ku_since_for")
                } else {
                    Tuple4("Para / por", "Indica propósito o destinatario.", "preposición", "ku_prep_responsible_for")
                }
            }
            "going" -> Tuple4("Going to (ir a)", "Forma para planes o intenciones; requiere auxiliar 'be' (am/is/are going to).", "perífrasis verbal", "ku_future_be_going_to_vs_will")
            "did", "didn't" -> Tuple4("Auxiliar de pasado", "Marca el pasado simple; el verbo que lo acompaña debe ir en forma base.", "auxiliar", "ku_past_simple_aux_base")
            "does", "doesn't" -> Tuple4("Auxiliar de presente", "Auxiliar para he/she/it; el verbo principal permanece en infinitivo sin 'to'.", "auxiliar", "ku_tps_s")
            "discuss", "discussed" -> Tuple4("Debatir / tratar", "Verbo transitivo directo: pasa directo al objeto (no se usa 'discuss about').", "verbo", "ku_discuss_no_about")
            "avoid", "avoids" -> Tuple4("Evitar", "Verbo que rige gerundio (-ing) para la acción siguiente (avoid making errors).", "verbo", "ku_verb_pattern_gerund_infinitive")
            "manage", "manages" -> Tuple4("Gestionar / administrar", "Dirigir recursos o conseguir superar un obstáculo.", "verbo", "ku_wordfamily_manage")
            "responsible" -> Tuple4("Responsable de", "Adjetivo que exige la preposición 'for' (responsible for the launch).", "adjetivo", "ku_prep_responsible_for")
            "although" -> Tuple4("Aunque", "Conector de contraste seguido de cláusula completa (sujeto + verbo).", "conector", "ku_connectors_despite_although")
            "despite" -> Tuple4("A pesar de", "Conector de contraste seguido de sustantivo o forma -ing.", "conector", "ku_connectors_despite_although")
            "can", "could" -> Tuple4("Poder (modal)", "Verbo modal que se une directamente al infinitivo sin 'to'.", "modal", "ku_modals_can_could_bare")
            "should", "must" -> Tuple4("Deber (modal)", "Expresa recomendación u obligación; sin 'to' hacia el verbo principal.", "modal", "ku_modals_can_could_bare")
            "deal" -> {
                if (sentLower.contains("deal with")) {
                    Tuple4("Gestionar / tratar con", "Enfrentar o solucionar un asunto o relacionarse con alguien.", "verbo", "ku_wordfamily_manage")
                } else {
                    Tuple4("Trato / acuerdo", "Pacto o condición comercial alcanzada.", "sustantivo", "ku_modals_can_could_bare")
                }
            }
            "meeting" -> Tuple4("Reunión", "Encuentro formal de trabajo o seguimiento.", "sustantivo", "ku_future_be_going_to_vs_will")
            "budget" -> Tuple4("Presupuesto", "Estimación financiera asignada a un objetivo.", "sustantivo", "ku_wordfamily_manage")
            "deadline" -> Tuple4("Fecha límite", "Plazo máximo para completar una entrega.", "sustantivo", "ku_prep_responsible_for")
            "client", "clients" -> Tuple4("Cliente(s)", "Persona o empresa que recibe el servicio o producto.", "sustantivo", "ku_prep_responsible_for")
            "project", "projects" -> Tuple4("Proyecto", "Iniciativa o plan de trabajo con objetivos definidos.", "sustantivo", "ku_future_be_going_to_vs_will")
            "team" -> Tuple4("Equipo", "Grupo de colaboradores que trabajan hacia una meta.", "sustantivo", "ku_prep_responsible_for")
            "report" -> Tuple4("Informe / reporte", "Documento con datos o conclusiones.", "sustantivo", "ku_past_simple_aux_base")
            "proposal" -> Tuple4("Propuesta", "Oferta o plan presentado para aprobación.", "sustantivo", "ku_discuss_no_about")
            "presentation" -> Tuple4("Presentación", "Exposición formal ante una audiencia.", "sustantivo", "ku_modals_can_could_bare")
            "contract" -> Tuple4("Contrato", "Acuerdo legal entre dos o más partes.", "sustantivo", "ku_modals_can_could_bare")
            "negotiate" -> Tuple4("Negociar", "Buscar un acuerdo mediante el diálogo sobre condiciones.", "verbo", "ku_modals_can_could_bare")
            "review" -> Tuple4("Revisar / revisión", "Examinar en detalle para corregir o validar.", "verbo/sustantivo", "ku_verb_pattern_gerund_infinitive")
            "feedback" -> Tuple4("Retroalimentación", "Comentarios constructivos sobre desempeño o producto.", "sustantivo", "ku_agree_verb")
            "timeline" -> Tuple4("Cronograma", "Secuencia de tiempos y fechas para el proyecto.", "sustantivo", "ku_future_be_going_to_vs_will")
            "stakeholders" -> Tuple4("Partes interesadas", "Personas con interés legítimo en el resultado del proyecto.", "sustantivo", "ku_prep_responsible_for")
            "it" -> Tuple4("Pronombre neutro 'It'", "En inglés es obligatorio como sujeto sintáctico de oraciones impersonales.", "pronombre", "ku_subject_explicit_dummy_it")
            else -> {
                // Heuristic fallback
                val def = when {
                    clean.endsWith("ly") -> Pair("De forma adverbial", "adverbio")
                    clean.endsWith("tion") || clean.endsWith("ment") -> Pair("Término conceptual / acción", "sustantivo")
                    clean.endsWith("able") || clean.endsWith("ive") -> Pair("Cualidad descriptiva", "adjetivo")
                    clean.endsWith("ed") -> Pair("Forma de pasado / participio", "verbo")
                    clean.endsWith("ing") -> Pair("Forma continua / gerundio", "verbo")
                    else -> Pair("Término en contexto", "palabra clave")
                }
                Tuple4(clean.replaceFirstChar { it.uppercase() }, "Significado en contexto dentro de esta frase.", def.second, matchingKu?.id)
            }
        }

        val kuEffective = assignedKu ?: matchingKu?.id ?: kuId
        val kuName = pack?.knowledge_units?.firstOrNull { it.id == kuEffective }?.name ?: kuEffective

        return ContextualWordLookup(
            word = clean,
            translationEs = translation,
            contextualMeaningEs = contextMeaning,
            partOfSpeech = pos,
            relatedKuId = kuEffective,
            relatedKuName = kuName,
            originSentence = sentence
        )
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}

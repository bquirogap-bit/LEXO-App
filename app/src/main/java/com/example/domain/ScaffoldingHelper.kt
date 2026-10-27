package com.example.domain

object ScaffoldingHelper {

    /**
     * Generates pedagogical scaffolding bullets for the given KU and exercise.
     * Rule: Must NOT give away the direct answer.
     * Provides rule retrieval cues, attentional focus, and Spanish transfer warnings.
     */
    fun getClues(ku: KnowledgeUnit?, exercise: Exercise?, lang: String = "es"): List<String> {
        val kuId = ku?.id ?: exercise?.ku ?: ""
        
        return when {
            kuId.contains("tps_s") || kuId.contains("third_person") -> {
                if (lang == "es") listOf(
                    "Con he / she / it en presente simple, el verbo cambia su terminación.",
                    "Observa el sujeto inmediatamente anterior al verbo principal.",
                    "Comprueba si la forma base requiere añadir -s o -es."
                ) else listOf(
                    "With he / she / it in present simple, the verb ending usually changes.",
                    "Look closely at the subject performing the action.",
                    "Check whether the base verb requires -s or -es."
                )
            }
            kuId.contains("agree") -> {
                if (lang == "es") listOf(
                    "«Agree» funciona como verbo pleno en inglés, no como adjetivo.",
                    "Evita el calco del español «estar de acuerdo» (no uses el verbo to be).",
                    "Estructura directa: sujeto + agree / agrees."
                ) else listOf(
                    "“Agree” is a full verb in English, not an adjective.",
                    "Avoid the literal translation from Spanish (do not insert 'to be').",
                    "Direct structure: subject + agree / agrees."
                )
            }
            kuId.contains("depend") -> {
                if (lang == "es") listOf(
                    "El verbo «depend» se acompaña de una preposición fija en inglés.",
                    "En español se dice «depender de», pero en inglés nunca se usa «of».",
                    "Utiliza la preposición correcta que denota apoyo/apoyo sobre algo: «on»."
                ) else listOf(
                    "The verb “depend” takes a strict fixed preposition in English.",
                    "Never use “of” after depend (a frequent Spanish transfer mistake).",
                    "Use the fixed preposition denoting reliance: “on”."
                )
            }
            kuId.contains("since_for") || kuId.contains("for_vs_since") -> {
                if (lang == "es") listOf(
                    "Distingue entre duración acumulada y punto de inicio exacto.",
                    "«for» se utiliza para lapsos de tiempo (horas, meses, años).",
                    "«since» se utiliza exclusivamente con el momento puntual en que comenzó (2020, yesterday, March)."
                ) else listOf(
                    "Distinguish between total duration and the starting reference point.",
                    "Use “for” to indicate length of time (hours, months, years).",
                    "Use “since” only for the exact starting moment (2020, yesterday, March)."
                )
            }
            kuId.contains("future_be_going_to") || kuId.contains("going_to") -> {
                if (lang == "es") listOf(
                    "La estructura para planes e intenciones requiere la forma conjugada de «be».",
                    "Fórmula obligatoria: Sujeto + am/is/are + going to + verbo base.",
                    "Revisa no omitir el verbo «to be» antes de «going to»."
                ) else listOf(
                    "Plans and intentions require the conjugated form of “be”.",
                    "Mandatory structure: Subject + am/is/are + going to + base verb.",
                    "Make sure not to omit “to be” before “going to”."
                )
            }
            kuId.contains("past_simple_aux") || kuId.contains("did") -> {
                if (lang == "es") listOf(
                    "En preguntas y negaciones en pasado simple, el auxiliar «did» ya marca el tiempo.",
                    "El verbo principal que acompaña a «did / didn't» debe quedar en su forma base infinitiva.",
                    "Evita la doble marcación de pasado (ejemplo: didn't + went es incorrecto)."
                ) else listOf(
                    "In past simple negatives and questions, the auxiliary “did” already carries the past marker.",
                    "The main verb following “did / didn't” must remain in its base form.",
                    "Avoid double past marking (e.g. didn't + went is incorrect)."
                )
            }
            kuId.contains("subject_explicit") || kuId.contains("dummy_it") -> {
                if (lang == "es") listOf(
                    "En inglés no existen oraciones impersonales sin sujeto sintáctico.",
                    "Cuando en español decimos «es importante» o «hace frío», el inglés exige un sujeto neutro.",
                    "Introduce el pronombre «it» antes del verbo to be."
                ) else listOf(
                    "English strictly requires an explicit syntactic subject in clauses.",
                    "Spanish often omits impersonal subjects, but English requires a dummy subject.",
                    "Check if “it” is needed before the verb."
                )
            }
            kuId.contains("present_perf_vs_past") -> {
                if (lang == "es") listOf(
                    "Fíjate si el enunciado incluye un marcador de tiempo cerrado o terminado.",
                    "Palabras como «yesterday, last year, ago, in 2021» exigen Pasado Simple estricto.",
                    "El Present Perfect solo se usa cuando el periodo sigue abierto o no se especifica fecha."
                ) else listOf(
                    "Look for finished time markers in the prompt.",
                    "Time words like “yesterday, last year, ago” require strict Past Simple.",
                    "Present Perfect is reserved for unfinished time or unspecified experiences."
                )
            }
            kuId.contains("modal") -> {
                if (lang == "es") listOf(
                    "Los modales básicos (can, could, should, must) se conectan directamente con el verbo base.",
                    "Nunca intercales la partícula «to» entre el modal y el verbo de acción.",
                    "Forma: Modal + verbo en infinitivo sin «to»."
                ) else listOf(
                    "Core modal verbs connect directly to the base verb.",
                    "Never insert “to” between the modal and the action verb.",
                    "Structure: Modal + bare infinitive."
                )
            }
            kuId.contains("verb_pattern") || kuId.contains("gerund") -> {
                if (lang == "es") listOf(
                    "El verbo principal determina si el siguiente verbo va en gerundio (-ing) o infinitivo (to + verbo).",
                    "Verbos como avoid, enjoy, consider requieren -ing.",
                    "Verbos como decide, plan, manage requieren to + infinitivo."
                ) else listOf(
                    "The primary verb governs whether the following verb takes -ing or an infinitive.",
                    "Verbs like avoid, enjoy, consider take -ing.",
                    "Verbs like decide, plan, manage take to + infinitive."
                )
            }
            kuId.contains("discuss") -> {
                if (lang == "es") listOf(
                    "«Discuss» es un verbo transitivo directo en inglés.",
                    "No se utiliza «discuss about» (calco de 'discutir sobre').",
                    "Pasa directamente al objeto: discuss the topic / discuss the project."
                ) else listOf(
                    "“Discuss” is a direct transitive verb in English.",
                    "Do not use “discuss about” (a common transfer error).",
                    "Connect directly to the object: discuss the matter."
                )
            }
            kuId.contains("connectors") || kuId.contains("despite") -> {
                if (lang == "es") listOf(
                    "Distingue si el conector va seguido de una cláusula completa (sujeto + verbo) o de un sustantivo.",
                    "«Although / even though» se usan con cláusula completa.",
                    "«Despite / in spite of» se usan con sustantivo o forma en -ing."
                ) else listOf(
                    "Check whether the connector precedes a full clause (subject + verb) or a noun phrase.",
                    "“Although / even though” take a full clause.",
                    "“Despite / in spite of” take a noun phrase or -ing form."
                )
            }
            else -> {
                val expl = if (lang == "es") ku?.explanation?.get("es") else ku?.explanation?.get("en")
                if (expl != null && expl.isNotBlank()) {
                    listOf(
                        expl,
                        if (lang == "es") "Verifica la concordancia y el patrón sintáctico." else "Check agreement and syntactic pattern.",
                        if (lang == "es") "Evita traducir palabra por palabra desde tu idioma materno." else "Avoid word-for-word translation from your native language."
                    )
                } else {
                    if (lang == "es") listOf(
                        "Revisa la estructura sintáctica del enunciado.",
                        "Identifica el sujeto y el tiempo verbal adecuado.",
                        "Ten presente las preposiciones o terminaciones requeridas por este patrón."
                    ) else listOf(
                        "Review the syntactic structure of the sentence.",
                        "Identify the subject and appropriate verb tense.",
                        "Pay attention to prepositions or required suffixes."
                    )
                }
            }
        }
    }
}

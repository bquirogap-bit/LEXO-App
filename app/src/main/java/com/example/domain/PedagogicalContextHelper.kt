package com.example.domain

object PedagogicalContextHelper {

    data class PedagogicalContext(
        val kuId: String,
        val kuTitle: String,
        val expectedActionLabel: String,
        val expectedActionDetail: String,
        val targetGrammarStructure: String,
        val expectedProduction: String,
        val frequentTrapsToAvoid: String,
        val differenceToAutomate: String,
        val illustrativeExample: String
    )

    /**
     * Determines the clear expected action label and detail based on exercise type, format, and modality.
     */
    fun getActionDescriptor(exercise: Exercise, format: String = "in_context", modality: String = "typed"): Pair<String, String> {
        return when {
            modality == "spoken" || format == "speaking" -> {
                "Responder oralmente" to "Habla en inglés usando el control de micrófono integrado, cuidando la estructura y concordancia."
            }
            format == "word_tiles" -> {
                "Ordenar palabras (Word Tiles)" to "Toca las fichas en el orden sintáctico correcto para construir la frase, descartando distractores."
            }
            format == "sentence_scramble" -> {
                "Reorganizar la oración" to "Reordena los fragmentos para recomponer la oración gramatical correcta conservando puntuación y contracciones."
            }
            exercise.type == "translation" || format == "translation" -> {
                "Traducir al inglés" to "Expresa la idea propuesta usando la estructura gramatical del patrón objetivo sin traducir palabra por palabra."
            }
            exercise.type == "fill_in_blank" -> {
                "Completar el espacio" to "Introduce la forma morfosintáctica exacta requerida por el contexto sintáctico."
            }
            exercise.type == "reformulation" -> {
                "Reformular una frase" to "Reescribe la oración aplicando el patrón objetivo manteniendo el significado original."
            }
            exercise.type == "error_correction" || format == "error_correction" -> {
                "Identificar y corregir error" to "Detecta el calco o incorrección en la frase propuesta y produce la forma gramatical válida."
            }
            exercise.type == "sentence_completion" || format == "open_production" -> {
                "Formular una oración completa" to "Completa o formula una frase coherente con el contexto laboral integrando la estructura meta."
            }
            else -> {
                "Producir en inglés" to "Escribe tu respuesta demostrando dominio del patrón gramatical en este contexto."
            }
        }
    }

    /**
     * Generates a pedagogical brief explaining what to understand before starting or during practice.
     * Does NOT reveal the direct solution of the exercise.
     */
    fun getContext(
        ku: KnowledgeUnit?,
        exercise: Exercise,
        format: String = "in_context",
        modality: String = "typed"
    ): PedagogicalContext {
        val kuId = ku?.id ?: exercise.ku
        val kuTitle = ku?.name ?: kuId
        val (actionLabel, actionDetail) = getActionDescriptor(exercise, format, modality)

        return when {
            kuId.contains("tps_s") || kuId.contains("third_person") -> {
                PedagogicalContext(
                    kuId = kuId,
                    kuTitle = kuTitle,
                    expectedActionLabel = actionLabel,
                    expectedActionDetail = actionDetail,
                    targetGrammarStructure = "Sujeto singular (he / she / it / cargo singular) + Verbo terminado en -s o -es en Presente Simple.",
                    expectedProduction = "Añadir de forma obligatoria la desinencia de 3ª persona singular cuando el sujeto que realiza la acción sea he, she, it o un sustantivo singular (ej. director, colleague, system).",
                    frequentTrapsToAvoid = "Omitir la -s final ('she review', 'the manager manage') por influencia del español, donde la conjugación cambia de otra manera y no se asocia la -s a una tercera persona singular.",
                    differenceToAutomate = "En inglés conversacional y formal, la omisión de la -s es uno de los errores más notorios. Buscamos que la concordancia con el sujeto singular sea un reflejo instantáneo antes de continuar la frase.",
                    illustrativeExample = "Modelo ilustrativo: 'The lead engineer analyzes the report every morning.' (Evitar: 'The lead engineer analyze...')"
                )
            }
            kuId.contains("agree") -> {
                PedagogicalContext(
                    kuId = kuId,
                    kuTitle = kuTitle,
                    expectedActionLabel = actionLabel,
                    expectedActionDetail = actionDetail,
                    targetGrammarStructure = "Sujeto + agree / agrees (with alguien / on un tema / that cláusula)",
                    expectedProduction = "Utilizar 'agree' como un verbo pleno activo, conjugándolo directamente según el sujeto sin insertar ninguna forma del verbo 'to be'.",
                    frequentTrapsToAvoid = "Escribir o decir 'I am agree' o 'we are agree'. En español decimos 'estoy de acuerdo' (verbo estar + adjetivo), lo que empuja a traducirlo erróneamente con 'am/are'.",
                    differenceToAutomate = "Desconectar la traducción literal de 'estar de acuerdo'. 'Agree' en inglés funciona como 'pensar' o 'concordar': no lleva auxiliar en afirmaciones ('I agree', 'She agrees').",
                    illustrativeExample = "Modelo ilustrativo: 'I agree with your strategic decision.' (Evitar: 'I am agree with...')"
                )
            }
            kuId.contains("depend") -> {
                PedagogicalContext(
                    kuId = kuId,
                    kuTitle = kuTitle,
                    expectedActionLabel = actionLabel,
                    expectedActionDetail = actionDetail,
                    targetGrammarStructure = "Sujeto + depend / depends + ON (o upon) + objeto",
                    expectedProduction = "Acompañar siempre al verbo 'depend' con la preposición fija 'on' para indicar contingencia o dependencia.",
                    frequentTrapsToAvoid = "Usar la preposición 'of' ('depend of'). En español decimos 'depender de', lo que provoca una traducción refleja incorrecta de la preposición.",
                    differenceToAutomate = "Automatizar que en inglés 'depend' rige apoyo semántico sobre algo ('on'). La combinación 'depend on' debe salir como un único bloque léxico sin pensarlo.",
                    illustrativeExample = "Modelo ilustrativo: 'The launch timeline depends on the final audit.' (Evitar: 'depends of...')"
                )
            }
            kuId.contains("future_will_vs_going_to") || kuId.contains("going_to") || kuId.contains("future_be_going_to") -> {
                PedagogicalContext(
                    kuId = kuId,
                    kuTitle = kuTitle,
                    expectedActionLabel = actionLabel,
                    expectedActionDetail = actionDetail,
                    targetGrammarStructure = "Sujeto + am / is / are + going to + Verbo en forma base",
                    expectedProduction = "Formular planes, proyectos coordinados e intenciones previas con la perífrasis 'be going to', asegurando conjugar el auxiliar 'be' según el sujeto.",
                    frequentTrapsToAvoid = "1) Omitir el verbo 'be' ('We going to launch...'). 2) Recurrir a 'will' para planes o citas ya agendadas, perdiendo el matiz de plan premeditado.",
                    differenceToAutomate = "Distinguir de forma automática entre una decisión espontánea tomada en el acto ('I'll email him right now') y un plan de trabajo coordinado ('We are going to meet on Friday').",
                    illustrativeExample = "Modelo ilustrativo: 'Our team is going to implement the new feature next sprint.' (Evitar: 'Our team going to implement...')"
                )
            }
            kuId.contains("past_aux_did_base") || kuId.contains("past_simple_aux_base") -> {
                PedagogicalContext(
                    kuId = kuId,
                    kuTitle = kuTitle,
                    expectedActionLabel = actionLabel,
                    expectedActionDetail = actionDetail,
                    targetGrammarStructure = "Sujeto + didn't + Verbo en forma BASE || Did + Sujeto + Verbo en forma BASE...?",
                    expectedProduction = "Mantener el verbo principal en su forma infinitiva sin to (forma base) cuando el pasado ya está indicado por el auxiliar negativo 'didn't' o interrogativo 'did'.",
                    frequentTrapsToAvoid = "Doble marcación de tiempo pasado: conjugar tanto el auxiliar como el verbo ('didn't went', 'didn't saw', 'didn't had').",
                    differenceToAutomate = "Entrenar la inhibición: en cuanto la mente formula 'didn't', el verbo principal debe regresar inmediatamente a su forma base sin excepción.",
                    illustrativeExample = "Modelo ilustrativo: 'We didn't receive the revised contract yesterday.' (Evitar: 'We didn't received...')"
                )
            }
            kuId.contains("since_for") || kuId.contains("for_vs_since") -> {
                PedagogicalContext(
                    kuId = kuId,
                    kuTitle = kuTitle,
                    expectedActionLabel = actionLabel,
                    expectedActionDetail = actionDetail,
                    targetGrammarStructure = "'for' + duración acumulada (meses, años, horas) || 'since' + fecha o punto de inicio (2021, Monday, 9 AM)",
                    expectedProduction = "Discriminar si la expresión temporal mide un período continuo o indica cuándo comenzó una acción que continúa en el presente.",
                    frequentTrapsToAvoid = "Traducir 'desde hace dos años' como 'since two years'. En inglés, la acumulación de tiempo exige obligatoriamente 'for two years'.",
                    differenceToAutomate = "Reconocer al instante la naturaleza del complemento temporal: ¿es un intervalo medible ('for') o un momento puntual en el calendario ('since')?",
                    illustrativeExample = "Modelo ilustrativo: 'They have managed this account for three years' frente a '...since 2021'."
                )
            }
            kuId.contains("discuss") -> {
                PedagogicalContext(
                    kuId = kuId,
                    kuTitle = kuTitle,
                    expectedActionLabel = actionLabel,
                    expectedActionDetail = actionDetail,
                    targetGrammarStructure = "Sujeto + discuss / discusses / discussed + Objeto directo (sin preposición)",
                    expectedProduction = "Usar 'discuss' como un verbo transitivo directo que conecta de inmediato con el asunto a tratar.",
                    frequentTrapsToAvoid = "Añadir 'about' después de discuss ('discuss about the budget') por calco de 'discutir sobre / hablar de'.",
                    differenceToAutomate = "Eliminar la necesidad de intercalar una preposición. En inglés profesional: 'discuss the issue', 'discuss the proposal'.",
                    illustrativeExample = "Modelo ilustrativo: 'We need to discuss the project requirements during the call.' (Evitar: 'discuss about...')"
                )
            }
            kuId.contains("responsible") -> {
                PedagogicalContext(
                    kuId = kuId,
                    kuTitle = kuTitle,
                    expectedActionLabel = actionLabel,
                    expectedActionDetail = actionDetail,
                    targetGrammarStructure = "Sujeto + be + responsible + FOR + sustantivo / gerundio (-ing)",
                    expectedProduction = "Acompañar al adjetivo 'responsible' con la preposición 'for' para delimitar la función o tarea asignada.",
                    frequentTrapsToAvoid = "Usar 'responsible of' o 'responsible about' por traducción directa del español 'responsable de'.",
                    differenceToAutomate = "Asociar 'responsible' a 'for' como una unidad indivisible en el vocabulario corporativo y de liderazgo.",
                    illustrativeExample = "Modelo ilustrativo: 'The product owner is responsible for prioritizing the backlog.' (Evitar: 'responsible of...')"
                )
            }
            kuId.contains("connectors") || kuId.contains("despite") || kuId.contains("although") -> {
                PedagogicalContext(
                    kuId = kuId,
                    kuTitle = kuTitle,
                    expectedActionLabel = actionLabel,
                    expectedActionDetail = actionDetail,
                    targetGrammarStructure = "'Despite / In spite of' + sustantivo / gerundio (-ing) || 'Although / Even though' + Sujeto + Verbo",
                    expectedProduction = "Seleccionar el conector de contraste adecuado según la estructura que le sigue en la cláusula concesiva.",
                    frequentTrapsToAvoid = "Decir 'despite that it was late' o 'despite he was'. Con 'despite' se usa sustantivo o gerundio; para oraciones con sujeto y verbo se usa 'although'.",
                    differenceToAutomate = "Detectar si lo que sigue es una entidad ('the delay') o una acción completa ('it was delayed') para seleccionar el conector exacto sin titubeos.",
                    illustrativeExample = "Modelo ilustrativo: 'Despite the technical challenges, we shipped on schedule.' vs 'Although we faced challenges, we shipped on schedule.'"
                )
            }
            kuId.contains("modal") || kuId.contains("can_could") -> {
                PedagogicalContext(
                    kuId = kuId,
                    kuTitle = kuTitle,
                    expectedActionLabel = actionLabel,
                    expectedActionDetail = actionDetail,
                    targetGrammarStructure = "Modal (can / could / should / must) + Verbo en forma base sin 'to'",
                    expectedProduction = "Seguir al verbo modal directamente con el infinitivo sin partícula 'to'.",
                    frequentTrapsToAvoid = "Insertar 'to' tras el modal ('we can to provide') o añadir desinencias de 3ª persona ('he cans').",
                    differenceToAutomate = "Tratar al modal como un operador auxiliar puro que nunca se conjuga ni requiere preposición hacia el verbo principal.",
                    illustrativeExample = "Modelo ilustrativo: 'We can optimize the query performance.' (Evitar: 'We can to optimize...')"
                )
            }
            else -> {
                val l1Note = ku?.l1_note?.get("es") ?: "Evita la traducción literal desde el español."
                val explanation = ku?.explanation?.get("es") ?: ku?.behavior ?: "Aplica la regla gramatical del patrón en este contexto."
                val behavior = ku?.behavior ?: "Usa la estructura adecuada según el contexto profesional."

                PedagogicalContext(
                    kuId = kuId,
                    kuTitle = kuTitle,
                    expectedActionLabel = actionLabel,
                    expectedActionDetail = actionDetail,
                    targetGrammarStructure = behavior,
                    expectedProduction = "Demostrar precisión morfosintáctica en el uso del patrón '$kuTitle' conforme al contexto solicitado.",
                    frequentTrapsToAvoid = l1Note,
                    differenceToAutomate = "Automatizar la asociación refleja entre la intención comunicativa y la estructura sintáctica sin mediación de la traducción mental.",
                    illustrativeExample = explanation
                )
            }
        }
    }
}

package com.example.domain

data class ValidationReport(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>,
    val totalKus: Int,
    val totalExercises: Int,
    val totalSignatures: Int
)

object ContentValidator {

    fun validate(pack: ContentPack): ValidationReport {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Unique IDs
        val kuIds = mutableSetOf<String>()
        for (ku in pack.knowledge_units) {
            if (!kuIds.add(ku.id)) {
                errors.add("Duplicate KU ID: ${ku.id}")
            }
        }

        val sigIds = mutableSetOf<String>()
        for (sig in pack.error_signatures) {
            if (!sigIds.add(sig.id)) {
                errors.add("Duplicate Signature ID: ${sig.id}")
            }
            if (sig.ku_id !in kuIds) {
                errors.add("Signature ${sig.id} references non-existent KU: ${sig.ku_id}")
            }
        }

        val exIds = mutableSetOf<String>()
        for (ex in pack.exercises) {
            if (!exIds.add(ex.id)) {
                errors.add("Duplicate Exercise ID: ${ex.id}")
            }
            if (ex.ku !in kuIds) {
                errors.add("Exercise ${ex.id} targets non-existent primary KU: ${ex.ku}")
            }
            for (secKu in ex.secondary_ku_ids) {
                if (secKu !in kuIds) {
                    errors.add("Exercise ${ex.id} targets non-existent secondary KU: $secKu")
                }
            }
            if (ex.secondary_ku_ids.size > 1) {
                errors.add("Exercise ${ex.id} targets more than 2 KUs in total")
            }

            // fill_in_blank prompts contain exactly one "___"
            if (ex.type == "fill_in_blank") {
                val blankCount = ex.prompt.split("___").size - 1
                if (blankCount != 1) {
                    errors.add("Exercise ${ex.id} fill_in_blank prompt must contain exactly one '___', found $blankCount")
                }
            }

            // deterministic_first exercises have non-empty accepted
            if (ex.eval_mode != "llm_only" && ex.accepted.isEmpty()) {
                errors.add("Exercise ${ex.id} has eval_mode=${ex.eval_mode} but empty accepted list")
            }

            // Self consistency: no accepted answer contains a known_wrong token sequence
            for (kw in ex.known_wrong) {
                for (acc in ex.accepted) {
                    if (Matching.containsTokenSequence(acc, kw.text)) {
                        errors.add("Exercise ${ex.id} accepted answer '$acc' contains known_wrong text '${kw.text}'")
                    }
                }
                if (kw.sig !in sigIds) {
                    errors.add("Exercise ${ex.id} references non-existent signature: ${kw.sig}")
                }
            }
        }

        // Each KU must have >= 6 exercises, >= 3 distinct contexts, >= 2 exercise types, >= 2 constructed
        for (ku in pack.knowledge_units) {
            val kuExercises = pack.exercises.filter { it.ku == ku.id || it.secondary_ku_ids.contains(ku.id) }
            if (kuExercises.size < 6) {
                errors.add("KU ${ku.id} has only ${kuExercises.size} exercises (minimum 6 required)")
            }

            val distinctContexts = kuExercises.map { it.context }.distinct()
            if (distinctContexts.size < 3) {
                errors.add("KU ${ku.id} has only ${distinctContexts.size} distinct contexts (minimum 3 required)")
            }

            val distinctTypes = kuExercises.map { it.type }.distinct()
            if (distinctTypes.size < 2) {
                errors.add("KU ${ku.id} has only ${distinctTypes.size} exercise types (minimum 2 required)")
            }

            val constructedCount = kuExercises.count { it.elicitation == "constructed" }
            if (constructedCount < 2) {
                errors.add("KU ${ku.id} has only $constructedCount constructed exercises (minimum 2 required)")
            }
        }

        return ValidationReport(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            totalKus = pack.knowledge_units.size,
            totalExercises = pack.exercises.size,
            totalSignatures = pack.error_signatures.size
        )
    }
}

package com.example.service

import com.example.domain.Exercise
import com.example.domain.KnowledgeUnit
import com.example.domain.RawEvaluation

interface EvaluatorPort {
    suspend fun evaluate(
        exercise: Exercise,
        targets: List<KnowledgeUnit>,
        rawResponse: String
    ): RawEvaluation

    suspend fun verifyError(
        ku: KnowledgeUnit,
        rawResponse: String,
        quote: String
    ): Boolean
}

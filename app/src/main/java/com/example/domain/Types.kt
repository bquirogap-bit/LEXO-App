package com.example.domain

enum class KuStatus(val plainNameEs: String, val plainNameEn: String) {
    UNTESTED("no explorado todavía", "not explored yet"),
    OBSERVED("visto una vez", "seen once"),
    SLIP("bajo observación", "worth watching"),
    RECURRENT("recurrente", "recurring"),
    RECOVERY("en recuperación", "recovering"),
    CONSOLIDATED("estable", "stable"),
    UNSTABLE("necesita revisión", "needs a re-check")
}

enum class ReasonCode(val textEs: String, val textEn: String) {
    RECURRING_PATTERN(
        "Este patrón apareció antes. Lo practicamos para estabilizarlo.",
        "This pattern appeared before. Practicing to stabilize it."
    ),
    UNSTABLE_AFTER_CONSOLIDATION(
        "Apareció una dificultad reciente en este patrón establecido.",
        "Recent difficulty appeared in this established pattern."
    ),
    IN_RECOVERY(
        "Mostrando progreso. Continuando práctica para consolidar.",
        "Showing recovery. Continuing practice to consolidate."
    ),
    RECENT_SLIP(
        "Observamos un desliz reciente. Verificando si fue casual.",
        "Observed a recent slip. Checking if it was incidental."
    ),
    REVIEW_DUE(
        "Momento programado para comprobar retención en el tiempo.",
        "Scheduled check to verify retention over time."
    ),
    TRANSFER_CHECK(
        "Verificando el uso correcto en un contexto diferente.",
        "Verifying correct use in a different context."
    ),
    NEW_AREA(
        "Explorando una nueva área para mapear tus patrones.",
        "Exploring a new area to map your patterns."
    )
}

data class KnowledgeUnit(
    val id: String,
    val name: String,
    val kind: String,
    val area: String,
    val band_introduced: String,
    val behavior: String,
    val obligatory_context: String,
    val success_criterion: String,
    val out_of_scope: List<String> = emptyList(),
    val prerequisites: List<String> = emptyList(),
    val l1_note: Map<String, String> = emptyMap(),
    val explanation: Map<String, String> = emptyMap()
)

data class ErrorSignature(
    val id: String,
    val ku_id: String,
    val name: String,
    val feedback_es: String,
    val feedback_en: String
)

data class KnownWrong(
    val text: String,
    val sig: String
)

data class KnownTypo(
    val typo: String,
    val fix: String
)

data class Exercise(
    val id: String,
    val ku: String,
    val secondary_ku_ids: List<String> = emptyList(),
    val type: String, // fill_in_blank | sentence_completion | translation | reformulation
    val elicitation: String, // constrained | constructed
    val context: String, // Business | Meetings | Travel | Technology | Negotiation | Leadership | ProjectManagement | Presentations
    val difficulty: Int = 1,
    val facet: String? = null,
    val situation: String,
    val prompt: String,
    val prompt_lang: String? = "en",
    val eval_mode: String? = "deterministic_first",
    val accepted: List<String> = emptyList(),
    val known_wrong: List<KnownWrong> = emptyList(),
    val known_typos: List<KnownTypo> = emptyList(),
    val notes_for_evaluator: String? = null
)

data class ContentPack(
    val pack_id: String,
    val version: String,
    val knowledge_units: List<KnowledgeUnit>,
    val error_signatures: List<ErrorSignature>,
    val exercises: List<Exercise>
)

data class EvidenceEvent(
    val id: String,
    val attemptId: String,
    val kuId: String,
    val role: String, // "primary" | "secondary"
    val opportunity: String, // "present" | "absent" | "unclear"
    val outcome: String, // "correct" | "error" | "partial" | "avoided" | "unclear"
    val errorKind: String?, // "target_violation" | "typo" | "other_error" | null
    val signatureId: String?,
    val quote: String?,
    val confidence: Double,
    val weight: Double,
    val counts: Boolean,
    val source: String, // "deterministic" | "llm" | "llm_verified"
    val context: String,
    val exerciseId: String,
    val facet: String?,
    val sessionId: String,
    val elicitation: String,
    val modality: String = "typed",
    val latencyMs: Long,
    val hintsUsed: Int,
    val at: Long,
    val status: String = "active", // "active" | "disputed"
    val configVersion: String
)

data class KuState(
    val userId: String = "default_user",
    val kuId: String,
    val status: KuStatus,
    val statusSince: Long,
    val opportunities: Int,
    val validErrors: Int,
    val successRun: Int,
    val recurrenceCount: Int,
    val contextsSucceeded: List<String>,
    val daysSucceeded: List<String>,
    val lastErrorAt: Long?,
    val lastSuccessAt: Long?,
    val everConsolidated: Boolean,
    val stepIndex: Int,
    val dueAt: Long?,
    val retryPending: Boolean,
    val alpha: Double,
    val beta: Double,
    val pHat: Double,
    val nEff: Double,
    val configVersion: String,
    val derivedFromEventCount: Int
)

data class RawKuEvaluation(
    val ku_id: String,
    val opportunity: String, // present | absent | unclear
    val outcome: String, // correct | error | partial | avoided | unclear
    val error_kind: String? = null, // target_violation | typo | other_error | null
    val error_signature_id: String? = null,
    val evidence_quote: String? = null,
    val suggested_correction: String? = null,
    val confidence: Double = 0.0,
    val uncertainty_reason: String? = null
)

data class OtherObservation(
    val description: String,
    val quote: String
)

data class RawEvaluation(
    val schema_version: String = "eval.v1",
    val per_ku: List<RawKuEvaluation> = emptyList(),
    val alternative_answer_valid: Boolean? = null,
    val self_corrected: Boolean? = false,
    val other_observations: List<OtherObservation> = emptyList()
)

data class GuardrailLogEntry(
    val rule: String,
    val detail: String
)

data class Feedback(
    val tone: String = "neutral",
    val message: String,
    val quote: String? = null,
    val correction: String? = null,
    val orthographicNote: String? = null,
    val patternNote: String? = null,
    val isRecurrenceAlert: Boolean = false
)

data class KuChange(
    val kuId: String,
    val from: KuStatus,
    val to: KuStatus
)

data class PlannedItem(
    val itemId: String,
    val exerciseId: String,
    val kuId: String,
    val reasonCode: ReasonCode,
    val reasonTextEs: String,
    val reasonTextEn: String,
    val modality: String = "writing", // "writing" | "speaking"
    val exerciseFormat: String = "standard", // "writing" | "speaking" | "translation" | "open_production" | "error_correction" | "pattern_retrieval" | "in_context"
    val associatedWord: String? = null
)

data class PlannedSession(
    val sessionId: String,
    val items: List<PlannedItem>,
    val rationale: String,
    val isFocusedPractice: Boolean = false,
    val focusedKuId: String? = null
)

data class VocabularyWord(
    val id: String,
    val word: String,
    val definitionEs: String,
    val definitionEn: String,
    val partOfSpeech: String, // "verb" | "noun" | "adjective" | "connector" | "adverb"
    val relatedKuId: String?,
    val naturalContexts: List<String>,
    val exampleSentence: String,
    val stepIndex: Int = 0,
    val dueAt: Long? = null,
    val reviewCount: Int = 0,
    val lastReviewedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class ImprovementIssue(
    val id: String,
    val title: String,
    val kuId: String,
    val kuName: String,
    val signatureId: String?,
    val status: KuStatus,
    val frequency: Int,
    val recentQuotes: List<String>,
    val priorityScore: Double,
    val dueAt: Long?,
    val recommendedAction: String,
    val explanation: String
)

data class MasteryMilestone(
    val id: String,
    val title: String,
    val description: String,
    val category: String, // "consolidation" | "recovery" | "transfer" | "modality" | "vocabulary" | "independence"
    val isUnlocked: Boolean,
    val currentProgress: Int,
    val targetProgress: Int,
    val achievedAt: Long? = null,
    val evidenceDetail: String
)

data class AttemptRecord(
    val attemptId: String,
    val sessionId: String,
    val exerciseId: String,
    val response: String,
    val submittedAt: Long,
    val latencyMs: Long,
    val hintsUsed: Int,
    val kind: String, // "scored" | "correction_rewrite"
    val evaluationStatus: String, // "scored" | "unscored"
    val source: String,
    val guardrailLog: List<GuardrailLogEntry>,
    val feedback: Feedback?,
    val kuChanges: List<KuChange>
)

data class GoldenCase(
    val id: String,
    val exercise_id: String,
    val ku_id: String,
    val response: String,
    val expected_outcome: String,
    val expected_opportunity: String,
    val expected_signature: String? = null,
    val expected_counts: Boolean = true,
    val notes: String? = null,
    val needs_human_review: Boolean = true
)

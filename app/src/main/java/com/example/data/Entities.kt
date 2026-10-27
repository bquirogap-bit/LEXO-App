package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attempts")
data class AttemptEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val exerciseId: String,
    val response: String,
    val submittedAt: Long,
    val latencyMs: Long,
    val hintsUsed: Int,
    val kind: String, // "scored" | "correction_rewrite"
    val evaluationStatus: String, // "scored" | "unscored"
    val source: String,
    val guardrailLogJson: String,
    val feedbackJson: String?,
    val kuChangesJson: String
)

@Entity(tableName = "evidence")
data class EvidenceEntity(
    @PrimaryKey val id: String,
    val attemptId: String,
    val kuId: String,
    val role: String,
    val opportunity: String,
    val outcome: String,
    val errorKind: String?,
    val signatureId: String?,
    val quote: String?,
    val confidence: Double,
    val weight: Double,
    val counts: Boolean,
    val source: String,
    val context: String,
    val exerciseId: String,
    val facet: String?,
    val sessionId: String,
    val elicitation: String,
    val modality: String,
    val latencyMs: Long,
    val hintsUsed: Int,
    val at: Long,
    val status: String, // "active" | "disputed"
    val configVersion: String
)

@Entity(tableName = "ku_states")
data class KuStateEntity(
    @PrimaryKey val kuId: String,
    val userId: String,
    val status: String,
    val statusSince: Long,
    val opportunities: Int,
    val validErrors: Int,
    val successRun: Int,
    val recurrenceCount: Int,
    val contextsSucceededJson: String,
    val daysSucceededJson: String,
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

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val sessionId: String,
    val createdAt: Long,
    val queueJson: String,
    val currentIndex: Int,
    val rationale: String,
    val isFinished: Boolean
)

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val userId: String = "default_user",
    val feedbackLang: String = "es",
    val testMode: Boolean = false,
    val activeSessionId: String? = null
)

@Entity(tableName = "vocabulary_words")
data class WordEntity(
    @PrimaryKey val id: String,
    val word: String,
    val definitionEs: String,
    val definitionEn: String,
    val partOfSpeech: String,
    val relatedKuId: String?,
    val naturalContextsJson: String,
    val exampleSentence: String,
    val stepIndex: Int,
    val dueAt: Long?,
    val reviewCount: Int,
    val lastReviewedAt: Long?,
    val createdAt: Long
)

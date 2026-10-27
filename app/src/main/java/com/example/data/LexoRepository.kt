package com.example.data

import com.example.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class LexoRepository(
    private val db: LexoDatabase,
    private val config: LearningConfig = LearningConfig()
) {
    private val attemptDao = db.attemptDao()
    private val evidenceDao = db.evidenceDao()
    private val kuStateDao = db.kuStateDao()
    private val sessionDao = db.sessionDao()
    private val prefDao = db.userPreferencesDao()
    private val wordDao = db.wordDao()

    val userPreferencesFlow: Flow<UserPreferencesEntity?> = prefDao.getPreferencesFlow()
    val allAttemptsFlow: Flow<List<AttemptRecord>> = attemptDao.getAllAttemptsFlow().map { list ->
        list.map { it.toDomain() }
    }
    val allKuStatesFlow: Flow<List<KuState>> = kuStateDao.getAllKuStatesFlow().map { list ->
        list.map { it.toDomain() }
    }
    val vocabularyWordsFlow: Flow<List<VocabularyWord>> = wordDao.getAllWordsFlow().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getPreferences(): UserPreferencesEntity {
        return prefDao.getPreferences() ?: UserPreferencesEntity().also {
            prefDao.insertPreferences(it)
        }
    }

    suspend fun updateFeedbackLang(lang: String) {
        val pref = getPreferences()
        prefDao.insertPreferences(pref.copy(feedbackLang = lang))
    }

    suspend fun updateTestMode(enabled: Boolean) {
        val pref = getPreferences()
        prefDao.insertPreferences(pref.copy(testMode = enabled))
    }

    suspend fun getAllEvidence(): List<EvidenceEvent> {
        return evidenceDao.getAllEvidence().map { it.toDomain() }
    }

    suspend fun getEvidenceForKu(kuId: String): List<EvidenceEvent> {
        return evidenceDao.getEvidenceForKu(kuId).map { it.toDomain() }
    }

    fun getEvidenceForKuFlow(kuId: String): Flow<List<EvidenceEvent>> {
        return evidenceDao.getEvidenceForKuFlow(kuId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getAttempt(id: String): AttemptRecord? {
        return attemptDao.getAttemptById(id)?.toDomain()
    }

    suspend fun saveAttempt(record: AttemptRecord) {
        attemptDao.insertAttempt(record.toEntity())
    }

    suspend fun saveEvidenceEvents(events: List<EvidenceEvent>) {
        evidenceDao.insertAll(events.map { it.toEntity() })
    }

    suspend fun getAllKuStates(pack: ContentPack): Map<String, KuState> {
        val statesFromDb = kuStateDao.getAllKuStates().map { it.toDomain() }.associateBy { it.kuId }
        val now = System.currentTimeMillis()
        val allEvents = getAllEvidence()

        // Ensure all KUs from pack exist in map
        return pack.knowledge_units.associate { ku ->
            val existing = statesFromDb[ku.id]
            if (existing != null) {
                ku.id to existing
            } else {
                val kuEvents = allEvents.filter { it.kuId == ku.id }
                val initial = Reducer.reduce(ku.id, kuEvents, config, now)
                ku.id to initial
            }
        }
    }

    suspend fun getKuState(kuId: String): KuState? {
        return kuStateDao.getKuState(kuId)?.toDomain()
    }

    suspend fun saveKuState(state: KuState) {
        kuStateDao.insertKuState(state.toEntity())
    }

    suspend fun recomputeKuState(kuId: String, now: Long = System.currentTimeMillis()): KuState {
        val events = evidenceDao.getEvidenceForKu(kuId).map { it.toDomain() }
        val state = Reducer.reduce(kuId, events, config, now)
        saveKuState(state)
        return state
    }

    suspend fun disputeEvidence(evidenceId: String, now: Long = System.currentTimeMillis()): KuState? {
        val ev = evidenceDao.getEvidenceById(evidenceId) ?: return null
        evidenceDao.updateStatus(evidenceId, "disputed")
        return recomputeKuState(ev.kuId, now)
    }

    suspend fun replayAllEvents(pack: ContentPack, now: Long = System.currentTimeMillis()): Map<String, Pair<KuState, KuState>> {
        val allEvents = getAllEvidence()
        val oldStates = getAllKuStates(pack)
        val diffMap = mutableMapOf<String, Pair<KuState, KuState>>()

        for (ku in pack.knowledge_units) {
            val kuEvents = allEvents.filter { it.kuId == ku.id }
            val recomputed = Reducer.reduce(ku.id, kuEvents, config, now)
            val old = oldStates[ku.id] ?: Reducer.reduce(ku.id, emptyList(), config, now)
            diffMap[ku.id] = Pair(old, recomputed)
            saveKuState(recomputed)
        }
        return diffMap
    }

    suspend fun resetDemo(demoEvents: List<EvidenceEvent>, pack: ContentPack, now: Long = System.currentTimeMillis()) {
        attemptDao.deleteAll()
        evidenceDao.deleteAll()
        kuStateDao.deleteAll()
        sessionDao.deleteAll()

        // Insert demo events
        saveEvidenceEvents(demoEvents)

        // Replay and populate derived KU states
        for (ku in pack.knowledge_units) {
            recomputeKuState(ku.id, now)
        }
    }

    suspend fun clearAllData() {
        attemptDao.deleteAll()
        evidenceDao.deleteAll()
        kuStateDao.deleteAll()
        sessionDao.deleteAll()
        prefDao.deleteAll()
        wordDao.deleteAll()
    }

    suspend fun getAllWords(): List<VocabularyWord> {
        val list = wordDao.getAllWords()
        if (list.isEmpty()) {
            val defaults = WordKnowledgeMatcher.getDefaultStarterWords()
            wordDao.insertAll(defaults.map { it.toEntity() })
            return defaults
        }
        return list.map { it.toDomain() }
    }

    suspend fun saveWord(word: VocabularyWord) {
        wordDao.insertWord(word.toEntity())
    }

    suspend fun deleteWord(wordId: String) {
        wordDao.deleteWord(wordId)
    }

    suspend fun advanceWordReview(wordId: String, success: Boolean, now: Long = System.currentTimeMillis()) {
        val word = wordDao.getWordById(wordId)?.toDomain() ?: return
        val newStep = if (success) word.stepIndex + 1 else 0
        val ladderDays = config.scheduler.ladderDays
        val daysToAdd = ladderDays.getOrElse(newStep) { ladderDays.last() }
        val nextDue = now + daysToAdd * 86400_000L

        val updated = word.copy(
            stepIndex = newStep,
            dueAt = nextDue,
            reviewCount = word.reviewCount + 1,
            lastReviewedAt = now
        )
        wordDao.insertWord(updated.toEntity())
    }

    suspend fun saveSession(session: PlannedSession, currentIndex: Int = 0, isFinished: Boolean = false) {
        val jsonArray = JSONArray()
        for (item in session.items) {
            val obj = JSONObject()
            obj.put("itemId", item.itemId)
            obj.put("exerciseId", item.exerciseId)
            obj.put("kuId", item.kuId)
            obj.put("reasonCode", item.reasonCode.name)
            obj.put("reasonTextEs", item.reasonTextEs)
            obj.put("reasonTextEn", item.reasonTextEn)
            obj.put("modality", item.modality)
            obj.put("exerciseFormat", item.exerciseFormat)
            if (item.associatedWord != null) obj.put("associatedWord", item.associatedWord)
            jsonArray.put(obj)
        }

        val entity = SessionEntity(
            sessionId = session.sessionId,
            createdAt = System.currentTimeMillis(),
            queueJson = jsonArray.toString(),
            currentIndex = currentIndex,
            rationale = session.rationale,
            isFinished = isFinished
        )
        sessionDao.insertSession(entity)
        val pref = getPreferences()
        prefDao.insertPreferences(pref.copy(activeSessionId = session.sessionId))
    }

    suspend fun getActiveSession(): Pair<PlannedSession, Int>? {
        val pref = getPreferences()
        val sid = pref.activeSessionId ?: return null
        val entity = sessionDao.getSession(sid) ?: return null
        if (entity.isFinished) return null

        val arr = JSONArray(entity.queueJson)
        val items = mutableListOf<PlannedItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            items.add(
                PlannedItem(
                    itemId = obj.getString("itemId"),
                    exerciseId = obj.getString("exerciseId"),
                    kuId = obj.getString("kuId"),
                    reasonCode = ReasonCode.valueOf(obj.getString("reasonCode")),
                    reasonTextEs = obj.getString("reasonTextEs"),
                    reasonTextEn = obj.getString("reasonTextEn"),
                    modality = obj.optString("modality", "writing"),
                    exerciseFormat = obj.optString("exerciseFormat", "standard"),
                    associatedWord = if (obj.has("associatedWord")) obj.getString("associatedWord") else null
                )
            )
        }

        return Pair(PlannedSession(entity.sessionId, items, entity.rationale), entity.currentIndex)
    }

    suspend fun finishSession(sessionId: String) {
        val entity = sessionDao.getSession(sessionId) ?: return
        sessionDao.insertSession(entity.copy(isFinished = true))
        val pref = getPreferences()
        if (pref.activeSessionId == sessionId) {
            prefDao.insertPreferences(pref.copy(activeSessionId = null))
        }
    }

    // Mapping functions
    private fun EvidenceEntity.toDomain() = EvidenceEvent(
        id = id,
        attemptId = attemptId,
        kuId = kuId,
        role = role,
        opportunity = opportunity,
        outcome = outcome,
        errorKind = errorKind,
        signatureId = signatureId,
        quote = quote,
        confidence = confidence,
        weight = weight,
        counts = counts,
        source = source,
        context = context,
        exerciseId = exerciseId,
        facet = facet,
        sessionId = sessionId,
        elicitation = elicitation,
        modality = modality,
        latencyMs = latencyMs,
        hintsUsed = hintsUsed,
        at = at,
        status = status,
        configVersion = configVersion
    )

    private fun EvidenceEvent.toEntity() = EvidenceEntity(
        id = id,
        attemptId = attemptId,
        kuId = kuId,
        role = role,
        opportunity = opportunity,
        outcome = outcome,
        errorKind = errorKind,
        signatureId = signatureId,
        quote = quote,
        confidence = confidence,
        weight = weight,
        counts = counts,
        source = source,
        context = context,
        exerciseId = exerciseId,
        facet = facet,
        sessionId = sessionId,
        elicitation = elicitation,
        modality = modality,
        latencyMs = latencyMs,
        hintsUsed = hintsUsed,
        at = at,
        status = status,
        configVersion = configVersion
    )

    private fun KuStateEntity.toDomain(): KuState {
        val ctxArr = JSONArray(contextsSucceededJson)
        val contexts = (0 until ctxArr.length()).map { ctxArr.getString(it) }
        val daysArr = JSONArray(daysSucceededJson)
        val days = (0 until daysArr.length()).map { daysArr.getString(it) }

        return KuState(
            userId = userId,
            kuId = kuId,
            status = KuStatus.valueOf(status),
            statusSince = statusSince,
            opportunities = opportunities,
            validErrors = validErrors,
            successRun = successRun,
            recurrenceCount = recurrenceCount,
            contextsSucceeded = contexts,
            daysSucceeded = days,
            lastErrorAt = lastErrorAt,
            lastSuccessAt = lastSuccessAt,
            everConsolidated = everConsolidated,
            stepIndex = stepIndex,
            dueAt = dueAt,
            retryPending = retryPending,
            alpha = alpha,
            beta = beta,
            pHat = pHat,
            nEff = nEff,
            configVersion = configVersion,
            derivedFromEventCount = derivedFromEventCount
        )
    }

    private fun KuState.toEntity(): KuStateEntity {
        val ctxArr = JSONArray()
        contextsSucceeded.forEach { ctxArr.put(it) }
        val daysArr = JSONArray()
        daysSucceeded.forEach { daysArr.put(it) }

        return KuStateEntity(
            kuId = kuId,
            userId = userId,
            status = status.name,
            statusSince = statusSince,
            opportunities = opportunities,
            validErrors = validErrors,
            successRun = successRun,
            recurrenceCount = recurrenceCount,
            contextsSucceededJson = ctxArr.toString(),
            daysSucceededJson = daysArr.toString(),
            lastErrorAt = lastErrorAt,
            lastSuccessAt = lastSuccessAt,
            everConsolidated = everConsolidated,
            stepIndex = stepIndex,
            dueAt = dueAt,
            retryPending = retryPending,
            alpha = alpha,
            beta = beta,
            pHat = pHat,
            nEff = nEff,
            configVersion = configVersion,
            derivedFromEventCount = derivedFromEventCount
        )
    }

    private fun AttemptRecord.toEntity(): AttemptEntity {
        val guardrailsArr = JSONArray()
        for (g in guardrailLog) {
            val obj = JSONObject()
            obj.put("rule", g.rule)
            obj.put("detail", g.detail)
            guardrailsArr.put(obj)
        }

        val feedbackObj = feedback?.let {
            JSONObject().apply {
                put("tone", it.tone)
                put("message", it.message)
                put("quote", it.quote)
                put("correction", it.correction)
                put("orthographicNote", it.orthographicNote)
                put("patternNote", it.patternNote)
                put("isRecurrenceAlert", it.isRecurrenceAlert)
            }.toString()
        }

        val kuChangesArr = JSONArray()
        for (c in kuChanges) {
            val obj = JSONObject()
            obj.put("kuId", c.kuId)
            obj.put("from", c.from.name)
            obj.put("to", c.to.name)
            kuChangesArr.put(obj)
        }

        return AttemptEntity(
            id = attemptId,
            sessionId = sessionId,
            exerciseId = exerciseId,
            response = response,
            submittedAt = submittedAt,
            latencyMs = latencyMs,
            hintsUsed = hintsUsed,
            kind = kind,
            evaluationStatus = evaluationStatus,
            source = source,
            guardrailLogJson = guardrailsArr.toString(),
            feedbackJson = feedbackObj,
            kuChangesJson = kuChangesArr.toString()
        )
    }

    private fun AttemptEntity.toDomain(): AttemptRecord {
        val gList = mutableListOf<GuardrailLogEntry>()
        val gArr = JSONArray(guardrailLogJson)
        for (i in 0 until gArr.length()) {
            val obj = gArr.getJSONObject(i)
            gList.add(GuardrailLogEntry(obj.getString("rule"), obj.getString("detail")))
        }

        val parsedFeedback = feedbackJson?.let {
            val obj = JSONObject(it)
            Feedback(
                tone = obj.optString("tone", "neutral"),
                message = obj.getString("message"),
                quote = if (obj.isNull("quote")) null else obj.getString("quote"),
                correction = if (obj.isNull("correction")) null else obj.getString("correction"),
                orthographicNote = if (obj.isNull("orthographicNote")) null else obj.getString("orthographicNote"),
                patternNote = if (obj.isNull("patternNote")) null else obj.getString("patternNote"),
                isRecurrenceAlert = obj.optBoolean("isRecurrenceAlert", false)
            )
        }

        val cList = mutableListOf<KuChange>()
        val cArr = JSONArray(kuChangesJson)
        for (i in 0 until cArr.length()) {
            val obj = cArr.getJSONObject(i)
            cList.add(
                KuChange(
                    kuId = obj.getString("kuId"),
                    from = KuStatus.valueOf(obj.getString("from")),
                    to = KuStatus.valueOf(obj.getString("to"))
                )
            )
        }

        return AttemptRecord(
            attemptId = id,
            sessionId = sessionId,
            exerciseId = exerciseId,
            response = response,
            submittedAt = submittedAt,
            latencyMs = latencyMs,
            hintsUsed = hintsUsed,
            kind = kind,
            evaluationStatus = evaluationStatus,
            source = source,
            guardrailLog = gList,
            feedback = parsedFeedback,
            kuChanges = cList
        )
    }

    private fun WordEntity.toDomain(): VocabularyWord {
        val arr = JSONArray(naturalContextsJson)
        val ctxList = (0 until arr.length()).map { arr.getString(it) }
        return VocabularyWord(
            id = id,
            word = word,
            definitionEs = definitionEs,
            definitionEn = definitionEn,
            partOfSpeech = partOfSpeech,
            relatedKuId = relatedKuId,
            naturalContexts = ctxList,
            exampleSentence = exampleSentence,
            stepIndex = stepIndex,
            dueAt = dueAt,
            reviewCount = reviewCount,
            lastReviewedAt = lastReviewedAt,
            createdAt = createdAt
        )
    }

    private fun VocabularyWord.toEntity(): WordEntity {
        val arr = JSONArray()
        naturalContexts.forEach { arr.put(it) }
        return WordEntity(
            id = id,
            word = word,
            definitionEs = definitionEs,
            definitionEn = definitionEn,
            partOfSpeech = partOfSpeech,
            relatedKuId = relatedKuId,
            naturalContextsJson = arr.toString(),
            exampleSentence = exampleSentence,
            stepIndex = stepIndex,
            dueAt = dueAt,
            reviewCount = reviewCount,
            lastReviewedAt = lastReviewedAt,
            createdAt = createdAt
        )
    }
}

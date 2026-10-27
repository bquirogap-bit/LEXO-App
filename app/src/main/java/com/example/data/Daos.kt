package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AttemptDao {
    @Query("SELECT * FROM attempts ORDER BY submittedAt DESC")
    fun getAllAttemptsFlow(): Flow<List<AttemptEntity>>

    @Query("SELECT * FROM attempts WHERE id = :id LIMIT 1")
    suspend fun getAttemptById(id: String): AttemptEntity?

    @Query("SELECT * FROM attempts ORDER BY submittedAt DESC")
    suspend fun getAllAttempts(): List<AttemptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: AttemptEntity)

    @Query("DELETE FROM attempts")
    suspend fun deleteAll()
}

@Dao
interface EvidenceDao {
    @Query("SELECT * FROM evidence ORDER BY at ASC")
    fun getAllEvidenceFlow(): Flow<List<EvidenceEntity>>

    @Query("SELECT * FROM evidence WHERE kuId = :kuId ORDER BY at ASC")
    fun getEvidenceForKuFlow(kuId: String): Flow<List<EvidenceEntity>>

    @Query("SELECT * FROM evidence ORDER BY at ASC")
    suspend fun getAllEvidence(): List<EvidenceEntity>

    @Query("SELECT * FROM evidence WHERE kuId = :kuId ORDER BY at ASC")
    suspend fun getEvidenceForKu(kuId: String): List<EvidenceEntity>

    @Query("SELECT * FROM evidence WHERE id = :id LIMIT 1")
    suspend fun getEvidenceById(id: String): EvidenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvidence(evidence: EvidenceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(evidenceList: List<EvidenceEntity>)

    @Query("UPDATE evidence SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM evidence")
    suspend fun deleteAll()
}

@Dao
interface KuStateDao {
    @Query("SELECT * FROM ku_states")
    fun getAllKuStatesFlow(): Flow<List<KuStateEntity>>

    @Query("SELECT * FROM ku_states WHERE kuId = :kuId LIMIT 1")
    fun getKuStateFlow(kuId: String): Flow<KuStateEntity?>

    @Query("SELECT * FROM ku_states")
    suspend fun getAllKuStates(): List<KuStateEntity>

    @Query("SELECT * FROM ku_states WHERE kuId = :kuId LIMIT 1")
    suspend fun getKuState(kuId: String): KuStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKuState(state: KuStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(states: List<KuStateEntity>)

    @Query("DELETE FROM ku_states")
    suspend fun deleteAll()
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSession(sessionId: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE sessionId = :sessionId LIMIT 1")
    fun getSessionFlow(sessionId: String): Flow<SessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}

@Dao
interface UserPreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE userId = :userId LIMIT 1")
    fun getPreferencesFlow(userId: String = "default_user"): Flow<UserPreferencesEntity?>

    @Query("SELECT * FROM user_preferences WHERE userId = :userId LIMIT 1")
    suspend fun getPreferences(userId: String = "default_user"): UserPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreferences(pref: UserPreferencesEntity)

    @Query("DELETE FROM user_preferences")
    suspend fun deleteAll()
}

@Dao
interface WordDao {
    @Query("SELECT * FROM vocabulary_words ORDER BY createdAt DESC")
    fun getAllWordsFlow(): Flow<List<WordEntity>>

    @Query("SELECT * FROM vocabulary_words ORDER BY createdAt DESC")
    suspend fun getAllWords(): List<WordEntity>

    @Query("SELECT * FROM vocabulary_words WHERE id = :id LIMIT 1")
    suspend fun getWordById(id: String): WordEntity?

    @Query("SELECT * FROM vocabulary_words WHERE word = :word LIMIT 1")
    suspend fun getWordByText(word: String): WordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: WordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<WordEntity>)

    @Query("DELETE FROM vocabulary_words WHERE id = :id")
    suspend fun deleteWord(id: String)

    @Query("DELETE FROM vocabulary_words")
    suspend fun deleteAll()
}

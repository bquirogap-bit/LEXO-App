package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AttemptEntity::class,
        EvidenceEntity::class,
        KuStateEntity::class,
        SessionEntity::class,
        UserPreferencesEntity::class,
        WordEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class LexoDatabase : RoomDatabase() {
    abstract fun attemptDao(): AttemptDao
    abstract fun evidenceDao(): EvidenceDao
    abstract fun kuStateDao(): KuStateDao
    abstract fun sessionDao(): SessionDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun wordDao(): WordDao

    companion object {
        @Volatile
        private var INSTANCE: LexoDatabase? = null

        fun getDatabase(context: Context): LexoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LexoDatabase::class.java,
                    "lexo_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

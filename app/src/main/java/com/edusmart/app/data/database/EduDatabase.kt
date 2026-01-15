package com.edusmart.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.edusmart.app.data.dao.*
import com.edusmart.app.data.entity.*

@Database(
    entities = [
        QuestionEntity::class,
        WrongQuestionEntity::class,
        NoteEntity::class,
        KnowledgePointEntity::class,
        TestRecordEntity::class,
        SpeakingRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
@androidx.room.TypeConverters(Converters::class)
abstract class EduDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    abstract fun wrongQuestionDao(): WrongQuestionDao
    abstract fun noteDao(): NoteDao
    abstract fun knowledgePointDao(): KnowledgePointDao
    abstract fun testRecordDao(): TestRecordDao
    abstract fun speakingRecordDao(): SpeakingRecordDao

    companion object {
        @Volatile
        private var INSTANCE: EduDatabase? = null

        fun getDatabase(context: Context): EduDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EduDatabase::class.java,
                    "edu_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


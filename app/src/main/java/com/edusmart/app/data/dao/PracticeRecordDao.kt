package com.edusmart.app.data.dao

import androidx.room.*
import com.edusmart.app.data.entity.PracticeRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeRecordDao {
    @Query("SELECT * FROM practice_records ORDER BY createdAt DESC")
    fun getAllPracticeRecords(): Flow<List<PracticeRecordEntity>>

    @Query("SELECT * FROM practice_records WHERE sourceQuestionId = :questionId ORDER BY createdAt DESC")
    fun getPracticeRecordsByQuestion(questionId: String): Flow<List<PracticeRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPracticeRecord(record: PracticeRecordEntity)

    @Query("SELECT * FROM practice_records WHERE id = :id")
    suspend fun getPracticeRecordById(id: String): PracticeRecordEntity?

    @Delete
    suspend fun deletePracticeRecord(record: PracticeRecordEntity)

    @Query("SELECT COUNT(*) FROM practice_records WHERE isCorrect = 1")
    suspend fun getCorrectCount(): Int

    @Query("SELECT COUNT(*) FROM practice_records")
    suspend fun getTotalCount(): Int
}

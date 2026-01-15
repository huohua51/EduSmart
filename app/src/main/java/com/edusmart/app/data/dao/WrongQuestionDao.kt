package com.edusmart.app.data.dao

import androidx.room.*
import com.edusmart.app.data.entity.WrongQuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WrongQuestionDao {
    @Query("SELECT * FROM wrong_questions ORDER BY createdAt DESC")
    fun getAllWrongQuestions(): Flow<List<WrongQuestionEntity>>
    
    @Query("SELECT * FROM wrong_questions WHERE nextReviewTime IS NULL OR nextReviewTime <= :currentTime")
    suspend fun getQuestionsToReview(currentTime: Long = System.currentTimeMillis()): List<WrongQuestionEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWrongQuestion(wrongQuestion: WrongQuestionEntity)
    
    @Update
    suspend fun updateWrongQuestion(wrongQuestion: WrongQuestionEntity)
    
    @Delete
    suspend fun deleteWrongQuestion(wrongQuestion: WrongQuestionEntity)
}


package com.edusmart.app.data.dao

import androidx.room.*
import com.edusmart.app.data.entity.QuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE subject = :subject")
    fun getQuestionsBySubject(subject: String): Flow<List<QuestionEntity>>
    
    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getQuestionById(id: String): QuestionEntity?
    
    @Query("SELECT * FROM questions WHERE content LIKE :keyword OR answer LIKE :keyword")
    suspend fun searchQuestions(keyword: String): List<QuestionEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)
    
    @Delete
    suspend fun deleteQuestion(question: QuestionEntity)
}


package com.edusmart.app.repository

import com.edusmart.app.data.dao.WrongQuestionDao
import com.edusmart.app.data.entity.WrongQuestionEntity
import kotlinx.coroutines.flow.Flow

class WrongQuestionRepository(private val wrongQuestionDao: WrongQuestionDao) {

    fun getAllWrongQuestions(): Flow<List<WrongQuestionEntity>> {
        return wrongQuestionDao.getAllWrongQuestions()
    }

    suspend fun getQuestionsToReview(currentTime: Long = System.currentTimeMillis()): List<WrongQuestionEntity> {
        return wrongQuestionDao.getQuestionsToReview(currentTime)
    }

    suspend fun insertWrongQuestion(wrongQuestion: WrongQuestionEntity) {
        wrongQuestionDao.insertWrongQuestion(wrongQuestion)
    }

    suspend fun updateWrongQuestion(wrongQuestion: WrongQuestionEntity) {
        wrongQuestionDao.updateWrongQuestion(wrongQuestion)
    }

    suspend fun deleteWrongQuestion(wrongQuestion: WrongQuestionEntity) {
        wrongQuestionDao.deleteWrongQuestion(wrongQuestion)
    }
}

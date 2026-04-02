package com.edusmart.app.repository

import android.util.Log
import com.edusmart.app.data.dao.QuestionDao
import com.edusmart.app.data.dao.WrongQuestionDao
import com.edusmart.app.data.entity.QuestionEntity
import com.edusmart.app.data.entity.WrongQuestionEntity
import com.edusmart.app.service.AIService
import com.edusmart.app.service.OCRService
import com.edusmart.app.service.WrongQuestionCloudService
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import java.util.UUID

/**
 * 题目仓库 - 云端错题本
 * 错题添加直接上传到云端
 */
class QuestionRepository(
    private val questionDao: QuestionDao,
    private val wrongQuestionDao: WrongQuestionDao,
    private val ocrService: OCRService,
    private val aiService: AIService,
    private val cloudService: WrongQuestionCloudService = WrongQuestionCloudService()
) {
    
    fun getQuestionsBySubject(subject: String): Flow<List<QuestionEntity>> {
        return questionDao.getQuestionsBySubject(subject)
    }
    
    suspend fun getQuestionById(id: String): QuestionEntity? {
        return questionDao.getQuestionById(id)
    }
    
    suspend fun searchQuestions(keyword: String): List<QuestionEntity> {
        return questionDao.searchQuestions(keyword)
    }
    
    /**
     * 扫描题目并识别
     */
    suspend fun scanQuestion(imagePath: String) {
        Log.d("QuestionRepository", "📸 扫描题目: $imagePath")
        // 实现略
    }
    
    /**
     * 添加到错题本 - 云端优先方案
     * 直接上传到云端，不再存储到本地数据库
     */
    suspend fun addToWrongQuestions(
        userId: String,
        token: String,
        questionId: String,
        userAnswer: String? = null,
        wrongReason: String? = null
    ): String {
        Log.d("QuestionRepository", "📝 添加错题到云端: questionId=$questionId")
        
        val question = questionDao.getQuestionById(questionId) 
            ?: throw Exception("题目不存在: $questionId")

        val wrongQuestion = WrongQuestionEntity(
            id = UUID.randomUUID().toString(),
            questionText = question.content,
            answer = question.answer,
            steps = "",
            knowledgePoints = "",
            analysis = wrongReason ?: "",
            userAnswer = userAnswer,
            wrongReason = wrongReason
        )
        
        // 直接上传到云端
        Log.d("QuestionRepository", "☁️ 上传错题到云端...")
        return cloudService.syncWrongQuestion(userId, token, wrongQuestion)
            .onSuccess { cloudId ->
                Log.d("QuestionRepository", "✅ 错题已上传到云端: $cloudId")
            }
            .onFailure { error ->
                Log.e("QuestionRepository", "❌ 错题上传失败: ${error.message}")
            }
            .getOrThrow()
    }
    
    /**
     * 从云端获取错题本
     */
    suspend fun getWrongQuestionsFromCloud(userId: String, token: String): List<WrongQuestionEntity> {
        Log.d("QuestionRepository", "🌐 从云端获取错题本...")
        return cloudService.getCloudWrongQuestions(userId, token)
            .map { jsonObjects ->
                jsonObjects.map { jsonObj ->
                    WrongQuestionEntity(
                        id = jsonObj.optString("_id", ""),
                        questionText = jsonObj.optString("questionText"),
                        answer = jsonObj.optString("answer", ""),
                        steps = jsonObj.optString("steps", ""),
                        knowledgePoints = jsonObj.optString("knowledgePoints", ""),
                        analysis = jsonObj.optString("analysis", ""),
                        createdAt = jsonObj.optLong("createdAt", System.currentTimeMillis()),
                        nextReviewTime = if (jsonObj.has("nextReviewTime")) jsonObj.optLong("nextReviewTime") else null
                    )
                }
            }
            .onSuccess { questions ->
                Log.d("QuestionRepository", "✅ 获取云端错题成功: ${questions.size} 条")
            }
            .onFailure { error ->
                Log.e("QuestionRepository", "❌ 获取云端错题失败: ${error.message}")
            }
            .getOrElse { emptyList() }
    }
    
    /**
     * 本地获取错题本（离线缓存用）
     * @deprecated 使用 getWrongQuestionsFromCloud 代替
     */
    fun getWrongQuestions(): Flow<List<WrongQuestionEntity>> {
        Log.d("QuestionRepository", "⚠️ 使用本地缓存获取错题本")
        return wrongQuestionDao.getAllWrongQuestions()
    }
}


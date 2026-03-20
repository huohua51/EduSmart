package com.edusmart.app.repository

import android.util.Log
import com.edusmart.app.data.dao.WrongQuestionDao
import com.edusmart.app.data.entity.WrongQuestionEntity
import com.edusmart.app.service.WrongQuestionCloudService
import kotlinx.coroutines.flow.Flow

/**
 * 错题仓库 - 云端优先方案
 * 所有数据操作都通过 WrongQuestionCloudService
 * 本地 DAO 仅用作离线缓存（可选）
 */
class WrongQuestionRepository(
    private val wrongQuestionDao: WrongQuestionDao,
    private val cloudService: WrongQuestionCloudService = WrongQuestionCloudService()
) {

    /**
     * 从云端获取所有错题
     */
    suspend fun getAllWrongQuestionsFromCloud(userId: String, token: String): List<WrongQuestionEntity> {
        Log.d("WrongQuestionRepository", "🌐 从云端获取所有错题...")
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
                        reviewCount = jsonObj.optInt("reviewCount", 0),
                        lastReviewTime = if (jsonObj.has("lastReviewTime")) jsonObj.optLong("lastReviewTime") else null,
                        nextReviewTime = if (jsonObj.has("nextReviewTime")) jsonObj.optLong("nextReviewTime") else null,
                        createdAt = jsonObj.optLong("createdAt", System.currentTimeMillis())
                    )
                }
            }
            .onSuccess { questions ->
                Log.d("WrongQuestionRepository", "✅ 从云端获取错题成功: ${questions.size} 条")
            }
            .onFailure { error ->
                Log.e("WrongQuestionRepository", "❌ 从云端获取错题失败: ${error.message}")
            }
            .getOrElse { emptyList() }
    }

    /**
     * 从本地获取所有错题（离线缓存用）
     * @deprecated 使用 getAllWrongQuestionsFromCloud 代替
     */
    fun getAllWrongQuestions(): Flow<List<WrongQuestionEntity>> {
        Log.d("WrongQuestionRepository", "⚠️ 使用本地缓存获取错题")
        return wrongQuestionDao.getAllWrongQuestions()
    }

    /**
     * 从云端获取需要复习的问题
     */
    suspend fun getQuestionsToReviewFromCloud(
        userId: String,
        token: String,
        currentTime: Long = System.currentTimeMillis()
    ): List<WrongQuestionEntity> {
        Log.d("WrongQuestionRepository", "🔄 从云端获取需要复习的问题...")
        return getAllWrongQuestionsFromCloud(userId, token)
            .filter { question ->
                question.nextReviewTime?.let { it <= currentTime } ?: false
            }
    }

    /**
     * 从本地获取需要复习的问题
     * @deprecated 使用 getQuestionsToReviewFromCloud 代替
     */
    suspend fun getQuestionsToReview(currentTime: Long = System.currentTimeMillis()): List<WrongQuestionEntity> {
        return wrongQuestionDao.getQuestionsToReview(currentTime)
    }

    /**
     * 添加错题到云端
     */
    suspend fun addWrongQuestionToCloud(
        userId: String,
        token: String,
        wrongQuestion: WrongQuestionEntity
    ): WrongQuestionEntity {
        Log.d("WrongQuestionRepository", "📝 添加错题到云端: ${wrongQuestion.questionText?.take(20)}")
        return cloudService.syncWrongQuestion(userId, token, wrongQuestion)
            .onSuccess { cloudId ->
                Log.d("WrongQuestionRepository", "✅ 错题已上传到云端: $cloudId")
            }
            .onFailure { error ->
                Log.e("WrongQuestionRepository", "❌ 错题上传失败: ${error.message}")
            }
            .map { cloudId -> wrongQuestion.copy(id = cloudId) }
            .getOrThrow()
    }

    /**
     * 批量添加错题到云端
     */
    suspend fun addWrongQuestionsToCloud(
        userId: String,
        token: String,
        wrongQuestions: List<WrongQuestionEntity>
    ): List<String> {
        Log.d("WrongQuestionRepository", "📦 批量添加错题到云端: ${wrongQuestions.size} 条")
        return cloudService.syncWrongQuestionsBatch(userId, token, wrongQuestions)
            .onSuccess { cloudIds ->
                Log.d("WrongQuestionRepository", "✅ 错题已批量上传: ${cloudIds.size} 条")
            }
            .onFailure { error ->
                Log.e("WrongQuestionRepository", "❌ 批量上传失败: ${error.message}")
            }
            .getOrElse { emptyList() }
    }

    /**
     * 保存错题到本地（离线缓存）
     * @deprecated 使用 addWrongQuestionToCloud 代替
     */
    suspend fun insertWrongQuestion(wrongQuestion: WrongQuestionEntity) {
        Log.d("WrongQuestionRepository", "⚠️ 保存到本地缓存: ${wrongQuestion.id}")
        wrongQuestionDao.insertWrongQuestion(wrongQuestion)
    }

    /**
     * 更新错题到云端
     */
    suspend fun updateWrongQuestionInCloud(
        userId: String,
        token: String,
        wrongQuestion: WrongQuestionEntity
    ) {
        Log.d("WrongQuestionRepository", "🔄 更新错题到云端: ${wrongQuestion.id}")
        // 注意：需要在后端添加 updateWrongQuestion 接口
        // 这里暂时使用删除+重新添加的方式
        deleteWrongQuestionInCloud(userId, token, wrongQuestion)
        addWrongQuestionToCloud(userId, token, wrongQuestion)
    }

    /**
     * 更新本地错题（离线缓存）
     * @deprecated 使用 updateWrongQuestionInCloud 代替
     */
    suspend fun updateWrongQuestion(wrongQuestion: WrongQuestionEntity) {
        Log.d("WrongQuestionRepository", "⚠️ 更新本地缓存: ${wrongQuestion.id}")
        wrongQuestionDao.updateWrongQuestion(wrongQuestion)
    }

    /**
     * 从云端删除错题
     */
    suspend fun deleteWrongQuestionInCloud(
        userId: String,
        token: String,
        wrongQuestion: WrongQuestionEntity
    ) {
        Log.d("WrongQuestionRepository", "🗑️ 从云端删除错题: ${wrongQuestion.id}")
        cloudService.deleteWrongQuestion(userId, token, wrongQuestion.id)
            .onSuccess {
                Log.d("WrongQuestionRepository", "✅ 错题已从云端删除: ${wrongQuestion.id}")
            }
            .onFailure { error ->
                Log.e("WrongQuestionRepository", "❌ 云端删除失败: ${error.message}")
                throw error
            }
    }

    /**
     * 删除本地错题（离线缓存）
     * @deprecated 使用 deleteWrongQuestionInCloud 代替
     */
    suspend fun deleteWrongQuestion(wrongQuestion: WrongQuestionEntity) {
        Log.d("WrongQuestionRepository", "⚠️ 删除本地缓存: ${wrongQuestion.id}")
        wrongQuestionDao.deleteWrongQuestion(wrongQuestion)
    }
}

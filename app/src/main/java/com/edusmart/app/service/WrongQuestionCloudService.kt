package com.edusmart.app.service

import android.util.Base64
import android.util.Log
import com.edusmart.app.config.SDKConfig
import com.edusmart.app.data.entity.WrongQuestionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 错题本云同步服务
 * 负责将本地错题本同步到腾讯云 CloudBase
 */
class WrongQuestionCloudService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val baseUrl = SDKConfig.TCB_API_BASE_URL
    private val functionName = "cloudbase-wrongquestion"
    
    private companion object {
        private const val TAG = "WrongQuestionCloud"
    }
    
    /**
     * 同步单个错题到云端
     * @param userId 用户ID
     * @param token 用户token
     * @param wrongQuestion 本地错题实体
     * @return 是否同步成功及云端ID
     */
    suspend fun syncWrongQuestion(
        userId: String,
        token: String,
        wrongQuestion: WrongQuestionEntity
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📤 开始同步错题: ${wrongQuestion.id}")
            
            // 构造请求JSON（仅文本信息，不上传图片）
            val questionJson = JSONObject().apply {
                put("questionText", wrongQuestion.questionText)
                put("answer", wrongQuestion.answer)
                put("analysis", wrongQuestion.analysis)

                // 解析步骤
                try {
                    val stepsArray = JSONArray(wrongQuestion.steps)
                    put("steps", stepsArray)
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ 步骤解析失败，使用默认值: ${e.message}")
                    put("steps", JSONArray(listOf(wrongQuestion.answer)))
                }

                // 解析知识点
                try {
                    val kpArray = JSONArray(wrongQuestion.knowledgePoints)
                    put("knowledgePoints", kpArray)
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ 知识点解析失败，使用空数组: ${e.message}")
                    put("knowledgePoints", JSONArray())
                }

                // 仅添加非空的可选字段
                wrongQuestion.userAnswer?.takeIf { it.isNotEmpty() }?.let {
                    put("userAnswer", it)
                }
                wrongQuestion.wrongReason?.takeIf { it.isNotEmpty() }?.let {
                    put("wrongReason", it)
                }

                // 复习相关字段
                put("reviewCount", wrongQuestion.reviewCount)
                wrongQuestion.lastReviewTime?.let { put("lastReviewTime", it) }
                wrongQuestion.nextReviewTime?.let { put("nextReviewTime", it) }
                put("createdAt", wrongQuestion.createdAt)
                // ✅ 注意：不上传图片，图片路径仅用于本地存储
            }
            
            // 构造完整请求（仅包含文本信息）
            val requestBodyJson = JSONObject().apply {
                put("action", "saveWrongQuestion")
                put("userId", userId)
                put("token", token)
                put("question", questionJson)
            }
            
            val requestBodyStr = requestBodyJson.toString()
            Log.d(TAG, "📤 请求体大小: ${requestBodyStr.length} 字节")
            Log.d(TAG, "📤 请求体:\n$requestBodyStr")
            
            val requestBody = requestBodyStr.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$baseUrl/$functionName")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()
            
            // 发送请求
            Log.d(TAG, "📤 正在发送 POST 请求到: $baseUrl/$functionName")
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            Log.d(TAG, "📥 收到响应 HTTP ${response.code}")
            Log.d(TAG, "📥 响应体: $responseBody")
            
            if (response.isSuccessful && responseBody != null) {
                try {
                    val jsonResp = JSONObject(responseBody)
                    if (jsonResp.optInt("code") == 0) {
                        val data = jsonResp.optJSONObject("data")
                        val cloudId = data?.optString("_id") 
                            ?: data?.optString("id")
                            ?: jsonResp.optString("id")
                            ?: wrongQuestion.id
                        
                        Log.d(TAG, "✅ 错题同步成功，云端ID: $cloudId")
                        return@withContext Result.success(cloudId)
                    } else {
                        val errorMsg = jsonResp.optString("message", "同步失败")
                        Log.e(TAG, "❌ 云端错误 (${jsonResp.optInt("code")}): $errorMsg")
                        return@withContext Result.failure(Exception(errorMsg))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 解析响应失败: ${e.message}", e)
                    return@withContext Result.failure(e)
                }
            } else {
                val errorMsg = responseBody ?: "未知错误"
                Log.e(TAG, "❌ HTTP请求失败: ${response.code}")
                Log.e(TAG, "❌ 错误详情: $errorMsg")
                return@withContext Result.failure(Exception("HTTP ${response.code}: $errorMsg"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 同步异常: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * 批量同步错题到云端（用于试卷扫描等场景）
     */
    suspend fun syncWrongQuestionsBatch(
        userId: String,
        token: String,
        wrongQuestions: List<WrongQuestionEntity>
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始批量同步 ${wrongQuestions.size} 道错题")
            
            val successIds = mutableListOf<String>()
            val failedIds = mutableListOf<String>()
            
            wrongQuestions.forEach { question ->
                syncWrongQuestion(userId, token, question)
                    .onSuccess { cloudId ->
                        successIds.add(cloudId)
                    }
                    .onFailure { error ->
                        Log.w(TAG, "单题同步失败 ${question.id}: ${error.message}")
                        failedIds.add(question.id)
                    }
            }
            
            Log.d(TAG, "批量同步完成: 成功 ${successIds.size}, 失败 ${failedIds.size}")
            
            return@withContext if (failedIds.isEmpty()) {
                Result.success(successIds)
            } else {
                // 部分成功也返回成功，但记录失败的本地ID
                Result.success(successIds)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 批量同步异常: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * 获取云端错题列表（用于对比本地和云端）
     */
    suspend fun getCloudWrongQuestions(
        userId: String,
        token: String
    ): Result<List<JSONObject>> = withContext(Dispatchers.IO) {
        try {
            val requestBody = JSONObject().apply {
                put("action", "getWrongQuestions")
                put("userId", userId)
                put("token", token)
            }.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$baseUrl/$functionName")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val jsonResp = JSONObject(responseBody)
                if (jsonResp.optInt("code") == 0) {
                    val dataArray = jsonResp.optJSONArray("data") ?: JSONArray()
                    val questions = mutableListOf<JSONObject>()
                    for (i in 0 until dataArray.length()) {
                        questions.add(dataArray.getJSONObject(i))
                    }
                    Log.d(TAG, "✅ 获取云端错题列表成功，共 ${questions.size} 条")
                    return@withContext Result.success(questions)
                } else {
                    return@withContext Result.failure(Exception(jsonResp.optString("message", "获取失败")))
                }
            } else {
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取云端错题失败: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    /**
     * 删除云端错题
     * @param userId 用户ID
     * @param token 用户token
     * @param questionId 错题的云端ID
     * @return 是否删除成功
     */
    suspend fun deleteWrongQuestion(
        userId: String,
        token: String,
        questionId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🗑️ 开始删除云端错题: $questionId")

            val requestBody = JSONObject().apply {
                put("action", "deleteWrongQuestion")
                put("userId", userId)
                put("token", token)
                put("questionId", questionId)
            }.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$baseUrl/$functionName")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            Log.d(TAG, "📤 正在发送删除请求到: $baseUrl/$functionName")
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d(TAG, "📥 收到响应 HTTP ${response.code}")
            Log.d(TAG, "📥 响应体: $responseBody")

            if (response.isSuccessful && responseBody != null) {
                try {
                    val jsonResp = JSONObject(responseBody)
                    if (jsonResp.optInt("code") == 0) {
                        Log.d(TAG, "✅ 错题删除成功: $questionId")
                        return@withContext Result.success(Unit)
                    } else {
                        val errorMsg = jsonResp.optString("message", "删除失败")
                        Log.e(TAG, "❌ 云端错误 (${jsonResp.optInt("code")}): $errorMsg")
                        return@withContext Result.failure(Exception(errorMsg))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 解析响应失败: ${e.message}", e)
                    return@withContext Result.failure(e)
                }
            } else {
                val errorMsg = responseBody ?: "未知错误"
                Log.e(TAG, "❌ HTTP请求失败: ${response.code}")
                Log.e(TAG, "❌ 错误详情: $errorMsg")
                return@withContext Result.failure(Exception("HTTP ${response.code}: $errorMsg"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 删除异常: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }
}

// 扩展函数用于便捷访问
private fun String.toMediaTypeObject() = this.toMediaType()

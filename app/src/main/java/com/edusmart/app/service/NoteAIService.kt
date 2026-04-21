package com.edusmart.app.service

import android.content.Context
import android.content.SharedPreferences
import com.edusmart.app.EduSmartApplication
import com.edusmart.app.config.SDKConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * 笔记 AI 服务。
 *
 * 原实现直接在客户端调用通义千问 API，现已改为调用自建后端的 Spring AI 端点：
 *  - POST /api/ai/notes/polish
 *  - POST /api/ai/notes/summary
 *  - POST /api/ai/notes/title
 *  - POST /api/ai/notes/knowledge-points
 *  - POST /api/ai/notes/subject
 *  - POST /api/ai/notes/qa
 *
 * 类名、公共方法签名保持不变，避免业务层大改。
 *
 * 认证：从 `auth` SharedPreferences 中读取登录时保存的 token。
 */
class NoteAIService(context: Context? = null) {

    /**
     * 兼容历史代码的 "空构造函数"：优先使用显式传入的 context，
     * 否则 fallback 到 [EduSmartApplication.instance]。
     */
    private val appContext: Context? = context
        ?: runCatching { EduSmartApplication.instance }.getOrNull()

    private val prefs: SharedPreferences? by lazy {
        appContext?.getSharedPreferences("auth", Context.MODE_PRIVATE)
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val apiBaseUrl: String get() = SDKConfig.SERVER_API_BASE_URL

    suspend fun polishNote(content: String, subject: String? = null): String = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("content", content)
            if (subject != null) put("subject", subject)
        }
        try {
            val resp = postAi("/ai/notes/polish", body)
            resp.optString("text").ifBlank { content }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "AI 润色失败", e)
            "润色失败: ${e.message}\n\n原始内容：\n$content"
        }
    }

    suspend fun summarizeNote(content: String, subject: String? = null): NoteSummary = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("content", content)
            if (subject != null) put("subject", subject)
        }
        try {
            val resp = postAi("/ai/notes/summary", body)
            NoteSummary(
                summary = resp.optString("summary"),
                keyPoints = resp.optJSONArray("keyPoints").toStringList(),
                tags = resp.optJSONArray("tags").toStringList()
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "AI 总结失败", e)
            NoteSummary("总结失败: ${e.message}", emptyList(), emptyList())
        }
    }

    suspend fun generateTitle(content: String, subject: String? = null): String = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("content", content)
            if (subject != null) put("subject", subject)
        }
        try {
            val resp = postAi("/ai/notes/title", body)
            resp.optString("title").trim().take(50).ifBlank { "笔记标题" }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "AI 生成标题失败", e)
            "笔记标题"
        }
    }

    suspend fun extractKnowledgePoints(content: String, subject: String? = null): List<String> = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("content", content)
            if (subject != null) put("subject", subject)
        }
        try {
            val resp = postAi("/ai/notes/knowledge-points", body)
            resp.optJSONArray("points").toStringList()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "AI 知识点提取失败", e)
            emptyList()
        }
    }

    suspend fun generateSubject(content: String, title: String? = null): String = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("content", content)
            if (title != null) put("title", title)
        }
        try {
            val resp = postAi("/ai/notes/subject", body)
            resp.optString("subject").ifBlank { "其他" }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "AI 生成科目失败", e)
            "其他"
        }
    }

    suspend fun answerQuestion(noteContent: String, question: String): String = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("content", noteContent)
            put("question", question)
        }
        try {
            val resp = postAi("/ai/notes/qa", body)
            resp.optString("answer").ifBlank { "无法回答问题" }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "AI 问答失败", e)
            "无法回答问题: ${e.message}"
        }
    }

    /* ============================== internal ============================== */

    private fun currentToken(): String {
        return prefs?.getString("token", null)
            ?: throw IOException("用户未登录，无法调用 AI 服务")
    }

    /** 向后端 AI 端点发送请求，返回 data 对象。 */
    private fun postAi(path: String, body: JSONObject): JSONObject {
        val token = currentToken()
        val request = Request.Builder()
            .url("$apiBaseUrl$path")
            .addHeader("Authorization", "Bearer $token")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string()
            if (!response.isSuccessful || bodyStr == null) {
                throw IOException("AI 后端调用失败: HTTP ${response.code} - ${bodyStr ?: "no body"}")
            }
            val root = JSONObject(bodyStr)
            if (root.optInt("code", -1) != 0) {
                throw IOException(root.optString("message", "AI 后端返回错误"))
            }
            return root.optJSONObject("data") ?: JSONObject()
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).map { getString(it) }
    }

    companion object {
        private const val TAG = "NoteAIService"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

/**
 * 笔记总结结果（保持原 data class 签名不变）。
 */
data class NoteSummary(
    val summary: String,
    val keyPoints: List<String>,
    val tags: List<String>
)

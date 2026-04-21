package com.edusmart.app.service

import com.edusmart.app.config.SDKConfig
import com.edusmart.app.data.entity.NoteEntity
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.resume

/**
 * 笔记云端服务。
 *
 * 类名、方法签名保持与原 "腾讯云开发" 时期一致，
 * 内部实现已切换到自建 Spring Boot 后端：
 *  - GET    /api/notes?subject=xxx
 *  - POST   /api/notes
 *  - PUT    /api/notes/{id}
 *  - DELETE /api/notes/{id}
 *  - POST   /api/notes/files
 */
class CloudBaseNoteService {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val apiBaseUrl: String get() = SDKConfig.SERVER_API_BASE_URL

    /* ========== 文件上传 ========== */

    private fun uploadNoteFileSync(
        userId: String,
        token: String,
        filePath: String,
        fileName: String
    ): String? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null
            val bytes = file.readBytes()
            val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

            val json = JSONObject().apply {
                put("fileBase64", base64)
                put("fileName", fileName)
                put("fileType", guessFileType(fileName))
            }
            val request = Request.Builder()
                .url("$apiBaseUrl/notes/files")
                .addHeader("Authorization", "Bearer $token")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            if (!response.isSuccessful || body == null) return null
            val root = JSONObject(body)
            if (root.optInt("code") != 0) return null
            root.optJSONObject("data")?.optString("url")?.let { absUrl(it) }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "同步上传文件失败", e)
            null
        }
    }

    suspend fun uploadNoteFile(
        userId: String,
        token: String,
        filePath: String,
        fileName: String
    ): Result<String> = suspendCancellableCoroutine { continuation ->
        try {
            val file = File(filePath)
            if (!file.exists()) {
                continuation.resume(Result.failure(Exception("文件不存在: $filePath")))
                return@suspendCancellableCoroutine
            }
            val bytes = file.readBytes()
            val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

            val json = JSONObject().apply {
                put("fileBase64", base64)
                put("fileName", fileName)
                put("fileType", guessFileType(fileName))
            }
            val request = Request.Builder()
                .url("$apiBaseUrl/notes/files")
                .addHeader("Authorization", "Bearer $token")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: java.io.IOException) {
                    continuation.resume(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    if (!response.isSuccessful || body == null) {
                        continuation.resume(Result.failure(Exception("HTTP ${response.code}")))
                        return
                    }
                    try {
                        val root = JSONObject(body)
                        if (root.optInt("code") == 0) {
                            val url = root.optJSONObject("data")?.optString("url").orEmpty()
                            if (url.isNotBlank()) {
                                continuation.resume(Result.success(absUrl(url)))
                            } else {
                                continuation.resume(Result.failure(Exception("上传成功但未返回 URL")))
                            }
                        } else {
                            continuation.resume(Result.failure(Exception(root.optString("message", "上传失败"))))
                        }
                    } catch (e: Exception) {
                        continuation.resume(Result.failure(e))
                    }
                }
            })
        } catch (e: Exception) {
            continuation.resume(Result.failure(e))
        }
    }

    /* ========== 笔记 CRUD ========== */

    suspend fun createNote(
        userId: String,
        token: String,
        note: NoteEntity
    ): Result<NoteEntity> = suspendCancellableCoroutine { continuation ->
        Thread {
            try {
                // 1) 先上传图片 / 音频
                val imageUrls = note.images.orEmpty().mapNotNull { path ->
                    uploadNoteFileSync(userId, token, path, File(path).name)
                }
                val audioUrl = note.audioPath?.let { path ->
                    uploadNoteFileSync(userId, token, path, File(path).name)
                }

                // 2) 组装 body 并发送
                val body = JSONObject().apply {
                    put("id", note.id)
                    put("title", note.title)
                    put("subject", note.subject)
                    put("content", note.content)
                    put("images", JSONArray().apply { imageUrls.forEach { put(it) } })
                    if (audioUrl != null) put("audioPath", audioUrl) else put("audioPath", JSONObject.NULL)
                    if (note.transcript != null) put("transcript", note.transcript) else put("transcript", JSONObject.NULL)
                    put("knowledgePoints", JSONArray().apply {
                        note.knowledgePoints.orEmpty().forEach { put(it) }
                    })
                }
                val request = Request.Builder()
                    .url("$apiBaseUrl/notes")
                    .addHeader("Authorization", "Bearer $token")
                    .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: java.io.IOException) {
                        continuation.resume(Result.failure(e))
                    }

                    override fun onResponse(call: Call, response: Response) {
                        continuation.resume(parseNoteResponse(response, fallback = note
                            .copy(
                                images = imageUrls.takeIf { it.isNotEmpty() },
                                audioPath = audioUrl ?: note.audioPath
                            )))
                    }
                })
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }.start()
    }

    suspend fun updateNote(
        userId: String,
        token: String,
        note: NoteEntity
    ): Result<NoteEntity> = suspendCancellableCoroutine { continuation ->
        try {
            val body = JSONObject().apply {
                put("title", note.title)
                put("subject", note.subject)
                put("content", note.content)
                put("images", JSONArray().apply { note.images.orEmpty().forEach { put(it) } })
                if (note.audioPath != null) put("audioPath", note.audioPath) else put("audioPath", JSONObject.NULL)
                if (note.transcript != null) put("transcript", note.transcript) else put("transcript", JSONObject.NULL)
                put("knowledgePoints", JSONArray().apply {
                    note.knowledgePoints.orEmpty().forEach { put(it) }
                })
            }
            val request = Request.Builder()
                .url("$apiBaseUrl/notes/${note.id}")
                .addHeader("Authorization", "Bearer $token")
                .put(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: java.io.IOException) {
                    continuation.resume(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(parseNoteResponse(response, fallback = note))
                }
            })
        } catch (e: Exception) {
            continuation.resume(Result.failure(e))
        }
    }

    suspend fun deleteNote(
        userId: String,
        token: String,
        noteId: String
    ): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder()
            .url("$apiBaseUrl/notes/$noteId")
            .addHeader("Authorization", "Bearer $token")
            .delete()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                continuation.resume(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) {
                    continuation.resume(Result.failure(Exception("HTTP ${response.code}")))
                    return
                }
                try {
                    val root = JSONObject(body)
                    if (root.optInt("code") == 0) {
                        continuation.resume(Result.success(Unit))
                    } else {
                        continuation.resume(Result.failure(Exception(root.optString("message", "删除失败"))))
                    }
                } catch (e: Exception) {
                    continuation.resume(Result.failure(e))
                }
            }
        })
    }

    suspend fun getNotes(
        userId: String,
        token: String,
        subject: String? = null
    ): Result<List<NoteEntity>> = suspendCancellableCoroutine { continuation ->
        val urlBuilder = StringBuilder("$apiBaseUrl/notes")
        if (!subject.isNullOrBlank()) urlBuilder.append("?subject=").append(java.net.URLEncoder.encode(subject, "UTF-8"))

        val request = Request.Builder()
            .url(urlBuilder.toString())
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                continuation.resume(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) {
                    continuation.resume(Result.failure(Exception("HTTP ${response.code}")))
                    return
                }
                try {
                    val root = JSONObject(body)
                    if (root.optInt("code") != 0) {
                        continuation.resume(Result.failure(Exception(root.optString("message", "获取失败"))))
                        return
                    }
                    val arr = root.optJSONArray("data") ?: JSONArray()
                    val list = (0 until arr.length()).map { i -> jsonToNote(arr.getJSONObject(i)) }
                    continuation.resume(Result.success(list))
                } catch (e: Exception) {
                    continuation.resume(Result.failure(e))
                }
            }
        })
    }

    /* ========== 解析 / 工具 ========== */

    private fun parseNoteResponse(response: Response, fallback: NoteEntity): Result<NoteEntity> {
        val body = response.body?.string()
        if (!response.isSuccessful || body == null) {
            return Result.failure(Exception("HTTP ${response.code}: $body"))
        }
        return try {
            val root = JSONObject(body)
            if (root.optInt("code") != 0) {
                return Result.failure(Exception(root.optString("message", "请求失败")))
            }
            val data = root.optJSONObject("data") ?: return Result.success(fallback)
            Result.success(jsonToNote(data, fallback))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun jsonToNote(obj: JSONObject, fallback: NoteEntity? = null): NoteEntity {
        val images = obj.optJSONArray("images")?.let { arr ->
            (0 until arr.length()).map { absUrl(arr.getString(it)) }
        }
        val kps = obj.optJSONArray("knowledgePoints")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        }
        return NoteEntity(
            id = obj.optString("id").ifBlank { obj.optString("_id") }.ifBlank { fallback?.id ?: "" },
            title = obj.optString("title", fallback?.title ?: ""),
            subject = obj.optString("subject", fallback?.subject ?: ""),
            content = obj.optString("content", fallback?.content ?: ""),
            images = images?.takeIf { it.isNotEmpty() } ?: fallback?.images,
            audioPath = obj.optString("audioPath").takeIf { !it.isNullOrBlank() && it != "null" }?.let { absUrl(it) }
                ?: fallback?.audioPath,
            transcript = obj.optString("transcript").takeIf { !it.isNullOrBlank() && it != "null" }
                ?: fallback?.transcript,
            knowledgePoints = kps ?: fallback?.knowledgePoints,
            createdAt = obj.optLong("createdAt", fallback?.createdAt ?: System.currentTimeMillis()),
            updatedAt = obj.optLong("updatedAt", fallback?.updatedAt ?: System.currentTimeMillis())
        )
    }

    private fun guessFileType(fileName: String): String = when {
        fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true)
                || fileName.endsWith(".png", true) || fileName.endsWith(".webp", true)
                || fileName.endsWith(".gif", true) -> "image"
        fileName.endsWith(".mp3", true) || fileName.endsWith(".wav", true)
                || fileName.endsWith(".m4a", true) || fileName.endsWith(".aac", true)
                || fileName.endsWith(".amr", true) -> "audio"
        else -> "file"
    }

    private fun absUrl(url: String): String {
        if (url.isBlank()) return url
        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("data:")) return url
        val base = SDKConfig.SERVER_BASE_URL.trimEnd('/')
        return if (url.startsWith("/")) "$base$url" else "$base/$url"
    }

    companion object {
        private const val TAG = "CloudBaseNoteService"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

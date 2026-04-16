package com.edusmart.app.service

import com.edusmart.app.config.SDKConfig
import com.edusmart.app.data.entity.NoteEntity
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import java.io.File

/**
 * Java 后端笔记服务（Spring Boot）
 * 对接 `edusmart-server` 的 `/api/notes`。
 */
class CloudBaseNoteService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()
    
    private val baseUrl = SDKConfig.JAVA_API_BASE_URL
    
    /**
     * 同步上传笔记文件（用于非协程环境）
     */
    // Java 后端当前版本不提供“上传文件并返回 URL”的接口：
    // 建议改成：图片/音频先上传到对象存储（COS/OSS/七牛等），这里仅保存 URL。
    
    /**
     * 异步上传笔记文件
     */
    // 如需保留 uploadNoteFile，请在 Java 后端新增 /api/files 或 /api/notes/upload 接口后再对接。
    
    /**
     * 创建笔记到云端
     */
    suspend fun createNote(
        userId: String,
        token: String,
        note: NoteEntity
    ): Result<NoteEntity> = suspendCancellableCoroutine { continuation ->
        Thread {
            try {
                val requestJson = JSONObject().apply {
                    put("title", note.title)
                    put("subject", note.subject)
                    put("content", note.content)
                    put("transcript", note.transcript ?: JSONObject.NULL)
                    put("knowledgePoints", JSONArray().apply {
                        note.knowledgePoints?.forEach { this.put(it) }
                    })
                    put("images", JSONArray().apply {
                        note.images?.forEach { this.put(it) }
                    })
                    // Java 后端字段叫 audioUrl，这里先用 note.audioPath 透传（建议你改成真正 URL）
                    put("audioUrl", note.audioPath ?: JSONObject.NULL)
                }

                val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$baseUrl/api/notes")
                    .addHeader("Authorization", "Bearer $token")
                    .post(requestBody)
                    .build()
                
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: java.io.IOException) {
                        continuation.resume(Result.failure(e))
                    }
                    
                    override fun onResponse(call: Call, response: Response) {
                        val body = response.body?.string()
                        if (response.isSuccessful && body != null) {
                            try {
                                val jsonResponse = JSONObject(body)
                                val cloudNote = parseNoteFromJson(jsonResponse, note)
                                continuation.resume(Result.success(cloudNote))
                            } catch (e: Exception) {
                                continuation.resume(Result.failure(e))
                            }
                        } else {
                            continuation.resume(Result.failure(Exception("HTTP请求失败: ${response.code}")))
                        }
                    }
                })
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }.start()  // 启动线程
    }
    
    /**
     * 更新云端笔记
     */
    suspend fun updateNote(
        userId: String,
        token: String,
        note: NoteEntity
    ): Result<NoteEntity> = suspendCancellableCoroutine { continuation ->
        Thread {
        try {
            val requestJson = JSONObject().apply {
                put("title", note.title)
                put("subject", note.subject)
                put("content", note.content)
                put("transcript", note.transcript ?: JSONObject.NULL)
                put("knowledgePoints", JSONArray().apply {
                    note.knowledgePoints?.forEach { this.put(it) }
                })
                put("images", JSONArray().apply {
                    note.images?.forEach { this.put(it) }
                })
                put("audioUrl", note.audioPath ?: JSONObject.NULL)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/api/notes/${note.id}")
                .addHeader("Authorization", "Bearer $token")
                .post(requestBody)
                .build()
            
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: java.io.IOException) {
                    continuation.resume(Result.failure(e))
                }
                
                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    if (response.isSuccessful && body != null) {
                        try {
                            val jsonResponse = JSONObject(body)
                            val cloudNote = parseNoteFromJson(jsonResponse, note)
                            continuation.resume(Result.success(cloudNote))
                        } catch (e: Exception) {
                            continuation.resume(Result.failure(e))
                        }
                    } else {
                        continuation.resume(Result.failure(Exception("HTTP请求失败: ${response.code}")))
                    }
                }
            })
        } catch (e: Exception) {
            continuation.resume(Result.failure(e))
        }
        }.start()  // 启动线程
    }
    
    /**
     * 删除云端笔记
     */
    suspend fun deleteNote(
        userId: String,
        token: String,
        noteId: String
    ): Result<Unit> = suspendCancellableCoroutine { continuation ->
        Thread {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/api/notes/$noteId")
                    .addHeader("Authorization", "Bearer $token")
                    .delete()
                    .build()
            
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: java.io.IOException) {
                    continuation.resume(Result.failure(e))
                }
                
                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    if (response.isSuccessful) {
                        continuation.resume(Result.success(Unit))
                    } else {
                        continuation.resume(Result.failure(Exception("HTTP请求失败: ${response.code} - ${body ?: "无响应内容"}")))
                    }
                }
            })
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }.start()  // 启动线程
    }
    
    /**
     * 获取云端笔记列表
     */
    suspend fun getNotes(
        userId: String,
        token: String,
        subject: String? = null
    ): Result<List<NoteEntity>> = suspendCancellableCoroutine { continuation ->
        Thread {
            try {
            val url = if (subject != null) {
                "$baseUrl/api/notes".toHttpUrl().newBuilder()
                    .addQueryParameter("subject", subject)
                    .build()
                    .toString()
            } else {
                "$baseUrl/api/notes"
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()
            
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: java.io.IOException) {
                    continuation.resume(Result.failure(e))
                }
                
                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    if (response.isSuccessful && body != null) {
                        try {
                            val dataArray = JSONArray(body)
                            val notes = mutableListOf<NoteEntity>()
                            for (i in 0 until dataArray.length()) {
                                val item = dataArray.getJSONObject(i)
                                notes.add(parseNoteFromJson(item, null))
                            }
                            continuation.resume(Result.success(notes))
                        } catch (e: Exception) {
                            continuation.resume(Result.failure(e))
                        }
                    } else {
                        continuation.resume(Result.failure(Exception("HTTP请求失败: ${response.code}")))
                    }
                }
            })
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }.start()  // 启动线程
    }

    private fun parseNoteFromJson(json: JSONObject, fallback: NoteEntity?): NoteEntity {
        val imagesArray = json.optJSONArray("images")
        val knowledgePointsArray = json.optJSONArray("knowledgePoints")
        return NoteEntity(
            id = json.optLong("id", fallback?.id?.toLongOrNull() ?: 0L).toString(),
            title = json.optString("title", fallback?.title ?: ""),
            subject = json.optString("subject", fallback?.subject ?: ""),
            content = json.optString("content", fallback?.content ?: ""),
            images = if (imagesArray != null) (0 until imagesArray.length()).map { imagesArray.getString(it) } else fallback?.images,
            audioPath = json.optString("audioUrl")?.takeIf { it.isNotEmpty() } ?: fallback?.audioPath,
            transcript = json.optString("transcript")?.takeIf { it.isNotEmpty() } ?: fallback?.transcript,
            knowledgePoints = if (knowledgePointsArray != null) (0 until knowledgePointsArray.length()).map { knowledgePointsArray.getString(it) } else fallback?.knowledgePoints,
            createdAt = fallback?.createdAt ?: System.currentTimeMillis(),
            updatedAt = fallback?.updatedAt ?: System.currentTimeMillis()
        )
    }
}


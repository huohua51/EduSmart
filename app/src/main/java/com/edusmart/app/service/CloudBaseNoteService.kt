package com.edusmart.app.service

import com.edusmart.app.config.SDKConfig
import com.edusmart.app.data.entity.NoteEntity
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import java.io.File

/**
 * 腾讯云开发笔记服务
 * 提供笔记的云端存储和同步功能
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
    
    private val baseUrl = SDKConfig.TCB_API_BASE_URL
    private val functionName = "cloudbase-note"
    
    /**
     * 同步上传笔记文件（用于非协程环境）
     */
    private fun uploadNoteFileSync(
        userId: String,
        token: String,
        filePath: String,
        fileName: String
    ): String? {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return null
            }
            
            val fileBytes = file.readBytes()
            val base64 = android.util.Base64.encodeToString(fileBytes, android.util.Base64.NO_WRAP)
            
            val json = JSONObject().apply {
                put("action", "uploadNoteFile")
                put("userId", userId)
                put("token", token)
                put("fileBase64", base64)
                put("fileName", fileName)
                put("fileType", when {
                    fileName.endsWith(".jpg", ignoreCase = true) -> "image"
                    fileName.endsWith(".png", ignoreCase = true) -> "image"
                    fileName.endsWith(".mp3", ignoreCase = true) -> "audio"
                    fileName.endsWith(".wav", ignoreCase = true) -> "audio"
                    else -> "file"
                })
            }
            
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/$functionName")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            
            if (response.isSuccessful && body != null) {
                val jsonResponse = JSONObject(body)
                if (jsonResponse.optInt("code") == 0) {
                    val data = jsonResponse.optJSONObject("data")
                    data?.optString("url")
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 异步上传笔记文件
     */
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
            
            // 读取文件并转换为 base64
            val fileBytes = file.readBytes()
            val base64 = android.util.Base64.encodeToString(fileBytes, android.util.Base64.NO_WRAP)
            
            val json = JSONObject().apply {
                put("action", "uploadNoteFile")
                put("userId", userId)
                put("token", token)
                put("fileBase64", base64)
                put("fileName", fileName)
                put("fileType", when {
                    fileName.endsWith(".jpg", ignoreCase = true) -> "image"
                    fileName.endsWith(".png", ignoreCase = true) -> "image"
                    fileName.endsWith(".mp3", ignoreCase = true) -> "audio"
                    fileName.endsWith(".wav", ignoreCase = true) -> "audio"
                    else -> "file"
                })
            }
            
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/$functionName")
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
                            if (jsonResponse.optInt("code") == 0) {
                                val data = jsonResponse.optJSONObject("data")
                                val url = data?.optString("url")
                                if (url != null) {
                                    continuation.resume(Result.success(url))
                                } else {
                                    continuation.resume(Result.failure(Exception("上传成功但未返回URL")))
                                }
                            } else {
                                val errorMsg = jsonResponse.optString("message", "上传失败")
                                continuation.resume(Result.failure(Exception(errorMsg)))
                            }
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
    }
    
    /**
     * 创建笔记到云端
     */
    suspend fun createNote(
        userId: String,
        token: String,
        note: NoteEntity
    ): Result<NoteEntity> = suspendCancellableCoroutine { continuation ->
        // 在后台线程中处理文件上传
        Thread {
            try {
                // 先上传图片
                val imageUrls = mutableListOf<String>()
                note.images?.forEach { imagePath ->
                    val fileName = File(imagePath).name
                    val result = try {
                        uploadNoteFileSync(userId, token, imagePath, fileName)
                    } catch (e: Exception) {
                        null
                    }
                    result?.let { imageUrls.add(it) }
                }
                
                // 上传音频
                var audioUrl: String? = null
                note.audioPath?.let { audioPath ->
                    val fileName = File(audioPath).name
                    val result = try {
                        uploadNoteFileSync(userId, token, audioPath, fileName)
                    } catch (e: Exception) {
                        null
                    }
                    result?.let { audioUrl = it }
                }
                
                val noteJson = JSONObject().apply {
                    put("id", note.id)
                    put("title", note.title)
                    put("subject", note.subject)
                    put("content", note.content)
                    put("images", JSONArray().apply {
                        imageUrls.forEach { url -> this.put(url) }
                    })
                    put("audioPath", audioUrl ?: JSONObject.NULL)
                    put("transcript", note.transcript ?: JSONObject.NULL)
                    put("knowledgePoints", JSONArray().apply {
                        note.knowledgePoints?.forEach { point -> this.put(point) }
                    })
                    put("createdAt", note.createdAt)
                    put("updatedAt", note.updatedAt)
                }
                
                val json = JSONObject().apply {
                    put("action", "createNote")
                    put("userId", userId)
                    put("token", token)
                    put("note", noteJson)
                }
                
                val requestBody = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$baseUrl/$functionName")
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
                                if (jsonResponse.optInt("code") == 0) {
                                    val data = jsonResponse.optJSONObject("data")
                                    if (data != null) {
                                        val cloudNote = NoteEntity(
                                            id = data.optString("_id", note.id),
                                            title = data.optString("title", note.title),
                                            subject = data.optString("subject", note.subject),
                                            content = data.optString("content", note.content),
                                            images = imageUrls.takeIf { it.isNotEmpty() },
                                            audioPath = audioUrl,
                                            transcript = data.optString("transcript")?.takeIf { it.isNotEmpty() } ?: note.transcript,
                                            knowledgePoints = note.knowledgePoints,
                                            createdAt = data.optLong("createdAt", note.createdAt),
                                            updatedAt = data.optLong("updatedAt", note.updatedAt)
                                        )
                                        continuation.resume(Result.success(cloudNote))
                                    } else {
                                        continuation.resume(Result.failure(Exception("创建成功但未返回数据")))
                                    }
                                } else {
                                    val errorMsg = jsonResponse.optString("message", "创建失败")
                                    continuation.resume(Result.failure(Exception(errorMsg)))
                                }
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
            val noteJson = JSONObject().apply {
                put("title", note.title)
                put("subject", note.subject)
                put("content", note.content)
                put("images", JSONArray().apply {
                    note.images?.forEach { imageUrl -> this.put(imageUrl) }
                })
                put("audioPath", note.audioPath ?: JSONObject.NULL)
                put("transcript", note.transcript ?: JSONObject.NULL)
                put("knowledgePoints", JSONArray().apply {
                    note.knowledgePoints?.forEach { point -> this.put(point) }
                })
            }
            
            val json = JSONObject().apply {
                put("action", "updateNote")
                put("userId", userId)
                put("token", token)
                put("noteId", note.id)
                put("note", noteJson)
            }
            
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/$functionName")
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
                            if (jsonResponse.optInt("code") == 0) {
                                val data = jsonResponse.optJSONObject("data")
                                if (data != null) {
                                    val cloudNote = NoteEntity(
                                        id = note.id,
                                        title = data.optString("title", note.title),
                                        subject = data.optString("subject", note.subject),
                                        content = data.optString("content", note.content),
                                        images = note.images,
                                        audioPath = note.audioPath,
                                        transcript = note.transcript,
                                        knowledgePoints = note.knowledgePoints,
                                        createdAt = note.createdAt,
                                        updatedAt = data.optLong("updatedAt", System.currentTimeMillis())
                                    )
                                    continuation.resume(Result.success(cloudNote))
                                } else {
                                    continuation.resume(Result.failure(Exception("更新成功但未返回数据")))
                                }
                            } else {
                                val errorMsg = jsonResponse.optString("message", "更新失败")
                                continuation.resume(Result.failure(Exception(errorMsg)))
                            }
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
                val json = JSONObject().apply {
                    put("action", "deleteNote")
                    put("userId", userId)
                    put("token", token)
                    put("noteId", noteId)
                }
                
                val requestBody = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$baseUrl/$functionName")
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
                            if (jsonResponse.optInt("code") == 0) {
                                continuation.resume(Result.success(Unit))
                            } else {
                                val errorMsg = jsonResponse.optString("message", "删除失败")
                                continuation.resume(Result.failure(Exception(errorMsg)))
                            }
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
     * 获取云端笔记列表
     */
    suspend fun getNotes(
        userId: String,
        token: String,
        subject: String? = null
    ): Result<List<NoteEntity>> = suspendCancellableCoroutine { continuation ->
        Thread {
            try {
                val json = JSONObject().apply {
                    put("action", "getNotes")
                    put("userId", userId)
                    put("token", token)
                    if (subject != null) {
                        put("subject", subject)
                    }
                }
            
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/$functionName")
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
                            if (jsonResponse.optInt("code") == 0) {
                                val dataArray = jsonResponse.optJSONArray("data")
                                val notes = mutableListOf<NoteEntity>()
                                
                                if (dataArray != null) {
                                    for (i in 0 until dataArray.length()) {
                                        val item = dataArray.getJSONObject(i)
                                        val imagesArray = item.optJSONArray("images")
                                        val knowledgePointsArray = item.optJSONArray("knowledgePoints")
                                        
                                        notes.add(NoteEntity(
                                            id = item.optString("_id", ""),
                                            title = item.optString("title", ""),
                                            subject = item.optString("subject", ""),
                                            content = item.optString("content", ""),
                                            images = if (imagesArray != null) {
                                                (0 until imagesArray.length()).map { imagesArray.getString(it) }
                                            } else null,
                                            audioPath = item.optString("audioPath")?.takeIf { it.isNotEmpty() },
                                            transcript = item.optString("transcript")?.takeIf { it.isNotEmpty() },
                                            knowledgePoints = if (knowledgePointsArray != null) {
                                                (0 until knowledgePointsArray.length()).map { knowledgePointsArray.getString(it) }
                                            } else null,
                                            createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                                            updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
                                        ))
                                    }
                                }
                                
                                continuation.resume(Result.success(notes))
                            } else {
                                val errorMsg = jsonResponse.optString("message", "获取失败")
                                continuation.resume(Result.failure(Exception(errorMsg)))
                            }
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
}


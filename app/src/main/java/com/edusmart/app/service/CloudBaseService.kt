package com.edusmart.app.service

import com.edusmart.app.config.SDKConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * 腾讯云开发 API 服务（普通函数调用）
 * 
 * 文档: https://cloud.tencent.com/document/product/876/18443
 * 控制台: https://console.cloud.tencent.com/tcb
 * 
 * 注意：使用普通函数，通过云开发API调用
 * URL格式: https://{env-id}.{region}.app.tcloudbase.com/api/{function-name}
 */
class CloudBaseService {
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
    private val envId = SDKConfig.TCB_ENV_ID

    // 云函数名称
    private val functionName = "cloudbase-auth"
    
    /**
     * 用户注册
     * 注意：需要先创建云函数来处理用户注册
     */
    suspend fun register(
        email: String,
        password: String,
        username: String
    ): Result<CloudBaseUser> = suspendCancellableCoroutine { continuation ->
        android.util.Log.d("CloudBaseService", "========== 开始注册 ==========")
        android.util.Log.d("CloudBaseService", "环境ID: $envId")
        android.util.Log.d("CloudBaseService", "邮箱: $email")
        android.util.Log.d("CloudBaseService", "用户名: $username")
        // 腾讯云开发普通函数 + HTTP 触发器调用格式
        // HTTP 触发器路径配置为: /cloudbase-auth
        // 直接使用触发路径，不需要 /api/ 前缀
        val apiUrl = "$baseUrl/$functionName"
        android.util.Log.d("CloudBaseService", "API URL: $apiUrl")
        
        val json = JSONObject().apply {
            put("action", "register")
            put("email", email)
            put("password", password)
            put("username", username)
        }
        
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(apiUrl)
            .post(requestBody)
            .build()
        
        android.util.Log.d("CloudBaseService", "发送注册请求...")
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                android.util.Log.e("CloudBaseService", "注册请求失败", e)
                android.util.Log.e("CloudBaseService", "错误信息: ${e.message}")
                continuation.resume(Result.failure(e))
            }
            
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                android.util.Log.d("CloudBaseService", "收到响应，状态码: ${response.code}")
                android.util.Log.d("CloudBaseService", "响应体: ${body?.take(500)}")
                
                if (response.isSuccessful && body != null) {
                    try {
                        val jsonResponse = JSONObject(body)
                        android.util.Log.d("CloudBaseService", "解析JSON成功")
                        android.util.Log.d("CloudBaseService", "code: ${jsonResponse.optInt("code")}")
                        android.util.Log.d("CloudBaseService", "has userId: ${jsonResponse.has("userId")}")
                        
                        if (jsonResponse.optInt("code") == 0 || jsonResponse.has("userId")) {
                            val userData = jsonResponse.optJSONObject("data") ?: jsonResponse
                            val userId = userData.optString("userId") ?: userData.optString("uid", "")
                            if (userId.isBlank()) {
                                android.util.Log.e("CloudBaseService", "❌ 服务器返回数据格式错误：缺少 userId")
                                android.util.Log.e("CloudBaseService", "完整响应: $body")
                                continuation.resume(Result.failure(Exception("服务器返回数据格式错误：缺少 userId")))
                                return
                            }
                            val user = CloudBaseUser(
                                userId = userId,
                                username = username,
                                email = email,
                                token = userData.optString("token", ""),
                                createdAt = System.currentTimeMillis()
                            )
                            android.util.Log.d("CloudBaseService", "✅ 注册成功")
                            android.util.Log.d("CloudBaseService", "用户ID: $userId")
                            android.util.Log.d("CloudBaseService", "Token: ${user.token.take(20)}...")
                            continuation.resume(Result.success(user))
                        } else {
                            val errorMsg = jsonResponse.optString("message", "注册失败")
                            android.util.Log.e("CloudBaseService", "❌ 注册失败: $errorMsg")
                            android.util.Log.e("CloudBaseService", "完整响应: $body")
                            continuation.resume(Result.failure(Exception(errorMsg)))
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CloudBaseService", "❌ 解析响应失败", e)
                        android.util.Log.e("CloudBaseService", "响应内容: $body")
                        continuation.resume(Result.failure(e))
                    }
                } else {
                    val errorMsg = "注册失败: ${response.code} - ${body ?: "无响应内容"}"
                    android.util.Log.e("CloudBaseService", "❌ HTTP请求失败")
                    android.util.Log.e("CloudBaseService", "状态码: ${response.code}")
                    android.util.Log.e("CloudBaseService", "响应: $body")
                    continuation.resume(Result.failure(Exception(errorMsg)))
                }
            }
        })
    }
    
    /**
     * 用户登录
     * 注意：需要先创建云函数来处理用户登录
     */
    suspend fun login(email: String, password: String): Result<CloudBaseUser> =
        suspendCancellableCoroutine { continuation ->
            android.util.Log.d("CloudBaseService", "========== 开始登录 ==========")
            android.util.Log.d("CloudBaseService", "邮箱: $email")
            val apiUrl = "$baseUrl/$functionName"
            android.util.Log.d("CloudBaseService", "API URL: $apiUrl")
            
            val json = JSONObject().apply {
                put("action", "login")
                put("email", email)
                put("password", password)
            }
            
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .build()
            
            android.util.Log.d("CloudBaseService", "发送登录请求...")
            
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: java.io.IOException) {
                    android.util.Log.e("CloudBaseService", "登录请求失败", e)
                    android.util.Log.e("CloudBaseService", "错误信息: ${e.message}")
                    continuation.resume(Result.failure(e))
                }
                
                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    android.util.Log.d("CloudBaseService", "收到响应，状态码: ${response.code}")
                    android.util.Log.d("CloudBaseService", "响应体: ${body?.take(500)}")
                    
                    if (response.isSuccessful && body != null) {
                        try {
                            val jsonResponse = JSONObject(body)
                            android.util.Log.d("CloudBaseService", "解析JSON成功")
                            android.util.Log.d("CloudBaseService", "code: ${jsonResponse.optInt("code")}")
                            android.util.Log.d("CloudBaseService", "has userId: ${jsonResponse.has("userId")}")
                            
                            if (jsonResponse.optInt("code") == 0 || jsonResponse.has("userId")) {
                                val userData = jsonResponse.optJSONObject("data") ?: jsonResponse
                                val userId = userData.optString("userId") ?: userData.optString("uid", "")
                                if (userId.isBlank()) {
                                    android.util.Log.e("CloudBaseService", "❌ 服务器返回数据格式错误：缺少 userId")
                                    android.util.Log.e("CloudBaseService", "完整响应: $body")
                                    continuation.resume(Result.failure(Exception("服务器返回数据格式错误：缺少 userId")))
                                    return
                                }
                                val user = CloudBaseUser(
                                    userId = userId,
                                    username = userData.optString("username", ""),
                                    email = email,
                                    avatarUrl = userData.optString("avatarUrl", null),
                                    token = userData.optString("token", ""),
                                    createdAt = userData.optLong("createdAt", System.currentTimeMillis())
                                )
                                android.util.Log.d("CloudBaseService", "✅ 登录成功")
                                android.util.Log.d("CloudBaseService", "用户ID: $userId")
                                android.util.Log.d("CloudBaseService", "用户名: ${user.username}")
                                android.util.Log.d("CloudBaseService", "Token: ${user.token.take(20)}...")
                                continuation.resume(Result.success(user))
                            } else {
                                val errorMsg = jsonResponse.optString("message", "登录失败")
                                android.util.Log.e("CloudBaseService", "❌ 登录失败: $errorMsg")
                                android.util.Log.e("CloudBaseService", "完整响应: $body")
                                continuation.resume(Result.failure(Exception(errorMsg)))
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("CloudBaseService", "❌ 解析响应失败", e)
                            android.util.Log.e("CloudBaseService", "响应内容: $body")
                            continuation.resume(Result.failure(e))
                        }
                    } else {
                        val errorMsg = "登录失败: ${response.code} - ${body ?: "无响应内容"}"
                        android.util.Log.e("CloudBaseService", "❌ HTTP请求失败")
                        android.util.Log.e("CloudBaseService", "状态码: ${response.code}")
                        android.util.Log.e("CloudBaseService", "响应: $body")
                        continuation.resume(Result.failure(Exception(errorMsg)))
                    }
                }
            })
        }
    
    /**
     * 获取用户信息
     */
    suspend fun getUserInfo(userId: String, token: String): Result<CloudBaseUser> =
        suspendCancellableCoroutine { continuation ->
            val json = JSONObject().apply {
                put("action", "getUserInfo")
                put("userId", userId)
            }
            
            val apiUrl = "$baseUrl/$functionName"
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(apiUrl)
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
                            if (jsonResponse.optInt("code") == 0 || jsonResponse.has("userId")) {
                                val userData = jsonResponse.optJSONObject("data") ?: jsonResponse
                                val user = CloudBaseUser(
                                    userId = userData.optString("userId") ?: userData.optString("uid", userId),
                                    username = userData.optString("username", ""),
                                    email = userData.optString("email", ""),
                                    avatarUrl = userData.optString("avatarUrl", null),
                                    token = token,
                                    createdAt = userData.optLong("createdAt", System.currentTimeMillis())
                                )
                                continuation.resume(Result.success(user))
                            } else {
                                continuation.resume(Result.failure(
                                    Exception(jsonResponse.optString("message", "获取用户信息失败"))
                                ))
                            }
                        } catch (e: Exception) {
                            continuation.resume(Result.failure(e))
                        }
                    } else {
                        continuation.resume(Result.failure(
                            Exception("获取用户信息失败: ${response.code} - ${body ?: "无响应内容"}")
                        ))
                    }
                }
            })
        }
    
    /**
     * 更新用户信息
     */
    suspend fun updateUserInfo(
        userId: String,
        token: String,
        username: String? = null,
        avatarUrl: String? = null
    ): Result<CloudBaseUser> = suspendCancellableCoroutine { continuation ->
        val json = JSONObject().apply {
            put("action", "updateUserInfo")
            put("userId", userId)
            if (username != null) {
                put("username", username)
            }
            if (avatarUrl != null) {
                put("avatarUrl", avatarUrl)
            }
        }
        
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl/$functionName")
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
                        if (jsonResponse.optInt("code") == 0 || jsonResponse.has("userId")) {
                            val userData = jsonResponse.optJSONObject("data") ?: jsonResponse
                            val user = CloudBaseUser(
                                userId = userData.optString("userId") ?: userData.optString("uid", userId),
                                username = userData.optString("username", ""),
                                email = userData.optString("email", ""),
                                avatarUrl = userData.optString("avatarUrl", null),
                                token = token,
                                createdAt = userData.optLong("createdAt", System.currentTimeMillis())
                            )
                            continuation.resume(Result.success(user))
                        } else {
                            continuation.resume(Result.failure(
                                Exception(jsonResponse.optString("message", "更新失败"))
                            ))
                        }
                    } catch (e: Exception) {
                        continuation.resume(Result.failure(e))
                    }
                } else {
                    continuation.resume(Result.failure(
                        Exception("更新失败: ${response.code} - ${body ?: "无响应内容"}")
                    ))
                }
            }
        })
        }
    
    /**
     * 上传头像到云存储
     */
    suspend fun uploadAvatar(
        userId: String,
        token: String,
        imageBytes: ByteArray,
        fileName: String? = null
    ): Result<String> = suspendCancellableCoroutine { continuation ->
        // 将图片转换为 base64
        val imageBase64 = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
        
        val apiUrl = "$baseUrl/$functionName"
        android.util.Log.d("CloudBaseService", "上传头像到: $apiUrl")
        android.util.Log.d("CloudBaseService", "图片大小: ${imageBytes.size} bytes")
        
        val json = JSONObject().apply {
            put("action", "uploadAvatar")
            put("userId", userId)
            put("imageBase64", imageBase64)
            if (fileName != null) {
                put("fileName", fileName)
            }
        }
        
        val jsonString = json.toString()
        android.util.Log.d("CloudBaseService", "请求 JSON 大小: ${jsonString.length} 字符")
        android.util.Log.d("CloudBaseService", "请求 JSON 前 200 字符: ${jsonString.take(200)}")
        android.util.Log.d("CloudBaseService", "Action 字段: ${json.optString("action", "未找到")}")
        
        val requestBody = jsonString.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer $token")
            .post(requestBody)
            .build()
        
        android.util.Log.d("CloudBaseService", "请求 URL: $apiUrl")
        android.util.Log.d("CloudBaseService", "请求头 Authorization: Bearer ${token.take(20)}...")
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                android.util.Log.e("CloudBaseService", "上传头像请求失败", e)
                continuation.resume(Result.failure(e))
            }
            
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                android.util.Log.d("CloudBaseService", "上传头像响应，状态码: ${response.code}")
                android.util.Log.d("CloudBaseService", "上传头像响应体: ${body?.take(500)}")
                
                if (!response.isSuccessful || body == null) {
                    val errorMsg = "上传失败: ${response.code} - ${body ?: "无响应内容"}"
                    android.util.Log.e("CloudBaseService", "上传头像 HTTP 错误: $errorMsg")
                    continuation.resume(Result.failure(Exception(errorMsg)))
                    return
                }
                
                try {
                    // 尝试解析响应（可能是HTTP响应格式或直接JSON）
                    val outerJson = JSONObject(body)
                    
                    // 检查是否是HTTP响应格式（包含statusCode和body字段）
                    val actualBody: String = if (outerJson.has("statusCode") && outerJson.has("body")) {
                        // HTTP响应格式：{"statusCode":200,"body":"{...}"}
                        val bodyStr = outerJson.optString("body", "")
                        android.util.Log.d("CloudBaseService", "检测到HTTP响应格式，解析body")
                        bodyStr
                    } else {
                        // 直接JSON格式
                        body
                    }
                    
                    val jsonResponse = JSONObject(actualBody)
                    android.util.Log.d("CloudBaseService", "解析后的code: ${jsonResponse.optInt("code")}")
                    
                    if (jsonResponse.optInt("code") == 0) {
                        val data = jsonResponse.optJSONObject("data")
                        val url = data?.optString("url") ?: ""
                        if (url.isNotEmpty()) {
                            android.util.Log.d("CloudBaseService", "✅ 上传成功，URL: $url")
                            continuation.resume(Result.success(url))
                        } else {
                            android.util.Log.e("CloudBaseService", "上传成功但未返回URL")
                            continuation.resume(Result.failure(
                                Exception("上传成功但未返回 URL")
                            ))
                        }
                    } else {
                        val errorMsg = jsonResponse.optString("message", "上传失败")
                        android.util.Log.e("CloudBaseService", "上传头像失败: $errorMsg")
                        continuation.resume(Result.failure(Exception(errorMsg)))
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CloudBaseService", "解析响应失败", e)
                    android.util.Log.e("CloudBaseService", "原始响应: $body")
                    continuation.resume(Result.failure(
                        Exception("解析响应失败: ${e.message}")
                    ))
                }
            }
        })
    }
}

/**
 * 腾讯云开发用户数据模型
 */
data class CloudBaseUser(
    val userId: String,
    val username: String,
    val email: String,
    val avatarUrl: String? = null,
    val token: String = "",
    val createdAt: Long = System.currentTimeMillis()
)


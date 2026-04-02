package com.edusmart.app.service

import com.edusmart.app.config.SDKConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * 阿里云函数计算 API 服务
 * 
 * 文档: https://help.aliyun.com/product/50980.html
 * 控制台: https://fcnext.console.aliyun.com/
 */
class AliyunService {
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
    
    private val baseUrl = SDKConfig.ALIYUN_FC_BASE_URL
    
    /**
     * 用户注册
     */
    suspend fun register(
        email: String,
        password: String,
        username: String
    ): Result<CloudBaseUser> = suspendCancellableCoroutine { continuation ->
        android.util.Log.d("AliyunService", "========== 开始注册 ==========")
        android.util.Log.d("AliyunService", "邮箱: $email")
        android.util.Log.d("AliyunService", "用户名: $username")
        android.util.Log.d("AliyunService", "API URL: $baseUrl")
        
        val json = JSONObject().apply {
            put("action", "register")
            put("email", email)
            put("password", password)
            put("username", username)
        }
        
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(baseUrl)
            .post(requestBody)
            .build()
        
        android.util.Log.d("AliyunService", "发送注册请求...")
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                android.util.Log.e("AliyunService", "注册请求失败", e)
                continuation.resume(Result.failure(e))
            }
            
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                android.util.Log.d("AliyunService", "收到响应，状态码: ${response.code}")
                android.util.Log.d("AliyunService", "响应体: $body")
                
                if (response.isSuccessful && body != null) {
                    try {
                        val jsonResponse = JSONObject(body)
                        if (jsonResponse.optInt("code") == 0) {
                            val userData = jsonResponse.optJSONObject("data")
                            val userId = userData?.optString("userId")
                            val username = userData?.optString("username")
                            val email = userData?.optString("email")
                            
                            if (userId == null || username == null || email == null) {
                                continuation.resume(Result.failure(Exception("服务器返回数据格式错误")))
                                return
                            }
                            
                            val user = CloudBaseUser(
                                userId = userId,
                                username = username,
                                email = email,
                                token = userData.optString("token", ""),
                                createdAt = System.currentTimeMillis()
                            )
                            android.util.Log.d("AliyunService", "✅ 注册成功")
                            continuation.resume(Result.success(user))
                        } else {
                            val errorMsg = jsonResponse.optString("message", "注册失败")
                            android.util.Log.e("AliyunService", "❌ 注册失败: $errorMsg")
                            continuation.resume(Result.failure(Exception(errorMsg)))
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AliyunService", "❌ 解析响应失败", e)
                        continuation.resume(Result.failure(e))
                    }
                } else {
                    val errorMsg = "注册失败: ${response.code} - ${body ?: "无响应内容"}"
                    android.util.Log.e("AliyunService", "❌ HTTP请求失败")
                    continuation.resume(Result.failure(Exception(errorMsg)))
                }
            }
        })
    }
    
    /**
     * 用户登录
     */
    suspend fun login(email: String, password: String): Result<CloudBaseUser> =
        suspendCancellableCoroutine { continuation ->
            android.util.Log.d("AliyunService", "========== 开始登录 ==========")
            android.util.Log.d("AliyunService", "邮箱: $email")
            
            val json = JSONObject().apply {
                put("action", "login")
                put("email", email)
                put("password", password)
            }
            
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(baseUrl)
                .post(requestBody)
                .build()
            
            android.util.Log.d("AliyunService", "发送登录请求...")
            
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: java.io.IOException) {
                    android.util.Log.e("AliyunService", "登录请求失败", e)
                    continuation.resume(Result.failure(e))
                }
                
                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    android.util.Log.d("AliyunService", "收到响应，状态码: ${response.code}")
                    android.util.Log.d("AliyunService", "响应体: $body")
                    
                    if (response.isSuccessful && body != null) {
                        try {
                            val jsonResponse = JSONObject(body)
                            if (jsonResponse.optInt("code") == 0) {
                                val userData = jsonResponse.optJSONObject("data")
                                val userId = userData?.optString("userId")
                                val username = userData?.optString("username")
                                val email = userData?.optString("email")
                                
                                if (userId == null || username == null || email == null) {
                                    continuation.resume(Result.failure(Exception("服务器返回数据格式错误")))
                                    return
                                }
                                
                                val user = CloudBaseUser(
                                    userId = userId,
                                    username = username,
                                    email = email,
                                    token = userData.optString("token", ""),
                                    createdAt = System.currentTimeMillis()
                                )
                                android.util.Log.d("AliyunService", "✅ 登录成功")
                                continuation.resume(Result.success(user))
                            } else {
                                val errorMsg = jsonResponse.optString("message", "登录失败")
                                android.util.Log.e("AliyunService", "❌ 登录失败: $errorMsg")
                                continuation.resume(Result.failure(Exception(errorMsg)))
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("AliyunService", "❌ 解析响应失败", e)
                            continuation.resume(Result.failure(e))
                        }
                    } else {
                        val errorMsg = "登录失败: ${response.code} - ${body ?: "无响应内容"}"
                        android.util.Log.e("AliyunService", "❌ HTTP请求失败")
                        continuation.resume(Result.failure(Exception(errorMsg)))
                    }
                }
            })
        }
    
    /**
     * 获取用户信息
     */
    suspend fun getUserInfo(userId: String, token: String, email: String? = null): Result<CloudBaseUser> =
        suspendCancellableCoroutine { continuation ->
            val json = JSONObject().apply {
                put("action", "getUserInfo")
                put("userId", userId)
                if (email != null) put("email", email)
            }
            
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(baseUrl)
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
                            if (jsonResponse.optInt("code") == 0) {
                                val userData = jsonResponse.optJSONObject("data")
                                val userId = userData?.optString("userId")
                                val username = userData?.optString("username")
                                val email = userData?.optString("email")
                                
                                if (userId != null && username != null && email != null) {
                                    val user = CloudBaseUser(
                                        userId = userId,
                                        username = username,
                                        email = email,
                                        token = token,
                                        createdAt = System.currentTimeMillis()
                                    )
                                    continuation.resume(Result.success(user))
                                } else {
                                    continuation.resume(Result.failure(Exception("服务器返回数据格式错误")))
                                }
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
        }
    
    /**
     * 更新用户信息
     */
    suspend fun updateUserInfo(userId: String, token: String, username: String?, avatarUrl: String?): Result<CloudBaseUser> =
        suspendCancellableCoroutine { continuation ->
            val json = JSONObject().apply {
                put("action", "updateUserInfo")
                put("userId", userId)
                if (username != null) put("username", username)
                if (avatarUrl != null) put("avatarUrl", avatarUrl)
            }
            
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(baseUrl)
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
                            if (jsonResponse.optInt("code") == 0) {
                                val userData = jsonResponse.optJSONObject("data")
                                val userId = userData?.optString("userId")
                                val username = userData?.optString("username")
                                val email = userData?.optString("email")
                                
                                if (userId != null && username != null && email != null) {
                                    val user = CloudBaseUser(
                                        userId = userId,
                                        username = username,
                                        email = email,
                                        token = token,
                                        createdAt = System.currentTimeMillis()
                                    )
                                    continuation.resume(Result.success(user))
                                } else {
                                    continuation.resume(Result.failure(Exception("服务器返回数据格式错误")))
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
        }
    
    /**
     * 上传头像
     */
    suspend fun uploadAvatar(userId: String, token: String, imageBytes: ByteArray, fileName: String): Result<String> =
        suspendCancellableCoroutine { continuation ->
            val base64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
            
            val json = JSONObject().apply {
                put("action", "uploadAvatar")
                put("userId", userId)
                put("imageBase64", base64Image)
                put("fileName", fileName)
            }
            
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(baseUrl)
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
        }
}


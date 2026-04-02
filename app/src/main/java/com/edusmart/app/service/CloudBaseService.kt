package com.edusmart.app.service

import com.edusmart.app.config.SDKConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * Java 后端 API 服务（Spring Boot）
 *
 * 对接 `edusmart-server`：
 * - POST /api/auth/register
 * - POST /api/auth/login
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
    
    private val baseUrl = SDKConfig.JAVA_API_BASE_URL
    
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
        android.util.Log.d("CloudBaseService", "邮箱: $email")
        android.util.Log.d("CloudBaseService", "用户名: $username")
        val apiUrl = "$baseUrl/api/auth/register"
        android.util.Log.d("CloudBaseService", "API URL: $apiUrl")
        
        val json = JSONObject().apply {
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
                        val userId = jsonResponse.optLong("userId", -1L)
                        val token = jsonResponse.optString("token", "")
                        val respEmail = jsonResponse.optString("email", email)
                        val respUsername = jsonResponse.optString("username", username)
                        if (userId <= 0 || token.isBlank()) {
                            val errorMsg = jsonResponse.optString("message", "注册失败")
                            continuation.resume(Result.failure(Exception(errorMsg)))
                            return
                        }
                        val user = CloudBaseUser(
                            userId = userId.toString(),
                            username = respUsername,
                            email = respEmail,
                            token = token,
                            createdAt = System.currentTimeMillis()
                        )
                        continuation.resume(Result.success(user))
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
            val apiUrl = "$baseUrl/api/auth/login"
            android.util.Log.d("CloudBaseService", "API URL: $apiUrl")
            
            val json = JSONObject().apply {
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
                            val userId = jsonResponse.optLong("userId", -1L)
                            val token = jsonResponse.optString("token", "")
                            val respEmail = jsonResponse.optString("email", email)
                            val respUsername = jsonResponse.optString("username", "")
                            if (userId <= 0 || token.isBlank()) {
                                val errorMsg = jsonResponse.optString("message", "邮箱或密码错误")
                                continuation.resume(Result.failure(Exception(errorMsg)))
                                return
                            }
                            val user = CloudBaseUser(
                                userId = userId.toString(),
                                username = respUsername,
                                email = respEmail,
                                token = token,
                                createdAt = System.currentTimeMillis()
                            )
                            continuation.resume(Result.success(user))
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
    
    // 下面这些旧的 CloudBase 功能（getUserInfo / updateUserInfo / uploadAvatar）
    // 迁移到 Java 后端时需要对应新增接口；当前版本先保留最小闭环：注册/登录。
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


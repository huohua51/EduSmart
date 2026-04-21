package com.edusmart.app.service

import com.edusmart.app.config.SDKConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import kotlin.coroutines.resume

class CloudBaseService {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val apiBaseUrl: String
        get() = SDKConfig.SERVER_API_BASE_URL

    suspend fun register(
        email: String,
        password: String,
        username: String
    ): Result<CloudBaseUser> = suspendCancellableCoroutine { continuation ->
        val body = JSONObject().apply {
            put("email", email)
            put("password", password)
            put("username", username)
        }
        val request = Request.Builder()
            .url("$apiBaseUrl/auth/register")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                continuation.resume(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(parseUserResponse(response, fallbackEmail = email))
            }
        })
    }

    suspend fun login(email: String, password: String): Result<CloudBaseUser> =
        suspendCancellableCoroutine { continuation ->
            val body = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
            val request = Request.Builder()
                .url("$apiBaseUrl/auth/login")
                .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: java.io.IOException) {
                    continuation.resume(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(parseUserResponse(response, fallbackEmail = email))
                }
            })
        }

    suspend fun getUserInfo(userId: String, token: String): Result<CloudBaseUser> =
        suspendCancellableCoroutine { continuation ->
            val request = Request.Builder()
                .url("$apiBaseUrl/auth/me")
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: java.io.IOException) {
                    continuation.resume(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(
                        parseUserResponse(
                            response,
                            fallbackToken = token,
                            fallbackUserId = userId
                        )
                    )
                }
            })
        }

    suspend fun updateUserInfo(
        userId: String,
        token: String,
        username: String? = null,
        avatarUrl: String? = null
    ): Result<CloudBaseUser> = suspendCancellableCoroutine { continuation ->
        val body = JSONObject().apply {
            if (username != null) put("username", username)
            if (avatarUrl != null) put("avatarUrl", avatarUrl)
        }
        val request = Request.Builder()
            .url("$apiBaseUrl/auth/me")
            .addHeader("Authorization", "Bearer $token")
            .put(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                continuation.resume(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(
                    parseUserResponse(
                        response,
                        fallbackToken = token,
                        fallbackUserId = userId
                    )
                )
            }
        })
    }

    suspend fun uploadAvatar(
        userId: String,
        token: String,
        imageBytes: ByteArray,
        fileName: String? = null
    ): Result<String> = suspendCancellableCoroutine { continuation ->
        val imageBase64 = android.util.Base64.encodeToString(
            imageBytes,
            android.util.Base64.NO_WRAP
        )
        val body = JSONObject().apply {
            put("imageBase64", imageBase64)
            if (fileName != null) put("fileName", fileName)
        }
        val request = Request.Builder()
            .url("$apiBaseUrl/auth/avatar")
            .addHeader("Authorization", "Bearer $token")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                continuation.resume(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (!response.isSuccessful || responseBody == null) {
                    continuation.resume(Result.failure(Exception("上传失败: ${response.code} - $responseBody")))
                    return
                }
                try {
                    val root = JSONObject(responseBody)
                    if (root.optInt("code") == 0) {
                        val data = root.optJSONObject("data")
                        val url = data?.optString("url").orEmpty()
                        if (url.isNotBlank()) {
                            continuation.resume(Result.success(absUrl(url)))
                        } else {
                            continuation.resume(Result.failure(Exception("上传成功但未返回 URL")))
                        }
                    } else {
                        continuation.resume(
                            Result.failure(
                                Exception(root.optString("message", "上传失败"))
                            )
                        )
                    }
                } catch (e: Exception) {
                    continuation.resume(Result.failure(e))
                }
            }
        })
    }

    private fun parseUserResponse(
        response: Response,
        fallbackEmail: String = "",
        fallbackToken: String = "",
        fallbackUserId: String = ""
    ): Result<CloudBaseUser> {
        val body = response.body?.string()
        if (!response.isSuccessful || body == null) {
            val msg = "请求失败: ${response.code} - ${body ?: "无响应内容"}"
            android.util.Log.e(TAG, msg)
            return Result.failure(Exception(msg))
        }

        return try {
            val root = JSONObject(body)
            if (root.optInt("code", -1) != 0) {
                Result.failure(Exception(root.optString("message", "请求失败")))
            } else {
                val data = root.optJSONObject("data")
                    ?: return Result.failure(Exception("返回缺少 data"))
                val userId = data.optString("userId").ifBlank { fallbackUserId }
                if (userId.isBlank()) {
                    return Result.failure(Exception("服务器返回缺少 userId"))
                }
                Result.success(
                    CloudBaseUser(
                        userId = userId,
                        username = data.optString("username", ""),
                        email = data.optString("email", fallbackEmail),
                        avatarUrl = data.optString("avatarUrl")
                            .takeIf { !it.isNullOrBlank() }
                            ?.let { absUrl(it) },
                        token = data.optString("token", fallbackToken),
                        createdAt = data.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "解析响应失败，body=$body", e)
            Result.failure(e)
        }
    }

    private fun absUrl(url: String): String {
        if (url.isBlank()) return url
        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("data:")) {
            return url
        }
        val base = SDKConfig.SERVER_BASE_URL.trimEnd('/')
        return if (url.startsWith("/")) "$base$url" else "$base/$url"
    }

    companion object {
        private const val TAG = "CloudBaseService"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

data class CloudBaseUser(
    val userId: String,
    val username: String,
    val email: String,
    val avatarUrl: String? = null,
    val token: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

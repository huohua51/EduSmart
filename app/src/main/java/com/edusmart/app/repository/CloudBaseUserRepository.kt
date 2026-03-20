package com.edusmart.app.repository

import android.content.Context
import android.content.SharedPreferences
import com.edusmart.app.data.entity.UserEntity
import com.edusmart.app.service.CloudBaseService
import com.edusmart.app.service.CloudBaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 腾讯云开发用户仓库
 * 
 * 注意：腾讯云开发的用户认证需要通过云函数实现
 * 请参考 TENCENT_CLOUDBASE_SETUP.md 中的云函数配置
 */
class CloudBaseUserRepository(private val context: Context) : UserRepositoryInterface {
    private val cloudBaseService = CloudBaseService()
    // ✅ 统一使用 "auth" SharedPreferences，与 UI 层一致
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    
    companion object {
        // ✅ 统一键名，与 UI 层一致
        private const val KEY_ACCESS_TOKEN = "token"
        private const val KEY_USER_ID = "userId"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
    }
    
    /**
     * 保存用户信息到本地
     */
    private fun saveUserInfo(user: CloudBaseUser) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, user.token)
            putString(KEY_USER_ID, user.userId)
            putString(KEY_USERNAME, user.username)
            putString(KEY_EMAIL, user.email)
            apply()
        }
    }
    
    /**
     * 获取 Token
     */
    private fun getToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    
    /**
     * 清除用户信息
     */
    private fun clearUserInfo() {
        prefs.edit().apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USERNAME)
            remove(KEY_EMAIL)
            apply()
        }
    }
    
    /**
     * 获取当前登录用户（返回 CloudBaseUser）
     */
    suspend fun getCurrentCloudBaseUser(): CloudBaseUser? = withContext(Dispatchers.IO) {
        val token = getToken() ?: return@withContext null
        val userId = prefs.getString(KEY_USER_ID, null) ?: return@withContext null
        
        val result = cloudBaseService.getUserInfo(userId, token)
        if (result.isSuccess) {
            val user = result.getOrNull()
            user?.let { saveUserInfo(it) }
            user
        } else {
            // 如果获取失败，尝试从本地恢复基本信息
            val username = prefs.getString(KEY_USERNAME, null)
            val email = prefs.getString(KEY_EMAIL, null)
            if (username != null && email != null) {
                CloudBaseUser(
                    userId = userId,
                    username = username,
                    email = email,
                    token = token
                )
            } else {
                null
            }
        }
    }
    
    /**
     * 获取当前登录用户ID（用于兼容接口）
     */
    override fun getCurrentUserId(): String? = prefs.getString(KEY_USER_ID, null)
    
    /**
     * 检查用户是否已登录
     */
    override fun isUserLoggedIn(): Boolean = getToken() != null
    
    /**
     * 用户注册
     */
    override suspend fun register(
        email: String,
        password: String,
        username: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val result = cloudBaseService.register(email, password, username)
            if (result.isSuccess) {
                val user = result.getOrNull()
                if (user == null) {
                    return@withContext Result.failure(Exception("注册失败：服务器返回数据为空"))
                }
                saveUserInfo(user)
                
                Result.success(
                    UserEntity(
                        userId = user.userId,
                        username = user.username,
                        email = user.email,
                        avatarUrl = user.avatarUrl,
                        createdAt = user.createdAt,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("注册失败"))
            }
        } catch (e: Exception) {
            android.util.Log.e("CloudBaseUserRepository", "注册异常", e)
            Result.failure(e)
        }
    }
    
    /**
     * 用户登录
     */
    override suspend fun login(email: String, password: String): Result<UserEntity> =
        withContext(Dispatchers.IO) {
            try {
                val result = cloudBaseService.login(email, password)
                if (result.isSuccess) {
                    val user = result.getOrNull()
                    if (user == null) {
                        return@withContext Result.failure(Exception("登录失败：服务器返回数据为空"))
                    }
                    saveUserInfo(user)
                    
                    Result.success(
                        UserEntity(
                            userId = user.userId,
                            username = user.username,
                            email = user.email,
                            avatarUrl = user.avatarUrl,
                            createdAt = user.createdAt,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("登录失败"))
                }
            } catch (e: Exception) {
                android.util.Log.e("CloudBaseUserRepository", "登录异常", e)
                Result.failure(e)
            }
        }
    
    /**
     * 用户登出
     */
    override fun logout() {
        clearUserInfo()
    }
    
    /**
     * 获取用户信息
     */
    override suspend fun getUserInfo(userId: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val token = getToken() ?: return@withContext Result.failure(Exception("未登录"))
        
        val result = cloudBaseService.getUserInfo(userId, token)
        if (result.isSuccess) {
            val user = result.getOrNull()!!
            saveUserInfo(user)
            
            Result.success(
                UserEntity(
                    userId = user.userId,
                    username = user.username,
                    email = user.email,
                    avatarUrl = user.avatarUrl,
                    createdAt = user.createdAt,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("获取用户信息失败"))
        }
    }
    
    /**
     * 更新用户信息
     */
    override suspend fun updateUserInfo(
        userId: String,
        username: String?,
        avatarUrl: String?
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val token = getToken() ?: return@withContext Result.failure(Exception("未登录"))
        
        val result = cloudBaseService.updateUserInfo(userId, token, username, avatarUrl)
        if (result.isSuccess) {
            val user = result.getOrNull()!!
            saveUserInfo(user)
            
            Result.success(
                UserEntity(
                    userId = user.userId,
                    username = user.username,
                    email = user.email,
                    avatarUrl = user.avatarUrl,
                    createdAt = user.createdAt,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("更新用户信息失败"))
        }
    }
    
    /**
     * 上传头像
     */
    override suspend fun uploadAvatar(userId: String, imageBytes: ByteArray): Result<String> = 
        withContext(Dispatchers.IO) {
            val token = getToken() ?: return@withContext Result.failure(Exception("未登录"))
            
            val fileName = "avatar_${System.currentTimeMillis()}.jpg"
            val result = cloudBaseService.uploadAvatar(userId, token, imageBytes, fileName)
            if (result.isSuccess) {
                val avatarUrl = result.getOrNull()!!
                // 上传成功后，自动更新用户信息中的头像 URL
                updateUserInfo(userId, null, avatarUrl)
                Result.success(avatarUrl)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("上传头像失败"))
            }
        }
}


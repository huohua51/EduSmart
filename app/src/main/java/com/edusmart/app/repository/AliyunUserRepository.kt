package com.edusmart.app.repository

import android.content.Context
import com.edusmart.app.data.entity.UserEntity
import com.edusmart.app.service.AliyunService
import com.edusmart.app.service.CloudBaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 阿里云用户仓库实现
 */
class AliyunUserRepository(private val context: Context) : UserRepositoryInterface {
    private val aliyunService = AliyunService()
    private val sharedPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_TOKEN = "token"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
    }
    
    override fun getCurrentUserId(): String? {
        return sharedPrefs.getString(KEY_USER_ID, null)
    }
    
    override fun isUserLoggedIn(): Boolean {
        return getCurrentUserId() != null && getToken() != null
    }
    
    private fun getToken(): String? {
        return sharedPrefs.getString(KEY_TOKEN, null)
    }
    
    override suspend fun register(
        email: String,
        password: String,
        username: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val result = aliyunService.register(email, password, username)
        if (result.isSuccess) {
            val cloudUser = result.getOrNull()!!
            saveUserInfo(cloudUser)
            Result.success(convertToUserEntity(cloudUser))
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("注册失败"))
        }
    }
    
    override suspend fun login(email: String, password: String): Result<UserEntity> =
        withContext(Dispatchers.IO) {
            val result = aliyunService.login(email, password)
            if (result.isSuccess) {
                val cloudUser = result.getOrNull()!!
                saveUserInfo(cloudUser)
                Result.success(convertToUserEntity(cloudUser))
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("登录失败"))
            }
        }
    
    override fun logout() {
        sharedPrefs.edit().clear().apply()
    }
    
    override suspend fun getUserInfo(userId: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val token = getToken() ?: return@withContext Result.failure(Exception("未登录"))
        val email = sharedPrefs.getString(KEY_EMAIL, null)
        
        val result = aliyunService.getUserInfo(userId, token, email)
        if (result.isSuccess) {
            val cloudUser = result.getOrNull()!!
            saveUserInfo(cloudUser)
            Result.success(convertToUserEntity(cloudUser))
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("获取用户信息失败"))
        }
    }
    
    override suspend fun updateUserInfo(
        userId: String,
        username: String?,
        avatarUrl: String?
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val token = getToken() ?: return@withContext Result.failure(Exception("未登录"))
        
        val result = aliyunService.updateUserInfo(userId, token, username, avatarUrl)
        if (result.isSuccess) {
            val cloudUser = result.getOrNull()!!
            saveUserInfo(cloudUser)
            Result.success(convertToUserEntity(cloudUser))
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("更新用户信息失败"))
        }
    }
    
    override suspend fun uploadAvatar(userId: String, imageBytes: ByteArray): Result<String> =
        withContext(Dispatchers.IO) {
            val token = getToken() ?: return@withContext Result.failure(Exception("未登录"))
            
            val fileName = "avatars/${userId}_${System.currentTimeMillis()}.jpg"
            val result = aliyunService.uploadAvatar(userId, token, imageBytes, fileName)
            if (result.isSuccess) {
                val avatarUrl = result.getOrNull()
                if (avatarUrl != null) {
                    // 上传成功后，自动更新用户信息中的头像 URL
                    updateUserInfo(userId, null, avatarUrl)
                    Result.success(avatarUrl)
                } else {
                    Result.failure(Exception("上传头像成功但未返回URL"))
                }
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("上传头像失败"))
            }
        }
    
    private fun saveUserInfo(user: CloudBaseUser) {
        sharedPrefs.edit().apply {
            putString(KEY_USER_ID, user.userId)
            putString(KEY_TOKEN, user.token)
            putString(KEY_USERNAME, user.username)
            putString(KEY_EMAIL, user.email)
            apply()
        }
    }
    
    private fun convertToUserEntity(cloudUser: CloudBaseUser): UserEntity {
        return UserEntity(
            userId = cloudUser.userId,
            username = cloudUser.username,
            email = cloudUser.email,
            avatarUrl = cloudUser.avatarUrl,
            createdAt = cloudUser.createdAt,
            updatedAt = System.currentTimeMillis()
        )
    }
}


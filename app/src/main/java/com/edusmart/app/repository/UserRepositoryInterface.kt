package com.edusmart.app.repository

import com.edusmart.app.data.entity.UserEntity

/**
 * 用户仓库接口
 * 统一 Firebase 和腾讯云开发的接口
 */
interface UserRepositoryInterface {
    /**
     * 检查用户是否已登录
     */
    fun isUserLoggedIn(): Boolean
    
    /**
     * 获取当前用户ID
     */
    fun getCurrentUserId(): String?
    
    /**
     * 用户注册
     */
    suspend fun register(
        email: String,
        password: String,
        username: String
    ): Result<UserEntity>
    
    /**
     * 用户登录
     */
    suspend fun login(email: String, password: String): Result<UserEntity>
    
    /**
     * 用户登出
     */
    fun logout()
    
    /**
     * 获取用户信息
     */
    suspend fun getUserInfo(userId: String): Result<UserEntity>
    
    /**
     * 更新用户信息
     */
    suspend fun updateUserInfo(
        userId: String,
        username: String?,
        avatarUrl: String?
    ): Result<UserEntity>
    
    /**
     * 上传头像
     */
    suspend fun uploadAvatar(userId: String, imageBytes: ByteArray): Result<String>
}


package com.edusmart.app.data.entity

import com.google.gson.annotations.SerializedName

/**
 * 用户数据模型
 * 用于腾讯云开发数据库存储
 */
data class UserEntity(
    @SerializedName("userId")
    var userId: String = "",
    
    @SerializedName("username")
    var username: String = "",
    
    @SerializedName("email")
    var email: String = "",
    
    @SerializedName("avatarUrl")
    var avatarUrl: String? = null,
    
    @SerializedName("createdAt")
    var createdAt: Long = System.currentTimeMillis(),
    
    @SerializedName("updatedAt")
    var updatedAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", null, System.currentTimeMillis(), System.currentTimeMillis())
}


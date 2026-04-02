package com.edusmart.app.repository

import com.edusmart.app.data.entity.UserEntity
// Firebase 相关导入已注释，因为项目已切换到腾讯云开发
// import com.google.firebase.auth.FirebaseAuth
// import com.google.firebase.auth.FirebaseUser
// import com.google.firebase.firestore.FirebaseFirestore
// import com.google.firebase.storage.FirebaseStorage
// import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Firebase 用户仓库（已废弃）
 * 项目已切换到腾讯云开发，请使用 CloudBaseUserRepository
 * 
 * 注意：此文件保留仅用于参考，实际不再使用
 */
@Deprecated("已切换到腾讯云开发，请使用 CloudBaseUserRepository", ReplaceWith("CloudBaseUserRepository"))
class UserRepository {
    // Firebase 相关代码已注释，因为项目已切换到腾讯云开发
    // private val auth = FirebaseAuth.getInstance()
    // private val firestore = FirebaseFirestore.getInstance()
    // private val storage = FirebaseStorage.getInstance()
    
    /**
     * 获取当前登录用户
     */
    // fun getCurrentUser(): FirebaseUser? = auth.currentUser
    
    /**
     * 检查用户是否已登录
     */
    // fun isUserLoggedIn(): Boolean = auth.currentUser != null
    
    /**
     * 用户注册（已废弃）
     */
    suspend fun register(email: String, password: String, username: String): Result<UserEntity> {
        return Result.failure(Exception("UserRepository 已废弃，请使用 CloudBaseUserRepository"))
        /* Firebase 代码已注释
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("注册失败"))
            
            val user = UserEntity(
                userId = firebaseUser.uid,
                username = username,
                email = email,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(user)
                .await()
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
        */
    }
    
    /**
     * 用户登录（已废弃）
     */
    suspend fun login(email: String, password: String): Result<UserEntity> {
        return Result.failure(Exception("UserRepository 已废弃，请使用 CloudBaseUserRepository"))
        /* Firebase 代码已注释
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("登录失败"))
            
            val userDoc = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()
            
            if (userDoc.exists()) {
                val user = userDoc.toObject(UserEntity::class.java)
                    ?: return Result.failure(Exception("用户数据不存在"))
                Result.success(user)
            } else {
                val user = UserEntity(
                    userId = firebaseUser.uid,
                    username = firebaseUser.displayName ?: "用户${firebaseUser.uid.take(6)}",
                    email = email,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                firestore.collection("users")
                    .document(firebaseUser.uid)
                    .set(user)
                    .await()
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
        */
    }
    
    /**
     * 用户登出（已废弃）
     */
    fun logout() {
        // auth.signOut()
    }
    
    /**
     * 获取用户信息（已废弃）
     */
    suspend fun getUserInfo(userId: String): Result<UserEntity> {
        return Result.failure(Exception("UserRepository 已废弃，请使用 CloudBaseUserRepository"))
        /* Firebase 代码已注释
        return try {
            val userDoc = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            
            if (userDoc.exists()) {
                val user = userDoc.toObject(UserEntity::class.java)
                    ?: return Result.failure(Exception("用户数据不存在"))
                Result.success(user)
            } else {
                Result.failure(Exception("用户不存在"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
        */
    }
    
    /**
     * 更新用户信息（已废弃）
     */
    suspend fun updateUserInfo(userId: String, username: String?, avatarUrl: String?): Result<UserEntity> {
        return Result.failure(Exception("UserRepository 已废弃，请使用 CloudBaseUserRepository"))
        /* Firebase 代码已注释
        return try {
            val updates = hashMapOf<String, Any>(
                "updatedAt" to System.currentTimeMillis()
            )
            
            username?.let { updates["username"] = it }
            avatarUrl?.let { updates["avatarUrl"] = it }
            
            firestore.collection("users")
                .document(userId)
                .update(updates)
                .await()
            
            getUserInfo(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
        */
    }
    
    /**
     * 上传头像（已废弃）
     */
    suspend fun uploadAvatar(userId: String, imageBytes: ByteArray): Result<String> {
        return Result.failure(Exception("UserRepository 已废弃，请使用 CloudBaseUserRepository"))
        /* Firebase 代码已注释
        return try {
            val fileName = "avatars/${userId}_${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child(fileName)
            
            val uploadTask = ref.putBytes(imageBytes).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()
            
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
        */
    }
}


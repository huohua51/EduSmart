package com.edusmart.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * 本地头像管理器
 * 负责头像的本地存储和读取，优先使用本地缓存
 */
object LocalAvatarManager {
    
    private const val AVATAR_CACHE_DIR = "avatars"
    private const val PREFS_NAME = "avatar_cache"
    private const val KEY_AVATAR_PREFIX = "avatar_"
    
    /**
     * 保存头像到本地
     * @param context 上下文
     * @param userId 用户ID
     * @param imageBytes 图片字节数组
     * @return 本地文件路径
     */
    fun saveAvatarLocally(context: Context, userId: String, imageBytes: ByteArray): String? {
        return try {
            // 方式1：保存为文件
            val avatarDir = File(context.filesDir, AVATAR_CACHE_DIR)
            if (!avatarDir.exists()) {
                avatarDir.mkdirs()
            }
            
            val avatarFile = File(avatarDir, "$userId.jpg")
            avatarFile.writeBytes(imageBytes)
            
            val filePath = avatarFile.absolutePath
            android.util.Log.d("LocalAvatarManager", "✅ 头像已保存到本地: $filePath")
            
            // 同时保存Base64到SharedPreferences作为备份
            val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString("$KEY_AVATAR_PREFIX$userId", base64)
                .putLong("${KEY_AVATAR_PREFIX}${userId}_timestamp", System.currentTimeMillis())
                .apply()
            
            filePath
        } catch (e: Exception) {
            android.util.Log.e("LocalAvatarManager", "保存头像到本地失败", e)
            null
        }
    }
    
    /**
     * 从本地读取头像
     * @param context 上下文
     * @param userId 用户ID
     * @return 图片字节数组，如果不存在返回null
     */
    fun loadAvatarLocally(context: Context, userId: String): ByteArray? {
        return try {
            // 方式1：从文件读取
            val avatarDir = File(context.filesDir, AVATAR_CACHE_DIR)
            val avatarFile = File(avatarDir, "$userId.jpg")
            
            if (avatarFile.exists()) {
                val bytes = avatarFile.readBytes()
                android.util.Log.d("LocalAvatarManager", "✅ 从本地文件加载头像: ${avatarFile.absolutePath}")
                return bytes
            }
            
            // 方式2：从SharedPreferences读取（备份）
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val base64 = prefs.getString("$KEY_AVATAR_PREFIX$userId", null)
            
            if (base64 != null) {
                val bytes = Base64.decode(base64, Base64.NO_WRAP)
                android.util.Log.d("LocalAvatarManager", "✅ 从SharedPreferences加载头像")
                return bytes
            }
            
            android.util.Log.d("LocalAvatarManager", "⚠️ 本地没有头像缓存")
            null
        } catch (e: Exception) {
            android.util.Log.e("LocalAvatarManager", "从本地加载头像失败", e)
            null
        }
    }
    
    /**
     * 获取本地头像文件路径（用于显示）
     * @param context 上下文
     * @param userId 用户ID
     * @return 本地文件路径，Coil可以直接加载
     */
    fun getLocalAvatarUri(context: Context, userId: String): String? {
        val avatarDir = File(context.filesDir, AVATAR_CACHE_DIR)
        val avatarFile = File(avatarDir, "$userId.jpg")
        
        return if (avatarFile.exists()) {
            val path = avatarFile.absolutePath
            android.util.Log.d("LocalAvatarManager", "✅ 返回本地文件路径: $path")
            path
        } else {
            android.util.Log.d("LocalAvatarManager", "⚠️ 本地头像文件不存在")
            null
        }
    }
    
    /**
     * 清除本地头像缓存
     * @param context 上下文
     * @param userId 用户ID
     */
    fun clearLocalAvatar(context: Context, userId: String) {
        try {
            // 删除文件
            val avatarDir = File(context.filesDir, AVATAR_CACHE_DIR)
            val avatarFile = File(avatarDir, "$userId.jpg")
            if (avatarFile.exists()) {
                avatarFile.delete()
            }
            
            // 删除SharedPreferences中的数据
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .remove("$KEY_AVATAR_PREFIX$userId")
                .remove("${KEY_AVATAR_PREFIX}${userId}_timestamp")
                .apply()
            
            android.util.Log.d("LocalAvatarManager", "✅ 已清除本地头像缓存")
        } catch (e: Exception) {
            android.util.Log.e("LocalAvatarManager", "清除本地头像缓存失败", e)
        }
    }
    
    /**
     * 检查本地是否有头像缓存
     * @param context 上下文
     * @param userId 用户ID
     * @return 是否存在缓存
     */
    fun hasLocalAvatar(context: Context, userId: String): Boolean {
        val avatarDir = File(context.filesDir, AVATAR_CACHE_DIR)
        val avatarFile = File(avatarDir, "$userId.jpg")
        
        if (avatarFile.exists()) {
            return true
        }
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains("$KEY_AVATAR_PREFIX$userId")
    }
}


package com.edusmart.app.feature.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.edusmart.app.repository.AliyunUserRepository
import com.edusmart.app.repository.CloudBaseUserRepository

/**
 * AuthViewModel 工厂类
 * 支持腾讯云开发和阿里云两种实现
 */
class AuthViewModelFactory(
    private val context: Context? = null,
    private val useCloudBase: Boolean = true,  // 默认使用腾讯云开发
    private val useAliyun: Boolean = false
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            useAliyun && context != null -> {
                AuthViewModel(
                    userRepository = AliyunUserRepository(context)
                ) as T
            }
            useCloudBase && context != null -> {
                AuthViewModel(
                    userRepository = CloudBaseUserRepository(context)
                ) as T
            }
            context != null -> {
                // 默认使用腾讯云开发
                AuthViewModel(context) as T
            }
            else -> {
                throw IllegalArgumentException("Context is required to create AuthViewModel")
            }
        }
    }
}


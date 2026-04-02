package com.edusmart.app.feature.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edusmart.app.data.entity.UserEntity
import com.edusmart.app.repository.CloudBaseUserRepository
import com.edusmart.app.repository.UserRepositoryInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val currentUser: UserEntity? = null,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val userRepository: UserRepositoryInterface
) : ViewModel() {
    
    constructor(context: Context) : this(CloudBaseUserRepository(context))
    
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    init {
        checkAuthStatus()
    }
    
    /**
     * 检查认证状态
     */
    private fun checkAuthStatus() {
        val isLoggedIn = userRepository.isUserLoggedIn()
        _authState.value = _authState.value.copy(
            isAuthenticated = isLoggedIn
        )
        
        if (isLoggedIn) {
            loadUserInfo()
        }
    }
    
    /**
     * 加载用户信息
     */
    private fun loadUserInfo() {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserId()
            
            if (userId != null) {
                val result = userRepository.getUserInfo(userId)
                if (result.isSuccess) {
                    _authState.value = _authState.value.copy(
                        currentUser = result.getOrNull(),
                        isAuthenticated = true
                    )
                } else {
                    _authState.value = _authState.value.copy(
                        errorMessage = result.exceptionOrNull()?.message
                    )
                }
            }
        }
    }
    
    /**
     * 用户注册
     */
    fun register(email: String, password: String, username: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(
                isLoading = true,
                errorMessage = null
            )
            
            val result = userRepository.register(email, password, username)
            if (result.isSuccess) {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    currentUser = result.getOrNull()
                )
            } else {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "注册失败"
                )
            }
        }
    }
    
    /**
     * 用户登录
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(
                isLoading = true,
                errorMessage = null
            )
            
            val result = userRepository.login(email, password)
            if (result.isSuccess) {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    currentUser = result.getOrNull()
                )
            } else {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "登录失败"
                )
            }
        }
    }
    
    /**
     * 用户登出
     */
    fun logout() {
        userRepository.logout()
        _authState.value = AuthState()
    }
    
    /**
     * 清除错误消息
     */
    fun clearError() {
        _authState.value = _authState.value.copy(errorMessage = null)
    }
    
    /**
     * 更新用户信息
     */
    fun updateUserInfo(username: String?, avatarUrl: String?) {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserId()
            if (userId == null) {
                _authState.value = _authState.value.copy(
                    errorMessage = "未登录"
                )
                return@launch
            }
            
            _authState.value = _authState.value.copy(
                isLoading = true,
                errorMessage = null
            )
            
            val result = userRepository.updateUserInfo(userId, username, avatarUrl)
            if (result.isSuccess) {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    currentUser = result.getOrNull()
                )
            } else {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "更新失败"
                )
            }
        }
    }
    
    /**
     * 上传头像
     */
    suspend fun uploadAvatar(userId: String, imageBytes: ByteArray): Result<String> {
        return userRepository.uploadAvatar(userId, imageBytes)
    }
}


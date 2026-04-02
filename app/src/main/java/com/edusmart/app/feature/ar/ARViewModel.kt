package com.edusmart.app.feature.ar

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.edusmart.app.feature.ar.model.ModelItem

/**
 * AR ViewModel - 管理AR场景状态和数据
 */
class ARViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // UI状态
    private val _uiState = MutableLiveData(ARUIState())
    val uiState: LiveData<ARUIState> = _uiState

    // 模型列表
    private val _models = MutableLiveData(emptyList<ModelItem>())
    val models: LiveData<List<ModelItem>> = _models

    // 当前选中的模型
    private val _selectedModel = MutableLiveData<ModelItem?>(null)
    val selectedModel: LiveData<ModelItem?> = _selectedModel

    // 是否正在放置模型
    private val _isPlacingModel = MutableLiveData(false)
    val isPlacingModel: LiveData<Boolean> = _isPlacingModel

    init {
        // 初始化加载模型
        loadModels()
        checkARSupport()
    }

    /**
     * 加载模型列表
     */
    private fun loadModels() {
        viewModelScope.launch {
            try {
                updateLoading(true, "加载模型中...")

                // 使用本地默认模型
                val defaultModels = ARConfig.getDefaultModels()

                // 过滤支持的格式
                val availableModels = defaultModels

                _models.value = availableModels
                updateLoading(false)

            } catch (e: Exception) {
                _uiState.value = _uiState.value?.copy(error = "加载模型失败: ${e.message}")
                updateLoading(false)
            }
        }
    }

    /**
     * 检查AR支持
     */
    private fun checkARSupport() {
        val isSupported = ARConfig.isDeviceSupported(context)
        val status = ARConfig.getARCoreStatus(context)

        _uiState.value = _uiState.value?.copy(
            isARSupported = isSupported,
            arStatus = status,
            message = if (isSupported) "AR功能可用" else "设备不支持AR"
        )
    }

    /**
     * 选择模型
     */
    fun selectModel(model: ModelItem) {
        _selectedModel.value = model
        _isPlacingModel.value = true  // 进入放置模式
        _uiState.value = _uiState.value?.copy(
            mode = ARMode.MODEL_SELECTED,
            message = "已选择: ${model.name} - 点击平面放置模型"
        )
    }

    /**
     * 取消选择模型
     */
    fun clearSelection() {
        _selectedModel.value = null
        _isPlacingModel.value = false  // 退出放置模式
        _uiState.value = _uiState.value?.copy(
            mode = ARMode.IDLE,
            message = "请选择模型"
        )
    }

    /**
     * 模型放置完成
     */
    fun modelPlaced() {
        _isPlacingModel.value = false
        _uiState.value = _uiState.value?.copy(
            mode = ARMode.MODEL_PLACED,
            message = "模型放置成功"
        )
    }

    /**
     * 切换平面检测
     */
    fun togglePlaneDetection(enabled: Boolean) {
        _uiState.value = _uiState.value?.copy(
            planeDetectionEnabled = enabled,
            message = if (enabled) "平面检测已开启" else "平面检测已关闭"
        )
    }

    /**
     * 重置场景
     */
    fun resetScene() {
        clearSelection()

        _uiState.value = ARUIState()

        // 重新检查AR支持
        checkARSupport()
    }

    /**
     * 更新加载状态
     */
    private fun updateLoading(isLoading: Boolean, message: String = "") {
        _uiState.value = _uiState.value?.copy(
            isLoading = isLoading,
            loadingMessage = message
        )
    }

    /**
     * 设置错误信息
     */
    fun setError(message: String) {
        _uiState.value = _uiState.value?.copy(error = message, mode = ARMode.ERROR)
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.value = _uiState.value?.copy(error = null)
    }

    /**
     * 更新手势状态
     */
    fun updateGestureState(gesture: String) {
        _uiState.value = _uiState.value?.copy(
            activeGesture = gesture,
            message = "手势: $gesture"
        )
    }
}

/**
 * AR UI状态
 */
data class ARUIState(
    val mode: ARMode = ARMode.IDLE,
    val isARSupported: Boolean = false,
    val arStatus: String = "UNKNOWN",
    val planeDetectionEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val placedModelCount: Int = 0,
    val activeGesture: String? = null,
    val message: String? = null,
    val error: String? = null
)

/**
 * AR模式枚举
 */
enum class ARMode {
    IDLE,
    SCANNING,
    MODEL_SELECTED,
    MODEL_PLACED,
    INTERACTING,
    ERROR
}

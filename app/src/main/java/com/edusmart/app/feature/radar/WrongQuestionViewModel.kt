package com.edusmart.app.feature.radar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edusmart.app.data.entity.WrongQuestionEntity
import com.edusmart.app.repository.WrongQuestionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WrongQuestionViewModel(
    private val repository: WrongQuestionRepository
) : ViewModel() {

    private val _wrongQuestions = MutableStateFlow<List<WrongQuestionEntity>>(emptyList())
    val wrongQuestions: StateFlow<List<WrongQuestionEntity>> = _wrongQuestions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadWrongQuestions()
    }

    private fun loadWrongQuestions() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAllWrongQuestions().collect { questions ->
                _wrongQuestions.value = questions
                _isLoading.value = false
            }
        }
    }

    // ☁️ 从云端加载错题
    fun loadWrongQuestionsFromCloud(userId: String, token: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                android.util.Log.d("WrongQuestionViewModel", "🌐 开始从云端加载错题...")
                
                if (userId.isEmpty() || token.isEmpty()) {
                    android.util.Log.e("WrongQuestionViewModel", "❌ userId或token为空")
                    _isLoading.value = false
                    return@launch
                }
                
                val questions = repository.getAllWrongQuestionsFromCloud(userId, token)
                _wrongQuestions.value = questions
                android.util.Log.d("WrongQuestionViewModel", "✅ 成功加载 ${questions.size} 道错题")
            } catch (e: Exception) {
                android.util.Log.e("WrongQuestionViewModel", "❌ 加载错题失败: ${e.message}")
                _wrongQuestions.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ☁️ 删除云端错题
    fun deleteWrongQuestionFromCloud(userId: String, token: String, wrongQuestion: WrongQuestionEntity) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                android.util.Log.d("WrongQuestionViewModel", "🗑️  删除错题: ${wrongQuestion.id}")
                repository.deleteWrongQuestionInCloud(userId, token, wrongQuestion)
                // 删除成功后重新加载列表
                loadWrongQuestionsFromCloud(userId, token)
            } catch (e: Exception) {
                android.util.Log.e("WrongQuestionViewModel", "❌ 删除失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteWrongQuestion(wrongQuestion: WrongQuestionEntity) {
        viewModelScope.launch {
            repository.deleteWrongQuestion(wrongQuestion)
        }
    }

    fun markAsReviewed(userId: String, token: String, wrongQuestion: WrongQuestionEntity) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                android.util.Log.d("WrongQuestionViewModel", "✅ 标记已复习: ${wrongQuestion.id}")

                val updated = wrongQuestion.copy(
                    reviewCount = wrongQuestion.reviewCount + 1,
                    lastReviewTime = System.currentTimeMillis(),
                    nextReviewTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000
                )

                // 更新到云端
                repository.updateWrongQuestionInCloud(userId, token, updated)

                // 重新加载列表以显示最新数据
                loadWrongQuestionsFromCloud(userId, token)
            } catch (e: Exception) {
                android.util.Log.e("WrongQuestionViewModel", "❌ 标记复习失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}

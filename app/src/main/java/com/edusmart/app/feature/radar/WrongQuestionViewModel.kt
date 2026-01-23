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

    fun deleteWrongQuestion(wrongQuestion: WrongQuestionEntity) {
        viewModelScope.launch {
            repository.deleteWrongQuestion(wrongQuestion)
        }
    }

    fun markAsReviewed(wrongQuestion: WrongQuestionEntity) {
        viewModelScope.launch {
            val updated = wrongQuestion.copy(
                reviewCount = wrongQuestion.reviewCount + 1,
                lastReviewTime = System.currentTimeMillis(),
                nextReviewTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000
            )
            repository.updateWrongQuestion(updated)
        }
    }
}

package com.edusmart.app.service

/**
 * 题目分析结果
 */
data class QuestionAnalysisResult(
    val success: Boolean,
    val questionText: String,
    val answer: String = "",
    val steps: List<String> = emptyList(),
    val knowledgePoints: List<String> = emptyList(),
    val analysis: String = "",
    val errorMessage: String? = null
)

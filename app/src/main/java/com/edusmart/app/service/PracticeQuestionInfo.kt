package com.edusmart.app.service

/**
 * 练习题信息（支持选择题和填空题）
 */
data class PracticeQuestionInfo(
    val questionText: String,
    val questionType: String, // "choice" 或 "fill"
    val options: List<String>, // 选择题的选项，填空题为空
    val correctAnswer: String,
    val steps: List<String>,
    val knowledgePoints: List<String>
)

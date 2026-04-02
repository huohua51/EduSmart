package com.edusmart.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "practice_records")
data class PracticeRecordEntity(
    @PrimaryKey val id: String,
    val questionText: String,
    val questionType: String, // "choice" 或 "fill"
    val correctAnswer: String,
    val userAnswer: String,
    val isCorrect: Boolean,
    val options: String?, // JSON格式，选择题的选项
    val steps: String, // JSON格式，解题步骤
    val knowledgePoints: String, // JSON格式，知识点列表
    val sourceQuestionId: String?, // 来源错题ID
    val createdAt: Long = System.currentTimeMillis()
)

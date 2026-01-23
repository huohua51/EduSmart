package com.edusmart.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wrong_questions")
data class WrongQuestionEntity(
    @PrimaryKey val id: String,
    val questionText: String,
    val answer: String,
    val steps: String, // JSON格式存储步骤列表
    val knowledgePoints: String, // JSON格式存储知识点列表
    val analysis: String,
    val imagePath: String? = null,
    val userAnswer: String? = null,
    val wrongReason: String? = null,
    val reviewCount: Int = 0,
    val lastReviewTime: Long? = null,
    val nextReviewTime: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)


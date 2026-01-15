package com.edusmart.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wrong_questions")
data class WrongQuestionEntity(
    @PrimaryKey val id: String,
    val questionId: String,
    val userAnswer: String? = null,
    val wrongReason: String? = null,
    val reviewCount: Int = 0,
    val lastReviewTime: Long? = null,
    val nextReviewTime: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)


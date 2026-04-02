package com.edusmart.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val id: String,
    val content: String,
    val subject: String, // 数学、物理、化学等
    val chapter: String, // 章节
    val knowledgePoints: List<String>, // 知识点列表
    val answer: String,
    val solution: String,
    val difficulty: Int, // 1-5
    val imageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)


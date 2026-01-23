package com.edusmart.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test_records")
data class TestRecordEntity(
    @PrimaryKey val id: String,
    val subject: String,
    val chapter: String? = null,
    val score: Float, // 得分率 0-1
    val totalQuestions: Int,
    val correctQuestions: Int,
    val wrongKnowledgePoints: List<String>? = null, // 错误知识点ID列表
    val testTime: Long = System.currentTimeMillis()
)


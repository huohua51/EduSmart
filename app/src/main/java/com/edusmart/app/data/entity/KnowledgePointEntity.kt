package com.edusmart.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_points")
data class KnowledgePointEntity(
    @PrimaryKey val id: String,
    val name: String,
    val subject: String,
    val chapter: String,
    val mastery: Float = 0f, // 掌握度 0-1
    val relatedPoints: List<String>? = null, // 关联知识点ID列表
    val lastTestTime: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)


package com.edusmart.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subject: String,
    val content: String, // 结构化笔记内容（JSON格式）
    val images: List<String>? = null, // 图片路径列表
    val audioPath: String? = null, // 录音文件路径
    val transcript: String? = null, // 语音转写文本
    val knowledgePoints: List<String>? = null, // 提取的知识点
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)


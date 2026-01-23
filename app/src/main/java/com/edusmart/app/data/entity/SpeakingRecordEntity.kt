package com.edusmart.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speaking_records")
data class SpeakingRecordEntity(
    @PrimaryKey val id: String,
    val scene: String, // 场景：airport, interview, restaurant等
    val userAudio: String, // 用户录音文件路径
    val transcript: String, // 转写文本
    val score: Float? = null, // 发音评分 0-100
    val suggestions: List<String>? = null, // AI建议
    val duration: Long, // 对话时长（秒）
    val createdAt: Long = System.currentTimeMillis()
)


package com.edusmart.app.repository

import com.edusmart.app.data.dao.SpeakingRecordDao
import com.edusmart.app.data.entity.SpeakingRecordEntity
import com.edusmart.app.service.AIService
import com.edusmart.app.service.SpeechService
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class SpeakingRepository(
    private val speakingRecordDao: SpeakingRecordDao,
    private val speechService: SpeechService,
    private val aiService: AIService
) {
    
    fun getAllSpeakingRecords(): Flow<List<SpeakingRecordEntity>> {
        return speakingRecordDao.getAllSpeakingRecords()
    }
    
    fun getSpeakingRecordsByScene(scene: String): Flow<List<SpeakingRecordEntity>> {
        return speakingRecordDao.getSpeakingRecordsByScene(scene)
    }
    
    /**
     * 保存口语练习记录
     */
    suspend fun saveSpeakingRecord(
        scene: String,
        audioPath: String,
        transcript: String,
        score: Float?,
        suggestions: List<String>?,
        duration: Long
    ) {
        val record = SpeakingRecordEntity(
            id = UUID.randomUUID().toString(),
            scene = scene,
            userAudio = audioPath,
            transcript = transcript,
            score = score,
            suggestions = suggestions,
            duration = duration
        )
        speakingRecordDao.insertSpeakingRecord(record)
    }
    
    /**
     * 评分发音
     */
    suspend fun scorePronunciation(
        audioPath: String,
        referenceText: String
    ): Float {
        return speechService.scorePronunciation(audioPath, referenceText)
    }
    
    /**
     * 获取发音建议
     */
    suspend fun getPronunciationSuggestions(
        userText: String,
        referenceText: String
    ): List<String> {
        return aiService.getPronunciationSuggestions(userText, referenceText)
    }
    
    /**
     * 生成对话回复
     */
    suspend fun generateDialogueResponse(
        scene: String,
        userInput: String
    ): String {
        return aiService.generateDialogue(scene, userInput)
    }
}


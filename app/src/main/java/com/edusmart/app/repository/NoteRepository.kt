package com.edusmart.app.repository

import com.edusmart.app.data.dao.NoteDao
import com.edusmart.app.data.entity.NoteEntity
import com.edusmart.app.service.AIService
import com.edusmart.app.service.OCRService
import com.edusmart.app.service.SpeechService
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class NoteRepository(
    private val noteDao: NoteDao,
    private val ocrService: OCRService,
    private val speechService: SpeechService,
    private val aiService: AIService
) {
    
    fun getAllNotes(): Flow<List<NoteEntity>> {
        return noteDao.getAllNotes()
    }
    
    fun getNotesBySubject(subject: String): Flow<List<NoteEntity>> {
        return noteDao.getNotesBySubject(subject)
    }
    
    suspend fun getNoteById(id: String): NoteEntity? {
        return noteDao.getNoteById(id)
    }
    
    /**
     * 识别黑板图片
     */
    suspend fun recognizeBlackboard(imagePath: String): String {
        return ocrService.recognizeText(imagePath)
    }
    
    /**
     * 转写录音
     */
    suspend fun transcribeAudio(audioPath: String): String {
        return speechService.transcribe(audioPath)
    }
    
    /**
     * 合并笔记（图片OCR + 语音转写）
     */
    suspend fun mergeNote(
        title: String,
        subject: String,
        imagePaths: List<String>?,
        audioPath: String?
    ): NoteEntity {
        val imageTexts = imagePaths?.map { ocrService.recognizeText(it) } ?: emptyList()
        val audioText = audioPath?.let { speechService.transcribe(it) } ?: ""
        
        val mergedContent = buildString {
            imageTexts.forEach { append(it).append("\n\n") }
            if (audioText.isNotEmpty()) {
                append("【语音转写】\n").append(audioText)
            }
        }
        
        // AI提取知识点
        val knowledgePoints = aiService.extractKnowledgePoints(mergedContent)
        
        val note = NoteEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            subject = subject,
            content = mergedContent,
            images = imagePaths,
            audioPath = audioPath,
            transcript = audioText,
            knowledgePoints = knowledgePoints
        )
        
        noteDao.insertNote(note)
        return note
    }
    
    suspend fun updateNote(note: NoteEntity) {
        noteDao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
    }
    
    suspend fun deleteNote(note: NoteEntity) {
        noteDao.deleteNote(note)
    }
}


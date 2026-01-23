<<<<<<< Updated upstream
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

=======
package com.edusmart.app.repository

import android.util.Log
import com.edusmart.app.data.dao.NoteDao
import com.edusmart.app.data.entity.NoteEntity
import com.edusmart.app.service.AIService
import com.edusmart.app.service.NoteAIService
import com.edusmart.app.service.NoteSummary
import com.edusmart.app.service.OCRService
import com.edusmart.app.service.SpeechService
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class NoteRepository(
    private val noteDao: NoteDao,
    private val ocrService: OCRService,
    private val speechService: SpeechService,
    private val aiService: AIService,
    private val noteAIService: NoteAIService = NoteAIService()
) {
    
    fun getAllNotes(): Flow<List<NoteEntity>> {
        return noteDao.getAllNotes().also {
            Log.d("NoteRepository", "开始收集笔记列表 Flow")
        }
    }
    
    fun getNotesBySubject(subject: String): Flow<List<NoteEntity>> {
        return noteDao.getNotesBySubject(subject)
    }
    
    suspend fun getNoteById(id: String): NoteEntity? {
        return noteDao.getNoteById(id)
    }
    
    suspend fun searchNotes(keyword: String): List<NoteEntity> {
        return noteDao.searchNotes("%$keyword%")
    }
    
    suspend fun getAllSubjects(): List<String> {
        return noteDao.getAllSubjects()
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
        
        // AI提取知识点（使用NoteAIService，自动生成3个知识点）
        val knowledgePoints = try {
            noteAIService.extractKnowledgePoints(mergedContent, subject)
        } catch (e: Exception) {
            android.util.Log.e("NoteRepository", "知识点提取失败，使用默认值", e)
            emptyList()
        }
        
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
    
    /**
     * AI润色笔记
     */
    suspend fun polishNote(noteId: String): String? {
        val note = noteDao.getNoteById(noteId) ?: return null
        return noteAIService.polishNote(note.content, note.subject)
    }
    
    /**
     * AI总结笔记
     */
    suspend fun summarizeNote(noteId: String): NoteSummary? {
        val note = noteDao.getNoteById(noteId) ?: return null
        return noteAIService.summarizeNote(note.content, note.subject)
    }
    
    /**
     * AI生成标题
     */
    suspend fun generateTitle(noteId: String): String? {
        val note = noteDao.getNoteById(noteId) ?: return null
        return noteAIService.generateTitle(note.content, note.subject)
    }
    
    /**
     * AI增强知识点提取
     */
    suspend fun enhanceKnowledgePoints(noteId: String): List<String>? {
        val note = noteDao.getNoteById(noteId) ?: return null
        return noteAIService.extractKnowledgePoints(note.content, note.subject)
    }
    
    /**
     * AI问答
     */
    suspend fun answerQuestion(noteId: String, question: String): String? {
        val note = noteDao.getNoteById(noteId) ?: return null
        return noteAIService.answerQuestion(note.content, question)
    }
    
    /**
     * 创建新笔记
     */
    suspend fun createNote(
        title: String,
        subject: String,
        content: String,
        imagePaths: List<String>? = null,
        audioPath: String? = null,
        transcript: String? = null
    ): NoteEntity {
        val note = NoteEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            subject = subject,
            content = content,
            images = imagePaths,
            audioPath = audioPath,
            transcript = transcript,
            // AI提取知识点（使用NoteAIService，自动生成3个知识点）
            knowledgePoints = try {
                noteAIService.extractKnowledgePoints(content, subject)
            } catch (e: Exception) {
                android.util.Log.e("NoteRepository", "知识点提取失败，使用默认值", e)
                emptyList()
            }
        )
        
        Log.d("NoteRepository", "开始保存笔记: id=${note.id}, title=${note.title}, subject=${note.subject}")
        
        // 确保保存到数据库
        noteDao.insertNote(note)
        Log.d("NoteRepository", "笔记已插入数据库: ${note.id}")
        
        // 验证保存是否成功（可选，用于调试）
        val savedNote = noteDao.getNoteById(note.id)
        if (savedNote == null) {
            Log.e("NoteRepository", "笔记保存验证失败: ${note.id}")
            throw Exception("笔记保存失败：数据库验证失败")
        }
        
        Log.d("NoteRepository", "笔记保存成功并已验证: id=${savedNote.id}, title=${savedNote.title}")
        return note
    }
}

>>>>>>> Stashed changes

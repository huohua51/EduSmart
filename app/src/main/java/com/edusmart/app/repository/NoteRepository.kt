package com.edusmart.app.repository

import android.util.Log
import com.edusmart.app.data.dao.NoteDao
import com.edusmart.app.data.entity.NoteEntity
import com.edusmart.app.service.AIService
import com.edusmart.app.service.CloudBaseNoteService
import com.edusmart.app.service.NoteAIService
import com.edusmart.app.service.NoteSummary
import com.edusmart.app.service.OCRService
import com.edusmart.app.service.SpeechService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

/**
 * 笔记仓库 - 云端优先方案
 * 所有数据操作都通过 CloudBaseNoteService
 * 本地 DAO 仅用作离线缓存（可选）
 */
class NoteRepository(
    private val noteDao: NoteDao,
    private val ocrService: OCRService,
    private val speechService: SpeechService,
    private val aiService: AIService,
    private val noteAIService: NoteAIService = NoteAIService(),
    private val cloudService: CloudBaseNoteService = CloudBaseNoteService()
) {
    
    /**
     * 从云端获取所有笔记
     */
    suspend fun getAllNotesFromCloud(userId: String, token: String): List<NoteEntity> {
        return cloudService.getNotes(userId, token)
            .onSuccess { notes ->
                Log.d("NoteRepository", "✅ 从云端获取笔记列表成功: ${notes.size} 条")
            }
            .onFailure { error ->
                Log.e("NoteRepository", "❌ 从云端获取笔记失败: ${error.message}")
            }
            .getOrElse { emptyList() }
    }
    
    /**
     * 按科目从云端获取笔记（云端过滤）
     */
    suspend fun getNotesBySubjectFromCloud(userId: String, token: String, subject: String): List<NoteEntity> {
        return getAllNotesFromCloud(userId, token)
            .filter { it.subject == subject }
    }
    
    /**
     * 从云端获取单个笔记
     */
    suspend fun getNoteByIdFromCloud(userId: String, token: String, id: String): NoteEntity? {
        return getAllNotesFromCloud(userId, token).find { it.id == id }
    }
    
    /**
     * 从云端搜索笔记
     */
    suspend fun searchNotesFromCloud(userId: String, token: String, keyword: String): List<NoteEntity> {
        return getAllNotesFromCloud(userId, token)
            .filter { note ->
                note.title.contains(keyword, ignoreCase = true) ||
                note.content.contains(keyword, ignoreCase = true) ||
                (note.subject?.contains(keyword, ignoreCase = true) ?: false)
            }
    }
    
    /**
     * 获取云端的所有科目
     */
    suspend fun getAllSubjectsFromCloud(userId: String, token: String): List<String> {
        return getAllNotesFromCloud(userId, token)
            .mapNotNull { it.subject }
            .distinct()
    }
    
    /**
     * 本地获取所有笔记（离线缓存用）
     * @deprecated 使用 getAllNotesFromCloud 代替
     */
    fun getAllNotes(): Flow<List<NoteEntity>> {
        return noteDao.getAllNotes().also {
            Log.d("NoteRepository", "⚠️ 使用本地缓存笔记列表")
        }
    }
    
    /**
     * 本地获取指定科目的笔记
     * @deprecated 使用 getNotesBySubjectFromCloud 代替
     */
    fun getNotesBySubject(subject: String): Flow<List<NoteEntity>> {
        return noteDao.getNotesBySubject(subject)
    }
    
    /**
     * 本地获取单个笔记
     * @deprecated 使用 getNoteByIdFromCloud 代替
     */
    suspend fun getNoteById(id: String): NoteEntity? {
        return noteDao.getNoteById(id)
    }
    
    /**
     * 本地搜索笔记
     * @deprecated 使用 searchNotesFromCloud 代替
     */
    suspend fun searchNotes(keyword: String): List<NoteEntity> {
        return noteDao.searchNotes("%$keyword%")
    }
    
    /**
     * 本地获取所有科目
     * @deprecated 使用 getAllSubjectsFromCloud 代替
     */
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
     * 合并笔记到云端（图片OCR + 语音转写）
     * 主要流程：本地处理 → 上传云端
     */
    suspend fun mergeNote(
        userId: String,
        token: String,
        title: String,
        subject: String,
        imagePaths: List<String>?,
        audioPath: String?
    ): NoteEntity {
        Log.d("NoteRepository", "🔄 开始处理笔记: title=$title")
        
        // 第 1 步：本地处理（OCR + 语音转写）
        val imageTexts = imagePaths?.map { imagePath ->
            Log.d("NoteRepository", "📸 OCR 处理图片: $imagePath")
            ocrService.recognizeText(imagePath)
        } ?: emptyList()
        
        val audioText = audioPath?.let { audioPath ->
            Log.d("NoteRepository", "🎙️ 语音转写: $audioPath")
            speechService.transcribe(audioPath)
        } ?: ""
        
        val mergedContent = buildString {
            imageTexts.forEach { append(it).append("\n\n") }
            if (audioText.isNotEmpty()) {
                append("【语音转写】\n").append(audioText)
            }
        }
        
        // 第 2 步：AI 提取知识点
        val knowledgePoints = try {
            Log.d("NoteRepository", "🤖 AI 提取知识点...")
            noteAIService.extractKnowledgePoints(mergedContent, subject)
        } catch (e: Exception) {
            Log.e("NoteRepository", "❌ 知识点提取失败，使用默认值", e)
            emptyList()
        }
        
        // 第 3 步：构造笔记对象
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
        
        // 第 4 步：上传到云端
        Log.d("NoteRepository", "☁️ 上传笔记到云端...")
        return cloudService.createNote(userId, token, note)
            .onSuccess { cloudNote ->
                Log.d("NoteRepository", "✅ 笔记已保存到云端: ${cloudNote.id}")
            }
            .onFailure { error ->
                Log.e("NoteRepository", "❌ 云端保存失败: ${error.message}")
            }
            .getOrThrow()  // 抛出异常，让调用者处理
    }
    
    /**
     * 更新笔记到云端
     */
    suspend fun updateNote(userId: String, token: String, note: NoteEntity): Unit {
        Log.d("NoteRepository", "🔄 更新笔记到云端: ${note.id}")
        cloudService.updateNote(userId, token, note.copy(updatedAt = System.currentTimeMillis()))
            .onSuccess {
                Log.d("NoteRepository", "✅ 笔记已更新: ${note.id}")
            }
            .onFailure { error ->
                Log.e("NoteRepository", "❌ 笔记更新失败: ${error.message}")
            }
            .getOrThrow()
    }
    
    /**
     * 删除笔记（从云端）
     */
    suspend fun deleteNote(userId: String, token: String, note: NoteEntity): Unit {
        Log.d("NoteRepository", "🗑️ 删除笔记: ${note.id}")
        return cloudService.deleteNote(userId, token, note.id)
            .onSuccess {
                Log.d("NoteRepository", "✅ 笔记已删除: ${note.id}")
            }
            .onFailure { error ->
                Log.e("NoteRepository", "❌ 笔记删除失败: ${error.message}")
            }
            .getOrThrow()
    }
    
    /**
     * AI 润色笔记（从云端获取）
     */
    suspend fun polishNote(userId: String, token: String, noteId: String): String? {
        Log.d("NoteRepository", "✨ AI 润色笔记: $noteId")
        val note = getNoteByIdFromCloud(userId, token, noteId) ?: return null
        return try {
            noteAIService.polishNote(note.content, note.subject)
        } catch (e: Exception) {
            Log.e("NoteRepository", "润色笔记失败", e)
            null
        }
    }
    
    /**
     * AI 总结笔记（从云端获取）
     */
    suspend fun summarizeNote(userId: String, token: String, noteId: String): NoteSummary? {
        Log.d("NoteRepository", "📝 AI 总结笔记: $noteId")
        val note = getNoteByIdFromCloud(userId, token, noteId) ?: return null
        return try {
            noteAIService.summarizeNote(note.content, note.subject)
        } catch (e: Exception) {
            Log.e("NoteRepository", "总结笔记失败", e)
            null
        }
    }
    
    /**
     * AI 生成标题（从云端获取）
     */
    suspend fun generateTitle(userId: String, token: String, noteId: String): String? {
        Log.d("NoteRepository", "📌 AI 生成标题: $noteId")
        val note = getNoteByIdFromCloud(userId, token, noteId) ?: return null
        return try {
            noteAIService.generateTitle(note.content, note.subject)
        } catch (e: Exception) {
            Log.e("NoteRepository", "生成标题失败", e)
            null
        }
    }
    
    /**
     * AI 增强知识点（从云端获取）
     */
    suspend fun enhanceKnowledgePoints(userId: String, token: String, noteId: String): List<String>? {
        Log.d("NoteRepository", "💡 AI 增强知识点: $noteId")
        val note = getNoteByIdFromCloud(userId, token, noteId) ?: return null
        return try {
            noteAIService.extractKnowledgePoints(note.content, note.subject)
        } catch (e: Exception) {
            Log.e("NoteRepository", "增强知识点失败", e)
            null
        }
    }
    
    /**
     * AI 问答（从云端获取）
     */
    suspend fun answerQuestion(userId: String, token: String, noteId: String, question: String): String? {
        Log.d("NoteRepository", "❓ AI 问答: $noteId, 问题: $question")
        val note = getNoteByIdFromCloud(userId, token, noteId) ?: return null
        return try {
            noteAIService.answerQuestion(note.content, question)
        } catch (e: Exception) {
            Log.e("NoteRepository", "AI问答失败", e)
            null
        }
    }
    
    /**
     * AI 生成科目
     * 根据笔记内容自动生成科目
     */
    suspend fun generateSubject(content: String, title: String? = null): String {
        Log.d("NoteRepository", "🤖 AI 生成科目...")
        return try {
            val generatedSubject = noteAIService.generateSubject(content, title)
            Log.d("NoteRepository", "✅ 科目生成成功: $generatedSubject")
            generatedSubject
        } catch (e: Exception) {
            Log.e("NoteRepository", "❌ 科目生成失败", e)
            "其他"
        }
    }
}


package com.edusmart.app.feature.note

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.edusmart.app.data.entity.NoteEntity
import com.edusmart.app.repository.NoteRepository
import com.edusmart.app.service.NoteSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class NoteViewModel(
    private val noteRepository: NoteRepository
) : ViewModel() {
    
    private val _notes = MutableStateFlow<List<NoteEntity>>(emptyList())
    val notes: StateFlow<List<NoteEntity>> = _notes.asStateFlow()
    
    private val _subjects = MutableStateFlow<List<String>>(emptyList())
    val subjects: StateFlow<List<String>> = _subjects.asStateFlow()
    
    private val _selectedSubject = MutableStateFlow<String?>(null)
    val selectedSubject: StateFlow<String?> = _selectedSubject.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        loadNotes()
        loadSubjects()
    }
    
    private var notesJob: kotlinx.coroutines.Job? = null
    
    /**
     * 从云端加载笔记列表（新方法）
     */
    fun loadNotesFromCloud(userId: String, token: String) {
        if (userId.isEmpty() || token.isEmpty()) {
            _errorMessage.value = "用户信息缺失，请先登录"
            return
        }
        
        notesJob?.cancel()
        notesJob = viewModelScope.launch {
            try {
                Log.d("NoteViewModel", "🌐 从云端加载笔记列表...")
                _isLoading.value = true
                val notesList = noteRepository.getAllNotesFromCloud(userId, token)
                _notes.value = notesList
                _errorMessage.value = null
                Log.d("NoteViewModel", "✅ 从云端加载笔记成功: ${notesList.size} 条")
            } catch (e: Exception) {
                Log.e("NoteViewModel", "❌ 从云端加载笔记失败", e)
                _errorMessage.value = "加载笔记失败: ${e.message}"
                _notes.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 按科目从云端加载笔记（新方法）
     */
    fun loadNotesBySubjectFromCloud(userId: String, token: String, subject: String) {
        if (userId.isEmpty() || token.isEmpty()) {
            _errorMessage.value = "用户信息缺失，请先登录"
            return
        }
        
        notesJob?.cancel()
        notesJob = viewModelScope.launch {
            try {
                Log.d("NoteViewModel", "🌐 从云端按科目加载笔记: $subject")
                _isLoading.value = true
                _selectedSubject.value = subject
                val notesList = noteRepository.getNotesBySubjectFromCloud(userId, token, subject)
                _notes.value = notesList
                _errorMessage.value = null
                Log.d("NoteViewModel", "✅ 加载成功: ${notesList.size} 条")
            } catch (e: Exception) {
                Log.e("NoteViewModel", "❌ 加载失败", e)
                _errorMessage.value = "加载笔记失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 从云端搜索笔记（新方法）
     */
    fun searchNotesFromCloud(userId: String, token: String, keyword: String) {
        if (userId.isEmpty() || token.isEmpty()) {
            _errorMessage.value = "用户信息缺失，请先登录"
            return
        }
        
        if (keyword.isEmpty()) {
            loadNotesFromCloud(userId, token)
            return
        }
        
        notesJob?.cancel()
        notesJob = viewModelScope.launch {
            try {
                Log.d("NoteViewModel", "🌐 从云端搜索笔记: $keyword")
                _isLoading.value = true
                val results = noteRepository.searchNotesFromCloud(userId, token, keyword)
                _notes.value = results
                _errorMessage.value = null
                Log.d("NoteViewModel", "✅ 搜索成功: ${results.size} 条")
            } catch (e: Exception) {
                Log.e("NoteViewModel", "❌ 搜索失败", e)
                _errorMessage.value = "搜索失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadNotes() {
        // 取消之前的任务，避免重复收集
        notesJob?.cancel()
        notesJob = viewModelScope.launch {
            var isFirstValue = true
            try {
                Log.d("NoteViewModel", "开始加载笔记列表")
                noteRepository.getAllNotes()
                    .onStart { 
                        _isLoading.value = true
                        Log.d("NoteViewModel", "Flow 开始收集")
                    }
                    .collect { notesList ->
                        Log.d("NoteViewModel", "收到笔记列表: ${notesList.size} 条笔记")
                        _notes.value = notesList
                        // 收到第一个值后立即清除加载状态
                        if (isFirstValue) {
                            _isLoading.value = false
                            isFirstValue = false
                            Log.d("NoteViewModel", "首次加载完成，已清除加载状态")
                        }
                    }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // 这是正常的取消操作（例如调用 loadNotes() 时取消之前的任务），不需要记录错误
                Log.d("NoteViewModel", "笔记加载任务被取消（这是正常的）")
                _isLoading.value = false
            } catch (e: Exception) {
                Log.e("NoteViewModel", "加载笔记失败", e)
                _isLoading.value = false
                _errorMessage.value = "加载笔记失败: ${e.message}"
            }
        }
    }
    
    fun loadNotesBySubject(subject: String) {
        viewModelScope.launch {
            var isFirstValue = true
            try {
                _selectedSubject.value = subject
                noteRepository.getNotesBySubject(subject)
                    .onStart { _isLoading.value = true }
                    .collect { notesList ->
                        _notes.value = notesList
                        // 收到第一个值后立即清除加载状态
                        if (isFirstValue) {
                            _isLoading.value = false
                            isFirstValue = false
                        }
                    }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "加载笔记失败: ${e.message}"
            }
        }
    }
    
    fun clearSubjectFilter() {
        _selectedSubject.value = null
        loadNotes()
    }
    
    fun loadSubjects() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                _subjects.value = noteRepository.getAllSubjects()
            } catch (e: Exception) {
                // 忽略错误
            }
        }
    }
    
    /**
     * 从云端加载科目列表（新方法）
     */
    fun loadSubjectsFromCloud(userId: String, token: String) {
        if (userId.isEmpty() || token.isEmpty()) {
            return
        }
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                Log.d("NoteViewModel", "🌐 从云端加载科目列表...")
                val subjectsList = noteRepository.getAllSubjectsFromCloud(userId, token)
                _subjects.value = subjectsList
                Log.d("NoteViewModel", "✅ 科目列表加载成功: ${subjectsList.size} 个科目")
            } catch (e: Exception) {
                Log.e("NoteViewModel", "❌ 加载科目列表失败", e)
                // 如果加载失败，至少保留一个默认科目
                if (_subjects.value.isEmpty()) {
                    _subjects.value = listOf("其他")
                }
            }
        }
    }
    
    fun searchNotes(keyword: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val results = noteRepository.searchNotes(keyword)
                _notes.value = results
            } catch (e: Exception) {
                _errorMessage.value = "搜索失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    suspend fun recognizeImage(imagePath: String): String {
        return try {
            noteRepository.recognizeBlackboard(imagePath)
        } catch (e: Exception) {
            _errorMessage.value = "OCR识别失败: ${e.message}"
            throw e
        }
    }
    
    suspend fun transcribeAudioNow(audioPath: String): String {
        return try {
            noteRepository.transcribeAudio(audioPath)
        } catch (e: Exception) {
            _errorMessage.value = "语音转写失败: ${e.message}"
            throw e
        }
    }
    
    /**
     * 创建笔记到云端（新方法）
     */
    suspend fun createNoteInCloud(
        userId: String,
        token: String,
        title: String,
        subject: String,
        imagePaths: List<String>? = null,
        audioPath: String? = null
    ): NoteEntity {
        return try {
            Log.d("NoteViewModel", "✍️ 创建笔记到云端: title=$title")
            _isLoading.value = true
            val note = noteRepository.mergeNote(
                userId, token, title, subject, imagePaths, audioPath
            )
            // 重新加载列表
            loadNotesFromCloud(userId, token)
            Log.d("NoteViewModel", "✅ 笔记创建成功: ${note.id}")
            note
        } catch (e: Exception) {
            Log.e("NoteViewModel", "❌ 创建失败", e)
            _errorMessage.value = "创建笔记失败: ${e.message}"
            throw e
        } finally {
            _isLoading.value = false
        }
    }
    
    suspend fun createNote(
        title: String,
        subject: String,
        content: String,
        imagePaths: List<String>? = null,
        audioPath: String? = null,
        transcript: String? = null
    ): NoteEntity {
        return try {
            Log.d("NoteViewModel", "开始创建笔记: title=$title, subject=$subject")
            // 使用本地方法创建笔记（旧方法，已废弃）
            throw UnsupportedOperationException("请使用 createNoteInCloud 方法")
        } catch (e: Exception) {
            Log.e("NoteViewModel", "创建笔记失败", e)
            _errorMessage.value = "创建笔记失败: ${e.message}"
            throw e
        }
    }
    
    suspend fun mergeNote(
        title: String,
        subject: String,
        imagePaths: List<String>?,
        audioPath: String?
    ): NoteEntity {
        return try {
            // 使用本地方法合并笔记（旧方法，已废弃）
            throw UnsupportedOperationException("请使用 createNoteInCloud 方法")
        } catch (e: Exception) {
            _errorMessage.value = "合并笔记失败: ${e.message}"
            throw e
        }
    }
    
    /**
     * 更新笔记到云端（新方法）
     */
    suspend fun updateNoteInCloud(userId: String, token: String, note: NoteEntity) {
        try {
            Log.d("NoteViewModel", "🔄 更新笔记到云端: ${note.id}")
            Log.d("NoteViewModel", "标题: ${note.title}")
            Log.d("NoteViewModel", "科目: ${note.subject}")
            Log.d("NoteViewModel", "内容长度: ${note.content.length}")
            Log.d("NoteViewModel", "userId: $userId")
            Log.d("NoteViewModel", "token: ${token.take(10)}...")
            _isLoading.value = true
            noteRepository.updateNote(userId, token, note)
            // 重新加载列表
            loadNotesFromCloud(userId, token)
            Log.d("NoteViewModel", "✅ 笔记已更新")
        } catch (e: Exception) {
            Log.e("NoteViewModel", "❌ 更新失败", e)
            Log.e("NoteViewModel", "错误类型: ${e.javaClass.simpleName}")
            Log.e("NoteViewModel", "错误消息: ${e.message}")
            e.printStackTrace()
            _errorMessage.value = "更新笔记失败: ${e.message}"
            throw e
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * 删除笔记从云端（新方法）
     */
    suspend fun deleteNoteInCloud(userId: String, token: String, note: NoteEntity) {
        try {
            Log.d("NoteViewModel", "🗑️ 从云端删除笔记: ${note.id}")
            _isLoading.value = true
            noteRepository.deleteNote(userId, token, note)
            // 重新加载列表
            loadNotesFromCloud(userId, token)
            Log.d("NoteViewModel", "✅ 笔记已删除")
        } catch (e: Exception) {
            Log.e("NoteViewModel", "❌ 删除失败", e)
            _errorMessage.value = "删除笔记失败: ${e.message}"
            throw e
        } finally {
            _isLoading.value = false
        }
    }
    
    suspend fun updateNote(note: NoteEntity) {
        try {
            throw UnsupportedOperationException("请使用 updateNoteInCloud 方法")
        } catch (e: Exception) {
            _errorMessage.value = "更新笔记失败: ${e.message}"
            throw e
        }
    }
    
    suspend fun deleteNote(note: NoteEntity) {
        try {
            throw UnsupportedOperationException("请使用 deleteNoteInCloud 方法")
        } catch (e: Exception) {
            _errorMessage.value = "删除笔记失败: ${e.message}"
            throw e
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun setError(message: String) {
        _errorMessage.value = message
    }
    
    // ========== AI功能 ==========
    
    /**
     * AI润色笔记（从云端获取）
     */
    suspend fun polishNote(userId: String, token: String, noteId: String): String? {
        return try {
            Log.d("NoteViewModel", "✨ 开始AI润色: $noteId")
            _isLoading.value = true
            val polished = noteRepository.polishNote(userId, token, noteId)
            Log.d("NoteViewModel", "✅ AI润色完成")
            polished
        } catch (e: Exception) {
            Log.e("NoteViewModel", "❌ AI润色失败", e)
            _errorMessage.value = "润色失败: ${e.message}"
            null
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * AI总结笔记（从云端获取）
     */
    suspend fun summarizeNote(userId: String, token: String, noteId: String): NoteSummary? {
        return try {
            Log.d("NoteViewModel", "📝 开始AI总结: $noteId")
            _isLoading.value = true
            val summary = noteRepository.summarizeNote(userId, token, noteId)
            Log.d("NoteViewModel", "✅ AI总结完成")
            summary
        } catch (e: Exception) {
            Log.e("NoteViewModel", "❌ AI总结失败", e)
            _errorMessage.value = "总结失败: ${e.message}"
            null
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * AI生成标题（从云端获取）
     */
    suspend fun generateTitle(userId: String, token: String, noteId: String): String? {
        return try {
            Log.d("NoteViewModel", "📌 开始生成标题: $noteId")
            _isLoading.value = true
            val title = noteRepository.generateTitle(userId, token, noteId)
            Log.d("NoteViewModel", "✅ 标题生成完成")
            title
        } catch (e: Exception) {
            Log.e("NoteViewModel", "❌ 生成标题失败", e)
            _errorMessage.value = "生成标题失败: ${e.message}"
            null
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * AI增强知识点（从云端获取）
     */
    suspend fun enhanceKnowledgePoints(userId: String, token: String, noteId: String): List<String>? {
        return try {
            Log.d("NoteViewModel", "💡 开始增强知识点: $noteId")
            _isLoading.value = true
            val points = noteRepository.enhanceKnowledgePoints(userId, token, noteId)
            Log.d("NoteViewModel", "✅ 知识点增强完成")
            points
        } catch (e: Exception) {
            Log.e("NoteViewModel", "❌ 增强知识点失败", e)
            _errorMessage.value = "提取知识点失败: ${e.message}"
            null
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * AI问答（从云端获取）
     */
    suspend fun answerQuestion(userId: String, token: String, noteId: String, question: String): String? {
        return try {
            Log.d("NoteViewModel", "❓ 开始AI问答: $noteId")
            _isLoading.value = true
            val answer = noteRepository.answerQuestion(userId, token, noteId, question)
            Log.d("NoteViewModel", "✅ AI问答完成")
            answer
        } catch (e: Exception) {
            Log.e("NoteViewModel", "❌ AI问答失败", e)
            _errorMessage.value = "回答问题失败: ${e.message}"
            null
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * AI生成科目
     * 根据笔记内容自动生成科目
     */
    suspend fun generateSubject(content: String, title: String? = null): String {
        return try {
            _isLoading.value = true
            val subject = noteRepository.generateSubject(content, title)
            // 如果生成的科目不在列表中，添加到列表
            if (subject.isNotEmpty() && !_subjects.value.contains(subject)) {
                _subjects.value = _subjects.value + subject
            }
            subject
        } catch (e: Exception) {
            Log.e("NoteViewModel", "生成科目失败", e)
            _errorMessage.value = "生成科目失败: ${e.message}"
            "其他"
        } finally {
            _isLoading.value = false
        }
    }
}

class NoteViewModelFactory(
    private val noteRepository: NoteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(noteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


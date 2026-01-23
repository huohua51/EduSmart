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
    
    suspend fun transcribeAudio(audioPath: String): String {
        return try {
            noteRepository.transcribeAudio(audioPath)
        } catch (e: Exception) {
            _errorMessage.value = "语音转写失败: ${e.message}"
            throw e
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
            val note = noteRepository.createNote(
                title = title,
                subject = subject,
                content = content,
                imagePaths = imagePaths,
                audioPath = audioPath,
                transcript = transcript
            )
            Log.d("NoteViewModel", "笔记创建成功，ID: ${note.id}")
            // 注意：不需要调用 loadNotes()，因为 Room Flow 会自动检测数据库变化并更新
            // Flow 会在数据库更新后自动发出新值，UI 会自动刷新
            // 但是需要重新加载科目列表，因为可能有新科目
            loadSubjects()
            note
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
            val note = noteRepository.mergeNote(title, subject, imagePaths, audioPath)
            // Room Flow 会自动更新，不需要手动调用 loadNotes()
            // 但是需要重新加载科目列表，因为可能有新科目
            loadSubjects()
            note
        } catch (e: Exception) {
            _errorMessage.value = "合并笔记失败: ${e.message}"
            throw e
        }
    }
    
    suspend fun updateNote(note: NoteEntity) {
        try {
            noteRepository.updateNote(note)
            // Room Flow 会自动更新，不需要手动调用 loadNotes()
            // 但是需要重新加载科目列表，因为科目可能被修改
            loadSubjects()
        } catch (e: Exception) {
            _errorMessage.value = "更新笔记失败: ${e.message}"
            throw e
        }
    }
    
    suspend fun deleteNote(note: NoteEntity) {
        try {
            noteRepository.deleteNote(note)
            // Room Flow 会自动更新，不需要手动调用 loadNotes()
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
     * AI润色笔记
     */
    suspend fun polishNote(noteId: String): String? {
        return try {
            _isLoading.value = true
            val polished = noteRepository.polishNote(noteId)
            polished
        } catch (e: Exception) {
            _errorMessage.value = "润色失败: ${e.message}"
            null
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * AI总结笔记
     */
    suspend fun summarizeNote(noteId: String): NoteSummary? {
        return try {
            _isLoading.value = true
            val summary = noteRepository.summarizeNote(noteId)
            summary
        } catch (e: Exception) {
            _errorMessage.value = "总结失败: ${e.message}"
            null
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * AI生成标题
     */
    suspend fun generateTitle(noteId: String): String? {
        return try {
            _isLoading.value = true
            val title = noteRepository.generateTitle(noteId)
            title
        } catch (e: Exception) {
            _errorMessage.value = "生成标题失败: ${e.message}"
            null
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * AI增强知识点
     */
    suspend fun enhanceKnowledgePoints(noteId: String): List<String>? {
        return try {
            _isLoading.value = true
            val points = noteRepository.enhanceKnowledgePoints(noteId)
            points
        } catch (e: Exception) {
            _errorMessage.value = "提取知识点失败: ${e.message}"
            null
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * AI问答
     */
    suspend fun answerQuestion(noteId: String, question: String): String? {
        return try {
            _isLoading.value = true
            val answer = noteRepository.answerQuestion(noteId, question)
            answer
        } catch (e: Exception) {
            _errorMessage.value = "回答问题失败: ${e.message}"
            null
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


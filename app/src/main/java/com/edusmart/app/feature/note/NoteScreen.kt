package com.edusmart.app.feature.note

import android.content.Context
import android.content.Intent
<<<<<<< Updated upstream
=======
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
>>>>>>> Stashed changes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.edusmart.app.feature.note.AudioRecordService
import com.edusmart.app.service.SpeechService
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun NoteScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val speechService = remember { SpeechService(context) }
    
    var isRecording by remember { mutableStateOf(false) }
    var isTranscribing by remember { mutableStateOf(false) }
<<<<<<< Updated upstream
=======
    var isRecognizing by remember { mutableStateOf(false) }
    var isShowingCamera by remember { mutableStateOf(false) }
    var isShowingEdit by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    // 使用 LaunchedEffect 处理搜索，添加防抖延迟
    LaunchedEffect(searchQuery) {
        kotlinx.coroutines.delay(300) // 防抖：300ms延迟
        if (searchQuery.isNotEmpty()) {
            viewModel.searchNotes(searchQuery)
        } else {
            // 搜索框为空时，重新加载所有笔记
            // loadNotes() 内部会取消之前的任务并重新开始收集 Flow，所以可以安全调用
            viewModel.loadNotes()
        }
    }
    
    // 使用 derivedStateOf 优化搜索状态计算（减少重组）
    val isSearching = remember(searchQuery) {
        searchQuery.isNotEmpty()
    }
    
    var capturedImagePath by remember { mutableStateOf<String?>(null) }
    var ocrResult by remember { mutableStateOf<String?>(null) }
>>>>>>> Stashed changes
    var transcriptResult by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf<List<String>>(emptyList()) }
    
<<<<<<< Updated upstream
    Column(
=======
    // AI功能状态
    var aiPolishedContent by remember { mutableStateOf<String?>(null) }
    var aiSummary by remember { mutableStateOf<NoteSummary?>(null) }
    var aiGeneratedTitle by remember { mutableStateOf<String?>(null) }
    var aiEnhancedPoints by remember { mutableStateOf<List<String>?>(null) }
    var aiAnswer by remember { mutableStateOf<String?>(null) }
    var aiQuestion by remember { mutableStateOf("") }
    var showingAIDialog by remember { mutableStateOf(false) }
    var aiProcessingNoteId by remember { mutableStateOf<String?>(null) }
    // 保存当前正在处理的笔记对象，用于应用AI结果
    var currentAINote by remember { mutableStateOf<NoteEntity?>(null) }
    
    // 录音完成广播接收器
    val recordingReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                android.util.Log.d("NoteScreen", "收到广播: action=${intent?.action}")
                when (intent?.action) {
                    AudioRecordService.ACTION_RECORDING_COMPLETE -> {
                        val audioPath = intent?.getStringExtra(AudioRecordService.EXTRA_AUDIO_PATH)
                        android.util.Log.d("NoteScreen", "收到录音完成广播，音频路径: $audioPath")
                        
                        // 立即保存音频路径，确保即使转写失败也能保存录音
                        currentAudioPath = audioPath
                        isRecording = false
                        
                        // 自动转写（在协程中执行，不阻塞主线程）
                        if (audioPath != null) {
                            android.util.Log.d("NoteScreen", "开始转写音频: $audioPath")
                            scope.launch {
                                try {
                                    isTranscribing = true
                                    android.util.Log.d("NoteScreen", "调用转写服务...")
                                    val transcript = viewModel.transcribeAudio(audioPath)
                                    android.util.Log.d("NoteScreen", "转写完成，结果长度: ${transcript.length}")
                                    transcriptResult = transcript
                                } catch (e: Exception) {
                                    android.util.Log.e("NoteScreen", "转写失败", e)
                                    // 转写失败时，不设置 transcriptResult，但保留 currentAudioPath
                                    // 这样用户仍然可以保存录音文件
                                    transcriptResult = null
                                    viewModel.setError("转写失败: ${e.message}，但录音已保存，您可以手动保存笔记")
                                } finally {
                                    isTranscribing = false
                                }
                            }
                        } else {
                            android.util.Log.w("NoteScreen", "音频路径为空，无法转写")
                            viewModel.setError("录音完成但音频路径为空")
                        }
                    }
                    AudioRecordService.ACTION_RECORDING_ERROR -> {
                        val errorMessage = intent?.getStringExtra(AudioRecordService.EXTRA_ERROR_MESSAGE)
                        android.util.Log.e("NoteScreen", "录音错误: $errorMessage")
                        isRecording = false
                        isTranscribing = false
                        // 设置错误信息
                        viewModel.setError(errorMessage ?: "录音失败")
                    }
                    AudioRecordService.ACTION_RECORDING_STARTED -> {
                        android.util.Log.d("NoteScreen", "录音已开始")
                        isRecording = true
                    }
                }
            }
        }
    }
    
    // 注册广播接收器
    DisposableEffect(Unit) {
        val filter = IntentFilter().apply {
            addAction(AudioRecordService.ACTION_RECORDING_COMPLETE)
            addAction(AudioRecordService.ACTION_RECORDING_ERROR)
            addAction(AudioRecordService.ACTION_RECORDING_STARTED)
        }
        android.util.Log.d("NoteScreen", "注册广播接收器: ${AudioRecordService.ACTION_RECORDING_COMPLETE}")
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.registerReceiver(recordingReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                android.util.Log.d("NoteScreen", "广播接收器已注册 (Android S+)")
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(recordingReceiver, filter)
                android.util.Log.d("NoteScreen", "广播接收器已注册 (Android < S)")
            }
        } catch (e: Exception) {
            android.util.Log.e("NoteScreen", "注册广播接收器失败", e)
        }
        onDispose {
            try {
                android.util.Log.d("NoteScreen", "注销广播接收器")
                context.unregisterReceiver(recordingReceiver)
            } catch (e: Exception) {
                android.util.Log.w("NoteScreen", "注销广播接收器失败", e)
            }
        }
    }
    
    // 导出助手
    val exportHelper = remember { NoteExportHelper(context) }
    
    // 应用润色内容的辅助函数
    suspend fun applyPolishedContent(note: NoteEntity) {
        try {
            android.util.Log.d("NoteScreen", "开始应用润色内容，原内容长度: ${note.content.length}, 新内容长度: ${aiPolishedContent!!.length}")
            val updatedNote = note.copy(content = aiPolishedContent!!, updatedAt = System.currentTimeMillis())
            android.util.Log.d("NoteScreen", "调用viewModel.updateNote...")
            viewModel.updateNote(updatedNote)
            android.util.Log.d("NoteScreen", "updateNote完成，从数据库重新加载...")
            // 从数据库重新加载笔记，确保获取最新数据
            val reloadedNote = noteRepository.getNoteById(note.id)
            android.util.Log.d("NoteScreen", "重新加载完成，reloadedNote: ${reloadedNote != null}")
            // 更新editingNote和currentAINote以刷新UI - 必须在主线程更新
            withContext(Dispatchers.Main) {
                val finalNote = reloadedNote ?: updatedNote
                android.util.Log.d("NoteScreen", "准备更新笔记状态")
                android.util.Log.d("NoteScreen", "新content: ${finalNote.content.take(50)}...")
                // 如果正在编辑这个笔记，更新editingNote
                if (editingNote?.id == note.id) {
                    editingNote = finalNote
                    android.util.Log.d("NoteScreen", "✅ editingNote已更新")
                }
                currentAINote = finalNote
                android.util.Log.d("NoteScreen", "✅ currentAINote已更新")
                showingAIDialog = false
                aiPolishedContent = null
                android.widget.Toast.makeText(
                    context,
                    "润色内容已应用",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("NoteScreen", "❌ 应用润色失败", e)
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(
                    context,
                    "应用失败: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                showingAIDialog = false
                aiPolishedContent = null
                currentAINote = null
            }
        }
    }
    
    // 应用总结内容的辅助函数
    suspend fun applySummaryContent(note: NoteEntity) {
        try {
            val summaryText = buildString {
                append("【摘要】\n")
                append(aiSummary!!.summary)
                append("\n\n【关键要点】\n")
                aiSummary!!.keyPoints.forEach { point ->
                    append("• $point\n")
                }
                if (aiSummary!!.tags.isNotEmpty()) {
                    append("\n【标签】\n")
                    aiSummary!!.tags.forEach { tag ->
                        append("• $tag ")
                    }
                }
                append("\n\n---\n\n")
                append(note.content)
            }
            android.util.Log.d("NoteScreen", "开始应用总结内容，原内容长度: ${note.content.length}, 新内容长度: ${summaryText.length}")
            val updatedNote = note.copy(content = summaryText, updatedAt = System.currentTimeMillis())
            android.util.Log.d("NoteScreen", "调用viewModel.updateNote...")
            viewModel.updateNote(updatedNote)
            android.util.Log.d("NoteScreen", "updateNote完成，从数据库重新加载...")
            // 从数据库重新加载笔记，确保获取最新数据
            val reloadedNote = noteRepository.getNoteById(note.id)
            android.util.Log.d("NoteScreen", "重新加载完成，reloadedNote: ${reloadedNote != null}")
            // 更新editingNote和currentAINote以刷新UI - 必须在主线程更新
            withContext(Dispatchers.Main) {
                val finalNote = reloadedNote ?: updatedNote
                android.util.Log.d("NoteScreen", "准备更新笔记状态")
                android.util.Log.d("NoteScreen", "新content: ${finalNote.content.take(50)}...")
                // 如果正在编辑这个笔记，更新editingNote
                if (editingNote?.id == note.id) {
                    editingNote = finalNote
                    android.util.Log.d("NoteScreen", "✅ editingNote已更新")
                }
                currentAINote = finalNote
                android.util.Log.d("NoteScreen", "✅ currentAINote已更新")
                showingAIDialog = false
                aiSummary = null
                android.widget.Toast.makeText(
                    context,
                    "总结内容已应用",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("NoteScreen", "❌ 应用总结失败", e)
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(
                    context,
                    "应用失败: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                showingAIDialog = false
                aiSummary = null
                currentAINote = null
            }
        }
    }
    
    // 渐变背景颜色（黄色/橙色到浅蓝色）
    val gradientColors = listOf(
        Color(0xFFFFE5B4), // 浅黄色
        Color(0xFFFFD89B), // 橙色
        Color(0xFFB8E6FF), // 浅蓝色
        Color(0xFFA8D8FF)  // 蓝色
    )
    
    Box(
>>>>>>> Stashed changes
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
<<<<<<< Updated upstream
        Text(
            text = "智能笔记精灵",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
=======
    Column(
        modifier = Modifier
            .fillMaxSize()
                .padding(20.dp)
>>>>>>> Stashed changes
        ) {
            Button(
                onClick = { /* TODO: 打开相机拍摄黑板 */ },
                modifier = Modifier.weight(1f)
            ) {
<<<<<<< Updated upstream
                Text("拍摄黑板")
            }
            
            Button(
                onClick = { 
                    if (!isRecording) {
                        // 开始录音
                        val intent = Intent(context, AudioRecordService::class.java).apply {
                            action = AudioRecordService.ACTION_START_RECORDING
=======
                Column(modifier = Modifier.weight(1f)) {
        Text(
                        text = "AI笔记",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D5016) // 深绿色
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "更懂你的创作伙伴",
                        fontSize = 16.sp,
                        color = Color(0xFF2D5016).copy(alpha = 0.8f)
                    )
                }
                IconButton(
                    onClick = { isShowingEdit = true; editingNote = null },
                    modifier = Modifier
                        .background(
                            color = Color.White.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(Icons.Default.Add, "新建笔记", tint = Color(0xFF2D5016))
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 描述文字
            Text(
                text = "AI全流程辅助创作\n激发创作灵感，精准润色表达，让囤积的知识变为生产力",
                fontSize = 14.sp,
                color = Color(0xFF555555),
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // 白色内容区域
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // 搜索栏
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("搜索笔记...", color = Color.Gray) },
                        leadingIcon = { 
                            Icon(
                                Icons.Default.Search, 
                                null,
                                tint = Color(0xFF2D5016)
                            ) 
                        },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, "清除", tint = Color.Gray)
                                }
                            }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2D5016),
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                            focusedTextColor = Color.Black, // 输入文字颜色：黑色
                            unfocusedTextColor = Color.Black // 未聚焦时文字颜色：黑色
                        )
                    )
            
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 科目筛选
                    if (subjects.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (selectedSubject == null) {
                                FilterChip(
                                    selected = true,
                                    onClick = { },
                                    label = { Text("全部", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF2D5016),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            } else {
                                FilterChip(
                                    selected = false,
                                    onClick = { viewModel.clearSubjectFilter() },
                                    label = { Text("全部", fontSize = 12.sp) }
                                )
                            }
                            subjects.forEach { subject ->
                                FilterChip(
                                    selected = selectedSubject == subject,
                                    onClick = { 
                                        if (selectedSubject == subject) {
                                            viewModel.clearSubjectFilter()
                                        } else {
                                            viewModel.loadNotesBySubject(subject)
                                        }
                                    },
                                    label = { Text(subject, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF2D5016),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
            
                    // 功能按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                            onClick = { isShowingCamera = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2D5016)
                            )
                        ) {
                            Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("拍摄黑板", fontSize = 14.sp)
            }
            
            Button(
                onClick = { 
                    if (!isRecording) {
                            // 检查权限
                            if (!audioPermissionState.allPermissionsGranted) {
                                audioPermissionState.launchMultiplePermissionRequest()
                                return@Button
                            }
                            
                            // 检查是否有 RECORD_AUDIO 权限
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                audioPermissionState.launchMultiplePermissionRequest()
                                return@Button
                            }
                            
                        val intent = Intent(context, AudioRecordService::class.java).apply {
                            action = AudioRecordService.ACTION_START_RECORDING
                        }
                            try {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                                } else {
                                    @Suppress("DEPRECATION")
                                    context.startService(intent)
                                }
                        isRecording = true
                        transcriptResult = null
                                currentAudioPath = null
                            } catch (e: Exception) {
                                // 处理启动服务失败的情况
                                e.printStackTrace()
                            }
                    } else {
                        // 停止录音
                        val intent = Intent(context, AudioRecordService::class.java).apply {
                            action = AudioRecordService.ACTION_STOP_RECORDING
                        }
                            try {
                        context.startService(intent)
                                // 立即更新UI状态，不等待广播
                        isRecording = false
                            } catch (e: Exception) {
                                // 如果服务启动失败，强制更新状态
                                isRecording = false
                                viewModel.setError("停止录音失败: ${e.message}")
                            }
                    }
                },
                modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) 
                                    Color(0xFFE53935)
                                else 
                                    Color(0xFF2D5016)
                            )
                        ) {
                            Icon(
                                if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isRecording) "停止录音" else "开始录音",
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 状态提示
        if (isRecording) {
            Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFEBEE)
                            )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFFE53935)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                                Text("正在录音中...", color = Color(0xFFE53935))
                }
            }
                        Spacer(modifier = Modifier.height(8.dp))
        }
        
        if (isTranscribing) {
            Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE3F2FD)
                            )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF2D5016)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                                Text("正在转写中...", color = Color(0xFF2D5016))
                }
            }
                        Spacer(modifier = Modifier.height(8.dp))
        }
        
                    if (isRecognizing) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE3F2FD)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF2D5016)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("正在识别中...", color = Color(0xFF2D5016))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // OCR结果
                    ocrResult?.let { result ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF5F5F5)
                )
            ) {
                Column(
                                modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                                        text = "OCR识别结果",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFF2D5016)
                        )
                                    Row {
                        TextButton(
                            onClick = { 
                                                scope.launch {
                                                    try {
                                                        viewModel.createNote(
                                                            title = "黑板笔记 ${SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date())}",
                                                            subject = subjects.firstOrNull() ?: "其他",
                                                            content = result,
                                                            imagePaths = listOfNotNull(capturedImagePath)
                                                        )
                                                        // 保存成功后清除临时数据
                                                        ocrResult = null
                                                        capturedImagePath = null
                                                        // 显示成功提示（通过Toast或Snackbar）
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "笔记已保存",
                                                            android.widget.Toast.LENGTH_SHORT
                                                        ).show()
                                                    } catch (e: Exception) {
                                                        viewModel.setError("保存失败: ${e.message}")
                                                    }
                                                }
                                            }
                                        ) {
                                            Text("保存", color = Color(0xFF2D5016))
                                        }
                                        TextButton(onClick = { ocrResult = null }) {
                                            Text("关闭", color = Color.Gray)
                                        }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
                        Spacer(modifier = Modifier.height(8.dp))
        }
        
                    // 转写结果或录音文件（即使转写失败也显示）
                    if (transcriptResult != null || currentAudioPath != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF5F5F5)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                                        text = if (transcriptResult != null) "语音转写结果" else "录音文件",
                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFF2D5016)
                                    )
                                    Row {
                                        TextButton(
                                            onClick = {
                                                scope.launch {
                                                    try {
                                                        val content = transcriptResult ?: "【录音文件】\n\n录音已保存，但转写失败。\n\n录音文件路径：$currentAudioPath"
                                                        viewModel.createNote(
                                                            title = "语音笔记 ${SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date())}",
                                                            subject = subjects.firstOrNull() ?: "其他",
                                                            content = content,
                                                            audioPath = currentAudioPath,
                                                            transcript = transcriptResult
                                                        )
                                                        // 保存成功后清除临时数据
                                                        transcriptResult = null
                                                        currentAudioPath = null
                                                        // 显示成功提示
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "笔记已保存",
                                                            android.widget.Toast.LENGTH_SHORT
                                                        ).show()
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("NoteScreen", "保存笔记失败", e)
                                                        viewModel.setError("保存失败: ${e.message}")
                                                    }
                                                }
                                            }
                                        ) {
                                            Text("保存", color = Color(0xFF2D5016))
                                        }
                                        TextButton(onClick = { 
                                            transcriptResult = null
                                            currentAudioPath = null
                                        }) {
                                            Text("关闭", color = Color.Gray)
                                        }
                                    }
                                }
                    Spacer(modifier = Modifier.height(8.dp))
                                if (transcriptResult != null) {
                                    Text(
                                        text = transcriptResult!!,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState())
                                    )
                                } else if (currentAudioPath != null) {
                                    Column {
                                        Text(
                                            text = "录音文件已保存，但转写失败。",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFE53935)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "文件路径：\n$currentAudioPath",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "您可以点击「保存」按钮保存录音文件，或手动输入笔记内容。",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // 错误提示
                    errorMessage?.let { error ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFEBEE)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFE53935),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.clearError() }) {
                                    Icon(Icons.Default.Close, "关闭", tint = Color(0xFFE53935))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // 笔记列表
                    if (isLoading && notes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF2D5016))
                        }
                    } else if (notes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.EditNote,
                                    null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.Gray.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = if (isSearching) "未找到相关笔记" else "暂无笔记，开始创建吧！",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray
                                )
                                if (!isSearching) {
                                    Text(
                                        text = "💡 提示：创建笔记后，点击笔记右上角的「⋮」菜单，即可使用AI功能（润色、总结、生成标题等）",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
>>>>>>> Stashed changes
                        }
                        context.startForegroundService(intent)
                        isRecording = true
                        errorMessage = null
                        transcriptResult = null
                    } else {
<<<<<<< Updated upstream
                        // 停止录音
                        val intent = Intent(context, AudioRecordService::class.java).apply {
                            action = AudioRecordService.ACTION_STOP_RECORDING
=======
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 使用 key 参数优化列表性能
                            items(
                                items = notes,
                                key = { it.id }
                            ) { note ->
                                NoteItem(
                                    note = note,
                                    onEdit = {
                                        editingNote = note
                                        isShowingEdit = true
                                    },
                                    onDelete = {
                scope.launch {
                                            viewModel.deleteNote(note)
                                        }
                                    },
                                    onShare = {
                                        // 在协程中执行，避免阻塞主线程
                                        scope.launch {
                                            exportHelper.shareNote(note)
                                        }
                                    },
                                    onExportPdf = {
                                        // 在协程中执行导出，避免阻塞主线程
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                val file = exportHelper.exportToPdf(note)
                                                if (file != null) {
                                                    withContext(Dispatchers.Main) {
                                                        exportHelper.shareFile(file, "application/pdf")
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "PDF导出成功",
                                                            android.widget.Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                        } else {
                                                    withContext(Dispatchers.Main) {
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "PDF导出失败，请检查存储权限",
                                                            android.widget.Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                        }
                    } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "PDF导出失败: ${e.message}",
                                                        android.widget.Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                                android.util.Log.e("NoteScreen", "PDF导出失败", e)
                                            }
                                        }
                                    },
                                    onExportImage = {
                                        // 在协程中执行导出，避免阻塞主线程
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                val file = exportHelper.exportToImage(note)
                                                if (file != null) {
                                                    withContext(Dispatchers.Main) {
                                                        exportHelper.shareFile(file, "image/png")
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "图片导出成功",
                                                            android.widget.Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                } else {
                                                    withContext(Dispatchers.Main) {
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "图片导出失败，请检查存储权限",
                                                            android.widget.Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "图片导出失败: ${e.message}",
                                                        android.widget.Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                                android.util.Log.e("NoteScreen", "图片导出失败", e)
                                            }
                                        }
                                    },
                                    onPolish = {
                                        scope.launch {
                                            try {
                                                aiProcessingNoteId = note.id
                                                currentAINote = note // 保存当前笔记对象
                                                val polished = viewModel.polishNote(note.id)
                                                if (polished != null) {
                                                    aiPolishedContent = polished
                                                    showingAIDialog = true
                                                }
                                            } catch (e: Exception) {
                                                viewModel.setError("AI润色失败: ${e.message}")
                                                currentAINote = null
                    } finally {
                                                aiProcessingNoteId = null
                                            }
                                        }
                                    },
                                    onSummarize = {
                                        scope.launch {
                                            try {
                                                aiProcessingNoteId = note.id
                                                currentAINote = note // 保存当前笔记对象
                                                val summary = viewModel.summarizeNote(note.id)
                                                if (summary != null) {
                                                    aiSummary = summary
                                                    showingAIDialog = true
                                                }
                                            } catch (e: Exception) {
                                                viewModel.setError("AI总结失败: ${e.message}")
                                                currentAINote = null
                                            } finally {
                                                aiProcessingNoteId = null
                                            }
                                        }
                                    },
                                    onGenerateTitle = {
                                        scope.launch {
                                            try {
                                                aiProcessingNoteId = note.id
                                                currentAINote = note // 保存当前笔记对象
                                                val title = viewModel.generateTitle(note.id)
                                                if (title != null) {
                                                    aiGeneratedTitle = title
                                                    showingAIDialog = true
                                                }
                                            } catch (e: Exception) {
                                                viewModel.setError("生成标题失败: ${e.message}")
                                                currentAINote = null
                                            } finally {
                                                aiProcessingNoteId = null
                                            }
                                        }
                                    },
                                    onEnhancePoints = {
                                        scope.launch {
                                            try {
                                                aiProcessingNoteId = note.id
                                                currentAINote = note // 保存当前笔记对象
                                                val points = viewModel.enhanceKnowledgePoints(note.id)
                                                if (points != null) {
                                                    aiEnhancedPoints = points
                                                    showingAIDialog = true
                                                }
                                            } catch (e: Exception) {
                                                viewModel.setError("增强知识点失败: ${e.message}")
                                                currentAINote = null
                                            } finally {
                                                aiProcessingNoteId = null
                                            }
                                        }
                                    },
                                    onAnswerQuestion = { question ->
                                        scope.launch {
                                            aiProcessingNoteId = note.id
                                            currentAINote = note // 保存当前笔记对象
                                            val answer = viewModel.answerQuestion(note.id, question)
                                            if (answer != null) {
                                                aiAnswer = answer
                                                showingAIDialog = true
                                            }
                                            aiProcessingNoteId = null
                                        }
                                    },
                                    isProcessing = aiProcessingNoteId == note.id
                                )
                            }
>>>>>>> Stashed changes
                        }
                        context.startService(intent)
                        isRecording = false
                        
                        // 等待录音文件生成后转写
                        // 注意：实际应该通过BroadcastReceiver接收录音完成事件
                        // 这里简化处理，需要手动触发转写
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isRecording) "停止录音" else "开始录音")
            }
        }
        
        // 录音中提示
        if (isRecording) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("正在录音中...")
                }
            }
        }
        
        // 转写中提示
        if (isTranscribing) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("正在转写中...")
                }
<<<<<<< Updated upstream
            }
        }
        
        // 转写结果
        transcriptResult?.let { result ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
=======
            },
            onCancel = { isShowingCamera = false },
            onPickFromGallery = { /* 暂不支持：需要时可在 NoteScreen 里接入相册选择逻辑 */ }
        )
    }
    
    // 编辑界面
    if (isShowingEdit) {
        // 使用key确保editingNote变化时NoteEditScreen会重新组合
        // 使用id、content的hashCode和updatedAt作为key，确保内容变化时也能触发重新组合
        val editKey = remember(editingNote?.id, editingNote?.content?.hashCode(), editingNote?.updatedAt) {
            "${editingNote?.id ?: "new"}_${editingNote?.content?.hashCode() ?: 0}_${editingNote?.updatedAt ?: 0}"
        }
        android.util.Log.d("NoteScreen", "NoteEditScreen key: $editKey")
        key(editKey) {
            NoteEditScreen(
                note = editingNote,
                subjects = subjects,
            onSave = { title, subject, content ->
                scope.launch {
                    try {
                        if (editingNote != null) {
                            viewModel.updateNote(editingNote!!.copy(
                                title = title,
                                subject = subject,
                                content = content
                            ))
                            android.widget.Toast.makeText(
                                context,
                                "笔记已更新",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            viewModel.createNote(title, subject, content)
                            android.widget.Toast.makeText(
                                context,
                                "笔记已保存",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                        isShowingEdit = false
                        editingNote = null
                    } catch (e: Exception) {
                        viewModel.setError("保存失败: ${e.message}")
                    }
                }
            },
            onCancel = {
                isShowingEdit = false
                editingNote = null
            }
            )
        }
    }
    
    // AI功能对话框
    if (showingAIDialog) {
        AlertDialog(
            onDismissRequest = {
                showingAIDialog = false
                aiPolishedContent = null
                aiSummary = null
                aiGeneratedTitle = null
                aiEnhancedPoints = null
                aiAnswer = null
                currentAINote = null // 清空当前AI处理的笔记
            },
            title = {
                Text(
                    text = when {
                        aiPolishedContent != null -> "AI润色结果"
                        aiSummary != null -> "笔记总结"
                        aiGeneratedTitle != null -> "智能标题"
                        aiEnhancedPoints != null -> "知识点增强"
                        aiAnswer != null -> "AI回答"
                        else -> "AI处理结果"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF2D5016)
>>>>>>> Stashed changes
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
<<<<<<< Updated upstream
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "转写结果",
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(
                            onClick = { 
                                transcriptResult = null
                                notes = notes + result
=======
                    // 润色结果
                    aiPolishedContent?.let { polished ->
                    Text(
                            text = polished,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    // 总结结果
                    aiSummary?.let { summary ->
                        Text(
                            text = "摘要：",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D5016)
                        )
                        Text(
                            text = summary.summary,
                            style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "关键要点：",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D5016)
                        )
                        summary.keyPoints.forEach { point ->
                            Text(
                                text = "• $point",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        if (summary.tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "标签：",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2D5016)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                summary.tags.forEach { tag ->
                                    AssistChip(
                                        onClick = { },
                                        label = { Text(tag, fontSize = 11.sp) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = Color(0xFFE8F5E9),
                                            labelColor = Color(0xFF2D5016)
                                        )
                                    )
                                }
                            }
                        }
                    }
                    
                    // 生成的标题
                    aiGeneratedTitle?.let { title ->
                            Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D5016)
                        )
                    }
                    
                    // 增强的知识点
                    aiEnhancedPoints?.let { points ->
                        Text(
                            text = "知识点：",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D5016)
                        )
                        points.forEach { point ->
                            Text(
                                text = "• $point",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    
                    // AI回答
                    aiAnswer?.let { answer ->
                        Text(
                            text = answer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                when {
                    aiPolishedContent != null -> {
                            TextButton(
                                onClick = { 
                                android.util.Log.d("NoteScreen", "========== 点击应用按钮（润色） ==========")
                                android.util.Log.d("NoteScreen", "currentAINote是否为null: ${currentAINote == null}")
                                android.util.Log.d("NoteScreen", "currentAINote.id: ${currentAINote?.id}")
                                android.util.Log.d("NoteScreen", "aiPolishedContent长度: ${aiPolishedContent?.length}")
                                
                                // 应用润色结果到笔记
                                val note = currentAINote
                                if (note == null) {
                                    android.util.Log.e("NoteScreen", "❌ currentAINote为null，尝试从数据库加载")
                                    // 如果currentAINote为null，尝试从aiProcessingNoteId加载
                                    scope.launch {
                                        try {
                                            val noteId = aiProcessingNoteId
                                            if (noteId != null) {
                                                val loadedNote = noteRepository.getNoteById(noteId)
                                                if (loadedNote != null) {
                                                    applyPolishedContent(loadedNote)
                                                } else {
                                                    withContext(Dispatchers.Main) {
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "错误：未找到要编辑的笔记",
                                                            android.widget.Toast.LENGTH_SHORT
                                                        ).show()
                                                        showingAIDialog = false
                                                        aiPolishedContent = null
                                                        currentAINote = null
                                                    }
                                                }
                                            } else {
                                                withContext(Dispatchers.Main) {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "错误：未找到要编辑的笔记",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                    showingAIDialog = false
                                                    aiPolishedContent = null
                                                    currentAINote = null
                                                }
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("NoteScreen", "❌ 加载笔记失败", e)
                                            withContext(Dispatchers.Main) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "错误：${e.message}",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                                showingAIDialog = false
                                                aiPolishedContent = null
                                                currentAINote = null
                                            }
                                        }
                                    }
                                    return@TextButton
                                }
                                
                                scope.launch {
                                    applyPolishedContent(note)
                                }
>>>>>>> Stashed changes
                            }
                        ) {
                            Text("保存到笔记")
                        }
                    }
<<<<<<< Updated upstream
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        }
        
        // 错误提示
        errorMessage?.let { error ->
            Card(
=======
                    aiSummary != null -> {
                        TextButton(
                            onClick = {
                                android.util.Log.d("NoteScreen", "========== 点击应用按钮（总结） ==========")
                                android.util.Log.d("NoteScreen", "currentAINote是否为null: ${currentAINote == null}")
                                android.util.Log.d("NoteScreen", "currentAINote.id: ${currentAINote?.id}")
                                android.util.Log.d("NoteScreen", "aiSummary: ${aiSummary != null}")
                                
                                // 应用总结结果到笔记（将摘要添加到内容开头）
                                val note = currentAINote
                                if (note == null) {
                                    android.util.Log.e("NoteScreen", "❌ currentAINote为null，尝试从数据库加载")
                                    // 如果currentAINote为null，尝试从aiProcessingNoteId加载
                                    scope.launch {
                                        try {
                                            val noteId = aiProcessingNoteId
                                            if (noteId != null) {
                                                val loadedNote = noteRepository.getNoteById(noteId)
                                                if (loadedNote != null) {
                                                    applySummaryContent(loadedNote)
                                                } else {
                                                    withContext(Dispatchers.Main) {
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "错误：未找到要编辑的笔记",
                                                            android.widget.Toast.LENGTH_SHORT
                                                        ).show()
                                                        showingAIDialog = false
                                                        aiSummary = null
                                                        currentAINote = null
                                                    }
                                                }
                                            } else {
                                                withContext(Dispatchers.Main) {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "错误：未找到要编辑的笔记",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                    showingAIDialog = false
                                                    aiSummary = null
                                                    currentAINote = null
                                                }
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("NoteScreen", "❌ 加载笔记失败", e)
                                            withContext(Dispatchers.Main) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "错误：${e.message}",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                                showingAIDialog = false
                                                aiSummary = null
                                                currentAINote = null
                                            }
                                        }
                                    }
                                    return@TextButton
                                }
                                
                                scope.launch {
                                    applySummaryContent(note)
                                }
                            }
                        ) {
                            Text("应用", color = Color(0xFF2D5016))
                        }
                    }
                    aiGeneratedTitle != null -> {
                        TextButton(
                            onClick = {
                                // 应用生成的标题
                                val note = currentAINote
                                if (note == null) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "错误：未找到要编辑的笔记",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                    showingAIDialog = false
                                    aiGeneratedTitle = null
                                    currentAINote = null
                                    return@TextButton
                                }
                                scope.launch {
                                    try {
                                        val updatedNote = note.copy(title = aiGeneratedTitle!!, updatedAt = System.currentTimeMillis())
                                        viewModel.updateNote(updatedNote)
                                        val reloadedNote = noteRepository.getNoteById(note.id)
                                        withContext(Dispatchers.Main) {
                                            val finalNote = reloadedNote ?: updatedNote
                                            // 如果正在编辑这个笔记，更新editingNote
                                            if (editingNote?.id == note.id) {
                                                editingNote = finalNote
                                            }
                                            currentAINote = finalNote
                                            showingAIDialog = false
                                            aiGeneratedTitle = null
                                            android.widget.Toast.makeText(
                                                context,
                                                "标题已应用",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("NoteScreen", "❌ 应用标题失败", e)
                                        withContext(Dispatchers.Main) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "应用失败: ${e.message}",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                            showingAIDialog = false
                                            aiGeneratedTitle = null
                                            currentAINote = null
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("应用", color = Color(0xFF2D5016))
                        }
                    }
                    aiEnhancedPoints != null -> {
                        TextButton(
                            onClick = {
                                // 应用增强的知识点
                                val note = currentAINote
                                if (note == null) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "错误：未找到要编辑的笔记",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                    showingAIDialog = false
                                    aiEnhancedPoints = null
                                    currentAINote = null
                                    return@TextButton
                                }
                                scope.launch {
                                    try {
                                        val updatedNote = note.copy(knowledgePoints = aiEnhancedPoints!!, updatedAt = System.currentTimeMillis())
                                        viewModel.updateNote(updatedNote)
                                        val reloadedNote = noteRepository.getNoteById(note.id)
                                        withContext(Dispatchers.Main) {
                                            val finalNote = reloadedNote ?: updatedNote
                                            // 如果正在编辑这个笔记，更新editingNote
                                            if (editingNote?.id == note.id) {
                                                editingNote = finalNote
                                            }
                                            currentAINote = finalNote
                                            showingAIDialog = false
                                            aiEnhancedPoints = null
                                            android.widget.Toast.makeText(
                                                context,
                                                "知识点已应用",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("NoteScreen", "❌ 应用知识点失败", e)
                                        withContext(Dispatchers.Main) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "应用失败: ${e.message}",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                            showingAIDialog = false
                                            aiEnhancedPoints = null
                                            currentAINote = null
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("应用", color = Color(0xFF2D5016))
                        }
                    }
                    else -> {
                        TextButton(
                            onClick = {
                                showingAIDialog = false
                                aiPolishedContent = null
                                aiSummary = null
                                aiGeneratedTitle = null
                                aiEnhancedPoints = null
                                aiAnswer = null
                            }
                        ) {
                            Text("关闭", color = Color(0xFF2D5016))
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showingAIDialog = false
                        aiPolishedContent = null
                        aiSummary = null
                        aiGeneratedTitle = null
                        aiEnhancedPoints = null
                        aiAnswer = null
                    }
                ) {
                    Text("取消", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun NoteItem(
    note: NoteEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onExportPdf: () -> Unit,
    onExportImage: () -> Unit,
    onPolish: () -> Unit = {},
    onSummarize: () -> Unit = {},
    onGenerateTitle: () -> Unit = {},
    onEnhancePoints: () -> Unit = {},
    onAnswerQuestion: (String) -> Unit = {},
    isProcessing: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit), // 点击卡片查看详情
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
>>>>>>> Stashed changes
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "错误",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        // 测试按钮（用于测试语音转写）
        Button(
            onClick = {
                // 测试转写功能（需要先有录音文件）
                scope.launch {
                    try {
                        isTranscribing = true
                        errorMessage = null
                        
                        // 查找最新的录音文件
                        val audioDir = File(context.getExternalFilesDir("audio")?.absolutePath ?: "")
                        val audioFiles = audioDir.listFiles()?.filter { it.name.endsWith(".m4a") || it.name.endsWith(".pcm") }
                        val latestFile = audioFiles?.maxByOrNull { it.lastModified() }
                        
                        if (latestFile != null && latestFile.exists()) {
                            val result = speechService.transcribe(latestFile.absolutePath)
                            transcriptResult = result
                        } else {
                            errorMessage = "未找到录音文件，请先录音"
                        }
                    } catch (e: Exception) {
                        errorMessage = "转写失败: ${e.message}"
                    } finally {
                        isTranscribing = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("测试转写（需要先录音）")
        }
        
        Button(
            onClick = { /* TODO: 自动合并笔记 */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("自动合并笔记")
        }
        
        // 笔记列表
        if (notes.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "我的笔记",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    notes.forEachIndexed { index, note ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 4.dp)
                            )
                            TextButton(
                                onClick = { 
                                    notes = notes.filterIndexed { i, _ -> i != index }
                                }
                            ) {
                                Text("删除")
                            }
                        }
                    }
                }
            }
        }
    }
}


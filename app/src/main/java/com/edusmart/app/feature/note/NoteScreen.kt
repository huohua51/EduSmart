package com.edusmart.app.feature.note

import android.content.Context
import android.content.Intent
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
    var transcriptResult by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf<List<String>>(emptyList()) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "智能笔记精灵",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { /* TODO: 打开相机拍摄黑板 */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("拍摄黑板")
            }
            
            Button(
                onClick = { 
                    if (!isRecording) {
                        // 开始录音
                        val intent = Intent(context, AudioRecordService::class.java).apply {
                            action = AudioRecordService.ACTION_START_RECORDING
                        }
                        context.startForegroundService(intent)
                        isRecording = true
                        errorMessage = null
                        transcriptResult = null
                    } else {
                        // 停止录音
                        val intent = Intent(context, AudioRecordService::class.java).apply {
                            action = AudioRecordService.ACTION_STOP_RECORDING
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
            }
        }
        
        // 转写结果
        transcriptResult?.let { result ->
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
                            }
                        ) {
                            Text("保存到笔记")
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
        }
        
        // 错误提示
        errorMessage?.let { error ->
            Card(
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


package com.edusmart.app.feature.note

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.edusmart.app.service.QwenAIService
import com.edusmart.app.ui.theme.*
import kotlinx.coroutines.launch

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun AIChatDialog(
    onDismiss: () -> Unit,
    qwenAIService: QwenAIService,
    initialMessage: String = ""
) {
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // 发送消息的函数
    fun sendMessage(text: String) {
        if (text.isBlank() || isSending) return
        
        val userMessage = ChatMessage(content = text, isUser = true)
        messages = messages + userMessage
        val currentInput = text
        inputText = ""
        isSending = true

        scope.launch {
            try {
                // 构建对话历史
                val chatHistory = messages.map { msg ->
                    mapOf(
                        "role" to if (msg.isUser) "user" else "assistant",
                        "content" to msg.content
                    )
                }
                
                val response = qwenAIService.chatWithHistory(chatHistory)
                val aiMessage = ChatMessage(
                    content = response,
                    isUser = false
                )
                messages = messages + aiMessage
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    content = "抱歉，我遇到了一些问题：${e.message}。请稍后再试。",
                    isUser = false
                )
                messages = messages + errorMessage
            } finally {
                isSending = false
            }
        }
    }

    // 处理初始消息
    LaunchedEffect(Unit) {
        if (initialMessage.isNotBlank()) {
            sendMessage(initialMessage)
        }
    }

    // 自动滚动到底部
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        // 白底黑字设计
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(0.dp)),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 顶部标题栏
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI助手",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "AI学习助手",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = "通义千问 · 随时为您解答疑惑",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Black.copy(alpha = 0.6f)
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = Color.Black
                            )
                        }
                    }
                }

                // 消息列表
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.White)
                ) {
                    if (messages.isEmpty()) {
                        // 空状态提示
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.Black.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "有什么问题尽管问我",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Black.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "我可以帮您解答学习中的疑惑",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(messages) { message ->
                                ChatMessageItem(message = message)
                            }
                            if (isSending) {
                                item {
                                    TypingIndicator()
                                }
                            }
                        }
                    }
                }

                // 输入框
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { 
                                    Text(
                                        "输入您的问题...",
                                        color = Color.Black.copy(alpha = 0.5f)
                                    ) 
                                },
                                enabled = !isSending,
                                maxLines = 4,
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Black,
                                    unfocusedBorderColor = Color.Black.copy(alpha = 0.3f),
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black
                                )
                            )
                            FloatingActionButton(
                                onClick = {
                                    sendMessage(inputText)
                                },
                                modifier = Modifier.size(56.dp),
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ) {
                                if (isSending) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.Black,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Send,
                                        contentDescription = "发送",
                                        tint = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            // AI头像
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(16.dp),
                color = PrimaryBlue // 清爽蓝色
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "AI",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 280.dp),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isUser) 16.dp else 4.dp,
                    bottomEnd = if (message.isUser) 4.dp else 16.dp
                ),
                color = if (message.isUser) 
                    Color.Black
                else 
                    LightPink, // 弥散淡蓝色毛玻璃效果
                tonalElevation = if (message.isUser) 0.dp else 1.dp
            ) {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isUser) 
                        Color.White
                    else 
                        Color.Black
                )
            }
        }

        if (message.isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            // 用户头像
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "我",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(16.dp),
            color = PrimaryBlue // 清爽蓝色
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "AI",
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            modifier = Modifier.widthIn(max = 80.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 4.dp,
                bottomEnd = 16.dp
            ),
            color = CardBackground // 清新淡蓝色背景
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) { index ->
                    CircularProgressIndicator(
                        modifier = Modifier.size(8.dp),
                        strokeWidth = 2.dp,
                        color = Color.Black
                    )
                }
            }
        }
    }
}


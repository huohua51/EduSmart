package com.edusmart.app.feature.speaking

import android.Manifest
import com.edusmart.app.ui.theme.*
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edusmart.app.service.SpeechServiceSparkChain
import com.edusmart.app.ui.components.BeautyButton
import com.edusmart.app.ui.components.BeautyLoadingCard
import com.edusmart.app.ui.components.BeautySceneChip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material.icons.filled.Translate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.AlertDialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SpeakingScreen() {
    val context = LocalContext.current

    // 初始化 ViewModel
    val viewModel: SpeakingViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SpeakingViewModel(context, SpeechServiceSparkChain(context)) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // 自定义话题输入状态
    var customTopicText by remember { mutableStateOf("") }
    var showCustomTopicDialog by remember { mutableStateOf(false) }

    // 学习目的选择对话框状态
    var showLearningPurposeDialog by remember { mutableStateOf(uiState.learningPurpose == null) }

    // 权限请求逻辑
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecording()
        } else {
            Toast.makeText(context, "请授予麦克风权限以进行练习", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "speaking_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (uiState.isAiSpeaking || uiState.isUserRecording) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_scale"
    )

    // 学习目的选择对话框 - 使用 LazyColumn 实现滚动
    if (showLearningPurposeDialog) {
        AlertDialog(
            onDismissRequest = { /* 不允许取消，必须选择 */ },
            title = {
                Text(
                    "选择您的学习目的",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp)  // 设置固定高度
                ) {
                    Text(
                        "为了提供更精准的练习内容，请选择您学习英语的目的：",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // 使用 LazyColumn 实现可滚动列表
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(LearningPurposes.getAll()) { purpose ->
                            Surface(
                                onClick = {
                                    viewModel.setLearningPurpose(purpose)
                                    showLearningPurposeDialog = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = CardBackground, // 清新淡蓝色背景
                                tonalElevation = 2.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 移除emoji显示
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = purpose.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                        Text(
                                            text = purpose.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Black.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }


    // 自定义话题对话框
    if (showCustomTopicDialog) {
        AlertDialog(
            onDismissRequest = { showCustomTopicDialog = false },
            title = { Text("自定义练习话题") },
            text = {
                Column {
                    Text("请输入您想要练习的话题：", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customTopicText,
                        onValueChange = { customTopicText = it },
                        label = { Text("例如：旅游、科技、美食等") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customTopicText.isNotBlank()) {
                            viewModel.setCustomTopic(customTopicText)
                            showCustomTopicDialog = false
                        }
                    },
                    enabled = customTopicText.isNotBlank()
                ) {
                    Text("开始练习")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomTopicDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 极简白底设计
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .scale(scale),
                        color = LightPink // 弥散淡蓝色毛玻璃效果
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AI口语私教",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        // 显示当前学习目的
                        uiState.learningPurpose?.let { purpose ->
                            Text(
                                text = purpose.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Black.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Row {
                    // 重新选择学习目的按钮
                    IconButton(onClick = { showLearningPurposeDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "重新选择学习目的",
                            tint = Color.Black.copy(alpha = 0.6f)
                        )
                    }

                    // 翻译开关按钮
                    IconButton(
                        onClick = { viewModel.toggleTranslation() }
                    ) {
                        Icon(
                            imageVector = if (uiState.showTranslation) Icons.Default.Translate else Icons.Default.Language,
                            contentDescription = if (uiState.showTranslation) "关闭翻译" else "开启翻译",
                            tint = if (uiState.showTranslation)
                                Color.Black
                            else
                                Color.Black.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // 场景选择区域
            AnimatedVisibility(visible = uiState.selectedScene == null && uiState.customTopic.isBlank()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "选择练习场景",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )

                        // 如果正在加载场景，显示加载指示器
                        if (uiState.isLoadingScenes) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "生成中...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // 显示根据学习目的生成的场景或默认场景
                    val scenesToShow = uiState.customScenes.ifEmpty {
                        // 如果还没有学习目的，显示提示
                        emptyList()
                    }

                    if (scenesToShow.isNotEmpty()) {
                        // 使用流式布局（Grid）显示场景，自动换行
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 显示所有场景
                            scenesToShow.forEach { scene ->
                                BeautySceneChip(
                                    text = scene.name,
                                    selected = uiState.selectedScene?.name == scene.name,
                                    onClick = { viewModel.onSceneSelected(scene) },
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            // 自定义话题按钮
                            BeautySceneChip(
                                text = "自定义话题",
                                selected = false,
                                onClick = { showCustomTopicDialog = true },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    } else if (!uiState.isLoadingScenes) {
                        // 如果没有场景且不在加载中，显示提示
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = CardBackground // 清新淡蓝色背景
                            )
                        ) {
                            Text(
                                text = "正在为您准备专属练习场景...",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            // 当前场景/话题信息
            AnimatedVisibility(visible = uiState.selectedScene != null || uiState.customTopic.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = CardBackground // 清新淡蓝色背景
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        uiState.selectedScene?.let { scene ->
                            // 移除emoji显示
                            Column {
                                Text(
                                    text = "场景: ${scene.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = scene.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Black.copy(alpha = 0.7f)
                                )
                            }
                        }

                        if (uiState.customTopic.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Default.Topic,
                                contentDescription = null,
                                tint = PrimaryBlue, // 清爽蓝色
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "当前话题: ${uiState.customTopic}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            // 对话消息列表
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.messages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (uiState.selectedScene == null && uiState.customTopic.isBlank())
                                "请选择场景或设置自定义话题开始练习"
                            else
                                "准备好了，请开始说话",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(uiState.messages.size) { index ->
                            val message = uiState.messages[index]
                            ChatBubble(
                                message = message,
                                showTranslation = uiState.showTranslation,
                                onTranslateClick = { viewModel.generateTranslationForMessage(index) },
                                onSuggestedReplyClick = { viewModel.generateSuggestedReply(index) }
                            )
                        }
                    }
                }
            }

            // 加载状态和录音按钮
            if (uiState.selectedScene != null || uiState.customTopic.isNotBlank()) {
                if (uiState.isProcessing) {
                    BeautyLoadingCard(
                        message = "AI 正在思考并评估发音...",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                BeautyButton(
                    text = when {
                        uiState.isUserRecording -> "⏹ 停止录音"
                        uiState.isAiSpeaking -> "🔊 AI正在播报..."
                        else -> "🎤 开始对话"
                    },
                    onClick = {
                        if (uiState.isUserRecording) {
                            viewModel.stopRecording()
                        } else {
                            val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) viewModel.startRecording() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    isPrimary = !uiState.isUserRecording,
                    enabled = !uiState.isProcessing && !uiState.isAiSpeaking // AI说话时禁用按钮，防止录入AI自己的声音
                )
            }
        }
    }
}
/**
 * 聊天气泡组件（支持翻译显示和参考回复）
 */
@Composable
fun ChatBubble(
    message: ChatMessage,
    showTranslation: Boolean,
    onTranslateClick: () -> Unit,
    onSuggestedReplyClick: () -> Unit
) {
    val isAI = message.role == "AI"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isAI) Arrangement.Start else Arrangement.End
    ) {
        if (isAI) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = PrimaryBlue, // 清爽蓝色
                modifier = Modifier.padding(top = 8.dp, end = 8.dp).size(20.dp)
            )
        }

        Column(
            horizontalAlignment = if (isAI) Alignment.Start else Alignment.End,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Surface(
                color = if (isAI) CardBackground else PrimaryBlue, // AI消息用清新淡蓝色，用户消息用清爽蓝色
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isAI) 0.dp else 16.dp,
                    bottomEnd = if (isAI) 16.dp else 0.dp
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // 英文内容
                    Text(
                        text = message.content,
                        color = if (isAI) Color.Black else Color.White, // AI消息用黑色，用户消息用白色
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // 翻译内容（如果开启且有翻译）
                    if (showTranslation && !message.translation.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = if (isAI)
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            else
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Translate,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isAI) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = message.translation,
                                color = if (isAI) Color.Black.copy(alpha = 0.7f)
                                else
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // 参考回复（仅AI消息且已生成）
                    if (isAI && !message.suggestedReply.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = PrimaryBlue // 清爽蓝色
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "参考回复:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue // 清爽蓝色
                                )
                                Text(
                                    text = message.suggestedReply,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // 发音评分（仅用户消息）
                    if (!isAI && message.score != null) {
                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "发音评分: ${message.score.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            LinearProgressIndicator(
                                progress = message.score / 100f,
                                modifier = Modifier.width(60.dp).height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = when {
                                    message.score >= 80 -> SuccessGreen
                                    message.score >= 60 -> WarningOrange
                                    else -> AccentCoral
                                },
                                trackColor = Color.White.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            // 消息底部：角色标签和操作按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = if (isAI) Arrangement.SpaceBetween else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isAI) {
                    Text(
                        text = "AI Coach",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )

                    // AI消息的操作按钮
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // 翻译按钮
                        if (message.translation == null) {
                            IconButton(
                                onClick = onTranslateClick,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Translate,
                                    contentDescription = "翻译",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // 参考回复按钮
                        if (message.suggestedReply == null) {
                            IconButton(
                                onClick = onSuggestedReplyClick,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Lightbulb,
                                    contentDescription = "参考回复",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "You",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}
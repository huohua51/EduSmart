package com.edusmart.app.feature.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.edusmart.app.data.database.EduDatabase
import com.edusmart.app.data.entity.WrongQuestionEntity
import com.edusmart.app.service.OCRService
import com.edusmart.app.service.QwenAIService
import com.edusmart.app.service.QuestionAnalysisResult
import com.edusmart.app.ui.components.BeautyLoadingCard
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultDetailScreen(
    navController: NavController,
    imagePath: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ocrService = remember { OCRService() }
    val qwenService = remember { QwenAIService() }
    val database = remember { EduDatabase.getDatabase(context) }

    var isLoading by remember { mutableStateOf(true) }
    var questionText by remember { mutableStateOf("") }
    var analysisResult by remember { mutableStateOf<QuestionAnalysisResult?>(null) }
    var showAddToWrongBookDialog by remember { mutableStateOf(false) }

    // 启动时自动识别和分析
    LaunchedEffect(imagePath) {
        try {
            // 步骤1: OCR识别
            questionText = ocrService.recognizeText(imagePath)

            // 步骤2: AI分析
            val result = qwenService.analyzeQuestion(questionText, imagePath)
            analysisResult = result
        } catch (e: Exception) {
            analysisResult = QuestionAnalysisResult(
                success = false,
                questionText = questionText,
                errorMessage = e.message
            )
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("题目解析") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    // 添加到错题本按钮
                    if (analysisResult?.success == true) {
                        IconButton(onClick = { showAddToWrongBookDialog = true }) {
                            Icon(Icons.Default.BookmarkAdd, "添加到错题本")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            if (isLoading) {
                // 加载中
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    BeautyLoadingCard(
                        message = "正在识别题目并分析...",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // 显示结果
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 题目文本
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "📝 题目",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = questionText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }

                    analysisResult?.let { result ->
                        if (result.success) {
                            // 答案卡片
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(4.dp, RoundedCornerShape(20.dp)),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // 答案
                                    Text(
                                        text = "✅ 答案",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = result.answer,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    // 解题步骤
                                    if (result.steps.isNotEmpty()) {
                                        Divider()
                                        Text(
                                            text = "📋 解题步骤",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        result.steps.forEachIndexed { index, step ->
                                            Text(
                                                text = "${index + 1}. $step",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                            )
                                        }
                                    }

                                    // 知识点
                                    if (result.knowledgePoints.isNotEmpty()) {
                                        Divider()
                                        Text(
                                            text = "💡 涉及知识点",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        result.knowledgePoints.forEach { point ->
                                            Text(
                                                text = "• $point",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                            )
                                        }
                                    }

                                    // 思路分析
                                    if (result.analysis.isNotEmpty()) {
                                        Divider()
                                        Text(
                                            text = "🧠 思路分析",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = result.analysis,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        } else {
                            // 无法解答
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(4.dp, RoundedCornerShape(20.dp)),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp)
                                ) {
                                    Text(
                                        text = "❌ 暂时无法解答",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = result.errorMessage ?: "AI暂时无法解答此题目，请尝试拍摄更清晰的照片或手动输入题目。",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 添加到错题本对话框
    if (showAddToWrongBookDialog) {
        AlertDialog(
            onDismissRequest = { showAddToWrongBookDialog = false },
            title = { Text("添加到错题本") },
            text = { Text("确定要将此题目添加到错题本吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            analysisResult?.let { result ->
                                val wrongQuestion = WrongQuestionEntity(
                                    id = UUID.randomUUID().toString(),
                                    questionText = questionText,
                                    answer = result.answer,
                                    steps = JSONArray(result.steps).toString(),
                                    knowledgePoints = JSONArray(result.knowledgePoints).toString(),
                                    analysis = result.analysis,
                                    imagePath = imagePath,
                                    userAnswer = null,
                                    wrongReason = null,
                                    reviewCount = 0,
                                    lastReviewTime = null,
                                    nextReviewTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000,
                                    createdAt = System.currentTimeMillis()
                                )
                                database.wrongQuestionDao().insertWrongQuestion(wrongQuestion)
                            }
                            showAddToWrongBookDialog = false
                        }
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddToWrongBookDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

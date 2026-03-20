package com.edusmart.app.feature.radar

import android.net.Uri
import com.edusmart.app.ui.theme.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.edusmart.app.data.database.EduDatabase
import com.edusmart.app.data.entity.WrongQuestionEntity
import com.edusmart.app.repository.RadarRepository
import com.edusmart.app.repository.WrongQuestionRepository
import com.edusmart.app.service.*
import com.edusmart.app.ui.components.BeautyLoadingCard
import com.edusmart.app.ui.components.RadarChart
import com.edusmart.app.util.KnowledgeMasteryAnalyzer
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreenNew() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { EduDatabase.getDatabase(context) }

    // ✅ 从 SharedPreferences 获取用户认证信息
    val sp = remember { context.getSharedPreferences("auth", android.content.Context.MODE_PRIVATE) }
    val userId = remember { sp.getString("userId", "") ?: "" }
    val token = remember { sp.getString("token", "") ?: "" }

    // 初始化服务和仓库
    val ocrService = remember { OCRService() }
    val qwenService = remember { QwenAIService() }
    val aiService = remember { AIService() }
    val examPaperService = remember {
        ExamPaperAnalysisService(ocrService, qwenService)
    }
    val repository = remember {
        RadarRepository(
            database.knowledgePointDao(),
            database.testRecordDao(),
            database.wrongQuestionDao(),
            ocrService,
            aiService,
            examPaperService
        )
    }
    val wrongQuestionRepository = remember {
        WrongQuestionRepository(database.wrongQuestionDao())
    }

    // 状态管理
    var showWrongQuestionScreen by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<ExamPaperAnalysisResult?>(null) }
    var showPracticeTest by remember { mutableStateOf(false) }
    var practiceQuestions by remember { mutableStateOf<List<PracticeQuestionInfo>>(emptyList()) }
    var selectedWrongQuestionId by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // 错题数据和掌握度分析
    // 修改后（从云端获取）
    var wrongQuestions by remember { mutableStateOf<List<WrongQuestionEntity>>(emptyList()) }
    var isLoadingWrongQuestions by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) } // 刷新触发器

    // 加载错题的函数
    suspend fun loadWrongQuestions() {
        if (userId.isNotEmpty() && token.isNotEmpty()) {
            isLoadingWrongQuestions = true
            try {
                wrongQuestions = wrongQuestionRepository.getAllWrongQuestionsFromCloud(userId, token)
                android.util.Log.d("RadarScreen", "✅ 从云端加载错题成功: ${wrongQuestions.size} 条")
            } catch (e: Exception) {
                android.util.Log.e("RadarScreen", "❌ 从云端加载错题失败: ${e.message}")
            } finally {
                isLoadingWrongQuestions = false
            }
        }
    }

    // 首次加载和刷新时触发
    LaunchedEffect(userId, token, refreshTrigger) {
        loadWrongQuestions()
    }
    val masteryAnalyzer = remember { KnowledgeMasteryAnalyzer() }
    val subjectMastery = remember(wrongQuestions) {
        masteryAnalyzer.analyzeSubjectMastery(wrongQuestions)
    }

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            scope.launch {
                try {
                    isAnalyzing = true
                    
                    // ⚠️ 检查登录状态
                    if (userId.isEmpty() || token.isEmpty()) {
                        android.widget.Toast.makeText(
                            context,
                            "请先登录",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        isAnalyzing = false
                        return@launch
                    }
                    
                    val imagePath = saveImageToFile(context, it)
                    val result = examPaperService.analyzeExamPaper(imagePath)
                    analysisResult = result
                    isAnalyzing = false

                    if (result.success && result.wrongQuestions.isNotEmpty()) {
                        // ☁️ 直接批量上传到云端
                        repository.batchAddWrongQuestions(userId, token, result.wrongQuestions, imagePath)
                        // 不显示Toast提示

                        // ✅ 立即刷新雷达图
                        refreshTrigger++
                    }
                } catch (e: Exception) {
                    android.util.Log.e("RadarScreenNew", "❌ 上传失败: ${e.message}")
                    android.widget.Toast.makeText(
                        context,
                        "上传失败: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    isAnalyzing = false
                }
            }
        }
    }

    if (showWrongQuestionScreen) {
        WrongQuestionScreen(onBack = {
            showWrongQuestionScreen = false
            // ✅ 从错题本返回时刷新雷达图
            refreshTrigger++
        })
    } else if (showPracticeTest && practiceQuestions.isNotEmpty()) {
        PracticeTestScreen(
            questions = practiceQuestions,
            sourceQuestionId = selectedWrongQuestionId,
            onBack = {
                showPracticeTest = false
                practiceQuestions = emptyList()
                // ✅ 从举一反三返回时刷新雷达图（可能有新的错题）
                refreshTrigger++
            }
        )
    } else {
        // 极简白底设计
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("知识雷达", color = Color.Black) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.White)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 扫描试卷按钮
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = CardBackground // 清新淡蓝色背景
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "试卷扫描",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.Black
                                )

                                Text(
                                    text = "扫描试卷，自动识别错题并加入错题本",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Black.copy(alpha = 0.7f)
                                )

                                Button(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PrimaryBlue
                                    )
                                ) {
                                    Icon(Icons.Default.CameraAlt, "扫描", tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("从相册选择试卷", color = Color.White)
                                }

                                OutlinedButton(
                                    onClick = { showWrongQuestionScreen = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.ErrorOutline, "错题本")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("查看错题本", color = Color.Black)
                                }
                            }
                        }
                    }

                    // 分析中状态
                    if (isAnalyzing) {
                        item {
                            BeautyLoadingCard(
                                message = "正在分析试卷...",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // 分析结果
                    analysisResult?.let { result ->
                        if (result.success) {
                            item {
                                AnalysisResultCard(
                                    result = result,
                                    onGenerateSimilar = { wrongQuestion ->
                                        scope.launch {
                                            try {
                                                val questions = examPaperService.generatePracticeQuestions(wrongQuestion, 3)
                                                if (questions.isNotEmpty()) {
                                                    practiceQuestions = questions
                                                    selectedWrongQuestionId = null
                                                    showPracticeTest = true
                                                } else {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "生成练习题失败，请重试",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            } catch (e: Exception) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "生成失败: ${e.message}",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // 雷达图展示
                    if (subjectMastery.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(2.dp, RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = CardBackground // 清新淡蓝色背景
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "知识掌握度雷达图",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.Black
                                    )

                                    val radarData = subjectMastery.mapValues { it.value.masteryScore }
                                    RadarChart(
                                        data = radarData,
                                        maxValue = 100f
                                    )
                                }
                            }
                        }

                        // 薄弱知识点列表 - 新设计
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 标题
                                Text(
                                    text = "薄弱知识点",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )

                                // 知识点卡片列表
                                subjectMastery.forEach { (subject, mastery) ->
                                    if (mastery.weakKnowledgePoints.isNotEmpty()) {
                                        mastery.weakKnowledgePoints.forEach { point ->
                                            WeakKnowledgePointCard(
                                                subject = subject,
                                                knowledgePoint = point.name,
                                                errorCount = point.errorCount
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
    }
}

@Composable
fun AnalysisResultCard(
    result: ExamPaperAnalysisResult,
    onGenerateSimilar: (WrongQuestionInfo) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground // 清新淡蓝色背景
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "分析完成",
                style = MaterialTheme.typography.titleLarge,
                color = Color.Black
            )

            Text(
                text = "学科：${result.subject}",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "识别到 ${result.wrongQuestions.size} 道错题",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black.copy(alpha = 0.7f)
            )

            Divider()

            result.wrongQuestions.take(3).forEach { question ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "题${question.questionNumber}: ${question.questionText}",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2
                    )
                    Text(
                        text = "知识点: ${question.knowledgePoints.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onGenerateSimilar(question) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "举一反三",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "举一反三",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (result.wrongQuestions.size > 3) {
                Text(
                    text = "还有 ${result.wrongQuestions.size - 3} 道错题...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black.copy(alpha = 0.5f)
                )
            }
        }
    }
}

fun saveImageToFile(context: android.content.Context, uri: Uri): String {
    val inputStream = context.contentResolver.openInputStream(uri)
    val file = File(context.filesDir, "images/${System.currentTimeMillis()}.jpg")
    file.parentFile?.mkdirs()
    file.outputStream().use { outputStream ->
        inputStream?.copyTo(outputStream)
    }
    return file.absolutePath
}

/**
 * 薄弱知识点卡片组件 - 参考UI设计
 */
@Composable
fun WeakKnowledgePointCard(
    subject: String,
    knowledgePoint: String,
    errorCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：图标圆圈
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = PrimaryBlue.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 中间：知识点信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = knowledgePoint,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "错误 $errorCount 次",
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentCoral
                )
            }
        }
    }
}

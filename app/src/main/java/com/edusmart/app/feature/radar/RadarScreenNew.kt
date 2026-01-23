package com.edusmart.app.feature.radar

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.edusmart.app.data.database.EduDatabase
import com.edusmart.app.repository.RadarRepository
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

    // 状态管理
    var showWrongQuestionScreen by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<ExamPaperAnalysisResult?>(null) }
    var showPracticeTest by remember { mutableStateOf(false) }
    var practiceQuestions by remember { mutableStateOf<List<PracticeQuestionInfo>>(emptyList()) }
    var selectedWrongQuestionId by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // 错题数据和掌握度分析
    val wrongQuestions by repository.getAllWrongQuestions().collectAsState(initial = emptyList())
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
                isAnalyzing = true
                val imagePath = saveImageToFile(context, it)
                val result = examPaperService.analyzeExamPaper(imagePath)
                analysisResult = result
                isAnalyzing = false

                if (result.success && result.wrongQuestions.isNotEmpty()) {
                    repository.batchAddWrongQuestions(result.wrongQuestions, imagePath)
                }
            }
        }
    }

    if (showWrongQuestionScreen) {
        WrongQuestionScreen(onBack = { showWrongQuestionScreen = false })
    } else if (showPracticeTest && practiceQuestions.isNotEmpty()) {
        PracticeTestScreen(
            questions = practiceQuestions,
            sourceQuestionId = selectedWrongQuestionId,
            onBack = {
                showPracticeTest = false
                practiceQuestions = emptyList()
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("知识雷达") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
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
                                .shadow(4.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "📄 试卷扫描",
                                    style = MaterialTheme.typography.titleLarge
                                )

                                Text(
                                    text = "扫描试卷，自动识别错题并加入错题本",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )

                                Button(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.CameraAlt, "扫描")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("拍摄或选择试卷")
                                }

                                OutlinedButton(
                                    onClick = { showWrongQuestionScreen = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.ErrorOutline, "错题本")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("查看错题本")
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
                                            val questions = examPaperService.generatePracticeQuestions(wrongQuestion, 3)
                                            practiceQuestions = questions
                                            selectedWrongQuestionId = null
                                            showPracticeTest = true
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
                                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "📊 知识掌握度雷达图",
                                        style = MaterialTheme.typography.titleLarge
                                    )

                                    val radarData = subjectMastery.mapValues { it.value.masteryScore }
                                    RadarChart(
                                        data = radarData,
                                        maxValue = 100f
                                    )
                                }
                            }
                        }

                        // 薄弱知识点列表
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "💡 薄弱知识点",
                                        style = MaterialTheme.typography.titleLarge
                                    )

                                    subjectMastery.forEach { (subject, mastery) ->
                                        if (mastery.weakKnowledgePoints.isNotEmpty()) {
                                            Text(
                                                text = subject,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            mastery.weakKnowledgePoints.forEach { point ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 16.dp, top = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "• ${point.name}",
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Text(
                                                        text = "错${point.errorCount}次",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
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
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "✅ 分析完成",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "学科：${result.subject}",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "识别到 ${result.wrongQuestions.size} 道错题",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onGenerateSimilar(question) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
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
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "举一反三",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            if (result.wrongQuestions.size > 3) {
                Text(
                    text = "还有 ${result.wrongQuestions.size - 3} 道错题...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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


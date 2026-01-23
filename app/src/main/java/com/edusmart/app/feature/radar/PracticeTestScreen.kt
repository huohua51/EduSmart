package com.edusmart.app.feature.radar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.edusmart.app.data.entity.PracticeRecordEntity
import com.edusmart.app.service.PracticeQuestionInfo
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeTestScreen(
    questions: List<PracticeQuestionInfo>,
    sourceQuestionId: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { EduDatabase.getDatabase(context) }

    var currentQuestionIndex by remember { mutableStateOf(0) }
    var userAnswers by remember { mutableStateOf(mutableMapOf<Int, String>()) }
    var showResult by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<Boolean>>(emptyList()) }

    val currentQuestion = questions.getOrNull(currentQuestionIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("举一反三练习 (${currentQuestionIndex + 1}/${questions.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
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
            if (showResult) {
                // 显示结果
                ResultScreen(
                    questions = questions,
                    userAnswers = userAnswers,
                    results = results,
                    onRestart = {
                        currentQuestionIndex = 0
                        userAnswers.clear()
                        showResult = false
                    },
                    onBack = onBack
                )
            } else {
                // 显示题目
                currentQuestion?.let { question ->
                    QuestionCard(
                        question = question,
                        questionIndex = currentQuestionIndex,
                        userAnswer = userAnswers[currentQuestionIndex],
                        onAnswerSelected = { answer ->
                            userAnswers[currentQuestionIndex] = answer
                        },
                        onNext = {
                            if (currentQuestionIndex < questions.size - 1) {
                                currentQuestionIndex++
                            }
                        },
                        onPrevious = {
                            if (currentQuestionIndex > 0) {
                                currentQuestionIndex--
                            }
                        },
                        onSubmit = {
                            // 计算结果
                            results = questions.mapIndexed { index, q ->
                                userAnswers[index] == q.correctAnswer
                            }

                            // 保存练习记录
                            scope.launch {
                                questions.forEachIndexed { index, q ->
                                    val record = PracticeRecordEntity(
                                        id = UUID.randomUUID().toString(),
                                        questionText = q.questionText,
                                        questionType = q.questionType,
                                        correctAnswer = q.correctAnswer,
                                        userAnswer = userAnswers[index] ?: "",
                                        isCorrect = userAnswers[index] == q.correctAnswer,
                                        options = if (q.options.isNotEmpty()) JSONArray(q.options).toString() else null,
                                        steps = JSONArray(q.steps).toString(),
                                        knowledgePoints = JSONArray(q.knowledgePoints).toString(),
                                        sourceQuestionId = sourceQuestionId
                                    )
                                    database.practiceRecordDao().insertPracticeRecord(record)
                                }
                            }

                            showResult = true
                        },
                        isLastQuestion = currentQuestionIndex == questions.size - 1,
                        hasAnswered = userAnswers.containsKey(currentQuestionIndex)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionCard(
    question: PracticeQuestionInfo,
    questionIndex: Int,
    userAnswer: String?,
    onAnswerSelected: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSubmit: () -> Unit,
    isLastQuestion: Boolean,
    hasAnswered: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 题目卡片
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
                    text = "第 ${questionIndex + 1} 题",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = question.questionText,
                    style = MaterialTheme.typography.bodyLarge
                )

                Divider()

                // 根据题目类型显示不同的答题界面
                when (question.questionType) {
                    "choice" -> {
                        // 选择题
                        question.options.forEach { option ->
                            val optionLetter = option.substringBefore(".").trim()
                            FilterChip(
                                selected = userAnswer == optionLetter,
                                onClick = { onAnswerSelected(optionLetter) },
                                label = { Text(option) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    "fill" -> {
                        // 填空题
                        var fillAnswer by remember { mutableStateOf(userAnswer ?: "") }
                        OutlinedTextField(
                            value = fillAnswer,
                            onValueChange = {
                                fillAnswer = it
                                onAnswerSelected(it)
                            },
                            label = { Text("请输入答案") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 按钮区域
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (questionIndex > 0) {
                OutlinedButton(
                    onClick = onPrevious,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("上一题")
                }
            }

            if (isLastQuestion) {
                Button(
                    onClick = onSubmit,
                    modifier = Modifier.weight(1f),
                    enabled = hasAnswered
                ) {
                    Text("提交答案")
                }
            } else {
                Button(
                    onClick = onNext,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("下一题")
                }
            }
        }
    }
}



@Composable
fun ResultScreen(
    questions: List<PracticeQuestionInfo>,
    userAnswers: Map<Int, String>,
    results: List<Boolean>,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    val correctCount = results.count { it }
    val totalCount = questions.size
    val score = (correctCount.toFloat() / totalCount * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (score >= 60) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = if (score >= 60) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text(
                    text = "得分：$score 分",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = "答对 $correctCount / $totalCount 题", style = MaterialTheme.typography.bodyLarge)
            }
        }

        // 详细解析
        questions.forEachIndexed { index, question ->
            val isCorrect = results.getOrNull(index) ?: false
            val userAnswer = userAnswers[index] ?: "未作答"

            Card(
                modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCorrect)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    else
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "第 ${index + 1} 题",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Icon(
                            imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }

                    Text(
                        text = question.questionText,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Divider()

                    Text(
                        text = "你的答案：$userAnswer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )

                    Text(
                        text = "正确答案：${question.correctAnswer}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (question.steps.isNotEmpty()) {
                        Divider()
                        Text(
                            text = "📋 解题步骤",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        question.steps.forEachIndexed { stepIndex, step ->
                            Text(
                                text = "${stepIndex + 1}. $step",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // 按钮区域
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("返回")
            }
            Button(
                onClick = onRestart,
                modifier = Modifier.weight(1f)
            ) {
                Text("再做一次")
            }
        }
    }
}

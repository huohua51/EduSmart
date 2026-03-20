package com.edusmart.app.feature.radar

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.edusmart.app.data.entity.PracticeRecordEntity
import com.edusmart.app.data.entity.WrongQuestionEntity
import com.edusmart.app.repository.WrongQuestionRepository
import com.edusmart.app.service.PracticeQuestionInfo
import com.edusmart.app.ui.theme.*
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
    val wrongQuestionRepository = remember { WrongQuestionRepository(database.wrongQuestionDao()) }

    // ✅ 获取用户信息
    val sp = remember { context.getSharedPreferences("auth", Context.MODE_PRIVATE) }
    val userId = remember { sp.getString("userId", "") ?: "" }
    val token = remember { sp.getString("token", "") ?: "" }

    var currentQuestionIndex by remember { mutableStateOf(0) }
    var userAnswers by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var showResult by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<Boolean>>(emptyList()) }

    val currentQuestion = questions.getOrNull(currentQuestionIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("举一反三练习 (${currentQuestionIndex + 1}/${questions.size})", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = PrimaryBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PageBackground
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(PageBackground)
        ) {
            if (showResult) {
                // 显示结果
                ResultScreen(
                    questions = questions,
                    userAnswers = userAnswers,
                    results = results,
                    onRestart = {
                        currentQuestionIndex = 0
                        userAnswers = emptyMap()
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
                            userAnswers = userAnswers + (currentQuestionIndex to answer)
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

                            // 保存练习记录并上传错题到云端
                            scope.launch {
                                questions.forEachIndexed { index, q ->
                                    val isCorrect = userAnswers[index] == q.correctAnswer

                                    val record = PracticeRecordEntity(
                                        id = UUID.randomUUID().toString(),
                                        questionText = q.questionText,
                                        questionType = q.questionType,
                                        correctAnswer = q.correctAnswer,
                                        userAnswer = userAnswers[index] ?: "",
                                        isCorrect = isCorrect,
                                        options = if (q.options.isNotEmpty()) JSONArray(q.options).toString() else null,
                                        steps = JSONArray(q.steps).toString(),
                                        knowledgePoints = JSONArray(q.knowledgePoints).toString(),
                                        sourceQuestionId = sourceQuestionId
                                    )
                                    database.practiceRecordDao().insertPracticeRecord(record)

                                    // ✅ 如果答错了，直接上传到云端错题本
                                    if (!isCorrect && userId.isNotEmpty() && token.isNotEmpty()) {
                                        val wrongQuestion = WrongQuestionEntity(
                                            id = UUID.randomUUID().toString(),
                                            questionText = q.questionText,
                                            answer = q.correctAnswer,
                                            userAnswer = userAnswers[index],
                                            wrongReason = "举一反三练习答错",
                                            steps = JSONArray(q.steps).toString(),
                                            knowledgePoints = JSONArray(q.knowledgePoints).toString(),
                                            analysis = "正确答案：${q.correctAnswer}",
                                            createdAt = System.currentTimeMillis(),
                                            reviewCount = 0,
                                            nextReviewTime = null
                                        )

                                        // 直接上传到云端
                                        try {
                                            wrongQuestionRepository.addWrongQuestionToCloud(
                                                userId = userId,
                                                token = token,
                                                wrongQuestion = wrongQuestion
                                            )
                                            android.util.Log.d("PracticeTest", "✅ 错题已上传到云端: ${q.questionText}")
                                        } catch (e: Exception) {
                                            android.util.Log.e("PracticeTest", "❌ 错题上传失败: ${e.message}")
                                        }
                                    }
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
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = CardBackground
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "第 ${questionIndex + 1} 题",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = question.questionText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )

                Divider()

                // 根据题目类型显示不同的答题界面
                when (question.questionType) {
                    "choice" -> {
                        // 选择题 - 使用Card代替FilterChip，提供更好的点击反馈
                        question.options.forEach { option ->
                            val optionLetter = option.substringBefore(".").trim()
                            val isSelected = userAnswer == optionLetter

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = if (isSelected) 4.dp else 1.dp,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onAnswerSelected(optionLetter) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) PrimaryBlue else Color.White
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 选项圆圈指示器
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(
                                                color = if (isSelected) Color.White else PrimaryBlue.copy(alpha = 0.2f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = PrimaryBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isSelected) Color.White else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
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
                            label = { Text("请输入答案", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                focusedLabelColor = PrimaryBlue,
                                cursorColor = PrimaryBlue
                            )
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
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PrimaryBlue
                    )
                ) {
                    Text("上一题", color = PrimaryBlue)
                }
            }

            if (isLastQuestion) {
                Button(
                    onClick = onSubmit,
                    modifier = Modifier.weight(1f),
                    enabled = hasAnswered,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        disabledContainerColor = SecondaryBlue.copy(alpha = 0.3f)
                    )
                ) {
                    Text("提交答案", color = Color.White)
                }
            } else {
                Button(
                    onClick = onNext,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    )
                ) {
                    Text("下一题", color = Color.White)
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
                containerColor = CardBackground
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
                    tint = if (score >= 60) PrimaryBlue else AccentCoral
                )
                Text(
                    text = "得分：$score 分",
                    style = MaterialTheme.typography.headlineMedium,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "答对 $correctCount / $totalCount 题",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
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
                        PrimaryBlue.copy(alpha = 0.1f)
                    else
                        AccentCoral.copy(alpha = 0.1f)
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
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (isCorrect) PrimaryBlue else AccentCoral
                        )
                    }

                    Text(
                        text = question.questionText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )

                    Divider()

                    Text(
                        text = "你的答案：$userAnswer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isCorrect) PrimaryBlue else AccentCoral,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "正确答案：${question.correctAnswer}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )

                    if (question.steps.isNotEmpty()) {
                        Divider()
                        Text(
                            text = "📋 解题步骤",
                            style = MaterialTheme.typography.titleSmall,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Bold
                        )
                        question.steps.forEachIndexed { stepIndex, step ->
                            Text(
                                text = "${stepIndex + 1}. $step",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
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
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PrimaryBlue
                )
            ) {
                Text("返回", color = PrimaryBlue)
            }
            Button(
                onClick = onRestart,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue
                )
            ) {
                Text("再做一次", color = Color.White)
            }
        }
    }
}

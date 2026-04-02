package com.edusmart.app.feature.radar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.edusmart.app.data.database.EduDatabase
import com.edusmart.app.data.entity.WrongQuestionEntity
import com.edusmart.app.repository.WrongQuestionRepository
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*
import com.edusmart.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrongQuestionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val database = remember { EduDatabase.getDatabase(context) }
    val repository = remember { WrongQuestionRepository(database.wrongQuestionDao()) }
    val viewModel = remember { WrongQuestionViewModel(repository) }

    // ✅ 从 SharedPreferences 获取用户认证信息
    val sp = remember { context.getSharedPreferences("auth", android.content.Context.MODE_PRIVATE) }
    val userId = remember { sp.getString("userId", "") ?: "" }
    val token = remember { sp.getString("token", "") ?: "" }
    
    // ⚠️ 如果未登录，显示提示并返回
    if (userId.isEmpty() || token.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("请先登录")
        }
        return
    }

    // ☁️ 从云端加载错题
    LaunchedEffect(userId, token) {
        if (userId.isNotEmpty() && token.isNotEmpty()) {
            viewModel.loadWrongQuestionsFromCloud(userId, token)
        }
    }

    val wrongQuestions by viewModel.wrongQuestions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedQuestion by remember { mutableStateOf<WrongQuestionEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf<WrongQuestionEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("错题本", color = TextPrimary) },
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
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (wrongQuestions.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = SecondaryBlue
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无错题",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "在拍照识题中添加错题后，会显示在这里",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(wrongQuestions) { question ->
                        WrongQuestionCard(
                            question = question,
                            onClick = { selectedQuestion = question },
                            onDelete = { showDeleteDialog = question }
                        )
                    }
                }
            }
        }
    }

    // 删除确认对话框
    showDeleteDialog?.let { question ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除错题") },
            text = { Text("确定要删除这道错题吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // ☁️ 使用云端删除
                        viewModel.deleteWrongQuestionFromCloud(userId, token, question)
                        showDeleteDialog = null
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            }
        )
    }

    // 错题详情对话框
    selectedQuestion?.let { question ->
        WrongQuestionDetailDialog(
            question = question,
            onDismiss = { selectedQuestion = null },
            onMarkReviewed = {
                viewModel.markAsReviewed(userId, token, question)
                selectedQuestion = null
            }
        )
    }
}

@Composable
fun WrongQuestionCard(
    question: WrongQuestionEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
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
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = question.questionText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = AccentCoral
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "复习次数: ${question.reviewCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryBlue
                )
                Text(
                    text = dateFormat.format(Date(question.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrongQuestionDetailDialog(
    question: WrongQuestionEntity,
    onDismiss: () -> Unit,
    onMarkReviewed: () -> Unit
) {
    val stepsList = remember(question.steps) {
        try {
            val array = JSONArray(question.steps)
            List(array.length()) { array.getString(it) }
        } catch (e: Exception) {
            listOf(question.steps)
        }
    }

    val knowledgePointsList = remember(question.knowledgePoints) {
        try {
            val array = JSONArray(question.knowledgePoints)
            List(array.length()) { array.getString(it) }
        } catch (e: Exception) {
            listOf(question.knowledgePoints)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "错题详情",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "关闭")
                    }
                }

                Divider()

                // 题目
                Column {
                    Text(
                        text = "题目",
                        style = MaterialTheme.typography.titleMedium,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = question.questionText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // 答案
                Column {
                    Text(
                        text = "答案",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = question.answer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // 解题步骤
                if (question.steps.isNotEmpty()) {
                    Column {
                        Text(
                            text = "解题步骤",
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        stepsList.forEachIndexed { index, step ->
                            Text(
                                text = "${index + 1}. $step",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                            )
                        }
                    }
                }

                // 知识点
                if (question.knowledgePoints.isNotEmpty()) {
                    Column {
                        Text(
                            text = "涉及知识点",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        knowledgePointsList.forEach { point ->
                            Text(
                                text = "• $point",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                            )
                        }
                    }
                }

                // 思路分析
                if (question.analysis.isNotEmpty()) {
                    Column {
                        Text(
                            text = "思路分析",
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = question.analysis,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Divider()

                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("关闭")
                    }
                    Button(
                        onClick = onMarkReviewed,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("标记已复习")
                    }
                }
            }
        }
    }
}

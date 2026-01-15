package com.edusmart.app.feature.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.edusmart.app.service.AIAnswerService
import com.edusmart.app.service.OCRService
import com.edusmart.app.ui.components.BeautyButton
import com.edusmart.app.ui.components.BeautyLoadingCard
import com.edusmart.app.ui.components.BeautyResultCard
import com.edusmart.app.ui.components.CameraPreview
import kotlinx.coroutines.launch

@Composable
fun ScanScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ocrService = remember { OCRService() }
    val aiAnswerService = remember { AIAnswerService() }
    
    var isScanning by remember { mutableStateOf(false) }
    var isRecognizing by remember { mutableStateOf(false) }
    var isAIGenerating by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<String?>(null) }
    var aiAnswer by remember { mutableStateOf<com.edusmart.app.service.AIAnswerResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var capturedImagePath by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 标题区域
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "拍照识题",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "对准题目拍照，AI自动识别并匹配答案",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            // 主要内容区域
            if (!isScanning && !isRecognizing && scanResult == null && errorMessage == null) {
                BeautyButton(
                    text = "📷 开始拍照",
                    onClick = { isScanning = true },
                    isPrimary = true
                )
            } else if (isScanning) {
                CameraPreview(
                    onImageCaptured = { imagePath ->
                        isScanning = false
                        isRecognizing = true
                        isAIGenerating = false
                        scanResult = null
                        aiAnswer = null
                        errorMessage = null
                        capturedImagePath = imagePath
                        
                        // 步骤1: OCR识别题目
                        scope.launch {
                            try {
                                val ocrResult = ocrService.recognizeText(imagePath)
                                scanResult = ocrResult
                                isRecognizing = false
                                
                                // 步骤2: AI生成答案
                                isAIGenerating = true
                                try {
                                    val answer = aiAnswerService.answerQuestionWithDoubao(
                                        questionText = ocrResult,
                                        imagePath = imagePath // 传递图片路径，AI可以识别公式
                                    )
                                    aiAnswer = answer
                                } catch (e: Exception) {
                                    errorMessage = "AI生成答案失败: ${e.message}"
                                }
                                isAIGenerating = false
                            } catch (e: Exception) {
                                errorMessage = "识别失败: ${e.message}"
                                isRecognizing = false
                                isAIGenerating = false
                            }
                        }
                    },
                    onCancel = {
                        isScanning = false
                    }
                )
            }
            
            // OCR识别中提示
            if (isRecognizing) {
                BeautyLoadingCard(
                    message = "📷 正在识别题目文字...",
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // AI生成答案中提示
            if (isAIGenerating) {
                BeautyLoadingCard(
                    message = "🤖 AI正在分析题目并生成答案...",
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // 错误提示
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(20.dp)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "❌ 识别失败",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        BeautyButton(
                            text = "重试",
                            onClick = { 
                                errorMessage = null
                                isScanning = true
                            },
                            isPrimary = false
                        )
                    }
                }
            }
            
            // OCR识别结果（题目文本）
            scanResult?.let { result ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "📝 识别到的题目",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = result,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            }
            
            // AI生成的答案
            aiAnswer?.let { answer ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(20.dp)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (answer.isError) 
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 答案
                        Column {
                            Text(
                                text = "✅ 答案",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = answer.answer,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Divider()
                        
                        // 解题步骤
                        if (answer.steps.isNotEmpty()) {
                            Column {
                                Text(
                                    text = "📋 解题步骤",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                answer.steps.forEachIndexed { index, step ->
                                    Text(
                                        text = "${index + 1}. $step",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                                    )
                                }
                            }
                            Divider()
                        }
                        
                        // 知识点
                        if (answer.knowledgePoints.isNotEmpty()) {
                            Column {
                                Text(
                                    text = "💡 涉及知识点",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                answer.knowledgePoints.forEach { point ->
                                    Text(
                                        text = "• $point",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                                    )
                                }
                            }
                            Divider()
                        }
                        
                        // 思路分析
                        if (answer.explanation.isNotEmpty()) {
                            Column {
                                Text(
                                    text = "🧠 思路分析",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = answer.explanation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 重新拍照按钮
                        BeautyButton(
                            text = "📷 重新拍照",
                            onClick = { 
                                scanResult = null
                                aiAnswer = null
                                capturedImagePath = null
                                isScanning = true
                            },
                            isPrimary = false
                        )
                    }
                }
            }
        }
    }
}


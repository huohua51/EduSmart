package com.edusmart.app.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * OCR识别服务
 * 使用Google ML Kit进行文字识别（支持中英文）
 * 
 * 备选方案：
 * 1. PaddleOCR - 离线识别，准确率高
 * 2. 百度OCR API - 云端识别，需要网络
 */
class OCRService {
    
    // 中文识别器（支持中英文混合）
    private val chineseRecognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    
    // 英文识别器（仅英文，速度更快）
    private val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    /**
     * 识别图片中的文字（使用中文识别器，支持中英文）
     * @param imagePath 图片文件路径
     * @return 识别出的文字内容
     */
    suspend fun recognizeText(imagePath: String): String = withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeFile(imagePath)
                ?: throw IllegalArgumentException("无法解码图片: $imagePath")
            
            // 限制图片大小，避免内存溢出
            val scaledBitmap = scaleBitmapIfNeeded(bitmap, 1024)
            
            val image = InputImage.fromBitmap(scaledBitmap, 0)
            
            // 使用suspendCancellableCoroutine将回调转换为协程
            suspendCancellableCoroutine { continuation ->
                chineseRecognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val result = StringBuilder()
                        for (block in visionText.textBlocks) {
                            result.append(block.text).append("\n")
                        }
                        continuation.resume(result.toString().trim())
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("OCR识别失败: ${e.message}", e)
        }
    }
    
    /**
     * 识别图片中的文字（使用英文识别器，仅英文）
     * @param imagePath 图片文件路径
     * @return 识别出的文字内容
     */
    suspend fun recognizeTextEnglish(imagePath: String): String = withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeFile(imagePath)
                ?: throw IllegalArgumentException("无法解码图片: $imagePath")
            
            val scaledBitmap = scaleBitmapIfNeeded(bitmap, 1024)
            val image = InputImage.fromBitmap(scaledBitmap, 0)
            
            suspendCancellableCoroutine { continuation ->
                latinRecognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val result = StringBuilder()
                        for (block in visionText.textBlocks) {
                            result.append(block.text).append("\n")
                        }
                        continuation.resume(result.toString().trim())
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("OCR识别失败: ${e.message}", e)
        }
    }
    
    /**
     * 识别题目并提取关键信息
     * @param imagePath 图片文件路径
     * @return 题目内容、科目、知识点等结构化信息
     */
    suspend fun recognizeQuestion(imagePath: String): QuestionRecognitionResult {
        val text = recognizeText(imagePath)
        
        // 使用简单的规则提取科目和知识点（实际应该使用AI模型）
        val subject = detectSubject(text)
        val knowledgePoints = extractKnowledgePoints(text, subject)
        val difficulty = estimateDifficulty(text)
        
        return QuestionRecognitionResult(
            content = text,
            subject = subject,
            knowledgePoints = knowledgePoints,
            difficulty = difficulty
        )
    }
    
    /**
     * 缩放图片以节省内存
     */
    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }
        
        val scale = maxSize.toFloat() / maxOf(width, height)
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * 检测科目（简单规则，实际应使用AI）
     */
    private fun detectSubject(text: String): String {
        val mathKeywords = listOf("方程", "函数", "几何", "代数", "三角", "导数", "积分", "x²", "√")
        val physicsKeywords = listOf("力", "速度", "加速度", "电路", "电压", "电流", "能量", "功")
        val chemistryKeywords = listOf("化学式", "分子", "原子", "反应", "元素", "化合物")
        val englishKeywords = listOf("the", "is", "are", "was", "were", "this", "that")
        
        val lowerText = text.lowercase()
        
        return when {
            mathKeywords.any { text.contains(it) } -> "数学"
            physicsKeywords.any { text.contains(it) } -> "物理"
            chemistryKeywords.any { text.contains(it) } -> "化学"
            englishKeywords.any { lowerText.contains(it) } -> "英语"
            else -> "未知"
        }
    }
    
    /**
     * 提取知识点（简单规则，实际应使用AI）
     */
    private fun extractKnowledgePoints(text: String, subject: String): List<String> {
        val points = mutableListOf<String>()
        
        when (subject) {
            "数学" -> {
                if (text.contains("方程") || text.contains("x²")) points.add("二次方程")
                if (text.contains("函数")) points.add("函数")
                if (text.contains("几何")) points.add("几何")
                if (text.contains("三角")) points.add("三角函数")
            }
            "物理" -> {
                if (text.contains("力")) points.add("力学")
                if (text.contains("电路")) points.add("电路")
                if (text.contains("能量")) points.add("能量")
            }
            "化学" -> {
                if (text.contains("反应")) points.add("化学反应")
                if (text.contains("分子")) points.add("分子结构")
            }
        }
        
        return if (points.isEmpty()) listOf("基础概念") else points
    }
    
    /**
     * 估算难度（1-5）
     */
    private fun estimateDifficulty(text: String): Int {
        val length = text.length
        val hasFormula = text.contains("=") || text.contains("+") || text.contains("-") || text.contains("×")
        val hasComplexSymbols = text.contains("²") || text.contains("√") || text.contains("∫")
        
        return when {
            hasComplexSymbols -> 5
            hasFormula && length > 100 -> 4
            hasFormula -> 3
            length > 50 -> 2
            else -> 1
        }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        chineseRecognizer.close()
        latinRecognizer.close()
    }
}

data class QuestionRecognitionResult(
    val content: String,
    val subject: String,
    val knowledgePoints: List<String>,
    val difficulty: Int
)

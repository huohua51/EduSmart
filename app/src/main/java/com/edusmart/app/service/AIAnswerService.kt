package com.edusmart.app.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.edusmart.app.config.SDKConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * AI答题服务
 * 支持豆包API和其他AI服务
 *
 * 功能：
 * 1. 识别题目（OCR + AI理解）
 * 2. 自动生成答案和解析
 * 3. 支持数学公式、复杂题目
 */
class AIAnswerService {

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            // 可以添加日志拦截器
            response
        }
        .build()

    /**
     * 使用豆包API回答问题
     *
     * @param questionText OCR识别出的题目文本
     * @param imagePath 题目图片路径（可选，用于图片识别）
     * @return AI生成的答案和解析
     */
    suspend fun answerQuestionWithDoubao(
        questionText: String,
        imagePath: String? = null
    ): AIAnswerResult = withContext(Dispatchers.IO) {
        try {
            // 构建提示词
            val prompt = buildPrompt(questionText)

            // 调用豆包API
            val response = callDoubaoAPI(prompt, imagePath)

            // 解析响应
            parseAIResponse(response)
        } catch (e: Exception) {
            AIAnswerResult(
                answer = "AI服务调用失败: ${e.message}",
                explanation = "请检查网络连接和API配置",
                steps = emptyList(),
                isError = true
            )
        }
    }

    /**
     * 构建AI提示词
     */
    private fun buildPrompt(questionText: String): String {
        return """
            你是一位经验丰富的教师，擅长解答各种学科题目。
            
            请仔细分析以下题目，并提供：
            1. 正确答案
            2. 详细的解题步骤
            3. 涉及的知识点
            4. 解题思路
            
            题目内容：
            $questionText
            
            请按照以下格式回答：
            【答案】
            [答案内容]
            
            【解题步骤】
            1. [步骤1]
            2. [步骤2]
            ...
            
            【知识点】
            - [知识点1]
            - [知识点2]
            
            【思路分析】
            [解题思路]
        """.trimIndent()
    }

    /**
     * 调用豆包API
     *
     * 注意：豆包API格式可能因版本而异，请根据实际API文档调整
     */
    private suspend fun callDoubaoAPI(
        prompt: String,
        imagePath: String?
    ): String = withContext(Dispatchers.IO) {
        val apiKey = SDKConfig.DOUBAO_SECRET_KEY
        if (apiKey == "your-doubao-api-key") {
            throw IllegalArgumentException("请先在 SDKConfig.kt 中配置豆包API密钥")
        }

        // 豆包API端点（请根据实际API文档调整）
        // 方式1: 使用OpenAI兼容格式
        val url = "https://ark.cn-beijing.volces.com/api/v3/chat/completions"

        // 方式2: 如果豆包使用不同的端点，请修改这里
        // val url = "https://open.volcengine.com/api/v1/chat/completions"

        // 构建请求体
        val requestBody = buildDoubaoRequestBody(prompt, imagePath)

        // 创建请求
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        // 执行请求
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: ""
            throw IOException("API调用失败: ${response.code} ${response.message}\n$errorBody")
        }

        response.body?.string() ?: throw IOException("响应体为空")
    }

    /**
     * 构建豆包API请求体
     *
     * 注意：根据豆包API实际格式调整
     */
    private fun buildDoubaoRequestBody(
        prompt: String,
        imagePath: String?
    ): RequestBody {
        val json = JSONObject()

        // 模型名称（请替换为您的模型ID）
        val modelId = SDKConfig.DOUBAO_MODEL_ID
        if (modelId != "your-doubao-model-id") {
            json.put("model", modelId)
        } else {
            // 默认模型（请根据实际API文档调整）
            json.put("model", "doubao-pro-4k") // 示例，请替换
        }

        // 消息列表
        val messages = org.json.JSONArray()
        val message = JSONObject()

        if (imagePath != null) {
            // 如果提供了图片，使用多模态输入（如果API支持）
            try {
                val content = org.json.JSONArray()

                // 文本部分
                val textPart = JSONObject()
                textPart.put("type", "text")
                textPart.put("text", prompt)
                content.put(textPart)

                // 图片部分（如果API支持图片输入）
                val imagePart = JSONObject()
                imagePart.put("type", "image_url")
                val imageUrl = JSONObject()
                imageUrl.put("url", "data:image/jpeg;base64,${imageToBase64(imagePath)}")
                imagePart.put("image_url", imageUrl)
                content.put(imagePart)

                message.put("role", "user")
                message.put("content", content)
            } catch (e: Exception) {
                // 如果图片处理失败，只发送文本
                message.put("role", "user")
                message.put("content", "$prompt\n\n（题目图片已包含，请根据题目内容解答）")
            }
        } else {
            // 纯文本输入
            message.put("role", "user")
            message.put("content", prompt)
        }

        messages.put(message)
        json.put("messages", messages)

        // 其他参数
        json.put("temperature", 0.7)
        json.put("max_tokens", 2000)
        json.put("stream", false)

        val jsonString = json.toString()
        return jsonString.toRequestBody("application/json".toMediaType())
    }

    /**
     * 将图片转换为Base64
     */
    private fun imageToBase64(imagePath: String): String {
        val bitmap = BitmapFactory.decodeFile(imagePath)
            ?: throw IllegalArgumentException("无法读取图片: $imagePath")

        // 压缩图片以减小大小
        val scaledBitmap = scaleBitmapIfNeeded(bitmap, 1024)

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()

        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * 缩放图片
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
     * 解析AI响应
     */
    private fun parseAIResponse(responseJson: String): AIAnswerResult {
        try {
            val json = JSONObject(responseJson)

            // 解析豆包API响应格式（根据实际API调整）
            val choices = json.getJSONArray("choices")
            if (choices.length() == 0) {
                throw IOException("AI响应中没有内容")
            }

            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.getJSONObject("message")
            val content = message.getString("content")

            // 解析答案、步骤等
            return parseAnswerContent(content)
        } catch (e: Exception) {
            // 如果解析失败，返回原始内容
            return AIAnswerResult(
                answer = responseJson,
                explanation = "AI响应解析失败，显示原始内容",
                steps = emptyList(),
                isError = false
            )
        }
    }

    /**
     * 解析答案内容（从AI返回的文本中提取结构化信息）
     */
    private fun parseAnswerContent(content: String): AIAnswerResult {
        val answer = extractSection(content, "【答案】", "【解题步骤】")
        val explanation = extractSection(content, "【思路分析】", null)

        val stepsText = extractSection(content, "【解题步骤】", "【知识点】")
        val steps = stepsText.split("\n")
            .filter { it.trim().isNotEmpty() && it.matches(Regex("^\\d+\\..*")) }
            .map { it.replace(Regex("^\\d+\\.\\s*"), "").trim() }

        val knowledgePoints = extractSection(content, "【知识点】", "【思路分析】")
            .split("\n")
            .filter { it.trim().isNotEmpty() && it.startsWith("-") }
            .map { it.replace(Regex("^-\\s*"), "").trim() }

        return AIAnswerResult(
            answer = answer.ifEmpty { "未找到答案" },
            explanation = explanation.ifEmpty { "未找到解析" },
            steps = steps,
            knowledgePoints = knowledgePoints,
            isError = false
        )
    }

    /**
     * 提取文本中的某个部分
     */
    private fun extractSection(text: String, startMarker: String, endMarker: String?): String {
        val startIndex = text.indexOf(startMarker)
        if (startIndex == -1) return ""

        val contentStart = startIndex + startMarker.length
        val contentEnd = if (endMarker != null) {
            val endIndex = text.indexOf(endMarker, contentStart)
            if (endIndex == -1) text.length else endIndex
        } else {
            text.length
        }

        return text.substring(contentStart, contentEnd).trim()
    }

    /**
     * 使用OpenAI兼容API（如果豆包不支持，可以使用其他服务）
     */
    suspend fun answerQuestionWithOpenAI(
        questionText: String,
        imagePath: String? = null
    ): AIAnswerResult = withContext(Dispatchers.IO) {
        // 类似实现，使用OpenAI格式的API
        // 可以支持Claude、文心一言等兼容OpenAI格式的API
        answerQuestionWithDoubao(questionText, imagePath) // 暂时使用豆包实现
    }
}

/**
 * AI答案结果
 */
data class AIAnswerResult(
    val answer: String,
    val explanation: String,
    val steps: List<String> = emptyList(),
    val knowledgePoints: List<String> = emptyList(),
    val isError: Boolean = false
)


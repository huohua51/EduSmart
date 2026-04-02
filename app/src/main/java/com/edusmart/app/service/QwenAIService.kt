package com.edusmart.app.service

import android.util.Base64
import com.edusmart.app.config.SDKConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 通义千问AI服务
 * 用于题目分析和答案生成
 */
class QwenAIService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * 分析题目并生成答案
     * @param questionText OCR识别的题目文本
     * @param imagePath 题目图片路径（可选，用于多模态识别）
     * @return 答案结果
     */
    suspend fun analyzeQuestion(
        questionText: String,
        imagePath: String? = null
    ): QuestionAnalysisResult = withContext(Dispatchers.IO) {
        try {
            val prompt = buildPrompt(questionText)
            val responseBody = callTongyiAPI(prompt)
            val messageContent = parseTongyiResponse(responseBody)
            parseAnalysisResult(messageContent, questionText)
        } catch (e: Exception) {
            QuestionAnalysisResult(
                success = false,
                errorMessage = "分析失败: ${e.message}",
                questionText = questionText
            )
        }
    }

    /**
     * 通用聊天方法
     * @param prompt 提示词
     * @return AI响应内容
     */
    suspend fun chat(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val responseBody = callTongyiAPI(prompt)
            parseTongyiResponse(responseBody)
        } catch (e: Exception) {
            throw Exception("AI调用失败: ${e.message}", e)
        }
    }

    /**
     * 多轮对话聊天方法
     * @param messages 对话历史，格式为 [{"role": "user", "content": "..."}, {"role": "assistant", "content": "..."}]
     * @return AI响应内容
     */
    suspend fun chatWithHistory(messages: List<Map<String, String>>): String = withContext(Dispatchers.IO) {
        try {
            val responseBody = callTongyiAPIWithHistory(messages)
            parseTongyiResponse(responseBody)
        } catch (e: Exception) {
            throw Exception("AI调用失败: ${e.message}", e)
        }
    }

    /**
     * 使用对话历史调用通义千问API
     */
    private suspend fun callTongyiAPIWithHistory(messages: List<Map<String, String>>): String = withContext(Dispatchers.IO) {
        val apiKey = SDKConfig.TONGYI_API_KEY

        if (apiKey == "your-tongyi-api-key" || apiKey.isEmpty()) {
            throw IllegalArgumentException("请先在 SDKConfig.kt 中配置通义千问API密钥")
        }

        val url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation"

        val json = JSONObject()
        json.put("model", "qwen-turbo")
        json.put("input", JSONObject().apply {
            put("messages", JSONArray().apply {
                messages.forEach { msg ->
                    put(JSONObject().apply {
                        put("role", msg["role"] ?: "user")
                        put("content", msg["content"] ?: "")
                    })
                }
            })
        })
        json.put("parameters", JSONObject().apply {
            put("result_format", "message")
        })

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                throw Exception("API调用失败 (${response.code}): ${response.message}\n$errorBody")
            }

            val responseBody = response.body?.string()
            if (responseBody.isNullOrEmpty()) {
                throw Exception("API响应体为空")
            }

            responseBody
        } catch (e: Exception) {
            throw Exception("通义千问API调用异常: ${e.message}", e)
        }
    }

    /**
     * 调用通义千问API
     */
    private suspend fun callTongyiAPI(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = SDKConfig.TONGYI_API_KEY

        if (apiKey == "your-tongyi-api-key" || apiKey.isEmpty()) {
            throw IllegalArgumentException("请先在 SDKConfig.kt 中配置通义千问API密钥")
        }

        val url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation"

        val json = JSONObject()
        json.put("model", "qwen-turbo")
        json.put("input", JSONObject().apply {
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        })
        json.put("parameters", JSONObject().apply {
            put("result_format", "message")
        })

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                throw Exception("API调用失败 (${response.code}): ${response.message}\n$errorBody")
            }

            val responseBody = response.body?.string()
            if (responseBody.isNullOrEmpty()) {
                throw Exception("API响应体为空")
            }

            responseBody
        } catch (e: Exception) {
            throw Exception("通义千问API调用异常: ${e.message}", e)
        }
    }

    /**
     * 解析通义千问API响应
     */
    private fun parseTongyiResponse(responseBody: String): String {
        try {
            val json = JSONObject(responseBody)

            if (json.has("output")) {
                val output = json.getJSONObject("output")
                if (output.has("text")) {
                    return output.getString("text").trim()
                }
                if (output.has("choices")) {
                    val choices = output.getJSONArray("choices")
                    if (choices.length() > 0) {
                        val firstChoice = choices.getJSONObject(0)
                        if (firstChoice.has("message")) {
                            val message = firstChoice.getJSONObject("message")
                            return message.getString("content").trim()
                        }
                    }
                }
            }

            if (json.has("choices")) {
                val choices = json.getJSONArray("choices")
                if (choices.length() > 0) {
                    val firstChoice = choices.getJSONObject(0)
                    if (firstChoice.has("message")) {
                        val message = firstChoice.getJSONObject("message")
                        return message.getString("content").trim()
                    }
                }
            }

            return responseBody
        } catch (e: Exception) {
            return responseBody
        }
    }
    /**
     * 构建提示词
     */
    private fun buildPrompt(questionText: String): String {
        return """
你是一个专业的教育助手，请分析以下题目并给出详细解答。

题目：
$questionText

请按照以下格式回答：
【答案】
（给出简洁的答案）

【解题步骤】
1. （第一步）
2. （第二步）
...

【知识点】
- （相关知识点1）
- （相关知识点2）

【思路分析】
（详细的解题思路和方法）

如果无法解答，请在【答案】部分说明"暂时无法解答此题目"。
        """.trimIndent()
    }

    /**
     * 构建多模态内容（文本+图片）
     */
    private fun buildMultimodalContent(questionText: String, imagePath: String): JSONArray {
        return JSONArray().apply {
            // 添加文本提示
            put(JSONObject().apply {
                put("text", buildPrompt(questionText))
            })
            // 添加图片
            put(JSONObject().apply {
                val imageBase64 = encodeImageToBase64(imagePath)
                put("image", "data:image/jpeg;base64,$imageBase64")
            })
        }
    }

    /**
     * 将图片编码为Base64
     */
    private fun encodeImageToBase64(imagePath: String): String {
        val imageFile = File(imagePath)
        val bytes = imageFile.readBytes()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * 解析AI返回的分析结果
     */
    private fun parseAnalysisResult(content: String, questionText: String): QuestionAnalysisResult {
        try {
            // 提取各个部分
            val answer = cleanMarkdown(extractSection(content, "【答案】", listOf("【解题步骤】", "【知识点】", "【思路分析】")))
            val steps = extractSteps(content).map { cleanMarkdown(it) }
            val knowledgePoints = extractKnowledgePoints(content).map { cleanMarkdown(it) }
            val analysis = cleanMarkdown(extractSection(content, "【思路分析】", emptyList()))

            // 检查是否无法解答
            val cannotAnswer = answer.contains("无法解答") || answer.contains("无法回答")

            return QuestionAnalysisResult(
                success = !cannotAnswer,
                questionText = questionText,
                answer = answer.ifEmpty { "暂时无法解答此题目" },
                steps = steps,
                knowledgePoints = knowledgePoints,
                analysis = analysis,
                errorMessage = if (cannotAnswer) "AI暂时无法解答此题目" else null
            )
        } catch (e: Exception) {
            return QuestionAnalysisResult(
                success = false,
                errorMessage = "解析结果失败: ${e.message}",
                questionText = questionText
            )
        }
    }

    /**
     * 清理Markdown格式符号
     */
    private fun cleanMarkdown(text: String): String {
        return text
            .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")  // 移除加粗 **text**
            .replace(Regex("\\*(.+?)\\*"), "$1")        // 移除斜体 *text*
            .replace(Regex("__(.+?)__"), "$1")          // 移除加粗 __text__
            .replace(Regex("_(.+?)_"), "$1")            // 移除斜体 _text_
            .replace(Regex("`(.+?)`"), "$1")            // 移除代码 `code`
            .replace(Regex("\\[(.+?)\\]\\(.+?\\)"), "$1") // 移除链接 [text](url)
            .trim()
    }

    /**
     * 提取指定章节的内容
     */
    private fun extractSection(content: String, startMarker: String, endMarkers: List<String>): String {
        val startIndex = content.indexOf(startMarker)
        if (startIndex == -1) return ""

        val contentAfterStart = content.substring(startIndex + startMarker.length)

        var endIndex = contentAfterStart.length
        for (endMarker in endMarkers) {
            val index = contentAfterStart.indexOf(endMarker)
            if (index != -1 && index < endIndex) {
                endIndex = index
            }
        }

        return contentAfterStart.substring(0, endIndex).trim()
    }

    /**
     * 提取解题步骤
     */
    private fun extractSteps(content: String): List<String> {
        val stepsSection = extractSection(content, "【解题步骤】", listOf("【知识点】", "【思路分析】"))
        if (stepsSection.isEmpty()) return emptyList()

        return stepsSection.lines()
            .filter { it.trim().matches(Regex("^\\d+\\..*")) }
            .map { it.trim().replaceFirst(Regex("^\\d+\\.\\s*"), "") }
    }

    /**
     * 提取知识点
     */
    private fun extractKnowledgePoints(content: String): List<String> {
        val kpSection = extractSection(content, "【知识点】", listOf("【思路分析】"))
        if (kpSection.isEmpty()) return emptyList()

        return kpSection.lines()
            .filter { it.trim().startsWith("-") || it.trim().startsWith("•") }
            .map { it.trim().replaceFirst(Regex("^[-•]\\s*"), "") }
    }
}

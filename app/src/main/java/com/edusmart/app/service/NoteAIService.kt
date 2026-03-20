package com.edusmart.app.service

import com.edusmart.app.config.SDKConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * 笔记AI服务
 * 提供笔记润色、总结、智能标题生成等功能
 */
class NoteAIService {

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            response
        }
        .build()

    /**
     * 润色笔记内容
     * 改善表达、修正语法、优化结构
     */
    suspend fun polishNote(content: String, subject: String? = null): String = withContext(Dispatchers.IO) {
        android.util.Log.d("NoteAIService", "========== 开始AI润色 ==========")
        android.util.Log.d("NoteAIService", "内容长度: ${content.length} 字符")
        android.util.Log.d("NoteAIService", "科目: ${subject ?: "未指定"}")

        try {
            val prompt = buildPolishPrompt(content, subject)
            android.util.Log.d("NoteAIService", "提示词长度: ${prompt.length} 字符")

            val response = callAIAPI(prompt)
            android.util.Log.d("NoteAIService", "API响应长度: ${response.length} 字符")

            val result = parseSimpleResponse(response)
            android.util.Log.d("NoteAIService", "✅ 润色成功，结果长度: ${result.length} 字符")
            android.util.Log.d("NoteAIService", "========== AI润色完成 ==========")
            result
        } catch (e: Exception) {
            android.util.Log.e("NoteAIService", "❌ 润色失败", e)
            android.util.Log.e("NoteAIService", "错误类型: ${e.javaClass.simpleName}")
            android.util.Log.e("NoteAIService", "错误信息: ${e.message}")
            e.printStackTrace()
            "润色失败: ${e.message}\n\n原始内容：\n$content"
        }
    }

    /**
     * 总结笔记内容
     * 生成摘要、提取要点
     */
    suspend fun summarizeNote(content: String, subject: String? = null): NoteSummary = withContext(Dispatchers.IO) {
        try {
            val prompt = buildSummaryPrompt(content, subject)
            val response = callAIAPI(prompt)
            parseSummaryResponse(response, content)
        } catch (e: Exception) {
            NoteSummary(
                summary = "总结失败: ${e.message}",
                keyPoints = emptyList(),
                tags = emptyList()
            )
        }
    }

    /**
     * 生成智能标题
     * 根据笔记内容自动生成合适的标题
     */
    suspend fun generateTitle(content: String, subject: String? = null): String = withContext(Dispatchers.IO) {
        try {
            val prompt = buildTitlePrompt(content, subject)
            val response = callAIAPI(prompt)
            parseSimpleResponse(response).trim().take(50) // 限制标题长度
        } catch (e: Exception) {
            "笔记标题"
        }
    }

    /**
     * 增强知识点提取
     * 更智能地提取和整理知识点
     * 使用通义千问提取
     */
    suspend fun extractKnowledgePoints(content: String, subject: String? = null): List<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildKnowledgePointsPrompt(content, subject)
            val response = callAIAPI(prompt)
            parseKnowledgePointsResponse(response)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * AI自动生成科目
     * 根据笔记内容自动判断科目
     * 使用通义千问生成
     */
    suspend fun generateSubject(content: String, title: String? = null): String = withContext(Dispatchers.IO) {
        try {
            val prompt = buildSubjectPrompt(content, title)
            val response = callAIAPI(prompt)
            parseSubjectResponse(response)
        } catch (e: Exception) {
            android.util.Log.e("NoteAIService", "生成科目失败", e)
            "其他"
        }
    }

    /**
     * 笔记问答
     * 基于笔记内容回答问题
     */
    suspend fun answerQuestion(noteContent: String, question: String): String = withContext(Dispatchers.IO) {
        try {
            val prompt = buildQAPrompt(noteContent, question)
            val response = callAIAPI(prompt)
            parseSimpleResponse(response)
        } catch (e: Exception) {
            "无法回答问题: ${e.message}"
        }
    }

    /**
     * 构建润色提示词
     */
    private fun buildPolishPrompt(content: String, subject: String?): String {
        val subjectContext = if (subject != null) "科目：$subject\n" else ""
        return """
            你是一位专业的笔记润色助手。请对以下笔记内容进行深度润色和扩展，要求：
            1. 保持原意和核心观点不变
            2. 改善表达，使语言更清晰、准确、生动
            3. 修正语法错误和错别字
            4. 优化段落结构，使逻辑更清晰
            5. 保持学术性和专业性
            6. 【重要】丰富内容细节，对关键概念进行适当展开和说明
            7. 【重要】添加必要的背景信息、解释和例证，使内容更加完整和深入
            8. 【重要】如果原内容过于简略，请根据主题和上下文进行合理的扩展和补充
            9. 使用更丰富的词汇和表达方式，避免简单重复
            10. 确保润色后的内容比原文更加详细、充实、有深度
            
            $subjectContext
            笔记内容：
            $content
            
            请直接返回润色和扩展后的完整内容，内容应该比原文更详细、更丰富。不要添加任何说明或标记，只返回润色后的内容。
        """.trimIndent()
    }

    /**
     * 构建总结提示词
     */
    private fun buildSummaryPrompt(content: String, subject: String?): String {
        val subjectContext = if (subject != null) "科目：$subject\n" else ""
        return """
            你是一位专业的笔记总结助手。请对以下笔记内容进行总结，要求：
            1. 生成一段简洁的摘要（100-200字）
            2. 提取3-5个关键要点
            3. 提取2-4个相关标签
            
            $subjectContext
            笔记内容：
            $content
            
            请按照以下格式返回：
            【摘要】
            [摘要内容]
            
            【关键要点】
            1. [要点1]
            2. [要点2]
            ...
            
            【标签】
            - [标签1]
            - [标签2]
            ...
        """.trimIndent()
    }

    /**
     * 构建标题生成提示词
     */
    private fun buildTitlePrompt(content: String, subject: String?): String {
        val subjectContext = if (subject != null) "科目：$subject\n" else ""
        return """
            请根据以下笔记内容，生成一个简洁、准确的标题（不超过20字）。
            
            $subjectContext
            笔记内容：
            ${content.take(500)}  // 只取前500字用于生成标题
            
            请直接返回标题，不要添加任何说明或引号。
        """.trimIndent()
    }

    /**
     * 构建知识点提取提示词
     */
    private fun buildKnowledgePointsPrompt(content: String, subject: String?): String {
        val subjectContext = if (subject != null) "科目：$subject\n" else ""
        return """
            请从以下笔记内容中提取关键知识点，要求：
            1. 【重要】必须提取恰好3个核心知识点，不多不少
            2. 每个知识点用一句话概括，简洁明了
            3. 按重要性从高到低排序
            4. 知识点应该覆盖笔记的核心内容
            5. 如果笔记内容较少，请根据主题和上下文合理推断和补充知识点
            
            $subjectContext
            笔记内容：
            $content
            
            请严格按照以下格式返回，必须返回3个知识点：
            - [知识点1]
            - [知识点2]
            - [知识点3]
            
            注意：必须返回3个知识点，不要多也不要少。
        """.trimIndent()
    }

    /**
     * 构建科目生成提示词
     */
    private fun buildSubjectPrompt(content: String, title: String?): String {
        val titleContext = if (title != null && title.isNotBlank()) "标题：$title\n" else ""
        return """
            请根据以下笔记内容，判断并生成最合适的科目名称。
            
            $titleContext
            笔记内容：
            ${content.take(1000)}  // 只取前1000字用于判断科目
            
            要求：
            1. 返回一个简洁的科目名称（2-6个字），例如：数学、语文、英语、物理、化学、生物、历史、地理、政治等
            2. 如果无法判断，返回"其他"
            3. 只返回科目名称，不要添加任何说明、引号或其他文字
            4. 如果是跨学科内容，选择最主要的科目
            
            请直接返回科目名称：
        """.trimIndent()
    }
    
    /**
     * 解析科目响应
     */
    private fun parseSubjectResponse(response: String): String {
        val cleaned = response.trim()
            .replace("\"", "")
            .replace("'", "")
            .replace("科目：", "")
            .replace("科目:", "")
            .replace("科目是：", "")
            .replace("科目是:", "")
            .replace("建议科目：", "")
            .replace("建议科目:", "")
            .trim()
        
        // 如果响应为空或过长，返回默认值
        if (cleaned.isEmpty() || cleaned.length > 10) {
            return "其他"
        }
        
        return cleaned
    }
    
    /**
     * 构建问答提示词
     */
    private fun buildQAPrompt(noteContent: String, question: String): String {
        return """
            你是一位学习助手。请根据以下笔记内容回答问题。

            笔记内容：
            $noteContent

            问题：
            $question

            要求：
            1. 基于笔记内容回答，不要编造信息
            2. 如果笔记中没有相关信息，请说明
            3. 回答要准确、简洁
            4. 【重要】不要使用任何 Markdown 格式，包括：标题符号（#）、加粗符号（**）、列表符号（- 或 *）、代码块符号（```）等
            5. 【重要】使用纯文本格式回答，可以使用换行和空格来组织内容
            6. 如果需要列举要点，使用数字序号（1. 2. 3.）或者直接用换行分段

            请直接返回答案，不要添加"答案："等前缀。
        """.trimIndent()
    }

    /**
     * 统一的API调用方法
     * 优先使用通义千问，如果未配置则使用豆包
     */
    private suspend fun callAIAPI(prompt: String): String = withContext(Dispatchers.IO) {
        // 使用通义千问
        android.util.Log.d("NoteAIService", "使用通义千问API...")
        return@withContext callTongyiAPI(prompt)

    }

    /**
     * 调用通义千问API
     * 使用OpenAI兼容模式
     */
    private suspend fun callTongyiAPI(prompt: String): String = withContext(Dispatchers.IO) {
        android.util.Log.d("NoteAIService", "========== 调用通义千问API ==========")

        val apiKey = SDKConfig.TONGYI_API_KEY
        android.util.Log.d("NoteAIService", "API Key: ${apiKey.take(8)}...${apiKey.takeLast(4)}")

        if (apiKey == "your-tongyi-api-key" || apiKey.isEmpty()) {
            android.util.Log.e("NoteAIService", "❌ 通义千问API Key未配置")
            throw IllegalArgumentException("请先在 SDKConfig.kt 中配置通义千问API密钥")
        }

        // 通义千问使用DashScope标准API端点
        // 关键修复：使用正确的域名 aliyuncs.com（不是 aliyun.com）
        val url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation"
        android.util.Log.d("NoteAIService", "API URL: $url")

        // 构建请求体（DashScope标准格式）
        // 关键修复：使用 messages 数组（不是 prompt 字符串）
        val json = JSONObject()
        json.put("model", "qwen-turbo")  // 通义千问模型
        json.put("input", JSONObject().apply {
            // ✅ 正确：使用 messages 数组
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        })
        json.put("parameters", JSONObject().apply {
            put("temperature", 0.8)  // 稍微提高温度，让输出更有创造性，内容更丰富
            put("max_tokens", 3000)  // 增加最大token数，允许生成更详细的内容
        })

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        android.util.Log.d("NoteAIService", "请求体: $json")

        val timeoutClient = OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        try {
            android.util.Log.d("NoteAIService", "发送HTTP请求...")
            val response = timeoutClient.newCall(request).execute()
            android.util.Log.d("NoteAIService", "响应状态码: ${response.code}")

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                android.util.Log.e("NoteAIService", "❌ 通义千问API调用失败")
                android.util.Log.e("NoteAIService", "状态码: ${response.code}")
                android.util.Log.e("NoteAIService", "错误响应: ${errorBody.take(500)}")

                // 如果是404，尝试OpenAI兼容端点
                if (response.code == 404) {
                    android.util.Log.w("NoteAIService", "⚠️ DashScope标准端点失败，尝试OpenAI兼容端点...")
                    return@withContext callTongyiAPIOpenAICompatible(prompt)
                }

                throw IOException("通义千问API调用失败 (${response.code}): ${response.message}\n${errorBody.take(500)}")
            }

            val responseBody = response.body?.string()
            if (responseBody.isNullOrEmpty()) {
                throw IOException("通义千问API响应体为空")
            }

            android.util.Log.d("NoteAIService", "✅ 通义千问API调用成功")
            android.util.Log.d("NoteAIService", "响应体长度: ${responseBody.length} 字符")

            // 解析DashScope格式的响应
            val parsedResponse = parseTongyiResponse(responseBody)
            android.util.Log.d("NoteAIService", "解析后的响应: ${parsedResponse.take(200)}...")
            parsedResponse
        } catch (e: Exception) {
            android.util.Log.e("NoteAIService", "❌ 通义千问API调用异常", e)
            throw IOException("通义千问API调用异常: ${e.message}", e)
        }
    }

    /**
     * 使用OpenAI兼容格式调用通义千问API（备用方案）
     */
    private suspend fun callTongyiAPIOpenAICompatible(prompt: String): String = withContext(Dispatchers.IO) {
        android.util.Log.d("NoteAIService", "========== 使用OpenAI兼容格式调用通义千问 ==========")

        val apiKey = SDKConfig.TONGYI_API_KEY
        // 关键修复：使用正确的域名 aliyuncs.com（不是 aliyun.com）
        val url = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
        android.util.Log.d("NoteAIService", "API URL: $url")

        // 构建请求体（OpenAI兼容格式）
        val json = JSONObject()
        json.put("model", "qwen-turbo")
        json.put("messages", org.json.JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })
        })
        json.put("temperature", 0.8)  // 稍微提高温度，让输出更有创造性
        json.put("max_tokens", 3000)  // 增加最大token数，允许生成更详细的内容

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())

        val timeoutClient = OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        try {
            val response = timeoutClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                android.util.Log.e("NoteAIService", "❌ OpenAI兼容格式也失败")
                android.util.Log.e("NoteAIService", "状态码: ${response.code}")
                throw IOException("通义千问API调用失败 (${response.code}): ${response.message}\n${errorBody.take(500)}")
            }

            val responseBody = response.body?.string()
            if (responseBody.isNullOrEmpty()) {
                throw IOException("通义千问API响应体为空")
            }

            android.util.Log.d("NoteAIService", "✅ OpenAI兼容格式调用成功")
            parseTongyiResponse(responseBody)
        } catch (e: Exception) {
            android.util.Log.e("NoteAIService", "❌ OpenAI兼容格式调用异常", e)
            throw IOException("通义千问API调用异常: ${e.message}", e)
        }
    }

    /**
     * 解析通义千问API响应
     * DashScope标准格式：{"output":{"text":"..."}} 或 {"output":{"choices":[{"message":{"content":"..."}}]}}
     * OpenAI兼容格式：{"choices":[{"message":{"content":"..."}}]}
     */
    private fun parseTongyiResponse(responseBody: String): String {
        try {
            val json = JSONObject(responseBody)

            // 尝试DashScope标准格式（text-generation）
            if (json.has("output")) {
                val output = json.getJSONObject("output")
                if (output.has("text")) {
                    return output.getString("text").trim()
                }
                // 或者choices格式
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

            // 尝试OpenAI格式
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

            // 如果都不匹配，返回原始内容
            android.util.Log.w("NoteAIService", "⚠️ 无法解析通义千问响应格式，返回原始内容")
            android.util.Log.d("NoteAIService", "响应内容: $responseBody")
            return responseBody
        } catch (e: Exception) {
            android.util.Log.e("NoteAIService", "❌ 解析通义千问响应失败", e)
            android.util.Log.d("NoteAIService", "原始响应: $responseBody")
            return responseBody
        }
    }

    /**
     * 调用豆包API
     */
//    private suspend fun callDoubaoAPI(prompt: String): String = withContext(Dispatchers.IO) {
//        android.util.Log.d("NoteAIService", "========== 调用豆包API ==========")
//
//        val apiKey = SDKConfig.DOUBAO_API_KEY
//        android.util.Log.d("NoteAIService", "API Key: ${apiKey.take(8)}...${apiKey.takeLast(4)}")
//        android.util.Log.d("NoteAIService", "API Key长度: ${apiKey.length} 字符")
//        android.util.Log.d("NoteAIService", "API Key格式: ${if (apiKey.startsWith("sk-")) "sk-开头" else if (apiKey.matches(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE))) "UUID格式" else "未知格式"}")
//
//        if (apiKey == "your-doubao-api-key" || apiKey.isEmpty()) {
//            android.util.Log.e("NoteAIService", "❌ API Key未配置")
//            throw IllegalArgumentException("请先在 SDKConfig.kt 中配置豆包API密钥")
//        }
//
//        // 验证API Key格式
//        val isValidFormat = apiKey.startsWith("sk-") || apiKey.matches(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE))
//        if (!isValidFormat) {
//            android.util.Log.w("NoteAIService", "⚠️ API Key格式可能不正确。豆包API Key应该是 'sk-' 开头或UUID格式。当前Key: ${apiKey.take(10)}...")
//        }
//
//        // 检查API Key是否有空格或特殊字符
//        if (apiKey.contains(" ") || apiKey.contains("\n") || apiKey.contains("\r") || apiKey.contains("\t")) {
//            android.util.Log.e("NoteAIService", "❌ API Key包含空格或换行符，这会导致认证失败！")
//            android.util.Log.e("NoteAIService", "请检查 SDKConfig.kt 中的 DOUBAO_API_KEY 是否有多余的空格或换行")
//        }
//
//        // 尝试使用Bot API，如果失败则回退到模型API
//        // 根据官方文档，Bot API使用不同的端点：/api/v3/bots
//        val botId = SDKConfig.DOUBAO_BOT_ID
//        val useBotAPI = botId != "your-bot-id" && botId.isNotEmpty() && botId.startsWith("bot-")
//
//        // Bot API和模型API使用相同的端点
//        // 根据OpenAI兼容接口的常见模式，Bot和模型使用相同的端点
//        // 区别只在于model参数：Bot API使用Bot ID，模型API使用模型名称
//        // 统一使用 /api/v3/chat/completions 端点
//        val url = "https://ark.cn-beijing.volces.com/api/v3/chat/completions"
//        android.util.Log.d("NoteAIService", "API URL: $url")
//        android.util.Log.d("NoteAIService", "尝试使用Bot API: $useBotAPI")
//        if (useBotAPI) {
//            android.util.Log.d("NoteAIService", "Bot ID: $botId")
//        } else {
//            android.util.Log.d("NoteAIService", "将使用模型API（DOUBAO_MODEL_ID）")
//        }
//
//        val requestBody = buildDoubaoRequestBody(prompt)
//        android.util.Log.d("NoteAIService", "请求体已构建")
//        android.util.Log.d("NoteAIService", "使用Bot ID: $botId")
//
//        // 记录请求体内容（用于调试）
//        // 注意：RequestBody 不能直接读取，我们会在 buildDoubaoRequestBody 中记录
//
//        // 创建带超时的客户端
//        val timeoutClient = OkHttpClient.Builder()
//            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
//            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
//            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
//            .build()
//
//        // 构建请求，添加详细的请求日志
//        // 确保API Key没有多余的空格
//        val cleanApiKey = apiKey.trim()
//        val authHeader = "Bearer $cleanApiKey"
//
//        android.util.Log.d("NoteAIService", "Authorization Header: Bearer ${cleanApiKey.take(8)}...${cleanApiKey.takeLast(4)}")
//        android.util.Log.d("NoteAIService", "Authorization Header长度: ${authHeader.length} 字符")
//
//        val requestBuilder = Request.Builder()
//            .url(url)
//            .header("Authorization", authHeader)
//            .header("Content-Type", "application/json")
//
//        // 如果是Bot API，可能需要额外的header
//        if (useBotAPI) {
//            // 某些平台可能需要额外的header来标识Bot调用
//            // requestBuilder.header("X-Bot-Id", botId)  // 如果需要的话
//        }
//
//        val request = requestBuilder.post(requestBody).build()
//
//        // 记录完整的请求信息（用于调试）
//        android.util.Log.d("NoteAIService", "========== 请求详情 ==========")
//        android.util.Log.d("NoteAIService", "URL: $url")
//        android.util.Log.d("NoteAIService", "Method: POST")
//        android.util.Log.d("NoteAIService", "Headers:")
//        request.headers.forEach { header ->
//            if (header.first == "Authorization") {
//                android.util.Log.d("NoteAIService", "  ${header.first}: Bearer ${apiKey.take(8)}...${apiKey.takeLast(4)}")
//            } else {
//                android.util.Log.d("NoteAIService", "  ${header.first}: ${header.second}")
//            }
//        }
//        android.util.Log.d("NoteAIService", "发送HTTP请求...")
//        val startTime = System.currentTimeMillis()
//
//        try {
//            val response = timeoutClient.newCall(request).execute()
//            val duration = System.currentTimeMillis() - startTime
//            android.util.Log.d("NoteAIService", "收到HTTP响应，耗时: ${duration}ms")
//            android.util.Log.d("NoteAIService", "响应状态码: ${response.code}")
//
//            if (!response.isSuccessful) {
//                val errorBody = response.body?.string() ?: ""
//                android.util.Log.e("NoteAIService", "❌ API调用失败")
//                android.util.Log.e("NoteAIService", "状态码: ${response.code}")
//                android.util.Log.e("NoteAIService", "错误响应: $errorBody")
//
//                // 如果是404错误且使用了Bot API，尝试回退到模型API
//                if (response.code == 404 && useBotAPI) {
//                    android.util.Log.w("NoteAIService", "⚠️ Bot API失败（404），尝试回退到模型API...")
//                    android.util.Log.w("NoteAIService", "Bot ID可能不存在或无权访问，使用模型API重试")
//
//                    // 回退到模型API：使用模型ID而不是Bot ID
//                    return@withContext callDoubaoAPIWithModel(prompt)
//                }
//
//                // 尝试解析错误信息
//                try {
//                    if (errorBody.isNotEmpty()) {
//                        val errorJson = JSONObject(errorBody)
//                        val errorObj = errorJson.optJSONObject("error")
//                        if (errorObj != null) {
//                            val errorCode = errorObj.optString("code", "")
//                            val errorMessage = errorObj.optString("message", "")
//                            android.util.Log.e("NoteAIService", "错误代码: $errorCode")
//                            android.util.Log.e("NoteAIService", "错误消息: $errorMessage")
//
//                            if (errorCode == "AuthenticationError" || response.code == 401) {
//                                throw IOException("API认证失败: $errorMessage\n\n" +
//                                        "请检查 SDKConfig.kt 中的 DOUBAO_API_KEY 是否正确。\n" +
//                                        "豆包API Key格式应该是 'sk-' 开头或UUID格式。\n" +
//                                        "请访问 https://console.volcengine.com/ 获取正确的API Key。")
//                            }
//
//                            // 如果是智能体不存在，尝试回退到模型API
//                            if (errorCode == "InvalidEndpointOrModel.NotFound" && response.code == 404 && useBotAPI) {
//                                android.util.Log.w("NoteAIService", "⚠️ 智能体不存在，尝试回退到模型API...")
//                                return@withContext callDoubaoAPIWithModel(prompt)
//                            }
//                        }
//                    }
//                } catch (e: Exception) {
//                    android.util.Log.w("NoteAIService", "解析错误响应失败", e)
//                }
//
//                throw IOException("API调用失败 (${response.code}): ${response.message}\n$errorBody")
//            }
//
//            val responseBody = response.body?.string()
//            if (responseBody.isNullOrEmpty()) {
//                android.util.Log.e("NoteAIService", "❌ API响应体为空")
//                throw IOException("API响应体为空")
//            }
//
//            android.util.Log.d("NoteAIService", "✅ API调用成功")
//            android.util.Log.d("NoteAIService", "响应体长度: ${responseBody.length} 字符")
//
//            // 记录响应体内容（截断长内容）
//            if (responseBody.length > 500) {
//                android.util.Log.d("NoteAIService", "响应体（前500字符）: ${responseBody.take(500)}...")
//            } else {
//                android.util.Log.d("NoteAIService", "响应体: $responseBody")
//            }
//
//            android.util.Log.d("NoteAIService", "========== API调用完成 ==========")
//            responseBody
//        } catch (e: java.net.SocketTimeoutException) {
//            android.util.Log.e("NoteAIService", "❌ API调用超时", e)
//            throw IOException("API调用超时，请检查网络连接", e)
//        } catch (e: java.net.UnknownHostException) {
//            android.util.Log.e("NoteAIService", "❌ 无法连接到服务器", e)
//            throw IOException("无法连接到服务器，请检查网络连接", e)
//        } catch (e: IOException) {
//            android.util.Log.e("NoteAIService", "❌ IO异常", e)
//            throw e
//        } catch (e: Exception) {
//            android.util.Log.e("NoteAIService", "❌ 未知异常", e)
//            e.printStackTrace()
//            throw IOException("API调用异常: ${e.message}", e)
//        }
//    }

    /**
     * 使用模型API调用（回退方案）
     * 当Bot API失败时使用
     * 尝试多个可能的模型名称
     */
//    private suspend fun callDoubaoAPIWithModel(prompt: String): String = withContext(Dispatchers.IO) {
//        android.util.Log.d("NoteAIService", "========== 使用模型API（回退方案）==========")
//
//        val apiKey = SDKConfig.DOUBAO_API_KEY
//
//        // 尝试多个可能的模型名称
//        val possibleModels = listOf(
//            SDKConfig.DOUBAO_MODEL_ID,  // 用户配置的模型ID
//            "doubao-pro-4k",           // 标准模型
//            "doubao-lite-4k",          // 轻量模型
//            "doubao-pro-32k",          // 长文本模型
//        ).filter { it != "your-doubao-model-id" && it.isNotEmpty() }
//
//        android.util.Log.d("NoteAIService", "将尝试以下模型: $possibleModels")
//
//        var lastException: Exception? = null
//
//        for (modelId in possibleModels) {
//            try {
//                android.util.Log.d("NoteAIService", "尝试使用模型: $modelId")
//                return@withContext callDoubaoAPIWithSpecificModel(prompt, modelId)
//            } catch (e: Exception) {
//                android.util.Log.w("NoteAIService", "模型 $modelId 失败: ${e.message}")
//                lastException = e
//                // 继续尝试下一个模型
//            }
//        }
//
//        // 所有模型都失败
//        android.util.Log.e("NoteAIService", "❌ 所有模型都失败")
//        throw IOException("所有模型都不可用。请检查：\n" +
//                "1. 在控制台创建模型端点并获取端点ID\n" +
//                "2. 确认API Key有访问模型的权限\n" +
//                "3. 确认服务已开通\n" +
//                "最后错误: ${lastException?.message}", lastException)
//    }

    /**
     * 使用指定模型ID调用API
     */
//    private suspend fun callDoubaoAPIWithSpecificModel(prompt: String, modelId: String): String = withContext(Dispatchers.IO) {
//        android.util.Log.d("NoteAIService", "使用模型ID: $modelId")
//
//        val apiKey = SDKConfig.DOUBAO_API_KEY
//        val url = "https://ark.cn-beijing.volces.com/api/v3/chat/completions"
//
//        // 构建使用模型ID的请求体
//        val json = JSONObject()
//        json.put("model", modelId)
//
//        val messages = org.json.JSONArray()
//        val message = JSONObject()
//        message.put("role", "user")
//        message.put("content", prompt)
//        messages.put(message)
//        json.put("messages", messages)
//
//        json.put("temperature", 0.7)
//        json.put("max_tokens", 2000)
//        json.put("stream", false)
//
//        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
//
//        val timeoutClient = OkHttpClient.Builder()
//            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
//            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
//            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
//            .build()
//
//        val request = Request.Builder()
//            .url(url)
//            .header("Authorization", "Bearer $apiKey")
//            .header("Content-Type", "application/json")
//            .post(requestBody)
//            .build()
//
//        try {
//            val response = timeoutClient.newCall(request).execute()
//
//            if (!response.isSuccessful) {
//                val errorBody = response.body?.string() ?: ""
//                android.util.Log.e("NoteAIService", "❌ 模型 $modelId 调用失败")
//                android.util.Log.e("NoteAIService", "状态码: ${response.code}")
//                android.util.Log.e("NoteAIService", "错误响应: $errorBody")
//                throw IOException("模型API调用失败 (${response.code}): ${response.message}\n$errorBody")
//            }
//
//            val responseBody = response.body?.string()
//            if (responseBody.isNullOrEmpty()) {
//                throw IOException("API响应体为空")
//            }
//
//            android.util.Log.d("NoteAIService", "✅ 模型 $modelId 调用成功")
//            android.util.Log.d("NoteAIService", "========== 模型API调用完成 ==========")
//            responseBody
//        } catch (e: Exception) {
//            android.util.Log.e("NoteAIService", "❌ 模型 $modelId 调用异常", e)
//            throw IOException("模型API调用异常: ${e.message}", e)
//        }
//    }

    /**
     * 构建豆包API请求体
     * 使用智能体（Bot）API，而不是直接模型API
     */
//    private fun buildDoubaoRequestBody(prompt: String): RequestBody {
//        val json = JSONObject()
//
//        // 使用智能体ID，而不是模型ID
//        val botId = SDKConfig.DOUBAO_BOT_ID
//        if (botId != "your-bot-id" && botId.isNotEmpty()) {
//            json.put("model", botId) // Bot API 使用 bot ID 作为 model 参数
//        } else {
//            // 如果没有配置 Bot ID，回退到模型ID（兼容旧配置）
//            val modelId = SDKConfig.DOUBAO_MODEL_ID
//            if (modelId != "your-doubao-model-id") {
//                json.put("model", modelId)
//            } else {
//                json.put("model", "doubao-pro-4k") // 默认模型
//            }
//        }
//
//        // Bot API 使用 messages 数组，可以包含 system 和 user 消息
//        val messages = org.json.JSONArray()
//
//        // 添加 system 消息（可选，智能体可能已有系统提示词）
//        // 如果需要覆盖系统提示词，可以取消下面的注释
//        /*
//        val systemMessage = JSONObject()
//        systemMessage.put("role", "system")
//        systemMessage.put("content", "你是一位专业的笔记AI助手，擅长润色笔记、生成总结、提取知识点和生成标题。")
//        messages.put(systemMessage)
//        */
//
//        // 添加 user 消息
//        val userMessage = JSONObject()
//        userMessage.put("role", "user")
//        userMessage.put("content", prompt)
//        messages.put(userMessage)
//
//        json.put("messages", messages)
//
//        json.put("temperature", 0.7)
//        json.put("max_tokens", 2000)
//        json.put("stream", false)
//
//        val jsonString = json.toString()
//        return jsonString.toRequestBody("application/json".toMediaType())
//    }

    /**
     * 解析简单响应（只返回文本内容）
     */
    private fun parseSimpleResponse(responseJson: String): String {
        try {
            val json = JSONObject(responseJson)
            val choices = json.getJSONArray("choices")
            if (choices.length() == 0) {
                throw IOException("AI响应中没有内容")
            }

            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.getJSONObject("message")
            return message.getString("content").trim()
        } catch (e: Exception) {
            // 如果解析失败，返回原始内容
            return responseJson
        }
    }

    /**
     * 解析总结响应
     */
    private fun parseSummaryResponse(responseJson: String, originalContent: String): NoteSummary {
        try {
            val content = parseSimpleResponse(responseJson)

            val summary = extractSection(content, "【摘要】", "【关键要点】")
                .ifEmpty { content.take(200) } // 如果没有找到摘要，取前200字

            val keyPointsText = extractSection(content, "【关键要点】", "【标签】")
            val keyPoints = keyPointsText.split("\n")
                .filter { it.trim().isNotEmpty() && it.matches(Regex("^\\d+\\..*")) }
                .map { it.replace(Regex("^\\d+\\.\\s*"), "").trim() }
                .take(5)

            val tagsText = extractSection(content, "【标签】", null)
            val tags = tagsText.split("\n")
                .filter { it.trim().isNotEmpty() && it.startsWith("-") }
                .map { it.replace(Regex("^-\\s*"), "").trim() }
                .take(4)

            return NoteSummary(
                summary = summary.ifEmpty { "无法生成摘要" },
                keyPoints = keyPoints.ifEmpty { listOf("未提取到关键要点") },
                tags = tags
            )
        } catch (e: Exception) {
            return NoteSummary(
                summary = "总结解析失败: ${e.message}",
                keyPoints = emptyList(),
                tags = emptyList()
            )
        }
    }

    /**
     * 解析知识点响应
     */
    private fun parseKnowledgePointsResponse(responseJson: String): List<String> {
        try {
            val content = parseSimpleResponse(responseJson)
            val points = content.split("\n")
                .filter { it.trim().isNotEmpty() && (it.startsWith("-") || it.startsWith("•") || it.matches(Regex("^\\d+[.、].*"))) }
                .map {
                    // 支持多种格式：- 知识点、• 知识点、1. 知识点、1、知识点
                    it.replace(Regex("^[-•]\\s*"), "")
                        .replace(Regex("^\\d+[.、]\\s*"), "")
                        .trim()
                }
                .filter { it.isNotEmpty() }
                .take(3)  // 固定取3个知识点

            // 如果解析出的知识点少于3个，尝试其他解析方式
            if (points.size < 3) {
                val alternativePoints = content.split(Regex("[\\n\\r]+"))
                    .filter { it.trim().isNotEmpty() && !it.startsWith("请") && !it.startsWith("注意") }
                    .map { it.trim() }
                    .filter { it.length > 5 && it.length < 100 } // 合理的知识点长度
                    .take(3)

                return if (alternativePoints.size >= 3) alternativePoints else points
            }

            return points
        } catch (e: Exception) {
            android.util.Log.e("NoteAIService", "解析知识点失败", e)
            return emptyList()
        }
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
}

/**
 * 笔记总结结果
 */
data class NoteSummary(
    val summary: String,
    val keyPoints: List<String>,
    val tags: List<String>
)


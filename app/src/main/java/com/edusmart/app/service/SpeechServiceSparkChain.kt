package com.edusmart.app.service

import android.content.Context
import android.util.Log
import com.edusmart.app.config.SDKConfig
import com.iflytek.sparkchain.core.LogLvl
import com.iflytek.sparkchain.core.SparkChain
import com.iflytek.sparkchain.core.SparkChainConfig
import com.iflytek.sparkchain.core.asr.ASR
import com.iflytek.sparkchain.core.asr.AsrCallbacks
import com.iflytek.sparkchain.core.asr.ASR.ASRResult
import com.iflytek.sparkchain.core.asr.ASR.ASRError
import com.iflytek.sparkchain.core.tts.OnlineTTS
import com.iflytek.sparkchain.core.tts.TTSCallbacks
import com.iflytek.sparkchain.core.tts.TTS.TTSEvent
import com.iflytek.sparkchain.core.tts.TTS.TTSResult
import com.iflytek.sparkchain.core.tts.TTS.TTSError

import com.iflytek.cloud.SpeechConstant
import com.iflytek.cloud.SpeechEvaluator
import com.iflytek.cloud.EvaluatorListener
import com.iflytek.cloud.EvaluatorResult
import com.iflytek.cloud.SpeechError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaCodec
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// 新增：讯飞星火LLM相关 - 根据官方接口修改
import com.iflytek.sparkchain.core.LLM
import com.iflytek.sparkchain.core.LLMCallbacks
import com.iflytek.sparkchain.core.LLMConfig
import com.iflytek.sparkchain.core.LLMResult
import com.iflytek.sparkchain.core.LLMError
import com.iflytek.sparkchain.core.LLMEvent
import com.iflytek.sparkchain.core.LLMFactory
import com.iflytek.sparkchain.core.LLMOutput
import java.io.File
import com.iflytek.cloud.SpeechUtility




/**
 * SparkChain和MSC语音服务实现
 * 集成 ASR (智能笔记模块使用的语音识别), TTS (弃用) 和 ISE (口语使用的语音评测) 以及 LLM 对话功能（口语使用的大模型对话）
 */
class SpeechServiceSparkChain(private val context: Context) {

    private var mAsr: ASR? = null
    private var mTts: OnlineTTS? = null
    private var audioTrack: AudioTrack? = null
    private var isRecognizing = false

    private var mLLM: LLM? = null  // LLM实例
    private var isLLMInitialized = false // 新增：标记LLM是否已初始化
    private val TAG = "SpeechServiceSparkChain"

    // LLM回调接口
    private var llmCallbacks: LLMCallbacks? = null

    // ========== 修复方案 ==========
// 在 SpeechServiceSparkChain.kt 的 companion object 中添加 ISE 初始化

    companion object {
        private const val TAG = "SpeechServiceSparkChain"
        private var isLibraryLoaded = false
        private var isSDKInitialized = false
        private var isIseInitialized = false  // ★ 新增: ISE初始化标记

        /**
         * 加载本地库
         */
        private fun loadNativeLibraries() {
            if (isLibraryLoaded) return
            try {
                System.loadLibrary("SparkChain")
                System.loadLibrary("spark")
                isLibraryLoaded = true
                Log.d(TAG, "本地库加载完成")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "加载本地库失败: ${e.message}")
                throw e
            }
        }

        /**
         * ★★★ 新增: 初始化讯飞语音评测SDK (ISE) ★★★
         */
        private fun initializeIse(context: Context) {
            if (isIseInitialized) {
                Log.d(TAG, "ISE已初始化，跳过")
                return
            }

            try {
                // 使用 SpeechUtility 初始化 MSC (讯飞语音云)
                val params = StringBuffer()
                params.append("appid=${SDKConfig.XUNFEI_APP_ID}")
                params.append(",")
                // 设置日志级别
                params.append(SpeechConstant.ENGINE_MODE + "=" + SpeechConstant.MODE_MSC)

                Log.d(TAG, ">>> 开始初始化 ISE SDK...")
                Log.d(TAG, "APP_ID: ${SDKConfig.XUNFEI_APP_ID}")

                // 初始化讯飞MSC
                val ret = SpeechUtility.createUtility(context, params.toString())

                if (ret == null) {
                    Log.e(TAG, "❌ ISE SDK初始化失败: createUtility返回null")
                    Log.e(TAG, "请检查:")
                    Log.e(TAG, "  1. APP_ID是否正确: ${SDKConfig.XUNFEI_APP_ID}")
                    Log.e(TAG, "  2. 是否添加了讯飞MSC库 (Msc.jar 或 msc.aar)")
                    Log.e(TAG, "  3. 是否添加了so库 (libmsc.so)")
                } else {
                    Log.d(TAG, "✅✅✅ ISE SDK初始化成功!")
                    isIseInitialized = true

                    // 验证ISE评估器是否可用
                    val testIse = SpeechEvaluator.createEvaluator(context, null)
                    if (testIse == null) {
                        Log.e(TAG, "⚠️ 警告: ISE SDK初始化成功，但评估器创建失败")
                    } else {
                        Log.d(TAG, "✅ ISE评估器测试通过")
                        testIse.destroy()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ ISE初始化异常", e)
            }
        }

        /**
         * ★★★ 修改: 统一初始化方法，同时初始化 SparkChain 和 ISE ★★★
         */
        fun initialize(context: Context) {
            Log.d(TAG, "========================================")
            Log.d(TAG, ">>> 开始初始化讯飞SDK...")

            // 1. 初始化 SparkChain (用于LLM)
            if (!isSDKInitialized) {
                try {
                    loadNativeLibraries()
                    val sparkChainConfig = SparkChainConfig.builder()
                        .appID(SDKConfig.XUNFEI_APP_ID)
                        .apiKey(SDKConfig.XUNFEI_API_KEY)
                        .apiSecret(SDKConfig.XUNFEI_API_SECRET)
                        .logPath("${context.getExternalFilesDir(null)?.absolutePath}/SparkChain.log")
                        .logLevel(LogLvl.VERBOSE.getValue())

                    val ret = SparkChain.getInst().init(context, sparkChainConfig)
                    if (ret == 0) {
                        Log.d(TAG, "✅ SparkChain SDK初始化成功")
                        isSDKInitialized = true
                    } else {
                        Log.e(TAG, "❌ SparkChain SDK初始化失败: $ret")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ SparkChain初始化异常", e)
                }
            }

            // 2. ★★★ 初始化 ISE (用于语音评测) ★★★
            initializeIse(context)

            Log.d(TAG, "========================================")
            Log.d(TAG, "SDK初始化完成:")
            Log.d(TAG, "  - SparkChain (LLM): ${if (isSDKInitialized) "✅" else "❌"}")
            Log.d(TAG, "  - ISE (评测): ${if (isIseInitialized) "✅" else "❌"}")
            Log.d(TAG, "========================================")
        }

        /**
         * SDK逆初始化
         */
        fun uninitialize() {
            if (isSDKInitialized) {
                SparkChain.getInst().unInit()
                isSDKInitialized = false
                Log.d(TAG, "SparkChain SDK逆初始化完成")
            }

            // ISE使用的SpeechUtility不需要显式逆初始化
            isIseInitialized = false
        }
    }



    // ========== 新增：LLM 大语言模型对话功能（根据官方接口修改） ==========

    /**
     * 初始化 LLM 引擎 - 根据流程图修改
     */
    private fun initLLM(): Boolean {
        if (isLLMInitialized && mLLM != null) return true

        try {
            // 根据流程图使用LLMFactory创建LLM实例
            mLLM = LLMFactory.textGeneration()

            if (mLLM == null) {
                Log.e(TAG, "LLMFactory.textGeneration() 返回null")
                return false
            }

            isLLMInitialized = true
            Log.d(TAG, "LLM 初始化成功")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "LLM 初始化失败", e)
            return false
        }
    }

    /**
     * 注册LLM回调 - 根据流程图修改
     */
    private fun registerLLMCallbacks(callbacks: LLMCallbacks) {
        mLLM?.registerLLMCallbacks(callbacks)
        llmCallbacks = callbacks
        Log.d(TAG, "LLM回调注册成功")
    }

    /**
     * 同步调用LLM
     * @param prompt 输入的提示词
     * @return LLM的响应结果
     */
    fun syncChat(prompt: String): String {
        return try {
            if (!initLLM()) {
                return "LLM初始化失败"
            }

            // 同步调用，根据流程图：LLMOutput syncOutput = chat_llm.run(question)
            val output: LLMOutput? = mLLM?.run(prompt)

            if (output == null) {
                Log.e(TAG, "同步调用返回null")
                return "调用失败"
            }

            val result = output.content
            Log.d(TAG, "同步调用结果: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "同步调用异常: ${e.message}", e)
            "调用异常: ${e.message}"
        }
    }

    /**
     * 为场景生成开场白（带学习目的）
     * @param sceneName 场景名称（如"机场"、"面试"等）
     * @param sceneDescription 场景描述
     * @param learningPurpose 学习目的
     * @return AI生成的开场白
     */
    suspend fun generateSceneOpeningWithPurpose(
        sceneName: String,
        sceneDescription: String,
        learningPurpose: String
    ): String = withContext(Dispatchers.IO) {
        val purposeContext = if (learningPurpose.isNotBlank()) {
            "The student's learning purpose is: $learningPurpose. Please tailor your language and tone accordingly."
        } else ""

        val prompt = """
            You are an English speaking coach. Generate a natural and engaging opening message for a conversation practice scenario.
            
            Scenario: $sceneName
            Description: $sceneDescription
            $purposeContext
            
            Requirements:
            1. Start the conversation directly as if you are in that scenario (e.g., you are the hotel receptionist, restaurant server, interviewer, etc.)
            2. Keep it concise (2-3 sentences maximum)
            3. Ask an open-ended question to encourage the student to respond
            4. Use natural, everyday English appropriate for the scenario and learning purpose
            5. Be welcoming and encouraging
            
            Generate ONLY the opening message in English, nothing else.
        """.trimIndent()

        return@withContext try {
            val result = syncChat(prompt)
            Log.d(TAG, "场景开场白生成成功: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "生成场景开场白失败: ${e.message}", e)
            "Hello! I'm your $sceneName coach. Let's practice English together!"
        }
    }

    /**
     * 为场景生成开场白（向后兼容）
     */
    suspend fun generateSceneOpening(sceneName: String, sceneDescription: String): String {
        return generateSceneOpeningWithPurpose(sceneName, sceneDescription, "")
    }

    /**
     * 为自定义话题生成开场白（带学习目的）
     * @param topic 话题内容
     * @param learningPurpose 学习目的
     * @return AI生成的开场白
     */
    suspend fun generateTopicOpeningWithPurpose(
        topic: String,
        learningPurpose: String
    ): String = withContext(Dispatchers.IO) {
        val purposeContext = if (learningPurpose.isNotBlank()) {
            "The student's learning purpose is: $learningPurpose. Please tailor your language and tone accordingly."
        } else ""

        val prompt = """
            You are an English speaking coach. Generate a natural and engaging opening message for a conversation about a specific topic.
            
            Topic: $topic
            $purposeContext
            
            Requirements:
            1. Start with a brief, friendly greeting
            2. Introduce the topic naturally
            3. Ask an interesting, thought-provoking question about the topic to start the conversation
            4. Keep it concise (2-3 sentences maximum)
            5. Use natural, conversational English appropriate for the learning purpose
            
            Generate ONLY the opening message in English, nothing else.
        """.trimIndent()

        return@withContext try {
            val result = syncChat(prompt)
            Log.d(TAG, "话题开场白生成成功: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "生成话题开场白失败: ${e.message}", e)
            "Great! Let's talk about $topic. What would you like to say about it?"
        }
    }

    /**
     * 为自定义话题生成开场白（向后兼容）
     */
    suspend fun generateTopicOpening(topic: String): String {
        return generateTopicOpeningWithPurpose(topic, "")
    }

    /**
     * AI对话：生成口语练习回复（同步版）
     */
    fun generateAIReplySync(
        userMessage: String,
        conversationHistory: List<String> = emptyList(),
        contextDescription: String = "General English conversation"
    ): String {
        val historyContext = conversationHistory.joinToString("\n")

        val systemPrompt = """
            You are a helpful and professional English speaking coach.
            
            $contextDescription
            
            Tasks:
            1. Respond naturally to the user's last message in English.
            2. If the user's English has grammar issues, briefly and gently correct them.
            3. Keep the conversation going related to the current topic.
            4. Keep your responses concise and conversational (2-4 sentences).
            
            Conversation History:
            $historyContext
            
            User's current message: "$userMessage"
            
            Please provide a natural response in English only.
        """.trimIndent()

        return syncChat(systemPrompt)
    }

    /**
     * 异步调用LLM
     * @param prompt 输入的提示词
     * @param onResult 结果回调 (result, isComplete)
     * @param onError 错误回调
     */
    fun asyncChat(
        prompt: String,
        onResult: (String, Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!initLLM()) {
            onError("LLM初始化失败")
            return
        }

        val callbacks = object : LLMCallbacks {
            private val resultBuilder = StringBuilder()
            private var isCompleted = false // 添加标记，防止重复完成

            override fun onLLMResult(result: LLMResult?, usrTag: Any?) {
                if (result == null || isCompleted) return // 添加检查

                val content = result.content ?: ""
                resultBuilder.append(content)

                // 流式返回中间结果
                onResult(content, false)

                // status: 2 代表最后一包
                if (result.status == 2) {
                    isCompleted = true // 标记为已完成
                    val finalResult = resultBuilder.toString()
                    Log.d(TAG, "异步调用完成: $finalResult")
                    onResult(finalResult, true)
                }
            }

            override fun onLLMError(error: LLMError?, usrTag: Any?) {
                if (isCompleted) return // 添加检查

                isCompleted = true
                val errorMsg = error?.errMsg ?: "未知错误"
                Log.e(TAG, "异步调用错误: $errorMsg")
                onError(errorMsg)
            }

            override fun onLLMEvent(event: LLMEvent?, usrTag: Any?) {
                Log.d(TAG, "LLM事件: ${event?.eventID}")
            }
        }

        // 注册回调
        registerLLMCallbacks(callbacks)

        // 异步调用
        val ret = mLLM?.arun(prompt)
        if (ret != 0) {
            Log.e(TAG, "异步调用启动失败: $ret")
            onError("启动失败: $ret")
        }
    }

    /**
     * AI对话：生成口语练习回复（异步版，带学习目的）- 协程封装
     */
    suspend fun generateAIReplyWithPurpose(
        userMessage: String,
        conversationHistory: List<String> = emptyList(),
        contextDescription: String = "General English conversation",
        learningPurpose: String = ""
    ): String = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val historyContext = conversationHistory.joinToString("\n")

            val purposeContext = if (learningPurpose.isNotBlank()) {
                "The student's learning purpose is: $learningPurpose. Please tailor your responses and corrections accordingly."
            } else ""

            val systemPrompt = """
                You are a helpful and professional English speaking coach.
                
                $contextDescription
                $purposeContext
                
                Tasks:
                1. Respond naturally to the user's last message in English.
                2. If the user's English has grammar issues, briefly and gently correct them.
                3. Keep the conversation going related to the current topic.
                4. Keep your responses concise and conversational (2-4 sentences).
                5. Adjust your language complexity based on the learning purpose.
                
                Conversation History:
                $historyContext
                
                User's current message: "$userMessage"
                
                Please provide a natural response in English only.
            """.trimIndent()

            asyncChat(
                prompt = systemPrompt,
                onResult = { result, isComplete ->
                    if (isComplete) {
                        continuation.resume(result)
                    }
                },
                onError = { error ->
                    Log.e(TAG, "AI对话生成失败: $error")
                    continuation.resume("Sorry, I'm having trouble connecting to my brain right now. Could you try again?")
                }
            )
        }
    }

    /**
     * AI对话：生成口语练习回复（异步版，向后兼容）- 协程封装
     */
    suspend fun generateAIReplyAsync(
        userMessage: String,
        conversationHistory: List<String> = emptyList(),
        contextDescription: String = "General English conversation"
    ): String {
        return generateAIReplyWithPurpose(userMessage, conversationHistory, contextDescription, "")
    }

    /**
     * 新增：翻译英文为中文
     */
    suspend fun translateToChinese(englishText: String): String = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val prompt = """
                请将以下英文翻译成简洁的中文，只输出翻译结果，不要有任何解释或其他内容：
                
                "$englishText"
            """.trimIndent()

            asyncChat(
                prompt = prompt,
                onResult = { result, isComplete ->
                    if (isComplete) {
                        continuation.resume(result)
                    }
                },
                onError = { error ->
                    Log.e(TAG, "翻译失败: $error")
                    continuation.resume("")
                }
            )
        }
    }

    /**
     * 翻译：英文翻译为中文（向后兼容）
     */
    suspend fun translateToEnglish(englishText: String): String {
        return translateToChinese(englishText)
    }

    /**
     * 新增：为AI消息生成参考回复
     */
    suspend fun generateSuggestedReply(
        aiMessage: String,
        conversationHistory: List<String> = emptyList(),
        contextDescription: String = "General English conversation",
        learningPurpose: String = ""
    ): String = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val historyContext = conversationHistory.joinToString("\n")

            val purposeContext = if (learningPurpose.isNotBlank()) {
                "The student's learning purpose is: $learningPurpose."
            } else ""

            val prompt = """
                You are an English speaking coach. The AI coach just said the following to the student:
                
                "$aiMessage"
                
                $contextDescription
                $purposeContext
                
                Conversation History:
                $historyContext
                
                Please provide 1-2 example responses that would be appropriate and natural replies to what the AI coach said.
                The responses should:
                1. Be natural and conversational
                2. Be appropriate for the context and learning purpose
                3. Demonstrate good English usage
                4. Keep it concise (1-2 sentences per example)
                
                Format: Just provide the example response(s) in English, separated by " OR " if you provide multiple options.
                Do not include any explanations or labels like "Example 1:" or "Response:".
            """.trimIndent()

            asyncChat(
                prompt = prompt,
                onResult = { result, isComplete ->
                    if (isComplete) {
                        continuation.resume(result)
                    }
                },
                onError = { error ->
                    Log.e(TAG, "生成参考回复失败: $error")
                    continuation.resume("")
                }
            )
        }
    }

    /**
     * 新增：根据学习目的生成场景列表
     * @param purposeName 学习目的名称（如"雅思考试"、"商务交流"等）
     * @param purposeDescription 学习目的描述
     * @return 生成的场景列表
     */
    suspend fun generateScenesForPurpose(
        purposeName: String,
        purposeDescription: String
    ): List<com.edusmart.app.feature.speaking.Scene> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val prompt = """
                You are an English speaking coach designing practice scenarios.
                
                Learning Purpose: $purposeName
                Description: $purposeDescription
                
                Please generate 5-6 specific, practical English speaking practice scenarios tailored to this learning purpose.
                
                For each scenario, provide:
                1. A concise scenario name (2-4 words, in Chinese if the purpose is Chinese-focused, otherwise English)
                2. A brief description (10-15 words describing what the scenario involves)
                3. An appropriate emoji that represents the scenario
                
                Format your response EXACTLY as follows (each scenario on one line):
                Emoji|Scenario Name|Description
                
                Example format:
                ✈️|机场办理登机|值机、安检、登机对话
                🏨|酒店入住|预订确认、入住登记、房间服务
                
                Generate 5-6 scenarios now, one per line, following this exact format.
            """.trimIndent()

            asyncChat(
                prompt = prompt,
                onResult = { result, isComplete ->
                    if (isComplete) {
                        try {
                            // 解析AI返回的场景列表
                            val scenes = result.lines()
                                .filter { it.contains("|") }
                                .mapNotNull { line ->
                                    val parts = line.split("|")
                                    if (parts.size >= 3) {
                                        com.edusmart.app.feature.speaking.Scene(
                                            name = parts[1].trim(),
                                            description = parts[2].trim(),
                                            emoji = parts[0].trim()
                                        )
                                    } else null
                                }

                            if (scenes.isNotEmpty()) {
                                Log.d(TAG, "成功生成 ${scenes.size} 个场景")
                                continuation.resume(scenes)
                            } else {
                                Log.w(TAG, "解析场景失败，返回空列表")
                                continuation.resume(emptyList())
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "解析场景时出错: ${e.message}")
                            continuation.resume(emptyList())
                        }
                    }
                },
                onError = { error ->
                    Log.e(TAG, "生成场景失败: $error")
                    continuation.resume(emptyList())
                }
            )
        }
    }

    // ========== TTS 语音合成相关（根据官方文档修改） ==========

    /**
     * 初始化 TTS 引擎
     * 根据官方文档，使用 OnlineTTS 构造函数并传入发音人参数
     */
    private fun initTts() {
        if (mTts != null) return

        try {
            // 使用 OnlineTTS 构造函数，传入发音人参数
            mTts = OnlineTTS("xiaoyan")  // xiaoyan 是默认发音人

            // 注册回调
            mTts?.registerCallbacks(createTtsCallbacks())

            // 配置TTS参数
            configureTtsParams()

            Log.d(TAG, "TTS 初始化成功")
        } catch (e: Exception) {
            Log.e(TAG, "TTS 初始化失败", e)
        }
    }

    /**
     * 配置 TTS 参数
     * 根据官方文档的功能参数配置方法
     */
    private fun configureTtsParams() {
        mTts?.apply {
            // 音频编码: raw 为未压缩的PCM
            aue("raw")

            // 音频采样率: 16k采样率
            auf("audio/L16;rate=16000")

            // 语速 [0-100]，默认50
            speed(50)

            // 音高 [0-100]，默认50
            pitch(50)

            // 音量 [0-100]，默认50
            volume(80)

            // 背景音: 0无背景音
            bgs(0)

            // 文本编码格式
            tte("UTF8")

            Log.d(TAG, "TTS 参数配置完成")
        }
    }

    /**
     * 创建 TTS 回调
     * 根据官方文档的 TTSCallbacks 接口定义
     */
    private fun createTtsCallbacks() = object : TTSCallbacks {
        override fun onResult(result: TTSResult?, usrTag: Any?) {
            if (result == null) return

            // 获取音频数据
            val audioData = result.getData()
            val status = result.getStatus()
            val len = result.getLen()

            Log.d(TAG, "TTS Result - len: $len, status: $status")

            // 播放音频数据
            if (audioData != null && audioData.isNotEmpty()) {
                playAudioData(audioData)
            }

            // status=2 表示合成结束
            if (status == 2) {
                Log.d(TAG, "TTS 合成完成")
            }
        }

        override fun onError(error: TTSError?, usrTag: Any?) {
            if (error == null) return
            val errorCode = error.getCode()
            val errorMsg = error.getErrMsg()
            val sid = error.getSid()

            Log.e(TAG, "TTS Error - code: $errorCode, msg: $errorMsg, sid: $sid")
        }
    }

    /**
     * 语音合成:文字转语音
     * 使用 aRun 方法进行异步调用
     */
    fun speak(text: String) {
        if (text.isBlank()) {
            Log.w(TAG, "文本为空，无法合成")
            return
        }

        // 确保TTS已初始化
        if (mTts == null) {
            initTts()
        }

        // 先停止之前的合成
        stopSpeaking()

        try {
            // 调用 aRun 方法开始合成
            val ret = mTts?.aRun(text)
            if (ret == 0) {
                Log.d(TAG, "开始合成文本: $text")
            } else {
                Log.e(TAG, "TTS 合成启动失败，返回码: $ret")
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS 合成失败", e)
        }
    }

    /**
     * 停止语音合成
     */
    fun stopSpeaking() {
        try {
            mTts?.stop()
            stopAudioPlayback()
            Log.d(TAG, "停止 TTS 合成")
        } catch (e: Exception) {
            Log.e(TAG, "停止 TTS 失败", e)
        }
    }

    /**
     * 播放音频数据
     */
    private fun playAudioData(data: ByteArray) {
        try {
            if (audioTrack == null) {
                val sampleRate = 16000
                val channelConfig = AudioFormat.CHANNEL_OUT_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT

                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    channelConfig,
                    audioFormat
                )

                audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    maxOf(minBufferSize, data.size * 2),
                    AudioTrack.MODE_STREAM
                )
                // 第一次初始化后启动播放
                audioTrack?.play()
            }

            // 【核心修复】：检查当前状态，如果不是正在播放状态（比如被上一次 stopSpeaking 停止了），则重新启动
            if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                audioTrack?.play()
                Log.d(TAG, "AudioTrack 重新启动播放")
            }

            // 写入音频数据
            val written = audioTrack?.write(data, 0, data.size)
            // Log.d(TAG, "写入音频数据: $written bytes")

        } catch (e: Exception) {
            Log.e(TAG, "播放音频失败: ${e.message}", e)
        }
    }

    /**
     * 停止音频播放
     */
    private fun stopAudioPlayback() {
        audioTrack?.apply {
            try {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                flush()
            } catch (e: Exception) {
                Log.e(TAG, "停止音频播放失败", e)
            }
        }
    }

    // ========== ASR 语音识别相关（保持不变） ==========

    /**
     * 英文语音识别（ASR）
     */
    suspend fun transcribeEnglish(audioPath: String): String = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            if (mAsr == null) {
                mAsr = ASR()
            }

            val callbacks = object : AsrCallbacks {
                private val resultBuilder = StringBuilder()

                override fun onResult(asrResult: ASRResult?, o: Any?) {
                    if (asrResult == null) return
                    val text = asrResult.bestMatchText ?: ""
                    resultBuilder.append(text)

                    if (asrResult.status == 2) {
                        val finalResult = resultBuilder.toString()
                        Log.d(TAG, "ASR 识别完成: $finalResult")
                        continuation.resume(finalResult)
                    }
                }

                override fun onError(asrError: ASRError?, o: Any?) {
                    continuation.resumeWithException(
                        Exception(asrError?.errMsg ?: "Unknown ASR error")
                    )
                }

                override fun onBeginOfSpeech() {
                    Log.d(TAG, "ASR 检测到语音开始")
                }

                override fun onEndOfSpeech() {
                    Log.d(TAG, "ASR 检测到语音结束")
                }
            }

            try {
                mAsr?.registerCallbacks(callbacks)
                mAsr?.language("en_us")
                mAsr?.setParams("domain", "iat")
                mAsr?.start("file_transcribe")

                val pcmData = decodeAudioToPCM(audioPath)
                mAsr?.write(pcmData)
                mAsr?.stop(false)

            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    /**
     * 语音转文字 (ASR)
     */
    suspend fun transcribe(audioPath: String): String = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            if (mAsr == null) {
                mAsr = ASR()
            }

            val callbacks = object : AsrCallbacks {
                private val resultBuilder = StringBuilder()

                override fun onResult(asrResult: ASRResult?, o: Any?) {
                    if (asrResult == null) return
                    val text = asrResult.bestMatchText ?: ""
                    resultBuilder.append(text)

                    if (asrResult.status == 2) {
                        val finalResult = resultBuilder.toString()
                        Log.d(TAG, "ASR 识别完成: $finalResult")
                        continuation.resume(finalResult)
                    }
                }

                override fun onError(asrError: ASRError?, o: Any?) {
                    continuation.resumeWithException(
                        Exception(asrError?.errMsg ?: "Unknown ASR error")
                    )
                }

                override fun onBeginOfSpeech() {
                    Log.d(TAG, "ASR 检测到语音开始")
                }

                override fun onEndOfSpeech() {
                    Log.d(TAG, "ASR 检测到语音结束")
                }
            }

            try {
                mAsr?.registerCallbacks(callbacks)
                mAsr?.language("zh_cn")
                mAsr?.setParams("domain", "iat")
                mAsr?.start("file_transcribe")

                val pcmData = decodeAudioToPCM(audioPath)
                mAsr?.write(pcmData)
                mAsr?.stop(false)

            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    // ========== ISE 发音评测相关（保持不变） ==========

    // ========== 修复后的 scorePronunciation 方法 ==========
// 放在 SpeechServiceSparkChain.kt 中替换原有的 scorePronunciation 方法

    /**
     * 发音评测 - 增强版,包含详细诊断日志
     */
    suspend fun scorePronunciation(
        audioPath: String,
        referenceText: String
    ): Float = withContext(Dispatchers.IO) {
        Log.d(TAG, "========================================")
        Log.d(TAG, ">>> 开始ISE发音评测")
        Log.d(TAG, "音频文件: $audioPath")
        Log.d(TAG, "参考文本: $referenceText")
        Log.d(TAG, "文本长度: ${referenceText.length} 字符")

        // 检查文件
        val audioFile = File(audioPath)
        Log.d(TAG, "文件存在: ${audioFile.exists()}")
        Log.d(TAG, "文件大小: ${audioFile.length()} bytes")
        Log.d(TAG, "文件可读: ${audioFile.canRead()}")

        if (!audioFile.exists()) {
            Log.e(TAG, "❌ 音频文件不存在,评分失败")
            return@withContext 0f
        }

        if (audioFile.length() < 1000) {
            Log.e(TAG, "❌ 音频文件太小 (${audioFile.length()} bytes),可能录音失败")
            return@withContext 0f
        }

        suspendCancellableCoroutine { continuation ->
            Log.d(TAG, ">>> 步骤1: 创建ISE评估器...")

            val mIse = SpeechEvaluator.createEvaluator(context, null)

            if (mIse == null) {
                Log.e(TAG, "❌❌❌ ISE评估器创建失败!")
                Log.e(TAG, "可能原因:")
                Log.e(TAG, "  1. 讯飞SDK未初始化 - 检查 Application.onCreate() 中是否调用了 SpeechServiceSparkChain.initialize()")
                Log.e(TAG, "  2. APP_ID 配置错误 - 当前: ${SDKConfig.XUNFEI_APP_ID}")
                Log.e(TAG, "  3. 权限不足 - 检查 RECORD_AUDIO 和 INTERNET 权限")
                Log.e(TAG, "  4. SDK库文件缺失 - 检查 libs/ 目录")

                continuation.resume(0f)
                return@suspendCancellableCoroutine
            }

            Log.d(TAG, "✅ ISE评估器创建成功")

            try {
                Log.d(TAG, ">>> 步骤2: 配置评测参数...")

                mIse.setParameter(SpeechConstant.LANGUAGE, "en_us")
                mIse.setParameter(SpeechConstant.ISE_CATEGORY, "read_sentence")
                mIse.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8")
                mIse.setParameter(SpeechConstant.RESULT_LEVEL, "complete")
                mIse.setParameter(SpeechConstant.VAD_BOS, "5000")
                mIse.setParameter(SpeechConstant.VAD_EOS, "1800")
                mIse.setParameter(SpeechConstant.AUDIO_FORMAT, "wav")
                mIse.setParameter(SpeechConstant.SAMPLE_RATE, "16000")

                Log.d(TAG, "✅ 参数配置完成")
                Log.d(TAG, ">>> 步骤3: 解码音频文件...")

                val pcmData = try {
                    val data = decodeAudioToPCM(audioPath)
                    Log.d(TAG, "✅ 音频解码成功: ${data.size} bytes PCM数据")
                    data
                } catch (e: Exception) {
                    Log.e(TAG, "❌❌❌ 音频解码失败!")
                    Log.e(TAG, "异常类型: ${e.javaClass.simpleName}")
                    Log.e(TAG, "异常信息: ${e.message}")
                    Log.e(TAG, "堆栈跟踪:", e)
                    Log.e(TAG, "可能原因:")
                    Log.e(TAG, "  1. 音频格式不支持 - 当前: M4A/AAC")
                    Log.e(TAG, "  2. MediaCodec 不可用")
                    Log.e(TAG, "  3. 文件损坏")
                    continuation.resume(0f)
                    mIse.destroy()
                    return@suspendCancellableCoroutine
                }

                if (pcmData.isEmpty()) {
                    Log.e(TAG, "❌ PCM数据为空,无法评分")
                    continuation.resume(0f)
                    mIse.destroy()
                    return@suspendCancellableCoroutine
                }

                Log.d(TAG, ">>> 步骤4: 启动评测...")

                mIse.startEvaluating(referenceText, null, object : EvaluatorListener {
                    override fun onResult(result: EvaluatorResult?, isLast: Boolean) {
                        Log.d(TAG, "收到评测结果回调: result=${result != null}, isLast=$isLast")

                        if (result != null && isLast) {
                            val xml = result.resultString
                            Log.d(TAG, "评测结果XML (前200字符): ${xml.take(200)}")

                            val score = parseIseScore(xml)
                            Log.d(TAG, "✅✅✅ 评测成功! 最终得分: $score")

                            continuation.resume(score)
                            mIse.destroy()
                        }
                    }

                    override fun onError(error: SpeechError?) {
                        Log.e(TAG, "❌❌❌ 评测过程出错!")
                        Log.e(TAG, "错误码: ${error?.errorCode}")
                        Log.e(TAG, "错误描述: ${error?.errorDescription}")

                        // 常见错误码说明
                        when (error?.errorCode) {
                            10106 -> Log.e(TAG, "→ 无效的APP_ID,请检查配置")
                            10107 -> Log.e(TAG, "→ 无效的参数")
                            10110 -> Log.e(TAG, "→ 网络连接失败")
                            10114 -> Log.e(TAG, "→ 服务器错误")
                            10160 -> Log.e(TAG, "→ 音频格式错误")
                            20001 -> Log.e(TAG, "→ 音频数据为空或太短")
                            20002 -> Log.e(TAG, "→ 未检测到有效语音")
                            else -> Log.e(TAG, "→ 其他错误,请查阅讯飞文档")
                        }

                        continuation.resume(0f)
                        mIse.destroy()
                    }

                    override fun onBeginOfSpeech() {
                        Log.d(TAG, "🎤 检测到语音开始")
                    }

                    override fun onEndOfSpeech() {
                        Log.d(TAG, "🎤 检测到语音结束")
                    }

                    override fun onVolumeChanged(volume: Int, data: ByteArray?) {
                        // Log.v(TAG, "音量: $volume")
                    }

                    override fun onEvent(eventType: Int, arg1: Int, arg2: Int, obj: android.os.Bundle?) {
                        Log.d(TAG, "事件: type=$eventType, arg1=$arg1, arg2=$arg2")
                    }
                })

                Log.d(TAG, ">>> 步骤5: 写入音频数据...")
                mIse.writeAudio(pcmData, 0, pcmData.size)
                Log.d(TAG, "✅ 音频数据已写入: ${pcmData.size} bytes")

                Log.d(TAG, ">>> 步骤6: 停止评测,等待结果...")
                mIse.stopEvaluating()
                Log.d(TAG, "评测已停止,等待回调...")

            } catch (e: Exception) {
                Log.e(TAG, "❌❌❌ 评测流程发生异常!")
                Log.e(TAG, "异常类型: ${e.javaClass.simpleName}")
                Log.e(TAG, "异常信息: ${e.message}")
                Log.e(TAG, "堆栈跟踪:", e)
                continuation.resume(0f)
                mIse.destroy()
            }
        }
    }

    /**
     * 解析评测结果 XML，提取总分
     */
    private fun parseIseScore(xml: String): Float {
        return try {
            val pattern = Pattern.compile("total_score=\"(.*?)\"")
            val matcher = pattern.matcher(xml)

            if (matcher.find()) {
                val score = matcher.group(1)?.toFloatOrNull() ?: 0f
                score * 20f
            } else {
                0f
            }
        } catch (e: Exception) {
            0f
        }
    }

    // ========== 工具方法（保持不变） ==========

    /**
     * 将音频文件解码为 PCM 数据
     */
    private fun decodeAudioToPCM(audioPath: String): ByteArray {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        val outputStream = java.io.ByteArrayOutputStream()

        try {
            extractor.setDataSource(audioPath)

            // 查找音频轨道
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue

                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    Log.d(Companion.TAG, "找到音频轨道: $mime")
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                throw Exception("未找到音频轨道")
            }

            extractor.selectTrack(audioTrackIndex)

            // 获取音频参数
            val sampleRate = audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"

            Log.d(Companion.TAG, "音频参数: sampleRate=$sampleRate, channelCount=$channelCount, mime=$mime")

            // 创建解码器
            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(audioFormat, null, null, 0)
            decoder.start()

            val bufferInfo = MediaCodec.BufferInfo()

            var inputEOS = false
            var outputEOS = false

            while (!outputEOS) {
                // 输入数据
                if (!inputEOS) {
                    val inputBufferIndex = decoder.dequeueInputBuffer(10000)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputBufferIndex)
                        if (inputBuffer != null) {
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)

                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputEOS = true
                            } else {
                                decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                // 输出数据
                val outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputBufferIndex >= 0) {
                    val outputBuffer = decoder.getOutputBuffer(outputBufferIndex)

                    if (outputBuffer != null && bufferInfo.size > 0) {
                        val pcmChunk = ByteArray(bufferInfo.size)
                        outputBuffer.get(pcmChunk, 0, bufferInfo.size)
                        outputStream.write(pcmChunk)
                    }

                    decoder.releaseOutputBuffer(outputBufferIndex, false)

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputEOS = true
                    }
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val newFormat = decoder.outputFormat
                    Log.d(Companion.TAG, "输出格式已更改: $newFormat")
                }
            }

            val pcmData = outputStream.toByteArray()
            Log.d(Companion.TAG, "PCM 解码完成，数据大小: ${pcmData.size} bytes")

            // 如果需要，可以在这里进行重采样（16kHz, 单声道）
            // 目前先返回原始 PCM 数据

            return pcmData

        } catch (e: Exception) {
            Log.e(Companion.TAG, "音频解码失败", e)
            throw Exception("音频解码失败: ${e.message}", e)
        } finally {
            decoder?.stop()
            decoder?.release()
            extractor.release()
            outputStream.close()
        }
    }


    /**
     * 释放所有资源
     */
    fun release() {
        // 停止 TTS
        stopSpeaking()
        mTts = null

        // 停止 ASR
        mAsr = null

        // 释放 AudioTrack
        audioTrack?.apply {
            stop()
            release()
        }
        audioTrack = null

        // 释放LLM资源
        mLLM = null
        isLLMInitialized = false
        llmCallbacks = null

        Log.d(TAG, "所有资源已释放")
    }
}
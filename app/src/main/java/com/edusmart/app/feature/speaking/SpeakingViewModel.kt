package com.edusmart.app.feature.speaking

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edusmart.app.service.SpeechServiceSparkChain
import com.edusmart.app.service.DoubaoTTSService
import com.edusmart.app.service.DoubaoASRService  // ★ 新增：豆包ASR服务
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class SpeakingViewModel(
    private val context: Context,
    private val speechService: SpeechServiceSparkChain
) : ViewModel() {

    private val TAG = "SpeakingVM"

    // 豆包TTS服务实例
    private val doubaoTTS = DoubaoTTSService(context)

    // ★ 新增：豆包ASR服务实例
    private val doubaoASR = DoubaoASRService(context)

    private val _uiState = MutableStateFlow(SpeakingUiState())
    val uiState = _uiState.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var currentAudioPath: String? = null

    // 新增：设置学习目的并生成相应场景
    fun setLearningPurpose(purpose: LearningPurpose) {
        Log.d(TAG, ">>> [设置学习目的] ${purpose.name}")

        // 清空所有与之前学习目的相关的状态
        _uiState.value = SpeakingUiState( // 创建全新的状态
            learningPurpose = purpose,
            isLoadingScenes = true, // 显示场景加载状态
            // 其他字段保持默认值（空列表、false等）
            showTranslation = _uiState.value.showTranslation // 保留翻译设置
        )

        // 根据学习目的生成场景
        viewModelScope.launch {
            try {
                Log.d(TAG, "开始为学习目的生成场景: ${purpose.name}")
                val generatedScenes = speechService.generateScenesForPurpose(purpose.name, purpose.description)
                Log.i(TAG, "成功生成 ${generatedScenes.size} 个场景")

                _uiState.value = _uiState.value.copy(
                    customScenes = generatedScenes,
                    isLoadingScenes = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "生成场景失败: ${e.message}")
                // 失败时使用默认场景
                _uiState.value = _uiState.value.copy(
                    customScenes = getDefaultScenesForPurpose(purpose),
                    isLoadingScenes = false
                )
            }
        }
    }

    // 获取默认场景（作为后备方案）
    private fun getDefaultScenesForPurpose(purpose: LearningPurpose): List<Scene> {
        return when (purpose.name) {
            "雅思考试" -> listOf(
                Scene("Part 1 自我介绍", "雅思口语第一部分常见话题", "📝"),
                Scene("Part 2 个人陈述", "2分钟独白练习", "🎤"),
                Scene("Part 3 深度讨论", "抽象话题讨论", "💭"),
                Scene("学术讨论", "教育、科技等学术话题", "📚"),
                Scene("生活话题", "日常生活相关讨论", "🏠")
            )
            "托福考试" -> listOf(
                Scene("独立口语", "个人观点表达", "💬"),
                Scene("综合口语", "听读说综合任务", "📖"),
                Scene("校园场景", "校园生活对话", "🎓"),
                Scene("学术讲座", "课堂讨论场景", "👨‍🏫"),
                Scene("问题解决", "校园问题讨论", "🤔")
            )
            "出国旅游" -> listOf(
                Scene("机场", "值机、安检、登机", "✈️"),
                Scene("酒店", "预订、入住、退房", "🏨"),
                Scene("餐厅", "点餐、询问、结账", "🍽️"),
                Scene("购物", "询价、试穿、付款", "🛍️"),
                Scene("问路", "方向指引、交通咨询", "🗺️"),
                Scene("紧急情况", "求助、报警、医疗", "🚨")
            )
            "出国生活" -> listOf(
                Scene("租房", "看房、签约、维修", "🏠"),
                Scene("银行", "开户、汇款、理财", "🏦"),
                Scene("超市", "日常采购、询问商品", "🛒"),
                Scene("医院", "挂号、看病、拿药", "🏥"),
                Scene("邮局", "寄件、取件、咨询", "📮"),
                Scene("社交", "邻里交往、聚会", "👋")
            )
            "商务交流" -> listOf(
                Scene("会议", "发言、讨论、总结", "💼"),
                Scene("邮件", "商务邮件沟通", "📧"),
                Scene("电话", "商务电话礼仪", "📞"),
                Scene("谈判", "价格、条款协商", "🤝"),
                Scene("演讲", "产品介绍、汇报", "📊"),
                Scene("社交", "商务晚宴、闲聊", "🍷")
            )
            "日常交流" -> listOf(
                Scene("打招呼", "日常问候与寒暄", "👋"),
                Scene("兴趣爱好", "分享个人爱好", "🎨"),
                Scene("天气", "天气话题讨论", "🌤️"),
                Scene("美食", "谈论食物与餐饮", "🍔"),
                Scene("电影音乐", "娱乐话题交流", "🎬"),
                Scene("旅行", "分享旅行经历", "🌍")
            )
            "求职面试" -> listOf(
                Scene("自我介绍", "面试开场介绍", "👤"),
                Scene("工作经历", "描述过往经验", "💼"),
                Scene("优势劣势", "SWOT分析", "⚖️"),
                Scene("情景问题", "解决实际问题", "🎯"),
                Scene("薪资谈判", "待遇福利讨论", "💰"),
                Scene("提问环节", "向面试官提问", "❓")
            )
            "学术研究" -> listOf(
                Scene("论文讨论", "研究方法与结果", "📄"),
                Scene("研讨会", "学术观点交流", "🎓"),
                Scene("导师会面", "研究进展汇报", "👨‍🏫"),
                Scene("同行评审", "论文评审讨论", "🔍"),
                Scene("会议发言", "学术会议演讲", "🎤"),
                Scene("合作研究", "跨国合作交流", "🌐")
            )
            else -> listOf(
                Scene("机场", "机场值机、安检、登机对话", "✈️"),
                Scene("面试", "求职面试场景对话", "💼"),
                Scene("餐厅", "点餐、结账等餐厅对话", "🍽️"),
                Scene("酒店", "酒店预订、入住对话", "🏨"),
                Scene("购物", "商场购物对话", "🛍️")
            )
        }
    }

    // 1. 场景选择 - 调用 SpeechService 生成开场白
    /**
     * 场景选择逻辑
     */
    fun onSceneSelected(scene: Scene) {
        Log.d(TAG, ">>> [场景选择] ${scene.name}")

        // 停止之前的任何播报和录音
        doubaoTTS.stopSpeaking()

        _uiState.value = _uiState.value.copy(
            selectedScene = scene,
            messages = emptyList(),
            isProcessing = true,
            isAiSpeaking = false,
            isUserRecording = false
        )

        viewModelScope.launch {
            try {
                val openingMessage = speechService.generateSceneOpeningWithPurpose(
                    sceneName = scene.name,
                    sceneDescription = scene.description,
                    learningPurpose = _uiState.value.learningPurpose?.name ?: ""
                )

                _uiState.value = _uiState.value.copy(
                    messages = listOf(ChatMessage("AI", openingMessage)),
                    isProcessing = false
                )

                // 播报 AI 开场白
                playAiVoice(openingMessage)

            } catch (e: Exception) {
                Log.e(TAG, "生成开场白失败: ${e.message}")
                val fallback = "Hello! Let's practice ${scene.name}."
                _uiState.value = _uiState.value.copy(
                    messages = listOf(ChatMessage("AI", fallback)),
                    isProcessing = false
                )
                playAiVoice(fallback)
            }
        }
    }


    // 新增：设置自定义话题 - 调用 SpeechService 生成开场白
    /**
     * 自定义话题逻辑
     */
    fun setCustomTopic(topic: String) {
        doubaoTTS.stopSpeaking()
        _uiState.value = _uiState.value.copy(
            customTopic = topic,
            messages = emptyList(),
            isProcessing = true,
            isAiSpeaking = false,
            isUserRecording = false
        )

        viewModelScope.launch {
            try {
                val openingMessage = speechService.generateTopicOpeningWithPurpose(
                    topic = topic,
                    learningPurpose = _uiState.value.learningPurpose?.name ?: ""
                )
                _uiState.value = _uiState.value.copy(
                    messages = listOf(ChatMessage("AI", openingMessage)),
                    isProcessing = false
                )
                playAiVoice(openingMessage)
            } catch (e: Exception) {
                val fallback = "Great topic! Let's talk about $topic."
                _uiState.value = _uiState.value.copy(
                    messages = listOf(ChatMessage("AI", fallback)),
                    isProcessing = false
                )
                playAiVoice(fallback)
            }
        }
    }



    /**
     * 统一的 AI 语音播报方法
     */
    private fun playAiVoice(text: String) {
        doubaoTTS.speak(
            text = text,
            onStart = {
                _uiState.value = _uiState.value.copy(isAiSpeaking = true)
                Log.d(TAG, "AI 开始说话")
            },
            onComplete = {
                _uiState.value = _uiState.value.copy(isAiSpeaking = false)
                Log.d(TAG, "AI 说话结束")
            },
            onError = { error ->
                Log.e(TAG, "TTS 播报失败: $error")
                _uiState.value = _uiState.value.copy(isAiSpeaking = false)
            }
        )
    }


    // 新增：切换翻译显示
    fun toggleTranslation() {
        _uiState.value = _uiState.value.copy(
            showTranslation = !_uiState.value.showTranslation
        )
        Log.d(TAG, "翻译显示: ${_uiState.value.showTranslation}")
    }

    // 新增：为AI消息生成翻译
    fun generateTranslationForMessage(messageIndex: Int) {
        val messages = _uiState.value.messages
        if (messageIndex !in messages.indices) return

        val message = messages[messageIndex]
        if (message.role != "AI" || message.translation != null) return

        viewModelScope.launch {
            try {
                Log.d(TAG, "生成AI消息翻译...")
                val translation = speechService.translateToChinese(message.content)

                val updatedMessages = messages.toMutableList()
                updatedMessages[messageIndex] = message.copy(translation = translation)
                _uiState.value = _uiState.value.copy(messages = updatedMessages)

                Log.i(TAG, "翻译生成成功: $translation")
            } catch (e: Exception) {
                Log.e(TAG, "生成翻译失败: ${e.message}")
            }
        }
    }

    // 新增：为AI消息生成参考回复
    fun generateSuggestedReply(messageIndex: Int) {
        val messages = _uiState.value.messages
        if (messageIndex !in messages.indices) return

        val message = messages[messageIndex]
        if (message.role != "AI" || message.suggestedReply != null) return

        viewModelScope.launch {
            try {
                Log.d(TAG, "生成参考回复...")

                // 构建上下文
                val conversationHistory = messages.take(messageIndex + 1).takeLast(3).map {
                    "${it.role}: ${it.content}"
                }

                val contextDescription = when {
                    _uiState.value.customTopic.isNotBlank() ->
                        "Topic: ${_uiState.value.customTopic}"
                    _uiState.value.selectedScene != null ->
                        "Scenario: ${_uiState.value.selectedScene?.name}"
                    else -> "General conversation"
                }

                val suggestedReply = speechService.generateSuggestedReply(
                    aiMessage = message.content,
                    conversationHistory = conversationHistory,
                    contextDescription = contextDescription,
                    learningPurpose = _uiState.value.learningPurpose?.name ?: ""
                )

                val updatedMessages = messages.toMutableList()
                updatedMessages[messageIndex] = message.copy(suggestedReply = suggestedReply)
                _uiState.value = _uiState.value.copy(messages = updatedMessages)

                Log.i(TAG, "参考回复生成成功: $suggestedReply")
            } catch (e: Exception) {
                Log.e(TAG, "生成参考回复失败: ${e.message}")
            }
        }
    }

    // 2. 开始录音
    /**
     * 开始录音 - ★ 修改：设置16kHz采样率以优化豆包ASR识别
     */
    fun startRecording() {
        if (_uiState.value.selectedScene == null && _uiState.value.customTopic.isBlank()) return

        try {
            // 关键：录音前必须停止 AI 说话，防止回声或音频占用
            doubaoTTS.stopSpeaking()

            val audioFile = File(context.externalCacheDir, "speaking_${System.currentTimeMillis()}.m4a")
            currentAudioPath = audioFile.absolutePath

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

                // ★★★ 关键修改：设置16kHz采样率，优化豆包ASR识别效果 ★★★
                setAudioSamplingRate(16000)
                setAudioEncodingBitRate(64000)
                setAudioChannels(1)  // 单声道

                setOutputFile(currentAudioPath)
                prepare()
                start()
            }

            _uiState.value = _uiState.value.copy(isUserRecording = true, isAiSpeaking = false)
            Log.i(TAG, "用户开始录音 (16kHz, AAC)")
        } catch (e: Exception) {
            Log.e(TAG, "录音启动失败: ${e.message}")
        }
    }

    // 3. 停止录音并处理
    /**
     * 停止录音并处理流水线 - ★ 修改：使用豆包ASR进行语音识别
     */
    fun stopRecording() {
        Log.d(TAG, "停止录音...")
        _uiState.value = _uiState.value.copy(isUserRecording = false, isProcessing = true)

        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "停止录音失败: ${e.message}")
        } finally {
            try { mediaRecorder?.release() } catch (_: Exception) {}
        }
        mediaRecorder = null

        val audioPath = currentAudioPath
        if (audioPath == null) {
            Log.e(TAG, "录音路径为空")
            _uiState.value = _uiState.value.copy(isProcessing = false)
            return
        }

        // 检查录音文件
        val audioFile = File(audioPath)
        Log.d(TAG, "录音文件: ${audioFile.absolutePath}, 大小: ${audioFile.length()} bytes")

        viewModelScope.launch {
            try {
                // ★★★ 1. 使用豆包ASR进行语音识别 ★★★
                Log.d(TAG, ">>> 开始豆包ASR识别...")
                val transcript = doubaoASR.transcribeEnglish(audioPath)
                Log.d(TAG, ">>> ASR结果: '$transcript' (长度: ${transcript.length})")

                // 检查识别结果
                if (transcript.isBlank()) {
                    Log.w(TAG, "ASR返回空结果，可能原因: 录音太短、声音太小、网络问题")
                    _uiState.value = _uiState.value.copy(isProcessing = false)
                    return@launch
                }

                // ★★★ 2. ISE 评分 - 增强错误处理和日志 ★★★
                Log.d(TAG, ">>> 开始ISE评分...")
                Log.d(TAG, "评分参数: audioPath=$audioPath, transcript='$transcript'")

                var score = 0f
                var scoreError: String? = null

                try {
                    // 检查音频文件是否存在
                    if (!audioFile.exists()) {
                        throw Exception("音频文件不存在: ${audioFile.absolutePath}")
                    }

                    if (audioFile.length() < 1000) {
                        throw Exception("音频文件太小 (${audioFile.length()} bytes)，可能录音失败")
                    }

                    // 检查识别文本是否有效
                    if (transcript.length < 5) {
                        Log.w(TAG, "识别文本太短，跳过评分")
                        scoreError = "文本太短"
                    } else {
                        // 调用评分服务
                        Log.d(TAG, "调用 scorePronunciation()...")
                        score = speechService.scorePronunciation(audioPath, transcript)
                        Log.d(TAG, ">>> 评分完成: $score")

                        // 检查评分结果
                        if (score <= 0f) {
                            Log.w(TAG, "评分返回0分，可能原因：音频格式问题、网络问题或评分服务错误")
                            scoreError = "评分服务返回0分"
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 评分失败详细信息:", e)
                    Log.e(TAG, "错误类型: ${e.javaClass.simpleName}")
                    Log.e(TAG, "错误消息: ${e.message}")
                    Log.e(TAG, "堆栈跟踪: ${e.stackTraceToString()}")
                    scoreError = e.message ?: "未知错误"
                    score = 0f
                }

                // 记录评分结果
                if (scoreError != null) {
                    Log.w(TAG, "⚠️ 评分失败但继续流程: $scoreError")
                } else if (score > 0) {
                    Log.i(TAG, "✅ 评分成功: $score 分")
                }

                // ★★★ 3. 翻译（可选） ★★★
                val translation = if (_uiState.value.showTranslation) {
                    try {
                        Log.d(TAG, ">>> 开始翻译...")
                        speechService.translateToChinese(transcript)
                    } catch (e: Exception) {
                        Log.e(TAG, "翻译失败: ${e.message}")
                        null
                    }
                } else null

                // ★★★ 4. 创建用户消息（包含评分和可能的错误信息） ★★★
                val userMsg = ChatMessage(
                    role = "User",
                    content = transcript,
                    score = if (score > 0) score else null,  // 只在成功时显示分数
                    translation = translation,
                    scoreError = scoreError  // 添加错误信息字段
                )
                val updatedWithUser = _uiState.value.messages + userMsg
                _uiState.value = _uiState.value.copy(messages = updatedWithUser)

                // ★★★ 5. 获取 AI 回复 ★★★
                Log.d(TAG, ">>> 获取AI回复...")
                val aiReply = getAIReply(updatedWithUser)
                Log.d(TAG, ">>> AI回复: '$aiReply'")

                // ★★★ 6. 播报回复 ★★★
                Log.d(TAG, ">>> 开始播报AI回复...")
                playAiVoice(aiReply)

                val aiTranslation = if (_uiState.value.showTranslation) {
                    try {
                        speechService.translateToChinese(aiReply)
                    } catch (e: Exception) {
                        null
                    }
                } else null

                _uiState.value = _uiState.value.copy(
                    messages = updatedWithUser + ChatMessage("AI", aiReply, translation = aiTranslation),
                    isProcessing = false
                )

                Log.d(TAG, "✅ 完整流程执行完毕")

                // 清理临时录音文件
                try { File(audioPath).delete() } catch (_: Exception) {}

            } catch (e: Exception) {
                Log.e(TAG, "❌❌❌ 流水线严重错误: ${e.message}", e)
                _uiState.value = _uiState.value.copy(isProcessing = false)
            }
        }
    }

    /**
     * 调用 SparkChain LLM API 获取回复
     */
    private suspend fun getAIReply(history: List<ChatMessage>): String {
        // 构建对话历史
        val conversationHistory = history.takeLast(5).map {
            "${it.role}: ${it.content}"
        }

        // 获取当前上下文（场景或自定义话题）
        val contextDescription = when {
            _uiState.value.customTopic.isNotBlank() ->
                "Topic: ${_uiState.value.customTopic}"
            _uiState.value.selectedScene != null ->
                "Scenario: ${_uiState.value.selectedScene?.name} (${_uiState.value.selectedScene?.description})"
            else -> "General English conversation"
        }

        // 获取用户最后一条消息
        val userMessage = history.lastOrNull()?.content ?: ""

        // 调用 SpeechServiceSparkChain 的 AI 对话功能（带学习目的）
        return speechService.generateAIReplyWithPurpose(
            userMessage = userMessage,
            conversationHistory = conversationHistory,
            contextDescription = contextDescription,
            learningPurpose = _uiState.value.learningPurpose?.name ?: ""
        )
    }

    override fun onCleared() {
        super.onCleared()
        // 停止TTS
        doubaoTTS.stopSpeaking()
        // TODO: doubaoASR 缺少 release 方法，需要在 DoubaoASRService 中实现
        // 释放讯飞资源
        speechService.release()
        // 清理临时录音文件
        currentAudioPath?.let { path ->
            try { java.io.File(path).delete() } catch (_: Exception) {}
        }
        Log.d(TAG, "ViewModel 已清理")
    }
}

// 新增：学习目的枚举
data class LearningPurpose(
    val name: String,
    val description: String,
    val emoji: String
)

// 预定义的学习目的
object LearningPurposes {
    val IELTS = LearningPurpose("雅思考试", "备考雅思口语考试", "📝")
    val TOEFL = LearningPurpose("托福考试", "备考托福口语考试", "📚")
    val TRAVEL = LearningPurpose("出国旅游", "日常旅游交流", "✈️")
    val LIVING_ABROAD = LearningPurpose("出国生活", "留学或移民日常生活", "🏠")
    val BUSINESS = LearningPurpose("商务交流", "与客户同事对接", "💼")
    val DAILY = LearningPurpose("日常交流", "提升日常英语能力", "💬")
    val INTERVIEW = LearningPurpose("求职面试", "准备英语面试", "🎯")
    val ACADEMIC = LearningPurpose("学术研究", "学术论文和研讨会", "🎓")

    fun getAll() = listOf(IELTS, TOEFL, TRAVEL, LIVING_ABROAD, BUSINESS, DAILY, INTERVIEW, ACADEMIC)
}

// 场景数据类
data class Scene(val name: String, val description: String, val emoji: String)


/**
 * 状态类：核心修改在于区分 AiSpeaking 和 UserRecording
 */
data class SpeakingUiState(
    val learningPurpose: LearningPurpose? = null,
    val customScenes: List<Scene> = emptyList(),
    val isLoadingScenes: Boolean = false,
    val selectedScene: Scene? = null,
    val customTopic: String = "",
    val isAiSpeaking: Boolean = false,    // AI 正在播报
    val isUserRecording: Boolean = false, // 用户正在录制
    val isProcessing: Boolean = false,    // 后台处理中（ASR/LLM）
    val messages: List<ChatMessage> = emptyList(),
    val showTranslation: Boolean = false
)

data class ChatMessage(
    val role: String,
    val content: String,
    val score: Float? = null,
    val translation: String? = null,
    val suggestedReply: String? = null,
    val scoreError: String? = null,  // ★ 新增：评分错误信息
    val timestamp: Long = System.currentTimeMillis()
)
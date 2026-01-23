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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaMetadataRetriever
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * SparkChain语音识别服务实现
 * 讯飞新版语音听写SDK (SparkChain_Android_2.0.1_rc1)
 * 
 * 使用前需要：
 * 1. 将SparkChain.aar和Codec.aar放入app/libs/目录 ✅
 * 2. 在build.gradle.kts中添加依赖 ✅
 * 3. 在SDKConfig.kt中配置AppID、APIKey、APISecret
 * 4. 在EduSmartApplication中初始化SDK
 */
class SpeechServiceSparkChain(private val context: Context) {
    
    private var mAsr: ASR? = null
    private var isRecognizing = false
    
    companion object {
        private const val TAG = "SpeechServiceSparkChain"
        private var isLibraryLoaded = false
        
        /**
         * 加载本地库
         * 必须在初始化 SDK 之前调用
         */
        private fun loadNativeLibraries() {
            if (isLibraryLoaded) {
                Log.d(TAG, "本地库已加载，跳过")
                return
            }
            
            try {
                // 加载 SparkChain 本地库
                // 注意：库名不包含 "lib" 前缀和 ".so" 后缀
                System.loadLibrary("SparkChain")
                Log.d(TAG, "成功加载 libSparkChain.so")
                
                System.loadLibrary("spark")
                Log.d(TAG, "成功加载 libspark.so")
                
                isLibraryLoaded = true
                Log.d(TAG, "所有本地库加载完成")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "加载本地库失败: ${e.message}", e)
                Log.e(TAG, "可能的原因：")
                Log.e(TAG, "1. .so 文件未正确打包到 APK")
                Log.e(TAG, "2. 架构不匹配（设备架构与 APK 中的架构不一致）")
                Log.e(TAG, "3. 需要完全清理并重新构建项目")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "加载本地库时发生异常", e)
                throw e
            }
        }
        
        /**
         * 初始化SparkChain SDK
         * 需要在Application.onCreate()中调用
         */
        fun initialize(context: Context) {
            Log.d(TAG, "========== initialize() 被调用 ==========")
            
            if (SDKConfig.XUNFEI_APP_ID == "your-xunfei-app-id" ||
                SDKConfig.XUNFEI_API_KEY == "your-xunfei-api-key" ||
                SDKConfig.XUNFEI_API_SECRET == "your-xunfei-api-secret") {
                Log.w(TAG, "SparkChain SDK配置不完整，请检查SDKConfig.kt")
                Log.w(TAG, "AppID: ${SDKConfig.XUNFEI_APP_ID}")
                Log.w(TAG, "APIKey: ${SDKConfig.XUNFEI_API_KEY}")
                Log.w(TAG, "APISecret: ${SDKConfig.XUNFEI_API_SECRET}")
                return
            }
            
            Log.d(TAG, "配置检查通过，开始加载本地库...")
            
            try {
                // 首先加载本地库
                Log.d(TAG, "调用 loadNativeLibraries()...")
                loadNativeLibraries()
                Log.d(TAG, "本地库加载完成，isLibraryLoaded = $isLibraryLoaded")
                
                // 然后初始化 SDK
                Log.d(TAG, "开始初始化 SparkChain SDK...")
                val sparkChainConfig = SparkChainConfig.builder()
                    .appID(SDKConfig.XUNFEI_APP_ID)
                    .apiKey(SDKConfig.XUNFEI_API_KEY)
                    .apiSecret(SDKConfig.XUNFEI_API_SECRET)
                    .logPath("${context.getExternalFilesDir(null)?.absolutePath}/SparkChain.log")
                    .logLevel(LogLvl.VERBOSE.getValue())
                
                Log.d(TAG, "调用 SparkChain.getInst().init()...")
                val ret = SparkChain.getInst().init(context, sparkChainConfig)
                if (ret == 0) {
                    Log.d(TAG, "========== SparkChain SDK初始化成功 ==========")
                } else {
                    Log.e(TAG, "========== SparkChain SDK初始化失败，错误码: $ret ==========")
                }
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "========== SparkChain SDK本地库加载失败 ==========")
                Log.e(TAG, "错误类型: UnsatisfiedLinkError")
                Log.e(TAG, "错误信息: ${e.message}")
                Log.e(TAG, "堆栈跟踪:")
                e.printStackTrace()
                Log.w(TAG, "可能的原因：\n1. .so文件未正确打包到APK\n2. 架构不匹配\n3. 需要完全清理并重新构建项目")
                // 不阻止应用启动，SDK可以在需要时再初始化
            } catch (e: Exception) {
                Log.e(TAG, "========== SparkChain SDK初始化异常 ==========")
                Log.e(TAG, "错误类型: ${e.javaClass.simpleName}")
                Log.e(TAG, "错误信息: ${e.message}")
                Log.e(TAG, "堆栈跟踪:")
                e.printStackTrace()
            }
            
            Log.d(TAG, "========== initialize() 完成 ==========")
        }
    }
    
    /**
     * 语音转文字（文件转写）
     * @param audioPath 音频文件路径
     * @return 转写文本
     */
    suspend fun transcribe(audioPath: String): String = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            var timeoutJob: kotlinx.coroutines.Job? = null
            var audioThread: Thread? = null
            
            try {
                Log.d(TAG, "========== 开始转写音频文件 ==========")
                Log.d(TAG, "音频路径: $audioPath")
                
                // 检查本地库是否已加载
                if (!Companion.isLibraryLoaded) {
                    val errorMsg = "SparkChain SDK本地库未加载。\n\n可能的原因：\n1. .so文件未正确打包到APK\n2. SDK初始化失败\n3. 架构不匹配\n\n录音文件已保存，但转写功能暂时不可用。"
                    Log.e(TAG, errorMsg)
                    continuation.resumeWithException(Exception(errorMsg))
                    return@suspendCancellableCoroutine
                }
                
                // 检查SparkChain是否可用
                try {
                    SparkChain.getInst()
                    Log.d(TAG, "✅ SparkChain SDK实例可用")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ SparkChain SDK未初始化或不可用", e)
                    continuation.resumeWithException(Exception("SparkChain SDK未初始化，请在EduSmartApplication中启用SDK初始化"))
                    return@suspendCancellableCoroutine
                }
                
                val audioFile = File(audioPath)
                if (!audioFile.exists()) {
                    Log.e(TAG, "❌ 音频文件不存在: $audioPath")
                    continuation.resumeWithException(Exception("音频文件不存在: $audioPath"))
                    return@suspendCancellableCoroutine
                }
                
                Log.d(TAG, "✅ 音频文件存在，大小: ${audioFile.length()} bytes")
                Log.d(TAG, "音频文件格式: ${audioFile.extension}")
                
                if (isRecognizing) {
                    Log.w(TAG, "⚠️ 正在识别中，请勿重复开启")
                    continuation.resumeWithException(Exception("正在识别中，请勿重复开启"))
                    return@suspendCancellableCoroutine
                }
                
                // 初始化ASR
                if (mAsr == null) {
                    try {
                        Log.d(TAG, "创建 ASR 对象...")
                        mAsr = ASR()
                        Log.d(TAG, "✅ ASR对象创建成功")
                    } catch (e: Throwable) {
                        val errorMsg = when (e) {
                            is UnsatisfiedLinkError -> {
                                "SparkChain SDK本地库未正确加载。\n\n可能的原因：\n1. .so文件未正确打包到APK\n2. SDK初始化失败\n3. 架构不匹配\n\n录音文件已保存，但转写功能暂时不可用。"
                            }
                            is Error -> {
                                "SparkChain SDK发生错误：${e.message}\n\n录音文件已保存，但转写功能暂时不可用。"
                            }
                            else -> {
                                "创建语音识别器失败：${e.message}\n\n录音文件已保存，但转写功能暂时不可用。"
                            }
                        }
                        Log.e(TAG, "❌ 创建ASR对象失败", e)
                        continuation.resumeWithException(Exception(errorMsg))
                        return@suspendCancellableCoroutine
                    }
                }
                
                val resultBuilder = StringBuilder()
                var isComplete = false
                var hasReceivedResult = false
                
                // 设置超时（30秒）
                timeoutJob = CoroutineScope(Dispatchers.IO).launch {
                    delay(30000) // 30秒超时
                    if (!isComplete && !continuation.isCompleted) {
                        Log.e(TAG, "❌ 转写超时（30秒），未收到结果")
                        isRecognizing = false
                        stopAsr()
                        continuation.resumeWithException(Exception("转写超时，请检查网络连接或音频文件格式"))
                    }
                }
                
                val callbacks = object : AsrCallbacks {
                    override fun onResult(asrResult: ASRResult, o: Any?) {
                        val status = asrResult.status
                        val result = asrResult.bestMatchText
                        
                        hasReceivedResult = true
                        Log.d(TAG, "📝 收到识别结果: status=$status, result=[$result], result长度=${result.length}")
                        
                        when (status) {
                            0 -> { // 第一块结果（初始结果）
                                Log.d(TAG, "第一块结果")
                                resultBuilder.clear()
                                resultBuilder.append(result)
                            }
                            1 -> { // 中间结果（累积的完整结果，不是增量）
                                Log.d(TAG, "中间结果（累积）")
                                // 中间结果是完整的累积结果，直接替换，不要追加
                                resultBuilder.clear()
                                resultBuilder.append(result)
                            }
                            2 -> { // 最后一块结果（最终结果）
                                Log.d(TAG, "✅ 最后一块结果，转写完成")
                                // 最终结果也是完整的，直接使用，不要追加
                                isComplete = true
                                timeoutJob?.cancel()
                                val finalResult = result.trim()
                                Log.d(TAG, "最终转写结果: [$finalResult]")
                                continuation.resume(finalResult)
                                stopAsr()
                            }
                            else -> {
                                Log.w(TAG, "未知状态: $status")
                            }
                        }
                    }
                    
                    override fun onError(asrError: ASRError, o: Any?) {
                        val errorMsg = "识别失败: 错误码=${asrError.code}, 错误信息=${asrError.errMsg}"
                        Log.e(TAG, "❌ $errorMsg")
                        isRecognizing = false
                        timeoutJob?.cancel()
                        continuation.resumeWithException(Exception(errorMsg))
                        stopAsr()
                    }
                    
                    override fun onBeginOfSpeech() {
                        Log.d(TAG, "🎤 开始识别")
                    }
                    
                    override fun onEndOfSpeech() {
                        Log.d(TAG, "🎤 结束识别")
                    }
                }
                
                Log.d(TAG, "注册回调...")
                mAsr?.registerCallbacks(callbacks)
                
                // 设置参数
                Log.d(TAG, "设置ASR参数...")
                mAsr?.language("zh_cn") // 中文识别
                mAsr?.domain("iat") // 日常用语
                mAsr?.accent("mandarin") // 普通话
                mAsr?.vinfo(true) // 返回端点信息
                mAsr?.dwa("wpgs") // 动态修正
                Log.d(TAG, "✅ ASR参数设置完成")
                
                // 开始识别
                Log.d(TAG, "启动识别...")
                val ret = mAsr?.start("file_transcribe")
                if (ret != 0) {
                    Log.e(TAG, "❌ 识别启动失败，错误码: $ret")
                    timeoutJob?.cancel()
                    continuation.resumeWithException(Exception("识别启动失败，错误码: $ret"))
                    return@suspendCancellableCoroutine
                }
                
                isRecognizing = true
                Log.d(TAG, "✅ 识别已启动，开始读取音频文件...")
                
                // 读取音频文件并写入
                // 注意：需要将 m4a 格式解码为 PCM
                audioThread = Thread {
                    try {
                        Log.d(TAG, "开始处理音频文件...")
                        
                        // 检查文件格式
                        val fileExtension = audioFile.extension.lowercase()
                        val pcmData: ByteArray
                        
                        if (fileExtension == "m4a" || fileExtension == "mp4" || fileExtension == "aac") {
                            // 需要解码为 PCM
                            Log.d(TAG, "检测到压缩音频格式 ($fileExtension)，开始解码为 PCM...")
                            pcmData = decodeAudioToPCM(audioPath)
                            Log.d(TAG, "✅ 音频解码完成，PCM 数据大小: ${pcmData.size} bytes")
                        } else {
                            // 假设已经是 PCM 格式，直接读取
                            Log.d(TAG, "假设音频文件已经是 PCM 格式，直接读取...")
                            val fileInputStream = FileInputStream(audioFile)
                            pcmData = fileInputStream.readBytes()
                            fileInputStream.close()
                            Log.d(TAG, "✅ 音频文件读取完成，大小: ${pcmData.size} bytes")
                        }
                        
                        // 发送 PCM 数据到 SDK
                        val bufferSize = 1280
                        var offset = 0
                        var chunkCount = 0
                        
                        while (offset < pcmData.size && isRecognizing) {
                            val chunkSize = minOf(bufferSize, pcmData.size - offset)
                            val chunk = pcmData.copyOfRange(offset, offset + chunkSize)
                            
                            mAsr?.write(chunk)
                            offset += chunkSize
                            chunkCount++
                            
                            if (chunkCount % 10 == 0) {
                                Log.d(TAG, "已发送 $chunkCount 个数据包，共 $offset bytes")
                            }
                            
                            Thread.sleep(40) // 模拟实时流
                        }
                        
                        Log.d(TAG, "✅ 音频数据发送完成，共发送 $chunkCount 个数据包，${pcmData.size} bytes")
                        Log.d(TAG, "等待最后一包结果...")
                        Thread.sleep(100) // 等待数据写入完成
                        mAsr?.stop(false) // 等待最后一包结果
                        Log.d(TAG, "已调用 stop(false)，等待结果回调...")
                        
                        // 如果30秒内没有收到结果，检查是否出错
                        Thread.sleep(5000) // 等待5秒
                        if (!isComplete && !hasReceivedResult) {
                            Log.w(TAG, "⚠️ 5秒内未收到任何结果，可能存在问题")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ 处理音频文件失败", e)
                        timeoutJob?.cancel()
                        if (!isComplete) {
                            continuation.resumeWithException(Exception("音频处理失败: ${e.message}"))
                        }
                    }
                }
                audioThread?.start()
                
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "❌ 语音转写时本地库加载失败", e)
                timeoutJob?.cancel()
                continuation.resumeWithException(Exception("SparkChain SDK本地库未正确加载。\n\n录音文件已保存，但转写功能暂时不可用。"))
            } catch (e: Error) {
                Log.e(TAG, "❌ 语音转写时发生错误", e)
                timeoutJob?.cancel()
                continuation.resumeWithException(Exception("语音转写失败：${e.message}\n\n录音文件已保存，但转写功能暂时不可用。"))
            } catch (e: Exception) {
                Log.e(TAG, "❌ 语音转写异常", e)
                timeoutJob?.cancel()
                continuation.resumeWithException(e)
            } finally {
                // 清理超时任务
                continuation.invokeOnCancellation {
                    timeoutJob?.cancel()
                    audioThread?.interrupt()
                    stopAsr()
                }
            }
        }
    }
    
    /**
     * 将音频文件解码为 PCM 格式
     * @param audioPath 音频文件路径（支持 m4a, mp4, aac 等格式）
     * @return PCM 音频数据（16-bit, 16kHz, 单声道）
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
                    Log.d(TAG, "找到音频轨道: $mime")
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
            
            Log.d(TAG, "音频参数: sampleRate=$sampleRate, channelCount=$channelCount, mime=$mime")
            
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
                    Log.d(TAG, "输出格式已更改: $newFormat")
                }
            }
            
            val pcmData = outputStream.toByteArray()
            Log.d(TAG, "PCM 解码完成，数据大小: ${pcmData.size} bytes")
            
            // 如果需要，可以在这里进行重采样（16kHz, 单声道）
            // 目前先返回原始 PCM 数据
            
            return pcmData
            
        } catch (e: Exception) {
            Log.e(TAG, "音频解码失败", e)
            throw Exception("音频解码失败: ${e.message}", e)
        } finally {
            decoder?.stop()
            decoder?.release()
            extractor.release()
            outputStream.close()
        }
    }
    
    private fun stopAsr() {
        if (isRecognizing && mAsr != null) {
            mAsr?.stop(true)
            isRecognizing = false
        }
    }
    
    /**
     * 实时语音识别（流式）
     * @param onResult 识别结果回调
     */
    fun startRealTimeRecognition(onResult: (String) -> Unit) {
        if (isRecognizing) {
            return
        }
        
        if (mAsr == null) {
            mAsr = ASR()
        }
        
        val callbacks = object : AsrCallbacks {
            override fun onResult(asrResult: ASRResult, o: Any?) {
                val status = asrResult.status
                val result = asrResult.bestMatchText
                
                if (status == 2) { // 最后一块结果
                    onResult(result)
                    stopAsr()
                } else {
                    onResult(result)
                }
            }
            
            override fun onError(asrError: ASRError, o: Any?) {
                isRecognizing = false
                Log.e(TAG, "实时识别错误: ${asrError.errMsg}")
            }
            
            override fun onBeginOfSpeech() {
                Log.d(TAG, "开始实时识别")
            }
            
            override fun onEndOfSpeech() {
                Log.d(TAG, "结束实时识别")
            }
        }
        
        mAsr?.registerCallbacks(callbacks)
        mAsr?.language("zh_cn")
        mAsr?.domain("iat")
        mAsr?.accent("mandarin")
        mAsr?.vinfo(true)
        mAsr?.dwa("wpgs")
        
        val ret = mAsr?.start("realtime_recognition")
        if (ret == 0) {
            isRecognizing = true
        } else {
            Log.e(TAG, "实时识别启动失败，错误码: $ret")
        }
    }
    
    /**
     * 写入音频数据（用于实时识别）
     */
    fun writeAudioData(data: ByteArray) {
        if (isRecognizing && mAsr != null) {
            mAsr?.write(data)
        }
    }
    
    /**
     * 停止实时识别
     */
    fun stopRealTimeRecognition() {
        stopAsr()
    }
    
    /**
     * 发音评分
     * @param audioPath 用户录音路径
     * @param referenceText 参考文本
     * @return 评分 0-100
     */
    suspend fun scorePronunciation(
        audioPath: String,
        referenceText: String
    ): Float = withContext(Dispatchers.IO) {
        // TODO: 如果SparkChain SDK支持发音评测，实现此功能
        // 否则可能需要使用其他SDK
        85.5f
    }
    
    /**
     * 释放资源
     */
    fun release() {
        stopAsr()
        mAsr = null
    }
}


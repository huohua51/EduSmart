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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
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
        
        /**
         * 初始化SparkChain SDK
         * 需要在Application.onCreate()中调用
         */
        fun initialize(context: Context) {
            if (SDKConfig.XUNFEI_APP_ID == "your-xunfei-app-id" ||
                SDKConfig.XUNFEI_API_KEY == "your-xunfei-api-key" ||
                SDKConfig.XUNFEI_API_SECRET == "your-xunfei-api-secret") {
                Log.w(TAG, "SparkChain SDK配置不完整，请检查SDKConfig.kt")
                return
            }
            
            try {
                val sparkChainConfig = SparkChainConfig.builder()
                    .appID(SDKConfig.XUNFEI_APP_ID)
                    .apiKey(SDKConfig.XUNFEI_API_KEY)
                    .apiSecret(SDKConfig.XUNFEI_API_SECRET)
                    .logPath("${context.getExternalFilesDir(null)?.absolutePath}/SparkChain.log")
                    .logLevel(LogLvl.VERBOSE.getValue())
                
                val ret = SparkChain.getInst().init(context, sparkChainConfig)
                if (ret == 0) {
                    Log.d(TAG, "SparkChain SDK初始化成功")
                } else {
                    Log.e(TAG, "SparkChain SDK初始化失败，错误码: $ret")
                }
            } catch (e: Exception) {
                Log.e(TAG, "SparkChain SDK初始化异常", e)
            }
        }
    }
    
    /**
     * 语音转文字（文件转写）
     * @param audioPath 音频文件路径
     * @return 转写文本
     */
    suspend fun transcribe(audioPath: String): String = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                val audioFile = File(audioPath)
                if (!audioFile.exists()) {
                    continuation.resumeWithException(Exception("音频文件不存在: $audioPath"))
                    return@suspendCancellableCoroutine
                }
                
                if (isRecognizing) {
                    continuation.resumeWithException(Exception("正在识别中，请勿重复开启"))
                    return@suspendCancellableCoroutine
                }
                
                // 初始化ASR
                if (mAsr == null) {
                    mAsr = ASR()
                }
                
                val resultBuilder = StringBuilder()
                var isComplete = false
                
                val callbacks = object : AsrCallbacks {
                    override fun onResult(asrResult: ASRResult, o: Any?) {
                        val status = asrResult.status
                        val result = asrResult.bestMatchText
                        
                        Log.d(TAG, "识别结果: status=$status, result=$result")
                        
                        when (status) {
                            0 -> { // 第一块结果
                                resultBuilder.clear()
                                resultBuilder.append(result)
                            }
                            1 -> { // 中间结果
                                resultBuilder.append(result)
                            }
                            2 -> { // 最后一块结果
                                resultBuilder.append(result)
                                isComplete = true
                                continuation.resume(resultBuilder.toString())
                                stopAsr()
                            }
                        }
                    }
                    
                    override fun onError(asrError: ASRError, o: Any?) {
                        val errorMsg = "识别失败: 错误码=${asrError.code}, 错误信息=${asrError.errMsg}"
                        Log.e(TAG, errorMsg)
                        isRecognizing = false
                        continuation.resumeWithException(Exception(errorMsg))
                        stopAsr()
                    }
                    
                    override fun onBeginOfSpeech() {
                        Log.d(TAG, "开始识别")
                    }
                    
                    override fun onEndOfSpeech() {
                        Log.d(TAG, "结束识别")
                    }
                }
                
                mAsr?.registerCallbacks(callbacks)
                
                // 设置参数
                mAsr?.language("zh_cn") // 中文识别
                mAsr?.domain("iat") // 日常用语
                mAsr?.accent("mandarin") // 普通话
                mAsr?.vinfo(true) // 返回端点信息
                mAsr?.dwa("wpgs") // 动态修正
                
                // 开始识别
                val ret = mAsr?.start("file_transcribe")
                if (ret != 0) {
                    continuation.resumeWithException(Exception("识别启动失败，错误码: $ret"))
                    return@suspendCancellableCoroutine
                }
                
                isRecognizing = true
                
                // 读取音频文件并写入
                Thread {
                    try {
                        val fileInputStream = FileInputStream(audioFile)
                        val buffer = ByteArray(1280)
                        var len: Int
                        
                        while (fileInputStream.read(buffer).also { len = it } != -1) {
                            if (!isRecognizing) {
                                break
                            }
                            if (len > 0) {
                                mAsr?.write(buffer.clone())
                                Thread.sleep(40) // 模拟实时流
                            }
                        }
                        
                        fileInputStream.close()
                        Thread.sleep(10)
                        mAsr?.stop(false) // 等待最后一包结果
                    } catch (e: Exception) {
                        Log.e(TAG, "读取音频文件失败", e)
                        if (!isComplete) {
                            continuation.resumeWithException(e)
                        }
                    }
                }.start()
                
            } catch (e: Exception) {
                Log.e(TAG, "语音转写异常", e)
                continuation.resumeWithException(e)
            }
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


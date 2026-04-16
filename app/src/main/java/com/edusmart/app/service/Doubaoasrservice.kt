package com.edusmart.app.service

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import com.edusmart.app.config.SDKConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 豆包ASR语音识别服务
 *
 * 基于火山引擎大模型流式语音识别API
 * 文档: https://www.volcengine.com/docs/6561/1354869
 *
 * 接口地址:
 * - 双向流式模式: wss://openspeech.bytedance.com/api/v3/sauc/bigmodel
 * - 流式输入模式: wss://openspeech.bytedance.com/api/v3/sauc/bigmodel_nostream
 */
class DoubaoASRService(private val context: Context) {

    private val TAG = "DoubaoASRService"

    // 使用流式输入模式（非双向流式），更适合录音文件识别
    private val ASR_URL = "wss://openspeech.bytedance.com/api/v3/sauc/bigmodel_nostream"

    // 音频参数
    private val SAMPLE_RATE = 16000
    private val AUDIO_FORMAT = "pcm"
    private val BITS = 16
    private val CHANNEL = 1

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    /**
     * 识别英文语音
     * @param audioPath 音频文件路径 (支持 m4a, mp3, wav 等格式)
     * @return 识别出的文本
     */
    suspend fun transcribeEnglish(audioPath: String): String = withContext(Dispatchers.IO) {
        transcribe(audioPath, "en-US")
    }

    /**
     * 识别中文语音
     * @param audioPath 音频文件路径
     * @return 识别出的文本
     */
    suspend fun transcribeChinese(audioPath: String): String = withContext(Dispatchers.IO) {
        transcribe(audioPath, "zh-CN")
    }

    /**
     * 语音识别主方法
     * @param audioPath 音频文件路径
     * @param language 语言代码 (en-US, zh-CN 等)
     */
    private suspend fun transcribe(audioPath: String, language: String): String {
        return suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "=== ASR 开始 ===")
            Log.d(TAG, "文件: $audioPath, 语言: $language")

            // 检查文件
            val file = java.io.File(audioPath)
            if (!file.exists()) {
                Log.e(TAG, "音频文件不存在!")
                continuation.resume("")
                return@suspendCancellableCoroutine
            }
            Log.d(TAG, "文件大小: ${file.length()} bytes")

            // 解码音频为 PCM
            val (pcmData, originalSampleRate) = try {
                decodeAudioToPCM(audioPath)
            } catch (e: Exception) {
                Log.e(TAG, "音频解码失败: ${e.message}")
                continuation.resume("")
                return@suspendCancellableCoroutine
            }

            if (pcmData.isEmpty()) {
                Log.e(TAG, "PCM 数据为空")
                continuation.resume("")
                return@suspendCancellableCoroutine
            }

            Log.d(TAG, "PCM 数据大小: ${pcmData.size} bytes")

            // 重采样到 16kHz (如果需要)
            val resampledPcm = resampleTo16k(pcmData, originalSampleRate)
            Log.d(TAG, "重采样后大小: ${resampledPcm.size} bytes")

            val connectId = UUID.randomUUID().toString()

            // 构建 WebSocket 请求，添加鉴权 Header
            val request = Request.Builder()
                .url(ASR_URL)
                .addHeader("X-Api-App-Key", SDKConfig.DOUBAO_APP_ID)
                .addHeader("X-Api-Access-Key", SDKConfig.DOUBAO_ACCESS_TOKEN)
                .addHeader("X-Api-Resource-Id", "volc.bigasr.sauc.duration")
                .addHeader("X-Api-Connect-Id", connectId)
                .build()

            Log.d(TAG, "ConnectId: $connectId")

            var finalResult = StringBuilder()
            var isCompleted = false

            val webSocket = client.newWebSocket(request, object : WebSocketListener() {

                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "✅ ASR WebSocket 已连接")
                    Log.d(TAG, "LogId: ${response.header("X-Tt-Logid")}")

                    // 发送 full client request (配置信息)
                    sendFullClientRequest(webSocket, language, connectId)

                    // 分包发送音频数据
                    sendAudioData(webSocket, resampledPcm)
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    Log.d(TAG, "📥 收到响应: ${bytes.size} bytes")

                    val result = parseResponse(bytes.toByteArray())

                    if (result.error != null) {
                        Log.e(TAG, "❌ ASR 错误: ${result.error}")
                        if (!isCompleted) {
                            isCompleted = true
                            continuation.resume(finalResult.toString())
                        }
                        return
                    }

                    // ★★★ 修复：只有当有实际文本时才更新结果 ★★★
                    if (result.text.isNotEmpty()) {
                        finalResult.clear()
                        finalResult.append(result.text)
                        Log.d(TAG, "识别结果更新: ${result.text}")
                    }

                    // ★★★ 修复：只有当 isLast 为 true 时才返回结果 ★★★
                    if (result.isLast) {
                        Log.d(TAG, "✅ ASR 完成，最终结果: $finalResult")
                        if (!isCompleted) {
                            isCompleted = true
                            webSocket.close(1000, "Complete")
                            continuation.resume(finalResult.toString())
                        }
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Log.d(TAG, "📥 文本响应: $text")
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "❌ ASR 连接失败: ${t.message}")
                    if (!isCompleted) {
                        isCompleted = true
                        continuation.resume(finalResult.toString())
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "ASR WebSocket 已关闭: $code - $reason")
                    if (!isCompleted) {
                        isCompleted = true
                        continuation.resume(finalResult.toString())
                    }
                }
            })

            continuation.invokeOnCancellation {
                webSocket.close(1000, "Cancelled")
            }
        }
    }

    /**
     * 发送 full client request (配置信息)
     */
    private fun sendFullClientRequest(ws: WebSocket, language: String, connectId: String) {
        try {
            val payload = JSONObject().apply {
                put("user", JSONObject().apply {
                    put("uid", "android_user_${System.currentTimeMillis()}")
                })
                put("audio", JSONObject().apply {
                    put("format", AUDIO_FORMAT)
                    put("sample_rate", SAMPLE_RATE)
                    put("bits", BITS)
                    put("channel", CHANNEL)
                    put("language", language)
                })
                put("request", JSONObject().apply {
                    put("model_name", "bigmodel")
                    put("enable_punc", true)        // 启用标点
                    put("enable_itn", true)         // 启用数字规整
                    put("result_type", "full")      // 返回完整结果
                })
            }

            val jsonStr = payload.toString()
            Log.d(TAG, "配置 JSON: ${jsonStr.take(200)}...")

            // 构建 full client request 二进制帧
            // Header: version(4bit) + header_size(4bit) + message_type(4bit) + flags(4bit) + serialization(4bit) + compression(4bit) + reserved(8bit)
            val jsonBytes = jsonStr.toByteArray(Charsets.UTF_8)

            // Header: 0x11 (version=1, header_size=1) + 0x10 (msg_type=1, flags=0) + 0x10 (serial=1, compress=0) + 0x00
            val header = byteArrayOf(
                0x11.toByte(),  // version=1, header_size=1 (4 bytes)
                0x10.toByte(),  // message_type=1 (full client request), flags=0
                0x10.toByte(),  // serialization=1 (JSON), compression=0
                0x00.toByte()   // reserved
            )

            val buffer = ByteBuffer.allocate(header.size + 4 + jsonBytes.size)
                .order(ByteOrder.BIG_ENDIAN)
                .put(header)
                .putInt(jsonBytes.size)
                .put(jsonBytes)

            ws.send(ByteString.of(*buffer.array()))
            Log.d(TAG, "📤 配置请求已发送")

        } catch (e: Exception) {
            Log.e(TAG, "发送配置请求失败: ${e.message}")
        }
    }

    /**
     * 分包发送音频数据
     */
    private fun sendAudioData(ws: WebSocket, pcmData: ByteArray) {
        try {
            val chunkSize = 6400  // 200ms of 16kHz 16bit mono
            val chunks = pcmData.toList().chunked(chunkSize)

            Log.d(TAG, "音频分包: ${chunks.size} 包, 每包约 $chunkSize bytes")

            chunks.forEachIndexed { index, chunk ->
                val audioBytes = chunk.toByteArray()
                val isLast = (index == chunks.size - 1)

                // Audio only request
                // Header: 0x11 + 0x20/0x22 + 0x00 + 0x00
                val flags = if (isLast) 0x02 else 0x00  // flags=2 表示最后一包
                val header = byteArrayOf(
                    0x11.toByte(),
                    (0x20 or flags).toByte(),  // message_type=2 (audio only), flags
                    0x00.toByte(),
                    0x00.toByte()
                )

                val buffer = ByteBuffer.allocate(header.size + 4 + audioBytes.size)
                    .order(ByteOrder.BIG_ENDIAN)
                    .put(header)
                    .putInt(audioBytes.size)
                    .put(audioBytes)

                ws.send(ByteString.of(*buffer.array()))

                // 控制发送速率
                if (!isLast) {
                    Thread.sleep(20)
                }
            }

            Log.d(TAG, "📤 音频数据发送完成")

        } catch (e: Exception) {
            Log.e(TAG, "发送音频数据失败: ${e.message}")
        }
    }

    private data class ASRResult(
        val text: String = "",
        val isLast: Boolean = false,
        val error: String? = null
    )

    /**
     * 解析响应
     */
    private fun parseResponse(data: ByteArray): ASRResult {
        try {
            if (data.size < 4) {
                return ASRResult(error = "响应太短")
            }

            val byte0 = data[0].toInt() and 0xFF
            val byte1 = data[1].toInt() and 0xFF
            val byte2 = data[2].toInt() and 0xFF

            val headerSize = byte0 and 0x0F
            val messageType = (byte1 shr 4) and 0x0F
            val flags = byte1 and 0x0F
            val serialization = (byte2 shr 4) and 0x0F
            val compression = byte2 and 0x0F

            Log.d(TAG, "响应: msgType=$messageType, flags=$flags, serial=$serialization, compress=$compression")

            // 计算 payload 偏移
            val headerBytes = headerSize * 4
            if (data.size < headerBytes + 4) {
                return ASRResult(error = "响应不完整")
            }

            // Payload 大小在 header 之后 (可能有 sequence)
            // 根据协议，可能是 offset 4 或 offset 8
            val payloadSize: Int
            val payloadOffset: Int

            // 尝试从 offset 8 读取 (有 sequence 字段)
            if (data.size >= 12) {
                payloadSize = ByteBuffer.wrap(data, 8, 4).order(ByteOrder.BIG_ENDIAN).int
                payloadOffset = 12
            } else {
                payloadSize = ByteBuffer.wrap(data, 4, 4).order(ByteOrder.BIG_ENDIAN).int
                payloadOffset = 8
            }

            if (payloadSize <= 0 || data.size < payloadOffset + payloadSize) {
                // 可能是空响应或格式不同，尝试其他偏移
                val altPayloadSize = ByteBuffer.wrap(data, 4, 4).order(ByteOrder.BIG_ENDIAN).int
                if (altPayloadSize > 0 && data.size >= 8 + altPayloadSize) {
                    return parsePayload(data, 8, altPayloadSize, compression, flags)
                }
                return ASRResult()
            }

            return parsePayload(data, payloadOffset, payloadSize, compression, flags)

        } catch (e: Exception) {
            Log.e(TAG, "解析响应异常: ${e.message}")
            return ASRResult(error = e.message)
        }
    }

    private fun parsePayload(data: ByteArray, offset: Int, size: Int, compression: Int, flags: Int): ASRResult {
        var payload = data.copyOfRange(offset, offset + size)

        // 解压
        if (compression == 1) {
            try {
                payload = gzipDecompress(payload)
            } catch (e: Exception) {
                Log.w(TAG, "解压失败: ${e.message}")
            }
        }

        val jsonStr = String(payload, Charsets.UTF_8)
        Log.d(TAG, "响应 JSON: ${jsonStr.take(300)}")

        // ★★★ 关键修复：只检查 flags 的第二位 (bit 1) ★★★
        // flags=1 (0b01): 中间结果，不是最终结果
        // flags=2 (0b10): 最终结果标志
        // flags=3 (0b11): 最终结果且有内容
        val isLastFromFlags = (flags and 0x02) != 0

        try {
            val json = JSONObject(jsonStr)

            // 检查错误
            val code = json.optInt("code", 0)
            if (code != 0) {
                val message = json.optString("message", "未知错误")
                return ASRResult(error = "[$code] $message", isLast = true)
            }

            // ★★★ 关键修复：从 result 对象中提取 text ★★★
            var text = ""
            val resultObj = json.optJSONObject("result")
            if (resultObj != null) {
                text = resultObj.optString("text", "")
            }

            // 如果 result.text 为空，尝试其他字段
            if (text.isEmpty()) {
                text = json.optString("text", "")
            }
            if (text.isEmpty()) {
                text = json.optString("result", "")
            }
            if (text.isEmpty()) {
                val payloadObj = json.optJSONObject("payload")
                text = payloadObj?.optString("result", "") ?: ""
            }

            // 检查是否结束 - 优先使用 flags，其次检查 JSON 字段
            val respIsLast = json.optBoolean("is_last", false) ||
                    json.optInt("is_final", 0) == 1

            // ★★★ 最终判断：flags 第二位为 1，或者 JSON 中明确标记结束 ★★★
            val finalIsLast = isLastFromFlags || respIsLast

            Log.d(TAG, "解析结果: text='${text.take(50)}...', flags=$flags, isLastFromFlags=$isLastFromFlags, respIsLast=$respIsLast, finalIsLast=$finalIsLast")

            return ASRResult(text = text, isLast = finalIsLast)

        } catch (e: Exception) {
            Log.w(TAG, "JSON 解析失败: ${e.message}")
            return ASRResult(isLast = isLastFromFlags)
        }
    }

    /**
     * 解码音频文件为 PCM
     * @return Pair of (PCM data, original sample rate)
     */
    private fun decodeAudioToPCM(audioPath: String): Pair<ByteArray, Int> {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        val outputStream = ByteArrayOutputStream()

        try {
            extractor.setDataSource(audioPath)

            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    Log.d(TAG, "音频轨道: $mime")
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                throw Exception("未找到音频轨道")
            }

            extractor.selectTrack(audioTrackIndex)

            val sampleRate = audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"

            Log.d(TAG, "音频参数: rate=$sampleRate, channels=$channelCount, mime=$mime")

            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(audioFormat, null, null, 0)
            decoder.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var inputEOS = false
            var outputEOS = false

            while (!outputEOS) {
                if (!inputEOS) {
                    val inputIndex = decoder.dequeueInputBuffer(10000)
                    if (inputIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputIndex)
                        if (inputBuffer != null) {
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputEOS = true
                            } else {
                                decoder.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputIndex >= 0) {
                    val outputBuffer = decoder.getOutputBuffer(outputIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        val chunk = ByteArray(bufferInfo.size)
                        outputBuffer.get(chunk, 0, bufferInfo.size)
                        outputStream.write(chunk)
                    }
                    decoder.releaseOutputBuffer(outputIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputEOS = true
                    }
                }
            }

            val pcmData = outputStream.toByteArray()
            Log.d(TAG, "PCM 解码完成: ${pcmData.size} bytes, sampleRate=$sampleRate")
            return Pair(pcmData, sampleRate)

        } finally {
            decoder?.stop()
            decoder?.release()
            extractor.release()
            outputStream.close()
        }
    }

    /**
     * 重采样到 16kHz
     * 使用线性插值进行降采样
     */
    private fun resampleTo16k(pcmData: ByteArray, originalSampleRate: Int = 44100): ByteArray {
        if (originalSampleRate == 16000) return pcmData

        val ratio = originalSampleRate.toDouble() / 16000.0
        val sampleCount = pcmData.size / 2  // 16-bit samples
        val outputSampleCount = (sampleCount / ratio).toInt()
        val output = ByteArray(outputSampleCount * 2)

        for (i in 0 until outputSampleCount) {
            val srcIndex = (i * ratio).toInt().coerceIn(0, sampleCount - 1)
            val byteIndex = srcIndex * 2
            if (byteIndex + 1 < pcmData.size) {
                output[i * 2] = pcmData[byteIndex]
                output[i * 2 + 1] = pcmData[byteIndex + 1]
            }
        }
        return output
    }

    private fun gzipCompress(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(data) }
        return bos.toByteArray()
    }

    private fun gzipDecompress(data: ByteArray): ByteArray {
        return GZIPInputStream(data.inputStream()).use { it.readBytes() }
    }
}
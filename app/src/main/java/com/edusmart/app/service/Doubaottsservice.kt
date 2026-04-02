package com.edusmart.app.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.edusmart.app.config.SDKConfig
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * 豆包TTS语音合成服务 - v6 修复 Authorization header 格式
 */
class DoubaoTTSService(private val context: Context) {

    private val TAG = "DoubaoTTSService"
    private var audioTrack: AudioTrack? = null
    private var webSocket: WebSocket? = null
    private val isSpeaking = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val SAMPLE_RATE = 24000
    private val VOICE_TYPE = "zh_female_vv_uranus_bigtts"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    fun speak(
        text: String,
        onStart: () -> Unit = {},
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        Log.d(TAG, "=== speak() 开始 ===")
        stopSpeaking()
        isSpeaking.set(true)

        // ★★★ 关键修复：火山引擎格式是 "Bearer;" 带分号 ★★★
        val request = Request.Builder()
            .url("wss://openspeech.bytedance.com/api/v1/tts/ws_binary")
            .addHeader("Authorization", "Bearer;${SDKConfig.DOUBAO_ACCESS_TOKEN}")
            .build()

        Log.d(TAG, "Authorization: Bearer;${SDKConfig.DOUBAO_ACCESS_TOKEN.take(8)}...")

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            private var hasReceivedAudio = false
            private var totalAudioBytes = 0

            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "✅ WebSocket 已连接")
                sendBinaryRequest(webSocket, text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d(TAG, "📥 收到消息: ${bytes.size} bytes")

                if (!isSpeaking.get()) return

                val result = handleBinaryMessage(bytes.toByteArray(), onStart = {
                    if (!hasReceivedAudio) {
                        hasReceivedAudio = true
                        Log.d(TAG, "🎵 首个音频包")
                        mainHandler.post { onStart() }
                    }
                })

                if (result.audioSize > 0) {
                    totalAudioBytes += result.audioSize
                }

                if (result.error != null) {
                    Log.e(TAG, "❌ ${result.error}")
                    mainHandler.post { onError(result.error) }
                    return
                }

                if (result.isLast) {
                    Log.d(TAG, "✅ 完成，总计: $totalAudioBytes bytes")
                    mainHandler.postDelayed({
                        if (isSpeaking.get()) onComplete()
                    }, 800)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "📥 文本消息: $text")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "❌ 连接失败: ${t.message}")
                mainHandler.post { onError("连接失败: ${t.message}") }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket 关闭: $code")
            }
        })
    }

    private fun sendBinaryRequest(ws: WebSocket, text: String) {
        try {
            Log.d(TAG, "AppID: ${SDKConfig.DOUBAO_APP_ID}")

            val payloadJson = JSONObject().apply {
                put("app", JSONObject().apply {
                    put("appid", SDKConfig.DOUBAO_APP_ID)
                    put("token", SDKConfig.DOUBAO_ACCESS_TOKEN)
                    put("cluster", "volcano_tts")
                })
                put("user", JSONObject().apply {
                    put("uid", "android_user_${System.currentTimeMillis()}")
                })
                put("audio", JSONObject().apply {
                    put("voice_type", VOICE_TYPE)
                    put("encoding", "pcm")
                    put("sample_rate", SAMPLE_RATE)
                })
                put("request", JSONObject().apply {
                    put("reqid", java.util.UUID.randomUUID().toString())
                    put("text", text)
                    put("operation", "submit")
                })
            }

            val jsonBytes = payloadJson.toString().toByteArray(Charsets.UTF_8)
            val compressedPayload = gzipCompress(jsonBytes)

            Log.d(TAG, "原始: ${jsonBytes.size}, 压缩: ${compressedPayload.size}")

            val header = byteArrayOf(
                0x11.toByte(),
                0x10.toByte(),
                0x11.toByte(),
                0x00.toByte()
            )

            val buffer = ByteBuffer.allocate(header.size + 4 + compressedPayload.size)
                .order(ByteOrder.BIG_ENDIAN)
                .put(header)
                .putInt(compressedPayload.size)
                .put(compressedPayload)

            ws.send(ByteString.of(*buffer.array()))
            Log.d(TAG, "📤 发送成功")

        } catch (e: Exception) {
            Log.e(TAG, "发送异常: ${e.message}", e)
        }
    }

    private fun gzipCompress(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(data) }
        return bos.toByteArray()
    }

    private fun gzipDecompress(data: ByteArray): ByteArray {
        return GZIPInputStream(data.inputStream()).use { it.readBytes() }
    }

    private data class ParseResult(
        val isLast: Boolean = false,
        val audioSize: Int = 0,
        val error: String? = null
    )

    private fun handleBinaryMessage(data: ByteArray, onStart: () -> Unit): ParseResult {
        try {
            if (data.size < 12) {
                return ParseResult(error = "数据太短")
            }

            val byte1 = data[1].toInt() and 0xFF
            val byte2 = data[2].toInt() and 0xFF

            val messageType = (byte1 shr 4) and 0x0F
            val flags = byte1 and 0x0F
            val serialization = (byte2 shr 4) and 0x0F
            val compression = byte2 and 0x0F

            Log.d(TAG, "msgType=$messageType, flags=$flags, serial=$serialization, compress=$compression")

            val payloadSize = ByteBuffer.wrap(data, 8, 4).order(ByteOrder.BIG_ENDIAN).int
            Log.d(TAG, "payloadSize=$payloadSize")

            val payloadOffset = 12
            if (data.size < payloadOffset + payloadSize) {
                return ParseResult(error = "数据不完整")
            }

            var payload = data.copyOfRange(payloadOffset, payloadOffset + payloadSize)

            if (compression == 1 && payload.isNotEmpty()) {
                try {
                    payload = gzipDecompress(payload)
                    Log.d(TAG, "解压后: ${payload.size} bytes")
                } catch (e: Exception) {
                    Log.e(TAG, "解压失败: ${e.message}")
                    return ParseResult(error = "解压失败")
                }
            }

            val isLast = (flags and 0x01) != 0

            return when (messageType) {
                0x0B -> {
                    Log.d(TAG, "🎵 音频: ${payload.size} bytes, isLast=$isLast")
                    onStart()
                    writeToAudioTrack(payload)
                    ParseResult(isLast = isLast, audioSize = payload.size)
                }
                0x0C -> {
                    if (serialization == 0) {
                        Log.d(TAG, "🎵 音频响应: ${payload.size} bytes")
                        onStart()
                        writeToAudioTrack(payload)
                        ParseResult(isLast = isLast, audioSize = payload.size)
                    } else {
                        handleJsonResponse(payload, isLast, onStart)
                    }
                }
                0x0F -> {
                    val jsonStr = String(payload, Charsets.UTF_8)
                    Log.d(TAG, "状态响应: $jsonStr")
                    try {
                        val json = JSONObject(jsonStr)
                        val code = json.optInt("code", 0)
                        val message = json.optString("message", "")
                        if (code != 0) {
                            ParseResult(error = "[$code] $message")
                        } else {
                            ParseResult(isLast = isLast)
                        }
                    } catch (e: Exception) {
                        ParseResult()
                    }
                }
                else -> ParseResult()
            }

        } catch (e: Exception) {
            Log.e(TAG, "解析异常: ${e.message}", e)
            return ParseResult(error = e.message)
        }
    }

    private fun handleJsonResponse(payload: ByteArray, isLast: Boolean, onStart: () -> Unit): ParseResult {
        try {
            val jsonStr = String(payload, Charsets.UTF_8)
            Log.d(TAG, "JSON 响应: ${jsonStr.take(200)}")

            val json = JSONObject(jsonStr)
            val code = json.optInt("code", 0)
            if (code != 0) {
                return ParseResult(error = json.optString("message", "错误"))
            }

            val audioData = json.optString("data", "")
            if (audioData.isNotEmpty()) {
                val decoded = android.util.Base64.decode(audioData, android.util.Base64.DEFAULT)
                Log.d(TAG, "🎵 Base64 音频: ${decoded.size} bytes")
                onStart()
                writeToAudioTrack(decoded)
                return ParseResult(isLast = isLast, audioSize = decoded.size)
            }

        } catch (e: Exception) {
            Log.w(TAG, "JSON 处理失败: ${e.message}")
        }
        return ParseResult(isLast = isLast)
    }

    private fun writeToAudioTrack(data: ByteArray) {
        if (!isSpeaking.get()) return

        try {
            if (audioTrack == null) initAudioTrack()
            audioTrack?.write(data, 0, data.size)
        } catch (e: Exception) {
            Log.e(TAG, "写入异常: ${e.message}")
        }
    }

    private fun initAudioTrack() {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
        Log.d(TAG, "✅ AudioTrack 就绪")
    }

    fun stopSpeaking() {
        isSpeaking.set(false)
        webSocket?.close(1000, "Stop")
        webSocket = null

        audioTrack?.apply {
            try {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    flush()
                    stop()
                }
                release()
            } catch (_: Exception) { }
        }
        audioTrack = null
    }
}
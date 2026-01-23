package com.edusmart.app.feature.note

import android.app.*
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.edusmart.app.MainActivity
import com.edusmart.app.R
import java.io.File
import java.io.IOException

class AudioRecordService : Service() {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var isRecording = false
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 需要指定服务类型
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            // Android 12 及以下使用旧的方式
            @Suppress("DEPRECATION")
            startForeground(NOTIFICATION_ID, notification)
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> startRecording()
            ACTION_STOP_RECORDING -> stopRecording()
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "录音服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "后台录音服务"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("正在录音")
            .setContentText("点击返回应用")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun startRecording() {
        if (isRecording) {
            Log.w(TAG, "录音已在进行中，忽略重复启动请求")
            return
        }
        
        try {
            // 确保目录存在
            val audioDir = getExternalFilesDir("audio")
            if (audioDir == null || !audioDir.exists()) {
                Log.e(TAG, "无法访问音频存储目录")
                sendErrorBroadcast("无法访问音频存储目录")
                return
            }
            
            outputFile = File(
                audioDir,
                "recording_${System.currentTimeMillis()}.m4a"
            )
            
            // 确保父目录存在
            outputFile?.parentFile?.mkdirs()
            
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            mediaRecorder?.apply {
                try {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setOutputFile(outputFile!!.absolutePath)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    
                    // 设置最大文件大小和最大持续时间（可选，防止文件过大）
                    setMaxFileSize(50 * 1024 * 1024) // 50MB
                    setMaxDuration(30 * 60 * 1000) // 30分钟
                    
                    prepare()
                    start()
                    isRecording = true
                    Log.d(TAG, "录音已开始: ${outputFile?.absolutePath}")
                    
                    // 发送录音开始广播
                    val startedIntent = Intent(ACTION_RECORDING_STARTED).apply {
                        setPackage(packageName)
                    }
                    sendBroadcast(startedIntent)
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "MediaRecorder 状态错误", e)
                    release()
                    mediaRecorder = null
                    sendErrorBroadcast("录音初始化失败: ${e.message}")
                } catch (e: IOException) {
                    Log.e(TAG, "录音文件IO错误", e)
                    release()
                    mediaRecorder = null
                    sendErrorBroadcast("无法创建录音文件: ${e.message}")
                } catch (e: RuntimeException) {
                    Log.e(TAG, "录音运行时错误", e)
                    release()
                    mediaRecorder = null
                    sendErrorBroadcast("录音启动失败: ${e.message}")
                }
            } ?: run {
                Log.e(TAG, "MediaRecorder 创建失败")
                sendErrorBroadcast("无法创建录音器")
            }
        } catch (e: Exception) {
            Log.e(TAG, "录音启动异常", e)
            sendErrorBroadcast("录音启动异常: ${e.message}")
        }
    }
    
    private fun stopRecording() {
        if (!isRecording) {
            Log.w(TAG, "录音未在进行中，忽略停止请求")
            return
        }
        
        val file = outputFile
        var recordingSuccess = false
        
        mediaRecorder?.apply {
            try {
                // 检查 MediaRecorder 状态
                val state = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12+ 可以查询状态
                    try {
                        // 注意：某些设备可能不支持 getState()，需要捕获异常
                        @Suppress("DEPRECATION")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            // 直接停止，不检查状态
                            stop()
                            recordingSuccess = true
                        } else {
                            stop()
                            recordingSuccess = true
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "停止录音时状态检查失败，继续停止", e)
                        try {
                            stop()
                            recordingSuccess = true
                        } catch (stopException: Exception) {
                            Log.e(TAG, "停止录音失败", stopException)
                            recordingSuccess = false
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    try {
                        stop()
                        recordingSuccess = true
                    } catch (e: Exception) {
                        Log.e(TAG, "停止录音失败", e)
                        recordingSuccess = false
                    }
                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "MediaRecorder 状态错误，无法停止", e)
                recordingSuccess = false
            } catch (e: RuntimeException) {
                Log.e(TAG, "停止录音运行时错误", e)
                recordingSuccess = false
            } catch (e: Exception) {
                Log.e(TAG, "停止录音异常", e)
                recordingSuccess = false
            } finally {
                try {
                    release()
                } catch (e: Exception) {
                    Log.e(TAG, "释放 MediaRecorder 失败", e)
                }
            }
        } ?: run {
            Log.w(TAG, "MediaRecorder 为 null，无法停止")
        }
        
        mediaRecorder = null
        isRecording = false
        
        // 返回录音文件路径或错误信息
        // 即使 recordingSuccess 为 false，只要文件存在且有效，也发送成功广播
        // 因为 MediaRecorder 的 stop() 可能因为状态问题失败，但文件已经写入
        if (file != null && file.exists() && file.length() > 0) {
            Log.d(TAG, "录音完成: ${file.absolutePath}, 大小: ${file.length()} bytes, recordingSuccess=$recordingSuccess")
            val resultIntent = Intent(ACTION_RECORDING_COMPLETE).apply {
                putExtra(EXTRA_AUDIO_PATH, file.absolutePath)
            }
            Log.d(TAG, "发送录音完成广播: action=$ACTION_RECORDING_COMPLETE, path=${file.absolutePath}")
            try {
                // 显式指定包名，确保广播能被正确接收
                resultIntent.setPackage(packageName)
                sendBroadcast(resultIntent)
                Log.d(TAG, "广播已发送到包: $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "发送广播失败", e)
            }
        } else {
            Log.w(TAG, "录音文件无效或不存在: file=$file, exists=${file?.exists()}, length=${file?.length()}")
            if (file != null && file.exists()) {
                // 删除无效文件
                try {
                    file.delete()
                } catch (e: Exception) {
                    Log.w(TAG, "删除无效录音文件失败", e)
                }
            }
            sendErrorBroadcast("录音文件无效或录音失败")
        }
        
        outputFile = null
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private fun sendErrorBroadcast(errorMessage: String) {
        val errorIntent = Intent(ACTION_RECORDING_ERROR).apply {
            putExtra(EXTRA_ERROR_MESSAGE, errorMessage)
            setPackage(packageName)
        }
        sendBroadcast(errorIntent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 确保清理资源
        if (isRecording) {
            stopRecording()
        }
        mediaRecorder?.release()
        mediaRecorder = null
        Log.d(TAG, "服务已销毁")
    }
    
    companion object {
        private const val TAG = "AudioRecordService"
        
        const val ACTION_START_RECORDING = "com.edusmart.app.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.edusmart.app.STOP_RECORDING"
        const val ACTION_RECORDING_COMPLETE = "com.edusmart.app.RECORDING_COMPLETE"
        const val ACTION_RECORDING_STARTED = "com.edusmart.app.RECORDING_STARTED"
        const val ACTION_RECORDING_ERROR = "com.edusmart.app.RECORDING_ERROR"
        const val EXTRA_AUDIO_PATH = "audio_path"
        const val EXTRA_ERROR_MESSAGE = "error_message"
        const val CHANNEL_ID = "audio_record_channel"
        const val NOTIFICATION_ID = 1
    }
}


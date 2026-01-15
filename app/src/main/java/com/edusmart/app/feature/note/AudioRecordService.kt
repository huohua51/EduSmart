package com.edusmart.app.feature.note

import android.app.*
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
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
        startForeground(NOTIFICATION_ID, createNotification())
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
        if (isRecording) return
        
        try {
            outputFile = File(
                getExternalFilesDir("audio"),
                "recording_${System.currentTimeMillis()}.m4a"
            )
            
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(outputFile!!.absolutePath)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                
                try {
                    prepare()
                    start()
                    isRecording = true
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun stopRecording() {
        if (!isRecording) return
        
        mediaRecorder?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaRecorder = null
        isRecording = false
        
        // 返回录音文件路径
        outputFile?.let { file ->
            val resultIntent = Intent(ACTION_RECORDING_COMPLETE).apply {
                putExtra(EXTRA_AUDIO_PATH, file.absolutePath)
            }
            sendBroadcast(resultIntent)
        }
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    companion object {
        const val ACTION_START_RECORDING = "com.edusmart.app.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.edusmart.app.STOP_RECORDING"
        const val ACTION_RECORDING_COMPLETE = "com.edusmart.app.RECORDING_COMPLETE"
        const val EXTRA_AUDIO_PATH = "audio_path"
        const val CHANNEL_ID = "audio_record_channel"
        const val NOTIFICATION_ID = 1
    }
}


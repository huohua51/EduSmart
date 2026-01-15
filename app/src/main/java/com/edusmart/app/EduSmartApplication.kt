package com.edusmart.app

import android.app.Application
import com.edusmart.app.config.SDKConfig
import com.edusmart.app.data.database.EduDatabase
import com.edusmart.app.service.SpeechServiceSparkChain

class EduSmartApplication : Application() {
    
    val database by lazy { EduDatabase.getDatabase(this) }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // 初始化SparkChain SDK（讯飞新版语音听写SDK）
        // 三元组已完整配置: AppID、APIKey、APISecret
        // ⚠️ .so 文件已放置，但可能未正确打包到 APK
        // 暂时禁用初始化，避免应用崩溃
        // 需要完全清理并重新构建项目
        /*
        try {
            if (SDKConfig.XUNFEI_APP_ID != "your-xunfei-app-id" &&
                SDKConfig.XUNFEI_API_KEY != "your-xunfei-api-key" &&
                SDKConfig.XUNFEI_API_SECRET != "your-xunfei-api-secret") {
                SpeechServiceSparkChain.initialize(this)
                android.util.Log.d("EduSmartApplication", "SparkChain SDK初始化已启动")
            } else {
                android.util.Log.w("EduSmartApplication", "SparkChain SDK配置不完整，请检查SDKConfig.kt")
            }
        } catch (e: Exception) {
            android.util.Log.e("EduSmartApplication", "SDK初始化失败", e)
            // 不阻止应用启动，SDK可以在需要时再初始化
        }
        */
        android.util.Log.w("EduSmartApplication", "SparkChain SDK初始化已暂时禁用，需要完全清理并重新构建")
    }
    
    companion object {
        lateinit var instance: EduSmartApplication
            private set
    }
}


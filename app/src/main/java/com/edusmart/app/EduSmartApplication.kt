package com.edusmart.app

import android.app.Application
import com.edusmart.app.config.SDKConfig
import com.edusmart.app.data.database.EduDatabase
import com.edusmart.app.service.SpeechServiceSparkChain
import com.google.android.datatransport.BuildConfig
import timber.log.Timber

class EduSmartApplication : Application() {
    
    val database by lazy { EduDatabase.getDatabase(this) }
    
    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("EduSmartApplication", "========== Application onCreate 开始 ==========")
        instance = this
        
        // 初始化Timber日志库
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.d("Timber日志库初始化完成 - DEBUG模式")
        } else {
            // 生产环境可以添加Crashlytics等日志树
            Timber.plant(Timber.DebugTree()) // 暂时都使用DebugTree
            Timber.d("Timber日志库初始化完成 - RELEASE模式")
        }
        
        // 初始化SparkChain SDK（讯飞新版语音听写SDK）
        // 三元组已完整配置: AppID、APIKey、APISecret
        // .so 文件已放置在 app/src/main/jniLibs/ 目录下
        android.util.Log.d("EduSmartApplication", "准备初始化 SparkChain SDK...")
        android.util.Log.d("EduSmartApplication", "AppID: ${SDKConfig.XUNFEI_APP_ID.take(10)}...")
        android.util.Log.d("EduSmartApplication", "APIKey: ${SDKConfig.XUNFEI_API_KEY.take(10)}...")
        
        try {
            if (SDKConfig.XUNFEI_APP_ID != "your-xunfei-app-id" &&
                SDKConfig.XUNFEI_API_KEY != "your-xunfei-api-key" &&
                SDKConfig.XUNFEI_API_SECRET != "your-xunfei-api-secret") {
                android.util.Log.d("EduSmartApplication", "配置检查通过，开始初始化...")
                SpeechServiceSparkChain.initialize(this)
                android.util.Log.d("EduSmartApplication", "SparkChain SDK初始化调用完成")
            } else {
                android.util.Log.w("EduSmartApplication", "SparkChain SDK配置不完整，请检查SDKConfig.kt")
                android.util.Log.w("EduSmartApplication", "AppID: ${SDKConfig.XUNFEI_APP_ID}")
                android.util.Log.w("EduSmartApplication", "APIKey: ${SDKConfig.XUNFEI_API_KEY}")
                android.util.Log.w("EduSmartApplication", "APISecret: ${SDKConfig.XUNFEI_API_SECRET}")
            }
        } catch (e: UnsatisfiedLinkError) {
            android.util.Log.e("EduSmartApplication", "========== SparkChain SDK本地库加载失败 ==========", e)
            android.util.Log.e("EduSmartApplication", "错误信息: ${e.message}")
            android.util.Log.e("EduSmartApplication", "堆栈跟踪:")
            e.printStackTrace()
            android.util.Log.w("EduSmartApplication", "可能的原因：\n1. .so文件未正确打包到APK\n2. 架构不匹配\n3. 需要完全清理并重新构建项目")
            // 不阻止应用启动，SDK可以在需要时再初始化
        } catch (e: Exception) {
            android.util.Log.e("EduSmartApplication", "========== SparkChain SDK初始化失败 ==========", e)
            android.util.Log.e("EduSmartApplication", "错误信息: ${e.message}")
            android.util.Log.e("EduSmartApplication", "堆栈跟踪:")
            e.printStackTrace()
            // 不阻止应用启动，SDK可以在需要时再初始化
        }
        
        android.util.Log.d("EduSmartApplication", "========== Application onCreate 完成 ==========")
    }
    
    companion object {
        lateinit var instance: EduSmartApplication
            private set
    }
}


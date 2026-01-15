# 快速开始指南

## 🚀 5分钟快速集成SDK

### OCR识别（已集成Google ML Kit）

**✅ 无需配置，直接使用！**

Google ML Kit已经集成完成，可以直接使用：

```kotlin
val ocrService = OCRService()
val text = ocrService.recognizeText(imagePath)
```

**首次使用：**
- 首次识别时会自动下载识别模型（约10-20MB）
- 建议在WiFi环境下使用
- 下载完成后可离线使用

---

### 语音识别（需要配置）

#### 步骤1: 选择SDK方案

**推荐：讯飞SDK**（支持离线识别）

#### 步骤2: 注册并下载SDK

1. 访问 https://www.xfyun.cn/
2. 注册账号并创建应用
3. 下载Android SDK（语音听写）
4. 获取AppID

#### 步骤3: 添加SDK到项目

1. 解压下载的SDK
2. 将 `Msc.jar` 复制到 `app/libs/` 目录
3. 将SDK中的 `assets` 文件复制到 `app/src/main/assets/`

#### 步骤4: 配置build.gradle.kts

在 `app/build.gradle.kts` 的 `dependencies` 中添加：

```kotlin
dependencies {
    // 讯飞SDK
    implementation(files("libs/Msc.jar"))
}
```

#### 步骤5: 配置AppID

编辑 `app/src/main/java/com/edusmart/app/config/SDKConfig.kt`：

```kotlin
const val XUNFEI_APP_ID = "你的AppID" // 替换这里
```

#### 步骤6: 初始化SDK

编辑 `app/src/main/java/com/edusmart/app/EduSmartApplication.kt`，取消注释：

```kotlin
override fun onCreate() {
    super.onCreate()
    instance = this
    
    // 初始化讯飞SDK
    if (SDKConfig.XUNFEI_APP_ID != "your-xunfei-app-id") {
        SpeechUtility.createUtility(this, "appid=${SDKConfig.XUNFEI_APP_ID}")
    }
}
```

#### 步骤7: 完善实现代码

编辑 `app/src/main/java/com/edusmart/app/service/SpeechService.kt`，取消注释并完善 `transcribeWithXunfei()` 方法中的代码。

**参考完整示例：**

```kotlin
import com.iflytek.cloud.*

private suspend fun transcribeWithXunfei(audioPath: String): String {
    return suspendCancellableCoroutine { continuation ->
        val speechRecognizer = SpeechRecognizer.createRecognizer(context, null)
        
        speechRecognizer.setParameter(SpeechConstant.PARAMS, null)
        speechRecognizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD)
        speechRecognizer.setParameter(SpeechConstant.RESULT_TYPE, "json")
        speechRecognizer.setParameter(SpeechConstant.LANGUAGE, "zh_cn")
        speechRecognizer.setParameter(SpeechConstant.ACCENT, "mandarin")
        
        speechRecognizer.setListener(object : RecognizerListener {
            override fun onResult(result: RecognizerResult?, isLast: Boolean) {
                result?.let {
                    val json = JSONObject(it.resultString)
                    val text = json.optString("ws")
                        .split("\"")
                        .filterIndexed { index, _ -> index % 2 == 1 }
                        .joinToString("")
                    
                    if (isLast) {
                        continuation.resume(text)
                        speechRecognizer.destroy()
                    }
                }
            }
            
            override fun onError(error: SpeechError?) {
                speechRecognizer.destroy()
                continuation.resumeWithException(Exception(error?.errorDescription))
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onBeginOfSpeech() {}
            override fun onEndOfSpeech() {}
            override fun onVolumeChanged(volume: Int, data: ByteArray?) {}
        })
        
        val audioFile = File(audioPath)
        val audioData = FileInputStream(audioFile).readBytes()
        speechRecognizer.writeAudio(audioData, 0, audioData.size)
        speechRecognizer.stopListening()
    }
}
```

#### 步骤8: 测试

运行应用，测试语音转写功能：

```kotlin
val speechService = SpeechService(context)
val text = speechService.transcribe(audioPath)
```

---

## 📋 配置检查清单

### OCR配置
- [x] Google ML Kit已集成，可直接使用
- [ ] （可选）如需PaddleOCR，参考 `SDK_INTEGRATION.md`

### 语音识别配置
- [ ] 注册讯飞开放平台账号
- [ ] 下载SDK到 `app/libs/` 目录
- [ ] 在 `build.gradle.kts` 中添加依赖
- [ ] 在 `SDKConfig.kt` 中配置AppID
- [ ] 在 `EduSmartApplication.kt` 中初始化SDK
- [ ] 完善 `SpeechService.kt` 中的实现代码
- [ ] 测试语音转写功能

---

## 🆘 常见问题

### Q: OCR识别失败？
**A:** 
- 检查图片是否清晰
- 确保有网络连接（首次下载模型）
- 检查存储权限

### Q: 语音识别不工作？
**A:**
- 检查是否已添加SDK到libs目录
- 检查AppID是否正确配置
- 检查是否在Application中初始化
- 检查录音权限

### Q: 编译错误 "Cannot resolve symbol"？
**A:**
- 确保已添加SDK依赖
- 同步Gradle: `File -> Sync Project with Gradle Files`
- 清理并重建: `Build -> Clean Project` 然后 `Build -> Rebuild Project`

### Q: 讯飞SDK初始化失败？
**A:**
- 检查AppID是否正确
- 检查assets文件是否已复制
- 检查网络连接（首次使用需要联网）

---

## 📚 详细文档

- **完整集成指南**: 查看 `SDK_INTEGRATION.md`
- **项目配置**: 查看 `SETUP.md`
- **项目说明**: 查看 `README.md`

---

## 💡 提示

1. **OCR**: Google ML Kit已集成，建议先测试OCR功能
2. **语音**: 如果暂时无法集成SDK，可以先使用模拟数据测试其他功能
3. **测试**: 建议在真机上测试，模拟器可能不支持某些功能


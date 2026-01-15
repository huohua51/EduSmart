# SDK集成指南

本文档详细说明如何集成OCR和语音识别SDK。

## 📷 OCR SDK集成

### 方案1: Google ML Kit（推荐，已集成）

**优点：**
- ✅ 免费使用
- ✅ 官方支持，稳定可靠
- ✅ 支持中英文识别
- ✅ 轻量级，易集成
- ✅ 支持离线识别（首次下载模型后）

**已集成步骤：**
1. ✅ 已在 `build.gradle.kts` 中添加依赖
2. ✅ 已在 `OCRService.kt` 中实现识别逻辑
3. ✅ 无需额外配置，可直接使用

**使用方法：**
```kotlin
val ocrService = OCRService()
val text = ocrService.recognizeText(imagePath)
```

**注意事项：**
- 首次使用需要下载识别模型（约10-20MB）
- 建议在WiFi环境下首次使用
- 识别准确率受图片质量影响

---

### 方案2: PaddleOCR（离线识别）

**优点：**
- ✅ 完全离线，无需网络
- ✅ 识别准确率高
- ✅ 支持中英文、数学公式

**集成步骤：**

1. **下载SDK**
   - 访问: https://github.com/PaddlePaddle/PaddleOCR
   - 下载Android版本SDK

2. **添加依赖**
   
   将下载的aar文件放入 `app/libs/` 目录，然后在 `app/build.gradle.kts` 中添加：
   ```kotlin
   dependencies {
       implementation(files("libs/paddleocr.aar"))
   }
   ```

3. **修改OCRService.kt**
   
   在 `OCRService.kt` 中添加PaddleOCR实现：
   ```kotlin
   private suspend fun recognizeWithPaddleOCR(imagePath: String): String {
       // PaddleOCR识别逻辑
   }
   ```

---

### 方案3: 百度OCR API（云端识别）

**优点：**
- ✅ 识别准确率高
- ✅ 支持多种语言
- ❌ 需要网络连接
- ❌ 有调用次数限制

**集成步骤：**

1. **注册账号**
   - 访问: https://cloud.baidu.com/
   - 注册并创建应用
   - 获取API Key和Secret Key

2. **添加依赖**
   ```kotlin
   dependencies {
       implementation("com.baidu.aip:java-sdk:4.16.8")
   }
   ```

3. **配置密钥**
   
   在 `SDKConfig.kt` 中配置：
   ```kotlin
   const val BAIDU_OCR_API_KEY = "your-api-key"
   const val BAIDU_OCR_SECRET_KEY = "your-secret-key"
   ```

4. **实现识别逻辑**
   ```kotlin
   private suspend fun recognizeWithBaiduOCR(imagePath: String): String {
       val client = AipOcr(BAIDU_OCR_API_KEY, BAIDU_OCR_SECRET_KEY)
       val image = FileInputStream(imagePath).readBytes()
       val options = HashMap<String, String>()
       options["language_type"] = "CHN_ENG"
       val result = client.basicGeneral(image, options)
       return result.getJSONArray("words_result")
           .joinToString("\n") { it.getJSONObject("words").getString("word") }
   }
   ```

---

## 🎤 语音识别SDK集成

### 方案1: 讯飞SDK（推荐，支持离线）

**优点：**
- ✅ 支持离线识别
- ✅ 支持方言识别
- ✅ 识别速度快
- ✅ 提供发音评测功能

**集成步骤：**

1. **注册账号并下载SDK**
   - 访问: https://www.xfyun.cn/
   - 注册账号并创建应用
   - 下载Android SDK（语音听写/语音转写）
   - 获取AppID

2. **添加SDK到项目**
   
   - 解压下载的SDK
   - 将 `Msc.jar` 复制到 `app/libs/` 目录
   - 将 `assets` 目录中的文件复制到 `app/src/main/assets/`

3. **配置build.gradle.kts**
   ```kotlin
   dependencies {
       implementation(files("libs/Msc.jar"))
   }
   ```

4. **配置AppID**
   
   在 `SDKConfig.kt` 中配置：
   ```kotlin
   const val XUNFEI_APP_ID = "your-app-id"
   ```

5. **初始化SDK**
   
   在 `Application` 类中初始化：
   ```kotlin
   class EduSmartApplication : Application() {
       override fun onCreate() {
           super.onCreate()
           // 初始化讯飞SDK
           SpeechUtility.createUtility(this, "appid=${SDKConfig.XUNFEI_APP_ID}")
       }
   }
   ```

6. **实现识别逻辑**
   
   在 `SpeechService.kt` 中取消注释并完善讯飞SDK代码。

**完整示例：**
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
                    val text = parseJsonResult(it.resultString)
                    if (isLast) {
                        continuation.resume(text)
                    }
                }
            }
            
            override fun onError(error: SpeechError?) {
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

---

### 方案2: 百度语音SDK

**优点：**
- ✅ 识别准确率高
- ✅ 支持多种语言
- ❌ 需要网络连接

**集成步骤：**

1. **注册账号**
   - 访问: https://ai.baidu.com/
   - 创建应用并获取API Key和Secret Key

2. **添加依赖**
   ```kotlin
   dependencies {
       implementation("com.baidu.aip:java-sdk:4.16.8")
   }
   ```

3. **配置密钥**
   
   在 `SDKConfig.kt` 中配置：
   ```kotlin
   const val BAIDU_API_KEY = "your-api-key"
   const val BAIDU_SECRET_KEY = "your-secret-key"
   ```

4. **实现识别逻辑**
   
   在 `SpeechService.kt` 中取消注释并完善百度SDK代码。

**完整示例：**
```kotlin
import com.baidu.aip.speech.AipSpeech

private suspend fun transcribeWithBaidu(audioPath: String): String {
    return suspendCancellableCoroutine { continuation ->
        val client = AipSpeech(BAIDU_API_KEY, BAIDU_SECRET_KEY)
        client.setConnectionTimeoutInMillis(2000)
        client.setSocketTimeoutInMillis(60000)
        
        val audioFile = File(audioPath)
        val audioData = FileInputStream(audioFile).readBytes()
        
        val options = HashMap<String, Any>()
        options["dev_pid"] = 1537 // 中文普通话
        options["format"] = "pcm"
        options["rate"] = 16000
        
        val result = client.asr(audioData, "pcm", 16000, options)
        
        if (result.has("result")) {
            val text = result.getJSONArray("result")[0].toString()
            continuation.resume(text)
        } else {
            continuation.resumeWithException(Exception("识别失败: ${result.getString("error_msg")}"))
        }
    }
}
```

---

## 🔧 配置检查清单

### OCR配置
- [ ] 选择OCR方案（ML Kit已集成，可直接使用）
- [ ] 如需PaddleOCR，下载SDK并添加到libs目录
- [ ] 如需百度OCR，配置API密钥

### 语音识别配置
- [ ] 选择语音SDK方案（推荐讯飞）
- [ ] 注册对应平台账号
- [ ] 下载SDK到libs目录
- [ ] 配置API密钥/AppID
- [ ] 在Application中初始化SDK
- [ ] 完善SpeechService.kt中的实现代码

### 测试
- [ ] 测试OCR识别功能
- [ ] 测试语音转写功能
- [ ] 测试离线识别（如使用）
- [ ] 检查错误处理和异常情况

---

## 📚 参考文档

- **Google ML Kit**: https://developers.google.com/ml-kit/vision/text-recognition
- **讯飞SDK**: https://www.xfyun.cn/doc/asr/android-sdk.html
- **百度SDK**: https://ai.baidu.com/ai-doc/SPEECH/Vk38lxily
- **PaddleOCR**: https://github.com/PaddlePaddle/PaddleOCR

---

## ⚠️ 注意事项

1. **API密钥安全**
   - 不要将密钥提交到Git仓库
   - 使用环境变量或配置文件（不提交）
   - 生产环境使用密钥管理服务

2. **网络权限**
   - 云端识别需要网络权限
   - 确保AndroidManifest.xml中已声明

3. **存储权限**
   - 读取音频文件需要存储权限
   - Android 10+需要分区存储权限

4. **性能优化**
   - 大文件识别前先压缩
   - 使用协程避免阻塞主线程
   - 合理使用缓存

5. **错误处理**
   - 网络错误重试机制
   - 超时处理
   - 用户友好的错误提示


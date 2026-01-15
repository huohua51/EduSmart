# 功能测试指南

## 🚀 快速开始测试

### 步骤1: 同步项目

1. 打开Android Studio
2. 点击 `File -> Sync Project with Gradle Files`
3. 等待同步完成（查看底部状态栏）

### 步骤2: 检查编译

1. 点击 `Build -> Make Project` 或按 `Ctrl+F9`
2. 查看 `Build` 窗口，确认没有错误
3. 如果有错误，查看错误信息并修复

### 步骤3: 运行应用

1. 连接Android设备或启动模拟器
   - **真机**: 通过USB连接，启用USB调试
   - **模拟器**: 点击设备管理器创建/启动模拟器
2. 点击工具栏的 `Run` 按钮（绿色三角形）或按 `Shift+F10`
3. 选择目标设备
4. 等待应用安装并启动

## 📱 功能测试清单

### ✅ 1. 应用启动测试

**测试步骤：**
1. 运行应用
2. 观察应用是否正常启动
3. 检查底部导航栏是否显示

**预期结果：**
- ✅ 应用正常启动，无崩溃
- ✅ 显示首页界面
- ✅ 底部导航栏显示6个图标（首页、拍照识题、AR、智能笔记、口语私教、知识雷达）

**如果失败：**
- 查看Logcat中的错误信息
- 检查AndroidManifest.xml配置
- 检查MainActivity是否正确

---

### ✅ 2. OCR识别测试（拍照识题）

**测试步骤：**
1. 点击底部导航栏的"拍照识题"或首页的"拍照识题"卡片
2. 点击"开始拍照"按钮
3. 授予相机权限（首次使用）
4. 对准一段文字或题目拍照
5. 等待识别结果

**预期结果：**
- ✅ 相机正常打开
- ✅ 可以正常拍照
- ✅ 显示"正在识别中..."
- ✅ 显示识别结果（文字内容）

**测试图片建议：**
- 清晰的文字图片
- 数学题目
- 中文或英文文本

**如果失败：**
- 检查相机权限是否授予
- 检查网络连接（首次使用需要下载模型）
- 查看Logcat中的错误信息

**查看日志：**
```bash
# 在Logcat中搜索
OCR
ML Kit
识别
```

---

### ✅ 3. 语音识别测试（智能笔记）

**测试步骤：**
1. 点击底部导航栏的"智能笔记"
2. 点击"开始录音"按钮
3. 授予录音权限（首次使用）
4. 说一段话（例如："这是一段测试录音"）
5. 点击"停止录音"
6. 等待转写结果

**预期结果：**
- ✅ 录音按钮变为"停止录音"（红色）
- ✅ 显示"正在录音中..."
- ✅ 停止后显示转写结果

**如果失败：**
- 检查录音权限是否授予
- 检查网络连接（云端识别需要网络）
- 查看Logcat中的SparkChain初始化日志

**查看日志：**
```bash
# 在Logcat中搜索
SpeechServiceSparkChain
SparkChain SDK初始化
识别结果
```

**验证SDK初始化：**
在Logcat中应该看到：
```
SpeechServiceSparkChain: SparkChain SDK初始化成功
```

如果看到"初始化失败"，检查：
- SDKConfig.kt中的三元组是否正确
- 网络连接是否正常

---

### ✅ 4. 其他功能测试

#### AR知识空间
- 点击"AR知识空间"
- 点击"启动AR相机"
- 检查是否正常启动（需要支持ARCore的真机）

#### AI口语私教
- 点击"AI口语私教"
- 选择场景（如"餐厅"）
- 点击"开始对话"
- 测试对话功能

#### 知识雷达
- 点击"知识雷达"
- 点击"10分钟快速测评"
- 测试测评功能

---

## 🔍 日志查看方法

### 在Android Studio中查看日志

1. **打开Logcat**
   - 点击底部工具栏的 `Logcat` 标签
   - 或 `View -> Tool Windows -> Logcat`

2. **过滤日志**
   - 在搜索框输入过滤条件：
     - `SpeechServiceSparkChain` - 语音识别日志
     - `OCRService` - OCR识别日志
     - `EduSmartApplication` - 应用初始化日志
     - `ERROR` - 只显示错误

3. **查看特定标签**
   ```
   tag:SpeechServiceSparkChain
   tag:OCRService
   level:ERROR
   ```

### 关键日志信息

#### SDK初始化成功
```
SpeechServiceSparkChain: SparkChain SDK初始化成功
```

#### SDK初始化失败
```
SpeechServiceSparkChain: SparkChain SDK初始化失败，错误码: xxx
```

#### OCR识别成功
```
识别结果: [识别的文字内容]
```

#### 语音转写成功
```
识别结果: status=2, result=[转写的文字]
```

---

## 🧪 代码测试方法

### 方法1: 单元测试

创建测试类测试各个服务：

```kotlin
// app/src/test/java/com/edusmart/app/service/OCRServiceTest.kt
import org.junit.Test
import org.junit.Assert.*

class OCRServiceTest {
    @Test
    fun testRecognizeText() {
        val ocrService = OCRService()
        // 测试OCR功能
    }
}
```

### 方法2: 在Activity中测试

在MainActivity或测试Activity中添加测试代码：

```kotlin
// 测试OCR
lifecycleScope.launch {
    try {
        val ocrService = OCRService()
        val result = ocrService.recognizeText("/path/to/test_image.jpg")
        Log.d("Test", "OCR结果: $result")
    } catch (e: Exception) {
        Log.e("Test", "OCR测试失败", e)
    }
}

// 测试语音识别
lifecycleScope.launch {
    try {
        val speechService = SpeechService(context)
        val result = speechService.transcribe("/path/to/test_audio.pcm")
        Log.d("Test", "语音转写结果: $result")
    } catch (e: Exception) {
        Log.e("Test", "语音识别测试失败", e)
    }
}
```

---

## 📊 功能验证清单

### 基础功能
- [ ] 应用正常启动
- [ ] 导航栏正常切换
- [ ] 各页面正常显示

### OCR功能
- [ ] 相机权限正常请求
- [ ] 可以正常拍照
- [ ] OCR识别正常工作
- [ ] 识别结果正确显示

### 语音识别功能
- [ ] 录音权限正常请求
- [ ] 可以正常录音
- [ ] SDK初始化成功
- [ ] 语音转写正常工作
- [ ] 转写结果正确显示

### 数据存储
- [ ] 笔记可以保存
- [ ] 错题可以保存
- [ ] 数据可以查询

---

## 🐛 常见问题排查

### 问题1: 应用崩溃

**排查步骤：**
1. 查看Logcat中的崩溃日志
2. 查看堆栈跟踪信息
3. 检查是否有空指针异常
4. 检查权限是否配置

**常见原因：**
- 缺少权限声明
- 空指针异常
- SDK未初始化

### 问题2: OCR识别失败

**排查步骤：**
1. 检查图片是否清晰
2. 检查网络连接（首次使用）
3. 查看Logcat中的错误信息
4. 检查ML Kit是否正确集成

**常见原因：**
- 图片质量太差
- 网络连接失败
- 模型下载失败

### 问题3: 语音识别失败

**排查步骤：**
1. 检查SDK初始化日志
2. 检查三元组配置是否正确
3. 检查网络连接
4. 检查音频文件格式

**常见原因：**
- SDK初始化失败
- 三元组配置错误
- 网络连接失败
- 音频格式不支持

### 问题4: 编译错误

**排查步骤：**
1. 查看Build窗口的错误信息
2. 检查依赖是否正确
3. 同步Gradle项目
4. 清理并重建项目

**常见命令：**
```bash
# 清理项目
Build -> Clean Project

# 重建项目
Build -> Rebuild Project

# 同步Gradle
File -> Sync Project with Gradle Files
```

---

## 🎯 测试场景建议

### 场景1: 完整流程测试

1. 启动应用
2. 拍照识别一道数学题
3. 查看识别结果
4. 开始录音，朗读题目
5. 停止录音，查看转写结果
6. 合并笔记

### 场景2: 边界测试

1. 测试空图片识别
2. 测试模糊图片识别
3. 测试长时间录音
4. 测试网络断开情况
5. 测试权限被拒绝情况

### 场景3: 性能测试

1. 测试连续多次识别
2. 测试大文件识别
3. 测试内存使用情况
4. 测试电池消耗

---

## 📝 测试报告模板

测试完成后，可以记录测试结果：

```
测试日期: [日期]
测试设备: [设备型号/模拟器]
Android版本: [版本号]

功能测试结果:
- OCR识别: ✅/❌
- 语音识别: ✅/❌
- 笔记保存: ✅/❌
- 其他功能: ✅/❌

发现的问题:
1. [问题描述]
2. [问题描述]

修复状态:
- [ ] 已修复
- [ ] 待修复
```

---

## 💡 测试技巧

1. **使用真机测试**
   - 真机测试更接近实际使用场景
   - 某些功能（如AR）只能在真机上测试

2. **查看详细日志**
   - 使用Logcat查看详细日志
   - 过滤关键标签查看特定功能

3. **逐步测试**
   - 先测试基础功能
   - 再测试复杂功能
   - 最后测试完整流程

4. **记录问题**
   - 遇到问题及时记录
   - 记录错误信息和复现步骤

---

## 🎉 测试完成标准

所有功能测试通过的标准：

- ✅ 应用可以正常启动和运行
- ✅ OCR识别功能正常工作
- ✅ 语音识别功能正常工作
- ✅ 数据可以正常保存和查询
- ✅ 没有严重崩溃或错误
- ✅ 用户体验流畅

---

**提示**: 建议先测试基础功能（OCR和语音识别），确认核心功能正常后再测试其他功能。


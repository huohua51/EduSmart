# 彻底解决 .so 文件打包问题

## 🔍 问题分析

即使 .so 文件已经放在 `jniLibs` 目录中，应用仍然找不到文件。这可能是因为：

1. **APK 没有重新打包**：需要完全清理并重新构建
2. **缓存问题**：Gradle 缓存了旧的 APK
3. **文件路径问题**：文件可能没有正确包含在构建中

## ✅ 完整解决方案

### 步骤1: 完全清理项目

在 Android Studio 中：

1. **停止所有运行中的应用**
2. **清理项目**：
   - **Build** → **Clean Project**
   - 等待完成

3. **清理 Gradle 缓存**：
   - **File** → **Invalidate Caches / Restart**
   - 选择：**Invalidate and Restart**
   - 等待 Android Studio 重启

4. **删除构建目录**（可选但推荐）：
   ```powershell
   # 在项目根目录执行
   Remove-Item -Recurse -Force build, app\build -ErrorAction SilentlyContinue
   ```

### 步骤2: 验证 .so 文件位置

确认文件结构正确：

```
app/src/main/jniLibs/
├── arm64-v8a/
│   ├── libSparkChain.so
│   └── libspark.so
├── armeabi-v7a/
│   ├── libSparkChain.so
│   └── libspark.so
├── x86/
│   ├── libSparkChain.so
│   └── libspark.so
└── x86_64/
    ├── libSparkChain.so
    └── libspark.so
```

### 步骤3: 同步 Gradle

- **File** → **Sync Project with Gradle Files**
- 等待同步完成

### 步骤4: 重新构建

1. **Build** → **Rebuild Project**
2. 等待构建完成（可能需要几分钟）

### 步骤5: 卸载旧版本

**重要**：必须卸载旧版本，因为旧版本没有 .so 文件

**方法A: 在设备上卸载**
- 长按应用图标 → 卸载

**方法B: 使用命令**
```powershell
adb uninstall com.edusmart.app
```

### 步骤6: 恢复 SDK 初始化

在 `EduSmartApplication.kt` 中，取消注释初始化代码：

```kotlin
try {
    if (SDKConfig.XUNFEI_APP_ID != "your-xunfei-app-id" &&
        SDKConfig.XUNFEI_API_KEY != "your-xunfei-api-key" &&
        SDKConfig.XUNFEI_API_SECRET != "your-xunfei-api-secret") {
        SpeechServiceSparkChain.initialize(this)
        android.util.Log.d("EduSmartApplication", "SparkChain SDK初始化已启动")
    }
} catch (e: Exception) {
    android.util.Log.e("EduSmartApplication", "SDK初始化失败", e)
}
```

### 步骤7: 重新安装并运行

1. 点击运行按钮 ▶️
2. 应用会重新安装
3. 查看 Logcat，应该看到：
   ```
   SparkChain init
   SparkChain SDK初始化成功
   ```

## 🔧 验证 .so 文件是否在 APK 中

构建完成后，可以检查 APK 文件：

```powershell
# 解压 APK 查看是否包含 .so 文件
# APK 位置：app\build\outputs\apk\debug\app-debug.apk
```

或者使用命令：
```powershell
# 列出 APK 中的 .so 文件
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead("app\build\outputs\apk\debug\app-debug.apk")
$zip.Entries | Where-Object { $_.Name -like "*.so" } | Select-Object FullName
$zip.Dispose()
```

应该看到类似：
```
lib/x86_64/libSparkChain.so
lib/x86_64/libspark.so
lib/arm64-v8a/libSparkChain.so
...
```

## ⚠️ 如果仍然失败

### 方案1: 检查 .so 文件架构

确认 .so 文件确实是 x86_64 架构：

```powershell
# 使用 file 命令（如果安装了 Git Bash）
file app\src\main\jniLibs\x86_64\libSparkChain.so
```

### 方案2: 暂时禁用 SparkChain

如果 .so 文件有问题，可以暂时禁用 SparkChain，让应用先运行起来：

在 `EduSmartApplication.kt` 中保持初始化代码被注释。

### 方案3: 使用真机测试

模拟器可能对 .so 文件支持有问题，可以尝试在真机上测试（需要 arm64-v8a 架构的文件）。

## 📝 当前状态

我已经：
- ✅ 暂时禁用了 SparkChain 初始化（避免崩溃）
- ✅ 更新了 build.gradle.kts 配置
- ✅ 创建了完整的解决指南

**现在请按照上述步骤操作，特别是步骤1-5，这是关键！**


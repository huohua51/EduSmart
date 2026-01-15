# 修复 SparkChain 原生库问题

## 🔍 问题分析

应用崩溃错误：
```
java.lang.UnsatisfiedLinkError: dlopen failed: library "libSparkChain.so" not found
```

**原因**：SparkChain SDK 需要原生库（.so 文件），但这些文件没有正确放置到项目中。

## ✅ 临时解决方案（已应用）

我已经暂时禁用了 SparkChain SDK 的初始化，这样应用可以正常运行，但语音识别功能暂时不可用。

## 🔧 完整解决方案

### 步骤1: 检查 AAR 文件中的 .so 文件

SparkChain SDK 的 AAR 文件位于：
```
app/libs/SparkChain.aar
```

### 步骤2: 提取 .so 文件

**方法A: 使用解压工具**

1. 将 `SparkChain.aar` 重命名为 `SparkChain.zip`
2. 解压 ZIP 文件
3. 找到 `jni/` 或 `libs/` 目录中的 `.so` 文件
4. 根据架构（arm64-v8a, armeabi-v7a, x86, x86_64）分类

**方法B: 使用命令行（Windows）**

```powershell
# 1. 重命名为 ZIP
Rename-Item app\libs\SparkChain.aar app\libs\SparkChain.zip

# 2. 解压到临时目录
Expand-Archive -Path app\libs\SparkChain.zip -DestinationPath app\libs\SparkChain_extracted -Force

# 3. 查找 .so 文件
Get-ChildItem app\libs\SparkChain_extracted -Recurse -Filter "*.so" | Select-Object FullName
```

### 步骤3: 创建 jniLibs 目录结构

在项目中创建以下目录结构：
```
app/src/main/jniLibs/
├── arm64-v8a/
│   └── libSparkChain.so
├── armeabi-v7a/
│   └── libSparkChain.so
├── x86/
│   └── libSparkChain.so
└── x86_64/
    └── libSparkChain.so
```

### 步骤4: 复制 .so 文件

将提取的 .so 文件按照架构复制到对应的目录：

```powershell
# 创建目录
New-Item -ItemType Directory -Path "app\src\main\jniLibs\arm64-v8a" -Force
New-Item -ItemType Directory -Path "app\src\main\jniLibs\armeabi-v7a" -Force
New-Item -ItemType Directory -Path "app\src\main\jniLibs\x86" -Force
New-Item -ItemType Directory -Path "app\src\main\jniLibs\x86_64" -Force

# 复制文件（根据实际路径调整）
# Copy-Item "app\libs\SparkChain_extracted\jni\arm64-v8a\libSparkChain.so" "app\src\main\jniLibs\arm64-v8a\"
# Copy-Item "app\libs\SparkChain_extracted\jni\armeabi-v7a\libSparkChain.so" "app\src\main\jniLibs\armeabi-v7a\"
# Copy-Item "app\libs\SparkChain_extracted\jni\x86\libSparkChain.so" "app\src\main\jniLibs\x86\"
# Copy-Item "app\libs\SparkChain_extracted\jni\x86_64\libSparkChain.so" "app\src\main\jniLibs\x86_64\"
```

### 步骤5: 恢复 SDK 初始化

在 `EduSmartApplication.kt` 中，取消注释初始化代码：

```kotlin
// 取消注释这部分代码
try {
    if (SDKConfig.XUNFEI_APP_ID != "your-xunfei-app-id" &&
        SDKConfig.XUNFEI_API_KEY != "your-xunfei-api-key" &&
        SDKConfig.XUNFEI_API_SECRET != "your-xunfei-api-secret") {
        SpeechServiceSparkChain.initialize(this)
    }
} catch (e: Exception) {
    android.util.Log.e("EduSmartApplication", "SDK初始化失败", e)
}
```

### 步骤6: 重新构建和运行

1. 同步 Gradle
2. 清理项目：**Build** → **Clean Project**
3. 重新构建：**Build** → **Rebuild Project**
4. 运行应用

## 📝 快速检查清单

- [ ] 已创建 `app/src/main/jniLibs/` 目录
- [ ] 已创建架构子目录（arm64-v8a, armeabi-v7a, x86, x86_64）
- [ ] 已从 AAR 中提取 .so 文件
- [ ] 已将 .so 文件复制到对应架构目录
- [ ] 已恢复 SDK 初始化代码
- [ ] 已重新构建项目

## ⚠️ 注意事项

1. **架构支持**：
   - `arm64-v8a` - 64位 ARM（现代手机）
   - `armeabi-v7a` - 32位 ARM（旧手机）
   - `x86` - 32位 x86（模拟器）
   - `x86_64` - 64位 x86（模拟器）

2. **最小支持**：
   - 如果只在真机上测试，至少需要 `arm64-v8a`
   - 如果使用模拟器，需要 `x86` 或 `x86_64`

3. **文件大小**：
   - .so 文件通常比较大（几MB到几十MB）
   - 确保所有架构的文件都已正确复制

## 🚀 当前状态

- ✅ 应用可以正常运行（SparkChain 已暂时禁用）
- ✅ 其他功能正常（拍照识题、AR、笔记等）
- ⚠️ 语音识别功能暂时不可用（需要 .so 文件）

---

**现在应用可以正常运行了！如果需要使用语音识别功能，请按照上述步骤提取和配置 .so 文件。**


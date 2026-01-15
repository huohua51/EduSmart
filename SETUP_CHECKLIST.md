# 项目配置检查清单

使用此清单确保项目在其他电脑上正确配置。

## ✅ 环境检查

### 必需软件

- [ ] Android Studio Hedgehog (2023.1.1) 或更高版本已安装
- [ ] JDK 17 或 JDK 21 已安装并配置
- [ ] Android SDK Platform 34 已安装
- [ ] Android SDK Build-Tools 已安装
- [ ] 网络连接正常（用于下载依赖）

### 项目文件

- [ ] `app/libs/SparkChain.aar` 文件存在
- [ ] `app/libs/Codec.aar` 文件存在
- [ ] `app/src/main/jniLibs/arm64-v8a/libSparkChain.so` 存在
- [ ] `app/src/main/jniLibs/arm64-v8a/libspark.so` 存在
- [ ] `app/src/main/jniLibs/armeabi-v7a/libSparkChain.so` 存在
- [ ] `app/src/main/jniLibs/armeabi-v7a/libspark.so` 存在
- [ ] `app/src/main/assets/models/` 目录存在（可选，用于 AR 模型）

## ✅ 配置文件检查

### Gradle 配置

- [ ] `gradle/wrapper/gradle-wrapper.properties` 中 Gradle 版本为 8.5
- [ ] `settings.gradle.kts` 中 AGP 版本为 8.1.0
- [ ] `settings.gradle.kts` 中 Kotlin 版本为 1.9.10
- [ ] `app/build.gradle.kts` 中 compileSdk 为 34
- [ ] `app/build.gradle.kts` 中 minSdk 为 26
- [ ] `app/build.gradle.kts` 中 targetSdk 为 34
- [ ] `app/build.gradle.kts` 中 Java 版本为 17

### Gradle 属性

- [ ] `gradle.properties` 中包含 `android.overridePathCheck=true`
- [ ] `gradle.properties` 中包含 `android.suppressUnsupportedCompileSdk=34`
- [ ] `gradle.properties` 中包含 KAPT 的 `--add-opens` 参数
- [ ] `gradle.properties` 中包含 `android.useAndroidX=true`
- [ ] `gradle.properties` 中包含 `android.enableJetifier=true`

### 仓库配置

- [ ] `settings.gradle.kts` 中包含 `google()` 仓库
- [ ] `settings.gradle.kts` 中包含 `mavenCentral()` 仓库
- [ ] `settings.gradle.kts` 中包含 `jitpack.io` 仓库

## ✅ Android Studio 配置

### JDK 设置

- [ ] `File -> Settings -> Build Tools -> Gradle -> Gradle JDK` 设置为 JDK 17 或 JDK 21
- [ ] JDK 版本正确显示（不是 "Invalid"）

### Gradle 设置

- [ ] `File -> Settings -> Build Tools -> Gradle` 选择 "Use Gradle from: 'gradle-wrapper.properties' file"
- [ ] Gradle 版本显示为 8.5

### Android SDK 设置

- [ ] `File -> Settings -> Appearance & Behavior -> System Settings -> Android SDK` 中已安装 "Android 14.0 (API 34)"
- [ ] Android SDK Build-Tools 已安装

## ✅ 项目同步

### 首次同步

- [ ] 打开项目后，Gradle 自动开始同步
- [ ] 等待 Gradle 8.5 下载完成（约 100MB）
- [ ] 等待所有依赖库下载完成（约 500MB-1GB）
- [ ] 同步过程中没有错误（只有警告可以忽略）

### 同步后检查

- [ ] `Build` 窗口中没有错误（红色）
- [ ] 只有警告（黄色），可以忽略
- [ ] 项目结构正确显示
- [ ] 所有依赖库都能正确解析

## ✅ 构建测试

### 清理和构建

- [ ] `Build -> Clean Project` 执行成功
- [ ] `Build -> Rebuild Project` 执行成功
- [ ] 没有编译错误

### 运行测试

- [ ] 连接 Android 设备或启动模拟器
- [ ] 点击 "Run" 按钮
- [ ] 应用成功安装到设备
- [ ] 应用可以正常启动

## ⚠️ 常见问题检查

### 如果 Gradle 同步失败

- [ ] 检查网络连接
- [ ] 检查 JDK 版本
- [ ] 尝试 `File -> Invalidate Caches / Restart`
- [ ] 检查 `gradle.properties` 配置

### 如果找不到 AAR 文件

- [ ] 确认 `app/libs/` 目录存在
- [ ] 确认 AAR 文件在正确位置
- [ ] 从原始项目复制缺失的文件

### 如果原生库加载失败

- [ ] 确认 `app/src/main/jniLibs/` 目录存在
- [ ] 确认所有 `.so` 文件存在
- [ ] 从原始项目复制缺失的文件

### 如果 KAPT 编译错误

- [ ] 检查 `gradle.properties` 中的 `--add-opens` 参数
- [ ] 确认 JDK 版本为 17 或 21
- [ ] 清理项目并重新构建

## 📋 快速检查命令

在项目根目录运行以下命令快速检查：

### Windows (PowerShell)

```powershell
# 检查 AAR 文件
Test-Path "app\libs\SparkChain.aar"
Test-Path "app\libs\Codec.aar"

# 检查原生库
Test-Path "app\src\main\jniLibs\arm64-v8a\libSparkChain.so"
Test-Path "app\src\main\jniLibs\armeabi-v7a\libSparkChain.so"

# 检查 Gradle 版本
Select-String -Path "gradle\wrapper\gradle-wrapper.properties" -Pattern "gradle-8.5"
```

### macOS/Linux

```bash
# 检查 AAR 文件
ls app/libs/SparkChain.aar
ls app/libs/Codec.aar

# 检查原生库
ls app/src/main/jniLibs/arm64-v8a/libSparkChain.so
ls app/src/main/jniLibs/armeabi-v7a/libSparkChain.so

# 检查 Gradle 版本
grep "gradle-8.5" gradle/wrapper/gradle-wrapper.properties
```

---

**提示**: 如果所有项目都打勾 ✅，说明项目配置正确，可以正常构建和运行。


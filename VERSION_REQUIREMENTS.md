# 项目版本要求详细说明

本文档详细列出了项目所需的所有版本信息，方便在其他电脑上配置和运行。

## 📋 目录

1. [开发环境要求](#开发环境要求)
2. [Gradle 配置](#gradle-配置)
3. [Android SDK 版本](#android-sdk-版本)
4. [依赖库版本](#依赖库版本)
5. [特殊配置](#特殊配置)
6. [在其他电脑上运行步骤](#在其他电脑上运行步骤)

---

## 🖥️ 开发环境要求

### 必需软件

| 软件 | 版本要求 | 说明 |
|------|---------|------|
| **Android Studio** | **Hedgehog (2023.1.1)** 或更高版本 | 推荐使用最新稳定版 |
| **JDK** | **JDK 17** 或 **JDK 21** | 必须，Gradle 8.5 需要 JDK 17+ |
| **Gradle** | **8.5** | 由 Gradle Wrapper 自动管理 |
| **Android SDK** | **API 34** (Android 14) | 编译目标 |
| **Kotlin** | **1.9.10** | 由插件管理 |

### 系统要求

- **操作系统**: Windows 10/11, macOS, Linux
- **内存**: 至少 8GB RAM（推荐 16GB）
- **磁盘空间**: 至少 10GB 可用空间（用于 SDK 和依赖）

---

## 🔧 Gradle 配置

### Gradle 版本

**文件**: `gradle/wrapper/gradle-wrapper.properties`

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
```

- **Gradle**: `8.5`
- **说明**: 由 Gradle Wrapper 自动下载，无需手动安装

### Android Gradle Plugin (AGP)

**文件**: `settings.gradle.kts`

```kotlin
id("com.android.application") version "8.1.0"
```

- **AGP**: `8.1.0`
- **说明**: Android 构建工具版本

### Kotlin 版本

**文件**: `settings.gradle.kts`

```kotlin
id("org.jetbrains.kotlin.android") version "1.9.10"
id("org.jetbrains.kotlin.kapt") version "1.9.10"
```

- **Kotlin**: `1.9.10`
- **说明**: Kotlin 编译器和 KAPT 版本必须一致

### Compose Compiler 版本

**文件**: `app/build.gradle.kts`

```kotlin
kotlinCompilerExtensionVersion = "1.5.3"
```

- **Compose Compiler**: `1.5.3`
- **说明**: 必须与 Kotlin 1.9.10 兼容

---

## 📱 Android SDK 版本

**文件**: `app/build.gradle.kts`

```kotlin
compileSdk = 34
minSdk = 26
targetSdk = 34
```

| 配置项 | 版本 | 说明 |
|--------|------|------|
| **compileSdk** | `34` | 编译时使用的 SDK 版本（Android 14） |
| **minSdk** | `26` | 最低支持的 Android 版本（Android 8.0） |
| **targetSdk** | `34` | 目标 Android 版本（Android 14） |

### Java 版本

```kotlin
sourceCompatibility = JavaVersion.VERSION_17
targetCompatibility = JavaVersion.VERSION_17
jvmTarget = "17"
```

- **Java**: `17`
- **说明**: 源代码和目标字节码版本

---

## 📦 依赖库版本

### Core Android 库

| 库名 | 版本 | 说明 |
|------|------|------|
| `androidx.core:core-ktx` | `1.12.0` | Android KTX 核心库 |
| `androidx.lifecycle:lifecycle-runtime-ktx` | `2.6.2` | 生命周期运行时 |
| `androidx.activity:activity-compose` | `1.8.1` | Compose Activity 支持 |
| `androidx.appcompat:appcompat` | `1.6.1` | AppCompat 兼容库 |

### Jetpack Compose

| 库名 | 版本 | 说明 |
|------|------|------|
| `androidx.compose:compose-bom` | `2023.10.01` | Compose BOM（统一版本管理） |
| `androidx.compose.ui:ui` | BOM 管理 | Compose UI 核心 |
| `androidx.compose.ui:ui-graphics` | BOM 管理 | Compose 图形 |
| `androidx.compose.ui:ui-tooling-preview` | BOM 管理 | Compose 预览工具 |
| `androidx.compose.material3:material3` | BOM 管理 | Material 3 设计库 |
| `androidx.compose.material:material-icons-extended` | BOM 管理 | Material 图标扩展 |
| `androidx.navigation:navigation-compose` | `2.7.5` | Compose 导航 |

### Camera & Media

| 库名 | 版本 | 说明 |
|------|------|------|
| `androidx.camera:camera-camera2` | `1.3.0` | Camera2 API |
| `androidx.camera:camera-lifecycle` | `1.3.0` | Camera 生命周期 |
| `androidx.camera:camera-view` | `1.3.0` | Camera 视图 |

### ARCore & 3D

| 库名 | 版本 | 说明 |
|------|------|------|
| `com.google.ar:core` | `1.40.0` | Google ARCore SDK |
| `io.github.sceneview:arsceneview` | `2.6.0` | SceneView AR 库 |
| `org.joml:joml` | `1.10.5` | 3D 数学库 |

### OCR (Google ML Kit)

| 库名 | 版本 | 说明 |
|------|------|------|
| `com.google.mlkit:text-recognition` | `16.0.0` | 文本识别 |
| `com.google.mlkit:text-recognition-chinese` | `16.0.0` | 中文文本识别 |

### 网络请求

| 库名 | 版本 | 说明 |
|------|------|------|
| `com.squareup.retrofit2:retrofit` | `2.9.0` | Retrofit 网络库 |
| `com.squareup.retrofit2:converter-gson` | `2.9.0` | Gson 转换器 |
| `com.squareup.okhttp3:logging-interceptor` | `4.12.0` | OkHttp 日志拦截器 |
| `com.google.code.gson:gson` | `2.10.1` | Gson JSON 库 |

### 数据库 (Room)

| 库名 | 版本 | 说明 |
|------|------|------|
| `androidx.room:room-runtime` | `2.6.1` | Room 运行时 |
| `androidx.room:room-ktx` | `2.6.1` | Room KTX 扩展 |
| `androidx.room:room-compiler` | `2.6.1` | Room 编译器（KAPT） |

### 其他库

| 库名 | 版本 | 说明 |
|------|------|------|
| `com.github.bumptech.glide:glide` | `4.16.0` | 图片加载库 |
| `com.google.accompanist:accompanist-permissions` | `0.32.0` | 权限处理库 |
| `com.github.PhilJay:MPAndroidChart` | `3.1.0` | 图表库（来自 JitPack） |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | `1.7.3` | Kotlin 协程 |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | `2.6.2` | ViewModel Compose 支持 |

### 测试库

| 库名 | 版本 | 说明 |
|------|------|------|
| `junit:junit` | `4.13.2` | JUnit 测试框架 |
| `androidx.test.ext:junit` | `1.1.5` | Android JUnit 扩展 |
| `androidx.test.espresso:espresso-core` | `3.5.1` | Espresso UI 测试 |

### 本地 AAR 文件

**文件**: `app/libs/`

- `SparkChain.aar` - 讯飞 SparkChain SDK
- `Codec.aar` - 讯飞编解码库

**说明**: 这些是本地 AAR 文件，需要放在 `app/libs/` 目录下。

---

## ⚙️ 特殊配置

### Gradle 属性

**文件**: `gradle.properties`

```properties
# JVM 参数（支持 KAPT 在 JDK 17+ 上运行）
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8 --add-opens=...

# AndroidX 支持
android.useAndroidX=true
android.enableJetifier=true

# Kotlin 代码风格
kotlin.code.style=official

# 允许非 ASCII 路径（中文路径支持）
android.overridePathCheck=true

# 抑制 compileSdk 34 警告
android.suppressUnsupportedCompileSdk=34
```

### 仓库配置

**文件**: `settings.gradle.kts`

```kotlin
repositories {
    google()           // Google Maven 仓库
    mavenCentral()     // Maven Central 仓库
    maven { url = uri("https://jitpack.io") }  // JitPack 仓库（用于 MPAndroidChart）
}
```

---

## 🚀 在其他电脑上运行步骤

### 步骤 1: 安装开发环境

1. **安装 Android Studio**
   - 下载: https://developer.android.com/studio
   - 版本: Hedgehog (2023.1.1) 或更高
   - 安装时选择 "Standard" 安装，会自动安装 Android SDK

2. **配置 JDK**
   - Android Studio 会自动安装 JDK，但需要确认版本
   - 打开: `File -> Settings -> Build Tools -> Gradle -> Gradle JDK`
   - 选择: **JDK 17** 或 **JDK 21**
   - 如果没有，点击 "Download JDK" 下载

3. **安装 Android SDK**
   - 打开: `Tools -> SDK Manager`
   - 确保安装:
     - ✅ Android SDK Platform 34
     - ✅ Android SDK Build-Tools 34.x.x
     - ✅ Android SDK Platform-Tools
     - ✅ Android SDK Command-line Tools

### 步骤 2: 导入项目

1. **打开项目**
   - 打开 Android Studio
   - 选择 `File -> Open`
   - 选择项目根目录（包含 `build.gradle.kts` 的目录）

2. **等待 Gradle 同步**
   - Android Studio 会自动开始 Gradle 同步
   - 首次同步会下载 Gradle 8.5（约 100MB）
   - 然后下载所有依赖库（约 500MB-1GB，取决于网络速度）
   - **注意**: 首次同步可能需要 10-30 分钟

3. **检查同步状态**
   - 查看底部状态栏的 "Gradle Sync" 进度
   - 如果失败，查看 "Build" 窗口的错误信息

### 步骤 3: 配置项目

1. **检查本地 AAR 文件**
   - 确认 `app/libs/` 目录存在
   - 确认 `SparkChain.aar` 和 `Codec.aar` 文件存在
   - 如果缺失，需要从原始项目复制

2. **检查原生库文件**
   - 确认 `app/src/main/jniLibs/` 目录存在
   - 确认包含以下文件:
     - `arm64-v8a/libSparkChain.so`
     - `arm64-v8a/libspark.so`
     - `armeabi-v7a/libSparkChain.so`
     - `armeabi-v7a/libspark.so`
   - 如果缺失，需要从原始项目复制

3. **检查 assets 目录**
   - 确认 `app/src/main/assets/models/` 目录存在（可选，用于 AR 模型）

### 步骤 4: 验证配置

1. **检查 Gradle 版本**
   - 打开: `File -> Settings -> Build Tools -> Gradle`
   - 选择: "Use Gradle from: 'gradle-wrapper.properties' file"
   - 确认版本: 8.5

2. **检查 JDK 版本**
   - 打开: `File -> Settings -> Build Tools -> Gradle -> Gradle JDK`
   - 确认版本: JDK 17 或 JDK 21

3. **检查 Android SDK**
   - 打开: `File -> Settings -> Appearance & Behavior -> System Settings -> Android SDK`
   - 确认 "SDK Platforms" 中已安装 "Android 14.0 (API 34)"

### 步骤 5: 构建和运行

1. **清理项目**
   - `Build -> Clean Project`

2. **同步 Gradle**
   - `File -> Sync Project with Gradle Files`

3. **构建项目**
   - `Build -> Rebuild Project`

4. **运行应用**
   - 连接 Android 设备或启动模拟器
   - 点击 "Run" 按钮（绿色播放图标）

---

## ⚠️ 常见问题

### 问题 1: Gradle 同步失败

**可能原因**:
- 网络问题（无法访问 Maven 仓库）
- JDK 版本不匹配
- Gradle 缓存损坏

**解决方法**:
1. 检查网络连接
2. 确认 JDK 版本为 17 或 21
3. 清理 Gradle 缓存: `File -> Invalidate Caches / Restart`

### 问题 2: 找不到 AAR 文件

**解决方法**:
- 确认 `app/libs/` 目录存在
- 确认 `SparkChain.aar` 和 `Codec.aar` 文件存在
- 如果缺失，从原始项目复制

### 问题 3: 原生库加载失败

**解决方法**:
- 确认 `app/src/main/jniLibs/` 目录存在
- 确认所有 `.so` 文件存在
- 如果缺失，从原始项目复制

### 问题 4: KAPT 编译错误

**解决方法**:
- 确认 `gradle.properties` 中包含 `--add-opens` 参数
- 确认 JDK 版本为 17 或 21
- 清理项目: `Build -> Clean Project`

### 问题 5: 中文路径问题

**解决方法**:
- 确认 `gradle.properties` 中包含 `android.overridePathCheck=true`
- 如果仍有问题，考虑将项目移到英文路径

---

## 📝 版本兼容性表

| 组件 | 版本 | 兼容性说明 |
|------|------|-----------|
| Gradle | 8.5 | 需要 JDK 17+ |
| AGP | 8.1.0 | 需要 Gradle 8.0+ |
| Kotlin | 1.9.10 | 与 Compose Compiler 1.5.3 兼容 |
| Compose Compiler | 1.5.3 | 需要 Kotlin 1.9.10 |
| compileSdk | 34 | AGP 8.1.0 支持（有警告，但可用） |
| minSdk | 26 | 支持 Android 8.0+ |
| targetSdk | 34 | Android 14 |

---

## 🔗 参考链接

- [Android Studio 下载](https://developer.android.com/studio)
- [Gradle 版本说明](https://docs.gradle.org/current/release-notes.html)
- [Kotlin 版本说明](https://kotlinlang.org/docs/releases.html)
- [Compose 版本说明](https://developer.android.com/jetpack/androidx/releases/compose)
- [ARCore 设备支持](https://developers.google.com/ar/devices)

---

**最后更新**: 2026-01-14

**项目名称**: EduSmart

**维护者**: 请根据实际情况填写


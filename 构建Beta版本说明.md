# 构建 Beta 版本 APK 说明

## 方法一：使用构建脚本（推荐）

### Windows 系统
双击运行 `build_beta.bat` 文件，或在命令行中执行：
```cmd
build_beta.bat
```

### Linux/Mac 系统
在终端中执行：
```bash
chmod +x build_beta.sh
./build_beta.sh
```

## 方法二：使用 Gradle 命令

### 如果项目有 Gradle Wrapper
```cmd
gradlew.bat assembleBeta
```

### 如果没有 Gradle Wrapper，使用系统 Gradle
```cmd
gradle assembleBeta
```

## 方法三：使用 Android Studio

1. 打开 Android Studio
2. 打开项目
3. 点击菜单：`Build` → `Select Build Variant`
4. 在 `Build Variants` 面板中，选择 `beta` 变体
5. 点击菜单：`Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`

## 构建输出位置

构建完成后，APK 文件位于：
```
app/build/outputs/apk/beta/app-beta-beta.apk
```

## Beta 版本信息

- **版本号**: 1.0.0-beta
- **版本代码**: 2
- **应用ID**: com.edusmart.app.beta
- **特性**: 
  - 不混淆代码（方便调试）
  - 可调试
  - 包含所有功能

## 注意事项

1. **首次构建可能需要较长时间**（下载依赖）
2. **确保网络连接正常**（需要下载依赖）
3. **确保已安装 JDK 17**（项目要求）
4. **Beta 版本未签名**，安装时可能需要允许"未知来源"

## 安装 Beta 版本

1. 将生成的 APK 文件传输到 Android 设备
2. 在设备上启用"未知来源"安装权限
3. 点击 APK 文件进行安装

## 故障排除

### 构建失败
- 检查网络连接
- 检查 JDK 版本（需要 JDK 17）
- 查看错误日志，根据提示修复

### 找不到 gradlew.bat
- 使用 Android Studio 打开项目，会自动生成 Gradle Wrapper
- 或手动运行：`gradle wrapper`

### 依赖下载失败
- 检查网络连接
- 可能需要配置代理（如果在中国大陆）
- 检查 `settings.gradle.kts` 中的仓库配置


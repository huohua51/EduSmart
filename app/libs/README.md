# libs 目录说明

此目录用于存放第三方SDK的JAR/AAR文件。

## 讯飞SDK集成

### 需要添加的文件

1. **Msc.jar** - 讯飞语音SDK核心库
   - 从讯飞开放平台下载: https://www.xfyun.cn/
   - 解压SDK后，将 `Msc.jar` 复制到此目录

2. **Assets文件** - 需要复制到 `app/src/main/assets/` 目录
   - 从SDK解压包中的 `assets` 目录复制所有文件

### 获取Msc.jar的步骤

1. 访问讯飞开放平台: https://www.xfyun.cn/
2. 注册账号并登录
3. 创建应用，选择"语音听写"或"语音转写"服务
4. 下载Android SDK
5. 解压SDK，找到 `Msc.jar` 文件
6. 将 `Msc.jar` 复制到此目录: `app/libs/Msc.jar`

### 配置

在 `app/build.gradle.kts` 中添加依赖：

```kotlin
dependencies {
    implementation(files("libs/Msc.jar"))
}
```

### 注意事项

- 确保文件名为 `Msc.jar`（区分大小写）
- 文件大小通常为几MB到几十MB
- 如果下载的SDK中没有Msc.jar，可能是版本不同，请查看SDK文档


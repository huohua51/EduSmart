# 配置 .so 文件到正确位置

## 📁 需要创建的目录结构

请在 `app/src/main/` 下创建以下目录结构：

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

## 🔧 手动操作步骤

### 方法1: 在 Android Studio 中操作

1. **创建目录**：
   - 在 `app/src/main/` 右键 → **New** → **Directory**
   - 输入：`jniLibs`
   - 在 `jniLibs` 下创建子目录：`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`

2. **复制文件**：
   - 将项目根目录的 `libSparkChain.so` 和 `libspark.so`
   - 复制到每个架构目录中（arm64-v8a, armeabi-v7a, x86, x86_64）

### 方法2: 使用文件管理器

1. 打开文件管理器，导航到项目目录
2. 创建路径：`app\src\main\jniLibs\arm64-v8a\`
3. 将 `libSparkChain.so` 和 `libspark.so` 复制到该目录
4. 重复步骤2-3，为其他架构创建目录并复制文件：
   - `app\src\main\jniLibs\armeabi-v7a\`
   - `app\src\main\jniLibs\x86\`
   - `app\src\main\jniLibs\x86_64\`

## ✅ 验证

完成后，目录结构应该是：

```
app/src/main/
├── jniLibs/
│   ├── arm64-v8a/
│   │   ├── libSparkChain.so
│   │   └── libspark.so
│   ├── armeabi-v7a/
│   │   ├── libSparkChain.so
│   │   └── libspark.so
│   ├── x86/
│   │   ├── libSparkChain.so
│   │   └── libspark.so
│   └── x86_64/
│       ├── libSparkChain.so
│       └── libspark.so
├── java/
├── res/
└── ...
```

## 🚀 完成后

1. **同步 Gradle**：File → Sync Project with Gradle Files
2. **清理项目**：Build → Clean Project
3. **重新构建**：Build → Rebuild Project
4. **运行应用**：应该可以正常启动，SparkChain SDK 会初始化

## 📝 注意事项

- 如果只在真机上测试，至少需要 `arm64-v8a` 目录
- 如果使用模拟器，需要 `x86` 或 `x86_64` 目录
- 所有架构目录中的文件名必须一致

---

**我已经恢复了 SparkChain SDK 的初始化代码，现在只需要将 .so 文件放到正确的位置即可！**


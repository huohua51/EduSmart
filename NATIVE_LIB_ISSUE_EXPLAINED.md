# 原生库文件缺失问题说明

## 🔴 问题情况

根据您的检查结果，发现以下问题：

```
❌ app/src/main/jniLibs/arm64-v8a/libSparkChain.so - 不存在
❌ app/src/main/jniLibs/arm64-v8a/libspark.so - 不存在  
❌ app/src/main/jniLibs/armeabi-v7a/libSparkChain.so - 不存在
❌ app/src/main/jniLibs/armeabi-v7a/libspark.so - 不存在
```

**这些文件是 SparkChain SDK 运行所必需的！** 缺失会导致应用运行时崩溃，出现 `UnsatisfiedLinkError`。

## ✅ 解决方案

### 方法1: 使用批处理脚本（最简单）

我已经为您创建了一个批处理脚本 `copy_native_libs.bat`：

1. **双击运行** `copy_native_libs.bat`
2. 脚本会自动：
   - 创建 `app/src/main/jniLibs/` 目录
   - 从 SDK 目录复制所有原生库文件
   - 验证文件是否复制成功

### 方法2: 手动复制

如果脚本无法运行，可以手动复制：

1. **找到源文件位置**：
   ```
   C:\Users\18335\Desktop\SparkChain_Android_SDK_2.0.1_rc1\SparkChain_Android_SDK_2.0.1_rc1\app\libs\SparkChain_extracted\jni\
   ```

2. **创建目标目录**：
   ```
   app\src\main\jniLibs\
   ```

3. **复制文件夹**：
   - 复制 `arm64-v8a` 文件夹到 `app\src\main\jniLibs\`
   - 复制 `armeabi-v7a` 文件夹到 `app\src\main\jniLibs\`

4. **最终目录结构应该是**：
   ```
   app/src/main/jniLibs/
   ├── arm64-v8a/
   │   ├── libSparkChain.so
   │   └── libspark.so
   └── armeabi-v7a/
       ├── libSparkChain.so
       └── libspark.so
   ```

### 方法3: 从 AAR 文件提取

如果 SDK 目录中的文件不存在，可以从 AAR 文件提取：

1. **将 AAR 文件重命名为 ZIP**：
   - `app/libs/SparkChain.aar` → `SparkChain.zip`

2. **解压 ZIP 文件**

3. **找到 jni 目录**：
   - 解压后的 `jni/` 目录

4. **复制到项目**：
   - 将 `jni/` 目录内容复制到 `app/src/main/jniLibs/`

## 🔍 验证文件

复制完成后，验证文件是否存在：

**PowerShell:**
```powershell
Test-Path "app\src\main\jniLibs\arm64-v8a\libSparkChain.so"
Test-Path "app\src\main\jniLibs\arm64-v8a\libspark.so"
Test-Path "app\src\main\jniLibs\armeabi-v7a\libSparkChain.so"
Test-Path "app\src\main\jniLibs\armeabi-v7a\libspark.so"
```

**文件管理器:**
- 打开 `app\src\main\jniLibs\` 目录
- 确认看到 `arm64-v8a` 和 `armeabi-v7a` 两个文件夹
- 每个文件夹中应该有 2 个 `.so` 文件

## ⚠️ 重要提示

1. **文件大小检查**：
   - 每个 `.so` 文件通常有几 MB 到几十 MB
   - 如果文件大小为 0 或很小，说明复制失败

2. **同步项目**：
   - 复制文件后，在 Android Studio 中：
     - `File -> Sync Project with Gradle Files`
     - 或 `Build -> Rebuild Project`

3. **如果仍然失败**：
   - 检查文件权限
   - 确认源文件确实存在
   - 尝试手动复制而不是使用脚本

## 📝 下一步

1. ✅ 运行 `copy_native_libs.bat` 或手动复制文件
2. ✅ 验证文件存在
3. ✅ 在 Android Studio 中同步项目
4. ✅ 重新构建项目
5. ✅ 运行应用测试

---

**提示**: 如果您的项目是从其他电脑复制过来的，确保同时复制了：
- ✅ `app/libs/` 目录（AAR 文件）
- ✅ `app/src/main/jniLibs/` 目录（原生库文件）

这两个都是必需的！




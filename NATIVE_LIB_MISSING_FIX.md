# 原生库文件缺失问题解决方案

## 🔴 问题描述

检查发现 `app/src/main/jniLibs/` 目录下的原生库文件（`.so` 文件）缺失：
- ❌ `arm64-v8a/libSparkChain.so`
- ❌ `arm64-v8a/libspark.so`
- ❌ `armeabi-v7a/libSparkChain.so`
- ❌ `armeabi-v7a/libspark.so`

这些文件是 SparkChain SDK 运行所必需的，缺失会导致应用运行时崩溃。

## ✅ 解决方案

### 方法1: 从 AAR 文件中提取（推荐）

如果您的项目中有 `SparkChain.aar` 文件，可以从其中提取原生库。

#### 步骤1: 检查 AAR 文件

确认 `app/libs/SparkChain.aar` 文件存在。

#### 步骤2: 提取原生库

**Windows (PowerShell):**

```powershell
# 创建临时目录
$tempDir = "temp_aar_extract"
New-Item -ItemType Directory -Path $tempDir -Force | Out-Null

# 将 AAR 文件重命名为 ZIP（AAR 本质上是 ZIP 文件）
Copy-Item "app\libs\SparkChain.aar" "$tempDir\SparkChain.zip"

# 解压 ZIP 文件
Expand-Archive -Path "$tempDir\SparkChain.zip" -DestinationPath "$tempDir\extracted" -Force

# 检查是否有 jni 目录
if (Test-Path "$tempDir\extracted\jni") {
    Write-Host "找到 jni 目录"
    
    # 创建目标目录
    $jniLibsDir = "app\src\main\jniLibs"
    New-Item -ItemType Directory -Path $jniLibsDir -Force | Out-Null
    
    # 复制原生库文件
    Copy-Item "$tempDir\extracted\jni\*" -Destination $jniLibsDir -Recurse -Force
    
    Write-Host "原生库文件已复制到 $jniLibsDir"
    
    # 列出复制的文件
    Get-ChildItem $jniLibsDir -Recurse -File | Select-Object FullName
} else {
    Write-Host "AAR 文件中没有找到 jni 目录"
    Write-Host "请检查 AAR 文件结构："
    Get-ChildItem "$tempDir\extracted" -Recurse | Select-Object FullName
}

# 清理临时文件
Remove-Item $tempDir -Recurse -Force
```

**macOS/Linux:**

```bash
# 创建临时目录
mkdir -p temp_aar_extract
cd temp_aar_extract

# 将 AAR 文件重命名为 ZIP 并解压
cp ../app/libs/SparkChain.aar SparkChain.zip
unzip -q SparkChain.zip -d extracted

# 检查是否有 jni 目录
if [ -d "extracted/jni" ]; then
    echo "找到 jni 目录"
    
    # 创建目标目录
    mkdir -p ../app/src/main/jniLibs
    
    # 复制原生库文件
    cp -r extracted/jni/* ../app/src/main/jniLibs/
    
    echo "原生库文件已复制"
    ls -R ../app/src/main/jniLibs
else
    echo "AAR 文件中没有找到 jni 目录"
    ls -R extracted
fi

# 清理临时文件
cd ..
rm -rf temp_aar_extract
```

### 方法2: 从原始项目复制

如果您有原始项目的完整副本，可以直接复制原生库文件。

#### 步骤1: 找到原始项目中的文件

在原始项目中找到以下目录：
```
原始项目/app/src/main/jniLibs/
├── arm64-v8a/
│   ├── libSparkChain.so
│   └── libspark.so
└── armeabi-v7a/
    ├── libSparkChain.so
    └── libspark.so
```

#### 步骤2: 复制到新项目

**Windows:**

```powershell
# 创建目标目录
New-Item -ItemType Directory -Path "app\src\main\jniLibs" -Force | Out-Null

# 从原始项目复制（替换为实际路径）
$sourcePath = "原始项目路径\app\src\main\jniLibs"
Copy-Item "$sourcePath\*" -Destination "app\src\main\jniLibs" -Recurse -Force
```

**macOS/Linux:**

```bash
# 创建目标目录
mkdir -p app/src/main/jniLibs

# 从原始项目复制（替换为实际路径）
cp -r "原始项目路径/app/src/main/jniLibs/"* app/src/main/jniLibs/
```

### 方法3: 从 SDK 包中提取

如果您有 SparkChain SDK 的原始包，可以从其中提取。

#### 步骤1: 找到 SDK 包

在 SDK 包中查找 `jni` 或 `libs` 目录，通常位于：
- `SparkChain_Android_SDK_2.0.1_rc1/lib/`
- `SparkChain_Android_SDK_2.0.1_rc1/jni/`
- 或类似路径

#### 步骤2: 复制原生库

```powershell
# Windows
$sdkPath = "SparkChain_Android_SDK_2.0.1_rc1\lib"  # 替换为实际路径
New-Item -ItemType Directory -Path "app\src\main\jniLibs" -Force | Out-Null
Copy-Item "$sdkPath\*" -Destination "app\src\main\jniLibs" -Recurse -Force
```

## 🔍 验证文件是否存在

提取或复制后，验证文件是否正确：

**Windows (PowerShell):**

```powershell
# 检查文件是否存在
Test-Path "app\src\main\jniLibs\arm64-v8a\libSparkChain.so"
Test-Path "app\src\main\jniLibs\arm64-v8a\libspark.so"
Test-Path "app\src\main\jniLibs\armeabi-v7a\libSparkChain.so"
Test-Path "app\src\main\jniLibs\armeabi-v7a\libspark.so"

# 列出所有文件
Get-ChildItem "app\src\main\jniLibs" -Recurse -File | Select-Object FullName
```

**macOS/Linux:**

```bash
# 检查文件是否存在
ls app/src/main/jniLibs/arm64-v8a/libSparkChain.so
ls app/src/main/jniLibs/arm64-v8a/libspark.so
ls app/src/main/jniLibs/armeabi-v7a/libSparkChain.so
ls app/src/main/jniLibs/armeabi-v7a/libspark.so

# 列出所有文件
find app/src/main/jniLibs -type f
```

## ⚠️ 重要提示

1. **文件结构必须正确**
   ```
   app/src/main/jniLibs/
   ├── arm64-v8a/
   │   ├── libSparkChain.so
   │   └── libspark.so
   └── armeabi-v7a/
       ├── libSparkChain.so
       └── libspark.so
   ```

2. **文件大小检查**
   - 每个 `.so` 文件通常有几 MB 到几十 MB
   - 如果文件大小为 0 或很小，说明提取失败

3. **同步项目**
   - 添加文件后，在 Android Studio 中：
     - `File -> Sync Project with Gradle Files`
     - 或 `Build -> Rebuild Project`

## 🐛 如果仍然失败

### 问题1: AAR 文件中没有 jni 目录

**可能原因**: AAR 文件可能使用了不同的打包方式。

**解决方法**:
1. 检查 AAR 文件内容：
   ```powershell
   # Windows
   Expand-Archive -Path "app\libs\SparkChain.aar" -DestinationPath "temp_extract" -Force
   Get-ChildItem "temp_extract" -Recurse | Select-Object FullName
   ```

2. 查找 `.so` 文件：
   ```powershell
   Get-ChildItem "temp_extract" -Recurse -Filter "*.so" | Select-Object FullName
   ```

3. 如果找到 `.so` 文件，手动创建目录结构并复制。

### 问题2: 找不到原始项目或 SDK

**解决方法**:
1. 联系项目维护者获取原生库文件
2. 或从 SparkChain SDK 官方文档中查找提取方法
3. 或检查是否有其他项目包含这些文件

## 📝 下一步

1. ✅ 提取或复制原生库文件
2. ✅ 验证文件存在且大小正常
3. ✅ 同步 Gradle 项目
4. ✅ 重新构建项目
5. ✅ 运行应用测试

---

**提示**: 如果您的项目是从其他电脑复制过来的，确保同时复制了 `app/libs/` 目录（AAR 文件）和 `app/src/main/jniLibs/` 目录（原生库文件）。




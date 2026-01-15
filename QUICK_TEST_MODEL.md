# 快速获取测试3D模型

## 🎯 最简单的测试方法

### 方法1: 使用Sketchfab（推荐）

1. **访问网站**: https://sketchfab.com/

2. **搜索简单模型**:
   - 搜索 "cube" - 立方体
   - 搜索 "sphere" - 球体
   - 搜索 "simple geometry" - 简单几何图形

3. **筛选条件**:
   - ✅ 选择 "Free"（免费）
   - ✅ 格式选择 "glTF" 或 "glb"
   - ✅ 选择简单的模型（面数少）

4. **下载步骤**:
   - 点击模型进入详情页
   - 点击 "Download" 按钮
   - 选择 "glTF" 格式
   - 下载ZIP文件
   - 解压后找到 `.gltf` 或 `.glb` 文件

5. **放置文件**:
   - 将 `.gltf` 或 `.glb` 文件复制到 `app/src/main/assets/models/`
   - 重命名为 `geometry.gltf` 或 `geometry.glb`

### 方法2: 使用在线测试模型URL

如果您有可访问的模型URL，可以在代码中使用：

```kotlin
// 在 ARActivity.kt 的 loadModelFromNetwork() 方法中添加
val testModelUrls = listOf(
    "https://your-model-url.com/model.gltf"
)
```

### 方法3: 创建简单的测试模型（Blender）

如果您有Blender：

1. 打开Blender
2. 创建默认立方体
3. 文件 -> 导出 -> glTF 2.0
4. 保存为 `geometry.gltf`
5. 放到 `app/src/main/assets/models/` 目录

## 📦 推荐的测试模型

### 简单几何图形（适合测试）
- **立方体** (Cube) - 最简单的模型
- **球体** (Sphere) - 圆形模型
- **圆锥** (Cone) - 几何图形

### 教育类模型（适合演示）
- **DNA双螺旋** - 搜索 "DNA"
- **水分子** - 搜索 "water molecule"
- **原子结构** - 搜索 "atom"

## 🔗 直接下载链接（如果可用）

注意：以下链接可能随时失效，建议从Sketchfab搜索下载。

### Sketchfab搜索链接
- 立方体: https://sketchfab.com/search?q=cube&type=models&features=downloadable&sort_by=-likeCount
- 球体: https://sketchfab.com/search?q=sphere&type=models&features=downloadable&sort_by=-likeCount
- 几何图形: https://sketchfab.com/search?q=geometry&type=models&features=downloadable&sort_by=-likeCount

## ⚡ 快速测试步骤

1. **下载模型**（5分钟）
   - 访问 Sketchfab
   - 搜索 "cube"
   - 下载GLTF格式

2. **放置文件**（1分钟）
   - 复制到 `app/src/main/assets/models/`
   - 重命名为 `geometry.gltf`

3. **运行应用**（2分钟）
   - 同步Gradle
   - 在真机上运行
   - 测试AR功能

**总计：约8分钟即可完成测试！**

---

**提示**: 如果暂时无法下载模型，可以先测试AR场景是否能正常启动。模型可以后续添加。


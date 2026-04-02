# 3D模型目录

## 📁 目录说明

此目录用于存放AR功能所需的3D模型文件。

## 📦 支持的格式

- **GLTF** (.gltf) - 推荐格式
- **GLB** (.glb) - 二进制GLTF格式，更小更快

## 🎨 如何获取3D模型

### 方法1: 从Sketchfab下载（推荐）

1. 访问 https://sketchfab.com/
2. 搜索关键词：
   - "education" - 教育类模型
   - "geometry" - 几何图形
   - "chemistry" - 化学分子
   - "physics" - 物理实验
   - "biology" - 生物模型
3. 筛选条件：
   - 选择 "Free"（免费）
   - 格式选择 "glTF" 或 "glb"
4. 下载模型文件
5. 将文件放到此目录

### 方法2: 从Poly by Google下载

1. 访问 https://poly.google.com/
2. 搜索并下载模型
3. 将文件放到此目录

### 方法3: 自己制作

使用以下工具制作3D模型：
- **Blender** (免费) - https://www.blender.org/
- **Maya** (商业软件)
- **3ds Max** (商业软件)

导出为GLTF格式。

## 📝 使用示例

### 示例1: 几何图形

文件名: `geometry.gltf`
- 立方体、球体、圆锥等基础几何图形
- 适合数学教学

### 示例2: 化学分子

文件名: `molecule.gltf`
- 水分子、DNA双螺旋等
- 适合化学教学

### 示例3: 物理实验

文件名: `physics.gltf`
- 杠杆、滑轮、电路等
- 适合物理教学

## 🔧 在代码中使用

在 `ARActivity.kt` 中修改模型路径：

```kotlin
val modelPath = "models/geometry.gltf" // 修改为您的模型文件名
```

## ⚠️ 注意事项

1. **文件大小**: 建议单个模型 < 10MB
2. **模型复杂度**: 过于复杂的模型可能影响性能
3. **纹理**: 确保纹理文件（如果有）也在assets目录中
4. **命名**: 使用英文文件名，避免特殊字符

## 🚀 快速测试

如果没有模型文件，可以先使用在线模型进行测试：

在 `ARActivity.kt` 中使用网络加载：

```kotlin
// 从网络加载模型（需要网络连接）
val modelUrl = "https://raw.githubusercontent.com/.../model.gltf"
loadModel(modelUrl)
```

---

**提示**: 可以先下载一个简单的测试模型（如立方体）来验证AR功能是否正常工作。


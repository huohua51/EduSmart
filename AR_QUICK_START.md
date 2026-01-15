# AR知识空间 - 快速开始

## ✅ 已完成的配置

1. **添加了 SceneView 库依赖** ✅
   - 在 `app/build.gradle.kts` 中已添加
   - `implementation("io.github.sceneview:arsceneview:2.6.0")`

2. **更新了 ARScreen.kt** ✅
   - 添加了AR支持检查
   - 添加了启动AR Activity的按钮

3. **ARActivity.kt 已存在** ✅
   - 需要完善实现

## 🚀 下一步：完善 ARActivity

### 方案1: 使用 SceneView（推荐）

更新 `ARActivity.kt`：

```kotlin
package com.edusmart.app.feature.ar

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.loaders.ModelLoader

class ARActivity : AppCompatActivity() {
    private lateinit var arSceneView: ArSceneView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 创建AR场景视图
        arSceneView = ArSceneView(this)
        setContentView(arSceneView)
        
        // 加载3D模型（需要先有模型文件）
        loadModel()
    }
    
    private fun loadModel() {
        // 方法1: 从assets加载
        // val modelLoader = ModelLoader(this)
        // val model = modelLoader.loadModel("models/geometry.gltf")
        // arSceneView.scene.addChild(model)
        
        // 方法2: 从网络加载
        // val modelUrl = "https://example.com/models/geometry.gltf"
        // arSceneView.loadModel(modelUrl)
        
        // 暂时显示提示
        // TODO: 添加3D模型文件
    }
    
    override fun onResume() {
        super.onResume()
        arSceneView.resume()
    }
    
    override fun onPause() {
        super.onPause()
        arSceneView.pause()
    }
}
```

### 方案2: 使用原生ARCore（更复杂但更灵活）

如果需要更多控制，可以使用原生ARCore API。

## 📦 添加3D模型

### 步骤1: 创建assets目录
```
app/src/main/assets/models/
```

### 步骤2: 下载3D模型
推荐网站：
- **Sketchfab**: https://sketchfab.com/
  - 搜索 "education"、"geometry"、"chemistry"
  - 筛选免费模型
  - 下载GLTF或GLB格式

- **Poly by Google**: https://poly.google.com/
  - Google的3D模型库

### 步骤3: 放置模型文件
将下载的模型文件（.gltf 或 .glb）放到：
```
app/src/main/assets/models/geometry.gltf
```

### 步骤4: 在代码中加载
```kotlin
val modelLoader = ModelLoader(this)
val model = modelLoader.loadModel("models/geometry.gltf")
arSceneView.scene.addChild(model)
```

## 🎨 推荐的教育类3D模型

### 几何图形
- 立方体、球体、圆锥、圆柱
- 搜索关键词: "geometry", "cube", "sphere"

### 化学分子
- 水分子、DNA双螺旋、蛋白质结构
- 搜索关键词: "molecule", "chemistry", "DNA"

### 物理实验
- 杠杆、滑轮、电路、磁铁
- 搜索关键词: "physics", "experiment", "circuit"

### 生物模型
- 细胞结构、器官模型、骨骼
- 搜索关键词: "biology", "cell", "anatomy"

## 🔧 手势交互

SceneView 库已经内置了手势交互：
- **旋转**: 单指拖动
- **缩放**: 双指捏合
- **移动**: 长按拖动

无需额外代码！

## ⚠️ 注意事项

1. **真机测试**: ARCore需要真机，模拟器不支持
2. **设备兼容性**: 检查设备是否支持ARCore
3. **模型大小**: 建议单个模型 < 10MB
4. **性能优化**: 复杂模型可能影响性能

## 📚 参考资源

1. **SceneView文档**: https://github.com/SceneView/sceneview-android
2. **ARCore文档**: https://developers.google.com/ar/develop
3. **GLTF格式**: https://www.khronos.org/gltf/

---

**现在您可以：**
1. 同步Gradle依赖
2. 下载3D模型并放到assets目录
3. 完善ARActivity.kt中的模型加载代码
4. 在真机上测试


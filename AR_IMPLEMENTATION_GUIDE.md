# AR知识空间实现指南

## 📚 可用的AR库和框架

### 1. **ARCore（推荐）** ✅ 已添加依赖
- **官方库**: Google ARCore
- **依赖**: `com.google.ar:core:1.40.0` (已添加)
- **优点**:
  - 官方支持，文档完善
  - 免费使用
  - 支持平面检测、图像识别、3D模型渲染
  - 支持手势交互（旋转、缩放、移动）
- **缺点**:
  - 需要真机测试（模拟器不支持）
  - 需要设备支持ARCore

### 2. **Sceneform（已废弃，但有替代方案）**
- **状态**: Google已停止维护
- **替代方案**: 
  - **Sceneform 1.16+** (社区维护版本)
  - **Filament** (Google推荐的新方案)
  - **自定义OpenGL渲染**

### 3. **其他AR库**
- **Vuforia**: 商业AR SDK（需要授权）
- **EasyAR**: 商业AR SDK
- **8th Wall**: WebAR解决方案

## 🎯 推荐方案：ARCore + Filament

### 为什么选择这个方案？
1. **ARCore** - 处理AR跟踪和平面检测
2. **Filament** - Google推荐的3D渲染引擎（替代Sceneform）
3. **完全免费** - 无需授权费用
4. **功能强大** - 支持复杂3D模型、动画、材质

## 📦 需要添加的依赖

### 1. ARCore（已添加）
```kotlin
implementation("com.google.ar:core:1.40.0")
```

### 2. Filament（3D渲染引擎）
```kotlin
// Filament Android库
implementation("com.google.android.filament:filament-android:1.45.0")
implementation("com.google.android.filament:gltfio-android:1.45.0")
```

### 3. ARCore Sceneform（社区维护版本，可选）
```kotlin
// 如果不想用Filament，可以用这个简化版本
implementation("io.github.sceneview:arsceneview:2.6.0")
```

## 🛠️ 实现步骤

### 方案1: 使用 ARCore + Filament（推荐）

#### 步骤1: 添加依赖
在 `app/build.gradle.kts` 中添加：

```kotlin
dependencies {
    // ARCore（已添加）
    implementation("com.google.ar:core:1.40.0")
    
    // Filament - 3D渲染引擎
    implementation("com.google.android.filament:filament-android:1.45.0")
    implementation("com.google.android.filament:gltfio-android:1.45.0")
    
    // ARCore工具类
    implementation("com.google.ar.sceneform:core:1.17.1")
    implementation("com.google.ar.sceneform:animation:1.17.1")
}
```

#### 步骤2: 创建AR视图组件
创建 `ARView.kt` 用于显示AR场景。

#### 步骤3: 加载3D模型
- 支持格式：GLTF、GLB、OBJ
- 模型来源：
  - 自己制作（Blender、Maya等）
  - 下载免费模型（Sketchfab、Poly等）
  - 使用在线模型库

### 方案2: 使用 ARCore + SceneView（简化版）

#### 添加依赖
```kotlin
implementation("io.github.sceneview:arsceneview:2.6.0")
```

这个库封装了ARCore和3D渲染，使用更简单。

### 方案3: 使用 ARCore + OpenGL（完全自定义）

适合需要完全控制渲染流程的场景。

## 📝 实现示例代码

### 基础AR视图实现

```kotlin
// ARView.kt
class ARView(context: Context) : View(context) {
    private var arSession: Session? = null
    private var arCamera: Camera? = null
    
    fun initialize(session: Session) {
        arSession = session
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 绘制AR内容
    }
}
```

### 3D模型加载示例

```kotlin
// 加载GLTF模型
val modelLoader = ModelLoader(context)
val model = modelLoader.loadModel("models/geometry.gltf")
arSceneView.scene.addChild(model)
```

## 🎨 3D模型资源

### 免费模型网站
1. **Sketchfab** - https://sketchfab.com/
   - 大量免费教育类3D模型
   - 支持GLTF格式下载

2. **Poly by Google** - https://poly.google.com/
   - Google的3D模型库（已停止更新，但可下载）

3. **TurboSquid** - https://www.turbosquid.com/
   - 部分免费模型

4. **Free3D** - https://free3d.com/
   - 免费3D模型下载

### 教育类模型推荐
- **几何图形**: 立方体、球体、圆锥等
- **化学分子**: 水分子、DNA双螺旋等
- **物理实验**: 杠杆、滑轮、电路等
- **生物模型**: 细胞结构、器官模型等

## 🔧 手势交互实现

### 旋转
```kotlin
// 使用手势检测器
val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
        // 旋转模型
        model.rotationY += distanceX * 0.01f
        return true
    }
})
```

### 缩放
```kotlin
// 使用ScaleGestureDetector
val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
    override fun onScale(detector: ScaleGestureDetector): Boolean {
        val scaleFactor = detector.scaleFactor
        model.scale *= scaleFactor
        return true
    }
})
```

## 📱 权限配置

在 `AndroidManifest.xml` 中添加：

```xml
<!-- ARCore需要 -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera.ar" android:required="false" />

<!-- ARCore元数据 -->
<meta-data android:name="com.google.ar.core" android:value="required" />
```

## 🚀 快速开始

### 最简单的实现方式

1. **使用 SceneView 库**（推荐新手）
   ```kotlin
   implementation("io.github.sceneview:arsceneview:2.6.0")
   ```

2. **创建AR Activity**
   ```kotlin
   class ARActivity : AppCompatActivity() {
       private lateinit var arSceneView: ArSceneView
       
       override fun onCreate(savedInstanceState: Bundle?) {
           super.onCreate(savedInstanceState)
           arSceneView = ArSceneView(this)
           setContentView(arSceneView)
           
           // 加载3D模型
           arSceneView.loadModel("models/geometry.gltf")
       }
   }
   ```

## 📚 学习资源

1. **ARCore官方文档**
   - https://developers.google.com/ar/develop

2. **Filament文档**
   - https://google.github.io/filament/

3. **SceneView库文档**
   - https://github.com/SceneView/sceneview-android

## ⚠️ 注意事项

1. **真机测试**: ARCore需要真机，模拟器不支持
2. **设备兼容性**: 检查设备是否支持ARCore
3. **性能优化**: 3D模型要优化，避免卡顿
4. **模型大小**: 建议单个模型 < 10MB

## 🎯 推荐实现路径

1. **第一阶段**: 使用 SceneView 库快速实现基础AR
2. **第二阶段**: 添加3D模型加载功能
3. **第三阶段**: 实现手势交互（旋转、缩放）
4. **第四阶段**: 优化性能和用户体验

---

**建议**: 先从 SceneView 库开始，它封装了ARCore和3D渲染，使用最简单！


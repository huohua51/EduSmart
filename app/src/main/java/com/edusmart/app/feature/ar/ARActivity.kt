package com.edusmart.app.feature.ar

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
// 暂时注释掉 SceneView 导入，因为依赖无法下载
// import io.github.sceneview.ar.ArSceneView
// import io.github.sceneview.ar.node.ArModelNode
// import io.github.sceneview.loaders.ModelLoader

/**
 * AR知识空间Activity
 * 
 * 使用 SceneView 库实现AR功能
 * - 支持平面检测
 * - 支持3D模型加载（GLTF/GLB格式）
 * - 支持手势交互（旋转、缩放、移动）
 * 
 * 使用前需要：
 * 1. 在 assets/models/ 目录放置3D模型文件（.gltf 或 .glb）
 * 2. 在 loadModel() 方法中指定模型文件名
 */
class ARActivity : AppCompatActivity() {
    // 暂时注释掉 SceneView 相关变量，因为依赖无法下载
    // private var arSceneView: ArSceneView? = null
    private var arSession: Session? = null
    // private var modelNode: ArModelNode? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 暂时显示提示，因为 SceneView 依赖无法下载
        Toast.makeText(
            this,
            "AR功能暂时不可用\nSceneView依赖无法下载，请检查网络或稍后重试",
            Toast.LENGTH_LONG
        ).show()
        finish()
        
        /* 原始代码暂时注释
        // 检查ARCore支持
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        if (availability.isTransient) {
            // 需要重新检查
            Toast.makeText(this, "正在检查AR支持...", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (availability.isSupported) {
            // ARCore支持，初始化AR场景
            initializeAR()
        } else {
            // ARCore不支持，显示提示并退出
            Toast.makeText(
                this,
                "您的设备不支持ARCore，无法使用AR功能",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
        */
    }
    
    /* 暂时注释掉，等待 SceneView 依赖可用
    private fun initializeAR() {
        try {
            // 创建AR场景视图
            arSceneView = ArSceneView(this).apply {
                // 启用平面检测
                planeRenderer.isVisible = true
                planeRenderer.isShadowReceiver = true
                
                // 设置场景
                scene.skybox.isVisible = false
                
                // 加载3D模型
                loadModel()
            }
            
            setContentView(arSceneView)
            
            Toast.makeText(this, "请将手机对准平面，等待模型出现", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            android.util.Log.e("ARActivity", "AR初始化失败", e)
            Toast.makeText(this, "AR初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    */
    
    /* 暂时注释掉，等待 SceneView 依赖可用
    /**
     * 加载3D模型
     * 
     * 支持两种方式：
     * 1. 从assets加载（推荐）- 需要先在 assets/models/ 目录放置模型文件
     * 2. 从网络加载（需要网络连接）- 适合测试
     * 
     * 支持的格式：GLTF (.gltf) 或 GLB (.glb)
     */
    private fun ArSceneView.loadModel() {
        try {
            // ========== 方法1: 从assets加载（推荐） ==========
            val modelLoader = ModelLoader(this@ARActivity)
            
            // 尝试加载的模型文件列表（按优先级）
            val modelPaths = listOf(
                "models/geometry.gltf",
                "models/geometry.glb",
                "models/molecule.gltf",
                "models/physics.gltf"
            )
            
            var modelLoaded = false
            for (modelPath in modelPaths) {
                try {
                    // 检查模型文件是否存在
                    val assets = this@ARActivity.assets
                    assets.open(modelPath).close()
                    
                    // 加载模型
                    val model = modelLoader.loadModel(modelPath)
                    
                    // 创建AR模型节点
                    modelNode = ArModelNode().apply {
                        // 设置模型
                        modelInstance = model
                        
                        // 设置初始位置（在平面上方0.5米）
                        // position = Vector3(0f, 0.5f, -1f)
                        
                        // 设置初始缩放（根据模型大小调整）
                        // scale = Vector3(0.1f, 0.1f, 0.1f)
                    }
                    
                    // 添加到场景
                    scene.addChild(modelNode)
                    
                    android.util.Log.d("ARActivity", "模型加载成功: $modelPath")
                    Toast.makeText(
                        this@ARActivity,
                        "模型加载成功: ${modelPath.substringAfterLast("/")}",
                        Toast.LENGTH_SHORT
                    ).show()
                    modelLoaded = true
                    break
                } catch (e: Exception) {
                    // 文件不存在，继续尝试下一个
                    android.util.Log.d("ARActivity", "模型文件不存在: $modelPath")
                }
            }
            
            // 如果没有找到本地模型，尝试从网络加载（测试用）
            if (!modelLoaded) {
                android.util.Log.w("ARActivity", "未找到本地模型，尝试从网络加载测试模型")
                loadModelFromNetwork()
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ARActivity", "加载模型失败", e)
            Toast.makeText(this@ARActivity, "加载模型失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * 从网络加载3D模型（用于测试）
     * 
     * 注意：需要网络连接，且模型URL必须可访问
     */
    private fun ArSceneView.loadModelFromNetwork() {
        // 示例：使用公开的测试模型URL
        // 实际使用时，请替换为您自己的模型URL
        val testModelUrls = listOf(
            // 可以添加一些公开的测试模型URL
            // "https://raw.githubusercontent.com/.../model.gltf"
        )
        
        if (testModelUrls.isEmpty()) {
            Toast.makeText(
                this@ARActivity,
                "未找到3D模型文件\n请下载模型到 assets/models/ 目录\n或配置网络模型URL",
                Toast.LENGTH_LONG
            ).show()
            android.util.Log.w("ARActivity", "未配置模型，AR场景将只显示平面检测")
            return
        }
        
        // TODO: 实现网络模型加载
        // 注意：SceneView库可能需要额外配置才能支持网络加载
        android.util.Log.d("ARActivity", "网络模型加载功能待实现")
    }
    */
    
    override fun onResume() {
        super.onResume()
        // arSceneView?.resume()
    }
    
    override fun onPause() {
        super.onPause()
        // arSceneView?.pause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // modelNode = null
        // arSceneView?.destroy()
        // arSceneView = null
        arSession?.close()
        arSession = null
    }
}


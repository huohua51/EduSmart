package com.edusmart.app.feature.ar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.activity.viewModels
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Plane
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.launch
import com.edusmart.app.R
import com.edusmart.app.feature.ar.model.ModelItem
import timber.log.Timber

class ARActivity : AppCompatActivity() {

    private lateinit var arSceneView: ARSceneView
    private val viewModel: ARViewModel by viewModels()

    private val placedAnchors = mutableListOf<AnchorNode>()

    // 相机权限请求
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkAndSetupAR()
        } else {
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("ARActivity onCreate开始")
        setContentView(R.layout.activity_ar)
        Timber.d("布局文件已设置")

        findViewById<android.widget.Button>(R.id.btnBack).setOnClickListener {
            Timber.d("返回按钮点击")
            finish()
        }

        findViewById<android.widget.Button>(R.id.btnSelectModel).setOnClickListener {
            Timber.d("选择模型按钮点击")
            showModelSelectionDialog()
        }

        findViewById<android.widget.Button>(R.id.btnClearModels)?.setOnClickListener {
            Timber.d("清除模型按钮点击")
            clearAllModels()
        }

        Timber.d("开始检查相机权限")
        // 先检查权限，再初始化AR
        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        Timber.d("checkCameraPermission: 检查权限状态")
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                Timber.d("相机权限已授予，开始检查AR支持")
                // 已有权限，直接初始化AR
                if (!checkAndSetupAR()) {
                    Timber.w("AR检查不通过，Activity可能退出")
                    // AR检查不通过，不继续执行
                }
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Timber.d("需要显示权限解释对话框")
                showPermissionRationaleDialog()
            }
            else -> {
                Timber.d("请求相机权限")
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun checkAndSetupAR(): Boolean {
        Timber.d("checkAndSetupAR: 开始检查AR支持")
        return try {
            val availability = ArCoreApk.getInstance().checkAvailability(this)
            Timber.d("ARCore可用性检查结果: $availability")

            when {
                availability.isSupported -> {
                    Timber.i("设备支持AR，初始化AR场景")
                    arSceneView = findViewById(R.id.arSceneView)
                    setupARScene()
                    true
                }
                availability.isTransient -> {
                    Timber.w("AR支持检查中，稍后重试")
                    Toast.makeText(this, "正在检查AR可用性...", Toast.LENGTH_SHORT).show()
                    // 延迟重试
                    android.os.Handler(mainLooper).postDelayed({
                        checkAndSetupAR()
                    }, 2000)
                    false
                }
                else -> {
                    Timber.e("设备不支持AR功能")
                    showNotSupportedDialog()
                    false
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "AR支持检查异常: ${e.message}")
            showNotSupportedDialog()
            false
        }
    }

    private fun setupARScene() {
        try {
            Timber.d("开始设置AR场景")
            arSceneView = findViewById(R.id.arSceneView)
            Timber.d("ARSceneView找到: ${arSceneView != null}")
            
            arSceneView.apply {
                onSessionCreated = { session ->
                    Timber.i("AR会话创建成功")
                    runOnUiThread {
                        Toast.makeText(
                            this@ARActivity,
                            "AR会话已创建，请移动设备扫描平面",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                onSessionFailed = { exception ->
                    Timber.e("AR会话创建失败: ${exception.message}", exception)
                    runOnUiThread {
                        Toast.makeText(
                            this@ARActivity,
                            "AR会话启动失败: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                // 注意：SceneView 的 onTouchEvent 第二个参数是碰撞系统的 HitResult（非 ARCore HitResult）。
                // 放置模型需要 ARCore HitResult，这里用 ARSceneView.hitTestAR() 主动做一次 ARCore hitTest。
                onTouchEvent = { motionEvent, _ ->
                    try {
                        if (motionEvent.action != MotionEvent.ACTION_UP) {
                            false
                        } else {
                            val selected = viewModel.selectedModel.value
                            if (selected == null) {
                                Toast.makeText(this@ARActivity, "请先选择模型", Toast.LENGTH_SHORT).show()
                                true
                            } else {
                                val hit = hitTestAR(
                                    xPx = motionEvent.x,
                                    yPx = motionEvent.y,
                                    planeTypes = setOf(
                                        Plane.Type.HORIZONTAL_UPWARD_FACING,
                                        Plane.Type.VERTICAL
                                    ),
                                    planePoseInPolygon = true,
                                )

                                if (hit != null) {
                                    placeModelAtHit(hit, selected)
                                    true
                                } else {
                                    Toast.makeText(this@ARActivity, "未检测到平面，请移动设备重试", Toast.LENGTH_SHORT).show()
                                    false
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "触摸事件处理异常")
                        false
                    }
                }
            }
            Timber.i("AR场景设置完成")
        } catch (e: Exception) {
            Timber.e(e, "AR场景设置失败: ${e.message}")
            Toast.makeText(this, "AR初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun placeModelAtHit(
        hit: com.google.ar.core.HitResult,
        model: ModelItem
    ) {
        lifecycleScope.launch {
            try {
                val anchor = hit.createAnchor()
                val anchorNode = AnchorNode(engine = arSceneView.engine, anchor = anchor)

                // assets/model 下的文件：ModelItem.modelPath 形如 "model/cube.glb"
                val modelInstance = arSceneView.modelLoader.loadModelInstance(model.modelPath)
                if (modelInstance == null) {
                    Toast.makeText(this@ARActivity, "模型加载失败: ${model.modelPath}", Toast.LENGTH_SHORT).show()
                    anchorNode.destroy()
                    return@launch
                }

                val modelNode = ModelNode(modelInstance = modelInstance).apply {
                    // 统一按 XYZ 三个方向等比缩放，避免因为某一轴为 0 导致模型发黑或畸形
                    scale = Position(model.scale, model.scale, model.scale)
                }

                anchorNode.addChildNode(modelNode)
                arSceneView.addChildNode(anchorNode)

                placedAnchors.add(anchorNode)
                viewModel.modelPlaced()
                Toast.makeText(this@ARActivity, "模型放置成功: ${model.name}", Toast.LENGTH_SHORT).show()
            } catch (e: Throwable) {
                Toast.makeText(this@ARActivity, "放置失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearAllModels() {
        placedAnchors.forEach { node ->
            try {
                arSceneView.removeChildNode(node)
            } catch (_: Throwable) {
            }
            try {
                node.destroy()
            } catch (_: Throwable) {
            }
        }
        placedAnchors.clear()
        viewModel.clearSelection()
    }

    private fun showModelSelectionDialog() {
        val models = ARConfig.getDefaultModels()
        val modelNames = models.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("选择模型")
            .setItems(modelNames) { _, which ->
                val selectedModel = models[which]
                viewModel.selectModel(selectedModel)
                Toast.makeText(this, "已选择: ${selectedModel.name}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showNotSupportedDialog() {
        AlertDialog.Builder(this)
            .setTitle("设备不支持")
            .setMessage("您的设备不支持AR功能")
            .setPositiveButton("确定") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("需要相机权限")
            .setMessage("AR功能需要相机权限，请前往系统设置授予权限")
            .setPositiveButton("确定") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("需要相机权限")
            .setMessage("AR功能需要使用相机来扫描现实世界")
            .setPositiveButton("授予权限") { _, _ ->
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton("取消") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun onResume() {
        super.onResume()
        Timber.d("ARActivity onResume")
        // 这里可以添加AR场景恢复逻辑
    }

    override fun onPause() {
        super.onPause()
        Timber.d("ARActivity onPause")
        // 这里可以暂停AR场景
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("ARActivity onDestroy")
        // 清理AR资源
        try {
            placedAnchors.forEach { node ->
                try {
                    arSceneView.removeChildNode(node)
                } catch (e: Exception) {
                    Timber.w("清理节点时出错: ${e.message}")
                }
                try {
                    node.destroy()
                } catch (e: Exception) {
                    Timber.w("销毁节点时出错: ${e.message}")
                }
            }
            placedAnchors.clear()
        } catch (e: Exception) {
            Timber.e("清理AR资源时出错: ${e.message}")
        }
    }
}

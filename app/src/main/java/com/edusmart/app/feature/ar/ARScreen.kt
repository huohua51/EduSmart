package com.edusmart.app.feature.ar

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * AR知识空间界面
 * 
 * 使用 SceneView 库实现AR功能
 * - 支持平面检测
 * - 支持3D模型加载（GLTF/GLB格式）
 * - 支持手势交互（旋转、缩放、移动）
 * 
 * 注意：
 * 1. 需要真机测试（模拟器不支持ARCore）
 * 2. 需要设备支持ARCore
 * 3. 3D模型需要放到 assets/models/ 目录
 */
@Composable
fun ARScreen() {
    val context = LocalContext.current
    var isARSupported by remember { mutableStateOf<Boolean?>(null) }
    
    // 检查ARCore支持
    LaunchedEffect(Unit) {
        isARSupported = checkARCoreSupport(context)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "AR知识空间",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Text(
            text = "拍摄课本插图，查看3D动态模型\n手势操作：旋转、缩放、拆解",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        // AR支持检查
        when (isARSupported) {
            true -> {
                Button(
                    onClick = { 
                        // 启动AR Activity
                        val intent = Intent(context, ARActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("启动AR相机")
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "支持的功能",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("• 几何图形3D展示与拆解")
                        Text("• 化学分子结构动态组装")
                        Text("• 物理实验过程AR模拟")
                        Text("• 历史地理场景重现")
                    }
                }
            }
            false -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "设备不支持AR",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "您的设备不支持ARCore，无法使用AR功能。\n请使用支持ARCore的设备。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            null -> {
                CircularProgressIndicator()
                Text("检查AR支持...")
            }
        }
    }
}

/**
 * 检查设备是否支持ARCore
 */
private fun checkARCoreSupport(context: Context): Boolean {
    return try {
        val availability = com.google.ar.core.ArCoreApk.getInstance().checkAvailability(context)
        availability.isSupported
    } catch (e: Exception) {
        false
    }
}


package com.edusmart.app.feature.ar

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edusmart.app.R
import com.edusmart.app.ui.theme.*
import com.google.ar.core.ArCoreApk

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
 * 3. 3D模型需要放到 assets/model/ 目录
 */
@Composable
fun ARScreen() {
    val context = LocalContext.current
    val arSupported = remember { checkARSupport(context) }

    // 极简白底设计
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // 插画图片
            Image(
                painter = painterResource(id = R.drawable.ar_illustration),
                contentDescription = "AR插画",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 标题
            Text(
                text = "AR知识空间",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (arSupported) {
                    "设备支持AR功能，可以体验3D模型"
                } else {
                    "设备可能不支持AR，部分功能受限"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    // 启动AR Activity
                    context.startActivity(Intent(context, ARActivity::class.java))
                },
                enabled = arSupported,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (arSupported) "启动AR相机" else "设备不支持AR",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    // TODO: 打开模型库或展示3D模型查看器
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = "浏览3D模型库",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (!arSupported) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CardBackground
                ) {
                    Text(
                        text = "提示：如需使用AR功能，请确保设备支持ARCore",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * 检查设备是否支持ARCore
 */
private fun checkARSupport(context: Context): Boolean {
    return try {
        val availability = ArCoreApk.getInstance().checkAvailability(context)
        // 直接使用 availability 的 isSupported 属性
        availability.isSupported
    } catch (e: Exception) {
        false
    }
}


package com.edusmart.app.feature.scan

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.edusmart.app.ui.components.BeautyButton
import com.edusmart.app.ui.components.CameraPreview
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun ScanScreen(
    navController: NavController,
    onScanningStateChange: (Boolean) -> Unit = {},
    autoStartScanning: Boolean = false
) {
    val context = LocalContext.current

    var isScanning by remember { mutableStateOf(autoStartScanning) }
    var isCroppingImage by remember { mutableStateOf(false) }
    var capturedImagePath by remember { mutableStateOf<String?>(null) }

    // 当拍照或裁剪状态改变时，通知外部（用于隐藏底部导航栏）
    LaunchedEffect(isScanning, isCroppingImage) {
        onScanningStateChange(isScanning || isCroppingImage)
    }

    // 从相册选择图片
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult

        isScanning = false

        val destFile = File(
            context.getExternalFilesDir("images"),
            "gallery_${System.currentTimeMillis()}.jpg"
        )

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    copyStream(inputStream, outputStream)
                }
            }
            // 显示裁剪界面
            capturedImagePath = destFile.absolutePath
            isCroppingImage = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 极简白底设计
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 如果正在裁剪图片（拍照或相册），显示裁剪界面
        if (isCroppingImage && capturedImagePath != null) {
            com.edusmart.app.ui.components.ImageCropPreview(
                imagePath = capturedImagePath!!,
                onCropComplete = { croppedPath ->
                    isCroppingImage = false
                    // 删除原始图片
                    File(capturedImagePath!!).delete()
                    // 跳转到结果页面
                    val encodedPath = URLEncoder.encode(croppedPath, StandardCharsets.UTF_8.toString())
                    navController.navigate("scan_result/$encodedPath")
                },
                onCancel = {
                    isCroppingImage = false
                    // 删除临时文件
                    capturedImagePath?.let { File(it).delete() }
                    capturedImagePath = null
                    if (autoStartScanning) {
                        onScanningStateChange(false)
                        navController.popBackStack()
                    }
                }
            )
        }
        // 如果正在拍照，优先全屏展示相机界面
        else if (isScanning) {
            CameraPreview(
                onImageCaptured = { imagePath ->
                    isScanning = false
                    // 显示裁剪界面
                    capturedImagePath = imagePath
                    isCroppingImage = true
                },
                onCancel = {
                    if (autoStartScanning) {
                        onScanningStateChange(false)
                        navController.popBackStack()
                    } else {
                        isScanning = false
                    }
                },
                onPickFromGallery = {
                    imagePickerLauncher.launch("image/*")
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 标题区域
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "拍照识题",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "对准题目拍照，AI自动识别并匹配答案",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "请将题目放在取景框中央，保持光线充足和画面清晰",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // 按钮区域
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BeautyButton(
                        text = "📷 开始拍照",
                        onClick = { isScanning = true },
                        isPrimary = true
                    )

                    BeautyButton(
                        text = "🖼 从相册选图",
                        onClick = { imagePickerLauncher.launch("image/*") },
                        isPrimary = false
                    )
                }
            }
        }
    }
}

// 简单的流拷贝工具函数
private fun copyStream(input: InputStream, output: OutputStream) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val bytesRead = input.read(buffer)
        if (bytesRead <= 0) break
        output.write(buffer, 0, bytesRead)
    }
}

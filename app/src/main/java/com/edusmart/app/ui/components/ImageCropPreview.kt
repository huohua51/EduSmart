package com.edusmart.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.edusmart.app.util.ImageCropUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImageCropPreview(
    imagePath: String,
    onCropComplete: (String) -> Unit,
    onCancel: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isCropping by remember { mutableStateOf(false) }
    var cropFrame by remember { mutableStateOf(CropFrame(0.1f, 0.2f, 0.9f, 0.8f)) }
    val bitmap = remember { BitmapFactory.decodeFile(imagePath) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        DraggableCameraGuide(
            guideText = "调整裁剪区域",
            onFrameChanged = { frame -> cropFrame = frame }
        )

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("取消")
            }
            Button(
                onClick = {
                    if (isCropping) return@Button
                    isCropping = true
                    scope.launch {
                        try {
                            val croppedPath = withContext(Dispatchers.IO) {
                                ImageCropUtil.cropImage(imagePath, cropFrame.left, cropFrame.top, cropFrame.right, cropFrame.bottom)
                            }
                            onCropComplete(croppedPath)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            onCropComplete(imagePath)
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isCropping
            ) {
                Text(if (isCropping) "裁剪中..." else "确认裁剪")
            }
        }
    }
}

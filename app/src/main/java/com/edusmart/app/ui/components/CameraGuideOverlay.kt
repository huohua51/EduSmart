package com.edusmart.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 相机拍照指导框组件
 * 显示一个矩形框引导用户将题目放置在框内
 */
@Composable
fun CameraGuideOverlay(
    modifier: Modifier = Modifier,
    guideText: String = "请将题目放入框内",
    qualityMessage: String? = null,
    isQualityGood: Boolean = true
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 绘制半透明遮罩和指导框
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // 指导框的尺寸（占屏幕的70%宽度，50%高度）
            val frameWidth = canvasWidth * 0.7f
            val frameHeight = canvasHeight * 0.5f
            val frameLeft = (canvasWidth - frameWidth) / 2
            val frameTop = (canvasHeight - frameHeight) / 2

            // 绘制半透明遮罩（框外区域）
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(0f, 0f),
                size = Size(canvasWidth, frameTop)
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(0f, frameTop + frameHeight),
                size = Size(canvasWidth, canvasHeight - frameTop - frameHeight)
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(0f, frameTop),
                size = Size(frameLeft, frameHeight)
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(frameLeft + frameWidth, frameTop),
                size = Size(canvasWidth - frameLeft - frameWidth, frameHeight)
            )

            // 绘制指导框边框
            val frameColor = if (isQualityGood) Color.Green else Color.Red
            val strokeWidth = 4.dp.toPx()

            // 绘制虚线边框
            drawRoundRect(
                color = frameColor,
                topLeft = Offset(frameLeft, frameTop),
                size = Size(frameWidth, frameHeight),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = Stroke(
                    width = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(20f, 10f),
                        phase = 0f
                    )
                )
            )

            // 绘制四个角的实线标记
            val cornerLength = 40.dp.toPx()
            val cornerStrokeWidth = 6.dp.toPx()

            // 左上角
            drawLine(
                color = frameColor,
                start = Offset(frameLeft, frameTop),
                end = Offset(frameLeft + cornerLength, frameTop),
                strokeWidth = cornerStrokeWidth
            )
            drawLine(
                color = frameColor,
                start = Offset(frameLeft, frameTop),
                end = Offset(frameLeft, frameTop + cornerLength),
                strokeWidth = cornerStrokeWidth
            )

            // 右上角
            drawLine(
                color = frameColor,
                start = Offset(frameLeft + frameWidth, frameTop),
                end = Offset(frameLeft + frameWidth - cornerLength, frameTop),
                strokeWidth = cornerStrokeWidth
            )
            drawLine(
                color = frameColor,
                start = Offset(frameLeft + frameWidth, frameTop),
                end = Offset(frameLeft + frameWidth, frameTop + cornerLength),
                strokeWidth = cornerStrokeWidth
            )

            // 左下角
            drawLine(
                color = frameColor,
                start = Offset(frameLeft, frameTop + frameHeight),
                end = Offset(frameLeft + cornerLength, frameTop + frameHeight),
                strokeWidth = cornerStrokeWidth
            )
            drawLine(
                color = frameColor,
                start = Offset(frameLeft, frameTop + frameHeight),
                end = Offset(frameLeft, frameTop + frameHeight - cornerLength),
                strokeWidth = cornerStrokeWidth
            )

            // 右下角
            drawLine(
                color = frameColor,
                start = Offset(frameLeft + frameWidth, frameTop + frameHeight),
                end = Offset(frameLeft + frameWidth - cornerLength, frameTop + frameHeight),
                strokeWidth = cornerStrokeWidth
            )
            drawLine(
                color = frameColor,
                start = Offset(frameLeft + frameWidth, frameTop + frameHeight),
                end = Offset(frameLeft + frameWidth, frameTop + frameHeight - cornerLength),
                strokeWidth = cornerStrokeWidth
            )
        }

        // 文字提示
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部指导文字
            Text(
                text = guideText,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 质量检测提示
            if (qualityMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = qualityMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isQualityGood) Color.Green else Color.Red,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .padding(top = 16.dp)
                )
            }
        }
    }
}

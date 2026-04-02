package com.edusmart.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.edusmart.app.ui.theme.*

/**
 * 可拖动调整的相机指导框组件
 * 类似作业帮的拍照框，支持拖动四个边来调整大小
 */
/**
 * 相框位置数据类
 */
data class CropFrame(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

@Composable
fun DraggableCameraGuide(
    modifier: Modifier = Modifier,
    guideText: String = "请将题目放入框内",
    onFrameChanged: (CropFrame) -> Unit = {}
) {
    val density = LocalDensity.current

    // 指导框的位置和尺寸状态（使用相对比例，0-1之间）
    var frameLeft by remember { mutableStateOf(0.15f) }
    var frameTop by remember { mutableStateOf(0.25f) }
    var frameRight by remember { mutableStateOf(0.85f) }
    var frameBottom by remember { mutableStateOf(0.75f) }

    // 当相框位置改变时，通知外部
    LaunchedEffect(frameLeft, frameTop, frameRight, frameBottom) {
        onFrameChanged(CropFrame(frameLeft, frameTop, frameRight, frameBottom))
    }

    // 拖动区域的大小（dp）
    val dragAreaSize = with(density) { 40.dp.toPx() }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 绘制遮罩和指导框
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()

                        val canvasWidth = size.width.toFloat()
                        val canvasHeight = size.height.toFloat()

                        // 计算当前框的像素位置
                        val leftPx = frameLeft * canvasWidth
                        val topPx = frameTop * canvasHeight
                        val rightPx = frameRight * canvasWidth
                        val bottomPx = frameBottom * canvasHeight

                        val touchX = change.position.x
                        val touchY = change.position.y

                        // 判断触摸点在哪个边的拖动区域
                        val onLeftEdge = touchX in (leftPx - dragAreaSize)..(leftPx + dragAreaSize) &&
                                         touchY in topPx..bottomPx
                        val onRightEdge = touchX in (rightPx - dragAreaSize)..(rightPx + dragAreaSize) &&
                                          touchY in topPx..bottomPx
                        val onTopEdge = touchY in (topPx - dragAreaSize)..(topPx + dragAreaSize) &&
                                        touchX in leftPx..rightPx
                        val onBottomEdge = touchY in (bottomPx - dragAreaSize)..(bottomPx + dragAreaSize) &&
                                           touchX in leftPx..rightPx

                        // 根据拖动位置调整框的大小
                        // 最小尺寸改为0.05（5%），允许更小的裁剪框
                        if (onLeftEdge) {
                            val newLeft = frameLeft + dragAmount.x / canvasWidth
                            // 限制最小宽度和边界
                            if (newLeft >= 0.05f && newLeft < frameRight - 0.05f) {
                                frameLeft = newLeft
                            }
                        }
                        if (onRightEdge) {
                            val newRight = frameRight + dragAmount.x / canvasWidth
                            if (newRight <= 0.95f && newRight > frameLeft + 0.05f) {
                                frameRight = newRight
                            }
                        }
                        if (onTopEdge) {
                            val newTop = frameTop + dragAmount.y / canvasHeight
                            if (newTop >= 0.05f && newTop < frameBottom - 0.05f) {
                                frameTop = newTop
                            }
                        }
                        if (onBottomEdge) {
                            val newBottom = frameBottom + dragAmount.y / canvasHeight
                            if (newBottom <= 0.95f && newBottom > frameTop + 0.05f) {
                                frameBottom = newBottom
                            }
                        }
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // 计算框的实际像素位置
            val leftPx = frameLeft * canvasWidth
            val topPx = frameTop * canvasHeight
            val rightPx = frameRight * canvasWidth
            val bottomPx = frameBottom * canvasHeight
            val frameWidth = rightPx - leftPx
            val frameHeight = bottomPx - topPx

            // 绘制半透明遮罩（框外区域）- 使用更柔和的遮罩
            val maskColor = Color(0xFF35568a).copy(alpha = 0.3f) // PrimaryBlue 半透明

            // 上方遮罩
            drawRect(
                color = maskColor,
                topLeft = Offset(0f, 0f),
                size = Size(canvasWidth, topPx)
            )
            // 下方遮罩
            drawRect(
                color = maskColor,
                topLeft = Offset(0f, bottomPx),
                size = Size(canvasWidth, canvasHeight - bottomPx)
            )
            // 左侧遮罩
            drawRect(
                color = maskColor,
                topLeft = Offset(0f, topPx),
                size = Size(leftPx, frameHeight)
            )
            // 右侧遮罩
            drawRect(
                color = maskColor,
                topLeft = Offset(rightPx, topPx),
                size = Size(canvasWidth - rightPx, frameHeight)
            )

            // 绘制指导框边框（实线）- 使用温暖的蓝色
            val frameColor = Color(0xFF8b9bc1) // SecondaryBlue
            val strokeWidth = 3.dp.toPx()

            drawRoundRect(
                color = frameColor,
                topLeft = Offset(leftPx, topPx),
                size = Size(frameWidth, frameHeight),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                style = Stroke(width = strokeWidth)
            )

            // 绘制四个角的加粗标记 - 使用深蓝色
            val cornerColor = Color(0xFF35568a) // PrimaryBlue
            val cornerLength = 30.dp.toPx()
            val cornerStrokeWidth = 5.dp.toPx()

            // 左上角
            drawLine(
                color = cornerColor,
                start = Offset(leftPx, topPx),
                end = Offset(leftPx + cornerLength, topPx),
                strokeWidth = cornerStrokeWidth
            )
            drawLine(
                color = cornerColor,
                start = Offset(leftPx, topPx),
                end = Offset(leftPx, topPx + cornerLength),
                strokeWidth = cornerStrokeWidth
            )

            // 右上角
            drawLine(
                color = cornerColor,
                start = Offset(rightPx, topPx),
                end = Offset(rightPx - cornerLength, topPx),
                strokeWidth = cornerStrokeWidth
            )
            drawLine(
                color = cornerColor,
                start = Offset(rightPx, topPx),
                end = Offset(rightPx, topPx + cornerLength),
                strokeWidth = cornerStrokeWidth
            )

            // 左下角
            drawLine(
                color = cornerColor,
                start = Offset(leftPx, bottomPx),
                end = Offset(leftPx + cornerLength, bottomPx),
                strokeWidth = cornerStrokeWidth
            )
            drawLine(
                color = cornerColor,
                start = Offset(leftPx, bottomPx),
                end = Offset(leftPx, bottomPx - cornerLength),
                strokeWidth = cornerStrokeWidth
            )

            // 右下角
            drawLine(
                color = cornerColor,
                start = Offset(rightPx, bottomPx),
                end = Offset(rightPx - cornerLength, bottomPx),
                strokeWidth = cornerStrokeWidth
            )
            drawLine(
                color = cornerColor,
                start = Offset(rightPx, bottomPx),
                end = Offset(rightPx, bottomPx - cornerLength),
                strokeWidth = cornerStrokeWidth
            )

            // 绘制四条边的中点拖动标记（小圆点）
            val dotRadius = 8.dp.toPx()
            val dotColor = Color.White

            // 左边中点
            drawCircle(
                color = dotColor,
                radius = dotRadius,
                center = Offset(leftPx, (topPx + bottomPx) / 2)
            )
            drawCircle(
                color = cornerColor,
                radius = dotRadius,
                center = Offset(leftPx, (topPx + bottomPx) / 2),
                style = Stroke(width = 2.dp.toPx())
            )

            // 右边中点
            drawCircle(
                color = dotColor,
                radius = dotRadius,
                center = Offset(rightPx, (topPx + bottomPx) / 2)
            )
            drawCircle(
                color = cornerColor,
                radius = dotRadius,
                center = Offset(rightPx, (topPx + bottomPx) / 2),
                style = Stroke(width = 2.dp.toPx())
            )

            // 上边中点
            drawCircle(
                color = dotColor,
                radius = dotRadius,
                center = Offset((leftPx + rightPx) / 2, topPx)
            )
            drawCircle(
                color = cornerColor,
                radius = dotRadius,
                center = Offset((leftPx + rightPx) / 2, topPx),
                style = Stroke(width = 2.dp.toPx())
            )

            // 下边中点
            drawCircle(
                color = dotColor,
                radius = dotRadius,
                center = Offset((leftPx + rightPx) / 2, bottomPx)
            )
            drawCircle(
                color = cornerColor,
                radius = dotRadius,
                center = Offset((leftPx + rightPx) / 2, bottomPx),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // 顶部提示文字
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = guideText,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "拖动边框调整识别区域",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

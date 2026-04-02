package com.edusmart.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edusmart.app.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * 雷达图组件 - 优雅的圆形设计
 */
@Composable
fun RadarChart(
    data: Map<String, Float>,
    modifier: Modifier = Modifier,
    maxValue: Float = 100f,
    gridLevels: Int = 5
) {
    val subjects = data.keys.toList()
    val values = data.values.toList()

    if (subjects.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无数据",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp)
                .padding(24.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
            val radius = minOf(canvasWidth, canvasHeight) / 2f * 0.65f

            val angleStep = 2 * PI.toFloat() / subjects.size

            // ✨ 绘制外圆框（最重要的特征）
            drawCircle(
                color = Color(0xFF35568a), // PrimaryBlue
                radius = radius,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // 绘制同心圆网格线（更细腻的层次）
            for (level in 1..gridLevels) {
                val levelRadius = radius * level / gridLevels

                drawCircle(
                    color = Color(0xFF8b9bc1).copy(alpha = 0.25f), // SecondaryBlue
                    radius = levelRadius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // 绘制轴线（从中心到外圆）
            for (i in subjects.indices) {
                val angle = -PI.toFloat() / 2 + angleStep * i
                val endX = center.x + radius * cos(angle)
                val endY = center.y + radius * sin(angle)

                drawLine(
                    color = Color(0xFF8b9bc1).copy(alpha = 0.4f), // SecondaryBlue
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            // 绘制数据区域（浅色填充）
            val dataPath = Path()
            for (i in subjects.indices) {
                val angle = -PI.toFloat() / 2 + angleStep * i
                val value = values[i].coerceIn(0f, maxValue)
                val dataRadius = radius * (value / maxValue)
                val x = center.x + dataRadius * cos(angle)
                val y = center.y + dataRadius * sin(angle)

                if (i == 0) {
                    dataPath.moveTo(x, y)
                } else {
                    dataPath.lineTo(x, y)
                }
            }
            dataPath.close()

            // 填充数据区域（使用淡蓝色）
            drawPath(
                path = dataPath,
                color = Color(0xFF8b9bc1).copy(alpha = 0.35f) // SecondaryBlue 半透明
            )

            // 绘制数据边框（深蓝色）
            drawPath(
                path = dataPath,
                color = Color(0xFF35568a), // PrimaryBlue
                style = Stroke(width = 2.5.dp.toPx())
            )

            // 绘制数据点
            for (i in subjects.indices) {
                val angle = -PI.toFloat() / 2 + angleStep * i
                val value = values[i].coerceIn(0f, maxValue)
                val dataRadius = radius * (value / maxValue)
                val x = center.x + dataRadius * cos(angle)
                val y = center.y + dataRadius * sin(angle)

                // 外圈（白色边框）
                drawCircle(
                    color = Color.White,
                    radius = 5.dp.toPx(),
                    center = Offset(x, y)
                )
                // 内圈（深蓝色）
                drawCircle(
                    color = Color(0xFF35568a), // PrimaryBlue
                    radius = 3.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            // 在图形周围绘制标签
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                for (i in subjects.indices) {
                    val angle = -PI.toFloat() / 2 + angleStep * i
                    val labelDistance = radius + 50.dp.toPx()
                    val labelX = center.x + labelDistance * cos(angle)
                    val labelY = center.y + labelDistance * sin(angle)

                    // 根据角度调整文本对齐方式
                    paint.textAlign = when {
                        angle < -PI.toFloat() * 0.75f || angle > PI.toFloat() * 0.75f -> android.graphics.Paint.Align.CENTER
                        angle < -PI.toFloat() * 0.25f -> android.graphics.Paint.Align.RIGHT
                        angle > PI.toFloat() * 0.25f -> android.graphics.Paint.Align.LEFT
                        else -> android.graphics.Paint.Align.CENTER
                    }

                    // 绘制科目名称（深蓝色，稍大字体）
                    paint.textSize = 15.sp.toPx()
                    paint.color = Color(0xFF35568a).toArgb() // PrimaryBlue
                    paint.typeface = android.graphics.Typeface.create(
                        android.graphics.Typeface.DEFAULT,
                        android.graphics.Typeface.BOLD
                    )
                    canvas.nativeCanvas.drawText(
                        subjects[i],
                        labelX,
                        labelY,
                        paint
                    )

                    // 绘制分数（次要蓝色）
                    paint.textSize = 13.sp.toPx()
                    paint.color = Color(0xFF8b9bc1).toArgb() // SecondaryBlue
                    paint.typeface = android.graphics.Typeface.DEFAULT
                    canvas.nativeCanvas.drawText(
                        "${values[i].toInt()}分",
                        labelX,
                        labelY + 20.dp.toPx(),
                        paint
                    )
                }
            }
        }
    }
}

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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * 雷达图组件
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
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val primaryColor = MaterialTheme.colorScheme.primary
        val textColor = MaterialTheme.colorScheme.onSurface

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(48.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
            val radius = minOf(canvasWidth, canvasHeight) / 2f * 0.6f

            val angleStep = 2 * PI.toFloat() / subjects.size

            // 绘制网格线
            for (level in 1..gridLevels) {
                val levelRadius = radius * level / gridLevels
                val path = Path()

                for (i in subjects.indices) {
                    val angle = -PI.toFloat() / 2 + angleStep * i
                    val x = center.x + levelRadius * cos(angle)
                    val y = center.y + levelRadius * sin(angle)

                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                path.close()

                drawPath(
                    path = path,
                    color = Color.Gray.copy(alpha = 0.3f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // 绘制轴线
            for (i in subjects.indices) {
                val angle = -PI.toFloat() / 2 + angleStep * i
                val endX = center.x + radius * cos(angle)
                val endY = center.y + radius * sin(angle)

                drawLine(
                    color = Color.Gray.copy(alpha = 0.5f),
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 绘制数据区域
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

            // 填充数据区域
            drawPath(
                path = dataPath,
                color = primaryColor.copy(alpha = 0.3f)
            )

            // 绘制数据边框
            drawPath(
                path = dataPath,
                color = primaryColor,
                style = Stroke(width = 2.dp.toPx())
            )

            // 在图形周围绘制标签
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = textColor.toArgb()
                    textSize = 14.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }

                for (i in subjects.indices) {
                    val angle = -PI.toFloat() / 2 + angleStep * i
                    val labelDistance = radius + 40.dp.toPx()
                    val labelX = center.x + labelDistance * cos(angle)
                    val labelY = center.y + labelDistance * sin(angle)

                    // 根据角度调整文本对齐方式
                    paint.textAlign = when {
                        angle < -PI.toFloat() * 0.75f || angle > PI.toFloat() * 0.75f -> android.graphics.Paint.Align.CENTER
                        angle < -PI.toFloat() * 0.25f -> android.graphics.Paint.Align.RIGHT
                        angle > PI.toFloat() * 0.25f -> android.graphics.Paint.Align.LEFT
                        else -> android.graphics.Paint.Align.CENTER
                    }

                    // 绘制科目名称
                    canvas.nativeCanvas.drawText(
                        subjects[i],
                        labelX,
                        labelY,
                        paint
                    )

                    // 绘制分数
                    paint.textSize = 12.sp.toPx()
                    paint.color = primaryColor.toArgb()
                    canvas.nativeCanvas.drawText(
                        "${values[i].toInt()}分",
                        labelX,
                        labelY + 18.dp.toPx(),
                        paint
                    )
                    paint.textSize = 14.sp.toPx()
                    paint.color = textColor.toArgb()
                }
            }
        }
    }
}

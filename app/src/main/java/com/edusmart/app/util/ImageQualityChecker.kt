package com.edusmart.app.util

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 图片质量检测工具类
 * 用于检测拍照图片的亮度、清晰度、对比度等质量指标
 */
class ImageQualityChecker {

    /**
     * 检测图片质量
     * @param bitmap 待检测的图片
     * @return 质量检测结果
     */
    fun checkQuality(bitmap: Bitmap): QualityResult {
        val brightness = calculateBrightness(bitmap)
        val sharpness = calculateSharpness(bitmap)
        val contrast = calculateContrast(bitmap)

        // 判断质量是否合格
        val isGood = brightness in BRIGHTNESS_MIN..BRIGHTNESS_MAX &&
                     sharpness >= SHARPNESS_MIN &&
                     contrast >= CONTRAST_MIN

        // 生成提示信息
        val message = when {
            brightness < BRIGHTNESS_MIN -> "光线太暗，请增加光线或打开闪光灯"
            brightness > BRIGHTNESS_MAX -> "光线过强，请避免逆光或强光直射"
            sharpness < SHARPNESS_MIN -> "图片模糊，请保持手机稳定重新拍照"
            contrast < CONTRAST_MIN -> "对比度不足，请调整拍摄角度"
            else -> "图片质量良好"
        }

        return QualityResult(
            brightness = brightness,
            sharpness = sharpness,
            contrast = contrast,
            isGood = isGood,
            message = message
        )
    }

    /**
     * 计算图片亮度
     * 返回值范围：0-255
     */
    private fun calculateBrightness(bitmap: Bitmap): Int {
        val width = bitmap.width
        val height = bitmap.height

        // 采样计算，提高性能
        val sampleSize = 10
        var sum = 0L
        var count = 0

        for (y in 0 until height step sampleSize) {
            for (x in 0 until width step sampleSize) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                // 使用加权平均计算亮度
                sum += (r * 299 + g * 587 + b * 114) / 1000
                count++
            }
        }

        return if (count > 0) (sum / count).toInt() else 0
    }

    /**
     * 计算图片清晰度
     * 使用Laplacian算子计算边缘强度
     * 返回值越大表示越清晰
     */
    private fun calculateSharpness(bitmap: Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height

        // 转换为灰度图
        val grayPixels = Array(height) { IntArray(width) }
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                grayPixels[y][x] = (r * 299 + g * 587 + b * 114) / 1000
            }
        }

        // 使用Laplacian算子
        var sum = 0.0
        var count = 0

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val laplacian = abs(
                    4 * grayPixels[y][x] -
                    grayPixels[y - 1][x] -
                    grayPixels[y + 1][x] -
                    grayPixels[y][x - 1] -
                    grayPixels[y][x + 1]
                )
                sum += laplacian
                count++
            }
        }

        return if (count > 0) sum / count else 0.0
    }

    /**
     * 计算图片对比度
     * 使用标准差计算
     */
    private fun calculateContrast(bitmap: Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height

        // 采样计算
        val sampleSize = 10
        val values = mutableListOf<Int>()

        for (y in 0 until height step sampleSize) {
            for (x in 0 until width step sampleSize) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                values.add((r * 299 + g * 587 + b * 114) / 1000)
            }
        }

        if (values.isEmpty()) return 0.0

        // 计算平均值
        val mean = values.average()

        // 计算标准差
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance)
    }

    companion object {
        // 亮度阈值（0-255）
        private const val BRIGHTNESS_MIN = 50
        private const val BRIGHTNESS_MAX = 200

        // 清晰度阈值
        private const val SHARPNESS_MIN = 10.0

        // 对比度阈值
        private const val CONTRAST_MIN = 30.0
    }
}

/**
 * 图片质量检测结果
 */
data class QualityResult(
    val brightness: Int,        // 亮度值 (0-255)
    val sharpness: Double,      // 清晰度值
    val contrast: Double,       // 对比度值
    val isGood: Boolean,        // 是否合格
    val message: String         // 提示信息
)

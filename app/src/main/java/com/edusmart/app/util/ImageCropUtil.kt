package com.edusmart.app.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

/**
 * 图片裁剪工具类
 * 用于根据相框位置裁剪图片
 */
object ImageCropUtil {

    /**
     * 根据相框比例裁剪图片
     * @param imagePath 原始图片路径
     * @param frameLeft 相框左边位置（0-1）
     * @param frameTop 相框顶部位置（0-1）
     * @param frameRight 相框右边位置（0-1）
     * @param frameBottom 相框底部位置（0-1）
     * @return 裁剪后的图片路径
     */
    fun cropImage(
        imagePath: String,
        frameLeft: Float,
        frameTop: Float,
        frameRight: Float,
        frameBottom: Float
    ): String {
        val originalFile = File(imagePath)

        // 读取原始图片
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = false
        var bitmap = BitmapFactory.decodeFile(imagePath, options)

        // 处理图片旋转（根据EXIF信息）
        bitmap = rotateImageIfRequired(bitmap, imagePath)

        val width = bitmap.width
        val height = bitmap.height

        // 计算裁剪区域
        val cropX = (frameLeft * width).toInt()
        val cropY = (frameTop * height).toInt()
        val cropWidth = ((frameRight - frameLeft) * width).toInt()
        val cropHeight = ((frameBottom - frameTop) * height).toInt()

        // 确保裁剪区域在图片范围内
        val safeX = cropX.coerceIn(0, width - 1)
        val safeY = cropY.coerceIn(0, height - 1)
        val safeWidth = cropWidth.coerceIn(1, width - safeX)
        val safeHeight = cropHeight.coerceIn(1, height - safeY)

        // 裁剪图片
        val croppedBitmap = Bitmap.createBitmap(
            bitmap,
            safeX,
            safeY,
            safeWidth,
            safeHeight
        )

        // 保存裁剪后的图片
        val croppedFile = File(
            originalFile.parent,
            "cropped_${originalFile.name}"
        )

        FileOutputStream(croppedFile).use { out ->
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        // 释放资源
        bitmap.recycle()
        croppedBitmap.recycle()

        return croppedFile.absolutePath
    }

    /**
     * 根据EXIF信息旋转图片
     */
    private fun rotateImageIfRequired(bitmap: Bitmap, imagePath: String): Bitmap {
        val exif = ExifInterface(imagePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
            else -> bitmap
        }
    }

    /**
     * 旋转图片
     */
    private fun rotateImage(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}

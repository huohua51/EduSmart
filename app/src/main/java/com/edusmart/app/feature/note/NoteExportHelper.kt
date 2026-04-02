package com.edusmart.app.feature.note

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.edusmart.app.data.entity.NoteEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 笔记导出和分享工具类
 */
class NoteExportHelper(private val context: Context) {
    
    /**
     * 导出笔记为PDF
     */
    fun exportToPdf(note: NoteEntity): File? {
        return try {
            val exportsDir = context.getExternalFilesDir("exports")
            if (exportsDir == null) {
                android.util.Log.e("NoteExportHelper", "无法获取exports目录")
                return null
            }
            
            val pdfFile = File(
                exportsDir,
                "note_${note.id}_${System.currentTimeMillis()}.pdf"
            ).apply {
                parentFile?.mkdirs()
            }
            
            android.util.Log.d("NoteExportHelper", "开始导出PDF: ${pdfFile.absolutePath}")
            
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            
            val paint = Paint().apply {
                textSize = 12f
                isAntiAlias = true
            }
            
            var y = 50f
            val margin = 50f
            val lineHeight = 20f
            val pageWidth = pageInfo.pageWidth - 2 * margin
            
            // 标题
            paint.textSize = 18f
            paint.isFakeBoldText = true
            canvas.drawText(note.title, margin, y, paint)
            y += lineHeight * 2
            
            // 科目和时间
            paint.textSize = 12f
            paint.isFakeBoldText = false
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val dateStr = dateFormat.format(Date(note.createdAt))
            canvas.drawText("科目: ${note.subject} | 创建时间: $dateStr", margin, y, paint)
            y += lineHeight * 2
            
            // 内容
            val lines = note.content.split("\n")
            paint.textSize = 12f
            var currentPage = page
            var currentCanvas = canvas
            var currentPageInfo = pageInfo
            for (line in lines) {
                if (y > currentPageInfo.pageHeight - 100) {
                    // 新页面
                    document.finishPage(currentPage)
                    val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create()
                    val newPage = document.startPage(newPageInfo)
                    currentPage = newPage
                    currentCanvas = newPage.canvas
                    currentPageInfo = newPageInfo
                    y = 50f
                }
                
                // 处理长文本换行
                val textWidth = paint.measureText(line)
                if (textWidth > pageWidth) {
                    var start = 0
                    while (start < line.length) {
                        var end = start
                        var currentWidth = 0f
                        while (end < line.length && currentWidth < pageWidth) {
                            currentWidth = paint.measureText(line.substring(start, end + 1))
                            end++
                        }
                        currentCanvas.drawText(line.substring(start, end), margin, y, paint)
                        y += lineHeight
                        start = end
                    }
                } else {
                    currentCanvas.drawText(line, margin, y, paint)
                    y += lineHeight
                }
            }
            
            // 知识点
            if (!note.knowledgePoints.isNullOrEmpty()) {
                if (y > currentPageInfo.pageHeight - 100) {
                    document.finishPage(currentPage)
                    val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create()
                    val newPage = document.startPage(newPageInfo)
                    currentPage = newPage
                    currentCanvas = newPage.canvas
                    currentPageInfo = newPageInfo
                    y = 50f
                }
                y += lineHeight
                paint.isFakeBoldText = true
                currentCanvas.drawText("知识点:", margin, y, paint)
                y += lineHeight
                paint.isFakeBoldText = false
                note.knowledgePoints.forEach { point ->
                    if (y > currentPageInfo.pageHeight - 100) {
                        document.finishPage(currentPage)
                        val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create()
                        val newPage = document.startPage(newPageInfo)
                        currentPage = newPage
                        currentCanvas = newPage.canvas
                        currentPageInfo = newPageInfo
                        y = 50f
                    }
                    currentCanvas.drawText("• $point", margin + 20, y, paint)
                    y += lineHeight
                }
            }
            
            document.finishPage(currentPage)
            
            val fileOutputStream = FileOutputStream(pdfFile)
            document.writeTo(fileOutputStream)
            document.close()
            fileOutputStream.close()
            
            android.util.Log.d("NoteExportHelper", "PDF导出成功: ${pdfFile.absolutePath}, 大小: ${pdfFile.length()} bytes")
            pdfFile
        } catch (e: Exception) {
            android.util.Log.e("NoteExportHelper", "PDF导出失败", e)
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 导出笔记为图片
     */
    fun exportToImage(note: NoteEntity): File? {
        return try {
            val exportsDir = context.getExternalFilesDir("exports")
            if (exportsDir == null) {
                android.util.Log.e("NoteExportHelper", "无法获取exports目录")
                return null
            }
            
            val width = 800
            val height = 1200
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            android.util.Log.d("NoteExportHelper", "开始导出图片")
            
            // 背景
            canvas.drawColor(android.graphics.Color.WHITE)
            
            val paint = Paint().apply {
                textSize = 24f
                isAntiAlias = true
                color = android.graphics.Color.BLACK
            }
            
            var y = 50f
            val margin = 50f
            val lineHeight = 30f
            
            // 标题
            paint.textSize = 28f
            paint.isFakeBoldText = true
            canvas.drawText(note.title, margin, y, paint)
            y += lineHeight * 2
            
            // 科目和时间
            paint.textSize = 18f
            paint.isFakeBoldText = false
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val dateStr = dateFormat.format(Date(note.createdAt))
            canvas.drawText("科目: ${note.subject} | 创建时间: $dateStr", margin, y, paint)
            y += lineHeight * 2
            
            // 内容
            paint.textSize = 20f
            val lines = note.content.split("\n")
            val pageWidth = width - 2 * margin
            for (line in lines) {
                if (y > height - 100) break
                
                // 处理长文本换行
                val textWidth = paint.measureText(line)
                if (textWidth > pageWidth) {
                    var start = 0
                    while (start < line.length && y <= height - 100) {
                        var end = start
                        var currentWidth = 0f
                        while (end < line.length && currentWidth < pageWidth) {
                            currentWidth = paint.measureText(line.substring(start, end + 1))
                            end++
                        }
                        canvas.drawText(line.substring(start, end), margin, y, paint)
                        y += lineHeight
                        start = end
                    }
                } else {
                    canvas.drawText(line, margin, y, paint)
                    y += lineHeight
                }
            }
            
            // 知识点
            if (!note.knowledgePoints.isNullOrEmpty() && y <= height - 150) {
                y += lineHeight
                paint.textSize = 22f
                paint.isFakeBoldText = true
                canvas.drawText("知识点:", margin, y, paint)
                y += lineHeight
                paint.textSize = 20f
                paint.isFakeBoldText = false
                note.knowledgePoints.forEach { point ->
                    if (y > height - 100) return@forEach
                    canvas.drawText("• $point", margin + 20, y, paint)
                    y += lineHeight
                }
            }
            
            // 保存图片
            val imageFile = File(
                exportsDir,
                "note_${note.id}_${System.currentTimeMillis()}.png"
            ).apply {
                parentFile?.mkdirs()
            }
            
            val fileOutputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.close()
            
            android.util.Log.d("NoteExportHelper", "图片导出成功: ${imageFile.absolutePath}, 大小: ${imageFile.length()} bytes")
            imageFile
        } catch (e: Exception) {
            android.util.Log.e("NoteExportHelper", "图片导出失败", e)
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 分享笔记（文本）
     */
    fun shareNote(note: NoteEntity) {
        val shareText = buildString {
            append("【${note.title}】\n")
            append("科目: ${note.subject}\n")
            append("创建时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(note.createdAt))}\n\n")
            append(note.content)
            if (!note.knowledgePoints.isNullOrEmpty()) {
                append("\n\n知识点:\n")
                note.knowledgePoints.forEach { point ->
                    append("• $point\n")
                }
            }
        }
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, note.title)
        }
        
        context.startActivity(Intent.createChooser(intent, "分享笔记"))
    }
    
    /**
     * 分享文件（PDF或图片）
     */
    fun shareFile(file: File, mimeType: String) {
        try {
            if (!file.exists()) {
                android.util.Log.e("NoteExportHelper", "文件不存在: ${file.absolutePath}")
                android.widget.Toast.makeText(
                    context,
                    "文件不存在，无法分享",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return
            }
            
            android.util.Log.d("NoteExportHelper", "准备分享文件: ${file.absolutePath}, MIME类型: $mimeType")
            
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                } catch (e: Exception) {
                    android.util.Log.e("NoteExportHelper", "FileProvider获取URI失败", e)
                    android.widget.Toast.makeText(
                        context,
                        "文件分享失败: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return
                }
            } else {
                Uri.fromFile(file)
            }
            
            android.util.Log.d("NoteExportHelper", "URI: $uri")
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(intent, "分享文件")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            
            android.util.Log.d("NoteExportHelper", "分享Intent已启动")
        } catch (e: Exception) {
            android.util.Log.e("NoteExportHelper", "分享文件失败", e)
            e.printStackTrace()
            android.widget.Toast.makeText(
                context,
                "分享失败: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}


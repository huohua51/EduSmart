package com.edusmart.app.feature.ar.model

/**
 * 3D模型项
 */
data class ModelItem(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val modelPath: String,
    val thumbnailUrl: String,
    val scale: Float = 0.1f,
    var format: String = ""
) {
    init {
        // 自动从路径中提取格式
        if (format.isEmpty()) {
            this.format = modelPath.substringAfterLast('.', "")
        }
    }
}

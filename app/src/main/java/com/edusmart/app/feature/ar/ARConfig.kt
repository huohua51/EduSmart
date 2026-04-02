package com.edusmart.app.feature.ar

import android.content.Context
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.edusmart.app.feature.ar.model.ModelItem

/**
 * AR模块配置常量
 */
object ARConfig {

    // 应用配置
    const val APP_NAME = "AR知识空间"
    const val APP_VERSION = "1.0.0"

    // AR配置
    const val AR_CORE_MIN_VERSION = "1.41.0"
    val DEFAULT_PLANE_FINDING_MODE = Config.PlaneFindingMode.HORIZONTAL
    val DEFAULT_LIGHT_ESTIMATION_MODE = Config.LightEstimationMode.ENVIRONMENTAL_HDR

    // 模型配置
    const val DEFAULT_MODEL_SCALE = 0.1f
    const val MIN_MODEL_SCALE = 0.01f
    const val MAX_MODEL_SCALE = 5.0f

    // 支持的3D模型格式
    val SUPPORTED_MODEL_FORMATS = listOf(
        "glb", "gltf", "fbx", "obj", "stl"
    )

    // 模型库URL（可在线加载）
    const val MODEL_LIBRARY_URL = "https://your-api.com/models"

    // 本地模型路径
    const val LOCAL_MODELS_PATH = "model/"

    // 手势灵敏度
    const val ROTATION_SENSITIVITY = 2.0f
    const val SCALE_SENSITIVITY = 0.01f
    const val TRANSLATION_SENSITIVITY = 0.005f

    // 性能配置
    const val MAX_MODEL_COUNT = 10
    const val CACHE_SIZE_MB = 100L

    /**
     * 检查设备是否支持AR
     */
    fun isDeviceSupported(context: Context): Boolean {
        return try {
            val availability = ArCoreApk.getInstance().checkAvailability(context)
            availability.isSupported
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取ARCore状态
     */
    fun getARCoreStatus(context: Context): String {
        return try {
            val availability = ArCoreApk.getInstance().checkAvailability(context)
            when {
                availability.isSupported -> "SUPPORTED"
                availability.isTransient -> "TRANSIENT_CHECKING"
                availability.isUnknown -> "UNKNOWN"
                else -> "UNSUPPORTED"
            }
        } catch (e: Exception) {
            "ERROR: ${e.message}"
        }
    }

    /**
     * 创建默认的AR会话配置
     */
    fun createDefaultConfig(session: Session): Config {
        return Config(session).apply {
            planeFindingMode = DEFAULT_PLANE_FINDING_MODE
            lightEstimationMode = DEFAULT_LIGHT_ESTIMATION_MODE
            cloudAnchorMode = Config.CloudAnchorMode.DISABLED
            focusMode = Config.FocusMode.AUTO
        }
    }

    /**
     * 获取默认模型列表
     */
    fun getDefaultModels(): List<ModelItem> {
        return listOf(
            ModelItem(
                id = "cube",
                name = "立方体",
                description = "基础几何体",
                category = "几何",
                modelPath = "model/cube.glb",
                thumbnailUrl = "https://placehold.co/300x200/3b82f6/white?text=立方体",
                scale = 0.1f
            ),
            ModelItem(
                id = "sphere",
                name = "球体",
                description = "基础几何体",
                category = "几何",
                modelPath = "model/sphere.glb",
                thumbnailUrl = "https://placehold.co/300x200/ef4444/white?text=球体",
                scale = 0.1f
            ),
            ModelItem(
                id = "cone",
                name = "圆锥",
                description = "基础几何体",
                category = "几何",
                modelPath = "model/cone.glb",
                thumbnailUrl = "https://placehold.co/300x200/10b981/white?text=圆锥",
                scale = 0.1f
            ),
            ModelItem(
                id = "benzene",
                name = "苯分子",
                description = "化学分子结构",
                category = "化学",
                modelPath = "model/chemistry/Benzene.glb",
                thumbnailUrl = "https://placehold.co/300x200/8b5cf6/white?text=苯分子",
                scale = 0.1f
            ),
            ModelItem(
                id = "ch4",
                name = "甲烷",
                description = "化学分子结构",
                category = "化学",
                modelPath = "model/chemistry/CH4.glb",
                thumbnailUrl = "https://placehold.co/300x200/06b6d4/white?text=甲烷",
                scale = 0.1f
            ),
            ModelItem(
                id = "co2",
                name = "二氧化碳",
                description = "化学分子结构",
                category = "化学",
                modelPath = "model/chemistry/co2.glb",
                thumbnailUrl = "https://placehold.co/300x200/f59e0b/white?text=二氧化碳",
                scale = 0.1f
            ),
            ModelItem(
                id = "h2o",
                name = "水分子",
                description = "化学分子结构",
                category = "化学",
                modelPath = "model/chemistry/H2O.glb",
                thumbnailUrl = "https://placehold.co/300x200/3b82f6/white?text=水分子",
                scale = 0.1f
            ),
            ModelItem(
                id = "o3",
                name = "臭氧",
                description = "化学分子结构",
                category = "化学",
                modelPath = "model/chemistry/O3.glb",
                thumbnailUrl = "https://placehold.co/300x200/10b981/white?text=臭氧",
                scale = 0.1f
            )
        )
    }
}

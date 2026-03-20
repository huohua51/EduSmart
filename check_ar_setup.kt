import com.google.ar.core.ArCoreApk

fun main() {
    println("检查AR Core支持...")
    
    // 模拟检查AR支持
    val isSupported = checkARSupport()
    
    if (isSupported) {
        println("✅ 设备支持AR Core")
    } else {
        println("❌ 设备不支持AR Core")
    }
    
    println("检查完成!")
}

fun checkARSupport(): Boolean {
    return try {
        // 这里只是模拟检查，实际需要在Android环境中运行
        true
    } catch (e: Exception) {
        false
    }
}
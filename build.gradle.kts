// Top-level build file
// 仓库配置统一在 settings.gradle.kts 中管理

plugins {
    // 暂时移除 Google Services 插件（使用腾讯云开发）
    // 如果需要使用 Firebase，请取消注释并修复版本兼容性问题
    // id("com.google.gms.google-services") version "4.4.4" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

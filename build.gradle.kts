// Top-level build file
// 仓库配置统一在 settings.gradle.kts 中管理

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

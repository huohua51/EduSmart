pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    
    plugins {
        id("com.android.application") version "8.6.0"
        id("org.jetbrains.kotlin.android") version "1.9.22"
        id("org.jetbrains.kotlin.kapt") version "1.9.22"
        // 暂时移除 Google Services 插件（使用腾讯云开发）
        // id("com.google.gms.google-services") version "4.4.4"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // JitPack 用于 GitHub 上的库（如 MPAndroidChart）
        maven { url = uri("https://jitpack.io") }
        // Sonatype 快照仓库（SceneView 备用）
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
    }
}

rootProject.name = "EduSmart"
include(":app")

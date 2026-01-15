pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    
    plugins {
        id("com.android.application") version "8.1.0"
        id("org.jetbrains.kotlin.android") version "1.9.10"
        id("org.jetbrains.kotlin.kapt") version "1.9.10"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // JitPack 用于 GitHub 上的库（如 MPAndroidChart）
        maven { url = uri("https://jitpack.io") }
        // SceneView 仓库
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
    }
}

rootProject.name = "EduSmart"
include(":app")

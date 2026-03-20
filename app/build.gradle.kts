plugins {
    id("com.android.application") version "8.6.0" // 添加版本号
    id("org.jetbrains.kotlin.android") version "1.9.22" // 升级到 1.9.22 以兼容 Compose Compiler 1.5.3
    id("org.jetbrains.kotlin.kapt")
    // 暂时移除 Google Services 插件（使用腾讯云开发）
    // id("com.google.gms.google-services")
}

android {
    namespace = "com.edusmart.app"
    compileSdk = 35  // 从 34 升级到 35

    defaultConfig {
        applicationId = "com.edusmart.app"
        minSdk = 26
        targetSdk = 35  // 从 34 升级到 35（与 compileSdk 保持一致）
        versionCode = 2  // Beta版本：版本号递增
        versionName = "1.0.0-beta"  // Beta版本标识

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    buildTypes {
        debug {
            // Debug版本用于测试
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 如果需要签名，在这里配置
            // signingConfig = signingConfigs.getByName("release")
        }
        // Beta版本配置
        create("beta") {
            initWith(getByName("release"))
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"
            isDebuggable = false
            isMinifyEnabled = false  // Beta版本暂时不混淆，方便调试
            matchingFallbacks += listOf("release", "debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        viewBinding = true
        dataBinding = false
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"  // 更新以兼容 Kotlin 1.9.22
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs(file("src/main/jniLibs"))
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
        }
        jniLibs {
            useLegacyPackaging = true
            pickFirsts += "**/libSparkChain.so"
            pickFirsts += "**/libspark.so"
        }
    }
}

dependencies {
    // Core Android - 保持当前版本，或升级到兼容版本
    implementation("androidx.core:core-ktx:1.12.0")  // 如果不需要 1.16.0，保持当前版本
    // 或者升级到兼容版本：
    // implementation("androidx.core:core-ktx:1.12.0")  // 保持当前版本，与 compileSdk 34 兼容

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // Camera & Media
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")

    // ARCore
    implementation("com.google.ar:core:1.41.0")

    // SceneView - 使用兼容Java 17的版本
    implementation("io.github.sceneview:sceneview:2.2.0")
    implementation("io.github.sceneview:arsceneview:2.2.0")

    // OCR - Google ML Kit
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("com.google.mlkit:text-recognition-chinese:16.0.0")

    // SparkChain SDK - 讯飞新版语音听写SDK (AAR格式)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))

    // 网络请求
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // 数据库
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // 图像处理
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("io.coil-kt:coil-compose:2.5.0")

    // 权限处理
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // 日志库
    implementation("com.jakewharton.timber:timber:5.0.1")

    // 图表可视化
    implementation("com.github.PhilJay:MPAndroidChart:3.1.0")

    // 3D渲染
    implementation("org.joml:joml:1.10.5")

    // 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")

    // Firebase - 暂时注释掉（使用腾讯云开发）
    // 如果需要使用 Firebase，请取消注释并修复版本兼容性问题
    // implementation(platform("com.google.firebase:firebase-bom:34.8.0"))
    // implementation("com.google.firebase:firebase-auth-ktx")
    // implementation("com.google.firebase:firebase-firestore-ktx")
    // implementation("com.google.firebase:firebase-storage-ktx")
    // implementation("com.google.firebase:firebase-analytics-ktx")

    // 测试
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // 添加版本约束，防止依赖升级到不兼容的版本
    constraints {
        implementation("androidx.core:core-ktx") {
            version {
                strictly("1.12.0")  // 强制使用 1.12.0，避免自动升级到 1.16.0
            }
        }
        implementation("androidx.core:core") {
            version {
                strictly("1.12.0")
            }
        }
    }
}
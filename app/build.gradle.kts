plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.edusmart.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.edusmart.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // NDK配置 - 支持SparkChain SDK所需的架构
        // 注意：x86 和 x86_64 架构的 .so 文件可能不正确，暂时只支持 ARM 架构
        // 如果需要在模拟器上测试，请确保有正确的 x86_64 架构文件
        ndk {
            // 只支持 ARM 架构（真机设备）
            // 如果需要支持模拟器，请确保 x86_64 目录下有正确的文件
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
            // abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")  // 取消注释以支持所有架构
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        // 暂时禁用数据绑定，避免相关错误
        dataBinding = false
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
    
    // 明确指定 jniLibs 源目录，确保 .so 文件被正确包含
    sourceSets {
        getByName("main") {
            // 使用 file() 确保路径正确，相对于项目根目录
            jniLibs.srcDirs(file("src/main/jniLibs"))
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // 排除 AAR 文件中可能冲突的资源
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
        }
        // 原生库（.so 文件）打包配置
        jniLibs {
            // 使用传统打包方式，确保 .so 文件被正确打包到 lib/ 目录
            // 对于某些 AGP 版本，useLegacyPackaging = true 更可靠
            useLegacyPackaging = true
            // 确保包含所有架构的 .so 文件
            // 如果多个 AAR 包含相同的 .so 文件，使用 pickFirsts 选择第一个
            pickFirsts += "**/libSparkChain.so"
            pickFirsts += "**/libspark.so"
            // 确保不排除任何 .so 文件
            // 注意：不要使用 excludes，这会排除 .so 文件
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
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
    implementation("com.google.ar:core:1.40.0")
    
    // SceneView - 最简单的AR库（推荐）
    // 封装了ARCore和3D渲染，支持GLTF模型和手势交互
    // 注意：如果网络无法访问，可以暂时注释掉，AR功能将不可用
    // implementation("io.github.sceneview:arsceneview:2.6.0")
    
    // 临时方案：如果SceneView无法下载，可以暂时移除AR功能
    // 或者使用其他AR库，如直接使用ARCore
    
    // OCR - Google ML Kit
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("com.google.mlkit:text-recognition-chinese:16.0.0")
    
    // SparkChain SDK - 讯飞新版语音听写SDK (AAR格式)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    
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
    
    // 权限处理
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    
    // 图表可视化
    implementation("com.github.PhilJay:MPAndroidChart:3.1.0")
    
    // 3D渲染 - 使用标准 joml 库（joml-android 不存在）
    implementation("org.joml:joml:1.10.5")
    
    // 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    
    // 测试
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

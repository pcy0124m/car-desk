plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.loomy.cardesk"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.loomy.cardesk"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        // 高德地图 Key 从 gradle.properties 注入 Manifest
        // 这样 Key 不写在源码里，方便各机器/仓库切换
        manifestPlaceholders["AMAP_KEY"] = (project.findProperty("AMAP_KEY") as String?) ?: ""
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    // 高德 3D 地图 SDK（车机横屏实时地图）
    implementation("com.amap.api:3dmap:9.8.3")
    // 高德定位 SDK（让地图跟着车走）
    implementation("com.amap.api:location:6.4.3")
}
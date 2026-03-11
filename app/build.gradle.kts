plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.test"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.test"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildFeatures {
            viewBinding = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Room依赖
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // 网络相关依赖（核心）
    implementation(libs.gson)                  // Gson解析
    implementation(libs.okhttp)               // OkHttp核心
    implementation(libs.loggingInterceptor)
    implementation(libs.retrofitConverterGson)// Retrofit Gson解析器
    implementation(libs.retrofit)             // Retrofit核心

}
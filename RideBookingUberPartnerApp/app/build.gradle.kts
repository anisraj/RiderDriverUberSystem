import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

android {
    namespace = "me.anisjamadar.locationsharingapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "me.anisjamadar.locationsharingapplication"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_KEY", gradleLocalProperties(rootDir, providers).getProperty("MY_API_KEY"))
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
    buildFeatures {
        buildConfig = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(project(":grpc"))

    //OlaMap SDK
    implementation(files("libs/OlaMapSdk-1.6.0.aar"))
    implementation(files("libs/Places-sdk-2.3.9.jar"))
    //Maplibre
    implementation (libs.maplibre.sdk)
    implementation (libs.maplibre.android.plugin.annotation)
    implementation (libs.maplibre.markerview.plugin.annotation)
    //Ola maps uses views, so for that these two libraries needed
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.cardview)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Location Services
    implementation(libs.play.services.location)

    //retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson.convertor)
    //okhttp
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    //hilt
    implementation(libs.hilt)
    kapt(libs.hilt.compiler)

    //for injecting viewmodel into compose functions
    implementation(libs.hilt.navigation.compose)
}
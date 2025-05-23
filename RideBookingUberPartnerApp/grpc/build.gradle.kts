plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.protobuf)
}

android {
    namespace = "me.anisjamadar.grpc"
    compileSdk = 35

    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.19.2"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.47.0"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                maybeCreate("java").apply {
                    option("lite")
                }
            }
            task.plugins {
                maybeCreate("grpc").apply {
                    option("lite")
                }
            }
        }
    }
}

dependencies {

    implementation("io.grpc:grpc-okhttp:1.47.0")
    implementation("io.grpc:grpc-protobuf-lite:1.47.0")
    implementation("io.grpc:grpc-stub:1.47.0")
    implementation("org.apache.tomcat:annotations-api:6.0.53")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}
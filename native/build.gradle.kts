plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "me.asrielyankare.gachaultils.jni"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        externalNativeBuild {
            cmake {
                cppFlags += ""
                arguments += "-DANDROID_STL=c++_static"
            }
        }

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
            version = "3.22.1"
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
    implementation("androidx.core:core-ktx:1.12.0")
}

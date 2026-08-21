plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "me.asrielyankare.gachaultils.blackbox"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
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
    implementation(project(":core"))

    // NewBlackbox integration
    // The NewBlackbox library should be placed as an AAR in blackbox/libs/
    // Or include the NewBlackbox project as a composite build in settings.gradle.kts
    //
    // For now, the integration layer is designed to work with NewBlackbox's API:
    // - top.niunaijun.blackbox.BlackBoxCore
    // - top.niunaijun.blackbox.fake.frameworks.BPackageManager
    // - top.niunaijun.blackbox.fake.frameworks.BActivityManager
    // - top.niunaijun.blackbox.fake.frameworks.BUserManager
    // - top.niunaijun.blackbox.core.env.BEnvironment
    // - top.niunaijun.blackbox.entity.pm.InstallOption
    // - top.niunaijun.blackbox.entity.pm.InstallResult
    // - top.niunaijun.blackbox.core.system.user.BUserInfo
    //
    // When NewBlackbox is available, uncomment the following:
    // implementation(project(":NewBlackbox:Bcore"))

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

plugins {
    id("com.android.library")
}

android {
    namespace = "me.asrielyankare.gachaultils.blackbox"
    compileSdk = 36

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
}

dependencies {
    implementation(project(":core"))

    // NewBlackbox Bcore AAR
    // Build the AAR via GitHub Actions: .github/workflows/build-newblackbox.yml
    // Then trigger the workflow manually (Actions → Build NewBlackbox AAR → Run workflow)
    // The AAR will be auto-committed to blackbox/libs/
    fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))).forEach { aar ->
        implementation(files(aar))
    }

    implementation(libs.core.ktx)
    implementation(libs.coroutines.core)
}

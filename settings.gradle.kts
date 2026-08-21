pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set("strict")
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "GachaCloudLoader"

include(":app")
include(":core")
include(":blackbox")
include(":native")
include(":gradle")
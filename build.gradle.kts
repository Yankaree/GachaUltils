plugins {
    kotlin("android") version "1.9.0" apply true
    application
}

repositories {
    mavenCentral()
    google()
}

tasks.register("test") {
    useTest {}
}
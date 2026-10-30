pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev")
        maven("https://maven.minecraftforge.net")
        maven("https://maven.kikugie.dev/releases")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.6.2"
}

stonecutter {
    centralScript = "build.gradle.kts"
    kotlinController = true
    create(rootProject) {
        // Root nodes (`:1.20.1`) only hold `versions/<mc>/gradle.properties`; no code lives there.
        versions("1.18.2", "1.20.1", "1.21.1")
        vcsVersion = "1.20.1"
        branch("common")
        branch("fabric")
        branch("forge")
    }
}

rootProject.name = "cmdEssentials"

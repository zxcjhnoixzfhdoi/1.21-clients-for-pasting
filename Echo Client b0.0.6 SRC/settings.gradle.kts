pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven(url = "https://maven.kikugie.dev/snapshots")
        mavenCentral()
        mavenLocal()
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9"
}

stonecutter {
    create(rootProject) {
        versions("1.21.11")
        version("26.1", "26.1").buildscript("build.26.1.gradle.kts")
        version("26.1.1", "26.1.1").buildscript("build.26.1.gradle.kts")
        version("26.1.2", "26.1.2").buildscript("build.26.1.gradle.kts")
        version("26.2-snapshot-2", "26.2-snapshot-2").buildscript("build.26.2.gradle.kts")
        vcsVersion = "1.21.11"
    }
}

rootProject.name = "echo"

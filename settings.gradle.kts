pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version("8.7.0")
        id("org.jetbrains.kotlin.android") version("1.9.22")
        id("org.jetbrains.kotlin.kapt") version("1.9.22")
        id("com.google.dagger.hilt.android") version("2.51.1")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FairSpeed"
include(":app")

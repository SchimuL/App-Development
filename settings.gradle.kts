pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // JitPack ist notwendig für MPAndroidChart UND Material-Calendarview
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "FCC_app"
include(":app")
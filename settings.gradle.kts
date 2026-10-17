@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        maven {
            name = "aliucord"
            url = uri("https://maven.aliucord.com/releases")
        }
        maven {
            name = "aliucord"
            url = uri("https://maven.aliucord.com/snapshots")
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            name = "aliucord"
            url = uri("https://maven.aliucord.com/snapshots")
        }
        google()
        mavenCentral()
        maven {
            name = "aliucord"
            url = uri("https://maven.aliucord.com/releases")
        }
    }
}

for (folder in rootDir.resolve("plugins").listFiles { it.isDirectory }) {
    include(":plugins:${folder.name}")
}
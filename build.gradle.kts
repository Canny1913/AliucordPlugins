import com.aliucord.gradle.AliucordExtension

import com.android.build.gradle.LibraryExtension

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension

plugins {
    alias(libs.plugins.aliucord.plugin)
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
}

subprojects {
    if (name == "plugins") {
        return@subprojects
    }

    val libs = rootProject.libs

    pluginManager.apply(libs.plugins.aliucord.plugin.get().pluginId)
    pluginManager.apply(libs.plugins.android.library.get().pluginId)
    pluginManager.apply(libs.plugins.kotlin.android.get().pluginId)

    configure<AliucordExtension> {
        author("canny1913", 1264872702821273633L, hyperlink = true)
        github("https://github.com/canny1913/AliucordPlugins")
    }

    configure<LibraryExtension> {
        namespace = "com.github.canny1913"
        compileSdk = 36

        defaultConfig {
            minSdk = 21
        }

        buildFeatures {
            aidl = false
            buildConfig = true
            renderScript = false
            shaders = false
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }
    }

    configure<KotlinAndroidExtension> {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
    }

    dependencies {
        val compileOnly by configurations

        compileOnly(libs.aliucord)
        compileOnly(libs.discord)
        compileOnly(libs.kotlin.stdlib)
    }
}
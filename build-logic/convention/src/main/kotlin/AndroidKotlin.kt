package com.awan.app.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/**
 * Configure base Kotlin with Android settings.
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension,
) {
    commonExtension.apply {
        compileSdk = 37
    }

    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}

/**
 * Configure Kotlin and Android settings for Application modules.
 */
internal fun Project.configureKotlinAndroid(
    applicationExtension: ApplicationExtension,
) {
    configureKotlinAndroid(applicationExtension as CommonExtension)
    applicationExtension.apply {
        defaultConfig {
            minSdk = 24
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
}

/**
 * Configure Kotlin and Android settings for Library modules.
 */
internal fun Project.configureKotlinAndroid(
    libraryExtension: LibraryExtension,
) {
    configureKotlinAndroid(libraryExtension as CommonExtension)
    libraryExtension.apply {
        defaultConfig {
            minSdk = 24
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
}
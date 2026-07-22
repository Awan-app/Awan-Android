import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.awan.buildlogic"

// The build-logic project uses the JVM embedded in Gradle.
// We lock the source/target to Java 17 to match the main project's toolchain.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Classpath dependencies for convention plugin code (not transitive to consumers).
dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

gradlePlugin {
    plugins {
        // Equivalent of com.android.application + kotlin.android configured for Awan.
        register("androidApplication") {
            id = "awan.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        // Equivalent of com.android.library + kotlin.android configured for Awan.
        register("androidLibrary") {
            id = "awan.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        // Pure-Kotlin JVM library (no Android) — for :core:model, :core:domain.
        register("jvmLibrary") {
            id = "awan.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        // Adds Jetpack Compose configuration (BOM + compiler plugin).
        register("androidCompose") {
            id = "awan.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        // Applies KSP + Hilt, wires hilt-android + hilt-compiler deps automatically.
        register("androidHilt") {
            id = "awan.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        // Meta-plugin for feature modules: library + hilt + common feature deps.
        register("androidFeature") {
            id = "awan.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
    }
}
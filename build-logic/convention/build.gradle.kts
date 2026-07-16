import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.awan.app.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "awan.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidCompose") {
            id = "awan.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidLibrary") {
            id = "awan.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "awan.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
    }
}
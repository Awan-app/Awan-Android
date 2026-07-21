import java.util.Properties

plugins {
    alias(libs.plugins.awan.android.library)
    alias(libs.plugins.awan.android.hilt)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

val awanBaseUrl = localProperties.getProperty("awan.base.url") 
    ?: "https://backend-production-dec8.up.railway.app/api/"
val awanBaseUrlFormatted = "\"$awanBaseUrl\""

android {
    namespace = "com.awan.app.core.network"

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "AWAN_BASE_URL", awanBaseUrlFormatted)
            buildConfigField("String", "ENVIRONMENT", "\"debug\"")
        }
        release {
            buildConfigField("String", "AWAN_BASE_URL", awanBaseUrlFormatted)
            buildConfigField("String", "ENVIRONMENT", "\"release\"")
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // JSON
    implementation(libs.kotlinx.serialization.json)
}

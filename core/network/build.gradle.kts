plugins {
    alias(libs.plugins.awan.android.library)
    alias(libs.plugins.awan.android.hilt)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

android {
    namespace = "com.awan.app.core.network"

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "AWAN_BASE_URL", "\"https://api.awan.app/\"")
            buildConfigField("String", "ENVIRONMENT", "\"debug\"")
        }
        release {
            buildConfigField("String", "AWAN_BASE_URL", "\"https://api.awan.app/\"")
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

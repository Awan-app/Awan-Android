plugins {
    id("awan.android.library")
    id("awan.android.compose")
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

android {
    namespace = "com.awan.core.navigation"
}

dependencies {
    api(libs.androidx.navigation3.runtime)
    api(libs.kotlinx.serialization.core)
    implementation(libs.androidx.compose.material.icons.core)
}

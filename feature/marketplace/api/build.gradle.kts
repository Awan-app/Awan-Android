plugins {
    alias(libs.plugins.awan.android.library)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

android {
    namespace = "com.awan.feature.marketplace.api"
}

dependencies {
    implementation(project(":core:navigation"))
    implementation(libs.kotlinx.serialization.core)
}

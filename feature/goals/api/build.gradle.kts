plugins {
    alias(libs.plugins.awan.android.library)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

android {
    namespace = "com.awan.feature.goals.api"
}

dependencies {
    api(project(":core:navigation"))
}

plugins {
    alias(libs.plugins.awan.android.feature)
    alias(libs.plugins.awan.android.compose)
}

android {
    namespace = "com.awan.feature.auth.impl"
}

dependencies {
    implementation(project(":feature:auth:api"))
}

plugins {
    alias(libs.plugins.awan.android.feature)
    alias(libs.plugins.awan.android.compose)
}

android {
    namespace = "com.awan.feature.profile.impl"
}

dependencies {
    implementation(project(":feature:profile:api"))
}

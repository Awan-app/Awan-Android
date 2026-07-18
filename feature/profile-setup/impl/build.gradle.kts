plugins {
    alias(libs.plugins.awan.android.feature)
    alias(libs.plugins.awan.android.compose)
}

android {
    namespace = "com.awan.feature.profile_setup.impl"
}

dependencies {
    implementation(project(":feature:profile-setup:api"))
}
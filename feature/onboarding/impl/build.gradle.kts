plugins {
    alias(libs.plugins.awan.android.feature)
    alias(libs.plugins.awan.android.compose)
}

android {
    namespace = "com.awan.feature.onboarding.impl"
}

dependencies {
    implementation(project(":feature:onboarding:api"))
}

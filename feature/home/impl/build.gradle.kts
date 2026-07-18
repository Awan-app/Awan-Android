plugins {
    alias(libs.plugins.awan.android.feature)
    alias(libs.plugins.awan.android.compose)
}

android {
    namespace = "com.awan.feature.home.impl"
}

dependencies {
    implementation(project(":feature:home:api"))
}

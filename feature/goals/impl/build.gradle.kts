plugins {
    alias(libs.plugins.awan.android.feature)
    alias(libs.plugins.awan.android.compose)
}

android {
    namespace = "com.awan.feature.goals.impl"
}

dependencies {
    implementation(project(":feature:goals:api"))
}

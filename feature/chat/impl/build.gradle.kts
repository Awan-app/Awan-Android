plugins {
    alias(libs.plugins.awan.android.feature)
    alias(libs.plugins.awan.android.compose)
}

android {
    namespace = "com.awan.feature.chat.impl"
}

dependencies {
    implementation(project(":feature:chat:api"))
}

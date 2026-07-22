plugins {
    alias(libs.plugins.awan.android.library)
    alias(libs.plugins.awan.android.compose)
    alias(libs.plugins.awan.android.hilt)
}

android {
    namespace = "com.awan.app.core.common"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.compose.ui)
}

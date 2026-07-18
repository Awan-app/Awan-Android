plugins {
    alias(libs.plugins.awan.android.library)
    alias(libs.plugins.awan.android.hilt)
}

android {
    namespace = "com.awan.app.core.common"
}

dependencies {
    // kotlinx-coroutines for Flow operators used in asResult().
    implementation(libs.kotlinx.coroutines.core)
}

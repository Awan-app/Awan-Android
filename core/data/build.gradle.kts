plugins {
    alias(libs.plugins.awan.android.library)
    alias(libs.plugins.awan.android.hilt)
}

android {
    namespace = "com.awan.app.core.data"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:network"))
    implementation(project(":core:datastore"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}

plugins {
    alias(libs.plugins.awan.android.library)
    alias(libs.plugins.awan.android.hilt)
    alias(libs.plugins.awan.android.workmanager)
}

android {
    namespace = "com.awan.app.core.notifications"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    // Only for AwanToastManager — the in-app presentation of a notification while the app is open.
    // The BOM is here to version design-system's transitive Compose artifacts; this module has no
    // composables, so it deliberately does not apply the compose convention plugin.
    implementation(platform(libs.androidx.compose.bom))
    implementation(project(":core:design-system"))
    implementation(libs.androidx.core.ktx)
    // ProcessLifecycleOwner — tells the poster whether the app is on screen right now.
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

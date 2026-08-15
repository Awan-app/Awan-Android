plugins {
    alias(libs.plugins.awan.android.feature)
    alias(libs.plugins.awan.android.compose)
}

android {
    namespace = "com.awan.feature.profile.impl"
    // For BuildConfig.DEBUG alone: the notification settings screen carries a debug-only card that
    // fires each notification on demand, and it must not reach a release build.
    buildFeatures { buildConfig = true }
}

dependencies {
    implementation(project(":feature:profile:api"))
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(project(":core:model"))
    implementation(project(":core:design-system"))
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(project(":core:navigation"))
    implementation(libs.androidx.appcompat)
}

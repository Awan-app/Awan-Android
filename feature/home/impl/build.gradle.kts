plugins {
    alias(libs.plugins.awan.android.feature)
    alias(libs.plugins.awan.android.compose)
}

android {
    namespace = "com.awan.feature.home.impl"
}

dependencies {
    implementation(project(":feature:home:api"))
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:design-system"))
    implementation(libs.androidx.compose.material.icons.extended)
}

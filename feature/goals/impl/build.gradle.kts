plugins {
    alias(libs.plugins.awan.android.feature)
    alias(libs.plugins.awan.android.compose)
}

android {
    namespace = "com.awan.feature.goals.impl"
}

dependencies {
    implementation(project(":feature:goals:api"))
    implementation(project(":core:design-system"))
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
}

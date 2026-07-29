plugins {
    alias(libs.plugins.awan.android.feature)
    alias(libs.plugins.awan.android.compose)
    alias(libs.plugins.awan.android.hilt)
}

android {
    namespace = "com.awan.feature.calendar.impl"
}

dependencies {
    implementation(project(":feature:calendar:api"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:design-system"))
}

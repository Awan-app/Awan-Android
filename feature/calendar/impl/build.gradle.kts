plugins {
    alias(libs.plugins.awan.android.feature)
    alias(libs.plugins.awan.android.compose)
}

android {
    namespace = "com.awan.feature.calendar.impl"
}

dependencies {
    implementation(project(":feature:calendar:api"))
    implementation(project(":core:design-system"))
    implementation(project(":core:data"))
    implementation(project(":core:model"))
}

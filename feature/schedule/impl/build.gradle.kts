plugins {
    alias(libs.plugins.awan.android.feature)
    alias(libs.plugins.awan.android.compose)
}

android {
    namespace = "com.awan.feature.schedule.impl"
}

dependencies {
    implementation(project(":feature:schedule:api"))

    implementation(project(":core:scheduling"))
    implementation(project(":core:navigation"))
    implementation(project(":core:model"))
}
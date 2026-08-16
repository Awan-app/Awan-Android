plugins {
    alias(libs.plugins.awan.android.feature)
    alias(libs.plugins.awan.android.compose)
}

android {
    namespace = "com.awan.feature.splash.impl"
}

dependencies {
    implementation(project(":feature:splash:api"))
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(project(":core:design-system"))
}

plugins {
    alias(libs.plugins.awan.android.feature)
    alias(libs.plugins.awan.android.compose)
}

android {
    namespace = "com.awan.feature.aitasks.impl"
}

dependencies {
    implementation(project(":feature:ai-tasks:api"))
    implementation(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:design-system"))

    testImplementation(libs.kotlinx.coroutines.test)
}

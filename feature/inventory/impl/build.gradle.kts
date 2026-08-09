plugins {
    alias(libs.plugins.awan.android.feature)
    alias(libs.plugins.awan.android.compose)
}

android {
    namespace = "com.awan.feature.inventory.impl"
}

dependencies {
    implementation(project(":feature:inventory:api"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:design-system"))
    implementation(project(":core:navigation"))
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

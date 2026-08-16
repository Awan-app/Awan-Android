plugins {
    alias(libs.plugins.awan.android.feature)
    alias(libs.plugins.awan.android.compose)
}

android {
    namespace = "com.awan.feature.taskdetails.impl"
}

dependencies {
    implementation(project(":feature:task-details:api"))
    implementation(project(":feature:goals:api"))
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:design-system"))
    implementation(project(":core:model"))
    implementation(libs.androidx.compose.material.icons.extended)
}

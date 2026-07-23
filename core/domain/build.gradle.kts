plugins {
    alias(libs.plugins.awan.android.library)
    alias(libs.plugins.awan.android.hilt)
}

android {
    namespace = "com.awan.app.core.domain"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:common"))
    implementation(libs.kotlinx.coroutines.core)
    compileOnly(libs.javax.inject)
    testImplementation(libs.junit)
}

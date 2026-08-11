plugins {
    alias(libs.plugins.awan.android.library)
    alias(libs.plugins.awan.android.hilt)
    alias(libs.plugins.awan.android.workmanager)
}


android {
    namespace = "com.awan.app.core.data"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:database"))
    api(project(":core:network"))
    implementation(project(":core:datastore"))
    implementation(libs.room.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.retrofit)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

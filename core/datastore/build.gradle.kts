plugins {
    alias(libs.plugins.awan.android.library)
    alias(libs.plugins.awan.android.hilt)
}

android {
    namespace = "com.awan.app.core.datastore"
}

dependencies {
    // Proto-generated UserPreferences classes.
    implementation(project(":core:datastore-proto"))
    // AppError, Result, Dispatcher qualifier.
    implementation(project(":core:common"))

    // Proto DataStore (structured UserPreferences).
    implementation(libs.datastore.core)
    // Plain Preferences DataStore is NOT used here — tokens use EncryptedSharedPreferences.

    // EncryptedSharedPreferences for hardware-backed token storage (Android Keystore).
    implementation(libs.androidx.security.crypto)
}

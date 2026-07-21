plugins {
    alias(libs.plugins.awan.android.library)
    alias(libs.plugins.awan.android.hilt)
    alias(libs.plugins.awan.android.room)
}

android {
    namespace = "com.awan.app.core.database"
}

// Export Room schema files so migrations can be validated with MigrationTestHelper.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}


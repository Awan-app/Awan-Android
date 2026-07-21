plugins {
    alias(libs.plugins.awan.jvm.library)
}

dependencies {
    implementation(project(":core:model"))
    // Annotation only — Hilt/Dagger provides javax.inject at runtime in the consuming app.
    compileOnly(libs.javax.inject)

    testImplementation(libs.junit)
}

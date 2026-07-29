plugins {
    alias(libs.plugins.awan.android.application)
    alias(libs.plugins.awan.android.compose)
    alias(libs.plugins.awan.android.hilt)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

android {
    namespace = "com.awan.app"

    defaultConfig {
        applicationId = "com.awan.app"
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    lint {
        abortOnError = true
        disable += setOf(
            "TypographyFractions",
            "TypographyQuotes",
            "IconMissingDensityFolder",
        )
        error += "HardcodedText"
        error += "MissingTranslation"
    }
}

dependencies {
    // Feature modules
    implementation(project(":feature:splash:impl"))
    implementation(project(":feature:onboarding:api"))
    implementation(project(":feature:onboarding:impl"))
    implementation(project(":feature:auth:api"))
    implementation(project(":feature:auth:impl"))
    implementation(project(":feature:home:api"))
    implementation(project(":feature:home:impl"))
    implementation(project(":feature:calendar:api"))
    implementation(project(":feature:calendar:impl"))
    implementation(project(":feature:chat:api"))
    implementation(project(":feature:chat:impl"))
    implementation(project(":feature:goals:api"))
    implementation(project(":feature:goals:impl"))
    implementation(project(":feature:profile:api"))
    implementation(project(":feature:profile:impl"))
    implementation(project(":feature:marketplace:api"))
    implementation(project(":feature:marketplace:impl"))
    implementation(project(":feature:add-task"))

    // Core modules
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:model"))
    implementation(project(":core:datastore"))
    implementation(project(":core:network"))
    implementation(project(":core:design-system"))
    implementation(project(":core:navigation"))
    implementation(project(":feature:splash:api"))


    // Compose (platform managed by awan.android.compose convention plugin)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)

    // Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.appcompat)

    // Navigation 3
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.material3.adaptive.navigation3)

    // Serialization
    implementation(libs.kotlinx.serialization.core)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // Lint
    lintChecks(libs.composeLintChecks)
}

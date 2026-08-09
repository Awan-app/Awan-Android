plugins {
    alias(libs.plugins.awan.android.application)
    alias(libs.plugins.awan.android.compose)
    alias(libs.plugins.awan.android.hilt)
    alias(libs.plugins.awan.android.workmanager)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.google.services)
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

abstract class EnsureGoogleServicesJsonTask : DefaultTask() {
    @get:Internal
    abstract val projectDirProperty: DirectoryProperty

    @TaskAction
    fun ensureFile() {
        val googleServicesFile = projectDirProperty.file("google-services.json").get().asFile
        if (!googleServicesFile.exists()) {
            googleServicesFile.writeText(
                """
                {
                  "project_info": {
                    "project_number": "000000000000",
                    "project_id": "awan-dummy",
                    "storage_bucket": "awan-dummy.appspot.com"
                  },
                  "client": [
                    {
                      "client_info": {
                        "mobilesdk_app_id": "1:000000000000:android:0000000000000000000000",
                        "android_client_info": {
                          "package_name": "com.awan.app"
                        }
                      },
                      "oauth_client": [],
                      "api_key": [
                        {
                          "current_key": "dummy_api_key"
                        }
                      ],
                      "services": {}
                    }
                  ],
                  "configuration_version": "1"
                }
                """.trimIndent()
            )
        }
    }
}

val ensureGoogleServicesJson = tasks.register<EnsureGoogleServicesJsonTask>("ensureGoogleServicesJson") {
    projectDirProperty.set(layout.projectDirectory)
}

tasks.matching { it.name.startsWith("process") && it.name.contains("GoogleServices") }.configureEach {
    dependsOn(ensureGoogleServicesJson)
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
    implementation(project(":feature:ai-tasks:api"))
    implementation(project(":feature:ai-tasks:impl"))
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
    implementation(libs.androidx.hilt.navigation.compose)

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

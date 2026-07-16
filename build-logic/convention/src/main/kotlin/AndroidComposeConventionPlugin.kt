import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.awan.app.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            // Enable compose build feature. We must defer until the correct Android plugin
            // has been applied, because the registered extension type differs:
            //   com.android.application  → ApplicationExtension
            //   com.android.library      → LibraryExtension
            // Using pluginManager.withPlugin() is the correct lazy approach so this plugin
            // is reusable across both application and library modules.
            pluginManager.withPlugin("com.android.application") {
                extensions.configure<ApplicationExtension> {
                    buildFeatures { compose = true }
                }
            }
            pluginManager.withPlugin("com.android.library") {
                extensions.configure<LibraryExtension> {
                    buildFeatures { compose = true }
                }
            }

            // Compose BOM + debug tooling — shared by every Compose module.
            // Individual Compose artifact versions are governed by the BOM, so modules
            // never need to pin them explicitly.
            dependencies {
                val bom = libs.findLibrary("androidx-compose-bom").get()
                add("implementation", platform(bom))
                add("androidTestImplementation", platform(bom))
                add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
                add("debugImplementation", libs.findLibrary("androidx-compose-ui-test-manifest").get())
            }
        }
    }
}

import com.android.build.api.dsl.LibraryExtension
import com.awan.app.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // Compose library foundation: com.android.library + kotlin.android + configureKotlinAndroid()
            pluginManager.apply("awan.android.library")
            // Compose compiler + buildFeatures.compose + BOM (both impl and androidTest)
            pluginManager.apply("awan.android.compose")

            extensions.configure<LibraryExtension> {
                // targetSdk in LibraryExtension.defaultConfig is deprecated in AGP 8.x.
                // Library modules don't ship an APK so targetSdk only affects lint.
                lint {
                    targetSdk = 37
                }
            }

            dependencies {
                // Core Android dependencies every feature UI module needs.
                // Compose artifacts (ui, material3, graphics, tooling-preview) are intentionally
                // NOT pinned here — they're resolved transitively via the BOM added by awan.android.compose.
                add("implementation", libs.findLibrary("androidx-core-ktx").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-ktx").get())
                add("implementation", libs.findLibrary("androidx-activity-compose").get())

                // Test dependencies
                add("testImplementation", libs.findLibrary("junit").get())
                add("androidTestImplementation", libs.findLibrary("androidx-junit").get())
                add("androidTestImplementation", libs.findLibrary("androidx-espresso-core").get())
            }
        }
    }
}

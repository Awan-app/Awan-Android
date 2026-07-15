import com.android.build.api.dsl.LibraryExtension
import com.awan.app.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("awan.android.library")
            pluginManager.apply("awan.android.compose")

            extensions.configure<LibraryExtension> {
                // targetSdk in LibraryExtension.defaultConfig is deprecated in AGP 8.x.
                // For library modules, set it via lint instead.
                lint {
                    targetSdk = 35
                }
            }

            dependencies {
                // Core dependencies required by every feature module
                add("implementation", libs.findLibrary("androidx-core-ktx").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-ktx").get())
                add("implementation", libs.findLibrary("androidx-activity-compose").get())
                add("implementation", libs.findLibrary("androidx-compose-material3").get())
                add("implementation", libs.findLibrary("androidx-compose-ui").get())
                add("implementation", libs.findLibrary("androidx-compose-ui-graphics").get())
                add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())

                // Common testing dependencies
                add("testImplementation", libs.findLibrary("junit").get())
                add("androidTestImplementation", platform(libs.findLibrary("androidx-compose-bom").get()))
                add("androidTestImplementation", libs.findLibrary("androidx-junit").get())
                add("androidTestImplementation", libs.findLibrary("androidx-espresso-core").get())
            }
        }
    }
}

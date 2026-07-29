import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/**
 * Convention plugin for navigation modules (such as :core:navigation).
 * Applies Android Library, Compose, and Kotlin Serialization plugins,
 * and configures Navigation 3 and serialization dependencies.
 */
abstract class AndroidNavigationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("awan.android.library")
                apply("awan.android.compose")
                apply("org.jetbrains.kotlin.plugin.serialization")
            }

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            dependencies {
                add("api", libs.findLibrary("androidx-navigation3-runtime").get())
                add("api", libs.findLibrary("kotlinx-serialization-core").get())
                add("implementation", libs.findLibrary("androidx-compose-material-icons-core").get())
            }
        }
    }
}

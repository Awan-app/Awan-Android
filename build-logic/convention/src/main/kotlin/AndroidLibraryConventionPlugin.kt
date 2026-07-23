import com.android.build.api.dsl.LibraryExtension
import com.apps.awan.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

abstract class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                defaultConfig {
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    // Consumer proguard files
                    if (file("consumer-rules.pro").exists()) {
                        consumerProguardFiles("consumer-rules.pro")
                    }
                }
            }
        }
    }
}


import com.android.build.api.dsl.LibraryExtension
import extensions.configureKotlin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class LibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                configureKotlin(this)
                testOptions.targetSdk = EyesonConstants.TARGET_SDK
                lint.targetSdk = EyesonConstants.TARGET_SDK
            }
        }
    }
}
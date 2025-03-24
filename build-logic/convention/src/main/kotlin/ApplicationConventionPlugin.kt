import com.android.build.api.dsl.ApplicationExtension
import extensions.configureKotlin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class ApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<ApplicationExtension> {
                configureKotlin(this)
                defaultConfig.targetSdk = EyesonConstants.TARGET_SDK
            }
        }
    }
}
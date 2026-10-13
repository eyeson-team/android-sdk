import com.android.build.api.dsl.ApplicationExtension
import extensions.configureKotlin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class ApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")

            extensions.configure<ApplicationExtension> {
                configureKotlin(this)
                defaultConfig.targetSdk = EyesonConstants.TARGET_SDK
            }
        }
    }
}
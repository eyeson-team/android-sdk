import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.eyeson.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
}


gradlePlugin {
    plugins {
        register("applicationConventionPlugin") {
            id = libs.plugins.eyeson.application.get().pluginId
            implementationClass = "ApplicationConventionPlugin"
        }
        register("libraryConventionPlugin") {
            id = libs.plugins.eyeson.library.get().pluginId
            implementationClass = "LibraryConventionPlugin"
        }
    }
}

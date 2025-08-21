plugins {
    alias(libs.plugins.eyeson.library)
    alias(libs.plugins.ksp)
    id("maven-publish")
}

if (project.file("gstreamer-configurations.gradle").exists()) {
    apply(from = "gstreamer-configurations.gradle")
}


android {
    namespace = "com.eyeson.sdk.gstreamercapturer"

    defaultConfig {
        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        externalNativeBuild {
            cmake {
                val gstRoot: String? = if (project.hasProperty("gstAndroidRoot")) {
                    project.property("gstAndroidRoot") as String
                } else {
                    System.getenv("GSTREAMER_ROOT_ANDROID")
                }

                if (gstRoot == null) {
                    throw GradleException("GSTREAMER_ROOT_ANDROID must be set, or \"gstAndroidRoot\" must be defined in your gradle.properties in the top level directory of the unpacked universal GStreamer Android binaries")
                }

                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DGSTREAMER_ROOT_ANDROID=$gstRoot",
                    "-GNinja"
                )

                targets += listOf("native-lib")

                abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            }
        }
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

    flavorDimensions += "version"

    productFlavors {
        create("production") {
            dimension = "version"
        }
    }

    externalNativeBuild {
        cmake {
            path = file("jni/CMakeLists.txt")
        }
    }
    ndkVersion = "25.2.9519653"

    buildFeatures {
        buildConfig = true
        aidl = true
    }

}

if (project.file("flavor-configurations.gradle").exists()) {
    apply(from = "flavor-configurations.gradle")
}

dependencies {

    implementation(project(":sdk"))
    implementation(libs.eyeson.webrtc.android)

    testImplementation(libs.jUnit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
plugins {
    alias(libs.plugins.eyeson.library)
    alias(libs.plugins.ksp)
    id("maven-publish")
}

group = EyesonConstants.GROUP_ID
version = EyesonConstants.VERSION


if (project.file("gstreamer-configurations.gradle").exists()) {
    apply(from = "gstreamer-configurations.gradle")
}


android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField("String", "SDK_VERSION", "\"${version}\"")

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
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }

    flavorDimensions += "version"

    productFlavors {
        create("production") {
            dimension = "version"
            buildConfigField("String", "API_URL", "\"https://api.eyeson.team/\"")
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
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

    namespace = "com.eyeson.sdk"
}

if (project.file("flavor-configurations.gradle").exists()) {
    apply(from = "flavor-configurations.gradle")
}

dependencies {
    api(libs.eyeson.webrtc.android)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.okhttp)
    implementation(libs.okhttp.tls)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi)
    implementation(libs.moshi.adapters)
    ksp(libs.moshi.kotlin.codegen)

    testImplementation(libs.jUnit)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("sdk") {
                from(components["productionRelease"])
                groupId = EyesonConstants.GROUP_ID
                artifactId = "sdk"
                version = EyesonConstants.VERSION
            }
        }
    }
}
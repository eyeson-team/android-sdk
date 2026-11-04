plugins {
    alias(libs.plugins.eyeson.library)
    alias(libs.plugins.kotlin.serialization)
    id("maven-publish")
}

group = EyesonConstants.GROUP_ID
version = EyesonConstants.VERSION

kotlin {
    // Published library: every public declaration must state its visibility and return type.
    explicitApi()
}

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField("String", "SDK_VERSION", "\"${version}\"")
    }

    buildTypes {
        debug {
            isDefault = true
        }
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
            isDefault = true
            buildConfigField("String", "API_URL", "\"https://api.eyeson.team/\"")
        }
    }

    publishing {
        singleVariant("productionRelease") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    buildFeatures {
        buildConfig = true
    }

    testOptions {
        // Robolectric needs the merged manifest and resources to build its Application.
        unitTests.isIncludeAndroidResources = true
    }

    namespace = "com.eyeson.sdk"
}

if (project.file("flavor-configurations.gradle").exists()) {
    apply(from = "flavor-configurations.gradle")
}

dependencies {
    api(libs.eyeson.webrtc.android)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.annotation)
    implementation(libs.okhttp)
    implementation(libs.okhttp.tls)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.resources)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.jUnit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.okhttp.mockwebserver3)
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
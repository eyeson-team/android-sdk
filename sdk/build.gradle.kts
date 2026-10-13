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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.annotation)
    implementation(libs.okhttp)
    implementation(libs.okhttp.tls)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.jUnit)
    testImplementation(libs.robolectric)
    testImplementation(libs.okhttp.mockwebserver)
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
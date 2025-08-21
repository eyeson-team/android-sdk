plugins {
    alias(libs.plugins.eyeson.application)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose)
    alias(libs.plugins.kapt)
    alias(libs.plugins.ksp)
}

android {

    defaultConfig {
        applicationId = "com.eyeson.android"
        versionCode = EyesonConstants.DEMO_APP_VERSION
        versionName = EyesonConstants.VERSION

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
            resValue("string", "app_name", "Eyeson SDK")
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        compose = true
        buildConfig = true
    }

    kapt {
        correctErrorTypes = true
        generateStubs = true
    }

    namespace = "com.eyeson.android"
}

if (project.file("flavor-configurations.gradle").exists()) {
    apply(from = "flavor-configurations.gradle")
}

dependencies {
    implementation(project(":sdk"))
    implementation(project(":gstreamercapture"))

    coreLibraryDesugaring(libs.android.desugarJdkLibsNio)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.timber)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintLayout)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.fragment)
    implementation(libs.android.material)

    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.lifecycle.runtime.ktx)


    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.accompanist.systemUiController)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.qrScanner)
    implementation(libs.okhttp)
    implementation(libs.okhttp.tls)
    implementation(libs.okhttp.tls)
    implementation(libs.okhttp.logging)
    implementation(libs.coil.compose)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    testImplementation(libs.jUnit)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
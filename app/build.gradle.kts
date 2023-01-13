plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id(Plugins.hilt)
}

android {
    compileSdk = Versions.compileSdk

    defaultConfig {
        applicationId = "com.eyeson.android"
        minSdk = Versions.minSdk
        targetSdk = Versions.targetSdk
        versionCode = Versions.versionCode
        versionName = Versions.versionName

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
            resValue("string", "app_name", "eyeson SDK")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Versions.composeCompiler
    }

    kapt {
        correctErrorTypes = true
    }

    namespace = "com.eyeson.android"
}

if (project.file("flavor-configurations.gradle").exists()) {
    apply(from = "flavor-configurations.gradle")
}

dependencies {
    implementation(project(":sdk"))

    implementation(Libraries.hiltAndroid)
    kapt(LibrariesKapt.hiltAndroidCompiler)
    implementation(Libraries.hiltNavigationCompose)

    implementation(Libraries.timber)
    implementation(Libraries.coreKtx)
    implementation(Libraries.appcompat)
    implementation(Libraries.material)
    implementation(Libraries.constraintLayout)
    implementation(Libraries.lifecycleViewModelKtx)
    implementation(Libraries.lifecycleRuntimeKtx)
    implementation(Libraries.fragmentKtx)

    val composeBom = platform(Libraries.androidxComposeBom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(Libraries.androidxComposeMaterial)
    implementation(Libraries.androidxComposeUiToolingPreview)
    debugImplementation(Libraries.androidxComposeDebugUiTooling)
    implementation(Libraries.androidxActivityCompose)
    implementation(Libraries.androidxLifecycleViewModelCompose)
    implementation(Libraries.androidxNavigationCompose)
    implementation(Libraries.accompanistSystemUiController)
    implementation(Libraries.constraintLayoutCompose)

    implementation(Libraries.legacySupportV4)
    implementation(Libraries.recyclerview)
    implementation(Libraries.annotation)

    implementation(Libraries.datastorePreference)

    implementation(Libraries.qrScanner)

    testImplementation(Libraries.jUnit)
    androidTestImplementation(Libraries.jUnitTest)
    androidTestImplementation(Libraries.espressoCore)
}
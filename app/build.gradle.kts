plugins {
    id("com.android.application")
    id("kotlin-android")
    kotlin("kapt")
    id(Plugins.hilt)
    id(Plugins.compose)
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
            resValue("string", "app_name", "Eyeson SDK")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Versions.composeCompiler
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

    coreLibraryDesugaring(Libraries.desugarJdkLibsNio)

    implementation(Libraries.hiltAndroid)
    kapt(LibrariesKapt.hiltAndroidCompiler)
    implementation(Libraries.hiltNavigationCompose)

    implementation(Libraries.timber)
    implementation(Libraries.coreKtx)
    implementation(Libraries.appcompat)
//    implementation(Libraries.material)
    implementation(Libraries.constraintLayout)
    implementation(Libraries.lifecycleViewModelKtx)
    implementation(Libraries.lifecycleViewModelCompose)
    implementation(Libraries.lifecycleRuntimeCompose)
    implementation(Libraries.lifecycleRuntimeKtx)
    implementation(Libraries.fragmentKtx)
    implementation(Libraries.googleAndroidMaterial)

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
    implementation(Libraries.accompanistPermissions)
    implementation(Libraries.constraintLayoutCompose)

    implementation(Libraries.legacySupportV4)
    implementation(Libraries.recyclerview)
    implementation(Libraries.annotation)
    implementation(Libraries.datastorePreference)

    implementation(Libraries.qrScanner)
    implementation(Libraries.okhttp)
    implementation(Libraries.okhttpTls)
    implementation(Libraries.okhttpLoggingInterceptor)
    implementation(Libraries.coil)
    implementation(Libraries.coilCompose)

    implementation(Libraries.media3Exoplayer)
    implementation(Libraries.media3Ui)

    testImplementation(Libraries.jUnit)
    androidTestImplementation(Libraries.jUnitTest)
    androidTestImplementation(Libraries.espressoCore)
}
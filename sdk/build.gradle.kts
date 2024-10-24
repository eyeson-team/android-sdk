plugins {
    id("com.android.library")
    id("kotlin-android")
    id("maven-publish")
    id(Plugins.ksp)
}

group = Versions.groupId
version = Versions.versionName

android {
    compileSdk = Versions.compileSdk

    defaultConfig {
        minSdk = Versions.minSdk

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField("String", "SDK_VERSION", "\"${Versions.versionName}\"")
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
            buildConfigField("String", "API_URL", "\"https://api.eyeson.team/\"")
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }

    namespace = "com.eyeson.sdk"
}

if (project.file("flavor-configurations.gradle").exists()) {
    apply(from = "flavor-configurations.gradle")
}

dependencies {
    api(Libraries.webrtcAndroid)

    implementation(Libraries.coreKtx)
    implementation(Libraries.appcompat)

    implementation(Libraries.okhttp)
    implementation(Libraries.okhttpTls)
    implementation(Libraries.retrofit)
    implementation(Libraries.okhttpLoggingInterceptor)
    implementation(Libraries.retrofitMoshiConverter)
    implementation(Libraries.moshi)
    implementation(Libraries.moshiAdapters)
    ksp(LibrariesKapt.moshiKotlinCodegen)

    testImplementation(Libraries.jUnit)
    androidTestImplementation(Libraries.jUnitTest)
    androidTestImplementation(Libraries.espressoCore)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("sdk") {
                from(components["productionRelease"])
                groupId = Versions.groupId
                artifactId = "sdk"
                version = Versions.versionName
            }
        }
    }
}
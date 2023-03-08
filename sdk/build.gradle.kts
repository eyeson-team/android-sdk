plugins {
    id("com.android.library")
    id("kotlin-android")
    kotlin("kapt")
    id("maven-publish")
    id("com.kezong.fat-aar")
}

group = Versions.groupId
version = Versions.versionName

android {
    compileSdk = Versions.compileSdk

    defaultConfig {
        minSdk = Versions.minSdk
        targetSdk = Versions.targetSdk

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    namespace = "com.eyeson.sdk"
}

if (project.file("flavor-configurations.gradle").exists()) {
    apply(from = "flavor-configurations.gradle")
}

dependencies {
    releaseEmbed(project(":webrtc", configuration = "default"))
    debugApi(project(":webrtc", configuration = "default"))

    implementation(Libraries.coreKtx)
    implementation(Libraries.appcompat)

    implementation(Libraries.okhttp)
    implementation(Libraries.okhttpLoggingInterceptor)
    implementation(Libraries.retrofit)
    implementation(Libraries.retrofitMoshiConverter)
    implementation(Libraries.moshi)
    implementation(Libraries.moshiAdapters)
    kapt(LibrariesKapt.moshiKotlinCodegen)

    testImplementation(Libraries.jUnit)
    androidTestImplementation(Libraries.jUnitTest)
    androidTestImplementation(Libraries.espressoCore)
}

fataar {
    transitive = true
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
object Libraries {

    // DI
    const val hiltAndroid = "com.google.dagger:hilt-android:${Versions.hilt}"
    const val hiltNavigationCompose =
        "androidx.hilt:hilt-navigation-compose:${Versions.hiltNavigationCompose}"

    // UI and appcompat
    const val coreKtx = "androidx.core:core-ktx:${Versions.coreKtx}"
    const val appcompat = "androidx.appcompat:appcompat:${Versions.appCompat}"
    const val material = "com.google.android.material:material:${Versions.material}"
    const val fragmentKtx = "androidx.fragment:fragment-ktx:${Versions.fragmentKtx}"

    const val constraintLayout =
        "androidx.constraintlayout:constraintlayout:${Versions.constraintLayout}"
    const val lifecycleViewModelKtx =
        "androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.lifecycleViewModelKtx}"
    const val lifecycleViewModelCompose =
        "androidx.lifecycle:lifecycle-viewmodel-compose:${Versions.lifecycleViewModelKtx}"
    const val lifecycleRuntimeCompose =
        "androidx.lifecycle:lifecycle-runtime-compose:${Versions.lifecycleViewModelKtx}"


    const val lifecycleRuntimeKtx =
        "androidx.lifecycle:lifecycle-runtime-ktx:${Versions.lifecycleRuntimeKtx}"
    const val legacySupportV4 = "androidx.legacy:legacy-support-v4:${Versions.legacySupportV4}"
    const val recyclerview = "androidx.recyclerview:recyclerview:${Versions.recyclerview}"
    const val qrScanner = "com.github.yuriy-budiyev:code-scanner:${Versions.qrScanner}"

    // Compose
    const val androidxComposeBom = "androidx.compose:compose-bom:${Versions.androidxComposeBom}"
    const val androidxComposeMaterial = Versions.androidxComposeMaterial
    const val androidxComposeUiToolingPreview = Versions.androidxComposeUiToolingPreview
    const val androidxComposeDebugUiTooling = Versions.androidxComposeDebugUiTooling
    const val androidxActivityCompose =
        "androidx.activity:activity-compose:${Versions.androidxActivityCompose}"
    const val androidxLifecycleViewModelCompose =
        "androidx.lifecycle:lifecycle-viewmodel-compose:${Versions.androidxLifecycleViewModelCompose}"
    const val androidxNavigationCompose =
        "androidx.navigation:navigation-compose:${Versions.androidxNavigationCompose}"

    const val accompanistSystemUiController =
        "com.google.accompanist:accompanist-systemuicontroller:${Versions.accompanist}"

    const val accompanistPermissions =
        "com.google.accompanist:accompanist-permissions:${Versions.accompanist}"

    const val constraintLayoutCompose =
        "androidx.constraintlayout:constraintlayout-compose:${Versions.constraintLayoutCompose}"

    // Annotation
    const val annotation = "androidx.annotation:annotation:${Versions.annotation}"

    // Storage
    const val datastorePreference =
        "androidx.datastore:datastore-preferences:${Versions.datastorePreference}"

    // Networking
    const val okhttp = "com.squareup.okhttp3:okhttp:${Versions.okhttp}"
    const val okhttpLoggingInterceptor =
        "com.squareup.okhttp3:logging-interceptor:${Versions.okhttp}"
    const val retrofit = "com.squareup.retrofit2:retrofit:${Versions.retrofit}"
    const val retrofitMoshiConverter = "com.squareup.retrofit2:converter-moshi:${Versions.retrofit}"
    const val moshi = "com.squareup.moshi:moshi:${Versions.moshi}"
    const val moshiAdapters = "com.squareup.moshi:moshi-adapters:${Versions.moshi}"
    const val coil = "io.coil-kt:coil:${Versions.coil}"
    const val coilCompose = "io.coil-kt:coil-compose:${Versions.coil}"

    // Logging
    const val timber = "com.jakewharton.timber:timber:${Versions.timber}"

    // Testing
    const val jUnit = "junit:junit:${Versions.jUnit}"
    const val jUnitTest = "androidx.test.ext:junit:${Versions.jUnitTest}"
    const val espressoCore = "androidx.test.espresso:espresso-core:${Versions.espressoCore}"
}

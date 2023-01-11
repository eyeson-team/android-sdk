package com.eyeson.android

import android.app.Application
import com.eyeson.android.BuildConfig.DEBUG
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class EyesonDemoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initTimber()
    }

    private fun initTimber() {
        if (DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
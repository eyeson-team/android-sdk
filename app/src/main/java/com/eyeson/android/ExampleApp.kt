package com.eyeson.android

import android.app.Application
import com.eyeson.android.BuildConfig.DEBUG
import timber.log.Timber

class ExampleApp : Application() {
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
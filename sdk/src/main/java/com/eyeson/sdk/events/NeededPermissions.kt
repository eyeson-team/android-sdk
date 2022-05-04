package com.eyeson.sdk.events

import android.Manifest

enum class NeededPermissions(val permissions: String) {
    RECORD_AUDIO(Manifest.permission.RECORD_AUDIO),
    CAMERA(Manifest.permission.CAMERA);
}
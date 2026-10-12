package com.eyeson.sdk.events

import android.Manifest

public enum class NeededPermissions(public val permissions: String) {
    RECORD_AUDIO(Manifest.permission.RECORD_AUDIO),
    CAMERA(Manifest.permission.CAMERA);
}
package com.eyeson.sdk.model.local.sepp

import com.eyeson.sdk.model.local.base.LocalBaseCommand

internal data class RecordingStatusUpdate(
    val active: Boolean,
    val enabled: Boolean
) : LocalBaseCommand
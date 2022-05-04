package com.eyeson.sdk.model.local.datachannel

import com.eyeson.sdk.model.local.base.LocalBaseCommand

internal data class VoiceActivity(
    val userId: String,
    val on: Boolean
) : LocalBaseCommand
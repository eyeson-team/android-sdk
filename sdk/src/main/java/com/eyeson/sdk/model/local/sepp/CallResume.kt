package com.eyeson.sdk.model.local.sepp

import com.eyeson.sdk.model.local.base.LocalBaseCommand

internal data class CallResume(
    val callId: String,
    val sdp: String
) : LocalBaseCommand

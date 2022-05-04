package com.eyeson.sdk.model.local.sepp

import com.eyeson.sdk.model.local.base.LocalBaseCommand

internal data class CallAccepted(
    val callId: String,
    val sdp: String
) : LocalBaseCommand
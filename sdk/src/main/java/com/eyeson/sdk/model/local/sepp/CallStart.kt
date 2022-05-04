package com.eyeson.sdk.model.local.sepp

import com.eyeson.sdk.model.local.base.LocalBaseCommand

internal data class CallStart(
    val displayName: String,
    val sdp: String
) : LocalBaseCommand

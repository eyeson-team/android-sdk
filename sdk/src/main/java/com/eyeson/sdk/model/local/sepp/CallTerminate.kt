package com.eyeson.sdk.model.local.sepp

import com.eyeson.sdk.model.local.base.LocalBaseCommand

internal data class CallTerminate(
    val callId: String,
    val termCode: Int
) : LocalBaseCommand

package com.eyeson.sdk.model.local.sepp

import com.eyeson.sdk.model.local.base.LocalBaseCommand

internal data class CallTerminated(
    val terminateCode: Int
) : LocalBaseCommand

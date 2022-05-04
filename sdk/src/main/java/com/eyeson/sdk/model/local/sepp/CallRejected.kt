package com.eyeson.sdk.model.local.sepp

import com.eyeson.sdk.model.local.base.LocalBaseCommand

internal data class CallRejected(val rejectCode: Int) : LocalBaseCommand

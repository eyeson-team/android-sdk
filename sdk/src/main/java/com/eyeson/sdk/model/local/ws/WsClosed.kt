package com.eyeson.sdk.model.local.ws

import com.eyeson.sdk.model.local.base.LocalBaseCommand

internal data class WsClosed(val code: Int, val reason: String) : LocalBaseCommand
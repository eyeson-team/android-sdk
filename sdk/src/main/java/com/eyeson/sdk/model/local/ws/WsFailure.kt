package com.eyeson.sdk.model.local.ws

import com.eyeson.sdk.model.local.base.LocalBaseCommand
import okhttp3.Response

internal data class WsFailure(val response: Response?) : LocalBaseCommand
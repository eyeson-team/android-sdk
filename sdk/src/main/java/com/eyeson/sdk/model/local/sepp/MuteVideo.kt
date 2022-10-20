package com.eyeson.sdk.model.local.sepp

import com.eyeson.sdk.model.local.base.LocalBaseCommand

internal data class MuteVideo(
    val muted: Boolean,
    val cid: String
) : LocalBaseCommand
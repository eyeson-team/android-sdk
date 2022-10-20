package com.eyeson.sdk.model.local.sepp

import com.eyeson.sdk.model.local.base.LocalBaseCommand

internal data class ChatOutgoing(
    val content: String,
    val userId: String
) : LocalBaseCommand
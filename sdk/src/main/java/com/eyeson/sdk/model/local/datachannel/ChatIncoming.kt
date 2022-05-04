package com.eyeson.sdk.model.local.datachannel

import com.eyeson.sdk.model.local.base.LocalBaseCommand
import java.util.*

internal data class ChatIncoming(
    val userId: String,
    val content: String,
    val messageId: String,
    val timestamp: Date
) : LocalBaseCommand
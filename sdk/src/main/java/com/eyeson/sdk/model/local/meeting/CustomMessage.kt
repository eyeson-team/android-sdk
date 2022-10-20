package com.eyeson.sdk.model.local.meeting

import com.eyeson.sdk.model.local.base.LocalBaseCommand
import java.util.*

internal data class CustomMessage(
    val content: String,
    val createdAt: Date,
    val userId: String
) : LocalBaseCommand


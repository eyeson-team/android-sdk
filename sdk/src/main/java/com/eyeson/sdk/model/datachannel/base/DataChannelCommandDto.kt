package com.eyeson.sdk.model.datachannel.base

import com.eyeson.sdk.model.local.base.LocalBaseCommand

internal interface DataChannelCommandDto {
    val type: String

    fun toLocal(): LocalBaseCommand
}
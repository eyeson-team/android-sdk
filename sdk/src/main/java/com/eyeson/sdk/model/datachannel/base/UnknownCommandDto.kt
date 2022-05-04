package com.eyeson.sdk.model.datachannel.base

import com.eyeson.sdk.model.local.base.UnknownCommand
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal class UnknownCommandDto(
    override val type: String = "Unknown command"
) : DataChannelCommandDto {

    override fun toLocal(): UnknownCommand {
        return UnknownCommand()
    }
}
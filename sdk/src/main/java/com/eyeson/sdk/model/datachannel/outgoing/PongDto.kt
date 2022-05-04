package com.eyeson.sdk.model.datachannel.outgoing

import com.eyeson.sdk.model.datachannel.base.DataChannelCommandDto
import com.eyeson.sdk.model.local.base.UnknownCommand
import com.eyeson.sdk.model.local.datachannel.Pong
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class PongDto(
    @Json(name = "type") override val type: String
) : DataChannelCommandDto {
    override fun toLocal(): UnknownCommand {
        return UnknownCommand()
    }
}

internal fun Pong.fromLocal(): PongDto {
    return PongDto(type = "pong")
}
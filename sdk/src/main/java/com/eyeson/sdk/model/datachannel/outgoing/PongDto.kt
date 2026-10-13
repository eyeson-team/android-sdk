package com.eyeson.sdk.model.datachannel.outgoing

import com.eyeson.sdk.model.datachannel.base.DataChannelCommandDto
import com.eyeson.sdk.model.local.base.UnknownCommand
import com.eyeson.sdk.model.local.datachannel.Pong
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PongDto(
    @SerialName("type") override val type: String
) : DataChannelCommandDto {
    override fun toLocal(): UnknownCommand {
        return UnknownCommand()
    }
}

internal fun Pong.fromLocal(): PongDto {
    return PongDto(type = "pong")
}
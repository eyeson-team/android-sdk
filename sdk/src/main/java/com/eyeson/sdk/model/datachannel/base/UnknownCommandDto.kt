package com.eyeson.sdk.model.datachannel.base

import com.eyeson.sdk.model.local.base.UnknownCommand
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class UnknownCommandDto(
    @SerialName("type") override val type: String = "Unknown command"
) : DataChannelCommandDto {

    override fun toLocal(): UnknownCommand {
        return UnknownCommand()
    }
}
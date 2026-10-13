package com.eyeson.sdk.model.sepp.base

import com.eyeson.sdk.model.local.base.UnknownCommand
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class UnknownCommandDto(
    @SerialName("type") override val type: String = "Unknown command"
) : SeppBaseCommandDto {

    override fun toLocal(): UnknownCommand {
        return UnknownCommand()
    }
}
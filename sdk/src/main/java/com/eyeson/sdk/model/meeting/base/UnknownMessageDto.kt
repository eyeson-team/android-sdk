package com.eyeson.sdk.model.meeting.base

import com.eyeson.sdk.model.local.base.UnknownCommand
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class UnknownMessageDto(
    @SerialName("type") override val type: String = "Unknown command"
) : MeetingBaseMessageDto {
    override fun toLocal(): UnknownCommand {
        return UnknownCommand()
    }
}
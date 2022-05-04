package com.eyeson.sdk.model.meeting.base

import com.eyeson.sdk.model.local.base.UnknownCommand
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal class UnknownMessageDto(
    override val type: String = "Unknown command"
) : MeetingBaseMessageDto {
    override fun toLocal(): UnknownCommand {
        return UnknownCommand()
    }
}
package com.eyeson.sdk.model.meeting.incoming

import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.local.meeting.RoomSetup
import com.eyeson.sdk.model.meeting.base.MeetingBaseMessageDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RoomSetupDto(
    @SerialName("type") override val type: String,
    @SerialName("content") val meeting: MeetingDto
) : MeetingBaseMessageDto {

    override fun toLocal(): RoomSetup {
        return RoomSetup(meeting)
    }
}
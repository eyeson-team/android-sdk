package com.eyeson.sdk.model.meeting.incoming

import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.local.meeting.RoomSetup
import com.eyeson.sdk.model.meeting.base.MeetingBaseMessageDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class RoomSetupDto(
    @Json(name = "type") override val type: String,
    @Json(name = "content") val meeting: MeetingDto
) : MeetingBaseMessageDto {

    override fun toLocal(): RoomSetup {
        return RoomSetup(meeting)
    }
}
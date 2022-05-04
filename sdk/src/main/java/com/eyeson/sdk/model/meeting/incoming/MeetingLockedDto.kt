package com.eyeson.sdk.model.meeting.incoming

import com.eyeson.sdk.model.local.meeting.MeetingLocked
import com.eyeson.sdk.model.meeting.base.MeetingBaseMessageDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class MeetingLockedDto(
    @Json(name = "type") override val type: String,
    @Json(name = "locked") val locked: Boolean
) : MeetingBaseMessageDto {
    override fun toLocal(): MeetingLocked {
        return MeetingLocked(locked = locked)
    }
}
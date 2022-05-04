package com.eyeson.sdk.model.meeting.incoming

import com.eyeson.sdk.model.api.RecordingDto
import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.meeting.Recording
import com.eyeson.sdk.model.meeting.base.MeetingBaseMessageDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
internal data class RecordingUpdateDto(
    @Json(name = "type") override val type: String,
    @Json(name = "recording") val recording: RecordingDto
) : MeetingBaseMessageDto {
    override fun toLocal(): Recording {
        return Recording(
            id = recording.id,
            duration = recording.duration,
            downloadLink = recording.links.download,
            createdAt = Date(recording.createdAt),
            user = UserInfo(
                id = recording.user.id,
                name = recording.user.name,
                avatar = recording.user.avatar,
                guest = recording.user.guest,
                joinedAt = recording.user.joinedAt
            )
        )
    }
}
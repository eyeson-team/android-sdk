package com.eyeson.sdk.model.meeting.incoming

import com.eyeson.sdk.model.api.RecordingDto
import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.meeting.RecordingLocal
import com.eyeson.sdk.model.meeting.base.MeetingBaseMessageDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
internal data class RecordingUpdateDto(
    @SerialName("type") override val type: String,
    @SerialName("recording") val recording: RecordingDto
) : MeetingBaseMessageDto {
    override fun toLocal(): RecordingLocal {
        return RecordingLocal(
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
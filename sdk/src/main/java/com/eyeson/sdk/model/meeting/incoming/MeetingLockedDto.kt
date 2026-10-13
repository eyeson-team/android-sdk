package com.eyeson.sdk.model.meeting.incoming

import com.eyeson.sdk.model.local.meeting.MeetingLockedLocal
import com.eyeson.sdk.model.meeting.base.MeetingBaseMessageDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MeetingLockedDto(
    @SerialName("type") override val type: String,
    @SerialName("locked") val locked: Boolean
) : MeetingBaseMessageDto {
    override fun toLocal(): MeetingLockedLocal {
        return MeetingLockedLocal(locked = locked)
    }
}
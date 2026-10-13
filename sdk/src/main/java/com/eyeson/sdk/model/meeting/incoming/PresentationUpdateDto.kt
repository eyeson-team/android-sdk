package com.eyeson.sdk.model.meeting.incoming


import com.eyeson.sdk.model.api.PresentationDto
import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.meeting.PresentationUpdateLocal
import com.eyeson.sdk.model.meeting.base.MeetingBaseMessageDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PresentationUpdateDto(
    @SerialName("type") override val type: String,
    @SerialName("presentation") val presentation: PresentationDto?
) : MeetingBaseMessageDto {
    override fun toLocal(): PresentationUpdateLocal {
        return PresentationUpdateLocal(
            user = presentation?.user?.let { user ->
                UserInfo(
                    id = user.id,
                    name = user.name,
                    avatar = user.avatar,
                    guest = user.guest,
                    joinedAt = user.joinedAt
                )
            }
        )
    }
}
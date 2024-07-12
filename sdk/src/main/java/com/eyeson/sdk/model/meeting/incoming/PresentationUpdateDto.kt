package com.eyeson.sdk.model.meeting.incoming


import com.eyeson.sdk.model.api.PresentationDto
import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.meeting.PresentationUpdate
import com.eyeson.sdk.model.meeting.base.MeetingBaseMessageDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PresentationUpdateDto(
    @Json(name = "type") override val type: String,
    @Json(name = "presentation") val presentation: PresentationDto?
) : MeetingBaseMessageDto {
    override fun toLocal(): PresentationUpdate {
        return PresentationUpdate(
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
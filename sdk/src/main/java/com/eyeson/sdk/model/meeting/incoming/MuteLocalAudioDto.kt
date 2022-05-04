package com.eyeson.sdk.model.meeting.incoming

import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.meeting.MuteLocalAudio
import com.eyeson.sdk.model.meeting.base.MeetingBaseMessageDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
internal data class MuteLocalAudioDto(
    @Json(name = "type") override val type: String,
    @Json(name = "user") val user: User
) : MeetingBaseMessageDto {
    @JsonClass(generateAdapter = true)
    data class User(
        @Json(name = "id") val id: String,
        @Json(name = "name") val name: String,
        @Json(name = "avatar") val avatar: String?,
        @Json(name = "guest") val guest: Boolean,
        @Json(name = "joined_at") val joinedAt: Date
    )

    override fun toLocal(): MuteLocalAudio {
        return MuteLocalAudio(
            UserInfo(
                id = user.id,
                name = user.name,
                avatar = user.avatar,
                guest = user.guest,
                joinedAt = user.joinedAt
            )
        )
    }
}
package com.eyeson.sdk.model.meeting.incoming

import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.meeting.MuteLocalAudioLocal
import com.eyeson.sdk.model.meeting.base.MeetingBaseMessageDto
import com.eyeson.sdk.serializer.DateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
internal data class MuteLocalAudioDto(
    @SerialName("type") override val type: String,
    @SerialName("user") val user: User
) : MeetingBaseMessageDto {
    @Serializable
    data class User(
        @SerialName("id") val id: String,
        @SerialName("name") val name: String,
        @SerialName("avatar") val avatar: String?,
        @SerialName("guest") val guest: Boolean,
        @Serializable(with = DateSerializer::class)
        @SerialName("joined_at") val joinedAt: Date
    )

    override fun toLocal(): MuteLocalAudioLocal {
        return MuteLocalAudioLocal(
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
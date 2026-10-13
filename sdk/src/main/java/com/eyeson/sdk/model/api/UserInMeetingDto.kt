package com.eyeson.sdk.model.api


import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.serializer.DateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
internal data class UserInMeetingDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("avatar") val avatar: String?,
    @SerialName("guest") val guest: Boolean,
    @Serializable(with = DateSerializer::class)
    @SerialName("joined_at") val joinedAt: Date
)

internal fun UserInMeetingDto.toLocal(): UserInfo {
    return UserInfo(
        id = id,
        name = name,
        avatar = avatar,
        guest = guest,
        joinedAt = joinedAt
    )
}
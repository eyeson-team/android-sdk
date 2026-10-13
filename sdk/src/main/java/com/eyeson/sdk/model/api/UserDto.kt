package com.eyeson.sdk.model.api

import com.eyeson.sdk.model.local.api.UserInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
internal data class UserDto(
    @SerialName("id") val id: String,
    @SerialName("room_id") val roomId: String,
    @SerialName("name") val name: String,
    @SerialName("avatar") val avatar: String?,
    @SerialName("guest") val guest: Boolean,
    @SerialName("blocked") val blocked: Boolean,
    @SerialName("ready") val ready: Boolean
) {
    fun toLocal(joinedAt: Date): UserInfo {
        return UserInfo(
            id = id,
            name = name,
            avatar = avatar,
            guest = guest,
            joinedAt = joinedAt
        )
    }
}

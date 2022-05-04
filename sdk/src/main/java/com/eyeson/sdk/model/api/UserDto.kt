package com.eyeson.sdk.model.api

import com.eyeson.sdk.model.local.api.UserInfo
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
internal data class UserDto(
    @Json(name = "id") val id: String,
    @Json(name = "room_id") val roomId: String,
    @Json(name = "name") val name: String,
    @Json(name = "avatar") val avatar: String?,
    @Json(name = "guest") val guest: Boolean,
    @Json(name = "blocked") val blocked: Boolean,
    @Json(name = "ready") val ready: Boolean
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

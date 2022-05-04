package com.eyeson.sdk.model.api


import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.meeting.Recording
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
internal data class RecordingDto(
    @Json(name = "id") val id: String,
    @Json(name = "created_at") val createdAt: Long,
    @Json(name = "duration") val duration: Int?,
    @Json(name = "links") val links: Links,
    @Json(name = "user") val user: User
) {
    @JsonClass(generateAdapter = true)
    data class Links(
        @Json(name = "self") val self: String,
        @Json(name = "download") val download: String?
    )

    @JsonClass(generateAdapter = true)
    data class User(
        @Json(name = "id") val id: String,
        @Json(name = "name") val name: String,
        @Json(name = "avatar") val avatar: String?,
        @Json(name = "guest") val guest: Boolean,
        @Json(name = "joined_at") val joinedAt: Date
    )

    fun toLocal(): Recording {
        return Recording(
            id = id,
            duration = duration,
            downloadLink = links.download,
            createdAt = Date(createdAt),
            user = UserInfo(
                id = user.id,
                name = user.name,
                avatar = user.avatar,
                guest = user.guest,
                joinedAt = user.joinedAt
            )
        )
    }
}
package com.eyeson.sdk.model.api


import com.eyeson.sdk.model.local.api.Recording
import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.serializer.DateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*
import java.util.concurrent.TimeUnit

@Serializable
internal data class RecordingDto(
    @SerialName("id") val id: String,
    /** Epoch seconds, unlike the ISO 8601 strings the API uses elsewhere. */
    @SerialName("created_at") val createdAt: Long,
    @SerialName("duration") val duration: Int?,
    @SerialName("links") val links: Links,
    @SerialName("user") val user: User
) {
    @Serializable
    data class Links(
        @SerialName("self") val self: String,
        @SerialName("download") val download: String?
    )

    @Serializable
    data class User(
        @SerialName("id") val id: String,
        @SerialName("name") val name: String,
        @SerialName("avatar") val avatar: String?,
        @SerialName("guest") val guest: Boolean,
        @Serializable(with = DateSerializer::class)
        @SerialName("joined_at") val joinedAt: Date
    )

    fun toLocal(): Recording {
        return Recording(
            id = id,
            duration = duration,
            downloadLink = links.download,
            createdAt = Date(TimeUnit.SECONDS.toMillis(createdAt)),
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
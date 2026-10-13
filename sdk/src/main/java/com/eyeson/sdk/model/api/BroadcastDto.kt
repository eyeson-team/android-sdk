package com.eyeson.sdk.model.api


import com.eyeson.sdk.serializer.DateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
internal data class BroadcastDto(
    @SerialName("id") val id: String,
    @SerialName("platform") val platform: String,
    @SerialName("player_url") val playerUrl: String,
    @SerialName("user") val user: User
) {
    @Serializable
    data class User(
        @SerialName("id") val id: String,
        @SerialName("name") val name: String,
        @SerialName("avatar") val avatar: String?,
        @SerialName("guest") val guest: Boolean,
        @Serializable(with = DateSerializer::class)
        @SerialName("joined_at") val joinedAt: Date
    )
}
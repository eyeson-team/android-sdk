package com.eyeson.sdk.model.api

import com.eyeson.sdk.serializer.DateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
internal data class PresentationDto(
    @SerialName("room") val room: Room?,
    @SerialName("user") val user: User?
) {
    @Serializable
    data class Room(
        @SerialName("guest_token") val guestToken: String,
        @SerialName("id") val id: String,
        @SerialName("name") val name: String,
        @SerialName("ready") val ready: Boolean,
        @SerialName("shutdown") val shutdown: Boolean,
        @SerialName("started_at") val startedAt: String
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
}
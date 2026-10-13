package com.eyeson.sdk.model.api


import com.eyeson.sdk.model.local.api.PermalinkMeetingInfo
import com.eyeson.sdk.serializer.DateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
internal data class PermalinkDto(
    @SerialName("permalink")
    val permalink: Permalink,
    @SerialName("room")
    val room: Room,
    @SerialName("team")
    val team: Team,
    @SerialName("options")
    val options: MeetingOptionsDto,
) {
    @Serializable
    data class Permalink(
        @SerialName("id")
        val id: String,
        @SerialName("created_at")
        val createdAt: String,
        @SerialName("guest_token")
        val guestToken: String,
        @Serializable(with = DateSerializer::class)
        @SerialName("expires_at")
        val expiresAt: Date?,
    )

    @Serializable
    data class Team(
        @SerialName("name")
        val name: String,
    )

    @Serializable
    data class Room(
        @SerialName("id")
        val id: String,
        @SerialName("name")
        val name: String,
        @Serializable(with = DateSerializer::class)
        @SerialName("started_at")
        val startedAt: Date?,
    )

    fun toLocal() = PermalinkMeetingInfo(
        id = permalink.id,
        createdAt = permalink.createdAt,
        guestToken = permalink.guestToken,
        expiresAt = permalink.expiresAt,
        room = PermalinkMeetingInfo.Room(
            id = room.id,
            name = room.name,
            startedAt = room.startedAt
        ),
        team = PermalinkMeetingInfo.Team(name = team.name),
        options = options.toLocal()
    )
}
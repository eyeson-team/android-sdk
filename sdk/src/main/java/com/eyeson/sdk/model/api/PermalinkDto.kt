package com.eyeson.sdk.model.api


import com.eyeson.sdk.model.local.api.PermalinkMeetingInfo
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
internal data class PermalinkDto(
    @Json(name = "permalink")
    val permalink: Permalink,
    @Json(name = "room")
    val room: Room,
    @Json(name = "team")
    val team: Team,
    @Json(name = "options")
    val options: MeetingOptionsDto,
) {
    @JsonClass(generateAdapter = true)
    data class Permalink(
        @Json(name = "id")
        val id: String,
        @Json(name = "created_at")
        val createdAt: String,
        @Json(name = "guest_token")
        val guestToken: String,
        @Json(name = "expires_at")
        val expiresAt: Date?,
    )

    @JsonClass(generateAdapter = true)
    data class Team(
        @Json(name = "name")
        val name: String,
    )

    @JsonClass(generateAdapter = true)
    data class Room(
        @Json(name = "id")
        val id: String,
        @Json(name = "name")
        val name: String,
        @Json(name = "started_at")
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
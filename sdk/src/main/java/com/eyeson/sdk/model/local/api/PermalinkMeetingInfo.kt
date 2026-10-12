package com.eyeson.sdk.model.local.api

import java.util.Date

public data class PermalinkMeetingInfo(
    val id: String,
    val createdAt: String,
    val guestToken: String,
    val expiresAt: Date?,
    val room: Room,
    val team: Team,
    val options: MeetingOptions,
) {
    public data class Team(
        val name: String,
    )

    public data class Room(
        val id: String,
        val name: String,
        val startedAt: Date?,
    )
}
package com.eyeson.sdk.model.api


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
data class PermalinkDto(
    @Json(name = "permalink")
    val permalink: Permalink,
    @Json(name = "room")
    val room: Room,
    @Json(name = "options")
    val options: Options,
) {
    @JsonClass(generateAdapter = true)
    data class Options(
        @Json(name = "widescreen")
        val widescreen: Boolean,
    )

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
    data class Room(
        @Json(name = "id")
        val id: String,
        @Json(name = "name")
        val name: String,
        @Json(name = "started_at")
        val startedAt: Date?,
    )
}
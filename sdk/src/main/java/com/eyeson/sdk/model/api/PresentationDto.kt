package com.eyeson.sdk.model.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
internal data class PresentationDto(
    @Json(name = "room") val room: Room?,
    @Json(name = "user") val user: User?
) {
    @JsonClass(generateAdapter = true)
    data class Room(
        @Json(name = "guest_token") val guestToken: String,
        @Json(name = "id") val id: String,
        @Json(name = "name") val name: String,
        @Json(name = "ready") val ready: Boolean,
        @Json(name = "shutdown") val shutdown: Boolean,
        @Json(name = "started_at") val startedAt: String
    )

    @JsonClass(generateAdapter = true)
    data class User(
        @Json(name = "id") val id: String,
        @Json(name = "name") val name: String,
        @Json(name = "avatar") val avatar: String?,
        @Json(name = "guest") val guest: Boolean,
        @Json(name = "joined_at") val joinedAt: Date
    )
}
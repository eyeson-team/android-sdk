package com.eyeson.sdk.model.api


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
internal data class BroadcastDto(
    @Json(name = "id") val id: String,
    @Json(name = "platform") val platform: String,
    @Json(name = "player_url") val playerUrl: String,
    @Json(name = "user") val user: User
) {
    @JsonClass(generateAdapter = true)
    data class User(
        @Json(name = "id") val id: String,
        @Json(name = "name") val name: String,
        @Json(name = "avatar") val avatar: String?,
        @Json(name = "guest") val guest: Boolean,
        @Json(name = "joined_at") val joinedAt: Date
    )
}
package com.eyeson.sdk.model.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
internal data class RoomDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") var name: String,
    @Json(name = "ready") var ready: Boolean,
    @Json(name = "started_at") val startedAt: Date,
    @Json(name = "guest_token") var guestToken: String
)


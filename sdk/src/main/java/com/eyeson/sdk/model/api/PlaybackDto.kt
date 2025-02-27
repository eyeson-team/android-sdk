package com.eyeson.sdk.model.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class PlaybackDto(
    @Json(name = "url") val url: String,
    @Json(name = "name") val name: String?,
    @Json(name = "play_id") val playId: String?,
    @Json(name = "replacement_id") val replacementId: String?,
    @Json(name = "audio") val audio: Boolean?,
    @Json(name = "loop_count") val loopCount: Int?,
)
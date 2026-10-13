package com.eyeson.sdk.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PlaybackDto(
    @SerialName("url") val url: String,
    @SerialName("name") val name: String?,
    @SerialName("play_id") val playId: String?,
    @SerialName("replacement_id") val replacementId: String?,
    @SerialName("audio") val audio: Boolean?,
    @SerialName("loop_count") val loopCount: Int?,
)
package com.eyeson.sdk.model.meeting.outgoing

import com.eyeson.sdk.moshi.adapter.AsString
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class MuteAllDto(
    @Json(name = "command") val command: String = "message",
    @AsString
    @Json(name = "identifier") var identifier: SubscribeToChannel.Identifier =
        SubscribeToChannel.Identifier(SubscribeToChannel.ROOM_CHANNEL),
    @AsString
    @Json(name = "data") var data: Data = Data()
) {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "message") val message: String = STFU,
        @Json(name = "action") val action: String = STFU
    )

    companion object {
        private const val STFU = "stfu"
    }
}
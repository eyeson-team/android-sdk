package com.eyeson.sdk.model.meeting.base

import com.eyeson.sdk.moshi.adapter.AsString
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class MeetingBaseCommandDto(
    @Json(name = "type") val type: String?,
    @Json(name = "command") val command: String?,
    @AsString
    @Json(name = "identifier") var identifier: Identifier?,
    @Json(name = "message") val message: MeetingBaseMessageDto?,
) {
    @JsonClass(generateAdapter = true)
    data class Identifier(
        @Json(name = "channel") val channel: String?
    )
}
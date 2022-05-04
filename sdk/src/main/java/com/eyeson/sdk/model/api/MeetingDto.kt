package com.eyeson.sdk.model.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class MeetingDto(
    @Json(name = "access_key") var accessKey: String,
    @Json(name = "ready") var ready: Boolean,
    @Json(name = "locked") var locked: Boolean,
    @Json(name = "room") var room: RoomDto,
    @Json(name = "user") var user: UserDto,
    @Json(name = "links") var links: Links,
    @Json(name = "recording") var recording: RecordingDto?,
    @Json(name = "broadcasts") var broadcasts: List<BroadcastDto>,
    @Json(name = "snapshots") var snapshots: List<SnapshotDto>,
    @Json(name = "signaling") var signaling: SignalingDto
) {
    @JsonClass(generateAdapter = true)
    data class Links(
        @Json(name = "guest_join") var guestJoin: String,
        @Json(name = "websocket") var websocket: String
    )
}
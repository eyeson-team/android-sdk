package com.eyeson.sdk.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MeetingDto(
    @SerialName("access_key") var accessKey: String,
    @SerialName("ready") var ready: Boolean,
    @SerialName("locked") var locked: Boolean,
    @SerialName("room") var room: RoomDto,
    @SerialName("user") var user: UserDto,
    @SerialName("links") var links: Links,
    @SerialName("recording") var recording: RecordingDto?,
    @SerialName("broadcasts") var broadcasts: List<BroadcastDto>,
    @SerialName("snapshots") var snapshots: List<SnapshotDto>,
    @SerialName("signaling") var signaling: SignalingDto,
    @SerialName("options") var options: MeetingOptionsDto,
    @SerialName("playbacks") var playbacks: List<PlaybackDto>,
) {
    @Serializable
    data class Links(
        @SerialName("guest_join") var guestJoin: String?,
        @SerialName("websocket") var websocket: String,
    )
}
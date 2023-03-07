package com.eyeson.sdk.model.sepp.outgoing


import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.local.sepp.CallStart
import com.eyeson.sdk.model.sepp.base.SeppBaseCommandDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class CallStartDto(
    @Json(name = "type") override val type: String,
    @Json(name = "msg_id") val msgId: String,
    @Json(name = "from") val from: String,
    @Json(name = "to") val to: String,
    @Json(name = "data") val data: Data
) : SeppBaseCommandDto {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "sdp") val sdp: Sdp,
        @Json(name = "display_name") val displayName: String,
        @Json(name = "mute_video") val muteVideo: Boolean,
    ) {
        @JsonClass(generateAdapter = true)
        data class Sdp(
            @Json(name = "type") val type: String,
            @Json(name = "sdp") val sdp: String
        )
    }

    override fun toLocal(): CallStart {
        return CallStart(data.displayName, data.sdp.sdp)
    }
}


internal fun CallStart.fromLocal(meeting: MeetingDto, videoOnStart: Boolean): CallStartDto {
    return CallStartDto(
        type = SeppCommandsOutgoing.CALL_START.type,
        msgId = "",
        from = meeting.signaling.options.clientId,
        to = meeting.signaling.options.confId,
        data = CallStartDto.Data(
            CallStartDto.Data.Sdp(type = "offer", sdp = sdp),
            displayName = displayName,
            muteVideo = !videoOnStart
        )
    )
}
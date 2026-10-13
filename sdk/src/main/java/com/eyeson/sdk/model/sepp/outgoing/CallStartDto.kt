package com.eyeson.sdk.model.sepp.outgoing


import com.eyeson.sdk.BuildConfig
import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.local.sepp.CallStart
import com.eyeson.sdk.model.sepp.base.SeppBaseCommandDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CallStartDto(
    @SerialName("type") override val type: String,
    @SerialName("msg_id") val msgId: String,
    @SerialName("from") val from: String,
    @SerialName("to") val to: String,
    @SerialName("data") val data: Data
) : SeppBaseCommandDto {
    @Serializable
    data class Data(
        @SerialName("platform") val platform: String,
        @SerialName("sdp") val sdp: Sdp,
        @SerialName("display_name") val displayName: String,
        @SerialName("mute_video") val muteVideo: Boolean,
    ) {
        @Serializable
        data class Sdp(
            @SerialName("type") val type: String,
            @SerialName("sdp") val sdp: String
        )
    }

    override fun toLocal(): CallStart {
        return CallStart(data.displayName, data.sdp.sdp)
    }
}


internal fun CallStart.fromLocal(meeting: MeetingDto, videoOnStart: Boolean, platform: String): CallStartDto {
    return CallStartDto(
        type = SeppCommandsOutgoing.CALL_START.type,
        msgId = "",
        from = meeting.signaling.options.clientId,
        to = meeting.signaling.options.confId,
        data = CallStartDto.Data(
            platform = platform,
            sdp = CallStartDto.Data.Sdp(type = "offer", sdp = sdp),
            displayName = displayName,
            muteVideo = !videoOnStart
        )
    )
}
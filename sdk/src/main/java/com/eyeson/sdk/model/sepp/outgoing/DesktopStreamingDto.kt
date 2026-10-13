package com.eyeson.sdk.model.sepp.outgoing


import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.local.base.LocalBaseCommand
import com.eyeson.sdk.model.local.base.UnknownCommand
import com.eyeson.sdk.model.local.sepp.DesktopStreaming
import com.eyeson.sdk.model.sepp.base.SeppBaseCommandDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class DesktopStreamingDto(
    @SerialName("type") override val type: String,
    @SerialName("msg_id") val msgId: String,
    @SerialName("from") val from: String,
    @SerialName("to") val to: String,
    @SerialName("data") val data: Data
) : SeppBaseCommandDto {
    @Serializable
    data class Data(
        @SerialName("call_id") val callId: String,
        @SerialName("on") val on: Boolean,
        @SerialName("cid") val cid: String
    )

    override fun toLocal(): LocalBaseCommand {
        return UnknownCommand()
    }
}

internal fun DesktopStreaming.fromLocal(callId: String, meeting: MeetingDto): DesktopStreamingDto {
    return DesktopStreamingDto(
        type = SeppCommandsOutgoing.DESKTOP_STREAMING.type,
        msgId = "",
        from = meeting.signaling.options.clientId,
        to = meeting.signaling.options.confId,
        data = DesktopStreamingDto.Data(
            callId = callId,
            on = on,
            cid = cid
        )
    )
}

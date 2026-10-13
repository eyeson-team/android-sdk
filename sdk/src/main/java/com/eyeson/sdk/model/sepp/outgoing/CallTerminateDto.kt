package com.eyeson.sdk.model.sepp.outgoing


import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.local.sepp.CallTerminate
import com.eyeson.sdk.model.sepp.base.SeppBaseCommandDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CallTerminateDto(
    @SerialName("type") override val type: String,
    @SerialName("msg_id") val msgId: String,
    @SerialName("from") val from: String,
    @SerialName("to") val to: String,
    @SerialName("data") val data: Data
) : SeppBaseCommandDto {
    @Serializable
    data class Data(
        @SerialName("call_id") val callId: String,
        @SerialName("term_code") val termCode: Int
    )

    override fun toLocal(): CallTerminate {
        return CallTerminate(data.callId, data.termCode)
    }
}

internal fun CallTerminate.fromLocal(meeting: MeetingDto): CallTerminateDto {
    return CallTerminateDto(
        type = SeppCommandsOutgoing.CALL_TERMINATE.type,
        msgId = "",
        from = meeting.signaling.options.clientId,
        to = meeting.signaling.options.confId,
        data = CallTerminateDto.Data(callId = callId, termCode = termCode)
    )
}
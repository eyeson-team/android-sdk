package com.eyeson.sdk.model.sepp.outgoing


import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.local.sepp.CallTerminate
import com.eyeson.sdk.model.sepp.base.SeppBaseCommandDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class CallTerminateDto(
    @Json(name = "type") override val type: String,
    @Json(name = "msg_id") val msgId: String,
    @Json(name = "from") val from: String,
    @Json(name = "to") val to: String,
    @Json(name = "data") val data: Data
) : SeppBaseCommandDto {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "call_id") val callId: String,
        @Json(name = "term_code") val termCode: Int
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
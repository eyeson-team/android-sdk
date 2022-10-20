package com.eyeson.sdk.model.sepp.outgoing


import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.local.base.LocalBaseCommand
import com.eyeson.sdk.model.local.base.UnknownCommand
import com.eyeson.sdk.model.local.sepp.SetPresenter
import com.eyeson.sdk.model.sepp.base.SeppBaseCommandDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SetPresenterDto(
    @Json(name = "type") override val type: String,
    @Json(name = "msg_id") val msgId: String,
    @Json(name = "from") val from: String,
    @Json(name = "to") val to: String,
    @Json(name = "data") val data: Data
) : SeppBaseCommandDto {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "call_id") val callId: String,
        @Json(name = "on") val on: Boolean,
        @Json(name = "cid") val cid: String
    )

    override fun toLocal(): LocalBaseCommand {
        return UnknownCommand()
    }
}

internal fun SetPresenter.fromLocal(callId: String, meeting: MeetingDto): SetPresenterDto {
    return SetPresenterDto(
        type = SeppCommandsOutgoing.SET_PRESENTER.type,
        msgId = "",
        from = meeting.signaling.options.clientId,
        to = meeting.signaling.options.confId,
        data = SetPresenterDto.Data(
            callId = callId,
            on = on,
            cid = cid
        )
    )
}

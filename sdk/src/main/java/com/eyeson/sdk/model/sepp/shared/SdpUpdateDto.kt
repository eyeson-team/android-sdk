package com.eyeson.sdk.model.sepp.shared


import com.eyeson.sdk.model.local.base.LocalBaseCommand
import com.eyeson.sdk.model.local.sepp.SdpUpdate
import com.eyeson.sdk.model.sepp.base.SeppBaseCommandDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SdpUpdateDto(
    @Json(name = "type") override val type: String,
    @Json(name = "msg_id") val msgId: String,
    @Json(name = "from") val from: String,
    @Json(name = "to") val to: String,
    @Json(name = "data") val data: Data
) : SeppBaseCommandDto {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "call_id") val callId: String,
        @Json(name = "sdp") val sdp: Sdp
    ) {
        @JsonClass(generateAdapter = true)
        data class Sdp(
            @Json(name = "type") val type: String,
            @Json(name = "sdp") val sdp: String
        )
    }

    override fun toLocal(): LocalBaseCommand {
        return SdpUpdate(data.callId, data.sdp.sdp)
    }
}

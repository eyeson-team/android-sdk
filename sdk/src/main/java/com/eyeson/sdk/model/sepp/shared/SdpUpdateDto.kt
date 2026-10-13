package com.eyeson.sdk.model.sepp.shared


import com.eyeson.sdk.model.local.base.LocalBaseCommand
import com.eyeson.sdk.model.local.sepp.SdpUpdate
import com.eyeson.sdk.model.sepp.base.SeppBaseCommandDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SdpUpdateDto(
    @SerialName("type") override val type: String,
    @SerialName("msg_id") val msgId: String,
    @SerialName("from") val from: String,
    @SerialName("to") val to: String,
    @SerialName("data") val data: Data
) : SeppBaseCommandDto {
    @Serializable
    data class Data(
        @SerialName("call_id") val callId: String,
        @SerialName("sdp") val sdp: Sdp
    ) {
        @Serializable
        data class Sdp(
            @SerialName("type") val type: String,
            @SerialName("sdp") val sdp: String
        )
    }

    override fun toLocal(): LocalBaseCommand {
        return SdpUpdate(data.callId, data.sdp.sdp)
    }
}

package com.eyeson.sdk.model.sepp.incoming

import com.eyeson.sdk.model.local.sepp.CallRejected
import com.eyeson.sdk.model.sepp.base.SeppBaseCommandDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CallRejectedDto(
    @SerialName("type") override val type: String,
    @SerialName("msg_id") val msgId: String,
    @SerialName("from") val from: String,
    @SerialName("to") val to: String,
    @SerialName("data") val data: Data
) : SeppBaseCommandDto {

    @Serializable
    data class Data(
        @SerialName("reject_code") val rejectCode: Int
    )

    override fun toLocal(): CallRejected {
        return CallRejected(data.rejectCode)
    }
}

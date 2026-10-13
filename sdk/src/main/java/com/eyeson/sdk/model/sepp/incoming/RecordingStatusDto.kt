package com.eyeson.sdk.model.sepp.incoming


import com.eyeson.sdk.model.local.sepp.RecordingStatusUpdate
import com.eyeson.sdk.model.sepp.base.SeppBaseCommandDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RecordingStatusDto(
    @SerialName("type") override val type: String,
    @SerialName("msg_id") val msgId: String,
    @SerialName("from") val from: String,
    @SerialName("to") val to: String,
    @SerialName("data") val data: Data
) : SeppBaseCommandDto {
    @Serializable
    data class Data(
        @SerialName("call_id") val callId: String,
        @SerialName("active") val active: Boolean,
        @SerialName("enabled") val enabled: Boolean
    )

    override fun toLocal(): RecordingStatusUpdate {
        return RecordingStatusUpdate(active = data.active, enabled = data.enabled)
    }
}
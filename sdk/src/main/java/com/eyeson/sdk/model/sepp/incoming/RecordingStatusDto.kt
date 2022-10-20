package com.eyeson.sdk.model.sepp.incoming


import com.eyeson.sdk.model.local.sepp.RecordingStatusUpdate
import com.eyeson.sdk.model.sepp.base.SeppBaseCommandDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class RecordingStatusDto(
    @Json(name = "type") override val type: String,
    @Json(name = "msg_id") val msgId: String,
    @Json(name = "from") val from: String,
    @Json(name = "to") val to: String,
    @Json(name = "data") val data: Data
) : SeppBaseCommandDto {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "call_id") val callId: String,
        @Json(name = "active") val active: Boolean,
        @Json(name = "enabled") val enabled: Boolean
    )

    override fun toLocal(): RecordingStatusUpdate {
        return RecordingStatusUpdate(active = data.active, enabled = data.enabled)
    }
}
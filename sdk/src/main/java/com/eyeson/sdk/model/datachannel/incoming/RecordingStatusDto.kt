package com.eyeson.sdk.model.datachannel.incoming


import com.eyeson.sdk.model.datachannel.base.DataChannelCommandDto
import com.eyeson.sdk.model.local.datachannel.RecordingStatusUpdate
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class RecordingStatusDto(
    @Json(name = "type") override val type: String,
    @Json(name = "active") val active: Boolean,
    @Json(name = "enabled") val enabled: Boolean
) : DataChannelCommandDto {
    override fun toLocal(): RecordingStatusUpdate {
        return RecordingStatusUpdate(active = active, enabled = enabled)
    }
}
package com.eyeson.sdk.model.datachannel.incoming


import com.eyeson.sdk.model.datachannel.base.DataChannelCommandDto
import com.eyeson.sdk.model.local.datachannel.VoiceActivity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class VoiceActivityDto(
    @Json(name = "type") override val type: String,
    @Json(name = "cid") val userId: String,
    @Json(name = "on") val on: Boolean
) : DataChannelCommandDto {
    override fun toLocal(): VoiceActivity {
        return VoiceActivity(userId = userId, on = on)
    }
}

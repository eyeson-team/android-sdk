package com.eyeson.sdk.model.datachannel.incoming


import com.eyeson.sdk.model.datachannel.base.DataChannelCommandDto
import com.eyeson.sdk.model.local.datachannel.VoiceActivity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class VoiceActivityDto(
    @SerialName("type") override val type: String,
    @SerialName("cid") val userId: String,
    @SerialName("on") val on: Boolean
) : DataChannelCommandDto {
    override fun toLocal(): VoiceActivity {
        return VoiceActivity(userId = userId, on = on)
    }
}

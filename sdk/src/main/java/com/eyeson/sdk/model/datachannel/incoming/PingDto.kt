package com.eyeson.sdk.model.datachannel.incoming

import com.eyeson.sdk.model.datachannel.base.DataChannelCommandDto
import com.eyeson.sdk.model.local.datachannel.Ping
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PingDto(
    @SerialName("type") override val type: String
) : DataChannelCommandDto {
    override fun toLocal(): Ping {
        return Ping()
    }
}

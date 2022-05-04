package com.eyeson.sdk.model.datachannel.incoming

import com.eyeson.sdk.model.datachannel.base.DataChannelCommandDto
import com.eyeson.sdk.model.local.datachannel.Ping
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class PingDto(
    @Json(name = "type") override val type: String
) : DataChannelCommandDto {
    override fun toLocal(): Ping {
        return Ping()
    }
}

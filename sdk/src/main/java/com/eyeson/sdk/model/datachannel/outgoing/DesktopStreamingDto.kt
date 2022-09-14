package com.eyeson.sdk.model.datachannel.outgoing


import com.eyeson.sdk.model.datachannel.base.DataChannelCommandDto
import com.eyeson.sdk.model.local.base.UnknownCommand
import com.eyeson.sdk.model.local.datachannel.DesktopStreaming
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class DesktopStreamingDto(
    @Json(name = "type") override val type: String,
    @Json(name = "on") val on: Boolean,
    @Json(name = "cid") val cid: String
) : DataChannelCommandDto {
    override fun toLocal(): UnknownCommand {
        return UnknownCommand()
    }
}

internal fun DesktopStreaming.fromLocal(): DesktopStreamingDto {
    return DesktopStreamingDto(type = "desktopstreaming", on = on, cid = cid)
}
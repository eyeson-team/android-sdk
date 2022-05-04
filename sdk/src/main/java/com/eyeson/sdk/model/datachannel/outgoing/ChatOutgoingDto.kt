package com.eyeson.sdk.model.datachannel.outgoing


import com.eyeson.sdk.model.datachannel.base.DataChannelCommandDto
import com.eyeson.sdk.model.local.base.UnknownCommand
import com.eyeson.sdk.model.local.datachannel.ChatOutgoing
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class ChatOutgoingDto(
    @Json(name = "type") override val type: String,
    @Json(name = "content") val content: String,
    @Json(name = "cid") val userId: String
) : DataChannelCommandDto {
    override fun toLocal(): UnknownCommand {
        return UnknownCommand()
    }
}

internal fun ChatOutgoing.fromLocal(): ChatOutgoingDto {
    return ChatOutgoingDto(type = "chat", content = content, userId = userId)
}
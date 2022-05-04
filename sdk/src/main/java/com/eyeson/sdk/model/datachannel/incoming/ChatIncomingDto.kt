package com.eyeson.sdk.model.datachannel.incoming

import com.eyeson.sdk.model.datachannel.base.DataChannelCommandDto
import com.eyeson.sdk.model.local.datachannel.ChatIncoming
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
internal data class ChatIncomingDto(
    @Json(name = "type") override val type: String,
    @Json(name = "cid") val userId: String,
    @Json(name = "content") val content: String,
    @Json(name = "id") val messageId: String,
    @Json(name = "ts") val timestamp: Date
) : DataChannelCommandDto {

    override fun toLocal(): ChatIncoming {
        return ChatIncoming(
            userId = userId,
            content = content,
            messageId = messageId,
            timestamp = timestamp
        )
    }
}
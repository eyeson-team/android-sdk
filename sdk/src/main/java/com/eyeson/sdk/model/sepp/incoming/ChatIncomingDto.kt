package com.eyeson.sdk.model.sepp.incoming


import com.eyeson.sdk.model.local.sepp.ChatIncoming
import com.eyeson.sdk.model.sepp.base.SeppBaseCommandDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
internal data class ChatIncomingDto(
    @Json(name = "type") override val type: String,
    @Json(name = "msg_id") val msgId: String,
    @Json(name = "from") val from: String,
    @Json(name = "to") val to: String,
    @Json(name = "data") val data: Data
) : SeppBaseCommandDto {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "call_id") val callId: String,
        @Json(name = "cid") val userId: String,
        @Json(name = "content") val content: String,
        @Json(name = "id") val messageId: String,
        @Json(name = "ts") val timestamp: Date
    )

    override fun toLocal(): ChatIncoming {
        return ChatIncoming(
            userId = data.userId,
            content = data.content,
            messageId = data.messageId,
            timestamp = data.timestamp
        )
    }
}
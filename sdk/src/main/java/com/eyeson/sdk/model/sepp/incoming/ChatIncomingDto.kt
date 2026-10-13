package com.eyeson.sdk.model.sepp.incoming


import com.eyeson.sdk.model.local.sepp.ChatIncoming
import com.eyeson.sdk.model.sepp.base.SeppBaseCommandDto
import com.eyeson.sdk.serializer.DateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
internal data class ChatIncomingDto(
    @SerialName("type") override val type: String,
    @SerialName("msg_id") val msgId: String,
    @SerialName("from") val from: String,
    @SerialName("to") val to: String,
    @SerialName("data") val data: Data
) : SeppBaseCommandDto {
    @Serializable
    data class Data(
        @SerialName("call_id") val callId: String,
        @SerialName("cid") val userId: String,
        @SerialName("content") val content: String,
        @SerialName("id") val messageId: String,
        @Serializable(with = DateSerializer::class)
        @SerialName("ts") val timestamp: Date
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
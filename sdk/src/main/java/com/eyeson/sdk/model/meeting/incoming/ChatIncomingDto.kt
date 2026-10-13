package com.eyeson.sdk.model.meeting.incoming

import com.eyeson.sdk.model.local.sepp.ChatIncoming
import com.eyeson.sdk.model.meeting.base.MeetingBaseMessageDto
import com.eyeson.sdk.serializer.DateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
internal data class ChatIncomingDto(
    @SerialName("type") override val type: String,
    @SerialName("content") val content: String,
    @SerialName("cid") val userId: String,
    @Serializable(with = DateSerializer::class)
    @SerialName("created_at") val createdAt: Date
) : MeetingBaseMessageDto {
    override fun toLocal(): ChatIncoming {
        return ChatIncoming(
            userId = userId,
            content = content,
            timestamp = createdAt,
            messageId = ""
        )
    }
}

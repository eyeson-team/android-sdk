package com.eyeson.sdk.model.meeting.incoming

import com.eyeson.sdk.model.local.datachannel.ChatIncoming
import com.eyeson.sdk.model.meeting.base.MeetingBaseMessageDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
internal data class ChatIncomingDto(
    @Json(name = "type") override val type: String,
    @Json(name = "content") val content: String,
    @Json(name = "cid") val userId: String,
    @Json(name = "created_at") val createdAt: Date
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

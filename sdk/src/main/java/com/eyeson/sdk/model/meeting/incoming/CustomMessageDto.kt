package com.eyeson.sdk.model.meeting.incoming

import com.eyeson.sdk.model.local.meeting.CustomMessage
import com.eyeson.sdk.model.meeting.base.MeetingBaseMessageDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
internal data class CustomMessageDto(
    @Json(name = "type") override val type: String,
    @Json(name = "content") val content: String,
    @Json(name = "cid") val userId: String,
    @Json(name = "created_at") val createdAt: Date
) : MeetingBaseMessageDto {
    override fun toLocal(): CustomMessage {
        return CustomMessage(
            content = content,
            createdAt = createdAt,
            userId = userId
        )
    }
}

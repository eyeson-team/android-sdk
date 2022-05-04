package com.eyeson.sdk.model.meeting.incoming

import com.eyeson.sdk.model.api.BroadcastDto
import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.meeting.BroadcastUpdate
import com.eyeson.sdk.model.meeting.base.MeetingBaseMessageDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class BroadcastUpdateDto(
    @Json(name = "type") override val type: String,
    @Json(name = "broadcasts") val broadcasts: List<BroadcastDto>
) : MeetingBaseMessageDto {
    override fun toLocal(): BroadcastUpdate {
        return BroadcastUpdate(broadcasts.map {
            BroadcastUpdate.Broadcast(
                id = it.id,
                platform = it.platform,
                playerUrl = it.platform,
                user = UserInfo(
                    id = it.user.id,
                    name = it.user.name,
                    avatar = it.user.avatar,
                    guest = it.user.guest,
                    joinedAt = it.user.joinedAt
                )
            )
        })
    }
}
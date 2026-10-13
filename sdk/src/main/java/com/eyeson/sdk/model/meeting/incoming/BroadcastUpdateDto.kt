package com.eyeson.sdk.model.meeting.incoming

import com.eyeson.sdk.model.api.BroadcastDto
import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.meeting.BroadcastUpdateLocal
import com.eyeson.sdk.model.meeting.base.MeetingBaseMessageDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class BroadcastUpdateDto(
    @SerialName("type") override val type: String,
    @SerialName("broadcasts") val broadcasts: List<BroadcastDto>
) : MeetingBaseMessageDto {
    override fun toLocal(): BroadcastUpdateLocal {
        return BroadcastUpdateLocal(broadcasts.map {
            BroadcastUpdateLocal.Broadcast(
                id = it.id,
                platform = it.platform,
                playerUrl = it.playerUrl,
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
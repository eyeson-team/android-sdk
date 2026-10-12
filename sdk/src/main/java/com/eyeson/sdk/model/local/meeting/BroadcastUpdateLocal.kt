package com.eyeson.sdk.model.local.meeting

import com.eyeson.sdk.model.local.api.BroadcastUpdate
import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.base.LocalBaseCommand

internal data class BroadcastUpdateLocal(val broadcasts: List<Broadcast>) : LocalBaseCommand {
    internal data class Broadcast(
        val id: String,
        val platform: String,
        val playerUrl: String,
        val user: UserInfo
    )
}

internal fun BroadcastUpdateLocal.toLocal(): BroadcastUpdate {
    return BroadcastUpdate(
        broadcasts = broadcasts.map {
            BroadcastUpdate.Broadcast(
                id = it.id,
                platform = it.platform,
                playerUrl = it.playerUrl,
                user = it.user
            )
        }
    )
}

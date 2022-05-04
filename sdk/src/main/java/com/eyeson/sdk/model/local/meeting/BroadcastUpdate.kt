package com.eyeson.sdk.model.local.meeting

import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.base.LocalBaseCommand

data class BroadcastUpdate(val broadcasts: List<Broadcast>) : LocalBaseCommand {
    data class Broadcast(
        val id: String,
        val platform: String,
        val playerUrl: String,
        val user: UserInfo
    )
}

package com.eyeson.sdk.model.local.api

public data class BroadcastUpdate(val broadcasts: List<Broadcast>) {
    public data class Broadcast(
        val id: String,
        val platform: String,
        val playerUrl: String,
        val user: UserInfo
    )
}

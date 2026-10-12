package com.eyeson.sdk.model.local.api

public data class Playback(
    val url: String,
    val name: String?,
    val playId: String?,
    val replacedUser: UserInfo?,
    val audio: Boolean,
    val loopCount: Int
)

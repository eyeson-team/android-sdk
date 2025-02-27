package com.eyeson.sdk.model.local.api

data class PlaybackInitial(
    val url: String,
    val name: String?,
    val playId: String?,
    val replacementId: String?,
    val audio: Boolean,
    val loopCount: Int,
)

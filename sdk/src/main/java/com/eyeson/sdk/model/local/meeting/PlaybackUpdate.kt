package com.eyeson.sdk.model.local.meeting

import com.eyeson.sdk.model.local.base.LocalBaseCommand

internal data class PlaybackUpdate(
    val playing: List<PlaybackInternal>
) : LocalBaseCommand {

    internal data class PlaybackInternal(
        val url: String,
        val name: String?,
        val playId: String?,
        val replacementId: String?,
        val audio: Boolean,
        val loopCount: Int
    )
}


package com.eyeson.sdk.model.local.meeting

import com.eyeson.sdk.model.local.base.LocalBaseCommand

data class PlaybackUpdate(
    val playing: List<Playback>
) : LocalBaseCommand {

    data class Playback(
        val url: String,
        val name: String?,
        val playId: String?,
        val replacementId: String?,
        val audio: Boolean
    )
}


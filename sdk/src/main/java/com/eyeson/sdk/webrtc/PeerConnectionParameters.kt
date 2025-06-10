package com.eyeson.sdk.webrtc

import com.eyeson.sdk.options.AudioOptions
import com.eyeson.sdk.options.ScreenShareOptions
import com.eyeson.sdk.options.VideoOptions

internal data class PeerConnectionParameters(
    val receiveAudio: Boolean = true,
    val audioOptions: AudioOptions,
    val receiveVideo: Boolean = true,
    val videoOptions: VideoOptions,
    val screenShareOptions: ScreenShareOptions,
    val isWidescreen: Boolean = true,
    val dataChannelParameters: DataChannelParameters = DataChannelParameters(),
) {
    data class DataChannelParameters(
        val ordered: Boolean = true,
        val maxRetransmitTimeMs: Int = -1,
        val maxRetransmits: Int = -1,
        val protocol: String = "",
        val negotiated: Boolean = true,
        val id: Int = 0,
    )
}


package com.eyeson.sdk.options

import com.eyeson.sdk.annotations.Experimental

data class AudioOptions(
    val audioMaxAverageBitrateKbps: Int = 64,
    val echoCancellation: Boolean = true,
    val hardwareAcousticEchoCanceler: Boolean = true,
    val autoGainControl: Boolean = true,
    val highpassFilter: Boolean = true,
    val noiseSuppression: Boolean = true,
    val hardwareNoiseSuppressor: Boolean = true,
    val typingNoiseDetection: Boolean = true,
) {
    var stereo: Boolean = false
        @Experimental
        set
}

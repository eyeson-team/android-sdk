package com.eyeson.sdk.options

import androidx.annotation.IntRange
import com.eyeson.sdk.annotations.Experimental

/**
 * Audio parameters of a meeting.
 *
 * @property audioMaxAverageBitrateKbps The bitrate of the Opus audio that is sent, in kbps. It does
 *                  not limit the audio that is received. Values outside 6-510 fall back to 64.
 * @property stereo Experimental. Captures and plays out stereo and tells the remote side that
 *                  stereo can be received. Whether stereo is also sent depends on the remote side
 *                  asking for it. It is applied when the peer connection is created and therefore
 *                  cannot be changed while in a meeting.
 */
public data class AudioOptions(
    @IntRange(from = 6, to = 510)
    val audioMaxAverageBitrateKbps: Int = 64,
    val echoCancellation: Boolean = true,
    val hardwareAcousticEchoCanceler: Boolean = true,
    val autoGainControl: Boolean = true,
    val highpassFilter: Boolean = true,
    val noiseSuppression: Boolean = true,
    val hardwareNoiseSuppressor: Boolean = true,
    val typingNoiseDetection: Boolean = true,
    @property:Experimental
    val stereo: Boolean = false,
)

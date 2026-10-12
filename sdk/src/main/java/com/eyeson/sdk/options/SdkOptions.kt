package com.eyeson.sdk.options

import com.eyeson.sdk.BuildConfig
import org.webrtc.EglBase

/**
 * Options for how the SDK itself is set up, as opposed to the media parameters in
 * [AudioOptions], [VideoOptions] and [ScreenShareOptions].
 *
 * @property eglBase An [EglBase] for the SDK to use instead of creating its own, for an app that
 *                   has to be the root of the GL share group. It stays owned by the caller, is
 *                   never released by the SDK and has to outlive every meeting and renderer.
 *                   The EGL context is process wide, so it only arrives in time if nothing else of
 *                   the SDK has touched EGL yet: the first [com.eyeson.sdk.webrtc.VideoRenderer],
 *                   the first [com.eyeson.sdk.EyesonMeeting] and **the first read of
 *                   [com.eyeson.sdk.EyesonMeeting.eglBaseContext]** all create the context, and
 *                   whichever comes first wins. Handing one in afterwards throws
 *                   [IllegalStateException], as does a later meeting carrying a different
 *                   [EglBase]; the same instance again is a no-op. The two are therefore an
 *                   either-or: own the group through this, or join the SDK's through
 *                   `eglBaseContext`, never one after the other. **Most apps do not need this** —
 *                   joining works at any point and never throws.
 * @property apiUrl The eyeson API endpoint to talk to. Defaults to the endpoint of the build
 *                  flavor and only has to be set to reach a different one.
 * @property videoCodecHardwareAcceleration Uses the hardware video encoder and decoder of the
 *                                          device. Set it to `false` to fall back to the software
 *                                          codecs. It is applied when the peer connection factory
 *                                          is created and therefore cannot be changed while in a
 *                                          meeting.
 */
public data class SdkOptions(
    val eglBase: EglBase? = null,
    val apiUrl: String = BuildConfig.API_URL,
    val videoCodecHardwareAcceleration: Boolean = true,
)

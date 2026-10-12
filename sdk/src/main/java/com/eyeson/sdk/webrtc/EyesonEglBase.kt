package com.eyeson.sdk.webrtc

import org.webrtc.EglBase

/**
 * Holds the one [EglBase] the SDK uses.
 *
 * Everything that renders or encodes video has to be in the same EGL share group: the codec
 * factories and the capture thread in [PeerConnectionClient] as well as every
 * [com.eyeson.sdk.webrtc.VideoRenderer]. Renderers are usually created before an
 * [com.eyeson.sdk.EyesonMeeting] exists and can outlive it, so the [EglBase] is process wide
 * rather than bound to a meeting.
 *
 * It is created lazily on first use and never released: it is a single EGL context per process,
 * and keeping it alive removes every use after release problem between renderers and meetings.
 *
 * The SDK is the root of the share group unless an app takes that role with [provide]. Joining the
 * group needs neither: an app can create its own [EglBase] from [context] at any point, which is
 * what [com.eyeson.sdk.EyesonMeeting.eglBaseContext] is for and what every app that does not have
 * to be the root should use.
 */
internal object EyesonEglBase {
    private var provided: EglBase? = null
    private var created: EglBase? = null

    /**
     * The [EglBase] everything shares. The one handed in through [provide] if there is one,
     * otherwise the SDK's own, created on first use.
     */
    val eglBase: EglBase
        @Synchronized get() {
            provided?.let { return it }
            return created ?: EglBase.create().also { created = it }
        }

    /** Share group handle, which is all any WebRTC API actually needs. */
    val context: EglBase.Context
        get() = eglBase.eglBaseContext

    /**
     * Uses [eglBase] instead of an SDK created one. It stays owned by the caller and is never
     * released by the SDK.
     *
     * Only for an app that has to be the root of the share group. It has to arrive before anything
     * else touches EGL, which in practice means before the first
     * [com.eyeson.sdk.webrtc.VideoRenderer] is initialized. Arriving later would leave renderers
     * and the peer connection in different share groups, and a hardware decoded frame cannot be
     * sampled across groups, so it throws rather than letting that reach the screen.
     *
     * An app that only needs to share, not to own, uses [context] instead and has no order to get
     * right. `SdkOptions.eglBase`, applied in [com.eyeson.sdk.EyesonMeeting]'s `init`, is the only
     * caller.
     */
    @Synchronized
    fun provide(eglBase: EglBase) {
        if (provided === eglBase) {
            return
        }

        check(provided == null) {
            "A different EglBase was already provided to the SDK"
        }
        check(created == null) {
            "The SDK already created its own EglBase. Provide one before creating a VideoRenderer " +
                "or an EyesonMeeting, or share the SDK's own with EyesonMeeting.eglBaseContext."
        }

        provided = eglBase
    }
}

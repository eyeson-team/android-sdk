package com.eyeson.sdk.options

/**
 * Which media a meeting sends and receives.
 *
 * Passed to `join`, `joinAsGuest` and `connectPermalink`. It is applied when the peer connection
 * is created and therefore cannot be changed while in a meeting; the next join can use different
 * values.
 *
 * @property sendAudio `false` to never open the microphone. `RECORD_AUDIO` is then not required
 *                     to join, `microphoneEnabledOnStart` is ignored and `setMicrophoneEnabled`
 *                     does nothing.
 * @property receiveAudio `false` to not receive the audio of the meeting.
 * @property sendVideo `false` to never open the camera. `CAMERA` is then not required to join,
 *                     `videoEnabledOnStart` and a `screenShareInfo` passed for the start are
 *                     ignored, and `setVideoEnabled`, `switchCamera`, `switchCameraTo` and
 *                     `startScreenShare` do nothing. Camera and screen share are sent through the
 *                     same sender, so there is no screen sharing without it.
 * @property receiveVideo `false` to not receive the video of the meeting. The remote `VideoSink`
 *                        then never gets a frame.
 *
 * @throws IllegalArgumentException if all four are `false`.
 */
public data class MediaOptions(
    val sendAudio: Boolean = true,
    val receiveAudio: Boolean = true,
    val sendVideo: Boolean = true,
    val receiveVideo: Boolean = true,
) {
    init {
        require(sendAudio || receiveAudio || sendVideo || receiveVideo) {
            "MediaOptions must send or receive at least one kind of media"
        }
    }
}

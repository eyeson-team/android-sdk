package com.eyeson.sdk

import android.Manifest
import android.app.Application
import android.app.Notification
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.core.content.ContextCompat
import com.eyeson.sdk.callLogic.CallLogic
import com.eyeson.sdk.events.AudioMutedBy
import com.eyeson.sdk.events.BroadcastUpdated
import com.eyeson.sdk.events.CallRejectionReason
import com.eyeson.sdk.events.CallTerminationReason
import com.eyeson.sdk.events.CameraClosed
import com.eyeson.sdk.events.CameraDisconnected
import com.eyeson.sdk.events.CameraError
import com.eyeson.sdk.events.CameraFirstFrameAvailable
import com.eyeson.sdk.events.CameraFrozen
import com.eyeson.sdk.events.CameraOpened
import com.eyeson.sdk.events.CameraSwitchDone
import com.eyeson.sdk.events.CameraSwitchError
import com.eyeson.sdk.events.ChatMessageReceived
import com.eyeson.sdk.events.ConnectionStatisticUpdated
import com.eyeson.sdk.events.CustomMessageReceived
import com.eyeson.sdk.events.EyesonEvent
import com.eyeson.sdk.events.MediaPlayback
import com.eyeson.sdk.events.MediaPlaybackResponse
import com.eyeson.sdk.events.MediaPlaybackStarted
import com.eyeson.sdk.events.MediaPlaybackStopped
import com.eyeson.sdk.events.MeetingJoinFailed
import com.eyeson.sdk.events.MeetingJoined
import com.eyeson.sdk.events.MeetingJoining
import com.eyeson.sdk.events.MeetingLocked
import com.eyeson.sdk.events.MeetingTerminated
import com.eyeson.sdk.events.NeededPermissions
import com.eyeson.sdk.events.OptionsUpdated
import com.eyeson.sdk.events.PresentationResponse
import com.eyeson.sdk.events.PresentationStarted
import com.eyeson.sdk.events.PresentationStopped
import com.eyeson.sdk.events.PresentationUpdated
import com.eyeson.sdk.events.RecordingUpdated
import com.eyeson.sdk.events.SnapshotUpdated
import com.eyeson.sdk.events.StreamingModeChanged
import com.eyeson.sdk.events.UserJoined
import com.eyeson.sdk.events.UserLeft
import com.eyeson.sdk.events.UserListUpdated
import com.eyeson.sdk.events.VideoSourceUpdated
import com.eyeson.sdk.events.VoiceActivityChanged
import com.eyeson.sdk.exceptions.internal.FaultyInfoException
import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.local.api.MeetingInfo
import com.eyeson.sdk.model.local.api.MeetingInfoInitial
import com.eyeson.sdk.model.local.api.PermalinkMeetingInfo
import com.eyeson.sdk.model.local.api.Playback
import com.eyeson.sdk.model.local.api.PlaybackInitial
import com.eyeson.sdk.model.local.api.Recording
import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.base.LocalBaseCommand
import com.eyeson.sdk.model.local.call.CameraClosedLocal
import com.eyeson.sdk.model.local.call.CameraDisconnectedLocal
import com.eyeson.sdk.model.local.call.CameraErrorLocal
import com.eyeson.sdk.model.local.call.CameraFirstFrameAvailableLocal
import com.eyeson.sdk.model.local.call.CameraFrozenLocal
import com.eyeson.sdk.model.local.call.CameraOpenLocal
import com.eyeson.sdk.model.local.call.CameraSwitchDoneLocal
import com.eyeson.sdk.model.local.call.CameraSwitchErrorLocal
import com.eyeson.sdk.model.local.call.ConnectionStatisticLocal
import com.eyeson.sdk.model.local.call.MeetingJoinedLocal
import com.eyeson.sdk.model.local.call.ResumeCallLocal
import com.eyeson.sdk.model.local.call.StartCallLocal
import com.eyeson.sdk.model.local.call.toLocal
import com.eyeson.sdk.model.local.datachannel.VoiceActivity
import com.eyeson.sdk.model.local.meeting.BroadcastUpdateLocal
import com.eyeson.sdk.model.local.meeting.CustomMessageLocal
import com.eyeson.sdk.model.local.meeting.MeetingLockedLocal
import com.eyeson.sdk.model.local.meeting.MuteLocalAudioLocal
import com.eyeson.sdk.model.local.meeting.OptionsUpdateLocal
import com.eyeson.sdk.model.local.meeting.PlaybackUpdateLocal
import com.eyeson.sdk.model.local.meeting.PresentationUpdateLocal
import com.eyeson.sdk.model.local.meeting.RecordingLocal
import com.eyeson.sdk.model.local.meeting.SnapshotUpdateLocal
import com.eyeson.sdk.model.local.meeting.toLocal
import com.eyeson.sdk.model.local.sepp.CallAccepted
import com.eyeson.sdk.model.local.sepp.CallRejected
import com.eyeson.sdk.model.local.sepp.CallResume
import com.eyeson.sdk.model.local.sepp.CallResumed
import com.eyeson.sdk.model.local.sepp.CallStart
import com.eyeson.sdk.model.local.sepp.CallTerminated
import com.eyeson.sdk.model.local.sepp.ChatIncoming
import com.eyeson.sdk.model.local.sepp.MemberListUpdate
import com.eyeson.sdk.model.local.sepp.RecordingStatusUpdate
import com.eyeson.sdk.model.local.sepp.SdpUpdate
import com.eyeson.sdk.model.local.sepp.SourceUpdate
import com.eyeson.sdk.model.local.ws.ReconnectSignaling
import com.eyeson.sdk.model.local.ws.WsClosed
import com.eyeson.sdk.model.local.ws.WsFailure
import com.eyeson.sdk.model.meeting.incoming.BroadcastUpdateDto
import com.eyeson.sdk.model.meeting.incoming.SnapshotUpdateDto
import com.eyeson.sdk.network.RestCommunicator
import com.eyeson.sdk.network.WebSocketCommunicator
import com.eyeson.sdk.options.AudioOptions
import com.eyeson.sdk.options.MediaOptions
import com.eyeson.sdk.options.ScreenShareOptions
import com.eyeson.sdk.options.SdkOptions
import com.eyeson.sdk.options.VideoOptions
import com.eyeson.sdk.options.VideoResolution
import com.eyeson.sdk.utils.Logger
import com.eyeson.sdk.utils.collectIn
import com.eyeson.sdk.webrtc.EyesonEglBase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.webrtc.EglBase
import org.webrtc.SessionDescription
import org.webrtc.VideoSink
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The main class for interacting with the eyeson.com meeting service.
 *
 * This class provides methods to join, manage, and leave eyeson meetings, as well as
 * interact with meeting participants and the meeting environment.
 *
 * An instance can be reused: after [leave] the same instance can [join] another meeting. [leave]
 * releases everything a meeting holds, so an instance that is no longer needed just has to be
 * dereferenced.
 *
 * One instance holds at most one meeting at a time. Joining while a meeting is running or being
 * joined does nothing and returns [JoinResult.AlreadyJoined]; the running meeting is never
 * disturbed by it.
 *
 * @param application The Android Application instance. Used for context and resource access.
 * @param audioOptions Audio parameters used for every meeting joined with this instance.
 * @param videoOptions Video parameters for the local stream. Can be changed while in a meeting
 *                     with [updateVideoOptions] and persists across joins.
 * @param screenShareOptions Video parameters for screen sharing. Can be changed while in a
 *                           meeting with [updateScreenShareOptions] and persists across joins.
 * @param sdkOptions Setup of the SDK itself, such as the API endpoint or an EGL context to
 *                   own the share group. See [SdkOptions].
 */
public class EyesonMeeting(
    private val application: Application,
    private var audioOptions: AudioOptions = AudioOptions(),
    private var videoOptions: VideoOptions = VideoOptions(),
    private var screenShareOptions: ScreenShareOptions = ScreenShareOptions(),
    private var sdkOptions: SdkOptions = SdkOptions(),
) {
    private val _events = MutableSharedFlow<EyesonEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * Hot flow of all meeting events.
     *
     * Nothing is replayed: a collector only receives what is emitted while it is subscribed.
     * Subscribe before [join] is called, otherwise the events of the joining phase
     * ([MeetingJoining], [MeetingJoinFailed]) are missed. A collector that re-subscribes, e.g. one
     * driven by `repeatOnLifecycle`, therefore never sees an event a second time.
     *
     * A join that never began is not an event either: whether [join], [joinAsGuest] and
     * [connectPermalink] began one is their [JoinResult] return value.
     *
     * No event is delivered between [leave] and the next [join].
     */
    public val events: SharedFlow<EyesonEvent> = _events.asSharedFlow()

    private val eyesonMeetingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var webSocketCommunicator: WebSocketCommunicator? = null
    private val restCommunicator by lazy { RestCommunicator(sdkOptions.apiUrl) }

    private var callLogic: CallLogic? = null

    private var meeting: MeetingDto? = null
    private val joined = AtomicBoolean(false)
    private val eventsEnabled = AtomicBoolean(true)
    private var audioOnStart = true
    private var videoOnStart = true
    private var mediaOptions: MediaOptions? = null

    // Every access to userInMeeting holds userListMutex, including the REST lookups that fill it:
    // a Mutex, unlike a single-threaded dispatcher, stays held across their suspension, so two
    // handlers never fetch the same user twice or emit their events out of order. It is not
    // reentrant, which is why fetchUserInfo() expects the caller to hold it.
    private val userInMeeting = mutableMapOf<String, UserInfo>()
    private val userListMutex = Mutex()

    init {
        sdkOptions.eglBase?.let { EyesonEglBase.provide(it) }
    }

    /**
     * Changes the video parameters of the camera stream.
     *
     * Can be called at any time, in or out of a meeting, and the new values persist across
     * [leave] and [join]. In a running meeting the capture format is only reconfigured if a value
     * differs from the current one. Has no effect on a meeting that does not send video, see
     * [MediaOptions.sendVideo].
     *
     * @param videoResolution Capture resolution of the camera.
     * @param videoFps Capture frame rate of the camera.
     * @param videoMaxBitrate Upper limit of the encoded video in kbps. Only applies while the
     *                        camera is the active source, see [updateScreenShareOptions].
     * @param scaleResolutionDownBy Divides the encoded resolution on top of [videoResolution]:
     *                              640x360 scaled by `2.0` is encoded as 320x180. Only applies
     *                              while the camera is the active source.
     */
    public fun updateVideoOptions(
        videoResolution: VideoResolution = videoOptions.videoResolution,
        @IntRange(from = 0, to = 30) videoFps: Int = videoOptions.videoFps,
        @IntRange(from = 0, to = 2000) videoMaxBitrate: Int = videoOptions.videoMaxBitrate,
        @FloatRange(
            from = 1.0,
            to = 16.0
        ) scaleResolutionDownBy: Double = videoOptions.scaleResolutionDownBy,
    ) {
        videoOptions = videoOptions.copy(
            videoResolution = videoResolution,
            videoFps = videoFps,
            videoMaxBitrate = videoMaxBitrate,
            scaleResolutionDownBy = scaleResolutionDownBy
        )
        callLogic?.videoOptions = videoOptions
    }


    /**
     * Changes the video parameters of screen sharing.
     *
     * Works like [updateVideoOptions], for the screen instead of the camera. Camera and screen
     * share use one video sender, so only the bitrate and scale factor of the active source are
     * in effect. The new values persist across [leave] and [join]; a running screen share is only
     * reconfigured if a value differs from the current one.
     *
     * @param screenShareFps Capture frame rate of the screen.
     * @param screenShareResolution Capture resolution of the screen.
     * @param scaleResolutionDownBy Divides the encoded resolution on top of
     *                              [screenShareResolution].
     * @param videoMaxBitrate Upper limit of the encoded screen share in kbps.
     */
    public fun updateScreenShareOptions(
        @IntRange(from = 0, to = 30) screenShareFps: Int = screenShareOptions.screenShareFps,
        screenShareResolution: VideoResolution = screenShareOptions.screenShareResolution,
        @FloatRange(
            from = 1.0,
            to = 16.0
        ) scaleResolutionDownBy: Double = screenShareOptions.scaleResolutionDownBy,
        @IntRange(from = 0, to = 2000) videoMaxBitrate: Int = screenShareOptions.videoMaxBitrate,
    ) {
        screenShareOptions = screenShareOptions.copy(
            screenShareFps = screenShareFps,
            screenShareResolution = screenShareResolution,
            scaleResolutionDownBy = scaleResolutionDownBy,
            videoMaxBitrate = videoMaxBitrate
        )
        callLogic?.screenShareOptions = screenShareOptions
    }

    /**
     * What the SDK needs to capture the screen.
     *
     * Capturing runs in the SDK's `ScreenCapturerService`, a foreground service that shows
     * [notification] for as long as the screen is shared.
     *
     * @property mediaProjectionPermissionResultData The result `Intent` of the
     *           `MediaProjectionManager.createScreenCaptureIntent()` request the user accepted.
     * @property notificationId ID the foreground service posts [notification] under.
     * @property notification Shown while the screen is shared. Its channel has to exist already.
     */
    public data class ScreenShareInfo(
        val mediaProjectionPermissionResultData: Intent,
        val notificationId: Int,
        val notification: Notification,
    )

    /**
     * The outcome of a [join], [joinAsGuest] or [connectPermalink] call.
     *
     * It only says whether the meeting is being joined. Everything after that — [MeetingJoining],
     * [MeetingJoined], [MeetingJoinFailed] — is delivered on [events].
     */
    public sealed interface JoinResult {

        /**
         * The meeting is being joined. The outcome arrives on [events]: [MeetingJoining] followed
         * by [MeetingJoined], or [MeetingJoinFailed].
         */
        public data object Joining : JoinResult

        /**
         * Nothing happened because runtime permissions are missing. Request them
         * (`permissions.map { it.permissions }`) and call the join again.
         *
         * @property permissions The runtime permissions that are still missing. Never empty.
         */
        public data class MissingPermissions(
            val permissions: List<NeededPermissions>,
        ) : JoinResult

        /**
         * Nothing happened because this instance is already in a meeting or in the middle of
         * joining one. The running meeting is untouched and no event is emitted for this call.
         *
         * To join a different meeting, either call [leave] on this instance first or use a
         * separate [EyesonMeeting] — the limit is one meeting per instance, not one per app.
         * An instance whose meeting has ended needs neither: the SDK leaves on its own after
         * [MeetingJoinFailed] and [MeetingTerminated], so it can join again right away.
         */
        public data object AlreadyJoined : JoinResult
    }

    /**
     * Joins the meeting identified by [accessKey].
     *
     * Checks the runtime permissions first: `RECORD_AUDIO` if [MediaOptions.sendAudio] is set,
     * `CAMERA` if [MediaOptions.sendVideo] is set. Then returns right away; the meeting is fetched
     * and joined in the background and its progress arrives on [events], so collect [events]
     * before calling this.
     *
     * @param accessKey The access key of the meeting, as returned by the eyeson API when the room
     *                  is created.
     * @param frontCamera `true` to start with the front camera, `false` with the back camera.
     *                    Ignored if [MediaOptions.sendVideo] is `false`.
     * @param local Receives the local camera or screen video. Can be changed later with
     *              [setLocalVideoTarget]; `null` to not render it.
     * @param remote Receives the meeting's video. Can be changed later with
     *               [setRemoteVideoTarget]; `null` to not render it.
     * @param microphoneEnabledOnStart `true` to join with the microphone on. Always `false` if
     *                                 [MediaOptions.sendAudio] is `false`.
     * @param videoEnabledOnStart `true` to join with the video on. Always `false` if
     *                            [MediaOptions.sendVideo] is `false`.
     * @param mediaOptions Which media is sent and received. Fixed for the whole meeting, see
     *                     [getMediaOptions]. A direction that is not sent needs no permission.
     * @param screenShareInfo Joins with the screen shared instead of the camera; `null` to start
     *                        with the camera. Ignored if [MediaOptions.sendVideo] is `false`.
     *
     * @return [JoinResult.Joining] if the meeting is being joined. [JoinResult.MissingPermissions]
     *         or [JoinResult.AlreadyJoined] if nothing happened: no meeting was touched and no
     *         event is emitted.
     */
    public fun join(
        accessKey: String,
        frontCamera: Boolean,
        local: VideoSink?,
        remote: VideoSink?,
        microphoneEnabledOnStart: Boolean = true,
        videoEnabledOnStart: Boolean = true,
        mediaOptions: MediaOptions = MediaOptions(),
        screenShareInfo: ScreenShareInfo? = null,
    ): JoinResult {
        return joinMeeting(
            { restCommunicator.getMeetingInfo(accessKey) },
            frontCamera,
            local,
            remote,
            microphoneEnabledOnStart,
            videoEnabledOnStart,
            mediaOptions,
            screenShareInfo
        )
    }

    /**
     * Joins a meeting as a guest, with a guest token instead of an access key.
     *
     * Behaves like [join] in every other respect.
     *
     * @param guestToken The guest token of the meeting: [MeetingInfo.guestToken] of a running
     *                   meeting, or [PermalinkMeetingInfo.guestToken] of a permalink.
     * @param name Display name of the guest, shown to the other participants.
     * @param id Your own identifier for the guest; `null` to let eyeson assign one.
     * @param avatar URL of an avatar image; `null` for none.
     * @param frontCamera `true` to start with the front camera, `false` with the back camera.
     *                    Ignored if [MediaOptions.sendVideo] is `false`.
     * @param local Receives the local camera or screen video. Can be changed later with
     *              [setLocalVideoTarget]; `null` to not render it.
     * @param remote Receives the meeting's video. Can be changed later with
     *               [setRemoteVideoTarget]; `null` to not render it.
     * @param microphoneEnabledOnStart `true` to join with the microphone on. Always `false` if
     *                                 [MediaOptions.sendAudio] is `false`.
     * @param videoEnabledOnStart `true` to join with the video on. Always `false` if
     *                            [MediaOptions.sendVideo] is `false`.
     * @param mediaOptions Which media is sent and received. Fixed for the whole meeting, see
     *                     [getMediaOptions]. A direction that is not sent needs no permission.
     * @param screenShareInfo Joins with the screen shared instead of the camera; `null` to start
     *                        with the camera. Ignored if [MediaOptions.sendVideo] is `false`.
     *
     * @return [JoinResult.Joining] if the meeting is being joined. [JoinResult.MissingPermissions]
     *         or [JoinResult.AlreadyJoined] if nothing happened: no meeting was touched and no
     *         event is emitted.
     */
    public fun joinAsGuest(
        guestToken: String,
        name: String,
        id: String?,
        avatar: String?,
        frontCamera: Boolean,
        local: VideoSink?,
        remote: VideoSink?,
        microphoneEnabledOnStart: Boolean = true,
        videoEnabledOnStart: Boolean = true,
        mediaOptions: MediaOptions = MediaOptions(),
        screenShareInfo: ScreenShareInfo? = null,
    ): JoinResult {
        return joinMeeting(
            {
                restCommunicator.getMeetingInfoAsGuest(
                    guestToken,
                    name,
                    id,
                    avatar
                )
            },
            frontCamera,
            local,
            remote,
            microphoneEnabledOnStart,
            videoEnabledOnStart,
            mediaOptions,
            screenShareInfo
        )
    }

    /**
     * Joins the meeting of a permalink, starting it if it isn't running yet.
     *
     * Behaves like [join] in every other respect.
     *
     * @param userToken The user token of a permalink user, as returned by the eyeson permalink API.
     * @param frontCamera `true` to start with the front camera, `false` with the back camera.
     *                    Ignored if [MediaOptions.sendVideo] is `false`.
     * @param local Receives the local camera or screen video. Can be changed later with
     *              [setLocalVideoTarget]; `null` to not render it.
     * @param remote Receives the meeting's video. Can be changed later with
     *               [setRemoteVideoTarget]; `null` to not render it.
     * @param microphoneEnabledOnStart `true` to join with the microphone on. Always `false` if
     *                                 [MediaOptions.sendAudio] is `false`.
     * @param videoEnabledOnStart `true` to join with the video on. Always `false` if
     *                            [MediaOptions.sendVideo] is `false`.
     * @param mediaOptions Which media is sent and received. Fixed for the whole meeting, see
     *                     [getMediaOptions]. A direction that is not sent needs no permission.
     * @param screenShareInfo Joins with the screen shared instead of the camera; `null` to start
     *                        with the camera. Ignored if [MediaOptions.sendVideo] is `false`.
     *
     * @return [JoinResult.Joining] if the meeting is being joined. [JoinResult.MissingPermissions]
     *         or [JoinResult.AlreadyJoined] if nothing happened: no meeting was touched and no
     *         event is emitted.
     */
    public fun connectPermalink(
        userToken: String,
        frontCamera: Boolean,
        local: VideoSink?,
        remote: VideoSink?,
        microphoneEnabledOnStart: Boolean = true,
        videoEnabledOnStart: Boolean = true,
        mediaOptions: MediaOptions = MediaOptions(),
        screenShareInfo: ScreenShareInfo? = null,
    ): JoinResult {
        return joinMeeting(
            { restCommunicator.startPermalinkMeeting(userToken) },
            frontCamera,
            local,
            remote,
            microphoneEnabledOnStart,
            videoEnabledOnStart,
            mediaOptions,
            screenShareInfo
        )
    }

    /**
     * Fetches the details of a permalink without joining it, e.g. to show the room name before
     * joining or to get its guest token for [joinAsGuest].
     *
     * Needs no meeting and doesn't affect one.
     *
     * @param token The permalink token.
     * @return The permalink's details, or `null` if the request failed or the token is unknown.
     */
    public suspend fun getPermalinkMeetingInfo(token: String): PermalinkMeetingInfo? {
        return restCommunicator.getPermalinkMeetingInfo(token)
    }

    private fun joinMeeting(
        meetingInfoRequest: suspend () -> MeetingDto,
        frontCamera: Boolean,
        local: VideoSink?,
        remote: VideoSink?,
        microphoneEnabledOnStart: Boolean = true,
        videoEnabledOnStart: Boolean = true,
        mediaOptions: MediaOptions,
        screenShareInfo: ScreenShareInfo?,
    ): JoinResult {
        if (!joined.compareAndSet(false, true)) {
            return JoinResult.AlreadyJoined
        }

        val neededPermissions = checkForNeededPermissions(mediaOptions)
        if (neededPermissions.isNotEmpty()) {
            joined.set(false)
            return JoinResult.MissingPermissions(neededPermissions)
        }

        eventsEnabled.set(true)

        this.mediaOptions = mediaOptions
        audioOnStart = microphoneEnabledOnStart && mediaOptions.sendAudio
        videoOnStart = videoEnabledOnStart && mediaOptions.sendVideo

        val effectiveScreenShareInfo = if (!mediaOptions.sendVideo) {
            if (screenShareInfo != null) {
                Logger.w("Screen share on start is ignored in a meeting that does not send video")
            }
            null
        } else {
            screenShareInfo
        }

        eyesonMeetingScope.launch {
            userListMutex.withLock { userInMeeting.clear() }

            val meetingInfo = try {
                meetingInfoRequest()
            } catch (e: Exception) {
                when (e) {
                    is CancellationException -> {
                        throw e
                    }

                    is FaultyInfoException -> {
                        leaveAndEmit(MeetingJoinFailed(CallRejectionReason.fromRejectCode(e.code)))
                        return@launch
                    }

                    else -> {
                        leaveAndEmit(MeetingJoinFailed(CallRejectionReason.ERROR))
                        return@launch
                    }
                }
            }
            meeting = meetingInfo

            val meetingInfoInitial = MeetingInfoInitial(
                accessKey = meetingInfo.accessKey,
                id = meetingInfo.room.id,
                name = meetingInfo.room.name,
                startedAt = meetingInfo.room.startedAt,
                user = meetingInfo.user.toLocal(Date()),
                locked = meetingInfo.locked,
                guestToken = meetingInfo.room.guestToken,
                guestLink = meetingInfo.links.guestJoin,
                activeRecording = meetingInfo.recording?.toLocal(),
                activeBroadcasts = BroadcastUpdateDto("", meetingInfo.broadcasts).toLocal()
                    .toLocal(),
                snapshots = SnapshotUpdateDto("", meetingInfo.snapshots).toLocal().toLocal(),
                meetingOptions = meetingInfo.options.toLocal(),
                playbacks = meetingInfo.playbacks.map {
                    PlaybackInitial(
                        url = it.url,
                        name = it.name,
                        playId = it.playId,
                        replacementId = it.replacementId,
                        audio = it.audio ?: false,
                        loopCount = it.loopCount ?: 0
                    )
                }
            )

            emitEyesonEvent(MeetingJoining(meetingInfoInitial))

            webSocketCommunicator = WebSocketCommunicator(meetingInfo).apply {
                connect()

                events.collectIn(eyesonMeetingScope) { command ->
                    handleWebSocketEvents(
                        command,
                        frontCamera,
                        local,
                        remote,
                        mediaOptions,
                        effectiveScreenShareInfo
                    )
                }
            }
        }

        return JoinResult.Joining
    }


    /**
     * Replaces the camera with the screen.
     *
     * Only starts the process: capturing begins once the screen capture service is running.
     * From then on [isScreenShareActive] returns `true`; [stopScreenShare] switches back to the
     * camera.
     *
     * @param screenShareInfo Permission result and notification for the capture, see
     *                        [ScreenShareInfo].
     * @param asPresentation `true` to also make the screen the meeting's presentation once
     *                       capturing has started, as [setVideoAsPresentation] does. The result
     *                       arrives as [PresentationStarted].
     * @return `true` if screen sharing is being started. `false` if no call is running, the
     *         meeting does not send video (see [MediaOptions.sendVideo]), or the screen is already
     *         shared.
     */
    public fun startScreenShare(
        screenShareInfo: ScreenShareInfo,
        asPresentation: Boolean,
    ): Boolean {
        return callLogic?.startScreenShare(
            mediaProjectionPermissionResultData = screenShareInfo.mediaProjectionPermissionResultData,
            notificationId = screenShareInfo.notificationId,
            notification = screenShareInfo.notification,
            asPresentation = asPresentation,
        ) {
            if (asPresentation) {
                setVideoAsPresentation()
            }
        } ?: false
    }

    /**
     * Ends screen sharing and switches back to the camera that was active before.
     *
     * Does not end a presentation started with the screen share; call [stopPresentation] for
     * that. Does nothing if no call is running or the meeting does not send video.
     *
     * @param resumeLocalVideo `true` to send the camera video right away, `false` to leave the
     *                         local video off until [setVideoEnabled] is called.
     */
    public fun stopScreenShare(resumeLocalVideo: Boolean) {
        callLogic?.stopScreenShare(resumeLocalVideo)
    }

    /**
     * Whether the screen is currently shared, see [startScreenShare].
     *
     * @return `true` while the screen is captured, `false` otherwise or if no call is running.
     */
    public fun isScreenShareActive(): Boolean {
        return callLogic?.isScreenShareActive() ?: false
    }

    /**
     * Makes the local video the meeting's presentation, shown to every participant in the
     * presentation layout.
     *
     * The request runs in the background and its result arrives as [PresentationStarted]. Does
     * nothing, and emits nothing, if there is no meeting.
     */
    public fun setVideoAsPresentation() {
        eyesonMeetingScope.launch {
            val response = restCommunicator.startPresentation(meeting?.accessKey ?: return@launch)
            emitEyesonEvent(PresentationStarted(response.toPresentationResponse()))
        }
    }

    /**
     * Ends the meeting's presentation, no matter which participant started it.
     *
     * The request runs in the background and its result arrives as [PresentationStopped]. Does
     * nothing, and emits nothing, if there is no meeting.
     */
    public fun stopPresentation() {
        eyesonMeetingScope.launch {
            val response = restCommunicator.stopPresentation(meeting?.accessKey ?: return@launch)
            emitEyesonEvent(PresentationStopped(response.toPresentationResponse()))
        }
    }

    private suspend fun handleWebSocketEvents(
        command: LocalBaseCommand,
        frontCamera: Boolean,
        local: VideoSink?,
        remote: VideoSink?,
        mediaOptions: MediaOptions,
        screenShareInfo: ScreenShareInfo?,
    ) {
        when (command) {
            is StartCallLocal -> {
                meeting = command.meeting
                startCall(
                    command.meeting,
                    frontCamera,
                    local,
                    remote,
                    mediaOptions,
                    screenShareInfo
                )
            }

            is ResumeCallLocal -> {
                resumeCall(command.callId)
            }

            is CallAccepted -> {
                callLogic?.setRemoteDescription(
                    command.sdp,
                    SessionDescription.Type.ANSWER
                )
                emitEyesonEvent(StreamingModeChanged(callLogic?.isSfuMode() ?: return))
            }

            is CallResumed -> {
                callLogic?.setRemoteDescription(
                    command.sdp,
                    SessionDescription.Type.OFFER
                )
                emitEyesonEvent(StreamingModeChanged(callLogic?.isSfuMode() ?: return))
            }

            is CallRejected -> {
                leaveAndEmit(MeetingJoinFailed(CallRejectionReason.fromRejectCode(command.rejectCode)))
            }

            is CallTerminated -> {
                leaveAndEmit(MeetingTerminated(CallTerminationReason.fromTerminationCode(command.terminateCode)))
            }

            is SdpUpdate -> {
                callLogic?.setRemoteDescription(
                    command.sdp,
                    SessionDescription.Type.OFFER
                )
                emitEyesonEvent(StreamingModeChanged(callLogic?.isSfuMode() ?: return))
            }

            is ChatIncoming -> {
                handleChatIncoming(meeting ?: return, command)
            }

            is CustomMessageLocal -> {
                handleCustomMessageIncoming(meeting ?: return, command)
            }

            is MuteLocalAudioLocal -> {
                // There is no microphone to mute, so there is nothing to report either.
                if (!mediaOptions.sendAudio) {
                    return
                }
                setMicrophoneEnabled(false)
                emitEyesonEvent(AudioMutedBy(command.byUser))
            }

            is MeetingLockedLocal -> {
                emitEyesonEvent(MeetingLocked(command.locked))
            }

            is PlaybackUpdateLocal -> {
                userListMutex.withLock {
                    val infoNeededFor = command.playing.filterNot {
                        it.replacementId != null && userInMeeting.containsKey(it.replacementId) || userInMeeting.values.any { userInfo -> userInfo.id == it.replacementId }
                    }.mapNotNull {
                        it.replacementId
                    }

                    if (infoNeededFor.isNotEmpty()) {
                        fetchUserInfo(meeting ?: return@withLock, infoNeededFor)
                    }

                    val event = command.playing.map {
                        val userInfo =
                            userInMeeting.values.firstOrNull { userInfo -> userInfo.id == it.replacementId }
                        Playback(
                            url = it.url,
                            name = it.name,
                            playId = it.playId,
                            replacedUser = userInfo,
                            audio = it.audio,
                            loopCount = it.loopCount
                        )
                    }
                    emitEyesonEvent(MediaPlayback(event))
                }
            }

            is BroadcastUpdateLocal -> {
                emitEyesonEvent((BroadcastUpdated(command.toLocal())))
            }

            is RecordingLocal -> {
                emitEyesonEvent(RecordingUpdated(command.toLocal()))
            }

            is SnapshotUpdateLocal -> {
                emitEyesonEvent(SnapshotUpdated(command.toLocal()))
            }

            is WsFailure -> {
                leaveAndEmit(
                    MeetingTerminated(
                        CallTerminationReason.fromTerminationCode(
                            command.response?.code ?: CallTerminationReason.ERROR.terminationCode
                        )
                    )
                )
            }

            is WsClosed -> {
                leaveAndEmit(MeetingTerminated(CallTerminationReason.OK))
            }

            is ReconnectSignaling -> {
                val accessKey = meeting?.accessKey
                if (accessKey == null) {
                    terminateCallWithError()
                    return
                }

                eyesonMeetingScope.launch {
                    val meetingInfo = try {
                        restCommunicator.getMeetingInfo(accessKey)
                    } catch (e: Exception) {
                        if (e is CancellationException) {
                            throw e
                        } else {
                            terminateCallWithError()
                            return@launch
                        }
                    }
                    meeting = meetingInfo
                    webSocketCommunicator?.reconnectToSignaling(meetingInfo)
                }
            }

            is MemberListUpdate -> {
                userListMutex.withLock {
                    val infoNeededFor = command.added.filterNot {
                        userInMeeting.containsKey(it)
                    }

                    if (infoNeededFor.isNotEmpty()) {
                        fetchUserInfo(meeting ?: return@withLock, infoNeededFor)

                    }

                    if (command.added.isNotEmpty()) {
                        emitEyesonEvent(
                            UserJoined(
                                userInMeeting.filter { command.added.contains(it.key) }.values.toSet()
                                    .toList()
                            )
                        )
                    }

                    if (command.deleted.isNotEmpty()) {
                        emitEyesonEvent(
                            UserLeft(userInMeeting.filter { command.deleted.contains(it.key) }
                                .values.toSet().toList()))
                    }

                    command.deleted.forEach {
                        userInMeeting.remove(it)
                    }
                    emitEyesonEvent(
                        UserListUpdated(
                            userInMeeting.values.toSet().toList(),
                            command.mediaPlayIds
                        )
                    )
                }
            }

            is SourceUpdate -> {
                userListMutex.withLock {
                    val infoNeeded = command.sources.filterNot {
                        userInMeeting.containsKey(it) || it.contains(VIDEO_PLAYBACK_PREFIX)
                    }
                    if (infoNeeded.isNotEmpty()) {
                        fetchUserInfo(meeting ?: return@withLock, infoNeeded)
                    }

                    val videoSourceIds =
                        command.sources.slice(command.videSources.filter { it >= 0 })

                    emitEyesonEvent(
                        VideoSourceUpdated(
                            userInMeeting.filter {
                                videoSourceIds.contains(it.key)
                            }.values.toSet().toList(),
                            if (command.desktopStreamingId == null || command.desktopStreamingId == -1) {
                                null
                            } else {
                                userInMeeting[command.sources[command.desktopStreamingId]]
                            }
                        )
                    )
                }
            }

            is RecordingStatusUpdate -> {
                /**
                 * Not in use for now. Recording status is handled by
                 * @see [Recording]
                 */
                Logger.d("RecordingStatusUpdate: enabled ${command.enabled}; active ${command.active}")
            }

            is PresentationUpdateLocal -> {
                emitEyesonEvent(PresentationUpdated(command.toLocal()))
            }

            is OptionsUpdateLocal -> {
                emitEyesonEvent(OptionsUpdated(command.meetingOptions))
            }
        }
    }

    private fun terminateCallWithError() {
        leaveAndEmit(MeetingTerminated(CallTerminationReason.ERROR))
    }

    /**
     * Leaves the current Eyeson meeting and releases everything it holds.
     *
     * The instance stays usable: [join], [joinAsGuest] and [connectPermalink] can be called
     * again afterwards. Nothing is left running, so an instance that is done with is simply
     * dereferenced. No event is delivered on [events] between this call and the next join.
     *
     * Renderers are not affected and can be used again for the next meeting. Their lifecycle is
     * independent of the meeting, see [com.eyeson.sdk.webrtc.VideoRenderer.release]. Options set
     * through [updateVideoOptions] and [updateScreenShareOptions] are kept for the next meeting.
     */
    public fun leave() {
        // Stops event delivery for everything that races with the teardown below. Terminal events
        // are emitted by leaveAndEmit() once this returns and bypass the gate, so none of them is
        // swallowed.
        eventsEnabled.set(false)

        // Only the children are canceled: the scope itself has to stay usable for a subsequent
        // join. User lookups are children of whoever started them and are canceled along with
        // them.
        eyesonMeetingScope.coroutineContext.cancelChildren()

        webSocketCommunicator?.terminateCall()
        webSocketCommunicator = null

        callLogic?.disconnectCall()
        callLogic = null

        meeting = null
        mediaOptions = null
        joined.set(false)
    }

    /**
     * Fetches the current state of the meeting from the eyeson API.
     *
     * Unlike the snapshot in [MeetingJoining], this is up to date at the time of the call.
     *
     * @return The meeting's details, or `null` if there is no meeting or the request failed.
     */
    public suspend fun getMeetingInfo(): MeetingInfo? {

        val meetingInfo = try {
            restCommunicator.getMeetingInfo(meeting?.accessKey ?: return null)
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            } else {
                return null
            }
        }

        val playbacks = userListMutex.withLock {
            val infoNeededFor = meetingInfo.playbacks.filterNot { playback ->
                playback.replacementId != null && userInMeeting.containsKey(playback.replacementId)
                        || userInMeeting.values.any { userInfo -> userInfo.id == playback.replacementId }
            }.mapNotNull { playback ->
                playback.replacementId
            }

            if (infoNeededFor.isNotEmpty()) {
                fetchUserInfo(meetingInfo, infoNeededFor)
            }

            meetingInfo.playbacks.map {
                val userInfo =
                    userInMeeting.values.firstOrNull { userInfo -> userInfo.id == it.replacementId }

                Playback(
                    url = it.url,
                    name = it.name,
                    playId = it.playId,
                    replacedUser = userInfo,
                    audio = it.audio ?: false,
                    loopCount = it.loopCount ?: 0
                )
            }
        }

        return MeetingInfo(
            accessKey = meetingInfo.accessKey,
            id = meetingInfo.room.id,
            name = meetingInfo.room.name,
            startedAt = meetingInfo.room.startedAt,
            user = meetingInfo.user.toLocal(Date()),
            locked = meetingInfo.locked,
            guestToken = meetingInfo.room.guestToken,
            guestLink = meetingInfo.links.guestJoin,
            activeRecording = meetingInfo.recording?.toLocal(),
            activeBroadcasts = BroadcastUpdateDto("", meetingInfo.broadcasts).toLocal().toLocal(),
            snapshots = SnapshotUpdateDto("", meetingInfo.snapshots).toLocal().toLocal(),
            meetingOptions = meetingInfo.options.toLocal(),
            playbacks = playbacks
        )
    }

    /**
     * Replaces the sink that receives the local video, the one passed to [join] as `local`.
     *
     * Does nothing if no call is running, so the sink for the start of a meeting has to be passed
     * to [join]. [leave] detaches it.
     *
     * @param target The new sink, or `null` to stop rendering the local video. It is sent either
     *               way.
     */
    public fun setLocalVideoTarget(target: VideoSink?) {
        callLogic?.setLocalVideoTarget(target)
    }

    /**
     * Replaces the sink that receives the meeting's video, the one passed to [join] as `remote`.
     *
     * Does nothing if no call is running, so the sink for the start of a meeting has to be passed
     * to [join]. [leave] detaches it.
     *
     * @param target The new sink, or `null` to stop rendering the meeting's video. It is still
     *               received either way.
     */
    public fun setRemoteVideoTarget(target: VideoSink?) {
        callLogic?.setRemoteVideoTarget(target)
    }

    /**
     * Turns the local video on or off.
     *
     * Off stops capturing, which releases the camera, and tells the other participants the video
     * is muted. While the screen is shared it pauses the screen instead. The change is applied
     * asynchronously.
     *
     * Does nothing if no call is running or the meeting does not send video, see
     * [MediaOptions.sendVideo].
     *
     * @param enable `true` to send video, `false` to stop.
     */
    public fun setVideoEnabled(enable: Boolean) {
        if (mediaOptions?.sendVideo == false) {
            return
        }

        webSocketCommunicator?.setLocalVideoEnabled(enable)
        callLogic?.setLocalVideoEnabled(enable)
    }

    /**
     * Whether the local video is on.
     *
     * @return `true` if video is sent. `false` if it is off, no call is running, or the meeting
     *         does not send video.
     */
    public fun isVideoEnabled(): Boolean {
        return callLogic?.isLocalVideoActive() ?: false
    }

    /**
     * Switches to the next camera of the device.
     *
     * The result arrives as [CameraSwitchDone] or [CameraSwitchError]. Does nothing, and emits
     * nothing, if no call is running, the meeting does not send video, or the screen is shared.
     */
    public fun switchCamera() {
        callLogic?.switchCamera()
    }

    /**
     * Switches to a specific camera.
     *
     * The result arrives as [CameraSwitchDone] or [CameraSwitchError]. Does nothing, and emits
     * nothing, if no call is running, the meeting does not send video, or the screen is shared.
     *
     * @param cameraId A camera ID from [CameraManager.getCameraIdList].
     */
    public fun switchCameraTo(cameraId: String) {
        callLogic?.switchCameraTo(cameraId)
    }

    /**
     * Whether the active camera faces the front.
     *
     * @return `true` for a front camera, `false` for any other, `null` if no call is running.
     */
    public fun isFrontCamera(): Boolean? {
        return callLogic?.isFrontCamera()
    }

    /**
     * Asks eyeson to mute the microphones of all other participants. The local microphone is not
     * affected.
     *
     * Participants using this SDK get [AudioMutedBy]. Does nothing if there is no meeting.
     */
    public fun sendMuteOthers() {
        webSocketCommunicator?.sendMuteAll()
    }

    /**
     * Mutes or unmutes the local microphone. The change is applied asynchronously.
     *
     * Does nothing if no call is running or the meeting does not send audio, see
     * [MediaOptions.sendAudio].
     *
     * @param enable `true` to send audio, `false` to mute.
     */
    public fun setMicrophoneEnabled(enable: Boolean) {
        callLogic?.setAudioEnabled(enable)
    }

    /**
     * Whether the local microphone is on.
     *
     * @return `true` if audio is sent. `false` if it is muted, no call is running, or the meeting
     *         does not send audio.
     */
    public fun isMicrophoneEnabled(): Boolean {
        return callLogic?.isAudioEnabled() ?: false
    }

    /**
     * Mutes or unmutes the meeting's audio on this device only; the other participants are not
     * affected.
     *
     * Does nothing if no call is running. The setting is not kept: every meeting starts with the
     * remote audio on.
     *
     * @param enable `true` to play the meeting's audio, `false` to mute it.
     */
    public fun setRemoteAudioEnabled(enable: Boolean) {
        callLogic?.setRemoteAudioEnabled(enable)
    }

    /**
     * Sends a chat message to all participants, who receive it as [ChatMessageReceived].
     *
     * Runs in the background; a failure is only logged. Does nothing if there is no meeting.
     *
     * @param message The text of the message.
     */
    public fun sendChatMessage(message: String) {
        eyesonMeetingScope.launch {
            restCommunicator.sendChatMessage(meeting?.accessKey ?: return@launch, message)
        }
    }

    /**
     * Sends an app-defined message to all participants, who receive it as
     * [CustomMessageReceived].
     *
     * Unlike [sendChatMessage] it is not shown in the meeting's chat, so apps can use it for their
     * own signalling. Runs in the background; a failure is only logged. Does nothing if there is
     * no meeting.
     *
     * @param content The payload, e.g. JSON.
     */
    public fun sendCustomMessage(content: String) {
        eyesonMeetingScope.launch {
            restCommunicator.sendCustomMessage(meeting?.accessKey ?: return@launch, content)
        }
    }

    /**
     * Plays a video into the meeting for all participants.
     *
     * The request runs in the background and its result arrives as [MediaPlaybackStarted];
     * changes to the running playbacks arrive as [MediaPlayback]. Does nothing, and emits nothing,
     * if there is no meeting.
     *
     * @param url Publicly reachable URL of the video file.
     * @param name Display name of the playback; `null` for none.
     * @param playId Your own ID for the playback. Needed for [stopVideoPlayback] and echoed in
     *               [MediaPlaybackStarted] and [MediaPlayback].
     * @param replacedUser A participant whose video the playback replaces. `null`, or a user no
     *                     longer in the meeting, plays it as an additional video.
     * @param audio `true` to play the video's audio too.
     * @param loopCount How often the video repeats: `0` plays it once, `-1` loops forever. H.264
     *                  videos are always played once.
     */
    public fun startVideoPlayback(
        url: String,
        name: String?,
        playId: String?,
        replacedUser: UserInfo?,
        audio: Boolean,
        loopCount: Int = 0,
    ) {
        eyesonMeetingScope.launch {
            val replacementId =
                userListMutex.withLock {
                    userInMeeting.filterValues {
                        it.id == replacedUser?.id
                    }.keys.firstOrNull()
                }

            val response = restCommunicator.videoPlayback(
                meeting?.accessKey ?: return@launch,
                url,
                name,
                playId,
                replacementId,
                audio,
                loopCount
            )

            emitEyesonEvent(
                MediaPlaybackStarted(
                    playId,
                    response.toMediaPlaybackResponse()
                )
            )
        }
    }

    /**
     * Stops a playback started with [startVideoPlayback].
     *
     * The request runs in the background and its result arrives as [MediaPlaybackStopped]. Does
     * nothing, and emits nothing, if there is no meeting.
     *
     * @param playId The `playId` the playback was started with.
     */
    public fun stopVideoPlayback(playId: String) {
        eyesonMeetingScope.launch {
            val response = restCommunicator.stopVideoPlayback(
                meeting?.accessKey ?: return@launch, playId
            )

            emitEyesonEvent(
                MediaPlaybackStopped(
                    playId,
                    response.toMediaPlaybackResponse()
                )
            )
        }
    }

    /**
     * Turns on the SDK's logcat output. Applies to the whole process, not just this instance.
     * Off by default in release builds of the SDK.
     */
    public fun enableLogging() {
        Logger.enabled = true
    }

    /**
     * Turns off the SDK's logcat output. Applies to the whole process, not just this instance.
     */
    public fun disableLogging() {
        Logger.enabled = false
    }

    /**
     * Whether the meeting uses the 16:9 layout.
     *
     * @return The value at join time; `false` if there is no meeting. Later changes arrive as
     *         [OptionsUpdated].
     */
    public fun isWidescreen(): Boolean {
        return meeting?.options?.widescreen ?: false
    }

    /**
     * The media the current meeting sends and receives.
     *
     * @return The [MediaOptions] passed to the join, `null` if there is no meeting. They are
     *         fixed for the whole meeting.
     */
    public fun getMediaOptions(): MediaOptions? {
        return mediaOptions
    }

    /**
     * The local participant as eyeson knows them.
     *
     * @return The local user, `null` if there is no meeting.
     */
    public fun getUserInfo(): UserInfo? {
        return meeting?.user?.toLocal(Date())
    }


    private fun startCall(
        meeting: MeetingDto,
        frontCamera: Boolean,
        local: VideoSink?,
        remote: VideoSink?,
        mediaOptions: MediaOptions,
        screenShareInfo: ScreenShareInfo?,
    ) {
        callLogic = CallLogic(
            meeting = meeting,
            context = application,
            audioOptions = audioOptions,
            videoOptions = videoOptions,
            screenShareOptions = screenShareOptions,
            videoCodecHardwareAcceleration = sdkOptions.videoCodecHardwareAcceleration,
            mediaOptions = mediaOptions
        ).apply {
            setLocalVideoTarget(local)
            setRemoteVideoTarget(remote)
            startCall(
                frontCamera,
                audioOnStart,
                videoOnStart,
                screenShareInfo?.mediaProjectionPermissionResultData,
                screenShareInfo?.notificationId,
                screenShareInfo?.notification
            )

            events.collectIn(eyesonMeetingScope) { command ->
                if (command !is ConnectionStatisticLocal) {
                    Logger.d("got Event: $command")
                }
                handleCallEvents(command, meeting)
            }
        }
    }

    private fun resumeCall(callId: String) {
        val sdp = callLogic?.getSdpForCallResume() ?: ""
        if (sdp.isBlank()) {
            return
        }

        webSocketCommunicator?.resumeCall(CallResume(callId, sdp))
    }

    private suspend fun handleCallEvents(
        command: LocalBaseCommand,
        meeting: MeetingDto,
    ) {
        when (command) {
            is CallStart -> {
                webSocketCommunicator?.startCall(command, videoOnStart, BuildConfig.SDK_VERSION)
            }

            is VoiceActivity -> {
                userListMutex.withLock {
                    if (!userInMeeting.containsKey(command.userId)) {
                        fetchUserInfo(meeting, listOf(command.userId))
                    }
                    emitEyesonEvent(
                        VoiceActivityChanged(
                            user = userInMeeting[command.userId] ?: return@withLock,
                            active = command.on
                        )
                    )
                }
            }

            is CallTerminated -> {
                leaveAndEmit(MeetingTerminated(CallTerminationReason.fromTerminationCode(command.terminateCode)))
            }

            is MeetingJoinedLocal -> {
                webSocketCommunicator?.setLocalVideoEnabled(videoOnStart)
                emitEyesonEvent(MeetingJoined)
            }

            is CameraSwitchDoneLocal -> {
                emitEyesonEvent(CameraSwitchDone(command.isFrontCamera))
            }

            is CameraSwitchErrorLocal -> {
                emitEyesonEvent(CameraSwitchError(command.error))
            }

            is ConnectionStatisticLocal -> {
                emitEyesonEvent(ConnectionStatisticUpdated(command.toLocal()))
            }

            is CameraOpenLocal -> {
                emitEyesonEvent(CameraOpened(command.cameraName ?: ""))
            }

            is CameraFirstFrameAvailableLocal -> {
                emitEyesonEvent(CameraFirstFrameAvailable)
            }

            is CameraClosedLocal -> {
                emitEyesonEvent(CameraClosed)
            }

            is CameraDisconnectedLocal -> {
                emitEyesonEvent(CameraDisconnected)
            }

            is CameraFrozenLocal -> {
                emitEyesonEvent(CameraFrozen(command.error ?: ""))
            }

            is CameraErrorLocal -> {
                emitEyesonEvent(CameraError(command.error ?: ""))
            }
        }
    }

    /**
     * Caller must hold [userListMutex]. Users whose lookup fails are left out, so the event that
     * needed them skips or omits them instead of tearing down the collector it runs in.
     */
    private suspend fun fetchUserInfo(
        meeting: MeetingDto,
        userIds: List<String>,
    ) {
        restCommunicator.getUserInfo(meeting.accessKey, userIds)
            .forEach {
                userInMeeting[it.signalingId] = it.user
            }
    }

    private suspend fun handleChatIncoming(meeting: MeetingDto, chat: ChatIncoming) {
        userListMutex.withLock {
            val id = fetchUserInfoBasedOnLegacyId(chat.userId, meeting)
            emitEyesonEvent(
                ChatMessageReceived(
                    user = userInMeeting[id] ?: return@withLock,
                    message = chat.content,
                    timestamp = chat.timestamp
                )
            )
        }
    }

    private suspend fun handleCustomMessageIncoming(
        meeting: MeetingDto,
        message: CustomMessageLocal,
    ) {
        userListMutex.withLock {
            val id = fetchUserInfoBasedOnLegacyId(message.userId, meeting)
            emitEyesonEvent(
                CustomMessageReceived(
                    user = userInMeeting[id] ?: return@withLock,
                    message = message.content,
                    timestamp = message.createdAt
                )
            )
        }
    }

    private suspend fun fetchUserInfoBasedOnLegacyId(userId: String, meeting: MeetingDto): String {
        // NOTE: Chat messages from COM- API contain legacy (SIP) user ids
        // e.g. 623b195ec77b9700102f380c@integrations.visocon.com
        var id = userId
        if (id.contains("@")) {
            id = id.substring(0, id.indexOf("@"))
        }
        if (!userInMeeting.containsKey(id)) {
            fetchUserInfo(meeting, listOf(id))
        }
        return id
    }

    // A null status code means the request got no response, see RestCommunicator.
    private fun Int?.toPresentationResponse(): PresentationResponse =
        this?.let { PresentationResponse.fromResponseCode(it) } ?: PresentationResponse.UNSPECIFIED

    private fun Int?.toMediaPlaybackResponse(): MediaPlaybackResponse =
        this?.let { MediaPlaybackResponse.fromResponseCode(it) }
            ?: MediaPlaybackResponse.UNSPECIFIED

    private fun checkForNeededPermissions(mediaOptions: MediaOptions): List<NeededPermissions> {
        return buildList {
            if (mediaOptions.sendAudio &&
                ContextCompat.checkSelfPermission(application, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_DENIED
            ) {
                add(NeededPermissions.RECORD_AUDIO)
            }

            if (mediaOptions.sendVideo &&
                ContextCompat.checkSelfPermission(application, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED
            ) {
                add(NeededPermissions.CAMERA)
            }
        }
    }

    private fun emitEyesonEvent(event: EyesonEvent) {
        if (!eventsEnabled.get()) {
            return
        }
        // Never fails: _events is configured with BufferOverflow.DROP_OLDEST.
        _events.tryEmit(event)
    }

    /**
     * Ends the meeting, then announces that it ended.
     *
     * Emitting after the teardown is what lets an app join again straight from its
     * [MeetingTerminated] or [MeetingJoinFailed] handler: [joined] is already released by the time
     * the event is delivered, so the join cannot come back as [JoinResult.AlreadyJoined].
     *
     * The event bypasses the gate [leave] just closed — it belongs to the meeting that ended, not
     * to the silence after it. Emitting does not suspend, so it also survives the coroutine
     * [leave] has just cancelled to get here.
     */
    private fun leaveAndEmit(event: EyesonEvent) {
        leave()
        // Never fails: _events is configured with BufferOverflow.DROP_OLDEST.
        _events.tryEmit(event)
    }

    public companion object {
        internal const val VIDEO_PLAYBACK_PREFIX = "media-"

        /**
         * Share group handle of the EGL context the SDK renders and encodes with.
         *
         * The SDK owns one EGL context per process. It is created on first use and never
         * released, so this is valid at any point in the life of the process: before the first
         * [com.eyeson.sdk.webrtc.VideoRenderer], without a meeting, and after the last one left.
         * Reading it from any thread is safe and never throws.
         *
         * An app that renders or encodes video of its own passes it to `EglBase.create` to put
         * its own [EglBase] into the same share group, which is what keeps zero copy rendering
         * between the app and the SDK:
         * ```
         * val appEglBase = EglBase.create(EyesonMeeting.eglBaseContext)
         * ```
         * The SDK is the root of that group unless an app takes the role with
         * [SdkOptions.eglBase], which most apps have no reason to do — this getter shares the
         * group without the call order that owning it requires.
         *
         * Reading is not free of consequence for that other route: it creates the context if
         * nothing has yet, so the first read makes [SdkOptions.eglBase] throw for the rest of the
         * process. An app that has to own the share group hands its [EglBase] in and never reads
         * this first.
         */
        @JvmStatic
        public val eglBaseContext: EglBase.Context
            get() = EyesonEglBase.context
    }
}

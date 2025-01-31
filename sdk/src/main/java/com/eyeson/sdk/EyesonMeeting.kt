package com.eyeson.sdk

import android.Manifest
import android.app.Activity
import android.app.Application
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.eyeson.sdk.callLogic.CallLogic
import com.eyeson.sdk.events.CallRejectionReason
import com.eyeson.sdk.events.CallTerminationReason
import com.eyeson.sdk.events.EyesonEventListener
import com.eyeson.sdk.events.MediaPlaybackResponse
import com.eyeson.sdk.events.NeededPermissions
import com.eyeson.sdk.events.PresentationResponse
import com.eyeson.sdk.exceptions.internal.FaultyInfoException
import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.local.api.MeetingInfo
import com.eyeson.sdk.model.local.api.PermalinkMeetingInfo
import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.base.LocalBaseCommand
import com.eyeson.sdk.model.local.call.CameraClosed
import com.eyeson.sdk.model.local.call.CameraDisconnected
import com.eyeson.sdk.model.local.call.CameraError
import com.eyeson.sdk.model.local.call.CameraFirstFrameAvailable
import com.eyeson.sdk.model.local.call.CameraFrozen
import com.eyeson.sdk.model.local.call.CameraOpen
import com.eyeson.sdk.model.local.call.CameraSwitchDone
import com.eyeson.sdk.model.local.call.CameraSwitchError
import com.eyeson.sdk.model.local.call.ConnectionStatistic
import com.eyeson.sdk.model.local.call.MeetingJoined
import com.eyeson.sdk.model.local.call.ResumeCallLocal
import com.eyeson.sdk.model.local.call.StartCallLocal
import com.eyeson.sdk.model.local.datachannel.VoiceActivity
import com.eyeson.sdk.model.local.meeting.BroadcastUpdate
import com.eyeson.sdk.model.local.meeting.CustomMessage
import com.eyeson.sdk.model.local.meeting.MeetingLocked
import com.eyeson.sdk.model.local.meeting.MuteLocalAudio
import com.eyeson.sdk.model.local.meeting.OptionsUpdate
import com.eyeson.sdk.model.local.meeting.Playback
import com.eyeson.sdk.model.local.meeting.PlaybackUpdate
import com.eyeson.sdk.model.local.meeting.PresentationUpdate
import com.eyeson.sdk.model.local.meeting.Recording
import com.eyeson.sdk.model.local.meeting.SnapshotUpdate
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
import com.eyeson.sdk.utils.Logger
import com.eyeson.sdk.utils.collectIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.webrtc.EglBase
import org.webrtc.SessionDescription
import org.webrtc.VideoSink
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The main class for interacting with the eyeson.com meeting service.
 *
 * This class provides methods to join, manage, and leave eyeson meetings, as well as
 * interact with meeting participants and the meeting environment.
 *
 * @param application The Android Application instance. Used for context and resource access.
 * @param experimentalFeatureStereo **Experimental.** A boolean indicating whether to enable experimental stereo audio features.
 *  This feature is under development and may not be stable. Use with caution. Defaults to `false`.
 * @param customApiUrl An optional custom API URL to override the default eyeson API endpoint.
 */
class EyesonMeeting(
    private val application: Application,
    private val experimentalFeatureStereo: Boolean = false,
    customApiUrl: String? = null,
) {
    private var eventListener: EyesonEventListener? = null

    private val eyesonMeetingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val nameLookupScope =
        CoroutineScope(
            SupervisorJob() + Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        )

    private var webSocketCommunicator: WebSocketCommunicator? = null
    private val restCommunicator by lazy { RestCommunicator() }

    private var callLogic: CallLogic? = null

    private var meeting: MeetingDto? = null
    private val joined = AtomicBoolean(false)
    private var audioOnStart = true
    private var videoOnStart = true

    private val userInMeeting = mutableMapOf<String, UserInfo>()
    private val userListMutex = Mutex()

    private val rootEglBase: EglBase = EglBase.create()

    init {
        if (customApiUrl != null) {
            API_URL = customApiUrl
        }
    }

    /**
     * Data class that encapsulates information related to screen sharing.
     *
     * This class holds the necessary data to start and manage a screen sharing session,
     * including the media projection permission result, a notification ID, and the
     * notification itself used to inform the user about the ongoing screen share.
     *
     * @property mediaProjectionPermissionResultData The Intent received as a result of the user granting
     *                                               permission for media projection. This Intent is
     *                                               crucial for starting the media projection session.
     *                                               It typically comes from `onActivityResult` after
     *                                               requesting `MediaProjection` permission.
     * @property notificationId The unique ID associated with the notification that indicates the screen
     *                           sharing session is active. This ID is used to update or cancel the
     *                           notification later.
     * @property notification The Notification object displayed to the user while screen sharing is in
     *                        progress. This notification usually informs the user that their screen
     *                        is being shared and might provide controls to stop the sharing.
     */
    data class ScreenShareInfo(
        val mediaProjectionPermissionResultData: Intent,
        val notificationId: Int,
        val notification: Notification,
    )

    /**
     * Join an Eyeson meeting.
     *
     * This function initiates the process of connecting to an Eyeson meeting identified by the provided `accessKey`.
     *
     * @param accessKey The access key for the Eyeson meeting.
     * @param frontCamera `true` if the front camera should be used; `false` for the rear camera.
     * @param audioOnly `true` if the meeting should be audio-only (no video); `false` to enable video.
     * @param local A [VideoSink] to display the local video feed. Can be `null` if local video is not needed.
     * @param remote A [VideoSink] to display the remote video feed. Can be `null` if remote video is not needed.
     * @param eventListener An [EyesonEventListener] to receive events related to the meeting, such as connection status, participant changes, etc.
     * @param microphoneEnabledOnStart `true` if the microphone should be enabled when the meeting starts; `false` otherwise. Defaults to `true`.
     * @param videoEnabledOnStart `true` if the video should be enabled when the meeting starts; `false` otherwise. Defaults to `true`.
     * @param screenShareInfo Optional [ScreenShareInfo] to provide screen sharing configuration.
     *                        If provided, the meeting will start with screen sharing active.
     *                        Use `null` if no screen share is needed.
     *
     */
    fun join(
        accessKey: String,
        frontCamera: Boolean,
        audioOnly: Boolean,
        local: VideoSink?,
        remote: VideoSink?,
        eventListener: EyesonEventListener,
        microphoneEnabledOnStart: Boolean = true,
        videoEnabledOnStart: Boolean = true,
        screenShareInfo: ScreenShareInfo? = null,
    ) {
        this.eventListener = eventListener
        joinMeeting(
            { restCommunicator.getMeetingInfo(accessKey) },
            frontCamera,
            audioOnly,
            local,
            remote,
            microphoneEnabledOnStart,
            videoEnabledOnStart,
            screenShareInfo
        )
    }

    /**
     * Join an Eyeson meeting as a guest user.
     *
     * It uses a provided guest token to authenticate and retrieve meeting details.
     * The guest token can either bei obtained from a running meeting or from a permalink
     *
     * @param guestToken The guest token required to join the meeting.
     * @param name The name of the guest user, which will be displayed to other participants in the meeting.
     * @param id An optional identifier for the guest user. Can be null if no specific ID is needed.
     * @param avatar An optional URL to an avatar image for the guest user. Can be null if no avatar is desired.
     * @param frontCamera `true` if the front camera should be used; `false` for the rear camera.
     * @param audioOnly `true` if the meeting should be audio-only (no video); `false` to enable video.
     * @param local A [VideoSink] to display the local video feed. Can be `null` if local video is not needed.
     * @param remote A [VideoSink] to display the remote video feed. Can be `null` if remote video is not needed.
     * @param eventListener An [EyesonEventListener] to receive events related to the meeting, such as connection status, participant changes, etc.
     * @param microphoneEnabledOnStart `true` if the microphone should be enabled when the meeting starts; `false` otherwise. Defaults to `true`.
     * @param videoEnabledOnStart `true` if the video should be enabled when the meeting starts; `false` otherwise. Defaults to `true`.
     * @param screenShareInfo Optional [ScreenShareInfo] to provide screen sharing configuration.
     *                        If provided, the meeting will start with screen sharing active.
     *                        Use `null` if no screen share is needed.
     */
    fun joinAsGuest(
        guestToken: String,
        name: String,
        id: String?,
        avatar: String?,
        frontCamera: Boolean,
        audioOnly: Boolean,
        local: VideoSink?,
        remote: VideoSink?,
        eventListener: EyesonEventListener,
        microphoneEnabledOnStart: Boolean = true,
        videoEnabledOnStart: Boolean = true,
        screenShareInfo: ScreenShareInfo? = null,
    ) {
        this.eventListener = eventListener
        joinMeeting(
            {
                restCommunicator.getMeetingInfoAsGuest(
                    guestToken,
                    name,
                    id,
                    avatar
                )
            },
            frontCamera,
            audioOnly,
            local,
            remote,
            microphoneEnabledOnStart,
            videoEnabledOnStart,
            screenShareInfo
        )
    }

    /**
     * Connects to an Eyeson meeting using a permalink.
     *
     * Initiates a connection to an Eyeson meeting identified by a permalink `user_token`.
     *
     * @param userToken The user token required for authentication with the Eyeson service.
     *                  This token is obtained from the Eyeson API.
     * @param frontCamera `true` if the front camera should be used; `false` for the rear camera.
     * @param audioOnly `true` if the meeting should be audio-only (no video); `false` to enable video.
     * @param local A [VideoSink] to display the local video feed. Can be `null` if local video is not needed.
     * @param remote A [VideoSink] to display the remote video feed. Can be `null` if remote video is not needed.
     * @param eventListener An [EyesonEventListener] to receive events related to the meeting, such as connection status, participant changes, etc.
     * @param microphoneEnabledOnStart `true` if the microphone should be enabled when the meeting starts; `false` otherwise. Defaults to `true`.
     * @param videoEnabledOnStart `true` if the video should be enabled when the meeting starts; `false` otherwise. Defaults to `true`.
     * @param screenShareInfo Optional [ScreenShareInfo] to provide screen sharing configuration.
     *                        If provided, the meeting will start with screen sharing active.
     *                        Use `null` if no screen share is needed.
     */
    fun connectPermalink(
        userToken: String,
        frontCamera: Boolean,
        audioOnly: Boolean,
        local: VideoSink?,
        remote: VideoSink?,
        eventListener: EyesonEventListener,
        microphoneEnabledOnStart: Boolean = true,
        videoEnabledOnStart: Boolean = true,
        screenShareInfo: ScreenShareInfo? = null,
    ) {
        this.eventListener = eventListener
        joinMeeting(
            { restCommunicator.startPermalinkMeeting(userToken) },
            frontCamera,
            audioOnly,
            local,
            remote,
            microphoneEnabledOnStart,
            videoEnabledOnStart,
            screenShareInfo
        )
    }

    /**
     * Retrieves permalink meeting information using a given token.
     *
     * This function interacts with the REST API to fetch details about a meeting
     * associated with a specific permalink.
     *
     * @param token The permalink token used to identify the meeting.
     * @return A [PermalinkMeetingInfo] object containing the details of the meeting
     *         if the request is successful. Returns `null` if the meeting information
     *         cannot be retrieved, either due to an invalid token, network issues, or
     *         other server-side problems.
     */
    suspend fun getPermalinkMeetingInfo(token: String): PermalinkMeetingInfo? {
        return restCommunicator.getPermalinkMeetingInfo(token)
    }

    private fun joinMeeting(
        meetingInfoRequest: suspend () -> MeetingDto,
        frontCamera: Boolean,
        audiOnly: Boolean,
        local: VideoSink?,
        remote: VideoSink?,
        microphoneEnabledOnStart: Boolean = true,
        videoEnabledOnStart: Boolean = true,
        screenShareInfo: ScreenShareInfo?,
    ) {
        if (joined.getAndSet(true)) {
            return
        }
        val neededPermissions = checkForNeededPermissions(audiOnly, application)
        if (neededPermissions.isNotEmpty()) {
            eventListener?.onPermissionsNeeded(neededPermissions)
            return
        }
        audioOnStart = microphoneEnabledOnStart
        videoOnStart = videoEnabledOnStart

        eyesonMeetingScope.launch {
            val meetingInfo = try {
                meetingInfoRequest()
            } catch (e: Exception) {
                when (e) {
                    is CancellationException -> {
                        throw e
                    }

                    is FaultyInfoException -> {
                        eventListener?.onMeetingJoinFailed(CallRejectionReason.fromRejectCode(e.code))
                        return@launch
                    }

                    else -> {
                        eventListener?.onMeetingJoinFailed(CallRejectionReason.ERROR)
                        return@launch
                    }
                }
            }
            meeting = meetingInfo

            when (val meetingInfoParsed = getMeetingInfo()) {
                null -> {
                    terminateCallWithError()
                    return@launch
                }

                else -> {
                    eventListener?.onMeetingJoining(
                        meetingInfoParsed
                    )
                }
            }

            webSocketCommunicator = WebSocketCommunicator(meetingInfo).apply {
                connect()

                events.collectIn(eyesonMeetingScope) { command ->
                    handleWebSocketEvents(
                        command,
                        audiOnly,
                        frontCamera,
                        local,
                        remote,
                        screenShareInfo
                    )
                }
            }
        }
    }


    /**
     * Starts screen sharing.
     *
     * This function initiates the screen sharing process using the provided [screenShareInfo].
     * It leverages the underlying call logic to handle the actual screen sharing functionality.
     *
     * @param screenShareInfo [ScreenShareInfo] Information required for screen sharing, including media projection
     *                        permission data, notification details, etc.
     * @param asPresentation Boolean flag indicating whether the screen share should be treated as
     *                       a presentation. If `true`, the shared video will be set as the presentation.
     * @return `true` if the screen sharing was successfully started, `false` otherwise.
     *
     */
    fun startScreenShare(
        screenShareInfo: ScreenShareInfo,
        asPresentation: Boolean,
    ): Boolean {
        return callLogic?.startScreenShare(
            screenShareInfo.mediaProjectionPermissionResultData,
            asPresentation,
            screenShareInfo.notificationId,
            screenShareInfo.notification
        ) {
            if (asPresentation) {
                setVideoAsPresentation()
            }
        } ?: false
    }

    /**
     * Stops the current screen share session.
     *
     * This function halts the ongoing screen sharing process.
     *
     * @param resumeLocalVideo `true` if the local video feed should be resumed after
     *                         stopping the screen share; `false` otherwise. If set to
     *                         true, the camera will be turned back on if it was on
     *                         before the screen share started. If false, the local
     *                         video will remain off even after the screen share stops.
     */
    fun stopScreenShare(resumeLocalVideo: Boolean) {
        callLogic?.stopScreenShare(resumeLocalVideo)
    }

    /**
     * Checks if screen sharing is currently active in the ongoing call.
     *
     * @return `true` if screen sharing is active, `false` otherwise.
     */
    fun isScreenShareActive(): Boolean {
        return callLogic?.isScreenShareActive() ?: false
    }

    /**
     * Sets the current video as a presentation in the Eyeson meeting.
     */
    fun setVideoAsPresentation() {
        eyesonMeetingScope.launch {
            val response = restCommunicator.startPresentation(meeting?.accessKey ?: return@launch)
            eventListener?.onPresentationStartResponse(
                PresentationResponse.fromResponseCode(response)
            )
        }
    }

    /**
     * Stops the current presentation in the Eyeson meeting.
     *
     * **Note**
     * Stops any presentation in progress, regardless of who started it.
     */
    fun stopPresentation() {
        eyesonMeetingScope.launch {
            val response = restCommunicator.stopPresentation(meeting?.accessKey ?: return@launch)
            eventListener?.onPresentationStopResponse(
                PresentationResponse.fromResponseCode(response)
            )
        }
    }

    private suspend fun handleWebSocketEvents(
        command: LocalBaseCommand,
        audiOnly: Boolean,
        frontCamera: Boolean,
        local: VideoSink?,
        remote: VideoSink?,
        screenShareInfo: ScreenShareInfo?,
    ) {
        when (command) {
            is StartCallLocal -> {
                meeting = command.meeting
                startCall(command.meeting, audiOnly, frontCamera, local, remote, screenShareInfo)
            }

            is ResumeCallLocal -> {
                resumeCall(command.callId)
            }

            is CallAccepted -> {
                callLogic?.setRemoteDescription(
                    command.sdp,
                    SessionDescription.Type.ANSWER
                )
                eventListener?.onStreamingModeChanged(callLogic?.isSfuMode() ?: return)
            }

            is CallResumed -> {
                callLogic?.setRemoteDescription(
                    command.sdp,
                    SessionDescription.Type.OFFER
                )
                eventListener?.onStreamingModeChanged(callLogic?.isSfuMode() ?: return)
            }

            is CallRejected -> {
                eventListener?.onMeetingJoinFailed(CallRejectionReason.fromRejectCode(command.rejectCode))
                leave()
            }

            is CallTerminated -> {
                eventListener?.onMeetingTerminated(CallTerminationReason.fromTerminationCode(command.terminateCode))
                leave()
            }

            is SdpUpdate -> {
                callLogic?.setRemoteDescription(
                    command.sdp,
                    SessionDescription.Type.OFFER
                )
                eventListener?.onStreamingModeChanged(callLogic?.isSfuMode() ?: return)
            }

            is ChatIncoming -> {
                handleChatIncoming(meeting ?: return, command)
            }

            is CustomMessage -> {
                handleCustomMessageIncoming(meeting ?: return, command)
            }

            is MuteLocalAudio -> {
                setMicrophoneEnabled(false)
                eventListener?.onAudioMutedBy(command.byUser)
            }

            is MeetingLocked -> {
                eventListener?.onMeetingLocked(command.locked)
            }

            is PlaybackUpdate -> {
                withContext(nameLookupScope.coroutineContext) {
                    val infoNeededFor = command.playing.filterNot {
                        it.replacementId != null && userInMeeting.containsKey(it.replacementId) || userInMeeting.values.any { userInfo -> userInfo.id == it.replacementId }
                    }.mapNotNull {
                        it.replacementId
                    }

                    if (infoNeededFor.isNotEmpty()) {
                        fetchUserInfo(meeting ?: return@withContext, infoNeededFor)
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
                    eventListener?.onMediaPlayback(event)
                }
            }

            is BroadcastUpdate -> {
                eventListener?.onBroadcastUpdate(command)
            }

            is Recording -> {
                eventListener?.onRecordingUpdate(command)
            }

            is SnapshotUpdate -> {
                eventListener?.onSnapshotUpdate(command)
            }

            is WsFailure -> {
                eventListener?.onMeetingTerminated(
                    CallTerminationReason.fromTerminationCode(
                        command.response?.code ?: CallTerminationReason.ERROR.terminationCode
                    )
                )
                leave()
            }

            is WsClosed -> {
                eventListener?.onMeetingTerminated(CallTerminationReason.OK)
                leave()
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
                withContext(nameLookupScope.coroutineContext) {
                    val infoNeededFor = command.added.filterNot {
                        userInMeeting.containsKey(it)
                    }

                    if (infoNeededFor.isNotEmpty()) {
                        fetchUserInfo(meeting ?: return@withContext, infoNeededFor)

                    }

                    if (command.added.isNotEmpty()) {
                        eventListener?.onUserJoinedMeeting(
                            userInMeeting.filter { command.added.contains(it.key) }
                                .values.toSet().toList()
                        )
                    }

                    if (command.deleted.isNotEmpty()) {
                        eventListener?.onUserLeftMeeting(
                            userInMeeting.filter { command.deleted.contains(it.key) }
                                .values.toSet().toList()
                        )
                    }

                    command.deleted.forEach {
                        userListMutex.withLock {
                            userInMeeting.remove(it)
                        }
                    }
                    eventListener?.onUserListUpdate(
                        userInMeeting.values.toSet().toList(),
                        command.mediaPlayIds
                    )
                }
            }

            is SourceUpdate -> {
                withContext(nameLookupScope.coroutineContext) {
                    val infoNeeded = command.sources.filterNot {
                        userInMeeting.containsKey(it) || it.contains(VIDEO_PLAYBACK_PREFIX)
                    }
                    if (infoNeeded.isNotEmpty()) {
                        fetchUserInfo(meeting ?: return@withContext, infoNeeded)
                    }

                    val videoSourceIds =
                        command.sources.slice(command.videSources.filter { it >= 0 })

                    eventListener?.onVideoSourceUpdate(
                        userInMeeting.filter {
                            videoSourceIds.contains(it.key)
                        }.values.toSet().toList(),
                        if (command.desktopStreamingId == null || command.desktopStreamingId == -1) {
                            null
                        } else {
                            userInMeeting[command.sources[command.desktopStreamingId]]
                        }
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

            is PresentationUpdate -> {
                eventListener?.onPresentationUpdate(command)
            }

            is OptionsUpdate -> {
                eventListener?.onOptionsUpdate(command.meetingOptions)
            }
        }
    }

    private fun terminateCallWithError() {
        eventListener?.onMeetingTerminated(CallTerminationReason.ERROR)
        leave()
    }

    /**
     * Leaves the current Eyeson meeting.
     */
    fun leave() {
        eyesonMeetingScope.cancel()
        nameLookupScope.cancel()

        webSocketCommunicator?.terminateCall()
        webSocketCommunicator = null

        callLogic?.disconnectCall()
        callLogic = null

        eventListener = null
        meeting = null
        joined.set(false)
    }


    /**
     * Retrieves information about the current meeting, if available.

     * @return [MeetingInfo] containing the meeting details, or null if no meeting is active.
     */
    fun getMeetingInfo(): MeetingInfo? {
        return meeting?.let { meetingInfo ->
            MeetingInfo(
                accessKey = meetingInfo.accessKey,
                name = meetingInfo.room.name,
                startedAt = meetingInfo.room.startedAt,
                user = meetingInfo.user.toLocal(Date()),
                locked = meetingInfo.locked,
                guestToken = meetingInfo.room.guestToken,
                guestLink = meetingInfo.links.guestJoin,
                activeRecording = meetingInfo.recording?.toLocal(),
                activeBroadcasts = BroadcastUpdateDto("", meetingInfo.broadcasts).toLocal(),
                snapshots = SnapshotUpdateDto("", meetingInfo.snapshots).toLocal(),
                meetingOptions = meetingInfo.options.toLocal()
            )
        }
    }

    /**
     * Sets the target for the local video stream.
     *
     * This function allows you to specify where the local video feed should be rendered or processed.
     *
     * @param target The [VideoSink] to which the local video stream should be directed.
     *               Pass `null` to remove the current video target and stop rendering/processing the local video.
     *               If a non-null target is provided, the local video stream will be rendered or processed by this target.
     *
     * Example Usage:
     *
     * ```kotlin
     * // Assuming you have a VideoSink instance named 'myVideoSink'
     * setLocalVideoTarget(myVideoSink) // Set the target for local video to myVideoSink
     *
     * // Remove the local video target
     * setLocalVideoTarget(null)
     * ```
     *
     */
    fun setLocalVideoTarget(target: VideoSink?) {
        callLogic?.setLocalVideoTarget(target)
    }

    /**
     * Sets the target for the remote video stream.
     *
     * This function allows you to specify where the remote video feed should be rendered or processed.
     *
     * @param target The [VideoSink] to which the remote video stream should be directed.
     *               Pass `null` to remove the current video target and stop rendering/processing the remote video.
     *               If a non-null target is provided, the remote video stream will be rendered or processed by this target.
     *
     * Example Usage:
     *
     * ```kotlin
     * // Assuming you have a VideoSink instance named 'myVideoSink'
     * setRemoteVideoTarget(myVideoSink) // Set the target for remote video to myVideoSink
     *
     * // Remove the remote video target
     * setRemoteVideoTarget(null)
     * ```
     *
     */
    fun setRemoteVideoTarget(target: VideoSink?) {
        callLogic?.setRemoteVideoTarget(target)
    }

    /**
     * Enables or disables the local video stream.
     *
     * This function controls whether the local user's video is sent to the remote participants.
     *
     * @param enable `true` to enable the local video stream, `false` to disable it.
     *
     */
    fun setVideoEnabled(enable: Boolean) {
        webSocketCommunicator?.setLocalVideoEnabled(enable)
        callLogic?.setLocalVideoEnabled(enable)
    }

    /**
     * Checks if the local video stream is currently enabled.
     *
     * @return `true` if the local video stream is active, `false` otherwise.
     */
    fun isVideoEnabled(): Boolean {
        return callLogic?.isLocalVideoActive() ?: false
    }

    /**
     * Switches to the next available camera.
     *
     * This function cycles through the available cameras (e.g., front, rear, potentially others)
     * and switches the active camera being used for the call to the next one in the sequence.
     *
     */
    fun switchCamera() {
        callLogic?.switchCamera()
    }

    /**
     * Switches the active camera to the specified camera ID.
     *
     * This function delegates the camera switching operation to the underlying call logic.
     * It is used to change the active camera being used during a call or video session.
     *
     * @param cameraId The ID of the camera to switch to. [CameraManager.getCameraIdList]
     *
     */
    fun switchCameraTo(cameraId: String) {
        callLogic?.switchCameraTo(cameraId)
    }

    /**
     * Checks if the currently active camera is the front-facing camera.
     *
     *
     * @return `true` if the front camera is active, `false` if a different camera is active,
     *         and `null` if there is no active meeting.
     */
    fun isFrontCamera(): Boolean? {
        return callLogic?.isFrontCamera()
    }

    /**
     * Mutes other participants in the meeting.
     *
     */
    fun sendMuteOthers() {
        webSocketCommunicator?.sendMuteAll()
    }

    /**
     * Enables or disables the microphone.
     *
     * @param enable `true` to enable the microphone, `false` to disable it.
     */
    fun setMicrophoneEnabled(enable: Boolean) {
        callLogic?.setAudioEnabled(enable)
    }

    /**
     * Checks if the microphone is currently enabled for audio input.
     *
     * @return `true` if the microphone is enabled for audio input; `false` otherwise,
     *
     */
    fun isMicrophoneEnabled(): Boolean {
        return callLogic?.isAudioEnabled() ?: false
    }

    /**
     * Sends a chat message to the Eyeson meeting.
     *
     * This function asynchronously sends a chat message to the active Eyeson meeting.
     *
     * @param message The chat message string to be sent.
     *
     */
    fun sendChatMessage(message: String) {
        eyesonMeetingScope.launch {
            restCommunicator.sendChatMessage(meeting?.accessKey ?: return@launch, message)
        }
    }

    /**
     * Sends a custom message to the Eyeson meeting.
     *
     * This function asynchronously sends a user-defined message to all participants in the  meeting.
     *
     * @param content The string content of the custom message to be sent.
     *
     */
    fun sendCustomMessage(content: String) {
        eyesonMeetingScope.launch {
            restCommunicator.sendCustomMessage(meeting?.accessKey ?: return@launch, content)
        }
    }

    /**
     * Starts video playback in the Eyeson meeting.
     *
     * This function initiates the playback of a video from a given URL within the ongoing Eyeson meeting.
     *
     * @param url The URL of the video to be played back. This is a mandatory parameter.
     * @param name An optional name for the video playback. This can be used to identify the playback in the meeting.
     * @param playId An optional ID for the playback. This can be used to reference the playback later.
     * @param replacedUser An optional UserInfo object representing the user to be replaced by this video playback.
     *                     If provided, the video will replace the specified user's stream in the meeting.
     *                     If null, it will be a new video stream.
     * @param audio A boolean indicating whether the video's audio should be played. True to play audio, false otherwise.
     * @param loopCount The number of times the video should loop. Defaults to 0, meaning no looping (play once).
     *                  Use -1 to loop infinitely. Will **NOT** work if video codec is h264 -> always played once
     *
     */
    fun startVideoPlayback(
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

            eventListener?.onMediaPlaybackStartResponse(
                playId,
                MediaPlaybackResponse.fromResponseCode(response)
            )
        }
    }

    /**
     * Stops the playback of a previously started video.
     *
     * This function initiates the process of stopping video playback identified by the given `playId`.
     *
     * @param playId The unique identifier of the video playback to stop. This ID should have
     *               been received during the start of the video playback.
     *
     */
    fun stopVideoPlayback(playId: String) {
        eyesonMeetingScope.launch {
            val response = restCommunicator.stopVideoPlayback(
                meeting?.accessKey ?: return@launch, playId
            )

            eventListener?.onMediaPlaybackStopResponse(
                playId,
                MediaPlaybackResponse.fromResponseCode(response)
            )
        }
    }

    /**
     * Returns the EGL context associated with the root EGL base.
     *
     * This function provides access to the underlying EGL context that has been
     * initialized for the root EGL base.  This context can be used for sharing
     * resources and rendering contexts between different components that utilize
     * the same root EGL environment.
     *
     * @return The EGL context as an `EglBase.Context` object, or `null` if the root
     *         EGL base has not been initialized or if there is no associated context.
     *         Note that if the root EGL Base is not initialized, it will likely be null.
     */
    fun getEglContext(): EglBase.Context? {
        return rootEglBase.eglBaseContext
    }

    /**
     * Enables logging for the SDK.
     *
     */
    fun enabledLogging() {
        Logger.enabled = true
    }

    /**
     * Disables logging for the SDK.
     *
     */
    fun disableLogging() {
        Logger.enabled = false
    }

    /**
     * Checks if the current meeting is in widescreen mode.
     *
     * @return `true` if the meeting is in widescreen mode, `false` otherwise.
     */
    fun isWidescreen(): Boolean {
        return meeting?.options?.widescreen ?: false
    }

    /**
     * Retrieves the user information associated with the current meeting.
     *
     * This function accesses the `meeting` object (assumed to be a property of the class
     * this function belongs to) and attempts to extract the user information.
     * If a user is associated with the meeting, it converts the user data to a
     * local `UserInfo` object using the `toLocal` function, providing the current date.
     *
     * @return [UserInfo] if available, otherwise `null`.
     *
     */
    fun getUserInfo(): UserInfo? {
        return meeting?.user?.toLocal(Date())
    }

    private fun startCall(
        meeting: MeetingDto,
        audiOnly: Boolean,
        frontCamera: Boolean,
        local: VideoSink?,
        remote: VideoSink?,
        screenShareInfo: ScreenShareInfo?,
    ) {
        callLogic = CallLogic(
            meeting,
            audiOnly,
            application,
            rootEglBase,
            experimentalFeatureStereo
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
                if (command !is ConnectionStatistic) {
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
                withContext(nameLookupScope.coroutineContext) {
                    if (!userInMeeting.containsKey(command.userId)) {
                        fetchUserInfo(meeting, listOf(command.userId))
                    }
                    eventListener?.onVoiceActivity(
                        user = userInMeeting[command.userId] ?: return@withContext,
                        active = command.on
                    )
                }
            }

            is CallTerminated -> {
                eventListener?.onMeetingTerminated(CallTerminationReason.fromTerminationCode(command.terminateCode))
                leave()
            }

            is MeetingJoined -> {
                webSocketCommunicator?.setLocalVideoEnabled(videoOnStart)
                eventListener?.onMeetingJoined(getMeetingInfo() ?: return)
            }

            is CameraSwitchDone -> {
                eventListener?.onCameraSwitchDone(command.isFrontCamera)
            }

            is CameraSwitchError -> {
                eventListener?.onCameraSwitchError(command.error)
            }

            is ConnectionStatistic -> {
                eventListener?.onConnectionStatisticUpdate(command)
            }

            is CameraOpen -> {
                eventListener?.onCameraOpen(command.cameraName ?: "")
            }

            is CameraFirstFrameAvailable -> {
                eventListener?.onCameraFirstFrameAvailable()
            }

            is CameraClosed -> {
                eventListener?.onCameraClosed()
            }

            is CameraDisconnected -> {
                eventListener?.onCameraDisconnected()
            }

            is CameraFrozen -> {
                eventListener?.onCameraFrozen(command.error ?: "")
            }

            is CameraError -> {
                eventListener?.onCameraError(command.error ?: "")
            }
        }
    }

    private suspend fun fetchUserInfo(
        meeting: MeetingDto,
        userIds: List<String>,
    ) {
        restCommunicator.getUserInfo(meeting.accessKey, userIds)
            .forEach {
                userListMutex.withLock {
                    userInMeeting[it.signalingId] = it.user
                }
            }
    }

    private suspend fun handleChatIncoming(meeting: MeetingDto, chat: ChatIncoming) {
        withContext(nameLookupScope.coroutineContext) {
            val id = fetchUserInfoBasedOnLegacyId(chat.userId, meeting)

            eventListener?.onChatMessageReceived(
                user = userInMeeting[id] ?: return@withContext,
                message = chat.content,
                timestamp = chat.timestamp
            )
        }
    }

    private suspend fun handleCustomMessageIncoming(meeting: MeetingDto, message: CustomMessage) {
        withContext(nameLookupScope.coroutineContext) {
            val id = fetchUserInfoBasedOnLegacyId(message.userId, meeting)

            eventListener?.onCustomMessageReceived(
                user = userInMeeting[id] ?: return@withContext,
                message = message.content,
                timestamp = message.createdAt
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

    private fun checkForNeededPermissions(
        audiOnly: Boolean,
        context: Context,
    ): List<NeededPermissions> {
        val neededPermissions = mutableListOf<NeededPermissions>()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_DENIED
        ) {
            neededPermissions.add(NeededPermissions.RECORD_AUDIO)
        }

        if (!audiOnly
            && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_DENIED
        ) {
            neededPermissions.add(NeededPermissions.CAMERA)
        }
        return neededPermissions
    }

    companion object {
        internal var API_URL = BuildConfig.API_URL
        internal const val VIDEO_PLAYBACK_PREFIX = "media-"
    }
}

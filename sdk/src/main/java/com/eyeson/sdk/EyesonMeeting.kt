package com.eyeson.sdk

import android.Manifest
import android.app.Application
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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

    data class ScreenShareInfo(
        val mediaProjectionPermissionResultData: Intent,
        val notificationId: Int,
        val notification: Notification,
    )

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

    fun stopScreenShare(resumeLocalVideo: Boolean) {
        callLogic?.stopScreenShare(resumeLocalVideo)
    }

    fun isScreenShareActive(): Boolean {
        return callLogic?.isScreenShareActive() ?: false
    }

    fun setVideoAsPresentation() {
        eyesonMeetingScope.launch {
            val response = restCommunicator.startPresentation(meeting?.accessKey ?: return@launch)
            eventListener?.onPresentationStartResponse(
                PresentationResponse.fromResponseCode(response)
            )
        }
    }

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

    fun setLocalVideoTarget(target: VideoSink?) {
        callLogic?.setLocalVideoTarget(target)
    }

    fun setRemoteVideoTarget(target: VideoSink?) {
        callLogic?.setRemoteVideoTarget(target)
    }

    fun setVideoEnabled(enable: Boolean) {
        webSocketCommunicator?.setLocalVideoEnabled(enable)
        callLogic?.setLocalVideoEnabled(enable)
    }

    fun isVideoEnabled(): Boolean {
        return callLogic?.isLocalVideoActive() ?: false
    }

    fun switchCamera() {
        callLogic?.switchCamera()
    }

    fun isFrontCamera(): Boolean? {
        return callLogic?.isFrontCamera()
    }

    fun sendMuteOthers() {
        webSocketCommunicator?.sendMuteAll()
    }

    fun setMicrophoneEnabled(enable: Boolean) {
        callLogic?.setAudioEnabled(enable)
    }

    fun isMicrophoneEnabled(): Boolean {
        return callLogic?.isAudioEnabled() ?: false
    }

    fun sendChatMessage(message: String) {
        eyesonMeetingScope.launch {
            restCommunicator.sendChatMessage(meeting?.accessKey ?: return@launch, message)
        }
    }

    fun sendCustomMessage(content: String) {
        eyesonMeetingScope.launch {
            restCommunicator.sendCustomMessage(meeting?.accessKey ?: return@launch, content)
        }
    }

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

    fun getEglContext(): EglBase.Context? {
        return rootEglBase.eglBaseContext
    }

    fun enabledLogging() {
        Logger.enabled = true
    }

    fun disableLogging() {
        Logger.enabled = false
    }

    fun isWidescreen(): Boolean {
        return meeting?.options?.widescreen ?: false
    }

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

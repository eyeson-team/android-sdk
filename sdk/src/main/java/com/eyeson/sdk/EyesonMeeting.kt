package com.eyeson.sdk

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.eyeson.sdk.callLogic.CallLogic
import com.eyeson.sdk.events.CallRejectionReason
import com.eyeson.sdk.events.CallTerminationReason
import com.eyeson.sdk.events.EyesonEventListener
import com.eyeson.sdk.events.NeededPermissions
import com.eyeson.sdk.exceptions.internal.FaultyInfoException
import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.base.LocalBaseCommand
import com.eyeson.sdk.model.local.call.CameraSwitchDone
import com.eyeson.sdk.model.local.call.CameraSwitchError
import com.eyeson.sdk.model.local.call.ConnectionStatistic
import com.eyeson.sdk.model.local.call.MeetingJoined
import com.eyeson.sdk.model.local.call.ResumeCallLocal
import com.eyeson.sdk.model.local.call.StartCallLocal
import com.eyeson.sdk.model.local.datachannel.ChatIncoming
import com.eyeson.sdk.model.local.datachannel.MemberListUpdate
import com.eyeson.sdk.model.local.datachannel.RecordingStatusUpdate
import com.eyeson.sdk.model.local.datachannel.SourceUpdate
import com.eyeson.sdk.model.local.datachannel.VoiceActivity
import com.eyeson.sdk.model.local.meeting.BroadcastUpdate
import com.eyeson.sdk.model.local.meeting.MeetingLocked
import com.eyeson.sdk.model.local.meeting.MuteLocalAudio
import com.eyeson.sdk.model.local.meeting.PlaybackUpdate
import com.eyeson.sdk.model.local.meeting.Recording
import com.eyeson.sdk.model.local.meeting.SnapshotUpdate
import com.eyeson.sdk.model.local.sepp.CallAccepted
import com.eyeson.sdk.model.local.sepp.CallRejected
import com.eyeson.sdk.model.local.sepp.CallResume
import com.eyeson.sdk.model.local.sepp.CallResumed
import com.eyeson.sdk.model.local.sepp.CallStart
import com.eyeson.sdk.model.local.sepp.CallTerminated
import com.eyeson.sdk.model.local.sepp.SdpUpdate
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
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class EyesonMeeting(
    private val eventListener: EyesonEventListener,
    private val application: Application
) {
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

    fun join(
        accessKey: String,
        frontCamera: Boolean,
        audiOnly: Boolean,
        local: VideoSink?,
        remote: VideoSink?,
        microphoneEnabledOnStart: Boolean = true,
        videoEnabledOnStart: Boolean = true
    ) {
        joinMeeting(
            { restCommunicator.getMeetingInfo(accessKey) },
            frontCamera,
            audiOnly,
            local,
            remote,
            microphoneEnabledOnStart,
            videoEnabledOnStart
        )
    }

    fun joinAsGuest(
        guestToken: String,
        name: String,
        id: String?,
        avatar: String?,
        frontCamera: Boolean,
        audiOnly: Boolean,
        local: VideoSink?,
        remote: VideoSink?,
        microphoneEnabledOnStart: Boolean = true,
        videoEnabledOnStart: Boolean = true
    ) {
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
            audiOnly,
            local,
            remote,
            microphoneEnabledOnStart,
            videoEnabledOnStart
        )
    }

    private fun joinMeeting(
        meetingInfoRequest: suspend () -> MeetingDto,
        frontCamera: Boolean,
        audiOnly: Boolean,
        local: VideoSink?,
        remote: VideoSink?,
        microphoneEnabledOnStart: Boolean = true,
        videoEnabledOnStart: Boolean = true
    ) {
        if (joined.getAndSet(true)) {
            return
        }
        val neededPermissions = checkForNeededPermissions(audiOnly, application)
        if (neededPermissions.isNotEmpty()) {
            eventListener.onPermissionsNeeded(neededPermissions)
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
                        eventListener.onMeetingJoinFailed(CallRejectionReason.fromRejectCode(e.code))
                        return@launch
                    }
                    else -> {
                        eventListener.onMeetingJoinFailed(CallRejectionReason.ERROR)
                        return@launch
                    }
                }
            }
            meeting = meetingInfo

            eventListener.onMeetingJoining(
                meetingInfo.room.name,
                meetingInfo.room.startedAt,
                meetingInfo.user.toLocal(Date()),
                meetingInfo.locked,
                meetingInfo.room.guestToken,
                meetingInfo.links.guestJoin,
                meetingInfo.recording?.toLocal(),
                BroadcastUpdateDto("", meetingInfo.broadcasts).toLocal(),
                SnapshotUpdateDto("", meetingInfo.snapshots).toLocal(),
                meetingInfo.options.widescreen
            )

            webSocketCommunicator = WebSocketCommunicator(meetingInfo).apply {
                connect()

                events.collectIn(eyesonMeetingScope) { command ->
                    handleWebSocketEvents(command, audiOnly, frontCamera, local, remote)
                }
            }
        }
    }

    private suspend fun handleWebSocketEvents(
        command: LocalBaseCommand,
        audiOnly: Boolean,
        frontCamera: Boolean,
        local: VideoSink?,
        remote: VideoSink?
    ) {
        when (command) {
            is StartCallLocal -> {
                meeting = command.meeting
                startCall(command.meeting, audiOnly, frontCamera, local, remote)
            }
            is ResumeCallLocal -> {
                resumeCall(command.callId)
            }
            is CallAccepted -> {
                callLogic?.setRemoteDescription(
                    command.sdp,
                    SessionDescription.Type.ANSWER
                )
                eventListener.onStreamingModeChanged(callLogic?.isSfuMode() ?: return)
            }
            is CallResumed -> {
                callLogic?.setRemoteDescription(
                    command.sdp,
                    SessionDescription.Type.OFFER
                )
                eventListener.onStreamingModeChanged(callLogic?.isSfuMode() ?: return)
            }
            is CallRejected -> {
                leave()
                eventListener.onMeetingJoinFailed(CallRejectionReason.fromRejectCode(command.rejectCode))
            }
            is CallTerminated -> {
                leave()
                eventListener.onMeetingTerminated(CallTerminationReason.fromTerminationCode(command.terminateCode))
            }
            is SdpUpdate -> {
                callLogic?.setRemoteDescription(
                    command.sdp,
                    SessionDescription.Type.OFFER
                )
                eventListener.onStreamingModeChanged(callLogic?.isSfuMode() ?: return)
            }
            is ChatIncoming -> {
                handleChatIncoming(meeting ?: return, command)
            }
            is MuteLocalAudio -> {
                setMicrophoneEnabled(false)
                eventListener.onAudioMutedBy(command.byUser)
            }
            is MeetingLocked -> {
                eventListener.onMeetingLocked(command.locked)
            }
            is PlaybackUpdate -> {
                eventListener.onMediaPlayback(command.playing)
            }
            is BroadcastUpdate -> {
                eventListener.onBroadcastUpdate(command)
            }
            is Recording -> {
                eventListener.onRecordingUpdate(command)
            }
            is SnapshotUpdate -> {
                eventListener.onSnapshotUpdate(command)
            }
            is WsFailure -> {
                leave()
                eventListener.onMeetingTerminated(
                    CallTerminationReason.fromTerminationCode(
                        command.response?.code ?: CallTerminationReason.ERROR.terminationCode
                    )
                )
            }
            is WsClosed -> {
                leave()
                eventListener.onMeetingTerminated(CallTerminationReason.OK)
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
        }
    }

    private fun terminateCallWithError() {
        leave()
        eventListener.onMeetingTerminated(CallTerminationReason.ERROR)
    }

    fun leave() {
        eyesonMeetingScope.cancel()
        nameLookupScope.cancel()
        webSocketCommunicator?.terminateCall()
        callLogic?.disconnectCall()
        joined.set(false)
    }

    fun setLocalVideoTarget(target: VideoSink?) {
        callLogic?.setLocalVideoTarget(target)
    }

    fun setRemoteVideoTarget(target: VideoSink?) {
        callLogic?.setRemoteVideoTarget(target)
    }

    fun setVideoEnabled(enable: Boolean) {
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
        callLogic?.sendChatMessage(message)
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

    private fun startCall(
        meeting: MeetingDto,
        audiOnly: Boolean,
        frontCamera: Boolean,
        local: VideoSink?,
        remote: VideoSink?
    ) {
        callLogic = CallLogic(meeting, audiOnly, application, rootEglBase).apply {
            setLocalVideoTarget(local)
            setRemoteVideoTarget(remote)
            startCall(frontCamera, audioOnStart, videoOnStart)

            events.collectIn(eyesonMeetingScope) { command ->
                Logger.d("got Event: $command")
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
        meeting: MeetingDto
    ) {
        when (command) {
            is CallStart -> {
                webSocketCommunicator?.startCall(command)
            }
            is MemberListUpdate -> {
                withContext(nameLookupScope.coroutineContext) {
                    val infoNeededFor = command.added.filterNot {
                        userInMeeting.containsKey(it)
                    }

                    if (infoNeededFor.isNotEmpty()) {
                        fetchUserInfo(meeting, infoNeededFor)

                    }

                    if (command.added.isNotEmpty()) {
                        eventListener.onUserJoinedMeeting(
                            userInMeeting.filter { command.added.contains(it.key) }
                                .values.toSet().toList()
                        )
                    }

                    if (command.deleted.isNotEmpty()) {
                        eventListener.onUserLeftMeeting(
                            userInMeeting.filter { command.deleted.contains(it.key) }
                                .values.toSet().toList()
                        )
                    }

                    command.deleted.forEach {
                        userListMutex.withLock {
                            userInMeeting.remove(it)
                        }
                    }
                    eventListener.onUserListUpdate(userInMeeting.values.toSet().toList())
                }
            }
            is SourceUpdate -> {
                withContext(nameLookupScope.coroutineContext) {
                    val infoNeeded = command.sources.filterNot {
                        userInMeeting.containsKey(it)
                    }
                    if (infoNeeded.isNotEmpty()) {
                        fetchUserInfo(meeting, infoNeeded)
                    }

                    val videoSourceIds =
                        command.sources.slice(command.videSources.filter { it >= 0 })

                    eventListener.onVideoSourceUpdate(
                        userInMeeting.filter {
                            videoSourceIds.contains(it.key)
                        }.values.toSet().toList(),

                        if (command.desktopStreamingId == null) {
                            null
                        } else {
                            userInMeeting[command.sources[command.desktopStreamingId]]
                        }
                    )
                }
            }

            is VoiceActivity -> {
                withContext(nameLookupScope.coroutineContext) {
                    if (!userInMeeting.containsKey(command.userId)) {
                        fetchUserInfo(meeting, listOf(command.userId))
                    }
                    eventListener.onVoiceActivity(
                        user = userInMeeting[command.userId] ?: return@withContext,
                        active = command.on
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
            is CallTerminated -> {
                leave()
                eventListener.onMeetingTerminated(CallTerminationReason.fromTerminationCode(command.terminateCode))
            }
            is ChatIncoming -> {
                handleChatIncoming(meeting, command)
            }
            is MeetingJoined -> {
                eventListener.onMeetingJoined()
            }
            is CameraSwitchDone -> {
                eventListener.onCameraSwitchDone(command.isFrontCamera)
            }
            is CameraSwitchError -> {
                eventListener.onCameraSwitchError(command.error)
            }
            is ConnectionStatistic -> {
                eventListener.onConnectionStatisticUpdate(command)
            }
        }
    }

    private suspend fun fetchUserInfo(
        meeting: MeetingDto,
        userIds: List<String>
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
            // NOTE: Chat messages from COM- API contain legacy (SIP) user ids
            // e.g. 623b195ec77b9700102f380c@integrations.visocon.com
            var id = chat.userId
            if (id.contains("@")) {
                id = id.substring(0, id.indexOf("@"))
            }
            if (!userInMeeting.containsKey(id)) {
                fetchUserInfo(meeting, listOf(id))
            }

            Logger.d("CHAT inc: ${userInMeeting[id]} -> $chat")
            eventListener.onChatMessageReceived(
                user = userInMeeting[id] ?: return@withContext,
                message = chat.content,
                timestamp = chat.timestamp
            )
        }
    }

    private fun checkForNeededPermissions(
        audiOnly: Boolean,
        context: Context
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
}

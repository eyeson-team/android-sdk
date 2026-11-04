package com.eyeson.sdk.network

import com.eyeson.sdk.di.NetworkModule
import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.local.base.LocalBaseCommand
import com.eyeson.sdk.model.local.call.ResumeCallLocal
import com.eyeson.sdk.model.local.call.StartCallLocal
import com.eyeson.sdk.model.local.meeting.MuteLocalAudioLocal
import com.eyeson.sdk.model.local.meeting.RoomReady
import com.eyeson.sdk.model.local.sepp.CallAccepted
import com.eyeson.sdk.model.local.sepp.CallResume
import com.eyeson.sdk.model.local.sepp.CallStart
import com.eyeson.sdk.model.local.sepp.CallTerminate
import com.eyeson.sdk.model.local.sepp.MuteVideo
import com.eyeson.sdk.model.local.ws.ReconnectSignaling
import com.eyeson.sdk.model.local.ws.WsFailure
import com.eyeson.sdk.model.local.ws.WsOpen
import com.eyeson.sdk.model.meeting.outgoing.MuteAllDto
import com.eyeson.sdk.model.sepp.outgoing.CallResumeDto
import com.eyeson.sdk.model.sepp.outgoing.CallStartDto
import com.eyeson.sdk.model.sepp.outgoing.CallTerminateDto
import com.eyeson.sdk.model.sepp.outgoing.MuteVideoDto
import com.eyeson.sdk.model.sepp.outgoing.fromLocal
import com.eyeson.sdk.utils.collectIn
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicBoolean

internal class WebSocketCommunicator(
    @Volatile private var meeting: MeetingDto,
    private val json: Json = NetworkModule.json,
    private val httpClient: HttpClient = NetworkModule.httpClient,
    defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val communicatorScope = CoroutineScope(SupervisorJob() + defaultDispatcher)

    @Volatile
    private var meetingConnection: MeetingConnection? = null
    private val meetingReady = AtomicBoolean(false)

    @Volatile
    private var callId: String = ""

    private val meetingTryToReconnect = AtomicBoolean(true)
    private val signalingTryToReconnect = AtomicBoolean(true)

    @Volatile
    private var signalingConnection: SignalingConnection? = null
    private val signalingIsReconnecting = AtomicBoolean(false)

    private val _events = Channel<LocalBaseCommand>(Channel.UNLIMITED)
    val events: Flow<LocalBaseCommand> = _events.receiveAsFlow()

    fun connect() {
        val connection = MeetingConnection(meeting, json, httpClient, ioDispatcher)
        meetingConnection = connection
        connection.open { handleMeetingCommands(it) }
    }

    fun reconnectToSignaling(newMeetingInfo: MeetingDto) {
        meeting = newMeetingInfo
        signalingIsReconnecting.set(true)
        connectToSignaling()
    }

    fun startCall(callStart: CallStart, videoOnStart: Boolean, version: String) {
        val callStartDto =
            callStart.fromLocal(meeting, videoOnStart, "$CALL_START_VERSION_PREFIX$version")
        signalingConnection?.sendMessage(
            json.encodeToString(CallStartDto.serializer(), callStartDto)
        )
    }

    fun resumeCall(callResume: CallResume) {
        val callResumeDto = callResume.fromLocal(meeting)
        signalingConnection?.sendMessage(
            json.encodeToString(CallResumeDto.serializer(), callResumeDto)
        )
    }

    fun sendMuteAll() {
        val muteAll = MuteAllDto()
        meetingConnection?.sendMessage(json.encodeToString(MuteAllDto.serializer(), muteAll))
    }

    fun terminateCall() {
        communicatorScope.cancel()
        if (callId.isNotBlank()) {
            val callTerminateDto = CallTerminate(callId, TERMINATE_CALL_CODE_OK).fromLocal(meeting)

            signalingConnection?.sendMessage(
                json.encodeToString(CallTerminateDto.serializer(), callTerminateDto)
            )
        }
        meetingConnection?.disconnect()
        signalingConnection?.disconnect()
    }

    fun setLocalVideoEnabled(enable: Boolean) {
        val muteVideo = MuteVideo(muted = !enable, cid = meeting.signaling.options.clientId)
        val muteVideoDto = muteVideo.fromLocal(callId, meeting)

        signalingConnection?.sendMessage(json.encodeToString(MuteVideoDto.serializer(), muteVideoDto))
    }

    private fun connectToSignaling() {
        val connection = SignalingConnection(meeting, json, httpClient, ioDispatcher)
        signalingConnection = connection
        connection.open { handleSignalingCommands(it) }
    }

    private fun WebSocketConnection.open(handle: (LocalBaseCommand) -> Unit) {
        if (!communicatorScope.isActive) {
            return
        }
        events.collectIn(communicatorScope) { handle(it) }
        connect()
    }

    private fun handleMeetingCommands(command: LocalBaseCommand) {
        when (command) {
            is RoomReady -> {
                if (meetingReady.compareAndSet(false, true)) {
                    meeting = command.meeting
                    connectToSignaling()
                }
            }

            is MuteLocalAudioLocal -> {
                if (command.byUser.id != meeting.user.id) {
                    emitEvent(command)
                }
            }

            is WsFailure -> {
                if (meetingTryToReconnect.getAndSet(false)) {
                    meetingConnection?.disconnect()
                    meetingConnection = null
                    connect()
                } else {
                    emitEvent(command)
                }
            }

            is WsOpen -> {
                meetingTryToReconnect.set(true)
                emitEvent(command)
            }

            else -> {
                emitEvent(command)
            }
        }
    }

    private fun handleSignalingCommands(command: LocalBaseCommand) {
        when (command) {
            is WsOpen -> {
                signalingTryToReconnect.set(true)
                if (signalingIsReconnecting.getAndSet(false)) {
                    emitEvent(ResumeCallLocal(meeting, callId))

                } else {
                    emitEvent(StartCallLocal(meeting))
                }
            }

            is CallAccepted -> {
                callId = command.callId
                emitEvent(command)
            }

            is WsFailure -> {
                if (signalingTryToReconnect.getAndSet(false)) {
                    signalingConnection?.disconnect()
                    signalingConnection = null
                    emitEvent(ReconnectSignaling)
                } else {
                    emitEvent(command)
                }
            }

            else -> {
                emitEvent(command)
            }
        }
    }

    private fun emitEvent(event: LocalBaseCommand) {
        _events.trySend(event)
    }

    private companion object {
        const val TERMINATE_CALL_CODE_OK = 200
        const val CALL_START_VERSION_PREFIX = "android-"
    }
}
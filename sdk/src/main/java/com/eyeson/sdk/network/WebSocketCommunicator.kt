package com.eyeson.sdk.network

import com.eyeson.sdk.di.NetworkModule
import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.local.base.LocalBaseCommand
import com.eyeson.sdk.model.local.call.ResumeCallLocal
import com.eyeson.sdk.model.local.call.StartCallLocal
import com.eyeson.sdk.model.local.meeting.MuteLocalAudio
import com.eyeson.sdk.model.local.meeting.RoomReady
import com.eyeson.sdk.model.local.sepp.CallAccepted
import com.eyeson.sdk.model.local.sepp.CallResume
import com.eyeson.sdk.model.local.sepp.CallStart
import com.eyeson.sdk.model.local.sepp.CallTerminate
import com.eyeson.sdk.model.local.sepp.ChatOutgoing
import com.eyeson.sdk.model.local.sepp.DesktopStreaming
import com.eyeson.sdk.model.local.sepp.MuteVideo
import com.eyeson.sdk.model.local.sepp.SetPresenter
import com.eyeson.sdk.model.local.ws.ReconnectSignaling
import com.eyeson.sdk.model.local.ws.WsFailure
import com.eyeson.sdk.model.local.ws.WsOpen
import com.eyeson.sdk.model.meeting.outgoing.MuteAllDto
import com.eyeson.sdk.model.sepp.outgoing.CallResumeDto
import com.eyeson.sdk.model.sepp.outgoing.CallStartDto
import com.eyeson.sdk.model.sepp.outgoing.CallTerminateDto
import com.eyeson.sdk.model.sepp.outgoing.ChatOutgoingDto
import com.eyeson.sdk.model.sepp.outgoing.DesktopStreamingDto
import com.eyeson.sdk.model.sepp.outgoing.MuteVideoDto
import com.eyeson.sdk.model.sepp.outgoing.SetPresenterDto
import com.eyeson.sdk.model.sepp.outgoing.fromLocal
import com.eyeson.sdk.utils.collectIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

internal class WebSocketCommunicator(
    @Volatile private var meeting: MeetingDto
) {
    private val communicatorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val moshi = NetworkModule.moshi

    private var meetingCommunicator: MeetingConnection? = null
    private val meetingReady = AtomicBoolean(false)
    private var callId: String = ""

    private val meetingTryToReconnect = AtomicBoolean(true)
    private val signalingTryToReconnect = AtomicBoolean(true)

    private var signalingConnection: SignalingConnection? = null
    private val signalingIsReconnecting = AtomicBoolean(false)

    private val _events = MutableSharedFlow<LocalBaseCommand>(0)
    val events = _events.asSharedFlow()

    fun connect() {
        communicatorScope.launch {
            meetingCommunicator = connectToMeetingWs()
        }
    }

    fun startCall(callStart: CallStart, videoOnStart: Boolean) {
        val callStartDto = callStart.fromLocal(meeting, videoOnStart)
        val adapter = moshi.adapter(CallStartDto::class.java)
        signalingConnection?.sendMessage(adapter.toJson(callStartDto))
    }

    fun resumeCall(callResume: CallResume) {
        val callResumeDto = callResume.fromLocal(meeting)
        val adapter = moshi.adapter(CallResumeDto::class.java)
        signalingConnection?.sendMessage(adapter.toJson(callResumeDto))
    }

    fun sendMuteAll() {
        val muteAll = MuteAllDto()
        val adapter = moshi.adapter(MuteAllDto::class.java)
        meetingCommunicator?.sendMessage(adapter.toJson(muteAll))
    }

    fun terminateCall() {
        communicatorScope.cancel()
        if (callId.isNotBlank()) {
            val callTerminateDto = CallTerminate(callId, TERMINATE_CALL_CODE_OK).fromLocal(meeting)
            val adapter = moshi.adapter(CallTerminateDto::class.java)

            signalingConnection?.sendMessage(adapter.toJson(callTerminateDto))
        }
        meetingCommunicator?.disconnect()
        signalingConnection?.disconnect()
    }

    fun sendChatMessage(message: String) {
        val chatOutgoing = ChatOutgoing(message, meeting.signaling.options.clientId)
        val adapter = moshi.adapter(ChatOutgoingDto::class.java)

        signalingConnection?.sendMessage(adapter.toJson(chatOutgoing.fromLocal(callId, meeting)))
    }

    fun enablePresentation(enable: Boolean) {
        val setPresenter = SetPresenter(on = enable, cid = meeting.signaling.options.clientId)
        val desktopStreaming =
            DesktopStreaming(on = enable, cid = meeting.signaling.options.clientId)

        signalingConnection?.sendMessage(
            moshi.adapter(SetPresenterDto::class.java)
                .toJson(setPresenter.fromLocal(callId, meeting))
        )
        signalingConnection?.sendMessage(
            moshi.adapter(DesktopStreamingDto::class.java)
                .toJson(desktopStreaming.fromLocal(callId, meeting))
        )
    }


    fun setLocalVideoEnabled(enable: Boolean) {
        val muteVideo = MuteVideo(muted = !enable, cid = meeting.signaling.options.clientId)
        val adapter = moshi.adapter(MuteVideoDto::class.java)

        signalingConnection?.sendMessage(adapter.toJson(muteVideo.fromLocal(callId, meeting)))
    }


    private suspend fun connectToMeetingWs(): MeetingConnection {
        return coroutineScope {
            val meetingCommunicator = MeetingConnection(meeting)
            meetingCommunicator.connect()

            meetingCommunicator.events.collectIn(communicatorScope) {
                handleMeetingCommands(it)
            }
            meetingCommunicator
        }
    }

    private suspend fun connectToSignalingWs(): SignalingConnection {
        return coroutineScope {
            val signaling = SignalingConnection(meeting)
            signaling.connect()

            signaling.events.collectIn(communicatorScope) {
                handleSignalingCommands(it)
            }
            signaling
        }
    }

    private fun handleMeetingCommands(command: LocalBaseCommand) {
        when (command) {
            is RoomReady -> {
                if (meetingReady.compareAndSet(false, true)) {
                    communicatorScope.launch {
                        meeting = command.meeting
                        signalingConnection = connectToSignalingWs()
                    }
                }
            }
            is MuteLocalAudio -> {
                if (command.byUser.id != meeting.user.id) {
                    emitEvent(command)
                }
            }
            is WsFailure -> {
                if (meetingTryToReconnect.getAndSet(false)) {
                    meetingCommunicator?.disconnect()
                    meetingCommunicator = null
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
                    emitEvent(ReconnectSignaling(command.response))
                } else {
                    emitEvent(command)
                }
            }
            else -> {
                emitEvent(command)
            }
        }
    }

    fun reconnectToSignaling(newMeetingInfo: MeetingDto) {
        meeting = newMeetingInfo
        signalingIsReconnecting.set(true)
        communicatorScope.launch {
            signalingConnection = connectToSignalingWs()
        }
    }

    private fun emitEvent(event: LocalBaseCommand) {
        communicatorScope.launch {
            _events.emit(event)
        }
    }

    companion object {
        const val TERMINATE_CALL_CODE_OK = 200
    }
}
package com.eyeson.sdk.network

import com.eyeson.sdk.di.NetworkModule
import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.local.base.LocalBaseCommand
import com.eyeson.sdk.model.local.call.StartCallLocal
import com.eyeson.sdk.model.local.meeting.MuteLocalAudio
import com.eyeson.sdk.model.local.meeting.RoomReady
import com.eyeson.sdk.model.local.sepp.CallAccepted
import com.eyeson.sdk.model.local.sepp.CallStart
import com.eyeson.sdk.model.local.sepp.CallTerminate
import com.eyeson.sdk.model.local.ws.WsOpen
import com.eyeson.sdk.model.meeting.outgoing.MuteAllDto
import com.eyeson.sdk.model.sepp.outgoing.CallStartDto
import com.eyeson.sdk.model.sepp.outgoing.CallTerminateDto
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

    private var signalingConnection: SignalingConnection? = null

    private val _events = MutableSharedFlow<LocalBaseCommand>(0)
    val events = _events.asSharedFlow()

    fun connect() {
        communicatorScope.launch {
            meetingCommunicator = connectToMeetingWs()
        }
    }

    fun startCall(callStart: CallStart) {
        val callStartDto = callStart.fromLocal(meeting)
        val adapter = moshi.adapter(CallStartDto::class.java)
        signalingConnection?.sendMessage(adapter.toJson(callStartDto))
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
            else -> {
                emitEvent(command)
            }
        }
    }

    private fun handleSignalingCommands(command: LocalBaseCommand) {
        when (command) {
            is WsOpen -> {
                emitEvent(StartCallLocal(meeting))
            }
            is CallAccepted -> {
                callId = command.callId
                emitEvent(command)
            }
            else -> {
                emitEvent(command)
            }
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
package com.eyeson.android.ui.meeting

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyeson.android.EyesonNavigationParameter.ACCESS_KEY
import com.eyeson.android.ui.meeting.ChatMessage.IncomingMessage
import com.eyeson.android.ui.meeting.ChatMessage.OutgoingMessage
import com.eyeson.android.ui.view.Event
import com.eyeson.sdk.EyesonMeeting
import com.eyeson.sdk.events.CallRejectionReason
import com.eyeson.sdk.events.CallTerminationReason
import com.eyeson.sdk.events.EyesonEventListener
import com.eyeson.sdk.events.NeededPermissions
import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.call.ConnectionStatistic
import com.eyeson.sdk.model.local.meeting.BroadcastUpdate
import com.eyeson.sdk.model.local.meeting.PlaybackUpdate
import com.eyeson.sdk.model.local.meeting.Recording
import com.eyeson.sdk.model.local.meeting.SnapshotUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.webrtc.EglBase
import org.webrtc.VideoSink
import timber.log.Timber
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class MeetingViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    application: Application
) : ViewModel() {

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val _inCall = AtomicBoolean(false)
    fun inCall(): Boolean = _inCall.get()

    private val _p2p = MutableStateFlow(false)
    val p2p: StateFlow<Boolean> = _p2p.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )


    private val eventListener = object : EyesonEventListener() {
        override fun onPermissionsNeeded(neededPermissions: List<NeededPermissions>) {
            addEvent("onPermissionsNeeded: neededPermissions $neededPermissions")

        }

        override fun onMeetingJoining(
            name: String,
            startedAt: Date,
            user: UserInfo,
            locked: Boolean,
            guestToken: String,
            guestLink: String,
            activeRecording: Recording?,
            activeBroadcasts: BroadcastUpdate?,
            snapshots: SnapshotUpdate?,
            isWidescreen: Boolean
        ) {
            addEvent(
                "onMeetingJoining: name $name; startedAt $startedAt; user $user; " +
                        "locked $locked; guestToke $guestToken; guestLink $guestLink; activeRecording " +
                        "$activeRecording; activeBroadcasts $activeBroadcasts; snapshots $snapshots;" +
                        " isWidescreen $isWidescreen"
            )
        }

        override fun onMeetingJoined() {
            addEvent("onMeetingJoined")
            _inCall.set(true)
//            lastCameraState = isVideoEnabled()
        }

        override fun onMeetingJoinFailed(callRejectionReason: CallRejectionReason) {
            addEvent("onMeetingJoinFailed: callRejectionReason $callRejectionReason")
//            _callTerminated.value = true
        }

        override fun onMeetingTerminated(callTerminationReason: CallTerminationReason) {
            addEvent("onMeetingTerminated: callTerminationReason $callTerminationReason")
            _inCall.set(false)
//            Timber.d("KICK: _callTerminated.value = true")
//            _callTerminated.value = true
        }

        override fun onMeetingLocked(locked: Boolean) {
            addEvent("onMeetingLocked: locked $locked")
        }

        override fun onStreamingModeChanged(p2p: Boolean) {
            addEvent("onStreamingModeChanged: p2p $p2p")
            _p2p.value = p2p
        }

        override fun onVideoSourceUpdate(
            visibleUsers: List<UserInfo>,
            presenter: UserInfo?
        ) {
            addEvent("onVideoSourceUpdate: $visibleUsers; presenter $presenter")
//            if (!presentationActive && presenter?.id != null && presenter.id != eyesonMeeting?.getUserInfo()?.id) {
//                presentationActive = true
//                lastCameraState = isVideoEnabled()
//                eyesonMeeting?.setVideoEnabled(false)
//
//            }
//            if (presentationActive && presenter == null) {
//                presentationActive = false
//                eyesonMeeting?.setVideoEnabled(lastCameraState)
//                lastCameraState = isVideoEnabled()
//            }
        }

        override fun onAudioMutedBy(user: UserInfo) {
            addEvent("onAudioMutedBy: user $user")
        }

        override fun onMediaPlayback(playing: List<PlaybackUpdate.Playback>) {
            addEvent("onMediaPlayback: playing $playing")
        }

        override fun onBroadcastUpdate(activeBroadcasts: BroadcastUpdate) {
            addEvent("onBroadcastUpdate: activeBroadcasts $activeBroadcasts")
        }

        override fun onRecordingUpdate(recording: Recording) {
            addEvent("onRecordingUpdate: recording $recording")
        }

        override fun onSnapshotUpdate(snapshots: SnapshotUpdate) {
            addEvent("onSnapshotUpdate: snapshots $snapshots")
        }

        override fun onConnectionStatisticUpdate(statistic: ConnectionStatistic) {
//            addEvent("onConnectionStatisticUpdate: statistic $statistic")
//            Timber.d("onConnectionStatisticUpdate: statistic $statistic")
        }

        override fun onUserJoinedMeeting(users: List<UserInfo>) {
            addEvent("onUserJoinedMeeting: users $users")
        }

        override fun onUserLeftMeeting(users: List<UserInfo>) {
            addEvent("onUserLeftMeeting: users $users")
        }

        override fun onUserListUpdate(users: List<UserInfo>) {
            addEvent("onUserListUpdate: users $users")
        }

        override fun onVoiceActivity(user: UserInfo, active: Boolean) {
//            addEvent("onVoiceActivity: users $user; active $active")
        }

        override fun onChatMessageReceived(
            user: UserInfo,
            message: String,
            timestamp: Date
        ) {
            addEvent("onChatMessageReceived: user $user; message $message; timestamp $timestamp")

            emitChatMessage(user, message, timestamp)

        }

        override fun onCustomMessageReceived(user: UserInfo, message: String, timestamp: Date) {
            addEvent("onCustomMessageReceived: user $user; message $message; timestamp $timestamp")
        }

        override fun onCameraSwitchDone(isFrontCamera: Boolean) {
            addEvent("onCameraSwitchDone: isFrontCamera $isFrontCamera")
        }

        override fun onCameraSwitchError(error: String) {
            addEvent("onCameraSwitchError: error $error")
        }
    }



    private val eyesonMeeting by lazy {
        EyesonMeeting(eventListener = eventListener, application = application)
    }

    init {
        Timber.d(
            "MeetingViewModel: ${
                savedStateHandle.keys().map {
                    "$it -> ${savedStateHandle.get<String?>(it)}"
                }
            }"
        )

        Timber.d("MeetingViewModel: ${eyesonMeeting.getEglContext()}")

    }


    private fun addEvent(text: String) {
        _events.value = _events.value + Event(Date(), text)
    }

    fun getEglContext(): EglBase.Context? {
        return eyesonMeeting.getEglContext()
    }

    fun connect(
        local: VideoSink?,
        remote: VideoSink?,
    ) {
        eyesonMeeting.join(
            accessKey = checkNotNull(savedStateHandle.get<String>(ACCESS_KEY)),
            frontCamera = true,
            audiOnly = false,
            local = local,
            remote = remote,
            microphoneEnabledOnStart = true,
            videoEnabledOnStart = true
        )
    }

    fun setLocalVideoTarget(target: VideoSink?) {
        eyesonMeeting.setLocalVideoTarget(target)
    }

    fun setRemoteVideoTarget(target: VideoSink?) {
        eyesonMeeting.setRemoteVideoTarget(target)
    }

    private fun emitChatMessage(user: UserInfo, message: String, timestamp: Date) {
        val chatMessage = if (user == eyesonMeeting.getUserInfo()) {
            OutgoingMessage(message, timestamp)
        } else {
            IncomingMessage(message, user.name, timestamp, user.avatar)
        }

        _chatMessages.value = _chatMessages.value + chatMessage
    }

}

sealed class ChatMessage {
    data class OutgoingMessage(val text: String, val time: Date) : ChatMessage()
    data class IncomingMessage(
        val text: String,
        val from: String,
        val time: Date,
        val avatarUrl: String? = null
    ) : ChatMessage()
}
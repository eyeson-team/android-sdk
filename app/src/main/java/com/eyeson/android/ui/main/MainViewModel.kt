package com.eyeson.android.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eyeson.android.ui.events.Event
import com.eyeson.sdk.EyesonMeeting
import com.eyeson.sdk.events.CallRejectionReason
import com.eyeson.sdk.events.CallTerminationReason
import com.eyeson.sdk.events.EyesonEventListener
import com.eyeson.sdk.events.NeededPermissions
import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.meeting.BroadcastUpdate
import com.eyeson.sdk.model.local.meeting.PlaybackUpdate
import com.eyeson.sdk.model.local.meeting.Recording
import com.eyeson.sdk.model.local.meeting.SnapshotUpdate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.VideoSink
import timber.log.Timber
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private var eyesonMeeting: EyesonMeeting? = null

    private val _events = MutableSharedFlow<Event>(replay = 0)
    val events: SharedFlow<Event> = _events

    private val _callTerminated = MutableStateFlow(false)
    val callTerminated = _callTerminated.asStateFlow()

    var inCall = false

    private val eventListener = object : EyesonEventListener() {
        override fun onPermissionsNeeded(neededPermissions: List<NeededPermissions>) {
            addEvent("onPermissionsNeeded: neededPermissions $neededPermissions")
        }

        override fun onMeetingJoining(
            name: String,
            startedAt: Date,
            user: UserInfo,
            locked: Boolean,
            guestToke: String,
            guestLink: String,
            activeRecording: Recording?,
            activeBroadcasts: BroadcastUpdate?,
            snapshots: SnapshotUpdate?
        ) {
            addEvent(
                "onMeetingJoining: name $name; startedAt $startedAt; user $user; " +
                        "locked $locked; guestToke $guestToke; guestLink $guestLink; activeRecording " +
                        "$activeRecording; activeBroadcasts $activeBroadcasts; snapshots $snapshots"
            )
        }

        override fun onMeetingJoined() {
            addEvent("onMeetingJoined")
            inCall = true
        }

        override fun onMeetingJoinFailed(callRejectionReason: CallRejectionReason) {
            addEvent("onMeetingJoinFailed: callRejectionReason $callRejectionReason")
            _callTerminated.value = true
        }

        override fun onMeetingTerminated(callTerminationReason: CallTerminationReason) {
            addEvent("onMeetingTerminated: callTerminationReason $callTerminationReason")
            inCall = false
            Timber.d("KICK: _callTerminated.value = true")
            _callTerminated.value = true
        }

        override fun onMeetingLocked(locked: Boolean) {
            addEvent("onMeetingLocked: locked $locked")
        }

        override fun onStreamingModeChanged(p2p: Boolean) {
            addEvent("onStreamingModeChanged: p2p $p2p")
        }

        override fun onVideoSourceUpdate(
            visibleUsers: List<UserInfo>,
            presenter: UserInfo?
        ) {
            addEvent("onVideoSourceUpdate: $visibleUsers; presenter $presenter")
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
        }

        override fun onCameraSwitchDone(isFrontCamera: Boolean) {
            addEvent("onCameraSwitchDone: isFrontCamera $isFrontCamera")
        }

        override fun onCameraSwitchError(error: String) {
            addEvent("onCameraSwitchError: error $error")
        }
    }

    fun connect(accessKey: String, local: VideoSink?, remote: VideoSink?) {
        _callTerminated.value = false

        viewModelScope.launch {
            eyesonMeeting = EyesonMeeting(
                eventListener = eventListener,
                application = getApplication()
            ).apply {
                join(
                    accessKey = accessKey,
                    frontCamera = true,
                    audiOnly = false,
                    local = local,
                    remote = remote,
                    microphoneEnabledOnStart = true,
                    videoEnabledOnStart = true
                )
            }
        }
    }

    fun connectAsGuest(
        guestToken: String,
        name: String,
        local: VideoSink?,
        remote: VideoSink?
    ) {
        _callTerminated.value = false

        viewModelScope.launch {
            eyesonMeeting = EyesonMeeting(
                eventListener = eventListener,
                application = getApplication()
            ).apply {
                joinAsGuest(
                    guestToken = guestToken,
                    name = name,
                    id = null,
                    avatar = null,
                    frontCamera = true,
                    audiOnly = false,
                    local = local,
                    remote = remote,
                    microphoneEnabledOnStart = true,
                    videoEnabledOnStart = true
                )
            }
        }
    }

    fun disconnect() {
        eyesonMeeting?.leave()
        inCall = false
    }

    fun getEglContext(): EglBase.Context? {
        return eyesonMeeting?.getEglContext()
    }

    fun clearTarget() {
        eyesonMeeting?.setLocalVideoTarget(null)
        eyesonMeeting?.setRemoteVideoTarget(null)
    }

    fun setTargets(local: VideoSink?, remote: VideoSink?) {
        eyesonMeeting?.setLocalVideoTarget(local)
        eyesonMeeting?.setRemoteVideoTarget(remote)
    }

    var localVideoEnabled = false
    fun muteVideoLocal() {
        eyesonMeeting?.setVideoEnabled(localVideoEnabled)
        localVideoEnabled = localVideoEnabled.not()
        Timber.d("localVideoEnabled :$localVideoEnabled")
    }

    var audioEnabled = true
    fun muteAudio() {
        audioEnabled = audioEnabled.not()
        eyesonMeeting?.setMicrophoneEnabled(audioEnabled)
    }

    fun muteAll() {
        eyesonMeeting?.sendMuteOthers()
    }

    fun sendMessage(message: String) {
        eyesonMeeting?.sendChatMessage(message)
    }

    private fun addEvent(text: String) {
        viewModelScope.launch {
            _events.emit(Event(Date(), text))
        }
    }

    fun isVideoEnabled(): Boolean {
        return eyesonMeeting?.isVideoEnabled() ?: false
    }

    fun isMicrophoneEnabled(): Boolean {
        return eyesonMeeting?.isMicrophoneEnabled() ?: false
    }

    fun switchCamera() {
        val direction = when (eyesonMeeting?.isFrontCamera()) {
            true -> {
                "BACK"
            }
            false -> {
                "FRONT"
            }
            else -> {
                "NOT IN MEETING"
            }
        }
        Timber.d("switching camera to $direction")
        eyesonMeeting?.switchCamera()
    }
}
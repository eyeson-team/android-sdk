package com.eyeson.android.ui.view.main

import android.app.Application
import android.app.Notification
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eyeson.android.ui.view.Event
import com.eyeson.sdk.EyesonAudioManager
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
    private var audioManager: EyesonAudioManager? = null

    private val _events = MutableSharedFlow<Event>(replay = 0)
    val events: SharedFlow<Event> = _events

    private val _callTerminated = MutableStateFlow(false)
    val callTerminated = _callTerminated.asStateFlow()

    var inCall = false

    private var lastCameraState = isVideoEnabled()
    private var presentationActive = false

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
            inCall = true
            lastCameraState = isVideoEnabled()
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
            if (!presentationActive && presenter?.id != null && presenter.id != eyesonMeeting?.getUserInfo()?.id) {
                presentationActive = true
                lastCameraState = isVideoEnabled()
                eyesonMeeting?.setVideoEnabled(false)

            }
            if (presentationActive && presenter == null) {
                presentationActive = false
                eyesonMeeting?.setVideoEnabled(lastCameraState)
                lastCameraState = isVideoEnabled()
            }
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

    fun connect(
        accessKey: String,
        local: VideoSink?,
        remote: VideoSink?,
        mediaProjectionPermissionResultData: Intent? = null,
        notificationId: Int? = null,
        notification: Notification? = null
    ) {
        _callTerminated.value = false

        val screenShareInfo = if (mediaProjectionPermissionResultData != null &&
            notificationId != null && notification != null
        ) {
            EyesonMeeting.ScreenShareInfo(
                mediaProjectionPermissionResultData,
                notificationId,
                notification
            )
        } else {
            null
        }

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
                    videoEnabledOnStart = true,
                    screenShareInfo = screenShareInfo
                )
            }
            startAudioManager()
        }
    }

    fun startScreenShare(
        mediaProjectionPermissionResultData: Intent,
        asPresentation: Boolean,
        notificationId: Int,
        notification: Notification
    ) {
        val started = eyesonMeeting?.startScreenShare(
            EyesonMeeting.ScreenShareInfo(
                mediaProjectionPermissionResultData,
                notificationId,
                notification
            ),
            asPresentation
        )

        Timber.d("Screen share started: $started")
    }

    fun stopScreenShare() {
        eyesonMeeting?.stopScreenShare(true)
        eyesonMeeting?.stopPresentation()
    }

    fun setVideoAsPresentation() {
        eyesonMeeting?.setVideoAsPresentation()
    }

    fun stopPresentation() {
        eyesonMeeting?.stopPresentation()
    }


    fun connectAsGuest(
        guestToken: String,
        name: String,
        local: VideoSink?,
        remote: VideoSink?,
        mediaProjectionPermissionResultData: Intent? = null,
        notificationId: Int? = null,
        notification: Notification? = null
    ) {
        _callTerminated.value = false

        val screenShareInfo = if (mediaProjectionPermissionResultData != null &&
            notificationId != null && notification != null
        ) {
            EyesonMeeting.ScreenShareInfo(
                mediaProjectionPermissionResultData,
                notificationId,
                notification
            )
        } else {
            null
        }


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
                    videoEnabledOnStart = true,
                    screenShareInfo = screenShareInfo
                )
            }
            startAudioManager()
        }
    }

    fun disconnect() {
        audioManager?.stop()
        audioManager = null

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

    fun muteVideoLocal() {
        lastCameraState = !isVideoEnabled()
        eyesonMeeting?.setVideoEnabled(!isVideoEnabled())
    }

    fun muteAudio() {
        eyesonMeeting?.setMicrophoneEnabled(!isMicrophoneEnabled())
    }

    fun muteAll() {
        eyesonMeeting?.sendMuteOthers()
    }

    fun sendChatMessage(message: String) {
        eyesonMeeting?.sendChatMessage(message)
    }

    fun sendCustomMessage(message: String) {
        eyesonMeeting?.sendCustomMessage(message)
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

    private fun startAudioManager() {
        audioManager = EyesonAudioManager(getApplication())
        audioManager?.start(object : EyesonAudioManager.AudioManagerEvents {
            override fun onAudioDeviceChanged(
                selectedAudioDevice: EyesonAudioManager.AudioDevice,
                availableAudioDevices: Set<EyesonAudioManager.AudioDevice>
            ) {
                Timber.d("onAudioManagerDevicesChanged: $availableAudioDevices, selected: $selectedAudioDevice")
            }
        })
    }

    fun toggleSpeakerphone() {
        if (audioManager?.getSelectedAudioDevice() == EyesonAudioManager.AudioDevice.SpeakerPhone) {
            audioManager?.selectAudioDevice(EyesonAudioManager.AudioDevice.None)
        } else {
            audioManager?.selectAudioDevice(EyesonAudioManager.AudioDevice.SpeakerPhone)
        }
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
package com.eyeson.android.ui.meeting

import android.app.Application
import android.app.Notification
import android.content.ClipData
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyeson.android.EyesonNavigationParameter.ACCESS_KEY
import com.eyeson.android.EyesonNavigationParameter.GUEST_NAME
import com.eyeson.android.EyesonNavigationParameter.GUEST_TOKEN
import com.eyeson.android.R
import com.eyeson.android.data.SettingsRepository
import com.eyeson.android.ui.meeting.ChatMessage.IncomingMessage
import com.eyeson.android.ui.meeting.ChatMessage.OutgoingMessage
import com.eyeson.android.ui.settings.SettingsUiState
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import org.webrtc.EglBase
import org.webrtc.VideoSink
import timber.log.Timber
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class MeetingViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val application: Application,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _events = MutableStateFlow<List<EventEntry>>(emptyList())
    val events: StateFlow<List<EventEntry>> = _events.asStateFlow()

    private val _inCall = AtomicBoolean(false)
    fun inCall(): Boolean = _inCall.get()

    private val _p2p = MutableStateFlow(false)
    val p2p: StateFlow<Boolean> = _p2p.asStateFlow()

    private val _callTerminated = MutableStateFlow(CallTerminationReason.UNSPECIFIED)
    val callTerminated: StateFlow<CallTerminationReason> = _callTerminated.asStateFlow()

    private val _meetingJoinFailed = MutableStateFlow(false)
    val meetingJoinFailed: StateFlow<Boolean> = _meetingJoinFailed.asStateFlow()

    private val _userInMeetingCount = MutableStateFlow(0)
    val userInMeetingCount: StateFlow<Int> = _userInMeetingCount.asStateFlow()

    private val eventListener: EyesonEventListener = object : EyesonEventListener() {
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
            lastCameraState = isVideoEnabled()
            _cameraActive.value = isVideoEnabled()
        }

        override fun onMeetingJoinFailed(callRejectionReason: CallRejectionReason) {
            addEvent("onMeetingJoinFailed: callRejectionReason $callRejectionReason", true)
            _meetingJoinFailed.value = true
            audioManager.stop()
        }

        override fun onMeetingTerminated(callTerminationReason: CallTerminationReason) {
            addEvent("onMeetingTerminated: callTerminationReason $callTerminationReason")
            Timber.d("onMeetingTerminated: callTerminationReason $callTerminationReason")
            _inCall.set(false)
            audioManager.stop()
            _callTerminated.value = callTerminationReason
        }

        override fun onMeetingLocked(locked: Boolean) {
            addEvent("onMeetingLocked: locked $locked", true)
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
            Timber.d("onVideoSourceUpdate: _presentationActive.value ${_presentationActive.value} $visibleUsers; presenter $presenter")
            if (!_presentationActive.value && presenter?.id != null && presenter.id != eyesonMeeting.getUserInfo()?.id) {
                lastCameraState = isVideoEnabled()
                _cameraActive.value = false
                eyesonMeeting.setVideoEnabled(false)

            }
            if (_presentationActive.value && presenter == null) {
                eyesonMeeting.setVideoEnabled(lastCameraState)
                _cameraActive.value = lastCameraState
            }

            _presentationActive.value = presenter?.id != null
            Timber.d("_presentationActive.value: ${_presentationActive.value};")
        }

        override fun onAudioMutedBy(user: UserInfo) {
            addEvent("onAudioMutedBy: user $user")
            _microphoneActive.value = !isMicrophoneEnabled()
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
            _userInMeetingCount.value = users.size
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
            addEvent("onCameraSwitchError: error $error", true)
        }
    }

    private val eyesonMeeting: EyesonMeeting by lazy {
        EyesonMeeting(eventListener = eventListener, application = application)
    }

    private val audioManager by lazy { EyesonAudioManager(application) }

    private var lastCameraState = isVideoEnabled()

    private val _presentationActive = MutableStateFlow(false)
    val presentationActive: StateFlow<Boolean> = _presentationActive.asStateFlow()

    private val _screenShareActive = MutableStateFlow(false)
    val screenShareActive: StateFlow<Boolean> = _screenShareActive.asStateFlow()

    private val _cameraActive = MutableStateFlow(isVideoEnabled())
    val cameraActive: StateFlow<Boolean> = _cameraActive.asStateFlow()

    private val _microphoneActive = MutableStateFlow(isMicrophoneEnabled())
    val microphoneActive: StateFlow<Boolean> = _microphoneActive.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _audioDevices = MutableStateFlow<List<AudioDevice>>(emptyList())
    val audioDevices: StateFlow<List<AudioDevice>> = _audioDevices.asStateFlow()

    private fun addEvent(text: String, error: Boolean = false) {
        _events.value = emptyList<EventEntry>() + EventEntry(text, Date(), error) + _events.value
    }

    val meetingSettings = runBlocking {  settingsRepository.meetingSettings.first()}
    var screenCaptureAsPresentation = false

    fun getEglContext(): EglBase.Context? {
        return eyesonMeeting.getEglContext()
    }

    fun connect(
        local: VideoSink?,
        remote: VideoSink?,
        mediaProjectionPermissionResultData: Intent? = null,
        notificationId: Int? = null,
        notification: Notification? = null
    ) {

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

        when {
            savedStateHandle.get<String>(ACCESS_KEY) != null -> {
                eyesonMeeting.join(
                    accessKey = checkNotNull(savedStateHandle.get<String>(ACCESS_KEY)),
                    frontCamera = !meetingSettings.rearCamOnStart,
                    audiOnly = meetingSettings.audioOnly,
                    local = local,
                    remote = remote,
                    microphoneEnabledOnStart = meetingSettings.micOnStar,
                    videoEnabledOnStart = meetingSettings.videoOnStart,
                    screenShareInfo = screenShareInfo
                )
            }
            savedStateHandle.get<String>(GUEST_TOKEN) != null -> {
                eyesonMeeting.joinAsGuest(
                    guestToken = checkNotNull(savedStateHandle.get<String>(GUEST_TOKEN)),
                    name = checkNotNull(savedStateHandle.get<String>(GUEST_NAME)),
                    id = null,
                    avatar = null,
                    frontCamera = !meetingSettings.rearCamOnStart,
                    audiOnly = meetingSettings.audioOnly,
                    local = local,
                    remote = remote,
                    microphoneEnabledOnStart = meetingSettings.micOnStar,
                    videoEnabledOnStart = meetingSettings.videoOnStart,
                    screenShareInfo = screenShareInfo
                )
            }
            else -> {
                return
            }
        }
        startAudioManager()
    }

    fun disconnect() {
        audioManager.stop()

        eyesonMeeting.leave()
        _inCall.set(false)
    }

    fun setLocalVideoTarget(target: VideoSink?) {
        Timber.d("setLocalVideoTarget $target")
        eyesonMeeting.setLocalVideoTarget(target)
    }

    fun setRemoteVideoTarget(target: VideoSink?) {
        eyesonMeeting.setRemoteVideoTarget(target)
    }

    private fun emitChatMessage(user: UserInfo, message: String, timestamp: Date) {
        val chatMessage = if (user.id == eyesonMeeting.getUserInfo()?.id) {
            OutgoingMessage(message, timestamp)
        } else {
            IncomingMessage(message, user.name, timestamp, user.avatar)
        }

        _chatMessages.value = emptyList<ChatMessage>() + chatMessage + _chatMessages.value
    }

    private fun isVideoEnabled(): Boolean {
        return eyesonMeeting.isVideoEnabled()
    }

    private fun isMicrophoneEnabled(): Boolean {
        return eyesonMeeting.isMicrophoneEnabled()
    }

    fun isWideScreen(): Boolean {
        return eyesonMeeting.isWidescreen()
    }

    fun toggleLocalVideo() {
        lastCameraState = !isVideoEnabled()
        _cameraActive.value = !isVideoEnabled()
        eyesonMeeting.setVideoEnabled(!isVideoEnabled())
    }

    fun toggleLocalMicrophone() {
        eyesonMeeting.setMicrophoneEnabled(!isMicrophoneEnabled())
        _microphoneActive.value = isMicrophoneEnabled()
    }

    fun muteAll() {
        eyesonMeeting.sendMuteOthers()
    }

    fun switchCamera() {
        eyesonMeeting.switchCamera()
    }

    fun sendChatMessage(message: String) {
        eyesonMeeting.sendChatMessage(message)
    }

    fun getEventsClip(): ClipData {
        return ClipData.newPlainText(
            "Eyeson SDK event log", "${
                _events.value.map {
                    "$it\n"
                }
            }")
    }

    fun clearLog() {
        _events.value = emptyList()
    }

    private fun startAudioManager() {
        audioManager.start(object : EyesonAudioManager.AudioManagerEvents {
            override fun onAudioDeviceChanged(
                selectedAudioDevice: EyesonAudioManager.AudioDevice,
                availableAudioDevices: Set<EyesonAudioManager.AudioDevice>
            ) {
                Timber.d("onAudioManagerDevicesChanged: $availableAudioDevices, selected: $selectedAudioDevice")

                _audioDevices.value = availableAudioDevices.map {
                    val title = when (it) {
                        EyesonAudioManager.AudioDevice.Bluetooth -> R.string.bluetooth_device
                        EyesonAudioManager.AudioDevice.Earpiece -> R.string.ear_piece
                        EyesonAudioManager.AudioDevice.None -> R.string.none
                        EyesonAudioManager.AudioDevice.SpeakerPhone -> R.string.speaker_phone
                        EyesonAudioManager.AudioDevice.WiredHeadset -> R.string.wired_headset
                    }

                    AudioDevice(
                        application.getString(title),
                        it == selectedAudioDevice,
                        { audioManager.selectAudioDevice(it) })
                }
            }
        })
    }

    fun startScreenShare(
        mediaProjectionPermissionResultData: Intent,
        notificationId: Int,
        notification: Notification
    ) {
        val started = eyesonMeeting.startScreenShare(
            EyesonMeeting.ScreenShareInfo(
                mediaProjectionPermissionResultData,
                notificationId,
                notification
            ),
            screenCaptureAsPresentation
        )

        _screenShareActive.value = true
        Timber.d("Screen share started: $started")
    }

    fun setVideoAsPresentation() {
        if (eyesonMeeting.isScreenShareActive() && screenCaptureAsPresentation) {
            eyesonMeeting.setVideoAsPresentation()
        }
    }

    fun stopScreenShare() {
        _screenShareActive.value = false
        eyesonMeeting.stopScreenShare(true)
        eyesonMeeting.stopPresentation()
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

data class EventEntry(val event: String, val time: Date, val error: Boolean = false)

data class AudioDevice(
    val title: String,
    val selected: Boolean,
    val onClick: () -> Unit,
    val enabled: Boolean = true
)

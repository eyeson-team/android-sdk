package com.eyeson.android.ui.meeting

import android.app.Application
import android.app.Notification
import android.content.ClipData
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.LENS_FACING
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity.CAMERA_SERVICE
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Player.STATE_READY
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.eyeson.android.EyesonNavigationParameter.ACCESS_KEY
import com.eyeson.android.EyesonNavigationParameter.GUEST_NAME
import com.eyeson.android.EyesonNavigationParameter.GUEST_NAME_PERMALINK
import com.eyeson.android.EyesonNavigationParameter.GUEST_TOKEN
import com.eyeson.android.EyesonNavigationParameter.GUEST_TOKEN_PERMALINK
import com.eyeson.android.EyesonNavigationParameter.USER_TOKEN
import com.eyeson.android.R
import com.eyeson.android.data.SettingsRepository
import com.eyeson.android.ui.meeting.ChatMessage.IncomingMessage
import com.eyeson.android.ui.meeting.ChatMessage.OutgoingMessage
import com.eyeson.sdk.EyesonAudioManager
import com.eyeson.sdk.EyesonMeeting
import com.eyeson.sdk.events.CallRejectionReason
import com.eyeson.sdk.events.CallTerminationReason
import com.eyeson.sdk.events.EyesonEventListener
import com.eyeson.sdk.events.MediaPlaybackResponse
import com.eyeson.sdk.events.NeededPermissions
import com.eyeson.sdk.events.PresentationResponse
import com.eyeson.sdk.model.local.api.MeetingInfoInitial
import com.eyeson.sdk.model.local.api.MeetingOptions
import com.eyeson.sdk.model.local.api.PermalinkMeetingInfo
import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.call.ConnectionStatistic
import com.eyeson.sdk.model.local.meeting.BroadcastUpdate
import com.eyeson.sdk.model.local.meeting.Playback
import com.eyeson.sdk.model.local.meeting.PresentationUpdate
import com.eyeson.sdk.model.local.meeting.Recording
import com.eyeson.sdk.model.local.meeting.SnapshotUpdate
import com.eyeson.sdk.options.ScreenShareOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.webrtc.EglBase
import org.webrtc.VideoSink
import timber.log.Timber
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@OptIn(UnstableApi::class)
@HiltViewModel
class MeetingViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val application: Application,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    var meetingState = mutableStateOf<MeetingState>(MeetingState.Initial)
        private set

    val meetingSettings = runBlocking { settingsRepository.meetingSettings.first() }

    private val _events = MutableStateFlow<List<EventEntry>>(emptyList())
    val events: StateFlow<List<EventEntry>> = _events.asStateFlow()

    private val _p2p = MutableStateFlow(false)
    val p2p: StateFlow<Boolean> = _p2p.asStateFlow()

    private val _userInMeeting = MutableStateFlow(emptyList<UserInfo>())
    val userInMeeting: StateFlow<List<UserInfo>> = _userInMeeting.asStateFlow()

    private val eventListener: EyesonEventListener = object : EyesonEventListener() {
        override fun onPermissionsNeeded(neededPermissions: List<NeededPermissions>) {
            addEvent("onPermissionsNeeded: neededPermissions $neededPermissions")
        }

        override fun onMeetingJoining(meetingInfo: MeetingInfoInitial) {
            with(meetingInfo) {
                addEvent(
                    "onMeetingJoining: accessKey: $accessKey;  name $name; startedAt $startedAt; user $user; " +
                            "locked $locked; guestToke $guestToken; guestLink $guestLink; activeRecording " +
                            "$activeRecording; activeBroadcasts $activeBroadcasts; snapshots $snapshots;" +
                            " meetingOptions $meetingOptions, playbacks $playbacks"
                )
            }
            meetingState.value = MeetingState.Connecting
            _recordingActive.value = meetingInfo.activeRecording != null
        }

        override fun onMeetingJoined() {
            addEvent("onMeetingJoined")
            meetingState.value = MeetingState.Connected
            lastCameraState = isVideoEnabled()
            _cameraActive.value = isVideoEnabled()
        }

        override fun onMeetingJoinFailed(callRejectionReason: CallRejectionReason) {
            addEvent("onMeetingJoinFailed: callRejectionReason $callRejectionReason", true)
            audioManager.stop()
            meetingState.value = MeetingState.ConnectionFailed(callRejectionReason)
        }

        override fun onMeetingTerminated(callTerminationReason: CallTerminationReason) {
            addEvent("onMeetingTerminated: callTerminationReason $callTerminationReason")
            Timber.d("onMeetingTerminated: callTerminationReason $callTerminationReason")
            audioManager.stop()
            meetingState.value = MeetingState.ConnectionTerminated(callTerminationReason)
        }

        override fun onMeetingLocked(locked: Boolean) {
            addEvent("onMeetingLocked: locked $locked", true)
        }

        override fun onStreamingModeChanged(p2p: Boolean) {
            addEvent("onStreamingModeChanged: p2p $p2p")
            _p2p.value = p2p
            if (p2p) {
                pauseExoPlayers()
            }
        }

        override fun onVideoSourceUpdate(
            visibleUsers: List<UserInfo>,
            presenter: UserInfo?,
        ) {
            addEvent("onVideoSourceUpdate: $visibleUsers; presenter $presenter")
            if (!_presentationActive.value && presenter?.id != null && presenter.id != eyesonMeeting.getUserInfo()?.id) {
                lastCameraState = isVideoEnabled()
                _cameraActive.value = false
                eyesonMeeting.setVideoEnabled(false)
            }
            if (_presentationActive.value && presenter == null) {
                eyesonMeeting.setVideoEnabled(lastCameraState)
                _cameraActive.value = lastCameraState
            }
        }

        override fun onAudioMutedBy(user: UserInfo) {
            addEvent("onAudioMutedBy: user $user")
            setLocalAudioEnabled(enabled = false)
        }

        override fun onMediaPlayback(playing: List<Playback>) {
            addEvent("onMediaPlayback: playing $playing")

            val playMedia = { exoPlayer: ExoPlayer, playback: Playback ->
                viewModelScope.launch(Dispatchers.Main) {
                    exoPlayer.volume = if (playback.audio) 1F else 0F
                    exoPlayer.setMediaItem(MediaItem.fromUri(playback.url))
                }
            }
            if (_p2p.value) {
                for (playback in playing) {
                    if (!_localVideoPlaybackActive.value &&
                        playback.replacedUser?.id == (eyesonMeeting.getUserInfo()?.id ?: continue)
                    ) {
                        _localVideoPlaybackActive.value = true
                        playMedia(localExoPlayer, playback)
                    }

                    if (!_remoteVideoPlaybackActive.value &&
                        _userInMeeting.value.find {
                            it.id == playback.replacedUser?.id &&
                                    it.id != eyesonMeeting.getUserInfo()?.id
                        } != null
                    ) {
                        _remoteVideoPlaybackActive.value = true
                        playMedia(remoteExoPlayer, playback)
                    }
                }
            }
        }

        override fun onMediaPlaybackStartResponse(
            playId: String?,
            mediaPlaybackResponse: MediaPlaybackResponse,
        ) {
            addEvent(
                "onMediaPlaybackStartResponse: mediaPlaybackResponse $mediaPlaybackResponse",
                error = !mediaPlaybackResponse.isSuccessful()
            )

            _localVideoPlaybackPlayId.value = playId
        }

        override fun onMediaPlaybackStopResponse(
            playId: String,
            mediaPlaybackResponse: MediaPlaybackResponse,
        ) {
            addEvent(
                "onMediaPlaybackStopResponse: playId: $playId mediaPlaybackResponse $mediaPlaybackResponse",
                error = mediaPlaybackResponse != MediaPlaybackResponse.OK
            )
            _localVideoPlaybackActive.value = false
            _localVideoPlaybackPlayId.value = null
        }

        override fun onPresentationUpdate(presentationUpdate: PresentationUpdate) {
            addEvent("onPresentationUpdate: presentationUpdate $presentationUpdate")
            _presentationActive.value = presentationUpdate.user != null
        }

        override fun onPresentationStartResponse(presentationResponse: PresentationResponse) {
            addEvent("onPresentationStartResponse: presentationResponse $presentationResponse")
        }


        override fun onPresentationStopResponse(presentationResponse: PresentationResponse) {
            addEvent("onPresentationStopResponse: presentationResponse $presentationResponse")
        }

        override fun onBroadcastUpdate(activeBroadcasts: BroadcastUpdate) {
            addEvent("onBroadcastUpdate: activeBroadcasts $activeBroadcasts")
        }

        override fun onRecordingUpdate(recording: Recording) {
            addEvent("onRecordingUpdate: recording $recording")
            _recordingActive.value = recording.duration == null
        }

        override fun onSnapshotUpdate(snapshots: SnapshotUpdate) {
            addEvent("onSnapshotUpdate: snapshots $snapshots")
        }

        override fun onConnectionStatisticUpdate(statistic: ConnectionStatistic) {
            // NOTE: omitted to reduce clutter
//            addEvent("onConnectionStatisticUpdate: statistic $statistic")
//            Timber.d("onConnectionStatisticUpdate: statistic $statistic")
        }

        override fun onOptionsUpdate(meetingOptions: MeetingOptions) {
            addEvent("onOptionsUpdate: meetingOptions $meetingOptions")
        }

        override fun onUserJoinedMeeting(users: List<UserInfo>) {
            addEvent("onUserJoinedMeeting: users $users")
        }

        override fun onUserLeftMeeting(users: List<UserInfo>) {
            addEvent("onUserLeftMeeting: users $users")
        }

        override fun onUserListUpdate(users: List<UserInfo>, playbackPlayIds: List<String>) {
            addEvent("onUserListUpdate: users $users; playbackPlayIds $playbackPlayIds")
            _userInMeeting.value = users
        }

        override fun onVoiceActivity(user: UserInfo, active: Boolean) {
            // NOTE: omitted to reduce clutter
//            addEvent("onVoiceActivity: users $user; active $active")
        }

        override fun onChatMessageReceived(
            user: UserInfo,
            message: String,
            timestamp: Date,
        ) {
            addEvent("onChatMessageReceived: user $user; message $message; timestamp $timestamp")

            emitChatMessage(user, message, timestamp)
        }

        override fun onCustomMessageReceived(user: UserInfo, message: String, timestamp: Date) {
            addEvent("onCustomMessageReceived: user $user; message $message; timestamp $timestamp")
        }

        override fun onCameraOpen(cameraName: String) {
            // NOTE: omitted to reduce clutter
//            addEvent("onCameraOpen: cameraName $cameraName")
            cameraDisconnected.value = false
        }

        override fun onCameraFirstFrameAvailable() {
            // NOTE: omitted to reduce clutter
//            addEvent("onCameraFirstFrameAvailable")
        }

        override fun onCameraClosed() {
            // NOTE: omitted to reduce clutter
//            addEvent("onCameraClosed")
        }

        override fun onCameraDisconnected() {
            // NOTE: omitted to reduce clutter
//            addEvent("onCameraDisconnected")
            cameraDisconnected.value = true
        }

        override fun onCameraFrozen(error: String) {
            addEvent("onCameraFrozen: error $error", true)
        }

        override fun onCameraError(error: String) {
            addEvent("onCameraError: error $error", true)
            cameraDisconnected.value = true
        }

        override fun onCameraSwitchDone(isFrontCamera: Boolean) {
            addEvent("onCameraSwitchDone: isFrontCamera $isFrontCamera")
        }

        override fun onCameraSwitchError(error: String) {
            addEvent("onCameraSwitchError: error $error", true)
        }
    }

    private val eyesonMeeting: EyesonMeeting by lazy {
        EyesonMeeting(application = application)
    }

    private val audioManager by lazy {
        EyesonAudioManager(application) {
            when (it) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    setRemoteAudioEnabled(_remoteAudioActive.value)
                }

                AudioManager.AUDIOFOCUS_LOSS -> {
                    // Permanently lost audio focus. No further callbacks will be received.
                    // Call [EyesonAudioManage.requestAudioFocus] to request focus again
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    eyesonMeeting.setRemoteAudioEnabled(false)
                }
            }
        }
    }

    private var lastCameraState = isVideoEnabled()

    var cameraDisconnected = mutableStateOf<Boolean>(false)
        private set

    private val _presentationActive = MutableStateFlow(false)
    val presentationActive: StateFlow<Boolean> = _presentationActive.asStateFlow()

    private val _screenShareActive = MutableStateFlow(false)
    val screenShareActive: StateFlow<Boolean> = _screenShareActive.asStateFlow()

    private val _cameraActive = MutableStateFlow(isVideoEnabled())
    val cameraActive: StateFlow<Boolean> = _cameraActive.asStateFlow()

    private val _microphoneActive = MutableStateFlow(meetingSettings.micOnStar)
    val microphoneActive: StateFlow<Boolean> = _microphoneActive.asStateFlow()

    private val _remoteAudioActive = MutableStateFlow(true)
    val remoteAudioActive: StateFlow<Boolean> = _remoteAudioActive.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _audioDevices = MutableStateFlow<List<AudioDevice>>(emptyList())
    val audioDevices: StateFlow<List<AudioDevice>> = _audioDevices.asStateFlow()

    private val _remoteVideoPlaybackActive = MutableStateFlow(false)
    val remoteVideoPlaybackActive: StateFlow<Boolean> = _remoteVideoPlaybackActive.asStateFlow()

    private val _localVideoPlaybackActive = MutableStateFlow(false)
    val localVideoPlaybackActive: StateFlow<Boolean> = _localVideoPlaybackActive.asStateFlow()

    private val _localVideoPlaybackPlayId = MutableStateFlow<String?>(null)
    val localVideoPlaybackPlayId: StateFlow<String?> = _localVideoPlaybackPlayId.asStateFlow()

    private val _recordingActive = MutableStateFlow(false)
    val recordingActive: StateFlow<Boolean> = _recordingActive.asStateFlow()

    private fun addEvent(text: String, error: Boolean = false) {
        Timber.d("addEvent: $text")
        _events.value = emptyList<EventEntry>() + EventEntry(text, Date(), error) + _events.value
    }

    var screenCaptureAsPresentation = false

    fun getEglContext(): EglBase.Context? {
        return eyesonMeeting.getEglContext()
    }


    val remoteExoPlayer = getExoPlayer {
        _remoteVideoPlaybackActive.value = false
    }
    val localExoPlayer = getExoPlayer {
        _localVideoPlaybackActive.value = false
        _localVideoPlaybackPlayId.value = null
    }

    init {
        listAvailableCameras()
    }

    private fun getExoPlayer(onPlaybackEnd: () -> Unit): ExoPlayer {
        return ExoPlayer.Builder(application).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            addListener(
                object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (!isPlaying && (playbackState == STATE_IDLE || playbackState == STATE_ENDED)) {
                            onPlaybackEnd()
                        }
                    }
                }
            )
            prepare()
        }
    }

    override fun onCleared() {
        super.onCleared()
        remoteExoPlayer.release()
        localExoPlayer.release()
    }

    fun connect(
        local: VideoSink?,
        remote: VideoSink?,
        mediaProjectionPermissionResultData: Intent? = null,
        notificationId: Int? = null,
        notification: Notification? = null,
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
                    local = local,
                    remote = remote,
                    eventListener = eventListener,
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
                    local = local,
                    remote = remote,
                    eventListener = eventListener,
                    microphoneEnabledOnStart = meetingSettings.micOnStar,
                    videoEnabledOnStart = meetingSettings.videoOnStart,
                    screenShareInfo = screenShareInfo
                )
            }

            savedStateHandle.get<String>(USER_TOKEN) != null -> {
                eyesonMeeting.connectPermalink(
                    userToken = checkNotNull(savedStateHandle.get<String>(USER_TOKEN)),
                    frontCamera = !meetingSettings.rearCamOnStart,
                    local = local,
                    remote = remote,
                    eventListener = eventListener,
                    microphoneEnabledOnStart = meetingSettings.micOnStar,
                    videoEnabledOnStart = meetingSettings.videoOnStart,
                    screenShareInfo = screenShareInfo
                )
            }

            savedStateHandle.get<String>(GUEST_TOKEN_PERMALINK) != null -> {
                viewModelScope.launch {

                    var permalinkMeetingInfo: PermalinkMeetingInfo?
                    while (true) {
                        permalinkMeetingInfo = eyesonMeeting.getPermalinkMeetingInfo(
                            checkNotNull(savedStateHandle.get<String>(GUEST_TOKEN_PERMALINK))
                        )
                        when {
                            permalinkMeetingInfo == null -> {
                                meetingState.value =
                                    MeetingState.ConnectionFailed(CallRejectionReason.NOT_FOUND)
                                audioManager.stop()
                                return@launch
                            }

                            permalinkMeetingInfo.room.startedAt != null -> {
                                break
                            }
                        }
                        delay(PERMALINK_GUEST_POLLING_INTERVAL_MILLIS)
                    }

                    eyesonMeeting.joinAsGuest(
                        guestToken = checkNotNull(savedStateHandle.get<String>(GUEST_TOKEN_PERMALINK)),
                        name = checkNotNull(savedStateHandle.get<String>(GUEST_NAME_PERMALINK)),
                        id = null,
                        avatar = null,
                        frontCamera = !meetingSettings.rearCamOnStart,
                        local = local,
                        remote = remote,
                        eventListener = eventListener,
                        microphoneEnabledOnStart = meetingSettings.micOnStar,
                        videoEnabledOnStart = meetingSettings.videoOnStart,
                        screenShareInfo = screenShareInfo
                    )
                }
            }

            else -> {
                return
            }
        }
        startAudioManager()
    }

    fun disconnect() {
        audioManager.stop()

        remoteExoPlayer.release()
        localExoPlayer.release()

        eyesonMeeting.leave()
        meetingState.value = MeetingState.Disconnected
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
        setLocalVideoEnabled(!isVideoEnabled())
    }

    fun setLocalVideoEnabled(enabled: Boolean) {
        lastCameraState = enabled
        _cameraActive.value = enabled
        eyesonMeeting.setVideoEnabled(enabled)
    }

    private fun setLocalAudioEnabled(enabled: Boolean) {
        _microphoneActive.value = enabled
        eyesonMeeting.setMicrophoneEnabled(enabled)
    }

    fun toggleLocalMicrophone() {
        setLocalAudioEnabled(!isMicrophoneEnabled())
    }

    fun setRemoteAudioEnabled(enabled: Boolean) {
        _remoteAudioActive.value = enabled
        eyesonMeeting.setRemoteAudioEnabled(enabled)
    }

    fun toggleRemoteAudio() {
        setRemoteAudioEnabled(!_remoteAudioActive.value)
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
                availableAudioDevices: Set<EyesonAudioManager.AudioDevice>,
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
        notification: Notification,
    ) {
        val started = eyesonMeeting.startScreenShare(
            EyesonMeeting.ScreenShareInfo(
                mediaProjectionPermissionResultData,
                notificationId,
                notification
            ),
            asPresentation = screenCaptureAsPresentation
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
    }

    fun stopFullScreenPresentation() {
        eyesonMeeting.stopPresentation()
    }

    fun startVideoPlayback(
        url: String,
        replaceOwnVideo: Boolean,
        playAudio: Boolean,
        name: String? = null,
        playerId: String = UUID.randomUUID().toString(),
    ): String {
        eyesonMeeting.startVideoPlayback(
            url = url,
            name = name,
            playId = playerId,
            replacedUser = if (replaceOwnVideo) {
                eyesonMeeting.getUserInfo()
            } else {
                null
            },
            audio = playAudio
        )

        return playerId
    }

    fun stopVideoPlayback() {
        viewModelScope.launch(Dispatchers.Main) {
            localExoPlayer.pause()
            localExoPlayer.clearMediaItems()
        }
        eyesonMeeting.stopVideoPlayback(_localVideoPlaybackPlayId.value ?: return)
    }

    fun pauseExoPlayers() {
        val pausePayer = { exoPlayer: ExoPlayer ->
            viewModelScope.launch(Dispatchers.Main) {
                when (exoPlayer.playbackState) {
                    STATE_BUFFERING, STATE_READY -> {
                        exoPlayer.pause()
                        exoPlayer.clearMediaItems()
                    }

                    else -> Unit
                }
            }
        }

        pausePayer(localExoPlayer)
        _localVideoPlaybackActive.value = false

        pausePayer(remoteExoPlayer)
        _remoteVideoPlaybackActive.value = false
    }


    private fun listAvailableCameras() {
        val cameraManager = application.getSystemService(CAMERA_SERVICE) as CameraManager
        val availableCameras = cameraManager.cameraIdList.map {
            val lensFacing = cameraManager.getCameraCharacteristics(it).get(LENS_FACING).run {
                when (this) {
                    CameraCharacteristics.LENS_FACING_BACK -> "back"
                    CameraCharacteristics.LENS_FACING_FRONT -> "front"
                    else -> "external"
                }
            }

            mapOf(it to lensFacing)
        }
        Timber.d("Available cameras: $availableCameras")
    }


    companion object {
        const val PERMALINK_GUEST_POLLING_INTERVAL_MILLIS = 5_000L
    }
}

sealed interface MeetingState {
    data object Initial : MeetingState
    data object Connecting : MeetingState
    data object Connected : MeetingState
    data object Disconnected : MeetingState
    data class ConnectionFailed(val reason: CallRejectionReason) : MeetingState
    data class ConnectionTerminated(val reason: CallTerminationReason) : MeetingState
}

sealed class ChatMessage {
    data class OutgoingMessage(val text: String, val time: Date) : ChatMessage()
    data class IncomingMessage(
        val text: String,
        val from: String,
        val time: Date,
        val avatarUrl: String? = null,
    ) : ChatMessage()
}

data class EventEntry(val event: String, val time: Date, val error: Boolean = false)

data class AudioDevice(
    val title: String,
    val selected: Boolean,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)
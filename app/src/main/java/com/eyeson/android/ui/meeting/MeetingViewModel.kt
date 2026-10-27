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
import com.eyeson.android.EyesonDemoDestination.Meeting.JoinMode
import com.eyeson.android.R
import com.eyeson.android.data.SettingsRepository
import com.eyeson.android.ui.meeting.ChatMessage.IncomingMessage
import com.eyeson.android.ui.meeting.ChatMessage.OutgoingMessage
import com.eyeson.sdk.EyesonAudioManager
import com.eyeson.sdk.EyesonMeeting
import com.eyeson.sdk.events.AudioMutedBy
import com.eyeson.sdk.events.BroadcastUpdated
import com.eyeson.sdk.events.CallRejectionReason
import com.eyeson.sdk.events.CallTerminationReason
import com.eyeson.sdk.events.CameraClosed
import com.eyeson.sdk.events.CameraDisconnected
import com.eyeson.sdk.events.CameraError
import com.eyeson.sdk.events.CameraFirstFrameAvailable
import com.eyeson.sdk.events.CameraFrozen
import com.eyeson.sdk.events.CameraOpened
import com.eyeson.sdk.events.CameraSwitchDone
import com.eyeson.sdk.events.CameraSwitchError
import com.eyeson.sdk.events.ChatMessageReceived
import com.eyeson.sdk.events.ConnectionStatisticUpdated
import com.eyeson.sdk.events.CustomMessageReceived
import com.eyeson.sdk.events.EyesonEvent
import com.eyeson.sdk.events.MediaPlayback
import com.eyeson.sdk.events.MediaPlaybackResponse
import com.eyeson.sdk.events.MediaPlaybackStarted
import com.eyeson.sdk.events.MediaPlaybackStopped
import com.eyeson.sdk.events.MeetingJoinFailed
import com.eyeson.sdk.events.MeetingJoined
import com.eyeson.sdk.events.MeetingJoining
import com.eyeson.sdk.events.MeetingLocked
import com.eyeson.sdk.events.MeetingTerminated
import com.eyeson.sdk.events.OptionsUpdated
import com.eyeson.sdk.events.PresentationStarted
import com.eyeson.sdk.events.PresentationStopped
import com.eyeson.sdk.events.PresentationUpdated
import com.eyeson.sdk.events.RecordingUpdated
import com.eyeson.sdk.events.SnapshotUpdated
import com.eyeson.sdk.events.StreamingModeChanged
import com.eyeson.sdk.events.UserJoined
import com.eyeson.sdk.events.UserLeft
import com.eyeson.sdk.events.UserListUpdated
import com.eyeson.sdk.events.VideoSourceUpdated
import com.eyeson.sdk.events.VoiceActivityChanged
import com.eyeson.sdk.model.local.api.PermalinkMeetingInfo
import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.api.Playback
import com.eyeson.sdk.options.MediaOptions
import com.eyeson.sdk.options.Resolution16by9
import com.eyeson.sdk.options.Resolution4by3
import com.eyeson.sdk.options.ScreenShareMaxResolution
import com.eyeson.sdk.options.ScreenShareOptions
import com.eyeson.sdk.options.VideoOptions
import com.eyeson.sdk.options.VideoResolution
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.webrtc.VideoSink
import timber.log.Timber
import java.util.Date
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
@HiltViewModel(assistedFactory = MeetingViewModel.Factory::class)
class MeetingViewModel @AssistedInject constructor(
    @Assisted private val joinMode: JoinMode,
    private val application: Application,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(joinMode: JoinMode): MeetingViewModel
    }

    var meetingState = mutableStateOf<MeetingState>(MeetingState.Initial)
        private set

    val meetingSettings = runBlocking { settingsRepository.meetingSettings.first() }

    private val _events = MutableStateFlow<List<EventEntry>>(emptyList())
    val events: StateFlow<List<EventEntry>> = _events.asStateFlow()

    private val _p2p = MutableStateFlow(false)
    val p2p: StateFlow<Boolean> = _p2p.asStateFlow()

    private val _userInMeeting = MutableStateFlow(emptyList<UserInfo>())
    val userInMeeting: StateFlow<List<UserInfo>> = _userInMeeting.asStateFlow()

    private val eyesonMeeting: EyesonMeeting by lazy {
        EyesonMeeting(application = application)
    }

    init {
        viewModelScope.launch {
            eyesonMeeting.events.collectLatest {
                handleEvent(it)
            }
        }
    }

    private fun handleEvent(event: EyesonEvent) {
        when (event) {
            is MeetingJoining -> {
                with(event.meetingInfo) {
                    addEvent(
                        "MeetingJoining: accessKey: $accessKey;  name $name; startedAt $startedAt; user $user; " +
                                "locked $locked; guestToke $guestToken; guestLink $guestLink; activeRecording " +
                                "$activeRecording; activeBroadcasts $activeBroadcasts; snapshots $snapshots;" +
                                " meetingOptions $meetingOptions, playbacks $playbacks"
                    )
                }
                meetingState.value = MeetingState.Connecting
                _recordingActive.value = event.meetingInfo.activeRecording != null
            }

            is MeetingJoined -> {
                addEvent("MeetingJoined")
                meetingState.value = MeetingState.Connected
                lastCameraState = isVideoEnabled()
                _cameraActive.value = isVideoEnabled()
                // The aspect ratio of the meeting is only known now.
                _videoSettings.value = videoSettingsState(eyesonMeeting.isWidescreen())
                // Screen share on start is handled inside join(), so ask the SDK instead of
                // assuming the share is off.
                _screenShareActive.value = eyesonMeeting.isScreenShareActive()
            }

            is MeetingJoinFailed -> {
                addEvent(
                    "MeetingJoinFailed: callRejectionReason ${event.callRejectionReason}",
                    true
                )
                audioManager.stop()
                meetingState.value = MeetingState.ConnectionFailed(event.callRejectionReason)
            }

            is MeetingTerminated -> {
                addEvent("MeetingTerminated: callTerminationReason ${event.callTerminationReason}")
                audioManager.stop()
                meetingState.value = MeetingState.ConnectionTerminated(event.callTerminationReason)
            }

            is MeetingLocked -> {
                addEvent("MeetingLocked: locked ${event.locked}", true)
            }

            is StreamingModeChanged -> {
                addEvent("StreamingModeChanged: p2p ${event.p2p}")
                _p2p.value = event.p2p
                if (event.p2p) {
                    pauseExoPlayers()
                }
            }

            is VideoSourceUpdated -> {
                addEvent("VideoSourceUpdated: ${event.visibleUsers}; presenter ${event.presenter}")
                if (!_presentationActive.value && event.presenter?.id != null && event.presenter?.id != eyesonMeeting.getUserInfo()?.id) {
                    lastCameraState = isVideoEnabled()
                    _cameraActive.value = false
                    eyesonMeeting.setVideoEnabled(false)
                }
                if (_presentationActive.value && event.presenter == null) {
                    eyesonMeeting.setVideoEnabled(lastCameraState)
                    _cameraActive.value = lastCameraState
                }
            }

            is AudioMutedBy -> {
                addEvent("AudioMutedBy: user ${event.user}")
                setLocalAudioEnabled(enabled = false)
            }

            is MediaPlayback -> {
                addEvent("MediaPlayback: playing ${event.playing}")

                val playMedia = { exoPlayer: ExoPlayer, playback: Playback ->
                    viewModelScope.launch(Dispatchers.Main) {
                        exoPlayer.volume = if (playback.audio) 1F else 0F
                        exoPlayer.setMediaItem(MediaItem.fromUri(playback.url))
                    }
                }
                if (_p2p.value) {
                    for (playback in event.playing) {
                        if (!_localVideoPlaybackActive.value &&
                            playback.replacedUser?.id == (eyesonMeeting.getUserInfo()?.id
                                ?: continue)
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

            is MediaPlaybackStarted -> {
                addEvent(
                    "MediaPlaybackStarted: mediaPlaybackResponse ${event.mediaPlaybackResponse}",
                    error = !event.mediaPlaybackResponse.isSuccessful()
                )

                _localVideoPlaybackPlayId.value = event.playId

            }

            is MediaPlaybackStopped -> {
                addEvent(
                    "MediaPlaybackStopped: playId: ${event.playId} mediaPlaybackResponse ${event.mediaPlaybackResponse}",
                    error = event.mediaPlaybackResponse != MediaPlaybackResponse.OK
                )
                _localVideoPlaybackActive.value = false
                _localVideoPlaybackPlayId.value = null
            }

            is PresentationUpdated -> {
                addEvent("PresentationUpdated: presentationUpdate ${event.presentationUpdate}")
                _presentationActive.value = event.presentationUpdate.user != null
            }

            is PresentationStarted -> {
                addEvent("PresentationStarted: presentationResponse ${event.presentationResponse}")
            }

            is PresentationStopped -> {
                addEvent("PresentationStopped: presentationResponse ${event.presentationResponse}")
            }

            is BroadcastUpdated -> {
                addEvent("BroadcastUpdated: activeBroadcasts ${event.activeBroadcasts}")
            }

            is RecordingUpdated -> {
                addEvent("RecordingUpdated: recording ${event.recording}")
                _recordingActive.value = event.recording.duration == null
            }

            is SnapshotUpdated -> {
                addEvent("SnapshotUpdated: snapshots ${event.snapshots}")
            }

            is ConnectionStatisticUpdated -> {
                // NOTE: omitted to reduce clutter
//                addEvent("ConnectionStatisticUpdated: statistic ${event.statistic}")
            }

            is OptionsUpdated -> {
                addEvent("OptionsUpdated: meetingOptions ${event.meetingOptions}")
            }

            is UserJoined -> {
                addEvent("UserJoined: users ${event.users}")
            }

            is UserLeft -> {
                addEvent("UserLeft: users ${event.users}")
            }

            is UserListUpdated -> {
                addEvent("UserListUpdated: users ${event.users}; playbackPlayIds ${event.playbackPlayIds}")
                _userInMeeting.value = event.users
            }

            is VoiceActivityChanged -> {
                // NOTE: omitted to reduce clutter
//                addEvent("VoiceActivityChanged: users ${event.user}; active ${event.active}")
            }

            is ChatMessageReceived -> {
                addEvent("ChatMessageReceived: user ${event.user}; message ${event.message}; timestamp ${event.timestamp}")
                emitChatMessage(event.user, event.message, event.timestamp)
            }

            is CustomMessageReceived -> {
                addEvent("CustomMessageReceived: user ${event.user}; message ${event.message}; timestamp ${event.timestamp}")
            }

            is CameraOpened -> {
                // NOTE: omitted to reduce clutter
//                addEvent("CameraOpened: cameraName ${event.cameraName}")
                cameraDisconnected.value = false
            }

            is CameraFirstFrameAvailable -> {
                // NOTE: omitted to reduce clutter
//                addEvent("CameraFirstFrameAvailable")
            }

            is CameraClosed -> {
                // NOTE: omitted to reduce clutter
//                addEvent("CameraClosed")
            }

            is CameraDisconnected -> {
                // NOTE: omitted to reduce clutter
//                addEvent("CameraDisconnected")
                cameraDisconnected.value = true
            }

            is CameraFrozen -> {
                addEvent("CameraFrozen: error ${event.error}", true)
            }

            is CameraError -> {
                addEvent("CameraError: error ${event.error}", true)
                cameraDisconnected.value = true
            }

            is CameraSwitchDone -> {
                addEvent("CameraSwitchDone: isFrontCamera ${event.isFrontCamera}")
            }

            is CameraSwitchError -> {
                addEvent("CameraSwitchError: error ${event.error}", true)
            }
        }
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

    private val _microphoneActive = MutableStateFlow(meetingSettings.micOnStart)
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

    // Only the resolutions matching the aspect ratio of the meeting are offered. The SDK bounds
    // the two modes differently, so a 4:3 resolution in a widescreen meeting would silently be
    // scaled back to the widescreen maximum.
    private val widescreenResolutions = listOf(
        Resolution16by9.LOWEST,
        Resolution16by9.LOWER,
        Resolution16by9.DEFAULT,
        Resolution16by9.HIGH,
        Resolution16by9.HIGHEST,
    ).map { Resolution(it, "${it.width}x${it.height}") }

    private val standardResolutions = listOf(
        Resolution4by3.LOWEST,
        Resolution4by3.LOWER,
        Resolution4by3.DEFAULT,
        Resolution4by3.HIGH,
        Resolution4by3.HIGHEST,
    ).map { Resolution(it, "${it.width}x${it.height}") }

    // Screen share is bound to one maximum regardless of the meeting aspect ratio, so unlike the
    // camera it only needs a single list.
    private val screenShareResolutions = listOf(
        Resolution16by9.LOWEST,
        Resolution16by9.LOWER,
        Resolution16by9.DEFAULT,
        Resolution16by9.HIGH,
        Resolution16by9.HIGHEST,
        ScreenShareMaxResolution,
    ).map { Resolution(it, "${it.width}x${it.height}") }

    private val scaleDownByValues = listOf(1.0, 1.5, 2.0, 3.0, 4.0, 8.0, 16.0)

    private val fpsValues = listOf(0, 5, 10, 15, 20, 25, 30)

    private val maxBitrateValues = listOf(100, 250, 500, 1000, 1500, 2000)

    // isWidescreen() only knows the answer once the meeting is joined, so this is seeded again
    // on MeetingJoined. The lists above have to stay declared before this.
    private val _videoSettings = MutableStateFlow(videoSettingsState(widescreen = false))
    val videoSettings: StateFlow<VideoSettingsState> = _videoSettings.asStateFlow()

    // Unlike the camera settings this never depends on isWidescreen(), so seeding it once is enough.
    private val _screenShareSettings = MutableStateFlow(screenShareSettingsState())
    val screenShareSettings: StateFlow<ScreenShareSettingsState> =
        _screenShareSettings.asStateFlow()

    /**
     * Every control starts on the value the SDK uses when nothing was changed, which is what
     * [resetVideoOptions] goes back to.
     */
    private fun videoSettingsState(widescreen: Boolean): VideoSettingsState {
        val resolutions = if (widescreen) widescreenResolutions else standardResolutions
        val defaultResolution = if (widescreen) {
            Resolution16by9.DEFAULT
        } else {
            Resolution4by3.DEFAULT
        }
        val defaults = VideoOptions()

        return VideoSettingsState(
            resolutions = SettingsRange(
                values = resolutions,
                selectedIndex = resolutions
                    .indexOfFirst { it.resolution == defaultResolution }
                    .coerceAtLeast(0),
                onIndexChange = ::setResolutionIndex
            ),
            scaleDownBy = SettingsRange(
                values = scaleDownByValues,
                selectedIndex = scaleDownByValues
                    .indexOf(defaults.scaleResolutionDownBy)
                    .coerceAtLeast(0),
                onIndexChange = ::setScaleDownByIndex
            ),
            fps = SettingsRange(
                values = fpsValues,
                selectedIndex = fpsValues.indexOf(defaults.videoFps).coerceAtLeast(0),
                onIndexChange = ::setFpsIndex
            ),
            maxBitrate = SettingsRange(
                values = maxBitrateValues,
                selectedIndex = maxBitrateValues
                    .indexOf(defaults.videoMaxBitrate)
                    .coerceAtLeast(0),
                onIndexChange = ::setMaxBitrateIndex
            )
        )
    }

    /**
     * Every control starts on the value the SDK uses when nothing was changed, which is what
     * [resetScreenShareOptions] goes back to.
     */
    private fun screenShareSettingsState(): ScreenShareSettingsState {
        val defaults = ScreenShareOptions()

        return ScreenShareSettingsState(
            resolutions = SettingsRange(
                values = screenShareResolutions,
                selectedIndex = screenShareResolutions
                    .indexOfFirst { it.resolution == defaults.screenShareResolution }
                    .coerceAtLeast(0),
                onIndexChange = ::setScreenShareResolutionIndex
            ),
            scaleDownBy = SettingsRange(
                values = scaleDownByValues,
                selectedIndex = scaleDownByValues
                    .indexOf(defaults.scaleResolutionDownBy)
                    .coerceAtLeast(0),
                onIndexChange = ::setScreenShareScaleDownByIndex
            ),
            fps = SettingsRange(
                values = fpsValues,
                selectedIndex = fpsValues.indexOf(defaults.screenShareFps).coerceAtLeast(0),
                onIndexChange = ::setScreenShareFpsIndex
            ),
            maxBitrate = SettingsRange(
                values = maxBitrateValues,
                selectedIndex = maxBitrateValues
                    .indexOf(defaults.videoMaxBitrate)
                    .coerceAtLeast(0),
                onIndexChange = ::setScreenShareMaxBitrateIndex
            )
        )
    }

    private fun setResolutionIndex(index: Int) {
        _videoSettings.update { it.copy(resolutions = it.resolutions.copy(selectedIndex = index)) }
    }

    private fun setScaleDownByIndex(index: Int) {
        _videoSettings.update { it.copy(scaleDownBy = it.scaleDownBy.copy(selectedIndex = index)) }
    }

    private fun setFpsIndex(index: Int) {
        _videoSettings.update { it.copy(fps = it.fps.copy(selectedIndex = index)) }
    }

    private fun setMaxBitrateIndex(index: Int) {
        _videoSettings.update { it.copy(maxBitrate = it.maxBitrate.copy(selectedIndex = index)) }
    }

    private fun setScreenShareResolutionIndex(index: Int) {
        _screenShareSettings.update {
            it.copy(resolutions = it.resolutions.copy(selectedIndex = index))
        }
    }

    private fun setScreenShareScaleDownByIndex(index: Int) {
        _screenShareSettings.update {
            it.copy(scaleDownBy = it.scaleDownBy.copy(selectedIndex = index))
        }
    }

    private fun setScreenShareFpsIndex(index: Int) {
        _screenShareSettings.update { it.copy(fps = it.fps.copy(selectedIndex = index)) }
    }

    private fun setScreenShareMaxBitrateIndex(index: Int) {
        _screenShareSettings.update {
            it.copy(maxBitrate = it.maxBitrate.copy(selectedIndex = index))
        }
    }

    private fun addEvent(text: String, error: Boolean = false) {
        Timber.d("addEvent: $text")
        _events.value = emptyList<EventEntry>() + EventEntry(text, Date(), error) + _events.value
    }

    var screenCaptureAsPresentation = false

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

        val mediaOptions = MediaOptions(
            sendVideo = !meetingSettings.audioOnly,
            receiveVideo = !meetingSettings.audioOnly
        )

        val joinResult = when (joinMode) {
            is JoinMode.AccessKey -> {
                eyesonMeeting.join(
                    accessKey = joinMode.accessKey,
                    frontCamera = !meetingSettings.rearCamOnStart,
                    local = local,
                    remote = remote,
                    microphoneEnabledOnStart = meetingSettings.micOnStart,
                    videoEnabledOnStart = meetingSettings.videoOnStart,
                    mediaOptions = mediaOptions,
                    screenShareInfo = screenShareInfo
                )
            }

            is JoinMode.Guest -> {
                eyesonMeeting.joinAsGuest(
                    guestToken = joinMode.guestToken,
                    name = joinMode.guestName,
                    id = null,
                    avatar = null,
                    frontCamera = !meetingSettings.rearCamOnStart,
                    local = local,
                    remote = remote,
                    microphoneEnabledOnStart = meetingSettings.micOnStart,
                    videoEnabledOnStart = meetingSettings.videoOnStart,
                    mediaOptions = mediaOptions,
                    screenShareInfo = screenShareInfo
                )
            }

            is JoinMode.UserTokenPermalink -> {
                eyesonMeeting.connectPermalink(
                    userToken = joinMode.userToken,
                    frontCamera = !meetingSettings.rearCamOnStart,
                    local = local,
                    remote = remote,
                    microphoneEnabledOnStart = meetingSettings.micOnStart,
                    videoEnabledOnStart = meetingSettings.videoOnStart,
                    mediaOptions = mediaOptions,
                    screenShareInfo = screenShareInfo
                )
            }

            is JoinMode.GuestPermalink -> {
                viewModelScope.launch {

                    var permalinkMeetingInfo: PermalinkMeetingInfo?
                    while (true) {
                        permalinkMeetingInfo = eyesonMeeting.getPermalinkMeetingInfo(joinMode.guestToken)
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
                        delay(PERMALINK_GUEST_POLLING_INTERVAL_MILLIS.milliseconds)
                    }

                    if (reportJoinNotStarted(
                            eyesonMeeting.joinAsGuest(
                                guestToken = joinMode.guestToken,
                                name = joinMode.guestName,
                                id = null,
                                avatar = null,
                                frontCamera = !meetingSettings.rearCamOnStart,
                                local = local,
                                remote = remote,
                                microphoneEnabledOnStart = meetingSettings.micOnStart,
                                videoEnabledOnStart = meetingSettings.videoOnStart,
                                mediaOptions = mediaOptions,
                                screenShareInfo = screenShareInfo
                            )
                        )
                    ) {
                        // The audio manager is already running: this join only happens once the
                        // permalink meeting has started.
                        audioManager.stop()
                    }
                }
                // Placeholder, not a real result: the join above only happens once the permalink
                // meeting has started, and reports itself from inside the coroutine.
                EyesonMeeting.JoinResult.Joining
            }
        }

        if (reportJoinNotStarted(joinResult)) {
            return
        }

        startAudioManager()
    }

    /**
     * Returns `true` if the join was not started.
     */
    private fun reportJoinNotStarted(joinResult: EyesonMeeting.JoinResult): Boolean {
        return when (joinResult) {
            EyesonMeeting.JoinResult.Joining -> false

            is EyesonMeeting.JoinResult.MissingPermissions -> {
                addEvent("Join not started; missing permissions: ${joinResult.permissions}", true)
                true
            }

            EyesonMeeting.JoinResult.AlreadyJoined -> {
                addEvent("Join not started; already in a meeting", true)
                true
            }
        }
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

    /**
     * `videoCodecHardwareAcceleration` is not forwarded because it is an `SdkOptions` value and
     * can only be set when the meeting is created.
     */
    fun applyVideoOptions() {
        val settings = _videoSettings.value
        Timber.d(
            "applyVideoOptions: resolution ${settings.resolutions.selected.resolution}; " +
                    "fps ${settings.fps.selected}; maxBitrate ${settings.maxBitrate.selected}; " +
                    "scaleResolutionDownBy ${settings.scaleDownBy.selected}"
        )
        eyesonMeeting.updateVideoOptions(
            videoResolution = settings.resolutions.selected.resolution,
            videoFps = settings.fps.selected,
            videoMaxBitrate = settings.maxBitrate.selected,
            scaleResolutionDownBy = settings.scaleDownBy.selected
        )
    }

    fun resetVideoOptions() {
        _videoSettings.value = videoSettingsState(eyesonMeeting.isWidescreen())
        applyVideoOptions()
    }

    fun applyScreenShareOptions() {
        val settings = _screenShareSettings.value
        Timber.d(
            "applyScreenShareOptions: resolution ${settings.resolutions.selected.resolution}; " +
                    "fps ${settings.fps.selected}; maxBitrate ${settings.maxBitrate.selected}; " +
                    "scaleResolutionDownBy ${settings.scaleDownBy.selected}"
        )
        eyesonMeeting.updateScreenShareOptions(
            screenShareFps = settings.fps.selected,
            screenShareResolution = settings.resolutions.selected.resolution,
            scaleResolutionDownBy = settings.scaleDownBy.selected,
            videoMaxBitrate = settings.maxBitrate.selected
        )
    }

    fun resetScreenShareOptions() {
        _screenShareSettings.value = screenShareSettingsState()
        applyScreenShareOptions()
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

data class Resolution(
    val resolution: VideoResolution,
    val label: String,
)

/**
 * One "pick one of N values" control. [onIndexChange] belongs to the control itself, the same way
 * [AudioDevice.onClick] does.
 */
data class SettingsRange<T>(
    val values: List<T>,
    val selectedIndex: Int,
    val onIndexChange: (Int) -> Unit,
) {
    val selected: T get() = values[selectedIndex]
}

data class VideoSettingsState(
    val resolutions: SettingsRange<Resolution>,
    val scaleDownBy: SettingsRange<Double>,
    val fps: SettingsRange<Int>,
    val maxBitrate: SettingsRange<Int>,
)

/**
 * Same controls as [VideoSettingsState], applied while screen sharing is the active source.
 */
data class ScreenShareSettingsState(
    val resolutions: SettingsRange<Resolution>,
    val scaleDownBy: SettingsRange<Double>,
    val fps: SettingsRange<Int>,
    val maxBitrate: SettingsRange<Int>,
)


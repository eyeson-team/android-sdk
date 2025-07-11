package com.eyeson.android.ui.meeting

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.app.NotificationCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.eyeson.android.R
import com.eyeson.android.service.MeetingActiveService
import com.eyeson.android.ui.components.Chat
import com.eyeson.android.ui.components.KeepScreenOn
import com.eyeson.android.ui.components.RecordingIndicator
import com.eyeson.android.ui.components.applyRoundedCornerPadding
import com.eyeson.android.ui.components.findActivity
import com.eyeson.android.ui.theme.DarkGray800
import com.eyeson.android.ui.theme.EyesonDemoTheme
import com.eyeson.android.ui.theme.OverlayMenuHorizontalShape
import com.eyeson.android.ui.theme.OverlayMenuVerticalShape
import com.eyeson.sdk.events.CallTerminationReason
import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.webrtc.VideoRenderer
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import kotlin.math.roundToInt


@Composable
fun MeetingRout(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MeetingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    val meetingState by viewModel.meetingState
    val presentationActive by viewModel.presentationActive.collectAsStateWithLifecycle()
    val screenShareActive by viewModel.screenShareActive.collectAsStateWithLifecycle()
    val remoteVideoPlaybackActive by viewModel.remoteVideoPlaybackActive.collectAsStateWithLifecycle()
    val localVideoPlaybackActive by viewModel.localVideoPlaybackActive.collectAsStateWithLifecycle()
    val videoPlaybackSelectable by viewModel.localVideoPlaybackPlayId.collectAsStateWithLifecycle()
    val sfu by viewModel.p2p.collectAsStateWithLifecycle()
    val cameraActive by viewModel.cameraActive.collectAsStateWithLifecycle()
    val microphoneActive by viewModel.microphoneActive.collectAsStateWithLifecycle()
    val cameraDisconnected by viewModel.cameraDisconnected
    val remoteAudioActive by viewModel.remoteAudioActive.collectAsStateWithLifecycle()

    val remoteVideoRenderer = rememberVideoRendererWithLifecycle(viewModel.getEglContext()) {
        viewModel.setRemoteVideoTarget(it)
    }

    val localVideoRenderer = rememberVideoRendererWithLifecycle(viewModel.getEglContext()) {
        viewModel.setLocalVideoTarget(it)
    }

    val events by viewModel.events.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val audioDevices by viewModel.audioDevices.collectAsStateWithLifecycle()
    val userInMeeting by viewModel.userInMeeting.collectAsStateWithLifecycle()
    val recordingActive by viewModel.recordingActive.collectAsStateWithLifecycle()

    val window = context.findActivity().window
    val windowInsetsController =
        WindowCompat.getInsetsController(window, window.decorView)

    val onOnBack = {
        viewModel.disconnect()
        if (screenShareActive) {
            viewModel.stopScreenShare()
        }
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        MeetingActiveService.stop(context)

        onBack()
    }
    BackHandler {
        onOnBack()
    }

    val startScreenShareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode != AppCompatActivity.RESULT_OK || it.data == null) {
            Toast.makeText(
                context,
                context.getString(R.string.screen_sharing_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            viewModel.startScreenShare(
                mediaProjectionPermissionResultData = checkNotNull(it.data),
                notificationId = SCREEN_SHARE_NOTIFICATION_ID,
                notification = generateScreenShareNotification(context)
            )
        }
    }

    val startScreenShare: () -> Unit = {
        val manager = context.getSystemService(MediaProjectionManager::class.java)
        startScreenShareLauncher.launch(manager.createScreenCaptureIntent())
    }

    val connectWithScreenShareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode != AppCompatActivity.RESULT_OK || it.data == null) {
            Toast.makeText(
                context,
                context.getString(R.string.screen_sharing_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            viewModel.connect(
                local = localVideoRenderer,
                remote = remoteVideoRenderer,
                mediaProjectionPermissionResultData = it.data,
                notificationId = SCREEN_SHARE_NOTIFICATION_ID,
                notification = generateScreenShareNotification(context)
            )
        }
    }

    LaunchedEffect(meetingState) {
        when (meetingState) {
            is MeetingState.Initial -> {
                if (viewModel.meetingSettings.screenShareOnStart) {
                    val manager = context.getSystemService(MediaProjectionManager::class.java)
                    connectWithScreenShareLauncher.launch(manager.createScreenCaptureIntent())
                } else {
                    viewModel.connect(local = localVideoRenderer, remote = remoteVideoRenderer)
                }
            }

            is MeetingState.ConnectionFailed -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.connecting_to_meeting_failed),
                    Toast.LENGTH_SHORT
                ).show()
                onOnBack()
            }

            is MeetingState.ConnectionTerminated -> {
                when ((meetingState as MeetingState.ConnectionTerminated).reason) {
                    CallTerminationReason.UNSPECIFIED -> {
                        null
                    }

                    CallTerminationReason.OK -> {
                        R.string.call_terminated_remotely
                    }

                    CallTerminationReason.FORBIDDEN -> {
                        R.string.call_terminated_forbidden
                    }

                    CallTerminationReason.UNWANTED -> {
                        R.string.call_terminated_unwanted
                    }

                    else -> {
                        R.string.call_terminated_error
                    }
                }?.let {
                    Toast.makeText(
                        context,
                        context.getString(it),
                        Toast.LENGTH_SHORT
                    ).show()
                    onOnBack()
                }
            }

            else -> {
                /* NOOP */
            }
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(key1 = lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when {
                event.targetState == Lifecycle.State.CREATED -> {

                    MeetingActiveService.start(context)
                }

                event == Lifecycle.Event.ON_START -> {
                    windowInsetsController.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

                    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                }

                event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_DESTROY -> {
                    viewModel.setRemoteVideoTarget(remoteVideoRenderer)

                    if (event == Lifecycle.Event.ON_DESTROY && context.findActivity().isFinishing) {
                        onOnBack()
                    }
                }
            }
        }

        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(key1 = lifecycle, key2 = cameraDisconnected) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && cameraDisconnected) {
                viewModel.setLocalVideoEnabled(true)
            }
        }

        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    MeetingScreen(
        meetingState = meetingState,
        onBack = onOnBack,
        audioOnly = viewModel.meetingSettings.audioOnly,
        presentationActive = presentationActive,
        screenShareActive = screenShareActive,
        remoteVideoPlaybackActive = remoteVideoPlaybackActive,
        remoteExoPlayer = viewModel.remoteExoPlayer,
        localVideoPlaybackActive = localVideoPlaybackActive,
        localExoPlayer = viewModel.localExoPlayer,
        videoPlaybackSelectable = videoPlaybackSelectable,
        sfu = sfu,
        cameraActive = cameraActive,
        microphoneActive = microphoneActive,
        remoteVideoRenderer = remoteVideoRenderer,
        setRemoteTarget = { viewModel.setRemoteVideoTarget(it) },
        localVideoRenderer = localVideoRenderer,
        setLocalTarget = { viewModel.setLocalVideoTarget(it) },
        events = events,
        chatMessages = chatMessages,
        audioDevices = audioDevices,
        userInMeeting = userInMeeting,
        wideScreen = viewModel.isWideScreen(),
        toggleMicrophoneActive = { viewModel.toggleLocalMicrophone() },
        muteVideo = { viewModel.toggleLocalVideo() },
        switchCamera = { viewModel.switchCamera() },
        remoteAudioActive = remoteAudioActive,
        toggleRemoteAudioActive = { viewModel.toggleRemoteAudio() },
        changeScreenShareActive = {
            if (screenShareActive) {
                viewModel.stopScreenShare()
            } else {
                viewModel.screenCaptureAsPresentation = false
                startScreenShare()
            }
        },
        startFullScreenPresentation = {
            viewModel.screenCaptureAsPresentation = true
            if (screenShareActive) {
                viewModel.setVideoAsPresentation()
            } else {
                startScreenShare()
            }
        },
        stopFullScreenPresentation = { viewModel.stopFullScreenPresentation() },
        startVideoPlayback = { url, replaceOwnVideo, audio ->
            viewModel.startVideoPlayback(url, replaceOwnVideo, audio)
        },
        stopVideoPlayback = { viewModel.stopVideoPlayback() },
        muteAll = { viewModel.muteAll() },
        sendChatMessage = { viewModel.sendChatMessage(it) },
        getEventsClip = { viewModel.getEventsClip() },
        clearLog = { viewModel.clearLog() },
        recordingActive = recordingActive,
        modifier = modifier,

        )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingScreen(
    meetingState: MeetingState,
    onBack: () -> Unit,
    audioOnly: Boolean,
    presentationActive: Boolean,
    screenShareActive: Boolean,
    remoteVideoPlaybackActive: Boolean,
    remoteExoPlayer: ExoPlayer?,
    localVideoPlaybackActive: Boolean,
    localExoPlayer: ExoPlayer?,
    videoPlaybackSelectable: String?,
    sfu: Boolean,
    cameraActive: Boolean,
    microphoneActive: Boolean,
    remoteVideoRenderer: VideoRenderer,
    setRemoteTarget: (VideoRenderer?) -> Unit,
    localVideoRenderer: VideoRenderer,
    setLocalTarget: (VideoRenderer?) -> Unit,
    events: List<EventEntry>,
    chatMessages: List<ChatMessage>,
    audioDevices: List<AudioDevice>,
    userInMeeting: List<UserInfo>,
    wideScreen: Boolean,
    toggleMicrophoneActive: () -> Unit,
    muteVideo: () -> Unit,
    switchCamera: () -> Unit,
    remoteAudioActive: Boolean,
    toggleRemoteAudioActive: () -> Unit,
    changeScreenShareActive: () -> Unit,
    startFullScreenPresentation: () -> Unit,
    stopFullScreenPresentation: () -> Unit,
    startVideoPlayback: (String, Boolean, Boolean) -> Unit,
    stopVideoPlayback: () -> Unit,
    muteAll: () -> Unit,
    sendChatMessage: (String) -> Unit,
    getEventsClip: () -> ClipData,
    clearLog: () -> Unit,
    recordingActive: Boolean,
    modifier: Modifier = Modifier,
) {

    var open by rememberSaveable { mutableStateOf(false) }
    var chatOpen by rememberSaveable { mutableStateOf(false) }


    var whatIsOpen by rememberSaveable { mutableIntStateOf(0) }
    var videoUrl by rememberSaveable {
        mutableStateOf(DEMO_VIDEO_URL)
    }

    var playAudio by rememberSaveable {
        mutableStateOf(false)
    }
    var replaceOwnVideo by rememberSaveable {
        mutableStateOf(true)
    }

    val context = LocalContext.current
    val configuration: Configuration = LocalConfiguration.current

    KeepScreenOn()


    val meetingContent =
        @Composable { meetingContentModifier: Modifier, modifierLocalView: Modifier ->
            val fullSizeRemote = if (configuration.isLandscape()) {
                false
            } else {
                sfu
            }
            MeetingContent(
                meetingState = meetingState,
                userInMeeting = userInMeeting,
                audioOnly = audioOnly,
                cameraActive = cameraActive,
                presentationActive = presentationActive,
                remoteVideoRenderer = remoteVideoRenderer,
                setRemoteTarget = setRemoteTarget,
                localVideoRenderer = localVideoRenderer,
                setLocalTarget = setLocalTarget,
                sfu = sfu,
                wideScreen = wideScreen,
                fullSizeRemote = fullSizeRemote,
                remoteVideoPlaybackActive = remoteVideoPlaybackActive,
                remoteExoPlayer = remoteExoPlayer,
                localVideoPlaybackActive = localVideoPlaybackActive,
                localExoPlayer = localExoPlayer,
                modifierLocalView = modifierLocalView,
                modifier = meetingContentModifier
            )
        }

    Box {
        if (configuration.isLandscape()) {
            Row(
                modifier = modifier
                    .background(DarkGray800)
                    .displayCutoutPadding()

            ) {
                VerticalMeetingControls(
                    onBack = onBack,
                    audioOnly = audioOnly,
                    cameraChangeable = !presentationActive,
                    onSwitchCamera = switchCamera,
                    videoMuted = cameraActive,
                    onMuteVideo = muteVideo,
                    microphoneMuted = !microphoneActive,
                    onMuteMicrophone = toggleMicrophoneActive,
                    audioMuted = !remoteAudioActive,
                    onMuteAudio = toggleRemoteAudioActive,
                    modifier = Modifier.applyRoundedCornerPadding(
                        leftFraction = 0f,
                        topFraction = 0.35f,
                        rightFraction = 0f,
                        bottomFraction = 0.35f
                    )
                )

                meetingContent(
                    Modifier.weight(1f),
                    Modifier
                        .padding(end = 16.dp, bottom = 16.dp)
                        .size(120.dp, 80.dp)
                        .fillMaxSize()
                        .zIndex(1f)
                )

                VerticalMeetingSettings(
                    openSetting = {
                        whatIsOpen = SETTINGS_DEFAULT
                        open = true
                    },
                    openChat = {
                        chatOpen = true
                    },
                    modifier = Modifier.applyRoundedCornerPadding(
                        leftFraction = 0f,
                        topFraction = 0.35f,
                        rightFraction = 0f,
                        bottomFraction = 0.35f
                    )
                )
            }

            if (meetingState is MeetingState.Connected && recordingActive) {
                RecordingIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )
            }
        } else {
            Column(modifier = modifier.displayCutoutPadding()) {
                TopAppBar(
                    title = { /* NOOP */ },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                stringResource(id = R.string.label_go_back),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            whatIsOpen = SETTINGS_DEFAULT
                            open = true
                        }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                stringResource(id = R.string.label_settings),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    modifier = Modifier.zIndex(1f)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkGray800)
                ) {
                    meetingContent(
                        Modifier
                            .align(Alignment.Center),
                        Modifier
                            .padding(end = 16.dp, bottom = 104.dp)
                            .size(80.dp, 120.dp)
                    )

                    if (meetingState is MeetingState.Connected && recordingActive) {
                        RecordingIndicator(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(top = 8.dp, start = 8.dp)
                        )
                    }

                    HorizontalMeetingControls(
                        audioOnly = audioOnly,
                        cameraChangeable = !presentationActive,
                        onSwitchCamera = switchCamera,
                        videoMuted = cameraActive,
                        onMuteVideo = muteVideo,
                        microphoneMuted = !microphoneActive,
                        onMuteMicrophone = toggleMicrophoneActive,
                        audioMuted = !remoteAudioActive,
                        onMuteAudio = toggleRemoteAudioActive,
                        onShowChat = { chatOpen = true },
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .align(Alignment.BottomEnd)
                            .zIndex(1f),
                    )
                }
            }

        }
    }

    val (vertical, horizontal, overlayMenuShape) = if (configuration.isLandscape()) {
        Triple(1f, 0.7f, OverlayMenuHorizontalShape)
    } else {
        Triple(0.7f, 1f, OverlayMenuVerticalShape)
    }

    Crossfade(targetState = whatIsOpen) { screen ->
        when (screen) {
            SETTINGS_AUDIO -> {
                AudioSettings(
                    visible = open,
                    onClose = {
                        whatIsOpen = SETTINGS_DEFAULT
                        open = false
                    },
                    audioDevices = audioDevices,
                    horizontalContentRatio = horizontal,
                    verticalContentRatio = vertical,
                    contentShape = overlayMenuShape,
                )
            }

            SETTINGS_EVENT_LOG -> {
                EventLog(
                    visible = open,
                    onClose = {
                        whatIsOpen = SETTINGS_DEFAULT
                        open = false
                    },
                    events = events,
                    onClear = clearLog,
                    onCopy = {
                        val clipboardManager =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

                        clipboardManager?.setPrimaryClip(getEventsClip())
                    },

                    horizontalContentRatio = horizontal,
                    verticalContentRatio = vertical,
                    contentShape = overlayMenuShape,
                )
            }

            SETTINGS_VIDEO_PLAYBACK -> {
                VideoPlayback(
                    visible = open,
                    onClose = {
                        whatIsOpen = SETTINGS_DEFAULT
                        open = false
                    },
                    videoUrl = videoUrl,
                    onVideoUrlChange = { videoUrl = it },
                    audio = playAudio,
                    onAudioToggle = { playAudio = it },
                    replaceOwnVideo = replaceOwnVideo,
                    onReplaceOwnVideoChange = { replaceOwnVideo = it },
                    onPlay = {
                        startVideoPlayback(videoUrl, replaceOwnVideo, playAudio)
                        whatIsOpen = SETTINGS_DEFAULT
                        open = false
                    },
                    horizontalContentRatio = horizontal,
                    verticalContentRatio = vertical,
                    contentShape = overlayMenuShape,
                )
            }

            else -> {
                MeetingSettings(
                    visible = open,
                    onClose = {
                        open = false
                        whatIsOpen = SETTINGS_DEFAULT
                    },
                    screenShareActive = screenShareActive,
                    presentationActive = presentationActive,
                    onScreenShareActiveChange = changeScreenShareActive,
                    startFullScreenPresentation = startFullScreenPresentation,
                    stopFullScreenPresentation = stopFullScreenPresentation,
                    showVideoPlayback = {
                        whatIsOpen = SETTINGS_VIDEO_PLAYBACK
                    },
                    isVideoPlaying = videoPlaybackSelectable != null,
                    stopVideoPlayback = stopVideoPlayback,
                    muteAll = {
                        muteAll()
                        Toast.makeText(
                            context,
                            context.getString(R.string.you_muted_all_other_participants),
                            Toast.LENGTH_SHORT
                        ).show()

                    },
                    showAudioSettings = {
                        whatIsOpen = SETTINGS_AUDIO
                    },
                    showEventLog = { whatIsOpen = SETTINGS_EVENT_LOG },
                    horizontalContentRatio = horizontal,
                    verticalContentRatio = vertical,
                    contentShape = overlayMenuShape,
                )
            }
        }

        val chatShape = if (configuration.isLandscape()) {
            RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
        } else {
            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        }

        Chat(
            visible = chatOpen,
            onClose = { chatOpen = !chatOpen },
            messages = chatMessages,
            sendMessage = sendChatMessage,
            contentShape = chatShape,
            verticalContentRatio = vertical,
            horizontalContentRatio = horizontal,
            inputTextModifier = if (!configuration.isLandscape()) {
                Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(
                        interactionSource = null,
                        indication = null
                    ) { /* NOOP */ }
                    .applyRoundedCornerPadding(
                        leftFraction = 0f,
                        topFraction = 0.0f,
                        rightFraction = 0f,
                        bottomFraction = 0.35f
                    )
            } else {
                Modifier
            },
        )
    }
}

@Composable
fun MeetingContent(
    meetingState: MeetingState,
    userInMeeting: List<UserInfo>,
    audioOnly: Boolean,
    cameraActive: Boolean,
    presentationActive: Boolean,
    remoteVideoRenderer: VideoRenderer,
    setRemoteTarget: (VideoRenderer?) -> Unit,
    localVideoRenderer: VideoRenderer,
    setLocalTarget: (VideoRenderer?) -> Unit,
    sfu: Boolean,
    wideScreen: Boolean,
    fullSizeRemote: Boolean,
    remoteVideoPlaybackActive: Boolean,
    remoteExoPlayer: ExoPlayer?,
    localVideoPlaybackActive: Boolean,
    localExoPlayer: ExoPlayer?,
    modifierLocalView: Modifier,
    modifier: Modifier = Modifier,
) {
    when (meetingState) {
        is MeetingState.Connecting -> {
            Connecting(
                modifier = modifier
            )
        }

        is MeetingState.Connected -> {
            if (audioOnly) {
                AudioOnly(
                    participants = userInMeeting.count(),
                    modifier = modifier
                )
            } else {
                VideoViews(
                    showLocal = sfu && cameraActive && !presentationActive,
                    fullSizeRemote = fullSizeRemote,
                    remoteVideoRenderer = remoteVideoRenderer,
                    setRemoteTarget = setRemoteTarget,
                    localVideoRenderer = localVideoRenderer,
                    setLocalTarget = setLocalTarget,
                    modifier = modifier,
                    modifierLocalView = modifierLocalView,
                    wideScreen = wideScreen,
                    remoteExoPlayer = if (remoteVideoPlaybackActive) {
                        remoteExoPlayer
                    } else {
                        null
                    },
                    localExoPlayer = if (localVideoPlaybackActive && sfu) {
                        localExoPlayer
                    } else {
                        null
                    }
                )
            }
        }

        else -> {
            /* NOOP */
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoViews(
    showLocal: Boolean,
    fullSizeRemote: Boolean,
    remoteVideoRenderer: VideoRenderer,
    setRemoteTarget: (VideoRenderer?) -> Unit,
    localVideoRenderer: VideoRenderer,
    setLocalTarget: (VideoRenderer?) -> Unit,
    modifier: Modifier = Modifier,
    remoteExoPlayer: ExoPlayer? = null,
    localExoPlayer: ExoPlayer? = null,
    modifierLocalView: Modifier = Modifier,
    wideScreen: Boolean = false,
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY: Float by remember { mutableFloatStateOf(0f) }

    val remoteModifier = when {
        fullSizeRemote -> {
            Modifier
                .fillMaxSize()
        }

        wideScreen -> {
            Modifier.aspectRatio(16f / 9f)
        }

        else -> {
            Modifier.aspectRatio(4f / 3f)
        }
    }

    Box(modifier.fillMaxSize()) {
        if (remoteExoPlayer == null) {
            AndroidView(modifier = remoteModifier.align(Alignment.Center), factory = {
                remoteVideoRenderer
            })
            setRemoteTarget(remoteVideoRenderer)
        } else {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        hideController()
                        useController = false
                        player = remoteExoPlayer
                    }
                },
                modifier = remoteModifier.align(Alignment.Center)
            )
        }

        Box(
            modifier = modifierLocalView
                .align(Alignment.BottomEnd)
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }) {

            if (localExoPlayer == null) {
                val localTarget = if (showLocal) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { localVideoRenderer })

                    localVideoRenderer
                } else {
                    null
                }
                setLocalTarget(localTarget)
            } else {
                AndroidView(
                    factory = { context ->
                        PlayerView(context).apply {
                            player = localExoPlayer
                            hideController()
                            useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun AudioOnly(
    participants: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = stringResource(id = R.string.participants),
                tint = Color.White,
            )
            Text(
                "$participants", style = MaterialTheme.typography.displayLarge.copy(
                    color = Color.White
                ),

                modifier = Modifier
                    .padding(start = 4.dp)
            )
        }
        Text(
            stringResource(id = R.string.audio_only),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color.White
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            stringResource(id = R.string.no_video_is_sent_or_received),
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White.copy(alpha = 0.6f)
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth(0.7f)
        )
    }
}

@Composable
fun Connecting(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = Color.White
        )
        Text(
            stringResource(id = R.string.connecting),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color.White
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
fun rememberVideoRendererWithLifecycle(
    eglContext: EglBase.Context?,
    setTarget: (VideoRenderer?) -> Unit,
): VideoRenderer {
    val currentSetTarget by rememberUpdatedState(setTarget)

    val context = LocalContext.current
    val videoRenderer = remember {
        VideoRenderer(context).apply {
            init(eglContext)
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(key1 = lifecycle, key2 = videoRenderer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_CREATE) {
                currentSetTarget(videoRenderer)
            }
        }

        lifecycle.addObserver(observer)
        onDispose {
            currentSetTarget(null)
            videoRenderer.release()
            lifecycle.removeObserver(observer)
        }
    }
    return videoRenderer
}

fun Configuration.isLandscape() = orientation == Configuration.ORIENTATION_LANDSCAPE

private fun generateScreenShareNotification(context: Context): Notification {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.getSystemService(NotificationManager::class.java).apply {
            createNotificationChannel(
                NotificationChannel(
                    SCREEN_SHARE_CHANNEL_ID,
                    SCREEN_SHARE_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
    }

    return NotificationCompat.Builder(context, SCREEN_SHARE_CHANNEL_ID)
        .setOngoing(true)
        .setSilent(true)
        .setContentText(context.getText(R.string.your_screen_is_currently_being_recorded))
        .setContentTitle(context.getText(R.string.screen_capture))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .setSmallIcon(R.drawable.cast_24)
        .setCategory(Notification.CATEGORY_SERVICE)
        .build()
}

@UnstableApi
@Preview
@Preview(name = "landscape", device = "spec:width=640dp,height=360dp,dpi=480")
@Composable
fun SettingsScreenPreview() {

    val context = LocalContext.current
    val meetingState = MeetingState.Connected
    EyesonDemoTheme {
        MeetingScreen(
            meetingState = meetingState,
            onBack = {/*NOOP*/ },
            audioOnly = false,
            presentationActive = false,
            screenShareActive = false,
            remoteVideoPlaybackActive = false,
            remoteExoPlayer = null,
            localVideoPlaybackActive = false,
            localExoPlayer = null,
            videoPlaybackSelectable = null,
            sfu = true,
            cameraActive = true, microphoneActive = true,
            remoteVideoRenderer = VideoRenderer(context),
            setRemoteTarget = {/*NOOP*/ },
            localVideoRenderer = VideoRenderer(context),
            setLocalTarget = {/*NOOP*/ },
            events = emptyList(),
            chatMessages = emptyList(),
            audioDevices = emptyList(),
            userInMeeting = emptyList(),
            wideScreen = true,
            toggleMicrophoneActive = {/*NOOP*/ },
            muteVideo = {/*NOOP*/ },
            switchCamera = {/*NOOP*/ },
            remoteAudioActive = true,
            toggleRemoteAudioActive = {/*NOOP*/ },
            changeScreenShareActive = {/*NOOP*/ },
            startFullScreenPresentation = {/*NOOP*/ },
            stopFullScreenPresentation = {/*NOOP*/ },
            startVideoPlayback = { _, _, _ -> /*NOOP*/ },
            stopVideoPlayback = {/*NOOP*/ },
            muteAll = {/*NOOP*/ },
            sendChatMessage = {/*NOOP*/ },
            getEventsClip = { ClipData.newPlainText("Eyeson SDK event log", "") },
            recordingActive = true,
            clearLog = {/*NOOP*/ },
        )
    }
}

private const val SETTINGS_DEFAULT = 0
private const val SETTINGS_AUDIO = 1
private const val SETTINGS_EVENT_LOG = 2
private const val SETTINGS_VIDEO_PLAYBACK = 3

private const val SCREEN_SHARE_NOTIFICATION_ID = 42
private const val SCREEN_SHARE_CHANNEL_ID = "7"
private const val SCREEN_SHARE_CHANNEL_NAME = "Screen share active"


private const val DEMO_VIDEO_URL =
    "https://s3.eu-west-1.amazonaws.com/eyeson.team.mediainject/eyeson-1950.webm"
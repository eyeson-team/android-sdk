package com.eyeson.android.ui.meeting

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.eyeson.android.R
import com.eyeson.android.service.MeetingActiveService
import androidx.compose.material3.CircularProgressIndicator
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.eyeson.android.ui.components.Chat
import com.eyeson.android.ui.components.KeepScreenOn
import com.eyeson.android.ui.components.findActivity
import com.eyeson.android.ui.theme.DarkGray800
import com.eyeson.android.ui.theme.EyesonDemoTheme
import com.eyeson.sdk.events.CallTerminationReason
import com.eyeson.sdk.webrtc.VideoRenderer
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun MeetingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MeetingViewModel = hiltViewModel(),
) {
    var open by rememberSaveable { mutableStateOf(false) }
    var chatOpen by rememberSaveable { mutableStateOf(false) }
    val callConnected by viewModel.callConnected.collectAsStateWithLifecycle()
    val callTerminated by viewModel.callTerminated.collectAsStateWithLifecycle()
    val meetingJoinFailed by viewModel.meetingJoinFailed.collectAsStateWithLifecycle()
    val presentationActive by viewModel.presentationActive.collectAsStateWithLifecycle()
    val screenShareActive by viewModel.screenShareActive.collectAsStateWithLifecycle()
    val remoteVideoPlaybackActive by viewModel.remoteVideoPlaybackActive.collectAsStateWithLifecycle()
    val localVideoPlaybackActive by viewModel.localVideoPlaybackActive.collectAsStateWithLifecycle()
    val videoPlaybackSelectable by viewModel.localVideoPlaybackPlayId.collectAsStateWithLifecycle()

    val sfu by viewModel.p2p.collectAsStateWithLifecycle()

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

    val videoActive by viewModel.cameraActive.collectAsStateWithLifecycle()
    val microphoneActive by viewModel.microphoneActive.collectAsStateWithLifecycle()

    val remoteView = rememberVideoRendererWithLifecycle(viewModel.getEglContext()) {
        viewModel.setRemoteVideoTarget(it)
    }

    val localView = rememberVideoRendererWithLifecycle(viewModel.getEglContext()) {
        viewModel.setLocalVideoTarget(it)
    }

    val events by viewModel.events.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val audioDevices by viewModel.audioDevices.collectAsStateWithLifecycle()

    val userInMeeting by viewModel.userInMeeting.collectAsStateWithLifecycle()

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
                local = localView,
                remote = remoteView,
                mediaProjectionPermissionResultData = it.data,
                notificationId = SCREEN_SHARE_NOTIFICATION_ID,
                notification = generateScreenShareNotification(context)
            )
        }
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

    KeepScreenOn()

    val onOnBack = {
        viewModel.disconnect()
        onBack()
    }
    BackHandler {
        onOnBack()
    }

    if (configuration.isLandscape()) {
        Row(modifier = modifier) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(DarkGray800)
                    .zIndex(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {

                VerticalMeetingControls(
                    onBack = onBack,
                    audioOnly = viewModel.meetingSettings.audioOnly,
                    cameraChangeable = !presentationActive,
                    onSwitchCamera = { viewModel.switchCamera() },
                    videoMuted = videoActive,
                    onMuteVideo = { viewModel.toggleLocalVideo() },
                    microphoneMuted = !microphoneActive,
                    onMuteMicrophone = { viewModel.toggleLocalMicrophone() }
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(DarkGray800),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    !callConnected -> {
                        Connecting()
                    }

                    viewModel.meetingSettings.audioOnly -> {
                        AudioOnly(participants = userInMeeting.count())
                    }

                    else -> {
                        VideoViews(
                            showLocal = sfu && videoActive,
                            fullSizeRemote = false,
                            remoteView = remoteView,
                            localView = localView,
                            setLocalTarget = { viewModel.setLocalVideoTarget(it) },
                            modifierLocalView = Modifier
                                .padding(end = 16.dp, bottom = 16.dp)
                                .size(120.dp, 80.dp)
                                .fillMaxSize()
                                .zIndex(1f),
                            wideScreen = viewModel.isWideScreen(),
                            remoteExoPlayer = if (remoteVideoPlaybackActive) {
                                viewModel.remoteExoPlayer
                            } else {
                                null
                            },
                            localExoPlayer = if (localVideoPlaybackActive && sfu) {
                                viewModel.localExoPlayer
                            } else {
                                null
                            }
                        )
                    }
                }
            }

            CompositionLocalProvider(LocalRippleConfiguration provides RippleConfiguration(color = MaterialTheme.colorScheme.inverseOnSurface)) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(DarkGray800)
                        .zIndex(1f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {

                    IconButton(onClick = {
                        whatIsOpen = SETTINGS_DEFAULT
                        open = true
                    }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            stringResource(id = R.string.label_settings),
                            tint = Color.White
                        )
                    }

                    IconButton(onClick = { chatOpen = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_chat_24),
                            stringResource(id = R.string.show_chat),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    } else {
        Column(modifier = modifier) {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onOnBack) {
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

                when {
                    !callConnected -> {
                        Connecting()
                    }

                    viewModel.meetingSettings.audioOnly -> {
                        AudioOnly(participants = userInMeeting.count())
                    }

                    else -> {
                        VideoViews(
                            showLocal = sfu && videoActive,
                            fullSizeRemote = sfu,
                            remoteView = remoteView,
                            localView = localView,
                            setLocalTarget = { viewModel.setLocalVideoTarget(it) },
                            modifier = Modifier
                                .align(Alignment.Center),
                            modifierLocalView = Modifier
                                .padding(end = 16.dp, bottom = 88.dp)
                                .size(80.dp, 120.dp),
                            wideScreen = viewModel.isWideScreen(),
                            remoteExoPlayer = if (remoteVideoPlaybackActive) {
                                viewModel.remoteExoPlayer
                            } else {
                                null
                            },
                            localExoPlayer = if (localVideoPlaybackActive && sfu) {
                                viewModel.localExoPlayer
                            } else {
                                null
                            }
                        )
                    }
                }

                HorizontalMeetingControls(
                    audioOnly = viewModel.meetingSettings.audioOnly,
                    cameraChangeable = !presentationActive,
                    onSwitchCamera = { viewModel.switchCamera() },
                    videoMuted = videoActive,
                    onMuteVideo = { viewModel.toggleLocalVideo() },
                    microphoneMuted = !microphoneActive,
                    onMuteMicrophone = { viewModel.toggleLocalMicrophone() },
                    onShowChat = { chatOpen = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .zIndex(1f),
                )
            }
        }
    }

    LaunchedEffect(key1 = Unit) {
        when {
            viewModel.meetingSettings.screenShareOnStart && !viewModel.inCall() -> {
                val manager = context.getSystemService(MediaProjectionManager::class.java)
                connectWithScreenShareLauncher.launch(manager.createScreenCaptureIntent())
            }

            !viewModel.inCall() -> {
                viewModel.connect(localView, remoteView)
            }
        }
    }

    LaunchedEffect(key1 = callTerminated) {
        when (callTerminated) {
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

    LaunchedEffect(key1 = meetingJoinFailed) {
        if (meetingJoinFailed) {
            Toast.makeText(
                context,
                context.getString(R.string.connecting_to_meeting_failed),
                Toast.LENGTH_SHORT
            ).show()
            onOnBack()
        }
    }

    val (vertical, horizontal) = if (configuration.isLandscape()) {
        Pair(1f, 0.7f)
    } else {
        Pair(0.7f, 1f)
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
                    audioDevices,
                    horizontalContentRatio = horizontal,
                    verticalContentRatio = vertical
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
                    onClear = { viewModel.clearLog() },
                    onCopy = {
                        val clipboardManager =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

                        clipboardManager?.setPrimaryClip(viewModel.getEventsClip())
                    },
                    horizontalContentRatio = horizontal,
                    verticalContentRatio = vertical
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
                        viewModel.startVideoPlayback(videoUrl, replaceOwnVideo, playAudio)
                        whatIsOpen = SETTINGS_DEFAULT
                        open = false
                    },
                    horizontalContentRatio = horizontal,
                    verticalContentRatio = vertical
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
                    onScreenShareActiveChange = {
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
                    showVideoPlayback = {
                        whatIsOpen = SETTINGS_VIDEO_PLAYBACK
                    },
                    isVideoPlaying = videoPlaybackSelectable != null,
                    stopVideoPlayback = { viewModel.stopVideoPlayback() },
                    muteAll = {
                        viewModel.muteAll()
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
                    verticalContentRatio = vertical
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
            sendMessage = {
                viewModel.sendChatMessage(it)
            },
            contentShape = chatShape,
            verticalContentRatio = vertical,
            horizontalContentRatio = horizontal
        )
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(key1 = lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when {
                event.targetState == Lifecycle.State.STARTED
                        && event == Lifecycle.Event.ON_PAUSE
                        && !context.findActivity().isChangingConfigurations
                        && viewModel.inCall() -> {
                    val intent = Intent(context, MeetingActiveService::class.java)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ContextCompat.startForegroundService(context, intent)
                    } else {
                        context.startService(intent)
                    }
                }

                event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_DESTROY -> {
                    val intent = Intent(context, MeetingActiveService::class.java)
                    context.stopService(intent)

                    viewModel.setRemoteVideoTarget(remoteView)

                    if (event == Lifecycle.Event.ON_DESTROY && context.findActivity().isFinishing) {
                        viewModel.disconnect()
                    }
                }
            }
        }

        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}

@UnstableApi
@Composable
private fun VideoViews(
    showLocal: Boolean,
    fullSizeRemote: Boolean,
    remoteView: VideoRenderer,
    localView: VideoRenderer,
    setLocalTarget: (VideoRenderer?) -> Unit,
    modifier: Modifier = Modifier,
    remoteExoPlayer: ExoPlayer? = null,
    localExoPlayer: ExoPlayer? = null,
    modifierLocalView: Modifier = Modifier,
    wideScreen: Boolean = false,
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY: Float by remember { mutableStateOf(0f) }

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

    Box(modifier) {
        if (remoteExoPlayer == null) {
            AndroidView(modifier = remoteModifier.align(Alignment.Center), factory = {
                remoteView
            })
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

        Box(modifier = modifierLocalView
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
                    AndroidView(modifier = Modifier.fillMaxSize(),
                        factory = { localView })

                    localView
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
            stringResource(id = R.string.audio_only), style = MaterialTheme.typography.bodyLarge.copy(
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
            stringResource(id = R.string.connecting), style = MaterialTheme.typography.bodyLarge.copy(
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
        .setSmallIcon(R.drawable.cast_24)
        .setCategory(Notification.CATEGORY_SERVICE)
        .build()
}

@UnstableApi
@Preview
@Composable
fun SettingsScreenPreview() {

    EyesonDemoTheme {
        MeetingScreen({/*NOOP*/ })
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
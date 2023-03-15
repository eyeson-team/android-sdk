package com.eyeson.android.ui.meeting

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyeson.android.MainActivity
import com.eyeson.android.R
import com.eyeson.android.ui.components.Chat
import com.eyeson.android.ui.components.KeepScreenOn
import com.eyeson.android.ui.components.findActivity
import com.eyeson.android.ui.theme.DarkGray800
import com.eyeson.android.ui.theme.EyesonDemoTheme
import com.eyeson.android.ui.theme.WhiteRippleTheme
import com.eyeson.sdk.events.CallTerminationReason
import com.eyeson.sdk.webrtc.VideoRenderer
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import kotlin.math.roundToInt

@Composable
fun MeetingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MeetingViewModel = hiltViewModel()
) {
    var open by rememberSaveable { mutableStateOf(false) }
    var chatOpen by rememberSaveable { mutableStateOf(false) }
    val callConnected by viewModel.callConnected.collectAsStateWithLifecycle()
    val callTerminated by viewModel.callTerminated.collectAsStateWithLifecycle()
    val meetingJoinFailed by viewModel.meetingJoinFailed.collectAsStateWithLifecycle()
    val presentationActive by viewModel.presentationActive.collectAsStateWithLifecycle()
    val screenShareActive by viewModel.screenShareActive.collectAsStateWithLifecycle()

    val sfu by viewModel.p2p.collectAsStateWithLifecycle()

    var whatIsOpen by rememberSaveable { mutableStateOf(0) }

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

    val userInMeetingCount by viewModel.userInMeetingCount.collectAsStateWithLifecycle()

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
                CompositionLocalProvider(LocalRippleTheme provides WhiteRippleTheme) {
                    IconButton(onClick = onOnBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            stringResource(id = R.string.label_go_back),
                            tint = Color.White
                        )
                    }
                }

                VerticalMeetingControls(
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
                        AudioOnly(participants = userInMeetingCount)
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
                            wideScreen = viewModel.isWideScreen()
                        )
                    }
                }
            }
            CompositionLocalProvider(LocalRippleTheme provides WhiteRippleTheme) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(DarkGray800)
                        .zIndex(1f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {

                    IconButton(onClick = {
                        whatIsOpen = 0
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
                backgroundColor = MaterialTheme.colors.surface,
                elevation = 0.dp,
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onOnBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            stringResource(id = R.string.label_go_back),
                            tint = MaterialTheme.colors.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        whatIsOpen = 0
                        open = true
                    }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            stringResource(id = R.string.label_settings),
                            tint = MaterialTheme.colors.onSurface
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
                        AudioOnly(participants = userInMeetingCount)
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
            1 -> {
                AudioSettings(
                    visible = open,
                    onClose = {
                        whatIsOpen = 0
                        open = false
                    },
                    audioDevices,
                    horizontalContentRatio = horizontal,
                    verticalContentRatio = vertical
                )
            }
            2 -> {
                EventLog(
                    visible = open,
                    onClose = {
                        whatIsOpen = 0
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
            else -> {
                MeetingSettings(
                    visible = open,
                    onClose = {
                        open = false
                        whatIsOpen = 0
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
                    stopFullScreenPresentation = { viewModel.stopScreenShare() },
                    muteAll = {
                        viewModel.muteAll()
                        Toast.makeText(
                            context,
                            context.getString(R.string.you_muted_all_other_participants),
                            Toast.LENGTH_SHORT
                        ).show()

                    },
                    showAudioSettings = {
                        whatIsOpen = 1
                    },
                    showEventLog = { whatIsOpen = 2 },
                    horizontalContentRatio = horizontal,
                    verticalContentRatio = vertical
                )
            }
        }

        val chatShape = if (configuration.isLandscape()) {
            MaterialTheme.shapes.large
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
                event == Lifecycle.Event.ON_STOP
                        && !context.findActivity().isChangingConfigurations
                        && viewModel.inCall() -> {

                    val notification = generateInCallNotification(context)

                    with(NotificationManagerCompat.from(context)) {
                        notify(IN_CALL_NOTIFICATION_ID, notification)
                    }
                }
                event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_DESTROY -> {
                    with(NotificationManagerCompat.from(context)) {
                        cancel(IN_CALL_NOTIFICATION_ID)
                    }
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

@Composable
private fun VideoViews(
    showLocal: Boolean,
    fullSizeRemote: Boolean,
    remoteView: VideoRenderer,
    localView: VideoRenderer,
    setLocalTarget: (VideoRenderer?) -> Unit,
    modifier: Modifier = Modifier,
    modifierLocalView: Modifier = Modifier,
    wideScreen: Boolean = false
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
        AndroidView(modifier = remoteModifier.align(Alignment.Center), factory = {
            remoteView
        })

        val localTarget = if (showLocal) {
            AndroidView(modifier = modifierLocalView
                .align(Alignment.BottomEnd)
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                },
                factory = { localView })

            localView
        } else {
            null
        }

        setLocalTarget(localTarget)
    }
}

@Composable
fun AudioOnly(
    participants: Int,
    modifier: Modifier = Modifier
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
                "$participants", style = MaterialTheme.typography.h1.copy(
                    color = Color.White
                ),

                modifier = Modifier
                    .padding(start = 4.dp)
            )
        }
        Text(
            stringResource(id = R.string.audio_only), style = MaterialTheme.typography.body1.copy(
                color = Color.White
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            stringResource(id = R.string.no_video_is_sent_or_received),
            style = MaterialTheme.typography.caption.copy(
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
    modifier: Modifier = Modifier
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
            stringResource(id = R.string.connecting), style = MaterialTheme.typography.body1.copy(
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

private fun generateInCallNotification(context: Context): Notification {
    val pendingIntent =
        PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.getSystemService(NotificationManager::class.java).apply {
            createNotificationChannel(
                NotificationChannel(
                    IN_CALL_CHANNEL_ID,
                    IN_CALL_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
    }

    return NotificationCompat.Builder(context, IN_CALL_CHANNEL_ID)
        .setOngoing(true)
        .setSilent(true)
        .setContentText(context.getText(R.string.click_to_resume))
        .setContentTitle(context.getText(R.string.active_call))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setSmallIcon(R.drawable.video_call_24)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()
}


@Preview
@Composable
fun SettingsScreenPreview() {

    EyesonDemoTheme {
        MeetingScreen({/*NOOP*/ })
    }
}


private const val SCREEN_SHARE_NOTIFICATION_ID = 42
private const val SCREEN_SHARE_CHANNEL_ID = "7"
private const val SCREEN_SHARE_CHANNEL_NAME = "Screen share active"
private const val IN_CALL_NOTIFICATION_ID = 3
private const val IN_CALL_CHANNEL_ID = "17"
private const val IN_CALL_CHANNEL_NAME = "In call"
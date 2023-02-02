package com.eyeson.android.ui.meeting

import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ListItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyeson.android.R
import com.eyeson.android.ui.components.OverlayMenu
import com.eyeson.android.ui.theme.EyesonDemoTheme
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import timber.log.Timber


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MeetingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MeetingViewModel = hiltViewModel()
) {

    var open by rememberSaveable { mutableStateOf(false) }
    var option1 by rememberSaveable { mutableStateOf(false) }
    var audioSettingsOpen by rememberSaveable { mutableStateOf(false) }
    var screenShareActive by rememberSaveable { mutableStateOf(false) }

    val sfu by viewModel.p2p.collectAsStateWithLifecycle()

    var whatIsOpen by rememberSaveable { mutableStateOf(0) }

    val configuration: Configuration = LocalConfiguration.current

    val remoteView = rememberSurfaceViewRendererWithLifecycle(viewModel.getEglContext()) {
        viewModel.setRemoteVideoTarget(it)
    }

    val localView = rememberSurfaceViewRendererWithLifecycle(viewModel.getEglContext()) {
        viewModel.setLocalVideoTarget(it)
    }
    val scope = rememberCoroutineScope()


    Scaffold(
        modifier = modifier, topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.surface,
                elevation = 0.dp,
                title = {
                    Text("")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                }
            )
        }

    ) { padding ->
        val remoteModifier = if (sfu || configuration.isLandscape()) {
            Modifier
                .fillMaxSize()
                .aspectRatio(3f / 4f)
            Modifier.aspectRatio(3f / 4f)
            Modifier
        } else {
            Modifier.aspectRatio(4f / 3f)
        }

        Box(
            modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(modifier = remoteModifier.align(Alignment.Center), factory = {
                Timber.d("AndroidView FAC remote")
                remoteView
            }) {
                Timber.d("AndroidView UPDATE remote")
            }
            AndroidView(modifier = Modifier.size(120.dp, 200.dp), factory = { localView }) { }

        }
    }

    LaunchedEffect(key1 = Unit) {
        Timber.d("LaunchedEffect!!! in call ${viewModel.inCall()}")
        if (!viewModel.inCall()) {
            viewModel.connect(localView, remoteView)
        }
    }

    Timber.d("open = $open")


    Timber.d("configuration.orientation = ${configuration.orientation}")
    val (vertical, horizontal) = if (configuration.isLandscape()) {
        Pair(1f, 0.7f)
    } else {
        Pair(0.7f, 1f)
    }

    val audioDevices = listOf(
        AudioDevice(stringResource(id = R.string.bluetooth_device), false, {}),
        AudioDevice(stringResource(id = R.string.wired_headset), false, {}),
        AudioDevice(stringResource(id = R.string.ear_piece), false, {}),
        AudioDevice(stringResource(id = R.string.speaker_phone), false, {})
    )

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
                    listOf(
                        Event("1"),
                        Event("2"),
                        Event("3333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333312sdfsdfsadfasfdasfdsafsafasdfasdfasf"),
                        Event("4", true),
                        Event("5"), Event("1"),
                        Event("2"),
                        Event("3333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333312sdfsdfsadfasfdasfdsafsafasdfasdfasf"),
                        Event("4", true),
                        Event("5"), Event("1"),
                        Event("2"),
                        Event("3333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333312sdfsdfsadfasfdasfdsafsafasdfasdfasf"),
                        Event("4", true),
                        Event("5"), Event("1"),
                        Event("2"),
                        Event("3333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333312sdfsdfsadfasfdasfdsafsafasdfasdfasf"),
                        Event("4", true),
                        Event("5"), Event("1"),
                        Event("2"),
                        Event("3333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333312sdfsdfsadfasfdasfdsafsafasdfasdfasf"),
                        Event("4", true),
                        Event("5"), Event("1"),
                        Event("2")
                    ),
                    onClear = {},
                    onCopy = {},
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
                    onScreenShareActiveChange = { screenShareActive = screenShareActive.not() },
                    startFullScreenPresentation = {},
                    muteAll = {},
                    showAudioSettings = {
                        whatIsOpen = 1
//                    open = false
                    },
                    showEventLog = { whatIsOpen = 2 },
                    horizontalContentRatio = horizontal,
                    verticalContentRatio = vertical
                )
            }

        }
    }
}


@Composable
fun rememberSurfaceViewRendererWithLifecycle(
    eglContext: EglBase.Context?,
    setTarget: (SurfaceViewRenderer) -> Unit
): SurfaceViewRenderer {
    val currentSetTarget by rememberUpdatedState(setTarget)

    val context = LocalContext.current
    val surfaceViewRenderer = remember {
        SurfaceViewRenderer(context).apply {
//            init(eglContext, null)
            init(eglContext, object : RendererCommon.RendererEvents {
                override fun onFirstFrameRendered() {
                }

                override fun onFrameResolutionChanged(
                    videoWidth: Int,
                    videoHeight: Int,
                    rotation: Int
                ) {
                    Timber.d("onFrameResolutionChanged: videoWidth->$videoWidth videoHeight->$videoHeight rotation->$rotation")
                }

            })
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            setEnableHardwareScaler(true)
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(key1 = lifecycle, key2 = surfaceViewRenderer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_CREATE) {
                currentSetTarget(surfaceViewRenderer)
                Timber.d("DecodeCallback: Lifecycle.Event.ON_RESUME")
            } else if (event == Lifecycle.Event.ON_DESTROY) {
                surfaceViewRenderer.release()
                Timber.d("DecodeCallback: Lifecycle.Event.ON_PAUSE")
            }
        }

        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
    return surfaceViewRenderer
}

fun Configuration.isLandscape() = orientation == Configuration.ORIENTATION_LANDSCAPE


@Preview
@Composable
fun SettingsScreenPreview() {

    EyesonDemoTheme {
        MeetingScreen({/*NOOP*/ })
    }
}

package com.eyeson.android.ui.meeting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eyeson.android.R
import com.eyeson.android.ui.theme.DarkGray800
import com.eyeson.android.ui.theme.EyesonDemoTheme
import com.eyeson.android.ui.theme.WhiteRippleTheme

@Composable
fun HorizontalMeetingControls(
    audioOnly: Boolean,
    cameraChangeable: Boolean,
    onSwitchCamera: () -> Unit,
    videoMuted: Boolean,
    onMuteVideo: () -> Unit,
    microphoneMuted: Boolean,
    onMuteMicrophone: () -> Unit,
    modifier: Modifier = Modifier,
    onShowChat: () -> Unit,
    iconTint: Color = MaterialTheme.colors.onSurface
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
            CompositionLocalProvider(LocalContentColor provides iconTint) {
                IconButton(onClick = onSwitchCamera, enabled = !audioOnly && cameraChangeable) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_cameraswitch_24),
                        stringResource(id = R.string.switch_camera),
                    )
                }

                val (descriptionCam, iconCam) = if (audioOnly || !videoMuted) {
                    Pair(R.string.unmute_camera, R.drawable.baseline_videocam_off_24)
                } else {
                    Pair(R.string.mute_camera, R.drawable.baseline_videocam_24)
                }

                IconButton(onClick = onMuteVideo, enabled = !audioOnly && cameraChangeable) {
                    Icon(
                        painter = painterResource(id = iconCam),
                        stringResource(id = descriptionCam),
                    )
                }

                val (descriptionMic, iconMic) = if (microphoneMuted) {
                    Pair(R.string.unmute_microphone, R.drawable.baseline_mic_off_24)
                } else {
                    Pair(R.string.mute_microphone, R.drawable.baseline_mic_24)
                }

                IconButton(onClick = onMuteMicrophone) {
                    Icon(
                        painter = painterResource(id = iconMic),
                        stringResource(id = descriptionMic),
                    )
                }

                IconButton(onClick = onShowChat) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_chat_24),
                        stringResource(id = R.string.show_chat),
                    )
                }
            }
        }
    }
}

@Composable
fun VerticalMeetingControls(
    audioOnly: Boolean,
    cameraChangeable: Boolean,
    onSwitchCamera: () -> Unit,
    videoMuted: Boolean,
    onMuteVideo: () -> Unit,
    microphoneMuted: Boolean,
    onMuteMicrophone: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.White
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        CompositionLocalProvider(LocalContentColor provides iconTint) {
            CompositionLocalProvider(LocalRippleTheme provides WhiteRippleTheme) {
                IconButton(onClick = onSwitchCamera, enabled = !audioOnly && cameraChangeable) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_cameraswitch_24),
                        stringResource(id = R.string.switch_camera),
                    )
                }

                val (descriptionCam, iconCam) = if (audioOnly || !videoMuted) {
                    Pair(R.string.unmute_camera, R.drawable.baseline_videocam_off_24)
                } else {
                    Pair(R.string.mute_camera, R.drawable.baseline_videocam_24)
                }

                IconButton(onClick = onMuteVideo, enabled = !audioOnly && cameraChangeable) {
                    Icon(
                        painter = painterResource(id = iconCam),
                        stringResource(id = descriptionCam),
                    )
                }

                val (descriptionMic, iconMic) = if (microphoneMuted) {
                    Pair(R.string.unmute_microphone, R.drawable.baseline_mic_off_24)
                } else {
                    Pair(R.string.mute_microphone, R.drawable.baseline_mic_24)
                }

                IconButton(onClick = onMuteMicrophone) {
                    Icon(
                        painter = painterResource(id = iconMic),
                        stringResource(id = descriptionMic),
                    )
                }
            }
        }
    }
}


@Preview
@Composable
fun HorizontalMeetingControlsPreview() {

    var videoMuted by remember { mutableStateOf(false) }
    var microphoneMuted by remember { mutableStateOf(false) }

    EyesonDemoTheme {
        HorizontalMeetingControls(
            audioOnly = false,
            cameraChangeable = true,
            onSwitchCamera = { /*NOOP*/ },
            videoMuted = videoMuted,
            onMuteVideo = { videoMuted = !videoMuted },
            microphoneMuted = microphoneMuted,
            onMuteMicrophone = { microphoneMuted = !microphoneMuted },
            onShowChat = { /*NOOP*/ }
        )
    }
}

@Preview(name = "landscape", device = "spec:shape=Normal,width=640,height=360,unit=dp,dpi=480")
@Composable
fun VerticalMeetingControlsPreview() {

    var videoMuted by remember { mutableStateOf(false) }
    var microphoneMuted by remember { mutableStateOf(false) }

    EyesonDemoTheme {

        Box(
            modifier = Modifier
                .background(color = DarkGray800)
                .padding(16.dp)
        ) {
            VerticalMeetingControls(
                audioOnly = false,
                cameraChangeable = true,
                onSwitchCamera = { /*NOOP*/ },
                videoMuted = videoMuted,
                onMuteVideo = { videoMuted = !videoMuted },
                microphoneMuted = microphoneMuted,
                onMuteMicrophone = { microphoneMuted = !microphoneMuted }
            )
        }


    }
}



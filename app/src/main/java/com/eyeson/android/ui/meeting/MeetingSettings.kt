package com.eyeson.android.ui.meeting

import androidx.annotation.FloatRange
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eyeson.android.R
import com.eyeson.android.ui.components.EyesonDemoTextField
import com.eyeson.android.ui.components.OverlayMenu
import com.eyeson.android.ui.components.SettingsRadioButton
import com.eyeson.android.ui.components.SettingsTextButton
import com.eyeson.android.ui.components.SettingsToggle
import timber.log.Timber


@Composable
fun MeetingSettings(
    visible: Boolean,
    onClose: () -> Unit,
    screenShareActive: Boolean,
    presentationActive: Boolean,
    onScreenShareActiveChange: (Boolean) -> Unit,
    startFullScreenPresentation: () -> Unit,
    stopFullScreenPresentation: () -> Unit,
    showVideoPlayback: () -> Unit,
    isVideoPlaying: Boolean,
    stopVideoPlayback: () -> Unit,
    muteAll: () -> Unit,
    showAudioSettings: () -> Unit,
    showEventLog: () -> Unit,
    modifier: Modifier = Modifier,
    @FloatRange(from = 0.0, to = 1.0) horizontalContentRatio: Float = 1.0f,
    @FloatRange(from = 0.0, to = 1.0) verticalContentRatio: Float = 1.0f,
) {
    OverlayMenu(
        visible = visible,
        onClose = onClose,
        showDivider = true,
        horizontalContentRatio = horizontalContentRatio,
        modifier = modifier
    ) {
        BoxWithConstraints {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeightIn(max = (maxHeight.value * verticalContentRatio).dp)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp)
            ) {
                SettingsToggle(
                    value = screenShareActive,
                    onValueChange = onScreenShareActiveChange,
                    title = stringResource(id = R.string.share_screen),
                    enabled = !presentationActive
                )
                if (presentationActive) {
                    SettingsTextButton(
                        title = stringResource(id = R.string.full_screen_presentation),
                        buttonText = stringResource(id = R.string.stop),
                        onClick = stopFullScreenPresentation
                    )
                } else {
                    SettingsTextButton(
                        title = stringResource(id = R.string.full_screen_presentation),
                        buttonText = stringResource(id = R.string.start),
                        onClick = startFullScreenPresentation
                    )
                }
                SettingsTextButton(
                    title = stringResource(id = R.string.video_playback),
                    buttonText = if (isVideoPlaying) {
                        stringResource(id = R.string.stop)
                    } else {
                        stringResource(id = R.string.select)
                    },
                    onClick = if (isVideoPlaying) {
                        stopVideoPlayback
                    } else {
                        showVideoPlayback
                    }
                )

                SettingsTextButton(
                    title = stringResource(id = R.string.mute_all_participants),
                    buttonText = stringResource(id = R.string.mute),
                    onClick = muteAll
                )
                SettingsTextButton(
                    title = stringResource(id = R.string.audio_settings),
                    buttonText = stringResource(id = R.string.change),
                    onClick = showAudioSettings
                )
                SettingsTextButton(
                    title = stringResource(id = R.string.show_event_log),
                    buttonText = stringResource(id = R.string.show),
                    onClick = showEventLog,
                    showDivider = false
                )
            }
        }
    }
}

@Composable
fun AudioSettings(
    visible: Boolean,
    onClose: () -> Unit,
    audioDevices: List<AudioDevice>,
    modifier: Modifier = Modifier,
    @FloatRange(from = 0.0, to = 1.0) horizontalContentRatio: Float = 1.0f,
    @FloatRange(from = 0.0, to = 1.0) verticalContentRatio: Float = 1.0f,
) {

    Timber.d("audioDevices $audioDevices")
    OverlayMenu(
        visible = visible,
        title = stringResource(id = R.string.audio_settings).uppercase(),
        onClose = onClose,
        showDivider = true,
        horizontalContentRatio = horizontalContentRatio,
        contentShape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        modifier = modifier
    ) {
        BoxWithConstraints {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeightIn(max = (maxHeight.value * verticalContentRatio).dp)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp)
            ) {
                audioDevices.forEach { audioDevice ->
                    SettingsRadioButton(
                        title = audioDevice.title,
                        selected = audioDevice.selected,
                        onClick = audioDevice.onClick,
                        enabled = audioDevice.enabled
                    )
                }
            }
        }
    }
}

@Composable
fun EventLog(
    visible: Boolean,
    onClose: () -> Unit,
    events: List<EventEntry>,
    onClear: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    @FloatRange(from = 0.0, to = 1.0) horizontalContentRatio: Float = 1.0f,
    @FloatRange(from = 0.0, to = 1.0) verticalContentRatio: Float = 1.0f,
) {
    OverlayMenu(
        visible = visible,
        title = stringResource(id = R.string.event_log).uppercase(),
        onClose = onClose,
        showDivider = true,
        horizontalContentRatio = horizontalContentRatio,
        contentShape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        modifier = modifier
    ) {
        BoxWithConstraints(Modifier.padding(bottom = 16.dp)) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeightIn(max = (maxHeight.value * verticalContentRatio).dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                        .horizontalScroll(scrollState),
                    reverseLayout = true
                ) {
                    items(events) { event ->
                        val color = if (event.error) {
                            MaterialTheme.colors.error
                        } else {
                            Color.Unspecified
                        }
                        Text(event.event, color = color)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Divider()
                Row(modifier = Modifier.padding(top = 16.dp)) {
                    OutlinedButton(
                        onClick = onClear,
                        contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colors.error),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colors.error
                        ),
                        modifier = Modifier
                            .padding(start = 16.dp, end = 8.dp)
                            .weight(1f)
                    ) {
                        Text(
                            text = stringResource(id = R.string.clear).uppercase()
                        )
                    }
                    Button(
                        onClick = onCopy,
                        contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                        modifier = Modifier
                            .padding(start = 8.dp, end = 16.dp)
                            .weight(1f)
                    ) {
                        Text(
                            text = stringResource(id = R.string.copy).uppercase()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPlayback(
    visible: Boolean,
    onClose: () -> Unit,
    videoUrl: String,
    onVideoUrlChange: (String) -> Unit,
    audio: Boolean,
    onAudioToggle: (Boolean) -> Unit,
    replaceOwnVideo: Boolean,
    onReplaceOwnVideoChange: (Boolean) -> Unit,
    onPlay: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    @FloatRange(from = 0.0, to = 1.0) horizontalContentRatio: Float = 1.0f,
    @FloatRange(from = 0.0, to = 1.0) verticalContentRatio: Float = 1.0f,
) {

    OverlayMenu(
        visible = visible,
        title = stringResource(id = R.string.video_playback).uppercase(),
        onClose = onClose,
        showDivider = true,
        horizontalContentRatio = horizontalContentRatio,
        contentShape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        modifier = modifier
    ) {
        BoxWithConstraints {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeightIn(max = (maxHeight.value * verticalContentRatio).dp)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp)
            ) {
                EyesonDemoTextField(
                    onValueChange = { onVideoUrlChange(it) },
                    label = stringResource(id = R.string.video_url_hint).uppercase(),
                    value = videoUrl,
                    modifier = Modifier
                        .padding(top = 16.dp, end = 16.dp)
                )
                SettingsToggle(
                    value = audio,
                    onValueChange = onAudioToggle,
                    title = stringResource(id = R.string.play_audio),
                    showDivider = false
                )
                SettingsRadioButton(
                    title = stringResource(id = R.string.instead_of_own_video),
                    selected = replaceOwnVideo,
                    onClick = { onReplaceOwnVideoChange(true) },
                    showDivider = false,
                )
                SettingsRadioButton(
                    title = stringResource(id = R.string.as_additional_user),
                    selected = !replaceOwnVideo,
                    onClick = { onReplaceOwnVideoChange(false) },
                    showDivider = false,
                )
                Button(
                    onClick = {
                        onPlay(replaceOwnVideo)
                    },
                    contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                    enabled = videoUrl.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp)
                        .padding(end = 16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.play).uppercase()
                    )
                }
            }
        }
    }
}
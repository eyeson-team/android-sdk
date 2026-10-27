package com.eyeson.android.ui.meeting

import androidx.annotation.FloatRange
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eyeson.android.R
import com.eyeson.android.ui.components.EyesonDemoTextField
import com.eyeson.android.ui.components.OverlayMenu
import com.eyeson.android.ui.components.SettingsRadioButton
import com.eyeson.android.ui.components.SettingsSlider
import com.eyeson.android.ui.components.SettingsTextButton
import com.eyeson.android.ui.components.SettingsToggle
import com.eyeson.android.ui.theme.EyesonDemoTheme
import com.eyeson.sdk.options.Resolution16by9
import com.eyeson.sdk.options.ScreenShareMaxResolution


@Composable
fun MeetingSettings(
    visible: Boolean,
    onClose: () -> Unit,
    screenShareActive: Boolean,
    presentationActive: Boolean,
    onScreenShareActiveChange: () -> Unit,
    startFullScreenPresentation: () -> Unit,
    stopFullScreenPresentation: () -> Unit,
    showVideoPlayback: () -> Unit,
    isVideoPlaying: Boolean,
    stopVideoPlayback: () -> Unit,
    muteAll: () -> Unit,
    showAudioSettings: () -> Unit,
    showVideoSettings: () -> Unit,
    showScreenShareSettings: () -> Unit,
    showEventLog: () -> Unit,
    modifier: Modifier = Modifier,
    @FloatRange(from = 0.0, to = 1.0) horizontalContentRatio: Float = 1.0f,
    @FloatRange(from = 0.0, to = 1.0) verticalContentRatio: Float = 1.0f,
    contentShape: Shape = MaterialTheme.shapes.large,
) {
    OverlayMenu(
        visible = visible,
        onClose = onClose,
        showDivider = true,
        contentShape = contentShape,
        horizontalContentRatio = horizontalContentRatio,
        insertEdgeToEdgePadding = true,
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
                    onValueChange = { onScreenShareActiveChange() },
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

                if (screenShareActive) {
                    SettingsTextButton(
                        title = stringResource(id = R.string.screen_share_settings),
                        buttonText = stringResource(id = R.string.change),
                        onClick = showScreenShareSettings
                    )
                } else {
                    SettingsTextButton(
                        title = stringResource(id = R.string.video_settings),
                        buttonText = stringResource(id = R.string.change),
                        onClick = showVideoSettings
                    )
                }
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
    contentShape: Shape = MaterialTheme.shapes.large,
) {
    OverlayMenu(
        visible = visible,
        title = stringResource(id = R.string.audio_settings).uppercase(),
        onClose = onClose,
        showDivider = true,
        horizontalContentRatio = horizontalContentRatio,
        contentShape = contentShape,
        insertEdgeToEdgePadding = true,
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
    contentShape: Shape = MaterialTheme.shapes.large,
) {
    OverlayMenu(
        visible = visible,
        title = stringResource(id = R.string.event_log).uppercase(),
        onClose = onClose,
        showDivider = true,
        horizontalContentRatio = horizontalContentRatio,
        contentShape = contentShape,
        insertEdgeToEdgePadding = true,
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
                            MaterialTheme.colorScheme.error
                        } else {
                            Color.Unspecified
                        }
                        Text(event.event, color = color)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
                Row(modifier = Modifier.padding(top = 16.dp)) {
                    OutlinedButton(
                        onClick = onClear,
                        contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
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
    contentShape: Shape = MaterialTheme.shapes.large,
) {

    OverlayMenu(
        visible = visible,
        title = stringResource(id = R.string.video_playback).uppercase(),
        onClose = onClose,
        showDivider = true,
        horizontalContentRatio = horizontalContentRatio,
        contentShape = contentShape,
        insertEdgeToEdgePadding = true,
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
                    clearable = true,
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

@Composable
fun VideoSettings(
    visible: Boolean,
    onClose: () -> Unit,
    resolutions: SettingsRange<Resolution>,
    scaleDownBy: SettingsRange<Double>,
    fps: SettingsRange<Int>,
    maxBitrate: SettingsRange<Int>,
    apply: () -> Unit,
    reset: () -> Unit,
    modifier: Modifier = Modifier,
    @FloatRange(from = 0.0, to = 1.0) horizontalContentRatio: Float = 1.0f,
    @FloatRange(from = 0.0, to = 1.0) verticalContentRatio: Float = 1.0f,
    contentShape: Shape = MaterialTheme.shapes.large,
) {
    OverlayMenu(
        visible = visible,
        title = stringResource(id = R.string.video_settings).uppercase(),
        onClose = onClose,
        showDivider = true,
        horizontalContentRatio = horizontalContentRatio,
        contentShape = contentShape,
        insertEdgeToEdgePadding = true,
        modifier = modifier
    ) {
        BoxWithConstraints {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeightIn(max = (maxHeight.value * verticalContentRatio).dp)
                    .padding(start = 16.dp)
            ) {
                // fill = false keeps the sheet as small as its content and only scrolls once the
                // content no longer fits, so the buttons below always stay on screen.
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringResource(id = R.string.resolution),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    resolutions.values.forEachIndexed { index, resolution ->
                        SettingsRadioButton(
                            title = resolution.label,
                            selected = index == resolutions.selectedIndex,
                            onClick = { resolutions.onIndexChange(index) },
                            showDivider = false
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(top = 4.dp))

                    val selectedResolution = resolutions.selected.resolution
                    val selectedScaleDownBy = scaleDownBy.selected
                    SettingsSlider(
                        title = stringResource(id = R.string.scale_down),
                        // The factor divides the resolution on top of the one selected above, so
                        // show what actually ends up being encoded.
                        valueLabel = "$selectedScaleDownBy " +
                                "(${(selectedResolution.width / selectedScaleDownBy).toInt()}" +
                                "x${(selectedResolution.height / selectedScaleDownBy).toInt()})",
                        index = scaleDownBy.selectedIndex,
                        valueCount = scaleDownBy.values.size,
                        onIndexChange = scaleDownBy.onIndexChange
                    )
                    SettingsSlider(
                        title = stringResource(id = R.string.fps),
                        valueLabel = fps.selected.toString(),
                        index = fps.selectedIndex,
                        valueCount = fps.values.size,
                        onIndexChange = fps.onIndexChange
                    )
                    SettingsSlider(
                        title = stringResource(id = R.string.max_bitrate),
                        valueLabel = maxBitrate.selected.toString(),
                        index = maxBitrate.selectedIndex,
                        valueCount = maxBitrate.values.size,
                        onIndexChange = maxBitrate.onIndexChange,
                        showDivider = false
                    )
                }
                Row(modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)) {
                    OutlinedButton(
                        onClick = reset,
                        contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .weight(1f)
                    ) {
                        Text(text = stringResource(id = R.string.reset).uppercase())
                    }
                    Button(
                        onClick = apply,
                        contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                        modifier = Modifier
                            .padding(start = 8.dp, end = 16.dp)
                            .weight(1f)
                    ) {
                        Text(text = stringResource(id = R.string.apply).uppercase())
                    }
                }
            }
        }
    }
}

/**
 * The screen share counterpart to [VideoSettings].
 */
@Composable
fun ScreenShareSettings(
    visible: Boolean,
    onClose: () -> Unit,
    resolutions: SettingsRange<Resolution>,
    scaleDownBy: SettingsRange<Double>,
    fps: SettingsRange<Int>,
    maxBitrate: SettingsRange<Int>,
    apply: () -> Unit,
    reset: () -> Unit,
    modifier: Modifier = Modifier,
    @FloatRange(from = 0.0, to = 1.0) horizontalContentRatio: Float = 1.0f,
    @FloatRange(from = 0.0, to = 1.0) verticalContentRatio: Float = 1.0f,
    contentShape: Shape = MaterialTheme.shapes.large,
) {
    OverlayMenu(
        visible = visible,
        title = stringResource(id = R.string.screen_share_settings).uppercase(),
        onClose = onClose,
        showDivider = true,
        horizontalContentRatio = horizontalContentRatio,
        contentShape = contentShape,
        insertEdgeToEdgePadding = true,
        modifier = modifier
    ) {
        BoxWithConstraints {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeightIn(max = (maxHeight.value * verticalContentRatio).dp)
                    .padding(start = 16.dp)
            ) {
                // fill = false keeps the sheet as small as its content and only scrolls once the
                // content no longer fits, so the buttons below always stay on screen.
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringResource(id = R.string.resolution),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    resolutions.values.forEachIndexed { index, resolution ->
                        SettingsRadioButton(
                            title = resolution.label,
                            selected = index == resolutions.selectedIndex,
                            onClick = { resolutions.onIndexChange(index) },
                            showDivider = false
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(top = 4.dp))

                    val selectedResolution = resolutions.selected.resolution
                    val selectedScaleDownBy = scaleDownBy.selected
                    SettingsSlider(
                        title = stringResource(id = R.string.scale_down),
                        // The factor divides the resolution on top of the one selected above, so
                        // show what actually ends up being encoded.
                        valueLabel = "$selectedScaleDownBy " +
                                "(${(selectedResolution.width / selectedScaleDownBy).toInt()}" +
                                "x${(selectedResolution.height / selectedScaleDownBy).toInt()})",
                        index = scaleDownBy.selectedIndex,
                        valueCount = scaleDownBy.values.size,
                        onIndexChange = scaleDownBy.onIndexChange
                    )
                    SettingsSlider(
                        title = stringResource(id = R.string.fps),
                        valueLabel = fps.selected.toString(),
                        index = fps.selectedIndex,
                        valueCount = fps.values.size,
                        onIndexChange = fps.onIndexChange
                    )
                    SettingsSlider(
                        title = stringResource(id = R.string.max_bitrate),
                        valueLabel = maxBitrate.selected.toString(),
                        index = maxBitrate.selectedIndex,
                        valueCount = maxBitrate.values.size,
                        onIndexChange = maxBitrate.onIndexChange,
                        showDivider = false
                    )
                }
                Row(modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)) {
                    OutlinedButton(
                        onClick = reset,
                        contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .weight(1f)
                    ) {
                        Text(text = stringResource(id = R.string.reset).uppercase())
                    }
                    Button(
                        onClick = apply,
                        contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                        modifier = Modifier
                            .padding(start = 8.dp, end = 16.dp)
                            .weight(1f)
                    ) {
                        Text(text = stringResource(id = R.string.apply).uppercase())
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun VideoSettingsPreview() {
    var resolutionIndex by remember { mutableIntStateOf(2) }
    var scaleDownByIndex by remember { mutableIntStateOf(0) }
    var fpsIndex by remember { mutableIntStateOf(5) }
    var maxBitrateIndex by remember { mutableIntStateOf(5) }

    EyesonDemoTheme {
        VideoSettings(
            visible = true,
            onClose = {/*NOOP*/ },
            resolutions = SettingsRange(
                listOf(
                    Resolution16by9.LOWEST,
                    Resolution16by9.LOWER,
                    Resolution16by9.DEFAULT,
                    Resolution16by9.HIGH,
                    Resolution16by9.HIGHEST,
                ).map { Resolution(it, "${it.width}x${it.height}") },
                resolutionIndex
            ) { resolutionIndex = it },
            scaleDownBy = SettingsRange(
                listOf(1.0, 1.5, 2.0, 3.0, 4.0, 8.0, 16.0),
                scaleDownByIndex
            ) { scaleDownByIndex = it },
            fps = SettingsRange(
                listOf(5, 10, 15, 20, 25, 30),
                fpsIndex
            ) { fpsIndex = it },
            maxBitrate = SettingsRange(
                listOf(100, 250, 500, 1000, 1500, 2000),
                maxBitrateIndex
            ) { maxBitrateIndex = it },
            apply = {/*NOOP*/ },
            reset = {/*NOOP*/ }
        )
    }
}

@Preview
@Composable
fun ScreenShareSettingsPreview() {
    var resolutionIndex by remember { mutableIntStateOf(5) }
    var scaleDownByIndex by remember { mutableIntStateOf(0) }
    var fpsIndex by remember { mutableIntStateOf(3) }
    var maxBitrateIndex by remember { mutableIntStateOf(5) }

    EyesonDemoTheme {
        ScreenShareSettings(
            visible = true,
            onClose = {/*NOOP*/ },
            resolutions = SettingsRange(
                listOf(
                    Resolution16by9.LOWEST,
                    Resolution16by9.LOWER,
                    Resolution16by9.DEFAULT,
                    Resolution16by9.HIGH,
                    Resolution16by9.HIGHEST,
                    ScreenShareMaxResolution,
                ).map { Resolution(it, "${it.width}x${it.height}") },
                resolutionIndex
            ) { resolutionIndex = it },
            scaleDownBy = SettingsRange(
                listOf(1.0, 1.5, 2.0, 3.0, 4.0, 8.0, 16.0),
                scaleDownByIndex
            ) { scaleDownByIndex = it },
            fps = SettingsRange(
                listOf(0, 5, 10, 15, 20, 25, 30),
                fpsIndex
            ) { fpsIndex = it },
            maxBitrate = SettingsRange(
                listOf(100, 250, 500, 1000, 1500, 2000),
                maxBitrateIndex
            ) { maxBitrateIndex = it },
            apply = {/*NOOP*/ },
            reset = {/*NOOP*/ }
        )
    }
}

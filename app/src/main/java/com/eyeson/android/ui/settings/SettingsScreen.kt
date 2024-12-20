package com.eyeson.android.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eyeson.android.R
import com.eyeson.android.data.SettingsRepository
import com.eyeson.android.ui.components.SettingsToggle
import com.eyeson.android.ui.settings.SettingsUiState.Loading
import com.eyeson.android.ui.settings.SettingsUiState.Success
import com.eyeson.android.ui.theme.EyesonDemoTheme


@Composable
fun SettingsRout(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {

    val settingsUiState by viewModel.settingsUiState.collectAsState()

    SettingsScreen(
        onBack = onBack,
        settingsUiState = settingsUiState,
        micOnStartChange = viewModel::setMicOnStart,
        audioOnlyChange = viewModel::setAudioOnly,
        videoOnStartChange = viewModel::setVideoOnStart,
        rearCamOnStartChange = viewModel::setRearCamOnStart,
        screenShareOnStartChange = viewModel::setScreenShareOnStart
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    onBack: () -> Unit,
    settingsUiState: SettingsUiState,
    micOnStartChange: (Boolean) -> Unit = {},
    audioOnlyChange: (Boolean) -> Unit = {},
    videoOnStartChange: (Boolean) -> Unit = {},
    rearCamOnStartChange: (Boolean) -> Unit = {},
    screenShareOnStartChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {

    Scaffold(
        modifier = modifier.safeDrawingPadding(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { onBack() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(id = R.string.label_go_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                title = {
                    Text(
                        stringResource(id = R.string.settings),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 68.dp)
                    )
                }
            )
        }
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(start = 20.dp)
        ) {
            when (settingsUiState) {
                Loading -> {
                    Text(text = stringResource(R.string.loading))
                }

                is Success -> {
                    val settings = settingsUiState.settings

                    SettingsToggle(
                        value = settings.micOnStar,
                        onValueChange = {
                            micOnStartChange(it)
                        },
                        title = stringResource(id = R.string.microphone_on_start),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    SettingsToggle(
                        value = settings.audioOnly,
                        onValueChange = {
                            audioOnlyChange(it)
                        },
                        title = stringResource(id = R.string.audio_only),
                        description = stringResource(id = R.string.enable_for_low_data_connection)
                    )
                    SettingsToggle(
                        value = settings.videoOnStart,
                        onValueChange = {
                            videoOnStartChange(it)
                        },
                        title = stringResource(id = R.string.enable_video_on_start),
                        enabled = !settings.audioOnly
                    )
                    SettingsToggle(
                        value = settings.screenShareOnStart,
                        onValueChange = {
                            screenShareOnStartChange(it)
                        },
                        title = stringResource(id = R.string.enable_screen_share_on_start),
                        enabled = !settings.audioOnly
                    )
                    SettingsToggle(
                        value = settings.rearCamOnStart,
                        onValueChange = {
                            rearCamOnStartChange(it)
                        },
                        title = stringResource(id = R.string.enable_rear_camera_on_start),
                        enabled = !settings.audioOnly
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {

    var settingsUiState by remember {
        mutableStateOf(
            Success(
                SettingsRepository.MeetingSettings(
                    micOnStar = true,
                    audioOnly = false,
                    videoOnStart = true,
                    rearCamOnStart = false,
                    screenShareOnStart = false
                )
            )
        )
    }

    val updateSettings = { updatedSettings: SettingsRepository.MeetingSettings ->
        settingsUiState = Success(updatedSettings)
    }

    EyesonDemoTheme {
        SettingsScreen(
            onBack = {/*NOOP*/ },
            settingsUiState = settingsUiState,
            micOnStartChange = { updateSettings(settingsUiState.settings.copy(micOnStar = it)) },
            audioOnlyChange = { updateSettings(settingsUiState.settings.copy(audioOnly = it)) },
            videoOnStartChange = { updateSettings(settingsUiState.settings.copy(videoOnStart = it)) },
            rearCamOnStartChange = { updateSettings(settingsUiState.settings.copy(rearCamOnStart = it)) },
            screenShareOnStartChange = {
                updateSettings(
                    settingsUiState.settings.copy(
                        screenShareOnStart = it
                    )
                )
            }
        )
    }
}


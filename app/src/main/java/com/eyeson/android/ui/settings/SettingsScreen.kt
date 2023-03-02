package com.eyeson.android.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eyeson.android.R
import com.eyeson.android.ui.components.SettingsToggle
import com.eyeson.android.ui.settings.SettingsUiState.Loading
import com.eyeson.android.ui.settings.SettingsUiState.Success
import com.eyeson.android.ui.theme.EyesonDemoTheme


@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settingsUiState by viewModel.settingsUiState.collectAsState()

    Scaffold(
        modifier = modifier, topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.surface,
                elevation = 0.dp,
                navigationIcon = {
                    IconButton(onClick = { onBack() }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            stringResource(id = R.string.label_go_back),
                            tint = MaterialTheme.colors.onSurface
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
                    val settings = (settingsUiState as Success).settings

                    SettingsToggle(
                        value = settings.micOnStar,
                        onValueChange = {
                            viewModel.setMicOnStart(it)
                        },
                        title = stringResource(id = R.string.microphone_on_start),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    SettingsToggle(
                        value = settings.audioOnly,
                        onValueChange = {
                            viewModel.setAudioOnly(it)
                        },
                        title = stringResource(id = R.string.audio_only),
                        description = stringResource(id = R.string.enable_for_low_data_connection)
                    )
                    SettingsToggle(
                        value = settings.videoOnStart,
                        onValueChange = {
                            viewModel.setVideoOnStart(it)
                        },
                        title = stringResource(id = R.string.enable_video_on_start),
                        enabled = !settings.audioOnly
                    )
                    SettingsToggle(
                        value = settings.screenShareOnStart,
                        onValueChange = {
                            viewModel.setScreenShareOnStart(it)
                        },
                        title = stringResource(id = R.string.enable_screen_share_on_start),
                        enabled = !settings.audioOnly
                    )
                    SettingsToggle(
                        value = settings.rearCamOnStart,
                        onValueChange = {
                            viewModel.setRearCamOnStart(it)
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

    EyesonDemoTheme {
        SettingsScreen({/*NOOP*/ })

    }
}


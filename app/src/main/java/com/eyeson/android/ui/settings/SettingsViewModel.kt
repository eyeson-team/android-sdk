package com.eyeson.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyeson.android.data.SettingsRepository
import com.eyeson.android.ui.settings.SettingsUiState.Loading
import com.eyeson.android.ui.settings.SettingsUiState.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val settingsUiState: StateFlow<SettingsUiState> =
        combine(
            settingsRepository.micOnStart,
            settingsRepository.audioOnly,
            settingsRepository.videoOnStart,
            settingsRepository.rearCamOnStart,
            settingsRepository.screenShareOnStart
        ) { micOnStart, audioOnly, videoOnStart, rearCamOnStart, screenShareOnStart ->
            Success(
                Settings(
                    micOnStart,
                    audioOnly,
                    videoOnStart,
                    rearCamOnStart,
                    screenShareOnStart
                )
            )

        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = Loading
        )

    fun setMicOnStart(on: Boolean) {
        viewModelScope.launch { settingsRepository.setMicOnStart(on) }
    }

    fun setAudioOnly(on: Boolean) {
        viewModelScope.launch { settingsRepository.setAudioOnly(on) }
    }

    fun setVideoOnStart(on: Boolean) {
        viewModelScope.launch { settingsRepository.setVideoOnStart(on) }
    }

    fun setRearCamOnStart(on: Boolean) {
        viewModelScope.launch { settingsRepository.setRearCamOnStart(on) }
    }

    fun setScreenShareOnStart(on: Boolean) {
        viewModelScope.launch { settingsRepository.setScreenShareOnStart(on) }
    }
}

data class Settings(
    val micOnStar: Boolean,
    val audioOnly: Boolean,
    val videoOnStart: Boolean,
    val rearCamOnStart: Boolean,
    val screenShareOnStart: Boolean
)

sealed interface SettingsUiState {
    object Loading : SettingsUiState
    data class Success(val settings: Settings) : SettingsUiState
}

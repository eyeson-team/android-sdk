package com.eyeson.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyeson.android.data.SettingsRepository
import com.eyeson.android.ui.settings.SettingsUiState.Loading
import com.eyeson.android.ui.settings.SettingsUiState.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val settingsUiState: StateFlow<SettingsUiState> =
        settingsRepository.meetingSettings.map { settings ->
            Success(settings)

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

sealed interface SettingsUiState {
    object Loading : SettingsUiState
    data class Success(val settings: SettingsRepository.MeetingSettings) : SettingsUiState
}

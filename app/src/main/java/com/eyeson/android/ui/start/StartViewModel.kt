package com.eyeson.android.ui.start

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyeson.android.EyesonNavigationParameter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StartViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StartScreenState(
            accessKey = "",
            guestName = "SDK test user",
            guestToken = "",
            userTokenPermalink = "",
            guestNamePermalink = "SDK test user",
            guestTokenPermalink = ""
        )
    )
    val uiState: StateFlow<StartScreenState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            savedStateHandle.getStateFlow(
                key = EyesonNavigationParameter.GUEST_TOKEN,
                initialValue = ""
            ).collect {
                if (it.isNotBlank()) {
                    _uiState.value = _uiState.value.copy(guestToken = it)
                }
            }
        }
    }

    fun setAccessKey(accessKey: String) {
        _uiState.value = _uiState.value.copy(accessKey = accessKey)
    }

    fun setGuestName(guestName: String) {
        _uiState.value = _uiState.value.copy(guestName = guestName)
    }

    fun setGuestToken(guestToken: String) {
        _uiState.value = _uiState.value.copy(guestToken = guestToken)
    }

    fun setUserTokenPermalink(userTokenPermalink: String) {
        _uiState.value = _uiState.value.copy(userTokenPermalink = userTokenPermalink)
    }

    fun setGuestNamePermalink(guestNamePermalink: String) {
        _uiState.value = _uiState.value.copy(guestNamePermalink = guestNamePermalink)
    }

    fun setGuestTokenPermalink(guestTokenPermalink: String) {
        _uiState.value = _uiState.value.copy(guestTokenPermalink = guestTokenPermalink)
    }
}

data class StartScreenState(
    val accessKey: String,
    val guestName: String,
    val guestToken: String,
    val userTokenPermalink: String,
    val guestNamePermalink: String,
    val guestTokenPermalink: String,
) {

    val canConnect: Boolean
        get() = accessKey.isNotBlank() || (guestName.isNotBlank() && guestToken.isNotBlank())

    val canConnectPermalink: Boolean
        get() = userTokenPermalink.isNotBlank() || (guestNamePermalink.isNotBlank() && guestTokenPermalink.isNotBlank())
}

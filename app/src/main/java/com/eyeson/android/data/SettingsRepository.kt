package com.eyeson.android.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewModelScope
import com.eyeson.android.ui.settings.SettingsUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {

    private val micOnStartKey = booleanPreferencesKey("micOnStart")
    private val audioOnlyKey = booleanPreferencesKey("audioOnly")
    private val videoOnStartKey = booleanPreferencesKey("videoOnStart")
    private val rearCamOnStartKey = booleanPreferencesKey("rearCamOnStart")
    private val screenShareOnStartKey = booleanPreferencesKey("screenShareOnStart")

    private suspend fun <T> writeSetting(key: Preferences.Key<T>, value: T) {
        dataStore.edit { settings ->
            settings[key] = value
        }
    }

    private fun <T> readSetting(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
        return dataStore.data.map { preferences ->
            preferences[key] ?: defaultValue
        }
    }

    private val micOnStart = readSetting(micOnStartKey, true)
    suspend fun setMicOnStart(on: Boolean) {
        writeSetting(micOnStartKey, on)
    }

    private val audioOnly = readSetting(audioOnlyKey, false)
    suspend fun setAudioOnly(on: Boolean) {
        writeSetting(audioOnlyKey, on)
    }

    private val videoOnStart = readSetting(videoOnStartKey, true)
    suspend fun setVideoOnStart(on: Boolean) {
        writeSetting(videoOnStartKey, on)
    }

    private val rearCamOnStart = readSetting(rearCamOnStartKey, false)
    suspend fun setRearCamOnStart(on: Boolean) {
        writeSetting(rearCamOnStartKey, on)
    }

    private val screenShareOnStart = readSetting(screenShareOnStartKey, false)
    suspend fun setScreenShareOnStart(on: Boolean) {
        writeSetting(screenShareOnStartKey, on)
    }

    val meetingSettings: Flow<MeetingSettings> =
        combine(
            micOnStart,
            audioOnly,
            videoOnStart,
            rearCamOnStart,
            screenShareOnStart
        ) { micOnStart, audioOnly, videoOnStart, rearCamOnStart, screenShareOnStart ->
                MeetingSettings(
                    micOnStart,
                    audioOnly,
                    videoOnStart,
                    rearCamOnStart,
                    screenShareOnStart
                )
        }

    data class MeetingSettings(
        val micOnStar: Boolean,
        val audioOnly: Boolean,
        val videoOnStart: Boolean,
        val rearCamOnStart: Boolean,
        val screenShareOnStart: Boolean
    )
}

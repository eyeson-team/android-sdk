package com.eyeson.android.ui.view.main

import android.app.Application
import android.app.Notification
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eyeson.sdk.EyesonAudioManager
import com.eyeson.sdk.EyesonMeeting
import com.eyeson.sdk.events.MeetingJoinFailed
import com.eyeson.sdk.events.MeetingTerminated
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.VideoSink
import timber.log.Timber

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // One instance per view model: an EyesonMeeting holds one meeting at a time and can join
    // again after leave()
    private val eyesonMeeting: EyesonMeeting by lazy { EyesonMeeting(application = getApplication()) }
    private var audioManager: EyesonAudioManager? = null

    private val _callTerminated = MutableStateFlow(false)
    val callTerminated = _callTerminated.asStateFlow()

    var inCall = false

    init {
        viewModelScope.launch {
            eyesonMeeting.events.collect { event ->
                when (event) {
                    is MeetingJoinFailed -> {
                        Timber.d("Join failed: ${event.callRejectionReason}")
                        onMeetingEnded()
                    }

                    is MeetingTerminated -> {
                        Timber.d("Meeting terminated: ${event.callTerminationReason}")
                        onMeetingEnded()
                    }

                    else -> Unit
                }
            }
        }
    }

    // The SDK has already left at this point, so only the local state has to follow.
    private fun onMeetingEnded() {
        audioManager?.stop()
        audioManager = null
        inCall = false
        _callTerminated.value = true
    }

    fun connect(
        accessKey: String,
        local: VideoSink?,
        remote: VideoSink?,
        mediaProjectionPermissionResultData: Intent? = null,
        notificationId: Int? = null,
        notification: Notification? = null
    ) {
        _callTerminated.value = false

        val screenShareInfo = if (mediaProjectionPermissionResultData != null &&
            notificationId != null && notification != null
        ) {
            EyesonMeeting.ScreenShareInfo(
                mediaProjectionPermissionResultData,
                notificationId,
                notification
            )
        } else {
            null
        }

        val joinResult = eyesonMeeting.join(
            accessKey = accessKey,
            frontCamera = true,
            local = local,
            remote = remote,
            microphoneEnabledOnStart = true,
            videoEnabledOnStart = true,
            screenShareInfo = screenShareInfo
        )

        when (joinResult) {
            EyesonMeeting.JoinResult.Joining -> {
                // Set here and not on MeetingJoined: while joining, a re-created view has to
                // re-attach its renderers rather than start a second join.
                inCall = true
                startAudioManager()
            }

            is EyesonMeeting.JoinResult.MissingPermissions -> {
                Timber.d("Join not started; missing permissions: ${joinResult.permissions}")
            }

            EyesonMeeting.JoinResult.AlreadyJoined -> {
                Timber.d("Join not started; already in a meeting")
            }
        }
    }

    fun disconnect() {
        audioManager?.stop()
        audioManager = null

        eyesonMeeting.leave()
        inCall = false
    }

    fun clearTarget() {
        eyesonMeeting.setLocalVideoTarget(null)
        eyesonMeeting.setRemoteVideoTarget(null)
    }

    fun setTargets(local: VideoSink?, remote: VideoSink?) {
        eyesonMeeting.setLocalVideoTarget(local)
        eyesonMeeting.setRemoteVideoTarget(remote)
    }

    fun isVideoEnabled(): Boolean {
        return eyesonMeeting.isVideoEnabled()
    }

    private fun startAudioManager() {
        audioManager = EyesonAudioManager(getApplication())
        audioManager?.start(object : EyesonAudioManager.AudioManagerEvents {
            override fun onAudioDeviceChanged(
                selectedAudioDevice: EyesonAudioManager.AudioDevice,
                availableAudioDevices: Set<EyesonAudioManager.AudioDevice>
            ) {
                Timber.d("onAudioManagerDevicesChanged: $availableAudioDevices, selected: $selectedAudioDevice")
            }
        })
    }
}
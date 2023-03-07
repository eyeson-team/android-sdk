package com.eyeson.android.ui.view.main

import android.app.Application
import android.app.Notification
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eyeson.sdk.EyesonAudioManager
import com.eyeson.sdk.EyesonMeeting
import com.eyeson.sdk.events.CallRejectionReason
import com.eyeson.sdk.events.CallTerminationReason
import com.eyeson.sdk.events.EyesonEventListener
import com.eyeson.sdk.model.local.api.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.VideoSink
import timber.log.Timber

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private var eyesonMeeting: EyesonMeeting? = null
    private var audioManager: EyesonAudioManager? = null

    private val _callTerminated = MutableStateFlow(false)
    val callTerminated = _callTerminated.asStateFlow()

    var inCall = false

    private var lastCameraState = isVideoEnabled()
    private var presentationActive = false

    private val eventListener = object : EyesonEventListener() {
        override fun onMeetingJoined() {
            inCall = true
            lastCameraState = isVideoEnabled()
        }

        override fun onMeetingJoinFailed(callRejectionReason: CallRejectionReason) {
            _callTerminated.value = true
        }

        override fun onMeetingTerminated(callTerminationReason: CallTerminationReason) {
            inCall = false
            Timber.d("KICK: _callTerminated.value = true")
            _callTerminated.value = true
        }

        override fun onVideoSourceUpdate(
            visibleUsers: List<UserInfo>,
            presenter: UserInfo?
        ) {
            if (!presentationActive && presenter?.id != null && presenter.id != eyesonMeeting?.getUserInfo()?.id) {
                presentationActive = true
                lastCameraState = isVideoEnabled()
                eyesonMeeting?.setVideoEnabled(false)

            }
            if (presentationActive && presenter == null) {
                presentationActive = false
                eyesonMeeting?.setVideoEnabled(lastCameraState)
                lastCameraState = isVideoEnabled()
            }
        }
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

        viewModelScope.launch {
            eyesonMeeting = EyesonMeeting(
                eventListener = eventListener,
                application = getApplication()
            ).apply {
                join(
                    accessKey = accessKey,
                    frontCamera = true,
                    audiOnly = false,
                    local = local,
                    remote = remote,
                    microphoneEnabledOnStart = true,
                    videoEnabledOnStart = true,
                    screenShareInfo = screenShareInfo
                )
            }
            startAudioManager()
        }
    }

    fun disconnect() {
        audioManager?.stop()
        audioManager = null

        eyesonMeeting?.leave()
        inCall = false
    }

    fun getEglContext(): EglBase.Context? {
        return eyesonMeeting?.getEglContext()
    }

    fun clearTarget() {
        eyesonMeeting?.setLocalVideoTarget(null)
        eyesonMeeting?.setRemoteVideoTarget(null)
    }

    fun setTargets(local: VideoSink?, remote: VideoSink?) {
        eyesonMeeting?.setLocalVideoTarget(local)
        eyesonMeeting?.setRemoteVideoTarget(remote)
    }

    fun isVideoEnabled(): Boolean {
        return eyesonMeeting?.isVideoEnabled() ?: false
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
package com.eyeson.sdk.callLogic

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.projection.MediaProjection
import android.os.IBinder
import com.eyeson.sdk.di.NetworkModule
import com.eyeson.sdk.events.CallTerminationReason
import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.datachannel.base.DataChannelCommandDto
import com.eyeson.sdk.model.datachannel.base.UnknownCommandDto
import com.eyeson.sdk.model.datachannel.incoming.PingDto
import com.eyeson.sdk.model.datachannel.outgoing.PongDto
import com.eyeson.sdk.model.datachannel.outgoing.fromLocal
import com.eyeson.sdk.model.local.base.LocalBaseCommand
import com.eyeson.sdk.model.local.call.CameraClosedLocal
import com.eyeson.sdk.model.local.call.CameraDisconnectedLocal
import com.eyeson.sdk.model.local.call.CameraErrorLocal
import com.eyeson.sdk.model.local.call.CameraFirstFrameAvailableLocal
import com.eyeson.sdk.model.local.call.CameraFrozenLocal
import com.eyeson.sdk.model.local.call.CameraOpenLocal
import com.eyeson.sdk.model.local.call.CameraSwitchDoneLocal
import com.eyeson.sdk.model.local.call.CameraSwitchErrorLocal
import com.eyeson.sdk.model.local.call.MeetingJoinedLocal
import com.eyeson.sdk.model.local.datachannel.Pong
import com.eyeson.sdk.model.local.sepp.CallStart
import com.eyeson.sdk.model.local.sepp.CallTerminated
import com.eyeson.sdk.options.AudioOptions
import com.eyeson.sdk.options.MediaOptions
import com.eyeson.sdk.options.ScreenShareOptions
import com.eyeson.sdk.options.VideoOptions
import com.eyeson.sdk.serializer.DataChannelIncomingMessagesSerializer
import com.eyeson.sdk.service.ScreenCapturerService
import com.eyeson.sdk.utils.Logger
import com.eyeson.sdk.utils.WebRTCUtils
import com.eyeson.sdk.webrtc.PeerConnectionClient
import com.eyeson.sdk.webrtc.PeerConnectionParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.IceCandidate
import org.webrtc.PeerConnectionFactory
import org.webrtc.RTCStatsReport
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.SessionDescription
import org.webrtc.VideoCapturer
import org.webrtc.VideoFrame
import org.webrtc.VideoSink
import java.util.concurrent.atomic.AtomicBoolean


internal class CallLogic(
    @Volatile private var meeting: MeetingDto,
    private val context: Context,
    val audioOptions: AudioOptions,
    videoOptions: VideoOptions,
    screenShareOptions: ScreenShareOptions,
    private val videoCodecHardwareAcceleration: Boolean,
    private val mediaOptions: MediaOptions,
    private val json: Json = NetworkModule.json,
) {
    private val callLogicScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events = MutableSharedFlow<LocalBaseCommand>(0)
    val events = _events.asSharedFlow()


    private val sfuMode = AtomicBoolean(false)
    private val cameraIsFrontFacing = AtomicBoolean(true)

    private val connectionStatisticsRepository = ConnectionStatisticsRepository()
    private var screenCapturerService: ScreenCapturerService? = null
    private var screenCapturerServiceBound: Boolean = false
    private var screenCapturerServiceConnection: ServiceConnection? = null

    var videoOptions = videoOptions
        set(value) {
            field = value
            peerConnectionClient.updatePeerConnectionParameter(videoOptions = value)
        }

    var screenShareOptions = screenShareOptions
        set(value) {
            field = value
            peerConnectionClient.updatePeerConnectionParameter(screenShareOptions = value)
        }

    internal class ProxyVideoSink : VideoSink {
        private var target: VideoSink? = null

        @Synchronized
        override fun onFrame(frame: VideoFrame) {
            if (target == null) {
                return
            }
            target?.onFrame(frame)
        }

        @Synchronized
        fun setTarget(target: VideoSink?) {
            this.target = target
        }
    }

    private val remoteProxyVideoSink = ProxyVideoSink()
    private val localProxyVideoSink = ProxyVideoSink()

    private val peerConnectionEvents = object :
        PeerConnectionClient.PeerConnectionEvents() {

        override fun onIceCandidate(candidate: IceCandidate) {
            peerConnectionClient.addRemoteIceCandidate(candidate)
        }

        override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {
            peerConnectionClient.removeRemoteIceCandidates(candidates)
        }

        override fun onConnected() {
            Logger.d("onConnected")
            emitEvent(MeetingJoinedLocal)
        }

        override fun onDisconnected() {
            Logger.d("onDisconnected")
            emitEvent(CallTerminated(CallTerminationReason.OK.terminationCode))
        }

        override fun onPeerConnectionStatsReady(report: RTCStatsReport) {
            connectionStatisticsRepository.addNewRTCStatsReport(report)
            connectionStatisticsRepository.getStatsInfo()?.let {
                emitEvent(it)
            }
        }

        override fun onPeerConnectionError(description: String) {
            emitEvent(CallTerminated(CallTerminationReason.ERROR.terminationCode))
        }

        override fun onIceGatheringComplete(sdpToBeSent: String) {
            Logger.d("Ice gathering complete.")
            sendSdpCallStart(sdpToBeSent)
        }

        override fun onCameraSwitchDone(isFrontCamera: Boolean) {
            cameraIsFrontFacing.set(isFrontCamera)
            emitEvent(CameraSwitchDoneLocal(isFrontCamera))
        }

        override fun onCameraSwitchError(error: String) {
            emitEvent(CameraSwitchErrorLocal(error))
        }
    }

    private val cameraEventsHandler = object : CameraVideoCapturer.CameraEventsHandler {
        override fun onCameraError(error: String?) {
            emitEvent(CameraErrorLocal(error))
        }

        override fun onCameraDisconnected() {
            emitEvent(CameraDisconnectedLocal)
        }

        override fun onCameraFreezed(error: String?) {
            emitEvent(CameraFrozenLocal(error))
        }

        override fun onCameraOpening(cameraName: String?) {
            emitEvent(CameraOpenLocal(cameraName))
        }

        override fun onFirstFrameAvailable() {
            emitEvent(CameraFirstFrameAvailableLocal)
        }

        override fun onCameraClosed() {
            emitEvent(CameraClosedLocal)
        }
    }

    private val dataChannelEvents = object : PeerConnectionClient.DataChannelEvents {
        override fun onMessageReceived(message: String) {
            val command = try {
                json.decodeFromString(DataChannelIncomingMessagesSerializer, message)
            } catch (e: IllegalArgumentException) {
                // SerializationException is an IllegalArgumentException
                Logger.e("DataChannelEvents: parsing FAILED $e")
                e.printStackTrace()
                return
            }
            handleDataChannelCommands(command)
        }
    }

    private fun handleDataChannelCommands(command: DataChannelCommandDto) {
        when (command) {
            is PingDto -> {
                peerConnectionClient.sendDataChannelMessage(
                    json.encodeToString(PongDto.serializer(), Pong().fromLocal())
                )
            }

            !is UnknownCommandDto -> {
                emitEvent(command.toLocal())
            }
        }
    }

    fun setLocalVideoEnabled(enable: Boolean) {
        if (enable) {
            peerConnectionClient.startVideoSource()
        } else {
            peerConnectionClient.stopVideoSource()
        }
        peerConnectionClient.setLocalVideoEnabled(enable)
    }

    fun setAudioEnabled(enable: Boolean) {
        peerConnectionClient.setAudioEnabled(enable)
    }

    fun isAudioEnabled(): Boolean {
        return peerConnectionClient.enableAudio
    }

    fun setRemoteAudioEnabled(enable: Boolean) {
        peerConnectionClient.setAudioTracksEnabled(enable)
    }

    private val peerConnectionClient: PeerConnectionClient by lazy {
        PeerConnectionClient(
            context,
            PeerConnectionParameters(
                sendAudio = mediaOptions.sendAudio,
                receiveAudio = mediaOptions.receiveAudio,
                audioOptions = audioOptions,
                sendVideo = mediaOptions.sendVideo,
                receiveVideo = mediaOptions.receiveVideo,
                videoOptions = videoOptions,
                screenShareOptions = screenShareOptions,
                videoCodecHardwareAcceleration = videoCodecHardwareAcceleration,
                isWidescreen = meeting.options.widescreen
            ),
            peerConnectionEvents,
            dataChannelEvents,
        ).apply { createPeerConnectionFactory(PeerConnectionFactory.Options()) }
    }

    fun startCall(
        frontCamera: Boolean,
        microphoneEnabledOnStart: Boolean,
        videoEnabledOnStart: Boolean,
        mediaProjectionPermissionResultData: Intent?,
        notificationId: Int?,
        notification: Notification?,
    ) {
        val videoCapturer = when {
            // Not sending video takes precedence over a screen share on start: there is no video
            // sender to feed a capturer into.
            !mediaOptions.sendVideo -> null

            mediaProjectionPermissionResultData != null && notificationId != null && notification != null -> {
                val connection: ServiceConnection = object : ServiceConnection {
                    override fun onServiceConnected(className: ComponentName, service: IBinder) {
                        val binder = service as ScreenCapturerService.LocalBinder
                        screenCapturerService = binder.getService()
                        screenCapturerService?.startForegroundWithNotification(
                            notificationId,
                            notification
                        )
                        screenCapturerServiceBound = true
                    }

                    override fun onServiceDisconnected(arg0: ComponentName) {
                        endScreenShareForeground()
                    }
                }

                val intent = Intent(context, ScreenCapturerService::class.java)
                context.bindService(intent, connection, Context.BIND_AUTO_CREATE)

                ScreenCapturerAndroid(
                    mediaProjectionPermissionResultData,
                    object : MediaProjection.Callback() {
                        override fun onStop() {
                            Logger.d("ScreenCapturer stopped")
                            endScreenShareForeground()
                        }
                    })
            }

            else -> {
                createVideoCapturer(!frontCamera)
            }
        }

        peerConnectionClient.createPeerConnection(
            localProxyVideoSink,
            remoteProxyVideoSink,
            videoCapturer,
            meeting.signaling.options.stunServers,
            meeting.signaling.options.turnServers,
            microphoneEnabledOnStart,
            videoEnabledOnStart
        ) {
            peerConnectionClient.createOffer()
            peerConnectionClient.enableStatsEvents(true, STATS_INTERVAL_MS)
        }
    }

    fun disconnectCall() {
        endScreenShareForeground()

        remoteProxyVideoSink.setTarget(null)
        localProxyVideoSink.setTarget(null)

        peerConnectionClient.close()

        // A CallLogic is created per join and never reused. Cancelling the scope here stops
        // pending emitEvent coroutines from outliving the call they belong to.
        callLogicScope.cancel()
    }

    fun setLocalVideoTarget(target: VideoSink?) {
        localProxyVideoSink.setTarget(target)
    }

    fun setRemoteVideoTarget(target: VideoSink?) {
        remoteProxyVideoSink.setTarget(target)
    }

    fun startScreenShare(
        mediaProjectionPermissionResultData: Intent,
        notificationId: Int,
        notification: Notification,
        asPresentation: Boolean,
        enablePresentation: () -> Unit,
    ): Boolean {
        if (!mediaOptions.sendVideo) {
            Logger.w("Screen share is not possible in a meeting that does not send video")
            return false
        }

        if (screenCapturerServiceBound) {
            return false
        }

        screenCapturerServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                val binder = service as ScreenCapturerService.LocalBinder
                screenCapturerService = binder.getService()
                screenCapturerService?.startForegroundWithNotification(notificationId, notification)

                val screenCapturer = ScreenCapturerAndroid(
                    mediaProjectionPermissionResultData,
                    object : MediaProjection.Callback() {
                        override fun onStop() {
                            Logger.d("ScreenCapturer stopped")
                            endScreenShareForeground()
                        }
                    })

                peerConnectionClient.replaceVideoCapturer(
                    capturer = screenCapturer,
                    videoEnabledOnStart = true,
                )

                if (asPresentation) {
                    enablePresentation()
                }

                screenCapturerServiceBound = true
            }

            override fun onServiceDisconnected(arg0: ComponentName) {
                endScreenShareForeground()
            }
        }.also {
            val intent = Intent(context, ScreenCapturerService::class.java)
            context.bindService(intent, it, Context.BIND_AUTO_CREATE)
        }

        return true
    }

    private fun endScreenShareForeground() {
        screenCapturerService?.endForeground()
        screenCapturerServiceConnection?.let {
            context.unbindService(it)
        }
        screenCapturerServiceConnection = null
        screenCapturerService = null
        screenCapturerServiceBound = false
    }

    fun stopScreenShare(resumeLocalVideo: Boolean) {
        if (!mediaOptions.sendVideo) {
            return
        }

        endScreenShareForeground()

        peerConnectionClient.replaceVideoCapturer(
            createVideoCapturer(!cameraIsFrontFacing.get()),
            resumeLocalVideo
        )
    }

    fun isScreenShareActive(): Boolean {
        return peerConnectionClient.isScreencastActive()
    }

    private fun createVideoCapturer(preferBackCamera: Boolean): VideoCapturer? {
        val videoCapturer: VideoCapturer? = when {
            Camera2Enumerator.isSupported(context) -> {
                Logger.d("Creating capturer using camera2 API.")
                createCameraCapturer(Camera2Enumerator(context), preferBackCamera)
            }

            else -> {
                Logger.d("Creating capturer using camera1 API.")
                createCameraCapturer(
                    Camera1Enumerator(true),
                    preferBackCamera
                )
            }
        }
        if (videoCapturer == null) {
            Logger.e("Failed to open camera")
            return null
        }
        return videoCapturer
    }


    private fun createCameraCapturer(
        enumerator: CameraEnumerator,
        preferBackCamera: Boolean,
    ): VideoCapturer? {
        val deviceNames = enumerator.deviceNames
        var preferredCapturer: VideoCapturer? = null
        var preferredIsFrontFacing = false
        // First, try to find front facing camera
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val videoCapturer = enumerator.createCapturer(deviceName, cameraEventsHandler)

                if (videoCapturer != null) {
                    preferredCapturer = videoCapturer
                    preferredIsFrontFacing = true
                    break
                }
            }
        }

        if (preferredCapturer != null && !preferBackCamera) {
            cameraIsFrontFacing.set(true)
            return preferredCapturer
        }

        // Front facing camera not found, try something else
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                val videoCapturer = enumerator.createCapturer(deviceName, cameraEventsHandler)

                if (videoCapturer != null) {
                    preferredCapturer = videoCapturer
                    preferredIsFrontFacing = false
                    break
                }
            }
        }

        // The facing of the camera actually opened, which is not the preferred one if the device
        // has no camera facing that way. Camera switches update it from then on.
        if (preferredCapturer != null) {
            cameraIsFrontFacing.set(preferredIsFrontFacing)
        }
        return preferredCapturer
    }

    private fun prepareSdpForSending(sdpToBeSent: String?): String {
        if (sdpToBeSent.isNullOrBlank()) {
            Logger.i("prepareAndSendSdp: SDP is blank. Nothing to send")
            return ""
        }

        var newSdpToBeSent = ""
        var found = false
        val lines = sdpToBeSent.split("\r\n".toRegex())

        for (line in lines) {
            if (!found && line.startsWith("m=")) {
                newSdpToBeSent += "$SFU_CAPABLE_SDP_PARAMETER\r\n"
                newSdpToBeSent += "$DATA_CHANNEL_CAPABLE_SDP_PARAMETER\r\n"
                newSdpToBeSent += "$DATA_CHANNEL_KEEP_ALIVE_SDP_PARAMETER\r\n"
                newSdpToBeSent += "$SEPP_MESSAGING\r\n"
                found = true
            }
            newSdpToBeSent += "$line\r\n"
        }

        return newSdpToBeSent
    }

    private fun sendSdpCallStart(sdpToBeSent: String) {
        val preparedSdp = prepareSdpForSending(sdpToBeSent)
        if (preparedSdp.isBlank()) {
            return
        }
        emitEvent(CallStart(meeting.user.name, preparedSdp))
    }

    fun getSdpForCallResume(): String {
        return prepareSdpForSending(peerConnectionClient.localSdp?.description).also {
            WebRTCUtils.logSdp("getSdpForCallResume newSdp", it)
        }
    }

    fun setRemoteDescription(description: String, type: SessionDescription.Type) {
        val sdp = SessionDescription(type, description)
        sfuMode.set(description.contains(SFU_ON_SDP_PARAMETER))
        peerConnectionClient.setRemoteDescription(sdp)
    }

    fun isSfuMode(): Boolean {
        return sfuMode.get()
    }

    fun isLocalVideoActive(): Boolean {
        return peerConnectionClient.renderLocalVideo
    }

    fun switchCamera() {
        peerConnectionClient.switchCamera()
    }

    fun switchCameraTo(cameraId: String) {
        peerConnectionClient.switchCameraTo(cameraId)
    }

    fun isFrontCamera(): Boolean {
        return cameraIsFrontFacing.get()
    }

    private fun emitEvent(event: LocalBaseCommand) {
        callLogicScope.launch {
            _events.emit(event)
        }
    }

    companion object {
        private const val SFU_CAPABLE_SDP_PARAMETER = "a=sfu-capable"
        private const val SFU_ON_SDP_PARAMETER = "a=sfu-mode:on"
        private const val DATA_CHANNEL_CAPABLE_SDP_PARAMETER = "a=eyeson-datachan-capable"
        private const val DATA_CHANNEL_KEEP_ALIVE_SDP_PARAMETER = "a=eyeson-datachan-keepalive"
        private const val SEPP_MESSAGING = "a=eyeson-sepp-messaging"
        const val STATS_INTERVAL_MS = 1000
    }
}
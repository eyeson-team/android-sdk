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
import com.eyeson.sdk.model.local.call.CameraSwitchDone
import com.eyeson.sdk.model.local.call.CameraSwitchError
import com.eyeson.sdk.model.local.call.MeetingJoined
import com.eyeson.sdk.model.local.datachannel.Pong
import com.eyeson.sdk.model.local.sepp.CallStart
import com.eyeson.sdk.model.local.sepp.CallTerminated
import com.eyeson.sdk.service.ScreenCapturerService
import com.eyeson.sdk.utils.Logger
import com.eyeson.sdk.utils.WebRTCUtils
import com.eyeson.sdk.webrtc.PeerConnectionClient
import com.squareup.moshi.JsonDataException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.EglBase
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
    private val audioOnly: Boolean,
    private val context: Context,
    private val rootEglBase: EglBase,
    private val experimentalFeatureStereo: Boolean = false
) {
    private val callLogicScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events = MutableSharedFlow<LocalBaseCommand>(0)
    val events = _events.asSharedFlow()


    private val sfuMode = AtomicBoolean(false)
    private val cameraIsFrontFacing = AtomicBoolean(true)

    private val moshi = NetworkModule.moshi

    private val connectionStatisticsRepository = ConnectionStatisticsRepository()
    private var screenCapturerService: ScreenCapturerService? = null
    private var screenCapturerServiceBound: Boolean = false

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
            emitEvent(MeetingJoined())
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
            emitEvent(CameraSwitchDone(isFrontCamera))
        }

        override fun onCameraSwitchError(error: String) {
            emitEvent(CameraSwitchError(error))
        }
    }

    private val dataChannelEvents = object : PeerConnectionClient.DataChannelEvents {
        override fun onMessageReceived(message: String) {
            val adapter = moshi.adapter(DataChannelCommandDto::class.java)
            try {
                val command = adapter.fromJson(message)
                command?.let {
                    handleDataChannelCommands(it)
                }
            } catch (e: JsonDataException) {
                Logger.e("DataChannelEvents: parsing FAILED $e")
                e.printStackTrace()
            }
        }
    }

    private fun handleDataChannelCommands(command: DataChannelCommandDto) {
        when (command) {
            is PingDto -> {
                val adapter = moshi.adapter(PongDto::class.java)
                peerConnectionClient.sendDataChannelMessage(adapter.toJson(Pong().fromLocal()))
            }
            !is UnknownCommandDto -> {
                emitEvent(command.toLocal())
            }
        }
    }

    fun setLocalVideoEnabled(enable: Boolean) {
        peerConnectionClient.setLocalVideoEnabled(enable)
    }

    fun setAudioEnabled(enable: Boolean) {
        peerConnectionClient.setAudioEnabled(enable)
    }

    fun isAudioEnabled(): Boolean {
        return peerConnectionClient.enableAudio
    }

    private val peerConnectionClient: PeerConnectionClient by lazy {
        PeerConnectionClient(
            context,
            rootEglBase,
            PeerConnectionClient.PeerConnectionParameters(audioOnly, meeting.options.widescreen),
            peerConnectionEvents,
            dataChannelEvents,
            experimentalFeatureStereo
        ).apply { createPeerConnectionFactory(PeerConnectionFactory.Options()) }
    }

    fun startCall(
        frontCamera: Boolean,
        microphoneEnabledOnStart: Boolean,
        videoEnabledOnStart: Boolean,
        mediaProjectionPermissionResultData: Intent?,
        notificationId: Int?,
        notification: Notification?
    ) {
        val videoCapturer = when {
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

                ScreenCapturerAndroid(mediaProjectionPermissionResultData,
                    object : MediaProjection.Callback() {
                        override fun onStop() {
                            Logger.d("ScreenCapturer stopped")
                            endScreenShareForeground()
                        }
                    })
            }
            !audioOnly -> {
                createVideoCapturer(!frontCamera)
            }
            else -> {
                null
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
    }

    fun setLocalVideoTarget(target: VideoSink?) {
        localProxyVideoSink.setTarget(target)
    }

    fun setRemoteVideoTarget(target: VideoSink?) {
        remoteProxyVideoSink.setTarget(target)
    }

    fun startScreenShare(
        mediaProjectionPermissionResultData: Intent,
        asPresentation: Boolean,
        notificationId: Int,
        notification: Notification,
        enablePresentation: () -> Unit
    ): Boolean {
        if (screenCapturerServiceBound) {
            return false
        }

        val connection: ServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                val binder = service as ScreenCapturerService.LocalBinder
                screenCapturerService = binder.getService()
                screenCapturerService?.startForegroundWithNotification(notificationId, notification)

                val screenCapturer = ScreenCapturerAndroid(mediaProjectionPermissionResultData,
                    object : MediaProjection.Callback() {
                        override fun onStop() {
                            Logger.d("ScreenCapturer stopped")
                            endScreenShareForeground()
                        }
                    })

                peerConnectionClient.replaceVideoCapturer(
                    screenCapturer,
                    true,
                    customFps = SCREEN_SHARE_FPS
                )

                if (asPresentation) {
                    enablePresentation()
                }

                screenCapturerServiceBound = true
            }

            override fun onServiceDisconnected(arg0: ComponentName) {
                endScreenShareForeground()
            }
        }

        val intent = Intent(context, ScreenCapturerService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        return true
    }

    private fun endScreenShareForeground() {
        screenCapturerService?.endForeground()
        // TODO Unbind service
        screenCapturerService = null
        screenCapturerServiceBound = false
    }

    fun stopScreenShare(resumeLocalVideo: Boolean) {
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
        preferBackCamera: Boolean
    ): VideoCapturer? {
        val deviceNames = enumerator.deviceNames
        var preferredCapturer: VideoCapturer? = null
        // First, try to find front facing camera
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val videoCapturer = enumerator.createCapturer(deviceName, null)

                if (videoCapturer != null) {
                    preferredCapturer = videoCapturer
                    break
                }
            }
        }

        if (preferredCapturer != null && !preferBackCamera) {
            return preferredCapturer
        }

        // Front facing camera not found, try something else
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                val videoCapturer = enumerator.createCapturer(deviceName, null)

                if (videoCapturer != null) {
                    preferredCapturer = videoCapturer
                    break
                }
            }
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

    fun isVideoEnabled(): Boolean {
        return peerConnectionClient.isVideoCallEnabled
    }

    fun isLocalVideoActive(): Boolean {
        return peerConnectionClient.renderLocalVideo
    }

    fun switchCamera() {
        peerConnectionClient.switchCamera()
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

        private const val SCREEN_SHARE_FPS = 15
    }
}
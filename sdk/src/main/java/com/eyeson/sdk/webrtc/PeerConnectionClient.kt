/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 *
 * LICENSE: https://chromium.googlesource.com/external/webrtc/+/master/LICENSE
 * PATENTS: https://chromium.googlesource.com/external/webrtc/+/master/PATENTS
 * AUTHORS: https://chromium.googlesource.com/external/webrtc/+/master/AUTHORS
 */
package com.eyeson.sdk.webrtc

import android.content.Context
import android.util.Range
import com.eyeson.sdk.BuildConfig.DEBUG
import com.eyeson.sdk.model.api.TurnServerDto
import com.eyeson.sdk.options.Resolution16by9
import com.eyeson.sdk.options.Resolution4by3
import com.eyeson.sdk.options.ScreenShareOptions
import com.eyeson.sdk.options.VideoOptions
import com.eyeson.sdk.options.VideoResolution
import com.eyeson.sdk.utils.Logger
import com.eyeson.sdk.utils.WebRTCUtils.logSdp
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.CameraVideoCapturer
import org.webrtc.CameraVideoCapturer.CameraSwitchHandler
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.Logging
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceConnectionState
import org.webrtc.PeerConnection.IceGatheringState
import org.webrtc.PeerConnection.IceServer
import org.webrtc.PeerConnection.PeerConnectionState
import org.webrtc.PeerConnection.RTCConfiguration
import org.webrtc.PeerConnection.SignalingState
import org.webrtc.PeerConnectionFactory
import org.webrtc.RTCStatsReport
import org.webrtc.RtpSender
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SoftwareVideoDecoderFactory
import org.webrtc.SoftwareVideoEncoderFactory
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoDecoderFactory
import org.webrtc.VideoEncoderFactory
import org.webrtc.VideoSink
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule.AudioTrackErrorCallback
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Peer connection client implementation.
 *
 *
 * All public methods are routed to local looper thread.
 * All PeerConnectionEvents callbacks are invoked from the same looper thread.
 * This class is a singleton.
 */
internal class PeerConnectionClient(
    private val appContext: Context,
    private val rootEglBase: EglBase,
    peerConnectionParameters: PeerConnectionParameters,
    private val events: PeerConnectionEvents,
    private val dataChannelEvents: DataChannelEvents,
) {
    private val pcObserver = PCObserver()
    private val sdpObserver = SDPObserver()
    private val statsTimer = Timer()
    private val iceGatheringTimer = Timer()
    private val iceGatheringSend = AtomicBoolean(false)

    private val switchEventsHandler: CameraSwitchHandler = object : CameraSwitchHandler {
        override fun onCameraSwitchDone(isFrontCamera: Boolean) {
            events.onCameraSwitchDone(isFrontCamera)
        }

        override fun onCameraSwitchError(errorDescription: String) {
            events.onCameraSwitchError(errorDescription)
        }
    }
    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var videoSource: VideoSource? = null
    private var videoCapturerStopped = false
    private var isError = false
    private var localRender: VideoSink? = null
    private var remoteSinks: List<VideoSink>? = null

    private val audioConstraints: MediaConstraints by lazy {
        MediaConstraints().apply {
            listOf(
                AUDIO_ECHO_CANCELLATION_CONSTRAINT to peerConnectionParameters.audioOptions.echoCancellation,
                AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT to peerConnectionParameters.audioOptions.autoGainControl,
                AUDIO_HIGH_PASS_FILTER_CONSTRAINT to peerConnectionParameters.audioOptions.highpassFilter,
                AUDIO_NOISE_SUPPRESSION_CONSTRAINT to peerConnectionParameters.audioOptions.noiseSuppression,
                AUDIO_TYPING_NOISE_DETECTION_CONSTRAINT to peerConnectionParameters.audioOptions.typingNoiseDetection
            ).forEach { (key, value) ->
                mandatory.add(MediaConstraints.KeyValuePair(key, value.toString()))
            }
        }
    }

    private val sdpMediaConstraints: MediaConstraints by lazy {
        MediaConstraints().apply {
            listOf(
                RECEIVE_AUDIO_CONSTRAINT to peerConnectionParameters.receiveAudio,
                RECEIVE_VIDEO_CONSTRAINT to peerConnectionParameters.receiveVideo
            ).forEach { (key, value) ->
                mandatory.add(MediaConstraints.KeyValuePair(key, value.toString()))
            }
        }
    }

    // Queued remote ICE candidates are consumed only after both local and
    // remote descriptions are set. Similarly local ICE candidates are sent to
    // remote peer after both local and remote description are set.
    private var queuedRemoteCandidates: MutableList<IceCandidate>? = null
    private var isInitiator = false
    var localSdp: SessionDescription? = null
        private set

    private var videoCapturer: VideoCapturer? = null

    // enableVideo is set to true if video should be rendered and sent.
    var renderLocalVideo = true
        private set

    var renderRemoteVideo = true
        private set

    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null

    private val localVideoSender: RtpSender?
        get() {
            return peerConnection?.senders?.firstOrNull { sender ->
                sender.track()?.kind() == VIDEO_TRACK_TYPE
            }
        }

    var enableAudio = true
        private set
    private var localAudioTrack: AudioTrack? = null
    private var dataChannel: DataChannel? = null

    // Enable RtcEventLog.
    private var rtcEventLog: RtcEventLog? = null

    var peerConnectionParameters: PeerConnectionParameters = peerConnectionParameters
        private set

    /**
     * Create a PeerConnectionClient with the specified parameters. PeerConnectionClient takes
     * ownership of |eglBase|.
     */
    init {
        executor.execute {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(appContext)
                    .setEnableInternalTracer(true)
                    .createInitializationOptions()
            )
        }
    }

    /**
     * This function should only be called once.
     */
    fun createPeerConnectionFactory(options: PeerConnectionFactory.Options?) {
        check(factory == null) { "PeerConnectionFactory has already been constructed" }
        executor.execute { createPeerConnectionFactoryInternal(options) }
    }

    fun createPeerConnection(
        localRender: VideoSink?,
        remoteSink: VideoSink,
        videoCapturer: VideoCapturer?,
        stunServers: List<String>,
        turnServers: List<TurnServerDto>,
        microphoneEnabledOnStart: Boolean = true,
        videoEnabledOnStart: Boolean = true,
        peerConnectionReadyCallback: Runnable?,
    ) {
        createPeerConnection(
            localRender,
            listOf(remoteSink),
            videoCapturer,
            stunServers,
            turnServers,
            microphoneEnabledOnStart,
            videoEnabledOnStart,
            peerConnectionReadyCallback
        )
    }

    private fun createPeerConnection(
        localRender: VideoSink?,
        remoteSinks: List<VideoSink>?,
        videoCapturer: VideoCapturer?,
        stunServers: List<String>,
        turnServers: List<TurnServerDto>,
        microphoneEnabledOnStart: Boolean,
        videoEnabledOnStart: Boolean,
        peerConnectionReadyCallback: Runnable?,
    ) {
        this.localRender = localRender
        this.remoteSinks = remoteSinks
        this.videoCapturer = videoCapturer

        executor.execute {
            try {
                createPeerConnectionInternal(
                    stunServers,
                    turnServers,
                    peerConnectionReadyCallback,
                    microphoneEnabledOnStart,
                    videoEnabledOnStart
                )
            } catch (e: Exception) {
                reportError("Failed to create peer connection: ${e.message}")
                throw e
            }
        }
    }

    fun close() {
        executor.execute { closeInternal() }
    }


    private val isVideoCallEnabled: Boolean
        get() = renderLocalVideo && videoCapturer != null

    private fun createPeerConnectionFactoryInternal(options: PeerConnectionFactory.Options?) {
        isError = false

        val audioDeviceModule = createJavaAudioDevice()

        // Create peer connection factory.
        if (options != null) {
            Logger.d("Factory networkIgnoreMask option: ${options.networkIgnoreMask}")
        }
        val encoderFactory: VideoEncoderFactory
        val decoderFactory: VideoDecoderFactory
        if (peerConnectionParameters.videoOptions.videoCodecHardwareAcceleration) {
            encoderFactory = DefaultVideoEncoderFactory(
                rootEglBase.eglBaseContext, true /* enableIntelVp8Encoder */, false
            )
            decoderFactory = DefaultVideoDecoderFactory(rootEglBase.eglBaseContext)
        } else {
            encoderFactory = SoftwareVideoEncoderFactory()
            decoderFactory = SoftwareVideoDecoderFactory()
        }
        factory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setAudioDeviceModule(audioDeviceModule)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
        Logger.d("Peer connection factory created.")
    }

    private fun createJavaAudioDevice(): AudioDeviceModule {
        val audioTrackErrorCallback: AudioTrackErrorCallback = object : AudioTrackErrorCallback {
            override fun onWebRtcAudioTrackInitError(errorMessage: String) {
                Logger.e("onWebRtcAudioTrackInitError: $errorMessage")
                reportError(errorMessage)
            }

            override fun onWebRtcAudioTrackStartError(
                errorCode: JavaAudioDeviceModule.AudioTrackStartErrorCode, errorMessage: String,
            ) {
                Logger.e("onWebRtcAudioTrackStartError: $errorCode. $errorMessage")
                reportError(errorMessage)
            }

            override fun onWebRtcAudioTrackError(errorMessage: String) {
                Logger.e("onWebRtcAudioTrackError: $errorMessage")
                reportError(errorMessage)
            }
        }

        return JavaAudioDeviceModule.builder(appContext).apply {
            if (peerConnectionParameters.audioOptions.stereo) {
                setUseStereoInput(true)
                setUseStereoOutput(true)
            }
            setUseHardwareAcousticEchoCanceler(peerConnectionParameters.audioOptions.hardwareAcousticEchoCanceler)
            setUseHardwareNoiseSuppressor(peerConnectionParameters.audioOptions.hardwareNoiseSuppressor)
            setAudioTrackErrorCallback(audioTrackErrorCallback)

        }.createAudioDeviceModule()
    }

    private fun createPeerConnectionInternal(
        stunServers: List<String>,
        turnServers: List<TurnServerDto>,
        peerConnectionReadyCallback: Runnable?,
        microphoneEnabledOnStart: Boolean,
        videoEnabledOnStart: Boolean,
    ) {
        val iceServers = getIceServers(stunServers, turnServers)
        if (factory == null || isError || iceServers.isEmpty()) {
            Logger.e("PeerConnection factory is not created")
            return
        }
        Logger.d("Create peer connection.")
        queuedRemoteCandidates = ArrayList()
        val rtcConfig = RTCConfiguration(iceServers)
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_ONCE
        // Use ECDSA encryption.
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA
        // Enable DTLS for normal calls and disable for loopback calls.
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

        peerConnection = factory?.createPeerConnection(rtcConfig, pcObserver)

        val init = DataChannel.Init()
        init.ordered = peerConnectionParameters.dataChannelParameters.ordered
        init.negotiated = peerConnectionParameters.dataChannelParameters.negotiated
        init.maxRetransmits = peerConnectionParameters.dataChannelParameters.maxRetransmits
        init.maxRetransmitTimeMs =
            peerConnectionParameters.dataChannelParameters.maxRetransmitTimeMs
        init.id = peerConnectionParameters.dataChannelParameters.id
        init.protocol = peerConnectionParameters.dataChannelParameters.protocol
        dataChannel = peerConnection?.createDataChannel("data", init)?.apply {
            // For pre-negotiated data channels PeerConnection.Observer.onDataChannel will NOT be called
            registerObserver(object : DataChannel.Observer {
                override fun onBufferedAmountChange(previousAmount: Long) {
                    Logger.d("Data channel buffered amount changed: ${dataChannel?.label()}: ${dataChannel?.state()}")
                }

                override fun onStateChange() {
                    Logger.d("Data channel state changed: ${dataChannel?.label()}: ${dataChannel?.state()}")
                }

                override fun onMessage(buffer: DataChannel.Buffer) {
                    if (buffer.binary) {
                        Logger.d("Received binary msg over $dataChannel")
                        return
                    }
                    val data = buffer.data
                    val bytes = ByteArray(data.capacity())
                    data[bytes]
                    val strData = String(bytes, StandardCharsets.UTF_8)
                    if (!strData.containsPing()) {
                        Logger.d("Got msg: $strData over $dataChannel")
                    }
                    dataChannelEvents.onMessageReceived(strData)
                }
            })
        }

        isInitiator = false

        // Set INFO libjingle logging.
        // NOTE: this _must_ happen while |factory| is alive!
        if (DEBUG) {
            Logging.enableLogToDebugOutput(Logging.Severity.LS_INFO)
        } else {
            Logging.enableLogToDebugOutput(Logging.Severity.LS_NONE)
        }


        peerConnection?.addTrack(
            createVideoTrack(videoCapturer, videoEnabledOnStart),
            mediaStreamLabels
        )

        if (videoEnabledOnStart) {
            setVideoMaxBitrate(maxBitrateKbps = peerConnectionParameters.videoOptions.videoMaxBitrate)
        }

        if (peerConnectionParameters.receiveVideo) {
            // We can add the renderers right away because we don't need to wait for an
            // answer to get the remote track.
            remoteVideoTrack = getRemoteVideoTrack()
            remoteVideoTrack?.setEnabled(renderRemoteVideo)
            remoteSinks?.forEach { remoteSink ->
                remoteVideoTrack?.addSink(remoteSink)
            }
        }


        peerConnection?.addTrack(createAudioTrack(microphoneEnabledOnStart), mediaStreamLabels)

        Logger.d("Peer connection created.")
        peerConnectionReadyCallback?.run()
    }

    @Suppress("DEPRECATION")
    private fun getIceServers(
        stunServers: List<String>,
        turnServers: List<TurnServerDto>,
    ): List<IceServer> = buildList {
        stunServers.forEach { add(IceServer(it, "", "")) }
        turnServers.forEach { turn ->
            turn.urls.forEach { url ->
                add(IceServer(url, turn.username, turn.password))
            }
        }
    }

    private fun closeInternal() {
        Logger.d("Closing peer connection.")
        statsTimer.cancel()
        dataChannel?.unregisterObserver()
        dataChannel?.dispose()
        dataChannel = null

        // RtcEventLog should stop before the peer connection is disposed.
        rtcEventLog?.stop()
        rtcEventLog = null

        peerConnection?.dispose()
        peerConnection = null

        audioSource?.dispose()
        audioSource = null

        Logger.d("Stopping capture.")
        if (videoCapturer != null) {
            try {
                videoCapturer?.stopCapture()
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
            videoCapturerStopped = true
            videoCapturer?.dispose()
            videoCapturer = null
        }

        Logger.d("Closing video source.")
        if (videoSource != null) {
            videoSource?.dispose()
            videoSource = null
        }

        if (surfaceTextureHelper != null) {
            surfaceTextureHelper?.dispose()
            surfaceTextureHelper = null
        }

        localRender = null
        remoteSinks = null

        Logger.d("Closing peer connection factory.")
        if (factory != null) {
            factory?.dispose()
            factory = null
        }
        iceGatheringTimer.cancel()

        try {
            rootEglBase.release()
        } catch (e: Exception) {
            Logger.d("rootEglBase double free")
        }

        PeerConnectionFactory.stopInternalTracingCapture()
        PeerConnectionFactory.shutdownInternalTracer()

        events.onPeerConnectionClosed()
        Logger.d("Closing peer connection done.")
    }

    private fun getStats() {
        if (peerConnection == null || isError) {
            return
        }
        peerConnection?.getStats { report -> events.onPeerConnectionStatsReady(report) }
    }

    fun enableStatsEvents(enable: Boolean, periodMs: Int) {
        if (enable) {
            try {
                statsTimer.schedule(object : TimerTask() {
                    override fun run() {
                        executor.execute { getStats() }
                    }
                }, 0, periodMs.toLong())
            } catch (e: Exception) {
                Logger.e("Can not schedule statistics timer $e")
            }
        } else {
            statsTimer.cancel()
        }
    }

    fun updatePeerConnectionParameter(
        videoOptions: VideoOptions = peerConnectionParameters.videoOptions,
        screenShareOptions: ScreenShareOptions = peerConnectionParameters.screenShareOptions,
    ) {
        executor.execute {
            with(peerConnectionParameters.videoOptions) {
                if (videoOptions.videoResolution != videoResolution || videoOptions.videoFps != videoFps) {
                    val resolution = videoOptions.videoResolution.ensureVideoResolutionBounds()
                    changeCaptureFormat(
                        resolution.width,
                        resolution.height,
                        videoOptions.videoFps.ensureFps()
                    )
                }

                if (videoOptions.videoMaxBitrate != videoMaxBitrate) {
                    setVideoMaxBitrate(videoOptions.videoMaxBitrate)
                }
            }

            with(peerConnectionParameters.screenShareOptions) {
                if (isScreencastActive() && screenShareOptions != this) {
                    val resolution =
                        screenShareOptions.screenShareResolution.ensureScreenShareResolutionBounds()
                    changeCaptureFormat(
                        resolution.width,
                        resolution.height,
                        screenShareOptions.screenShareFps.ensureFps()
                    )
                }
            }

            peerConnectionParameters = peerConnectionParameters.copy(
                videoOptions = videoOptions,
                screenShareOptions = screenShareOptions
            )
        }
    }

    fun setAudioEnabled(enable: Boolean) {
        executor.execute {
            enableAudio = enable
            localAudioTrack?.setEnabled(enableAudio)
        }
    }

    // TODO: remove
//    fun setVideoEnabled(enable: Boolean) {
//        executor.execute {
//            renderLocalVideo = enable
//            renderRemoteVideo = enable
//            localVideoTrack?.setEnabled(renderLocalVideo)
//            remoteVideoTrack?.setEnabled(renderRemoteVideo)
//        }
//    }

    fun setLocalVideoEnabled(enable: Boolean) {
        executor.execute {
            renderLocalVideo = enable
            localVideoTrack?.setEnabled(renderLocalVideo)
        }
    }

    fun createOffer() {
        executor.execute {
            if (peerConnection != null && !isError) {
                Logger.d("PC Create OFFER")
                isInitiator = true
                peerConnection?.createOffer(sdpObserver, sdpMediaConstraints)
            }
        }
    }

    fun createAnswer() {
        executor.execute {
            if (peerConnection != null && !isError) {
                Logger.d("PC create ANSWER")
                isInitiator = false
                peerConnection?.createAnswer(sdpObserver, sdpMediaConstraints)
            }
        }
    }

    fun addRemoteIceCandidate(candidate: IceCandidate) {
        executor.execute {
            if (peerConnection != null && !isError) {
                if (queuedRemoteCandidates != null) {
                    queuedRemoteCandidates?.add(candidate)
                } else {
                    peerConnection?.addIceCandidate(candidate)
                }
            }
        }
    }

    fun removeRemoteIceCandidates(candidates: Array<IceCandidate>?) {
        executor.execute {
            if (peerConnection == null || isError) {
                return@execute
            }
            // Drain the queued remote candidates if there is any so that
            // they are processed in the proper order.
            drainCandidates()
            peerConnection?.removeIceCandidates(candidates)
        }
    }

    fun setRemoteDescription(sdp: SessionDescription) {
        Logger.d("setRemoteDescription: ${sdp.type}")
        executor.execute {
            if (peerConnection == null || isError) {
                return@execute
            }
            val sdpDescription = setStartBitrate(
                AUDIO_CODEC_OPUS,
                isVideoCodec = false,
                sdp.description,
                peerConnectionParameters.audioOptions.audioMaxAverageBitrateKbps.ensureAudioBitRate()
            )

            Logger.d("Set remote SDP. type:${sdp.type}")
            val sdpRemote = SessionDescription(sdp.type, "${sdpDescription.trim()}\r\n")
            logSdp(sdpRemote)
            peerConnection?.setRemoteDescription(sdpObserver, sdpRemote)
        }
    }

    fun setVideoMaxBitrate(maxBitrateKbps: Int?) {
        executor.execute {
            if (peerConnection == null || localVideoSender == null || isError) return@execute

            Logger.d("Requested max video bitrate: $maxBitrateKbps")
            val parameters = localVideoSender?.parameters ?: return@execute

            if (parameters.encodings.isEmpty()) {
                Logger.w("RtpParameters are not ready.")
                return@execute
            }

            parameters.encodings.forEach { encoding ->
                encoding.maxBitrateBps = maxBitrateKbps?.ensureVideoBitRate()?.times(BPS_IN_KBPS)
            }

            if (localVideoSender?.setParameters(parameters) == false) {
                Logger.e("RtpSender.setParameters failed.")
            }
        }
    }

    fun scaleResolutionDownBy(factor: Double?) {
        executor.execute {
            if (peerConnection == null || localVideoSender == null || isError) return@execute


            Logger.d("Requested resolution scale down factor: $factor")
            val parameters = localVideoSender?.parameters ?: return@execute

            if (parameters.encodings.isEmpty()) {
                Logger.w("RtpParameters are not ready.")
                return@execute
            }

            parameters.encodings.forEach { encoding ->
                encoding.scaleResolutionDownBy = factor
            }

            if (localVideoSender?.setParameters(parameters) == false) {
                Logger.e("RtpSender.setParameters failed.")
            }
        }
    }

    private fun reportError(errorMessage: String) {
        Logger.e("PeerConnection error: $errorMessage")
        executor.execute {
            if (!isError) {
                events.onPeerConnectionError(errorMessage)
                isError = true
            }
        }
    }

    private fun createAudioTrack(microphoneEnabledOnStart: Boolean): AudioTrack? {
        audioSource = factory?.createAudioSource(audioConstraints)
        localAudioTrack = factory?.createAudioTrack(AUDIO_TRACK_ID, audioSource)
        enableAudio = microphoneEnabledOnStart
        localAudioTrack?.setEnabled(microphoneEnabledOnStart)
        return localAudioTrack
    }

    private fun createVideoTrack(
        capturer: VideoCapturer?,
        videoEnabledOnStart: Boolean,
    ): VideoTrack? {
        surfaceTextureHelper =
            SurfaceTextureHelper.create("CaptureThread", rootEglBase.eglBaseContext)
        videoSource = factory?.createVideoSource(capturer?.isScreencast ?: false)

        val (captureResolution, fps) = if (capturer?.isScreencast == true) {
            Pair(
                peerConnectionParameters.screenShareOptions.screenShareResolution.ensureScreenShareResolutionBounds(),
                peerConnectionParameters.screenShareOptions.screenShareFps.ensureFps()
            )

        } else {
            Pair(
                peerConnectionParameters.videoOptions.videoResolution.ensureVideoResolutionBounds(),
                peerConnectionParameters.videoOptions.videoFps.ensureFps()
            )
        }


        if (videoEnabledOnStart) {
            capturer?.initialize(surfaceTextureHelper, appContext, videoSource?.capturerObserver)

            changeCaptureFormat(
                captureResolution.width,
                captureResolution.height,
                fps
            )

            capturer?.startCapture(
                captureResolution.width,
                captureResolution.height,
                fps
            )
        }

        localVideoTrack = factory?.createVideoTrack(VIDEO_TRACK_ID, videoSource)
        renderLocalVideo = videoEnabledOnStart
        localVideoTrack?.setEnabled(videoEnabledOnStart)
        localVideoTrack?.addSink(localRender)
        return localVideoTrack
    }

    fun replaceVideoCapturer(
        capturer: VideoCapturer?,
        videoEnabledOnStart: Boolean,
    ) {
        executor.execute {
            try {
                (videoCapturer as? ScreenCapturerAndroid)?.mediaProjection?.stop()
                videoSource?.dispose()
                videoCapturer?.stopCapture()

                surfaceTextureHelper?.stopListening()
                surfaceTextureHelper?.dispose()
                surfaceTextureHelper = null
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
            videoCapturerStopped = true
            videoCapturer?.dispose()
            videoCapturer = capturer

            localVideoTrack?.removeSink(localRender)

            createVideoTrack(
                capturer,
                videoEnabledOnStart
            )?.let {
                videoCapturerStopped = false
            }

            localVideoSender?.setTrack(localVideoTrack, false)
        }
    }

    fun startVideoSource() {
        executor.execute {
            when {
                videoCapturer != null && !isScreencastActive() -> {
                    val (videoWidth, videoHeight) = peerConnectionParameters.videoOptions.videoResolution.ensureVideoResolutionBounds()
                    val fps = peerConnectionParameters.videoOptions.videoFps.ensureFps()

                    videoCapturer?.initialize(
                        surfaceTextureHelper,
                        appContext,
                        videoSource?.capturerObserver
                    )
                    changeCaptureFormat(
                        videoWidth,
                        videoHeight,
                        fps
                    )

                    videoCapturer?.startCapture(
                        videoWidth,
                        videoHeight,
                        fps
                    )
                    videoCapturerStopped = false
                }

                isScreencastActive() -> {
                    setLocalVideoTracksEnabled(true)
                }
            }
        }
    }

    fun stopVideoSource() {
        executor.execute {
            when {
                videoCapturer != null && !isScreencastActive() -> {
                    try {
                        videoCapturer?.stopCapture()
                    } catch (_: InterruptedException) {
                    }
                    videoCapturerStopped = true
                }

                isScreencastActive() -> {
                    setLocalVideoTracksEnabled(false)
                }
            }
        }
    }

    private fun setLocalVideoTracksEnabled(enabled: Boolean) {
        peerConnection?.senders?.forEach { sender ->
            val track = sender.track()
            if (track?.kind() == VIDEO_TRACK_TYPE) {
                track.setEnabled(enabled)
            }
        }
    }

    // Returns the remote VideoTrack, assuming there is only one.
    private fun getRemoteVideoTrack(): VideoTrack? {
        return peerConnection?.transceivers?.firstNotNullOfOrNull { it.receiver.track() as? VideoTrack }
    }

    fun setAudioTracksEnabled(enable: Boolean) {
        peerConnection?.receivers?.forEach { rtpReceiver ->
            val track = rtpReceiver.track()
            if (track?.kind() == AUDIO_TRACK_TYPE) {
                track.setEnabled(enable)
            }
        }
    }

    private fun drainCandidates() {
        if (queuedRemoteCandidates != null) {
            Logger.d("Add ${queuedRemoteCandidates?.size} remote candidates")
            queuedRemoteCandidates?.forEach { candidate ->
                peerConnection?.addIceCandidate(candidate)
            }
            queuedRemoteCandidates = null
        }
    }

    private fun switchCameraInternal(cameraId: String? = null) {
        if (videoCapturer is CameraVideoCapturer) {
            if (!isVideoCallEnabled || isError) {
                Logger.e("Failed to switch camera. Video: $isVideoCallEnabled. Error : $isError")
                return  // No video is sent or only one camera is available or error happened.
            }
            Logger.d("Switch camera")
            val cameraVideoCapturer = videoCapturer as CameraVideoCapturer

            if (cameraId == null) {
                cameraVideoCapturer.switchCamera(switchEventsHandler)
            } else {
                cameraVideoCapturer.switchCamera(switchEventsHandler, cameraId)
            }
        } else {
            Logger.d("Will not switch camera, video capturer is not a camera")
        }
    }

    fun switchCamera() {
        executor.execute { switchCameraInternal() }
    }

    fun switchCameraTo(cameraId: String) {
        executor.execute { switchCameraInternal(cameraId) }
    }

    fun isScreencastActive(): Boolean {
        return videoCapturer?.isScreencast ?: false
    }

    fun changeCaptureFormat(width: Int, height: Int, framerate: Int) {
        executor.execute { changeCaptureFormatInternal(width, height, framerate) }
    }

    private fun changeCaptureFormatInternal(width: Int, height: Int, framerate: Int) {
        if (!isVideoCallEnabled || isError || videoCapturer == null) {
            Logger.e("Failed to change capture format. Video: $isVideoCallEnabled. Error : $isError")
            return
        }
        Logger.d("changeCaptureFormat: ${width}x$height@$framerate")
        videoSource?.adaptOutputFormat(width, height, framerate)
    }

    fun sendDataChannelMessage(message: String): Boolean {
        return dataChannel?.send(
            DataChannel.Buffer(
                ByteBuffer.wrap(message.toByteArray()),
                false
            )
        ) ?: false

    }

    /**
     * Peer connection events.
     */
    abstract class PeerConnectionEvents {
        /**
         * Callback fired once local SDP is created and set.
         */
        open fun onLocalDescription(sdp: SessionDescription?) {}

        /**
         * Callback fired once local Ice candidate is generated.
         */
        open fun onIceCandidate(candidate: IceCandidate) {}

        /**
         * Callback fired once local ICE candidates are removed.
         */
        open fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {}

        /**
         * Callback fired once connection is established (IceConnectionState is
         * CONNECTED).
         */
        open fun onIceConnected() {}

        /**
         * Callback fired once connection is closed (IceConnectionState is
         * DISCONNECTED).
         */
        open fun onIceDisconnected() {}

        /**
         * Callback fired once DTLS connection is established (PeerConnectionState
         * is CONNECTED).
         */
        open fun onConnected() {}

        /**
         * Callback fired once DTLS connection is disconnected (PeerConnectionState
         * is DISCONNECTED).
         */
        open fun onDisconnected() {}

        /**
         * Callback fired once peer connection is closed.
         */
        open fun onPeerConnectionClosed() {}

        /**
         * Callback fired once peer connection statistics is ready.
         */
        open fun onPeerConnectionStatsReady(report: RTCStatsReport) {}

        /**
         * Callback fired once peer connection error happened.
         */
        open fun onPeerConnectionError(description: String) {}
        open fun onIceGatheringComplete(sdpToBeSent: String) {}
        open fun onCameraSwitchDone(isFrontCamera: Boolean) {}
        open fun onCameraSwitchError(error: String) {}
    }

    interface DataChannelEvents {
        fun onMessageReceived(message: String)
    }

//    /**
//     * Peer connection parameters.
//     */
//    class DataChannelParameters(
//        val ordered: Boolean, val maxRetransmitTimeMs: Int, val maxRetransmits: Int,
//        val protocol: String, val negotiated: Boolean, val id: Int,
//    )
//
//    /**
//     * Peer connection parameters.
//     */
//    class PeerConnectionParameters(
//        var videoCallEnabled: Boolean,
//        val videoWidth: Int,
//        val videoHeight: Int,
//        val videoFps: Int,
//        val videoMaxBitrate: Int,
//        val videoCodecHwAcceleration: Boolean,
//        val audioStartBitrate: Int,
//        val noAudioProcessing: Boolean,
//        val disableBuiltInAEC: Boolean,
//        val disableBuiltInNS: Boolean,
//        val dataChannelParameters: DataChannelParameters,
//    ) {
//        constructor(audioOnly: Boolean, widescreen: Boolean) : this(
//            videoCallEnabled = !audioOnly,
//            videoWidth = 640,
//            videoHeight = if (widescreen) 360 else 480,
//            videoFps = 30,
//            videoMaxBitrate = 2000,
//            videoCodecHwAcceleration = true,
//            audioStartBitrate = 64,
//            noAudioProcessing = false,
//            disableBuiltInAEC = false,
//            disableBuiltInNS = false,
//            dataChannelParameters = DataChannelParameters(
//                ordered = true,
//                maxRetransmitTimeMs = -1,
//                maxRetransmits = -1,
//                protocol = "",
//                negotiated = true,
//                id = 0
//            )
//        )
//    }

    // Implementation detail: observe ICE & stream changes and react accordingly.
    private inner class PCObserver : PeerConnection.Observer {
        override fun onIceCandidate(candidate: IceCandidate) {
            Logger.d("IceGatheringState: onIceCandidate $candidate")
            executor.execute { events.onIceCandidate(candidate) }
        }

        override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {
            executor.execute { events.onIceCandidatesRemoved(candidates) }
        }

        override fun onSignalingChange(newState: SignalingState) {
            Logger.d("SignalingState: $newState")
        }

        override fun onIceConnectionChange(newState: IceConnectionState) {
            executor.execute {
                when (newState) {
                    IceConnectionState.CONNECTED -> {
                        events.onIceConnected()
                    }

                    IceConnectionState.DISCONNECTED -> {
                        events.onIceDisconnected()
                    }

                    IceConnectionState.FAILED -> {
                        reportError("ICE connection failed.")
                    }

                    else -> {
                        // NOOP
                    }
                }
            }
        }

        override fun onIceGatheringChange(newState: IceGatheringState) {
            Logger.d("IceGatheringState: $newState")
            val onComplete = {
                if (localSdp != null
                    && peerConnection != null
                    && iceGatheringSend.compareAndSet(false, true)
                ) {
                    executor.execute {
                        iceGatheringSend.getAndSet(true)
                        peerConnection?.localDescription?.description?.let {
                            events.onIceGatheringComplete(
                                it
                            )
                        }
                    }
                }
            }

            when (newState) {
                IceGatheringState.GATHERING -> {
                    iceGatheringTimer.schedule(
                        object : TimerTask() {
                            override fun run() {
                                onComplete()
                            }
                        }, ICE_GATHERING_TIMEOUT.toLong()
                    )
                }

                IceGatheringState.COMPLETE -> {
                    iceGatheringTimer.cancel()
                    onComplete()
                }

                else -> {
                    // NOOP
                }
            }
        }

        override fun onConnectionChange(newState: PeerConnectionState) {
            Logger.d("onConnectionChange: $newState")
            executor.execute {
                when (newState) {
                    PeerConnectionState.CONNECTED -> {
                        events.onConnected()
                    }

                    PeerConnectionState.DISCONNECTED -> {
                        events.onDisconnected()
                    }

                    PeerConnectionState.FAILED -> {
                        reportError("DTLS connection failed.")
                    }

                    else -> {
                        // NOOP
                    }
                }
            }
        }


        override fun onIceConnectionReceivingChange(receiving: Boolean) {
            Logger.d("IceConnectionReceiving changed to $receiving")
        }

        override fun onAddStream(stream: MediaStream) {
            // NOOP
        }

        override fun onRemoveStream(stream: MediaStream) {
            // NOOP
        }

        // NOT IN USE
        // For pre-negotiated data channels PeerConnection.Observer.onDataChannel
        // will NOT be called
        override fun onDataChannel(dc: DataChannel) {
            // NOOP
        }

        override fun onRenegotiationNeeded() {
            // No need to do anything; AppRTC follows a pre-agreed-upon
            // signaling/negotiation protocol.
        }
    }

    // Implementation detail: handle offer creation/signaling and answer setting,
    // as well as adding remote ICE candidates once the answer SDP is set.
    private inner class SDPObserver : SdpObserver {
        override fun onCreateSuccess(origSdp: SessionDescription) {
            if (localSdp != null) {
                reportError("Multiple SDP create.")
                return
            }
            var sdpDescription = origSdp.description

            if (peerConnectionParameters.audioOptions.stereo) {
                sdpDescription = setCodecStereo(sdpDescription)
            }

            val sdp = SessionDescription(origSdp.type, "${sdpDescription.trim()}\r\n")
            localSdp = sdp
            executor.execute {
                if (peerConnection != null && !isError) {
                    Logger.d("Set local SDP from ${sdp.type}")
                    logSdp(sdp)
                    peerConnection?.setLocalDescription(sdpObserver, sdp)
                }
            }
        }

        override fun onSetSuccess() {
            executor.execute {
                Logger.d("onSetSuccess start")
                if (peerConnection == null || isError) {
                    return@execute
                }
                if (isInitiator) {
                    // For offering peer connection we first create offer and set
                    // local SDP, then after receiving answer set remote SDP.
                    if (peerConnection?.remoteDescription == null) {
                        // We've just set our local SDP so time to send it.
                        Logger.d("Local SDP set successfully")
                        events.onLocalDescription(localSdp)
                    } else {
                        // We've just set remote description, so drain remote
                        // and send local ICE candidates.
                        Logger.d("Remote SDP set successfully")
                        drainCandidates()
                    }
                } else {
                    // For answering peer connection we set remote SDP and then
                    // create answer and set local SDP.
                    if (peerConnection?.localDescription != null) {
                        // We've just set our local SDP so time to send it, drain
                        // remote and send local ICE candidates.
                        Logger.d("Local SDP set successfully")
                        events.onLocalDescription(localSdp)
                        drainCandidates()
                    } else {
                        // We've just set remote SDP - do nothing for now -
                        // answer will be created soon.
                        Logger.d("Remote SDP set successfully")
                    }
                }
            }
        }

        override fun onCreateFailure(error: String) {
            reportError("createSDP error: $error")
        }

        override fun onSetFailure(error: String) {
            reportError("setSDP error: $error")
        }
    }

    private fun String.containsPing(): Boolean {
        return contains(TYPE_PING)
    }

    private fun VideoResolution.ensureVideoResolutionBounds(): VideoResolution {
        val ensureVideoResolution =
            { maxResolution: VideoResolution, fallbackResolution: VideoResolution ->
                when {
                    width <= 0 || height <= 0 -> fallbackResolution
                    width > maxResolution.width || height > maxResolution.height -> maxResolution
                    else -> this
                }
            }

        return if (peerConnectionParameters.isWidescreen) {
            ensureVideoResolution(
                VIDEO_WIDESCREEN_MAX_RESOLUTION,
                Resolution16by9.DEFAULT.resolution
            )
        } else {
            ensureVideoResolution(
                VIDEO_MAX_RESOLUTION,
                Resolution4by3.DEFAULT.resolution
            )
        }
    }

    private fun VideoResolution.ensureScreenShareResolutionBounds(): VideoResolution {

        val ensureScreenShareResolution =
            { fallbackResolution: VideoResolution ->
                when {
                    width <= 0 || height <= 0 -> fallbackResolution
                    width > fallbackResolution.width || height > fallbackResolution.height -> fallbackResolution
                    else -> this
                }
            }

        return ensureScreenShareResolution(
            SCREEN_SHARE_MAX_RESOLUTION
        )
    }

    private fun Int.ensureFps(): Int = if (this in 0..VIDEO_MAX_FPS) this else VIDEO_MAX_FPS

    private fun Int.ensureVideoBitRate(): Int =
        if (this in 0..VIDEO_MAX_BITRATE) this else VIDEO_MAX_BITRATE

    private fun Int.ensureAudioBitRate(): Int =
        if (this in AUDIO_BITRATE_RANGE) this else AUDIO_BITRATE_DEFAULT


    companion object {
        const val VIDEO_TRACK_ID = "ARDAMSv0"
        const val AUDIO_TRACK_ID = "ARDAMSa0"
        const val VIDEO_TRACK_TYPE = "video"
        const val AUDIO_TRACK_TYPE = "audio"
        private const val AUDIO_CODEC_OPUS = "opus"
        private const val VIDEO_CODEC_PARAM_START_BITRATE = "x-google-start-bitrate"
        private const val AUDIO_CODEC_PARAM_BITRATE = "maxaveragebitrate"
        private const val AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation"
        private const val AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl"
        private const val AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter"
        private const val AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression"
        private const val AUDIO_TYPING_NOISE_DETECTION_CONSTRAINT = "googTypingNoiseDetection"
        private const val RECEIVE_AUDIO_CONSTRAINT = "OfferToReceiveAudio"
        private const val RECEIVE_VIDEO_CONSTRAINT = "OfferToReceiveVideo"

        private const val BPS_IN_KBPS = 1000
        private val mediaStreamLabels = listOf("ALPACA_ID")

        private const val TYPE_PING = "\"type\":\"ping\""

        private const val AUDIO_BITRATE_DEFAULT = 64
        private val AUDIO_BITRATE_RANGE = Range(6, 510)

        private const val VIDEO_MAX_FPS = 30
        private val VIDEO_WIDESCREEN_MAX_RESOLUTION = VideoResolution(1280, 720)
        private val VIDEO_MAX_RESOLUTION = VideoResolution(1280, 960)
        private val SCREEN_SHARE_MAX_RESOLUTION = VideoResolution(1920, 1080)

        private val VIDEO_MAX_BITRATE = 2000

        // Executor thread is started once in private ctor and is used for all
        // peer connection API calls to ensure new peer connection factory is
        // created on the same thread as previously destroyed factory.
        private val executor = Executors.newSingleThreadExecutor()
        private const val ICE_GATHERING_TIMEOUT = 1500

        private fun setStartBitrate(
            codec: String, isVideoCodec: Boolean, sdpDescription: String, bitrateKbps: Int,
        ): String {
            val lines = sdpDescription.split("\r\n").toMutableList()
            val codecPattern = "^a=rtpmap:(\\d+) $codec(/\\d+)+\$".toRegex()
            val codecRtpMap = lines.indexOfFirst { codecPattern.matches(it) }
                .takeIf { it >= 0 }
                ?.let { codecPattern.matchEntire(lines[it])?.groupValues?.get(1) }
                ?: return sdpDescription.also { Logger.w("No rtpmap for $codec codec") }

            Logger.d("Found $codec rtpmap $codecRtpMap")

            val fmtpPattern = "^a=fmtp:$codecRtpMap \\w+=\\d+.*\$".toRegex()
            val fmtpIndex = lines.indexOfFirst { fmtpPattern.matches(it) }

            if (fmtpIndex >= 0) {
                lines[fmtpIndex] += if (isVideoCodec) {
                    "; $VIDEO_CODEC_PARAM_START_BITRATE=$bitrateKbps"
                } else {
                    "; $AUDIO_CODEC_PARAM_BITRATE=${bitrateKbps * 1000}"
                }
                Logger.d("Updated remote SDP line: ${lines[fmtpIndex]}")
            } else {
                val bitrateSet = if (isVideoCodec) {
                    "a=fmtp:$codecRtpMap $VIDEO_CODEC_PARAM_START_BITRATE=$bitrateKbps"
                } else {
                    "a=fmtp:$codecRtpMap $AUDIO_CODEC_PARAM_BITRATE=${bitrateKbps * 1000}"
                }
                Logger.d("Added remote SDP line: $bitrateSet")
                lines.add(codecRtpMap.toInt() + 1, bitrateSet)
            }

            return lines.joinToString("\r\n")
        }

        private fun setCodecStereo(
            sdpDescription: String,
            codec: String = AUDIO_CODEC_OPUS,
            on: Boolean = true,
        ): String {
            val lines = sdpDescription.split("\r\n").toTypedArray()
            val codecPattern = "^a=rtpmap:(\\d+) $codec(/\\d+)+\$".toRegex()
            val id = lines.firstNotNullOfOrNull { line ->
                codecPattern.matchEntire(line)?.groupValues?.get(1)
            }

            id?.let {
                val index = lines.indexOfFirst { it.startsWith("a=fmtp:$id") }
                if (index >= 0) {
                    val options = lines[index].substringAfter("a=fmtp:$id ", "")
                        .split(";")
                        .filterNot { it.contains("stereo") }
                        .toMutableList()
                    options.add("stereo=${if (on) 1 else 0}")
                    options.add("sprop-stereo=${if (on) 1 else 0}")
                    lines[index] = "a=fmtp:$id ${options.joinToString(";")}"
                }
            }

            return lines.joinToString("\r\n")
        }
    }
}
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
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.WindowMetrics
import com.eyeson.sdk.BuildConfig.DEBUG
import com.eyeson.sdk.di.NetworkModule
import com.eyeson.sdk.model.api.TurnServerDto
import com.eyeson.sdk.model.datachannel.outgoing.MuteVideoDto
import com.eyeson.sdk.model.datachannel.outgoing.fromLocal
import com.eyeson.sdk.model.local.datachannel.MuteVideo
import com.eyeson.sdk.utils.Logger
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
import org.webrtc.RtpReceiver
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
import org.webrtc.audio.JavaAudioDeviceModule.AudioRecordErrorCallback
import org.webrtc.audio.JavaAudioDeviceModule.AudioTrackErrorCallback
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern


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
    private val peerConnectionParameters: PeerConnectionParameters,
    private val events: PeerConnectionEvents,
    private val dataChannelEvents: DataChannelEvents
) {
    private val pcObserver = PCObserver()
    private val sdpObserver = SDPObserver()
    private val statsTimer = Timer()
    private val iceGatheringTimer = Timer()
    private val iceGatheringSend = AtomicBoolean(false)
    private val moshi = NetworkModule.moshi

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
    private var preferIsac = false
    private var videoCapturerStopped = false
    private var isError = false
    private var localRender: VideoSink? = null
    private var remoteSinks: List<VideoSink>? = null
    private var videoWidth = 0
    private var videoHeight = 0
    private var videoFps = 0
    private var audioConstraints: MediaConstraints? = null
    private var sdpMediaConstraints: MediaConstraints? = null

    // Queued remote ICE candidates are consumed only after both local and
    // remote descriptions are set. Similarly local ICE candidates are sent to
    // remote peer after both local and remote description are set.
    private var queuedRemoteCandidates: MutableList<IceCandidate>? = null
    private var isInitiator = false
    var localSdp // either offer or answer SDP
            : SessionDescription? = null
        private set

    private var videoCapturer: VideoCapturer? = null
    private val muteVideoOnStart = AtomicBoolean(false)

    // enableVideo is set to true if video should be rendered and sent.
    var renderVideo = true
        private set
    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var localVideoSender: RtpSender? = null

    // enableAudio is set to true if audioOnly should be sent.
    var enableAudio = true
        private set
    private var localAudioTrack: AudioTrack? = null
    private var dataChannel: DataChannel? = null

    // Enable RtcEventLog.
    private var rtcEventLog: RtcEventLog? = null


    /**
     * Create a PeerConnectionClient with the specified parameters. PeerConnectionClient takes
     * ownership of |eglBase|.
     */
    init {
        Logger.d("Preferred video codec: ${getSdpVideoCodecName(peerConnectionParameters)}")
        val fieldTrials = getFieldTrials(peerConnectionParameters)
        executor.execute {
            Logger.d(
                "Initialize WebRTC. Field trials: $fieldTrials " +
                        "Enable video HW acceleration: ${peerConnectionParameters.videoCodecHwAcceleration}"
            )
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(appContext)
                    .setFieldTrials(fieldTrials)
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
        peerConnectionReadyCallback: Runnable?
    ) {
        if (peerConnectionParameters.videoCallEnabled && videoCapturer == null) {
            Logger.w("Video call enabled but no video capturer provided.")
        }
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
        peerConnectionReadyCallback: Runnable?
    ) {
        this.localRender = localRender
        this.remoteSinks = remoteSinks
        this.videoCapturer = videoCapturer
        muteVideoOnStart.set(!videoEnabledOnStart)
        executor.execute {
            try {
                createMediaConstraintsInternal()
                createPeerConnectionInternal(
                    stunServers,
                    turnServers,
                    peerConnectionReadyCallback,
                    microphoneEnabledOnStart,
                    videoEnabledOnStart
                )
                maybeCreateAndStartRtcEventLog()
            } catch (e: Exception) {
                reportError("Failed to create peer connection: ${e.message}")
                throw e
            }
        }
    }

    fun close() {
        executor.execute { closeInternal() }
    }

    val isVideoCallEnabled: Boolean
        get() = peerConnectionParameters.videoCallEnabled && videoCapturer != null

    @Suppress("DEPRECATION")
    private fun createPeerConnectionFactoryInternal(options: PeerConnectionFactory.Options?) {
        isError = false
        if (peerConnectionParameters.tracing) {
            PeerConnectionFactory.startInternalTracingCapture(
                "${Environment.getExternalStorageDirectory().absolutePath}${File.separator}webrtc-trace.txt"
            )
        }

        // Check if ISAC is used by default.
        preferIsac = (peerConnectionParameters.audioCodec != null
                && peerConnectionParameters.audioCodec == AUDIO_CODEC_ISAC)

        val adm = createJavaAudioDevice()

        // Create peer connection factory.
        if (options != null) {
            Logger.d("Factory networkIgnoreMask option: ${options.networkIgnoreMask}")
        }
        val enableH264HighProfile = VIDEO_CODEC_H264_HIGH == peerConnectionParameters.videoCodec
        val encoderFactory: VideoEncoderFactory
        val decoderFactory: VideoDecoderFactory
        if (peerConnectionParameters.videoCodecHwAcceleration) {
            encoderFactory = DefaultVideoEncoderFactory(
                rootEglBase.eglBaseContext, true /* enableIntelVp8Encoder */, enableH264HighProfile
            )
            decoderFactory = DefaultVideoDecoderFactory(rootEglBase.eglBaseContext)
        } else {
            encoderFactory = SoftwareVideoEncoderFactory()
            decoderFactory = SoftwareVideoDecoderFactory()
        }
        factory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setAudioDeviceModule(adm)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
        Logger.d("Peer connection factory created.")
    }

    private fun createJavaAudioDevice(): AudioDeviceModule {
        // Enable/disable OpenSL ES playback.
        if (!peerConnectionParameters.useOpenSLES) {
            Logger.w("External OpenSLES ADM not implemented yet.")
        }

        // Set audioOnly record error callbacks.
        val audioRecordErrorCallback: AudioRecordErrorCallback = object : AudioRecordErrorCallback {
            override fun onWebRtcAudioRecordInitError(errorMessage: String) {
                Logger.e("onWebRtcAudioRecordInitError: $errorMessage")
                reportError(errorMessage)
            }

            override fun onWebRtcAudioRecordStartError(
                errorCode: JavaAudioDeviceModule.AudioRecordStartErrorCode, errorMessage: String
            ) {
                Logger.e("onWebRtcAudioRecordStartError: $errorCode. $errorMessage")
                reportError(errorMessage)
            }

            override fun onWebRtcAudioRecordError(errorMessage: String) {
                Logger.e("onWebRtcAudioRecordError: $errorMessage")
                reportError(errorMessage)
            }
        }
        val audioTrackErrorCallback: AudioTrackErrorCallback = object : AudioTrackErrorCallback {
            override fun onWebRtcAudioTrackInitError(errorMessage: String) {
                Logger.e("onWebRtcAudioTrackInitError: $errorMessage")
                reportError(errorMessage)
            }

            override fun onWebRtcAudioTrackStartError(
                errorCode: JavaAudioDeviceModule.AudioTrackStartErrorCode, errorMessage: String
            ) {
                Logger.e("onWebRtcAudioTrackStartError: $errorCode. $errorMessage")
                reportError(errorMessage)
            }

            override fun onWebRtcAudioTrackError(errorMessage: String) {
                Logger.e("onWebRtcAudioTrackError: $errorMessage")
                reportError(errorMessage)
            }
        }
        return JavaAudioDeviceModule.builder(appContext)
            .setUseHardwareAcousticEchoCanceler(!peerConnectionParameters.disableBuiltInAEC)
            .setUseHardwareNoiseSuppressor(!peerConnectionParameters.disableBuiltInNS)
            .setAudioRecordErrorCallback(audioRecordErrorCallback)
            .setAudioTrackErrorCallback(audioTrackErrorCallback)
            .createAudioDeviceModule()
    }

    private fun createMediaConstraintsInternal() {
        // Create video constraints if video call is enabled.
        if (isVideoCallEnabled) {
            videoWidth = peerConnectionParameters.videoWidth
            videoHeight = peerConnectionParameters.videoHeight
            videoFps = peerConnectionParameters.videoFps

            // If video resolution is not specified, default to HD.
            if (videoWidth == 0 || videoHeight == 0) {
                videoWidth = HD_VIDEO_WIDTH
                videoHeight = HD_VIDEO_HEIGHT
            }

            // If fps is not specified, default to 30.
            if (videoFps == 0) {
                videoFps = 30
            }
            Logging.d(TAG, "Capturing format: ${videoWidth}x$videoHeight@$videoFps")
        }

        // Create audioOnly constraints.
        audioConstraints = MediaConstraints().apply {
            // added for audioOnly performance measurements
            if (peerConnectionParameters.noAudioProcessing) {
                Logger.d("Disabling audioOnly processing")
                mandatory.add(
                    MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT, "false")
                )
                mandatory.add(
                    MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "false")
                )
                mandatory.add(
                    MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "false")
                )
                mandatory.add(
                    MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "false")
                )
            }
        }

        // Create SDP constraints.
        sdpMediaConstraints = MediaConstraints().apply {
            mandatory.add(
                MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true")
            )
            mandatory.add(
                MediaConstraints.KeyValuePair(
                    "OfferToReceiveVideo", java.lang.Boolean.toString(isVideoCallEnabled)
                )
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun createPeerConnectionInternal(
        stunServers: List<String>,
        turnServers: List<TurnServerDto>,
        peerConnectionReadyCallback: Runnable?,
        microphoneEnabledOnStart: Boolean,
        videoEnabledOnStart: Boolean
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
            // For pre-negotiated data channels PeerConnection.Observer.onDataChannel
            // will NOT be called
            registerObserver(object : DataChannel.Observer {
                override fun onBufferedAmountChange(previousAmount: Long) {
                    Logger.d("Data channel buffered amount changed: ${dataChannel?.label()}: ${dataChannel?.state()}")
                }

                override fun onStateChange() {
                    Logger.d("Data channel state changed: ${dataChannel?.label()}: ${dataChannel?.state()}")
                    if (dataChannel?.state() == DataChannel.State.OPEN && muteVideoOnStart.get()) {
                        muteVideoOnStart.set(false)
                        val chatOutgoing = MuteVideo(muted = true, cid = "")
                        val adapter = moshi.adapter(MuteVideoDto::class.java)
                        Logger.d("setLocalVideoEnabled ${adapter.toJson(chatOutgoing.fromLocal())}")
                        sendDataChannelMessage(adapter.toJson(chatOutgoing.fromLocal()))

                        setLocalVideoEnabled(false)
                    }
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

        if (isVideoCallEnabled) {
            peerConnection?.addTrack(
                createVideoTrack(videoCapturer, videoEnabledOnStart),
                mediaStreamLabels
            )
            // We can add the renderers right away because we don't need to wait for an
            // answer to get the remote track.
            remoteVideoTrack = getRemoteVideoTrack()
            remoteVideoTrack?.setEnabled(renderVideo)
            remoteSinks?.forEach { remoteSink ->
                remoteVideoTrack?.addSink(remoteSink)
            }
        }
        peerConnection?.addTrack(createAudioTrack(microphoneEnabledOnStart), mediaStreamLabels)
        if (isVideoCallEnabled) {
            findVideoSender()
        }
        if (peerConnectionParameters.aecDump) {
            try {
                val aecDumpFileDescriptor = ParcelFileDescriptor.open(
                    File(
                        "${Environment.getExternalStorageDirectory().path}${File.separator}Download/audio.aecdump"
                    ),
                    ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE
                            or ParcelFileDescriptor.MODE_TRUNCATE
                )
                factory?.startAecDump(aecDumpFileDescriptor.detachFd(), -1)
            } catch (e: IOException) {
                Logger.e("Can not open aecdump file: $e")
            }
        }

        Logger.d("Peer connection created.")
        peerConnectionReadyCallback?.run()
    }

    @Suppress("DEPRECATION")
    private fun getIceServers(
        stunServers: List<String>,
        turnServers: List<TurnServerDto>
    ): List<IceServer> {
        val iceServers: MutableList<IceServer> = ArrayList()
        try {
            for (stun in stunServers) {
                val stunServer = IceServer(stun, "", "")
                iceServers.add(stunServer)
            }
            for (turn in turnServers) {
                for (url in turn.urls) {
                    val turnServer = IceServer(url, turn.username, turn.password)
                    iceServers.add(turnServer)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return iceServers
    }

    private fun createRtcEventLogOutputFile(): File {
        val dateFormat: DateFormat = SimpleDateFormat("yyyyMMdd_hhmm_ss", Locale.getDefault())
        val date = Date()
        val outputFileName = "event_log_${dateFormat.format(date)}.log"
        return File(
            appContext.getDir(RTCEVENTLOG_OUTPUT_DIR_NAME, Context.MODE_PRIVATE),
            outputFileName
        )
    }

    private fun maybeCreateAndStartRtcEventLog() {
        if (peerConnection == null) {
            return
        }
        if (!peerConnectionParameters.enableRtcEventLog) {
            Logger.d("RtcEventLog is disabled.")
            return
        }
        rtcEventLog = RtcEventLog(peerConnection)
        rtcEventLog?.start(createRtcEventLogOutputFile())
    }

    private fun closeInternal() {
        if (factory != null && peerConnectionParameters.aecDump) {
            factory?.stopAecDump()
        }
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

        Logger.d("Closing audioOnly source.")
        audioSource?.dispose()
        audioSource = null

        Logger.d("Stopping capture.")
        try {
            videoCapturer?.stopCapture()
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
        videoCapturerStopped = true
        videoCapturer?.dispose()
        videoCapturer = null

        Logger.d("Closing video source.")
        videoSource?.dispose()
        videoSource = null

        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null

        localRender = null
        remoteSinks = null

        Logger.d("Closing peer connection factory.")
        factory?.dispose()
        factory = null
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

    fun setAudioEnabled(enable: Boolean) {
        executor.execute {
            enableAudio = enable
            localAudioTrack?.setEnabled(enableAudio)
        }
    }

    fun setVideoEnabled(enable: Boolean) {
        executor.execute {
            renderVideo = enable
            localVideoTrack?.setEnabled(renderVideo)
            remoteVideoTrack?.setEnabled(renderVideo)
        }
    }

    fun setLocalVideoEnabled(enable: Boolean) {
        executor.execute {
            renderVideo = enable
            localVideoTrack?.setEnabled(renderVideo)
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
            var sdpDescription = sdp.description
            if (preferIsac) {
                sdpDescription = preferCodec(sdpDescription, AUDIO_CODEC_ISAC, true)
            }
            if (isVideoCallEnabled) {
                sdpDescription = preferCodec(
                    sdpDescription,
                    getSdpVideoCodecName(peerConnectionParameters),
                    false
                )
            }
            if (peerConnectionParameters.audioStartBitrate > 0) {
                sdpDescription = setStartBitrate(
                    AUDIO_CODEC_OPUS,
                    false,
                    sdpDescription,
                    peerConnectionParameters.audioStartBitrate
                )
            }
            Logger.d("Set remote SDP. type:${sdp.type}")
            val sdpRemote = SessionDescription(sdp.type, "${sdpDescription.trim()}\r\n")
            peerConnection?.setRemoteDescription(sdpObserver, sdpRemote)
        }
    }

    fun stopVideoSource() {
        executor.execute {
            if (videoCapturer != null && !videoCapturerStopped) {
                Logger.d("Stop video source.")
                try {
                    videoCapturer?.stopCapture()
                } catch (e: InterruptedException) {
                }
                videoCapturerStopped = true
            }
        }
    }

    fun startVideoSource() {
        executor.execute {
            if (videoCapturer != null && videoCapturerStopped) {
                Logger.d("Restart video source.")
                videoCapturer?.startCapture(videoWidth, videoHeight, videoFps)
                videoCapturerStopped = false
            }
        }
    }

    fun setVideoMaxBitrate(maxBitrateKbps: Int?) {
        executor.execute {
            if (peerConnection == null || localVideoSender == null || isError) {
                return@execute
            }
            Logger.d("Requested max video bitrate: $maxBitrateKbps")
            if (localVideoSender == null) {
                Logger.w("Sender is not ready.")
                return@execute
            }
            val parameters = localVideoSender?.parameters ?: return@execute
            if (parameters.encodings.size == 0) {
                Logger.w("RtpParameters are not ready.")
                return@execute
            }
            for (encoding in parameters.encodings) {
                // Null value means no limit.
                encoding.maxBitrateBps =
                    if (maxBitrateKbps == null) null else maxBitrateKbps * BPS_IN_KBPS
            }
            if (localVideoSender?.setParameters(parameters) == false) {
                Logger.e("RtpSender.setParameters failed.")
            }
            Logger.d("Configured max video bitrate to: $maxBitrateKbps")
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
        localAudioTrack?.setEnabled(microphoneEnabledOnStart)
        return localAudioTrack
    }

    private fun createVideoTrack(
        capturer: VideoCapturer?,
        videoEnabledOnStart: Boolean,
        customVideoWidth: Int? = null,
        customVideoHeight: Int? = null,
        customFps: Int? = null
    ): VideoTrack? {
        surfaceTextureHelper =
            SurfaceTextureHelper.create("CaptureThread", rootEglBase.eglBaseContext)
        videoSource = factory?.createVideoSource(capturer?.isScreencast ?: false)
        capturer?.initialize(surfaceTextureHelper, appContext, videoSource?.capturerObserver)

        val captureResolution = when {
            customVideoWidth != null && customVideoHeight != null -> {
                Pair(customVideoWidth, customVideoHeight)
            }
            capturer?.isScreencast == true -> {
                val windowManager = appContext.getSystemService(WindowManager::class.java)

                @Suppress("DEPRECATION")
                var resolution = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val windowMetrics: WindowMetrics = windowManager.maximumWindowMetrics
                    Pair(windowMetrics.bounds.width(), windowMetrics.bounds.height())
                } else {
                    val metrics = DisplayMetrics()
                    windowManager.defaultDisplay.getRealMetrics(metrics)
                    Pair(metrics.widthPixels, metrics.heightPixels)
                }

                if (resolution.first < resolution.second) {
                    resolution = Pair(resolution.second, resolution.first)
                }
                resolution
            }
            else -> {
                Pair(videoWidth, videoHeight)
            }
        }

        capturer?.startCapture(
            captureResolution.first,
            captureResolution.second,
            customFps ?: videoFps
        )
        localVideoTrack = factory?.createVideoTrack(VIDEO_TRACK_ID, videoSource)
        localVideoTrack?.setEnabled(videoEnabledOnStart)
        localVideoTrack?.addSink(localRender)
        return localVideoTrack
    }

    fun replaceVideoCapturer(
        capturer: VideoCapturer?,
        videoEnabledOnStart: Boolean,
        customVideoWidth: Int? = null,
        customVideoHeight: Int? = null,
        customFps: Int? = null
    ) {
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
            videoEnabledOnStart,
            customVideoWidth,
            customVideoHeight,
            customFps
        )

        peerConnection?.senders?.forEach { sender ->
            if (sender.track() != null) {
                val trackType = sender.track()?.kind()
                if (trackType == VIDEO_TRACK_TYPE) {
                    sender?.setTrack(localVideoTrack, false)
                }
            }
        }
    }

    private fun findVideoSender() {
        peerConnection?.senders?.forEach { sender ->
            if (sender.track() != null) {
                val trackType = sender.track()?.kind()
                if (trackType == VIDEO_TRACK_TYPE) {
                    Logger.d("Found video sender.")
                    localVideoSender = sender
                }
            }
        }
    }

    // Returns the remote VideoTrack, assuming there is only one.
    private fun getRemoteVideoTrack(): VideoTrack? {
        peerConnection?.transceivers?.forEach { transceiver ->
            val track = transceiver.receiver.track()
            if (track is VideoTrack) {
                return track
            }
        }
        return null
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

    private fun switchCameraInternal() {
        if (videoCapturer is CameraVideoCapturer) {
            if (!isVideoCallEnabled || isError) {
                Logger.e("Failed to switch camera. Video: $isVideoCallEnabled. Error : $isError")
                return  // No video is sent or only one camera is available or error happened.
            }
            Logger.d("Switch camera")
            val cameraVideoCapturer = videoCapturer as CameraVideoCapturer
            cameraVideoCapturer.switchCamera(switchEventsHandler)
        } else {
            Logger.d("Will not switch camera, video caputurer is not a camera")
        }
    }

    fun switchCamera() {
        executor.execute { switchCameraInternal() }
    }

    fun changeCaptureFormat(width: Int, height: Int, framerate: Int) {
        executor.execute { changeCaptureFormatInternal(width, height, framerate) }
    }

    fun isScreencastActive(): Boolean {
        return videoCapturer?.isScreencast ?: false
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

    val isDataChannelOpen: Boolean
        get() = if (dataChannel != null) {
            dataChannel?.state() == DataChannel.State.OPEN
        } else {
            false
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

    /**
     * Peer connection parameters.
     */
    class DataChannelParameters(
        val ordered: Boolean, val maxRetransmitTimeMs: Int, val maxRetransmits: Int,
        val protocol: String, val negotiated: Boolean, val id: Int
    )

    /**
     * Peer connection parameters.
     */
    class PeerConnectionParameters(
        var videoCallEnabled: Boolean,
        val loopback: Boolean,
        val tracing: Boolean,
        val videoWidth: Int,
        val videoHeight: Int,
        val videoFps: Int,
        val videoMaxBitrate: Int,
        val videoCodec: String,
        val videoCodecHwAcceleration: Boolean,
        val videoFlexfecEnabled: Boolean,
        val audioStartBitrate: Int,
        val audioCodec: String?,
        val noAudioProcessing: Boolean,
        val aecDump: Boolean,
        val saveInputAudioToFile: Boolean,
        val useOpenSLES: Boolean,
        val disableBuiltInAEC: Boolean,
        val disableBuiltInAGC: Boolean,
        val disableBuiltInNS: Boolean,
        val disableWebRtcAGCAndHPF: Boolean,
        val enableRtcEventLog: Boolean,
        val useLegacyAudioDevice: Boolean,
        val dataChannelParameters: DataChannelParameters
    ) {
        constructor(ecoMode: Boolean, widescreen: Boolean) : this(
            videoCallEnabled = !ecoMode,
            loopback = false,
            tracing = false,
            videoWidth = 640,
            videoHeight = if (widescreen) 360 else 480,
            videoFps = 30,
            videoMaxBitrate = 2000,
            videoCodec = "VP8",
            videoCodecHwAcceleration = true,
            videoFlexfecEnabled = true,
            audioStartBitrate = 200,
            audioCodec = "",
            noAudioProcessing = false,
            aecDump = false,
            saveInputAudioToFile = false,
            useOpenSLES = false,
            disableBuiltInAEC = false,
            disableBuiltInAGC = false,
            disableBuiltInNS = false,
            disableWebRtcAGCAndHPF = false,
            enableRtcEventLog = false,
            useLegacyAudioDevice = false,
            dataChannelParameters = DataChannelParameters(
                ordered = true,
                maxRetransmitTimeMs = -1,
                maxRetransmits = -1,
                protocol = "",
                negotiated = true,
                id = 0
            )
        )
    }

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
            peerConnection?.iceConnectionState()
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
            Logger.d("New Data channel ${dc.label()}id: ${dc.id()}")
            dc.registerObserver(object : DataChannel.Observer {
                override fun onBufferedAmountChange(previousAmount: Long) {
                    Logger.d("Data channel buffered amount changed: ${dc.label()}: ${dc.state()}")
                }

                override fun onStateChange() {
                    Logger.d("Data channel state changed: ${dc.label()}: ${dc.state()}")
                }

                override fun onMessage(buffer: DataChannel.Buffer) {
                    if (buffer.binary) {
                        Logger.d("Received binary msg over $dc")
                        return
                    }
                    val data = buffer.data
                    val bytes = ByteArray(data.capacity())
                    data[bytes]
                    val strData = String(bytes, StandardCharsets.UTF_8)
                    if (!strData.containsPing()) {
                        Logger.d("Got msg: $strData over $dc")
                    }
                }
            })
        }

        override fun onRenegotiationNeeded() {
            // No need to do anything; AppRTC follows a pre-agreed-upon
            // signaling/negotiation protocol.
        }

        override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<MediaStream>) {
            // NOOP
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
            if (preferIsac) {
                sdpDescription = preferCodec(sdpDescription, AUDIO_CODEC_ISAC, true)
            }
            if (isVideoCallEnabled) {
                sdpDescription = preferCodec(
                    sdpDescription,
                    getSdpVideoCodecName(peerConnectionParameters),
                    false
                )
            }
            val sdp = SessionDescription(origSdp.type, "${sdpDescription.trim()}\r\n")
            localSdp = sdp
            executor.execute {
                if (peerConnection != null && !isError) {
                    Logger.d("Set local SDP from ${sdp.type}")
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
                        Logger.d("Local SDP set successfully 1")
                        events.onLocalDescription(localSdp)
                    } else {
                        // We've just set remote description, so drain remote
                        // and send local ICE candidates.
                        Logger.d("Remote SDP set successfully 1")
                        drainCandidates()
                    }
                } else {
                    // For answering peer connection we set remote SDP and then
                    // create answer and set local SDP.
                    if (peerConnection?.localDescription != null) {
                        // We've just set our local SDP so time to send it, drain
                        // remote and send local ICE candidates.
                        Logger.d("Local SDP set successfully 2")
                        events.onLocalDescription(localSdp)
                        drainCandidates()
                    } else {
                        // We've just set remote SDP - do nothing for now -
                        // answer will be created soon.
                        Logger.d("Remote SDP set successfully 2")
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

    companion object {
        const val VIDEO_TRACK_ID = "ARDAMSv0"
        const val AUDIO_TRACK_ID = "ARDAMSa0"
        const val VIDEO_TRACK_TYPE = "video"
        private const val TAG = "PCRTCClient"
        private const val VIDEO_CODEC_VP8 = "VP8"
        private const val VIDEO_CODEC_VP9 = "VP9"
        private const val VIDEO_CODEC_H264 = "H264"
        private const val VIDEO_CODEC_H264_BASELINE = "H264 Baseline"
        private const val VIDEO_CODEC_H264_HIGH = "H264 High"
        private const val AUDIO_CODEC_OPUS = "opus"
        private const val AUDIO_CODEC_ISAC = "ISAC"
        private const val VIDEO_CODEC_PARAM_START_BITRATE = "x-google-start-bitrate"
        private const val VIDEO_FLEXFEC_FIELDTRIAL =
            "WebRTC-FlexFEC-03-Advertised/Enabled/WebRTC-FlexFEC-03/Enabled/"
        private const val VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL = "WebRTC-IntelVP8/Enabled/"
        private const val DISABLE_WEBRTC_AGC_FIELDTRIAL =
            "WebRTC-Audio-MinimizeResamplingOnMobile/Enabled/"
        private const val AUDIO_CODEC_PARAM_BITRATE = "maxaveragebitrate"
        private const val AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation"
        private const val AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl"
        private const val AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter"
        private const val AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression"
        private const val DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT = "DtlsSrtpKeyAgreement"
        private const val HD_VIDEO_WIDTH = 1280
        private const val HD_VIDEO_HEIGHT = 720
        private const val BPS_IN_KBPS = 1000
        private const val RTCEVENTLOG_OUTPUT_DIR_NAME = "rtc_event_log"
        private val mediaStreamLabels = listOf("ALPACA_ID")

        private const val TYPE_PING = "\"type\":\"ping\""

        // Executor thread is started once in private ctor and is used for all
        // peer connection API calls to ensure new peer connection factory is
        // created on the same thread as previously destroyed factory.
        private val executor = Executors.newSingleThreadExecutor()
        private const val ICE_GATHERING_TIMEOUT = 1500
        private fun getSdpVideoCodecName(parameters: PeerConnectionParameters?): String {
            return when (parameters?.videoCodec) {
                VIDEO_CODEC_VP8 -> VIDEO_CODEC_VP8
                VIDEO_CODEC_VP9 -> VIDEO_CODEC_VP9
                VIDEO_CODEC_H264_HIGH, VIDEO_CODEC_H264_BASELINE -> VIDEO_CODEC_H264
                else -> VIDEO_CODEC_VP8
            }
        }

        private fun getFieldTrials(peerConnectionParameters: PeerConnectionParameters): String {
            var fieldTrials = ""
            if (peerConnectionParameters.videoFlexfecEnabled) {
                fieldTrials += VIDEO_FLEXFEC_FIELDTRIAL
                Logger.d("Enable FlexFEC field trial.")
            }
            fieldTrials += VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL
            if (peerConnectionParameters.disableWebRtcAGCAndHPF) {
                fieldTrials += DISABLE_WEBRTC_AGC_FIELDTRIAL
                Logger.d("Disable WebRTC AGC field trial.")
            }
            return fieldTrials
        }

        private fun setStartBitrate(
            codec: String, isVideoCodec: Boolean, sdpDescription: String, bitrateKbps: Int
        ): String {
            val lines = sdpDescription.split("\r\n".toRegex()).toTypedArray()
            var rtpmapLineIndex = -1
            var sdpFormatUpdated = false
            var codecRtpMap: String? = null
            // Search for codec rtpmap in format
            // a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
            var regex = "^a=rtpmap:(\\d+) $codec(/\\d+)+[\r]?$"
            var codecPattern = Pattern.compile(regex)
            for (i in lines.indices) {
                val codecMatcher = codecPattern.matcher(lines[i])
                if (codecMatcher.matches()) {
                    codecRtpMap = codecMatcher.group(1)
                    rtpmapLineIndex = i
                    break
                }
            }
            if (codecRtpMap == null) {
                Logger.w("No rtpmap for $codec codec")
                return sdpDescription
            }
            Logger.d("Found $codec rtpmap $codecRtpMap at ${lines[rtpmapLineIndex]}")

            // Check if a=fmtp string already exist in remote SDP for this codec and
            // update it with new bitrate parameter.
            regex = "^a=fmtp:$codecRtpMap \\w+=\\d+.*[\r]?$"
            codecPattern = Pattern.compile(regex)
            for (i in lines.indices) {
                val codecMatcher = codecPattern.matcher(lines[i])
                if (codecMatcher.matches()) {
                    Logger.d("Found $codec ${lines[i]}")
                    if (isVideoCodec) {
                        lines[i] += "; $VIDEO_CODEC_PARAM_START_BITRATE=$bitrateKbps"
                    } else {
                        lines[i] += "; " + AUDIO_CODEC_PARAM_BITRATE + "=" + bitrateKbps * 1000
                    }
                    Logger.d("Update remote SDP line: ${lines[i]}")
                    sdpFormatUpdated = true
                    break
                }
            }
            val newSdpDescription = StringBuilder()
            for (i in lines.indices) {
                newSdpDescription.append(lines[i]).append("\r\n")
                // Append new a=fmtp line if no such line exist for a codec.
                if (!sdpFormatUpdated && i == rtpmapLineIndex) {
                    val bitrateSet: String = if (isVideoCodec) {
                        "a=fmtp:$codecRtpMap $VIDEO_CODEC_PARAM_START_BITRATE=$bitrateKbps"
                    } else {
                        ("a=fmtp:" + codecRtpMap + " " + AUDIO_CODEC_PARAM_BITRATE + "="
                                + bitrateKbps * 1000)
                    }
                    Logger.d("Add remote SDP line: $bitrateSet")
                    newSdpDescription.append(bitrateSet).append("\r\n")
                }
            }
            return newSdpDescription.toString()
        }

        /**
         * Returns the line number containing "m=audio|video", or -1 if no such line exists.
         */
        private fun findMediaDescriptionLine(isAudio: Boolean, sdpLines: Array<String>): Int {
            val mediaDescription = if (isAudio) "m=audio " else "m=video "
            for (i in sdpLines.indices) {
                if (sdpLines[i].startsWith(mediaDescription)) {
                    return i
                }
            }
            return -1
        }

        private fun joinString(
            s: Iterable<CharSequence?>, delimiter: String, delimiterAtEnd: Boolean
        ): String {
            val iter = s.iterator()
            if (!iter.hasNext()) {
                return ""
            }
            val buffer = StringBuilder(iter.next().toString())
            while (iter.hasNext()) {
                buffer.append(delimiter).append(iter.next())
            }
            if (delimiterAtEnd) {
                buffer.append(delimiter)
            }
            return buffer.toString()
        }

        private fun movePayloadTypesToFront(
            preferredPayloadTypes: List<String?>, mLine: String
        ): String? {
            // The format of the media description line should be: m=<media> <port> <proto> <fmt> ...
            val origLineParts = listOf(*mLine.split(" ".toRegex()).toTypedArray())
            if (origLineParts.size <= 3) {
                Logger.e("Wrong SDP media description format: $mLine")
                return null
            }
            val header: List<String?> = origLineParts.subList(0, 3)
            val unpreferredPayloadTypes: MutableList<String?> =
                ArrayList(origLineParts.subList(3, origLineParts.size))
            unpreferredPayloadTypes.removeAll(preferredPayloadTypes)
            // Reconstruct the line with |preferredPayloadTypes| moved to the beginning of the payload
            // types.
            val newLineParts: MutableList<String?> = ArrayList()
            newLineParts.addAll(header)
            newLineParts.addAll(preferredPayloadTypes)
            newLineParts.addAll(unpreferredPayloadTypes)
            return joinString(newLineParts, " ", false /* delimiterAtEnd */)
        }

        private fun preferCodec(
            sdpDescription: String,
            codec: String,
            isAudio: Boolean
        ): String {
            val lines = sdpDescription.split("\r\n".toRegex()).toTypedArray()
            val mLineIndex = findMediaDescriptionLine(isAudio, lines)
            if (mLineIndex == -1) {
                Logger.w("No mediaDescription line, so can't prefer $codec")
                return sdpDescription
            }
            // A list with all the payload types with name |codec|. The payload types are integers in the
            // range 96-127, but they are stored as strings here.
            val codecPayloadTypes: MutableList<String?> = ArrayList()
            // a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
            val codecPattern = Pattern.compile("^a=rtpmap:(\\d+) $codec(/\\d+)+[\r]?$")
            for (line in lines) {
                val codecMatcher = codecPattern.matcher(line)
                if (codecMatcher.matches()) {
                    codecPayloadTypes.add(codecMatcher.group(1))
                }
            }
            if (codecPayloadTypes.isEmpty()) {
                Logger.w("No payload types with name $codec")
                return sdpDescription
            }
            val newMLine = movePayloadTypesToFront(codecPayloadTypes, lines[mLineIndex])
                ?: return sdpDescription
            Logger.d("Change media description from: ${lines[mLineIndex]} to $newMLine")
            lines[mLineIndex] = newMLine
            return joinString(listOf(*lines), "\r\n", true /* delimiterAtEnd */)
        }
    }
}
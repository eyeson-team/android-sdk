package com.eyeson.sdk.gstreamercapturer

import android.content.Context
import android.util.Log
import com.eyeson.sdk.EyesonMeeting
import com.eyeson.sdk.webrtc.NativeVideoCapturer
import org.freedesktop.gstreamer.GStreamer
import org.webrtc.CapturerObserver
import org.webrtc.JavaI420Buffer
import org.webrtc.PeerConnection
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoFrame
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ByteBufferVideoCapturer(private val mediaUrl: String) : VideoCapturer, NativeVideoCapturer {

    private var capturerObserver: CapturerObserver? = null
    private var executor: ExecutorService = getExecutor()
    private var started = false


    private fun getExecutor(): ExecutorService {
        return Executors.newSingleThreadExecutor()
    }

    override fun initialize(
        surfaceTextureHelper: SurfaceTextureHelper,
        context: Context,
        capturerObserver: CapturerObserver,
    ) {
        this.capturerObserver = capturerObserver

        System.loadLibrary("gstreamer_android")
        System.loadLibrary("native-lib")

        try {
            GStreamer.init(context)
        } catch (e: Exception) {
            Log.e(this.javaClass.name, "GStreamer.init Exception $e;")
        }
    }

    override fun startCapture(width: Int, height: Int, framerate: Int) {
        if (executor.isShutdown || executor.isTerminated) {
            executor = getExecutor()
        }
        started = true
        startPipeline(object : FrameCallback {
            override fun onFrame(
                width: Int,
                height: Int,
                y: ByteArray,
                u: ByteArray,
                v: ByteArray,
            ) {
                onFrameFromNative(width, height, y, u, v)
            }

        })
    }

    override fun stopCapture() {
        started = false
        stopPipeline()
        executor.shutdown()
    }

    override fun isScreencast() = false
    override fun dispose() {
        started = false
        stopPipeline()
        executor.shutdownNow()
    }

    override fun changeCaptureFormat(width: Int, height: Int, framerate: Int) {}

    fun onFrameFromNative(width: Int, height: Int, y: ByteArray, u: ByteArray, v: ByteArray) {
        if (!started || executor.isShutdown) return

        executor.execute {
            try {
                val yBuffer = ByteBuffer.allocateDirect(y.size).put(y).apply { flip() }
                val uBuffer = ByteBuffer.allocateDirect(u.size).put(u).apply { flip() }
                val vBuffer = ByteBuffer.allocateDirect(v.size).put(v).apply { flip() }

                val i420Buffer = JavaI420Buffer.wrap(
                    width, height,
                    yBuffer, width,
                    uBuffer, width / 2,
                    vBuffer, width / 2,
                    null
                )

                val videoFrame = VideoFrame(i420Buffer, 0, System.nanoTime())
                capturerObserver?.onFrameCaptured(videoFrame)
                videoFrame.release()
            } catch (e: Exception) {
                Log.e("ByteBufferCapturer", "onFrame failed: ${e.message}", e)
            }
        }
    }

    private external fun nativeInitPipeline(mediaUrl: String, frameHandler: Any)
    private external fun nativeStopPipeline()


    private var isPipelineRunning = false


    interface FrameCallback {
        fun onFrame(width: Int, height: Int, y: ByteArray, u: ByteArray, v: ByteArray)
    }

    fun startPipeline(
        frameCallback: FrameCallback,
    ) {
        isPipelineRunning = true
        nativeInitPipeline(mediaUrl, frameCallback)
    }

    fun stopPipeline() {
        if (isPipelineRunning) {
            nativeStopPipeline()

            isPipelineRunning = false
        }
    }
}

fun EyesonMeeting.replaceCapturerWithMediaStream(mediaUrl: String) {
    val byteBufferVideoCapturer = ByteBufferVideoCapturer(mediaUrl)
    replaceVideoCapturer(byteBufferVideoCapturer)
}

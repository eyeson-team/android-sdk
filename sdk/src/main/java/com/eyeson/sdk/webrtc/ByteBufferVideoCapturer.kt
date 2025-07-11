package com.eyeson.sdk.webrtc

import android.util.Log
import org.webrtc.CapturerObserver
import org.webrtc.JavaI420Buffer
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoFrame
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ByteBufferVideoCapturer : VideoCapturer {

    private var capturerObserver: CapturerObserver? = null
    private var executor: ExecutorService = getExecutor()
    private var started = false


    private fun getExecutor(): ExecutorService {
        return Executors.newSingleThreadExecutor()
    }

    override fun initialize(
        surfaceTextureHelper: SurfaceTextureHelper,
        context: android.content.Context,
        capturerObserver: CapturerObserver,
    ) {
        this.capturerObserver = capturerObserver
        startCapture(0, 0, 0)
    }

    override fun startCapture(width: Int, height: Int, framerate: Int) {
        started = true
        if (executor.isShutdown || executor.isTerminated) {
            executor = getExecutor()
        }
    }

    override fun stopCapture() {
        started = false
        executor.shutdown()
    }

    override fun isScreencast() = false
    override fun dispose() {
        started = false
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
}

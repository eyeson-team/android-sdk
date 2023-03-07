package com.eyeson.sdk.webrtc

import android.content.Context
import android.content.res.Resources.NotFoundException
import android.graphics.SurfaceTexture
import android.os.Looper
import android.util.AttributeSet
import android.view.TextureView
import com.eyeson.sdk.utils.Logger
import org.webrtc.EglBase
import org.webrtc.EglRenderer
import org.webrtc.GlRectDrawer
import org.webrtc.RendererCommon.GlDrawer
import org.webrtc.RendererCommon.RendererEvents
import org.webrtc.RendererCommon.ScalingType
import org.webrtc.RendererCommon.VideoLayoutMeasure
import org.webrtc.SurfaceEglRenderer
import org.webrtc.SurfaceViewRenderer
import org.webrtc.ThreadUtils
import org.webrtc.VideoFrame
import org.webrtc.VideoSink
import java.util.concurrent.CountDownLatch

/**
 * Based on [SurfaceViewRenderer]
 *
 * Standard View constructor. In order to render something, you must first call init().
 */
class VideoRenderer(context: Context, attrs: AttributeSet? = null) :
    TextureView(context, attrs),
    VideoSink, RendererEvents, TextureView.SurfaceTextureListener {
    // Cached resource name.
    private val resourceName: String
    private val videoLayoutMeasure = VideoLayoutMeasure()
    private val eglRenderer: SurfaceEglRenderer

    // Callback for reporting renderer events. Read-only after initialization so no lock required.
    private var rendererEvents: RendererEvents? = null

    // Accessed only on the main thread.
    private var rotatedFrameWidth = 0
    private var rotatedFrameHeight = 0
    private var enableFixedSize = false

    init {
        resourceName = getResourceName()
        eglRenderer = SurfaceEglRenderer(resourceName)
        surfaceTextureListener = this
    }
    /**
     * Initialize this class, sharing resources with `sharedContext`. The custom `drawer` will be used
     * for drawing frames on the EGLSurface. This class is responsible for calling release() on
     * `drawer`. It is allowed to call init() to reinitialize the renderer after a previous
     * init()/release() cycle.
     */
    /**
     * Initialize this class, sharing resources with `sharedContext`. It is allowed to call init() to
     * reinitialize the renderer after a previous init()/release() cycle.
     */
    @JvmOverloads
    fun init(
        sharedContext: EglBase.Context?,
        rendererEvents: RendererEvents? = null,
        configAttributes: IntArray? = EglBase.CONFIG_PLAIN,
        drawer: GlDrawer? = GlRectDrawer()
    ) {
        ThreadUtils.checkIsOnMainThread()
        this.rendererEvents = rendererEvents
        rotatedFrameWidth = 0
        rotatedFrameHeight = 0
        eglRenderer.init(sharedContext, this /* rendererEvents */, configAttributes, drawer)
    }

    /**
     * Block until any pending frame is returned and all GL resources released, even if an interrupt
     * occurs. If an interrupt occurs during release(), the interrupt flag will be set. This function
     * should be called before the Activity is destroyed and the EGLContext is still valid. If you
     * don't call this function, the GL resources might leak.
     */
    fun release() {
        eglRenderer.release()
    }

    /**
     * Register a callback to be invoked when a new video frame has been received.
     *
     * @param listener The callback to be invoked. The callback will be invoked on the render thread.
     * It should be lightweight and must not call removeFrameListener.
     * @param scale    The scale of the Bitmap passed to the callback, or 0 if no Bitmap is
     * required.
     * @param drawer   Custom drawer to use for this frame listener.
     */
    fun addFrameListener(
        listener: EglRenderer.FrameListener?, scale: Float, drawerParam: GlDrawer?
    ) {
        eglRenderer.addFrameListener(listener, scale, drawerParam)
    }

    /**
     * Register a callback to be invoked when a new video frame has been received. This version uses
     * the drawer of the EglRenderer that was passed in init.
     *
     * @param listener The callback to be invoked. The callback will be invoked on the render thread.
     * It should be lightweight and must not call removeFrameListener.
     * @param scale    The scale of the Bitmap passed to the callback, or 0 if no Bitmap is
     * required.
     */
    fun addFrameListener(listener: EglRenderer.FrameListener?, scale: Float) {
        eglRenderer.addFrameListener(listener, scale)
    }

    fun removeFrameListener(listener: EglRenderer.FrameListener?) {
        eglRenderer.removeFrameListener(listener)
    }

    /**
     * Enables fixed size for the surface. This provides better performance but might be buggy on some
     * devices. By default this is turned off.
     */
    fun setEnableHardwareScaler(enabled: Boolean) {
        ThreadUtils.checkIsOnMainThread()
        enableFixedSize = enabled
    }

    /**
     * Set if the video stream should be mirrored or not.
     */
    fun setMirror(mirror: Boolean) {
        eglRenderer.setMirror(mirror)
    }

    /**
     * Set how the video will fill the allowed layout area.
     */
    fun setScalingType(scalingType: ScalingType?) {
        ThreadUtils.checkIsOnMainThread()
        videoLayoutMeasure.setScalingType(scalingType)
        requestLayout()
    }

    fun setScalingType(
        scalingTypeMatchOrientation: ScalingType?,
        scalingTypeMismatchOrientation: ScalingType?
    ) {
        ThreadUtils.checkIsOnMainThread()
        videoLayoutMeasure.setScalingType(
            scalingTypeMatchOrientation,
            scalingTypeMismatchOrientation
        )
        requestLayout()
    }

    /**
     * Limit render framerate.
     *
     * @param fps Limit render framerate to this value, or use Float.POSITIVE_INFINITY to disable fps
     * reduction.
     */
    fun setFpsReduction(fps: Float) {
        eglRenderer.setFpsReduction(fps)
    }

    fun disableFpsReduction() {
        eglRenderer.disableFpsReduction()
    }

    fun pauseVideo() {
        eglRenderer.pauseVideo()
    }

    // VideoSink interface.
    override fun onFrame(frame: VideoFrame) {
        eglRenderer.onFrame(frame)
    }

    // View layout interface.
    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        ThreadUtils.checkIsOnMainThread()
        val size =
            videoLayoutMeasure.measure(widthSpec, heightSpec, rotatedFrameWidth, rotatedFrameHeight)
        setMeasuredDimension(size.x, size.y)
        Logger.d("onMeasure(). New size: " + size.x + "x" + size.y)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        ThreadUtils.checkIsOnMainThread()
        eglRenderer.setLayoutAspectRatio((right - left) / (bottom - top).toFloat())
    }

    private fun getResourceName(): String {
        return try {
            resources.getResourceEntryName(id)
        } catch (e: NotFoundException) {
            ""
        }
    }

    /**
     * Post a task to clear the SurfaceView to a transparent uniform color.
     */
    fun clearImage() {
        eglRenderer.clearImage()
    }

    override fun onFirstFrameRendered() {
        if (rendererEvents != null) {
            rendererEvents!!.onFirstFrameRendered()
        }
    }

    override fun onFrameResolutionChanged(videoWidth: Int, videoHeight: Int, rotation: Int) {
        if (rendererEvents != null) {
            rendererEvents!!.onFrameResolutionChanged(videoWidth, videoHeight, rotation)
        }
        val rotatedWidth = if (rotation == 0 || rotation == 180) videoWidth else videoHeight
        val rotatedHeight = if (rotation == 0 || rotation == 180) videoHeight else videoWidth
        // run immediately if possible for ui thread tests
        postOrRun {
            rotatedFrameWidth = rotatedWidth
            rotatedFrameHeight = rotatedHeight
            requestLayout()
        }
    }

    private fun postOrRun(r: Runnable) {
        if (Thread.currentThread() === Looper.getMainLooper().thread) {
            r.run()
        } else {
            post(r)
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        ThreadUtils.checkIsOnMainThread()
        eglRenderer.createEglSurface(surface)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        ThreadUtils.checkIsOnMainThread()
        val completionLatch = CountDownLatch(1)
        eglRenderer.releaseEglSurface { completionLatch.countDown() }
        ThreadUtils.awaitUninterruptibly(completionLatch)
        return true
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        /*NOOP*/
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        /*NOOP*/
    }
}
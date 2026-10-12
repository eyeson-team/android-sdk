package com.eyeson.sdk.options

import androidx.annotation.FloatRange
import androidx.annotation.IntRange

/**
 * @property scaleResolutionDownBy Divides the resolution that is actually encoded, on top of
 *                                 [videoResolution]. `1.0` sends [videoResolution] unchanged,
 *                                 `2.0` halves width and height. Values below `1.0` are not
 *                                 possible and are clamped.
 * @property videoMaxBitrate Upper limit of the encoded camera video in kbps. Only applies while
 *                           the camera is the active source, see
 *                           [ScreenShareOptions.videoMaxBitrate] for screen sharing.
 */
public data class VideoOptions(
    val videoResolution: VideoResolution = Resolution16by9.DEFAULT,
    @IntRange(from = 0, to = 30)
    val videoFps: Int = 30,
    @IntRange(from = 0, to = 2000)
    val videoMaxBitrate: Int = 2000,
    @FloatRange(from = 1.0, to = 16.0)
    val scaleResolutionDownBy: Double = 1.0,
)

/**
 * @property scaleResolutionDownBy Divides the resolution that is actually encoded, on top of
 *                                 [screenShareResolution]. `1.0` sends [screenShareResolution]
 *                                 unchanged, `2.0` halves width and height. Values below `1.0`
 *                                 are not possible and are clamped.
 * @property videoMaxBitrate Upper limit of the encoded screen share in kbps. Independent of
 *                           [VideoOptions.videoMaxBitrate], which only applies while the camera
 *                           is the active source.
 */
public data class ScreenShareOptions(
    @IntRange(from = 0, to = 30)
    val screenShareFps: Int = 15,
    val screenShareResolution: VideoResolution = ScreenShareMaxResolution,
    @FloatRange(from = 1.0, to = 16.0)
    val scaleResolutionDownBy: Double = 1.0,
    @IntRange(from = 0, to = 2000)
    val videoMaxBitrate: Int = 2000,
)

public open class VideoResolution(
    public val width: Int,
    public val height: Int,
){
    public operator fun component1(): Int {
        return width
    }

    public operator fun component2(): Int {
        return height
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return other is VideoResolution && width == other.width && height == other.height
    }

    override fun hashCode(): Int {
        return 31 * width + height
    }

    override fun toString(): String {
        return "${javaClass.simpleName}(${width}x$height)"
    }
}

public sealed class Resolution16by9(width: Int, height: Int) : VideoResolution(width, height) {
    public object LOWEST : Resolution16by9(320, 180)
    public object LOWER : Resolution16by9(480, 270)
    public object DEFAULT : Resolution16by9(640, 360)
    public object HIGH : Resolution16by9(960, 540)
    public object HIGHEST : Resolution16by9(1280, 720)
}

public sealed class Resolution4by3(width: Int, height: Int) : VideoResolution(width, height) {
    public object LOWEST : Resolution4by3(320, 240)
    public object LOWER : Resolution4by3(480, 360)
    public object DEFAULT : Resolution4by3(640, 480)
    public object HIGH : Resolution4by3(960, 720)
    public object HIGHEST : Resolution4by3(1280, 960)
}

public val ScreenShareMaxResolution: VideoResolution = VideoResolution(1920, 1080)

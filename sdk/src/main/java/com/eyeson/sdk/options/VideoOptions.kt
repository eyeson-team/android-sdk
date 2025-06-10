package com.eyeson.sdk.options

data class VideoOptions(
    val videoResolution: VideoResolution = Resolution16by9.DEFAULT.resolution,
    val videoFps: Int = 30,
    val videoMaxBitrate: Int = 2000,
    val videoCodecHardwareAcceleration: Boolean = true,
)

data class ScreenShareOptions(
    val screenShareFps: Int = 15,
    val screenShareResolution: VideoResolution = ScreenShareMaxResolution
)

data class VideoResolution(
    val width: Int,
    val height: Int,
)

enum class Resolution16by9(val resolution: VideoResolution) {
    LOWEST(VideoResolution(320, 180)),
    LOWER(VideoResolution(480, 270)),
    DEFAULT(VideoResolution(640, 360)),
    HIGH(VideoResolution(960, 540)),
    HIGHEST(VideoResolution(1280, 720));
}

enum class Resolution4by3(val resolution: VideoResolution) {
    LOWEST(VideoResolution(320, 240)),
    LOWER(VideoResolution(480, 360)),
    DEFAULT(VideoResolution(640, 480)),
    HIGH(VideoResolution(960, 720)),
    HIGHEST(VideoResolution(1280, 960));
}

val ScreenShareMaxResolution: VideoResolution = VideoResolution(1920, 1080)

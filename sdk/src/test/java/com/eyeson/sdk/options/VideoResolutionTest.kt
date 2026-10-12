package com.eyeson.sdk.options

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * [VideoResolution] is an open class, so it has to provide value semantics itself. The change
 * detection in `PeerConnectionClient.updatePeerConnectionParameter` relies on it.
 */
class VideoResolutionTest {

    @Test
    fun sealedResolutionEqualsPlainResolutionOfSameSize() {
        assertEquals(VideoResolution(640, 360), Resolution16by9.DEFAULT)
        assertEquals(Resolution16by9.DEFAULT, VideoResolution(640, 360))
        assertEquals(Resolution4by3.HIGHEST, VideoResolution(1280, 960))
    }

    @Test
    fun equalResolutionsShareHashCode() {
        assertEquals(VideoResolution(640, 360).hashCode(), Resolution16by9.DEFAULT.hashCode())
    }

    @Test
    fun differentSizesAreNotEqual() {
        assertNotEquals(Resolution16by9.DEFAULT, Resolution16by9.HIGH)
        assertNotEquals(VideoResolution(640, 360), VideoResolution(360, 640))
    }

    @Test
    fun optionsWithSameResolutionAreEqual() {
        assertEquals(
            VideoOptions(),
            VideoOptions(videoResolution = VideoResolution(640, 360))
        )
        assertEquals(
            ScreenShareOptions(),
            ScreenShareOptions(screenShareResolution = VideoResolution(1920, 1080))
        )
    }

    @Test
    fun toStringContainsDimensions() {
        assertEquals("VideoResolution(640x360)", VideoResolution(640, 360).toString())
        assertEquals("DEFAULT(640x360)", Resolution16by9.DEFAULT.toString())
    }
}

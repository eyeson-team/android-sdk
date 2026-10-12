package com.eyeson.sdk.options

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaOptionsTest {

    @Test
    fun defaultSendsAndReceivesEverything() {
        val options = MediaOptions()
        assertTrue(options.sendAudio)
        assertTrue(options.receiveAudio)
        assertTrue(options.sendVideo)
        assertTrue(options.receiveVideo)
    }

    @Test
    fun receiveOnlyIsAllowed() {
        val options = MediaOptions(sendAudio = false, sendVideo = false)
        assertFalse(options.sendAudio)
        assertFalse(options.sendVideo)
    }

    @Test(expected = IllegalArgumentException::class)
    fun nothingAtAllIsRejected() {
        MediaOptions(sendAudio = false, receiveAudio = false, sendVideo = false, receiveVideo = false)
    }

    @Test(expected = IllegalArgumentException::class)
    fun copyToNothingAtAllIsRejected() {
        MediaOptions(sendVideo = false, receiveVideo = false)
            .copy(sendAudio = false, receiveAudio = false)
    }
}

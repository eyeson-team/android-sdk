package com.eyeson.sdk.options

import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

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

    @Test
    fun nothingAtAllIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            MediaOptions(sendAudio = false, receiveAudio = false, sendVideo = false, receiveVideo = false)
        }
    }

    @Test
    fun copyToNothingAtAllIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            MediaOptions(sendVideo = false, receiveVideo = false)
                .copy(sendAudio = false, receiveAudio = false)
        }
    }
}

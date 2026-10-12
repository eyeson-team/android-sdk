package com.eyeson.sdk.webrtc

import com.eyeson.sdk.utils.Logger
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * `PeerConnectionClient.setOpusMaxAverageBitrate` munges the remote SDP on every join, and a
 * mistake there crashes the executor thread instead of failing a call.
 */
class OpusMaxAverageBitrateTest {

    @Before
    fun disableLogging() {
        // android.util.Log is not available in local unit tests.
        Logger.enabled = false
    }

    private fun sdp(vararg lines: String) = lines.joinToString("\r\n")

    @Test
    fun appendsToExistingFmtpLine() {
        val result = PeerConnectionClient.setOpusMaxAverageBitrate(
            sdp(
                "m=audio 9 UDP/TLS/RTP/SAVPF 111",
                "a=rtpmap:111 opus/48000/2",
                "a=fmtp:111 minptime=10;useinbandfec=1",
            ),
            bitrateKbps = 64
        )

        assertEquals(
            sdp(
                "m=audio 9 UDP/TLS/RTP/SAVPF 111",
                "a=rtpmap:111 opus/48000/2",
                "a=fmtp:111 minptime=10;useinbandfec=1;maxaveragebitrate=64000",
            ),
            result
        )
    }

    @Test
    fun replacesExistingBitrate() {
        val result = PeerConnectionClient.setOpusMaxAverageBitrate(
            sdp(
                "a=rtpmap:111 opus/48000/2",
                "a=fmtp:111 maxaveragebitrate=20000; useinbandfec=1",
            ),
            bitrateKbps = 32
        )

        assertEquals(
            sdp(
                "a=rtpmap:111 opus/48000/2",
                "a=fmtp:111 useinbandfec=1;maxaveragebitrate=32000",
            ),
            result
        )
    }

    @Test
    fun updatesFmtpLineStartingWithNonNumericParameter() {
        val result = PeerConnectionClient.setOpusMaxAverageBitrate(
            sdp(
                "a=rtpmap:111 opus/48000/2",
                "a=fmtp:111 sprop-maxcapturerate=48000;useinbandfec=1",
            ),
            bitrateKbps = 64
        )

        assertEquals(
            sdp(
                "a=rtpmap:111 opus/48000/2",
                "a=fmtp:111 sprop-maxcapturerate=48000;useinbandfec=1;maxaveragebitrate=64000",
            ),
            result
        )
    }

    @Test
    fun insertsFmtpLineAfterRtpmapWhenMissing() {
        // Shorter than the payload type, which used to be taken as the insert position.
        val result = PeerConnectionClient.setOpusMaxAverageBitrate(
            sdp(
                "m=audio 9 UDP/TLS/RTP/SAVPF 111",
                "a=rtpmap:111 opus/48000/2",
                "a=rtcp-fb:111 transport-cc",
            ),
            bitrateKbps = 64
        )

        assertEquals(
            sdp(
                "m=audio 9 UDP/TLS/RTP/SAVPF 111",
                "a=rtpmap:111 opus/48000/2",
                "a=fmtp:111 maxaveragebitrate=64000",
                "a=rtcp-fb:111 transport-cc",
            ),
            result
        )
    }

    @Test
    fun ignoresFmtpOfOtherPayloadTypeWithSamePrefix() {
        val result = PeerConnectionClient.setOpusMaxAverageBitrate(
            sdp(
                "a=rtpmap:111 opus/48000/2",
                "a=fmtp:1110 foo=1",
            ),
            bitrateKbps = 64
        )

        assertEquals(
            sdp(
                "a=rtpmap:111 opus/48000/2",
                "a=fmtp:111 maxaveragebitrate=64000",
                "a=fmtp:1110 foo=1",
            ),
            result
        )
    }

    @Test
    fun leavesSdpWithoutOpusUntouched() {
        val input = sdp("m=audio 9 UDP/TLS/RTP/SAVPF 0", "a=rtpmap:0 PCMU/8000")

        assertEquals(input, PeerConnectionClient.setOpusMaxAverageBitrate(input, bitrateKbps = 64))
    }
}

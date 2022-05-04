package com.eyeson.sdk.utils

import org.webrtc.SessionDescription
import java.util.regex.Pattern

internal object WebRTCUtils {
    fun logSdp(sdp: String, type: String?) {
        val lines = sdp.split("\r\n".toRegex()).toTypedArray()
        Logger.d("================SDP================")
        Logger.d("type: $type")
        for (line in lines) {
            Logger.d(line)
        }
        Logger.d("===================================")
    }

    fun logSdp(sdp: SessionDescription) {
        logSdp(sdp.description, sdp.type.toString())
    }

    fun checkSdpOffersVideo(sdp: String): Boolean {
        val lines = sdp.split("\r\n".toRegex()).toTypedArray()
        val patternRecvOnly = Pattern.compile("^a=recvonly[\r]?$")
        val patternInactive = Pattern.compile("^a=inactive[\r]?$")
        for (i in lines.indices) {
            val matcher1 = patternRecvOnly.matcher(lines[i])
            val matcher2 = patternInactive.matcher(lines[i])
            if (matcher1.matches() || matcher2.matches()) {
                return false
            }
        }
        return true
    }
}
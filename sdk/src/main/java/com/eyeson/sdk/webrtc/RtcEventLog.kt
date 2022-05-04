/*
 *  Copyright 2018 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 *
 * LICENSE: https://chromium.googlesource.com/external/webrtc/+/master/LICENSE
 * PATENTS: https://chromium.googlesource.com/external/webrtc/+/master/PATENTS
 * AUTHORS: https://chromium.googlesource.com/external/webrtc/+/master/AUTHORS
 */
package com.eyeson.sdk.webrtc

import android.os.ParcelFileDescriptor
import com.eyeson.sdk.utils.Logger.Companion.d
import com.eyeson.sdk.utils.Logger.Companion.e
import org.webrtc.PeerConnection
import java.io.File
import java.io.IOException

internal class RtcEventLog(peerConnection: PeerConnection?) {
    private val peerConnection: PeerConnection
    private var state = RtcEventLogState.INACTIVE
    fun start(outputFile: File?) {
        if (state == RtcEventLogState.STARTED) {
            e("RtcEventLog has already started.")
            return
        }
        val fileDescriptor: ParcelFileDescriptor = try {
            ParcelFileDescriptor.open(
                outputFile,
                ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE
                        or ParcelFileDescriptor.MODE_TRUNCATE
            )
        } catch (e: IOException) {
            e("Failed to create a new file$e")
            return
        }

        // Passes ownership of the file to WebRTC.
        val success =
            peerConnection.startRtcEventLog(fileDescriptor.detachFd(), OUTPUT_FILE_MAX_BYTES)
        if (!success) {
            e("Failed to start RTC event log.")
            return
        }
        state = RtcEventLogState.STARTED
        d("RtcEventLog started.")
    }

    fun stop() {
        if (state != RtcEventLogState.STARTED) {
            e("RtcEventLog was not started.")
            return
        }
        peerConnection.stopRtcEventLog()
        state = RtcEventLogState.STOPPED
        d("RtcEventLog stopped.")
    }

    internal enum class RtcEventLogState {
        INACTIVE, STARTED, STOPPED
    }

    companion object {
        private const val OUTPUT_FILE_MAX_BYTES = 10000000
    }

    init {
        if (peerConnection == null) {
            throw NullPointerException("The peer connection is null.")
        }
        this.peerConnection = peerConnection
    }
}
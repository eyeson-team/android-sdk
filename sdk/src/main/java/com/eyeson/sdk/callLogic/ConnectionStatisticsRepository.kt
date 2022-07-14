package com.eyeson.sdk.callLogic

import com.eyeson.sdk.model.local.call.ConnectionStatistic
import com.eyeson.sdk.model.local.call.ConnectionStatistic.EntryReceived
import com.eyeson.sdk.model.local.call.ConnectionStatistic.EntrySent
import com.eyeson.sdk.utils.Logger
import org.webrtc.RTCStatsReport
import java.math.BigInteger

internal class ConnectionStatisticsRepository {
    private var rtcStatistics = mutableListOf<RtcStatsEntry>()

    internal data class RtcStatsEntry(
        val timestampInMicros: Long,
        var jitterAudioSent: Double = 0.0,
        var jitterVideoSent: Double = 0.0,
        var jitterAudioReceived: Double = 0.0,
        var jitterVideoReceived: Double = 0.0,

        var packetsSentAudio: Long = 0,
        var packetsSentVideo: Long = 0,
        var packetsReceivedAudio: Long = 0,
        var packetsReceivedVideo: Long = 0,

        var packetsLostSentAudio: Int = 0,
        var packetsLostSentVideo: Int = 0,
        var packetsLostReceivedAudio: Int = 0,
        var packetsLostReceivedVideo: Int = 0,

        var roundTripTimeAudio: Double = 0.0,
        var roundTripTimeVideo: Double = 0.0,

        var bytesSentAudio: BigInteger = BigInteger.ZERO,
        var bytesSentVideo: BigInteger = BigInteger.ZERO,
        var bytesReceivedAudio: BigInteger = BigInteger.ZERO,
        var bytesReceivedVideo: BigInteger = BigInteger.ZERO,
    )

    fun addNewRTCStatsReport(report: RTCStatsReport) {
        val newEntry = RtcStatsEntry(report.timestampUs.toLong())
        report.statsMap.forEach {
            val members = it.value.members
            val type = it.value.type
            when {
                type == INBOUND_RTP && members[KIND] == AUDIO -> {
                    newEntry.jitterAudioReceived = members[JITTER] as? Double ?: 0.0
                    newEntry.packetsReceivedAudio = members[PACKETS_RECEIVED] as? Long ?: 0L
                    newEntry.packetsLostReceivedAudio = members[PACKETS_LOST] as? Int ?: 0
                    newEntry.bytesReceivedAudio = members[BYTES_RECEIVED] as? BigInteger
                        ?: BigInteger.ZERO
                }
                type == INBOUND_RTP && members[KIND] == VIDEO -> {
                    newEntry.jitterVideoReceived = members[JITTER] as? Double ?: 0.0
                    newEntry.packetsReceivedVideo = members[PACKETS_RECEIVED] as? Long ?: 0L
                    newEntry.packetsLostReceivedVideo = members[PACKETS_LOST] as? Int ?: 0
                    newEntry.bytesReceivedVideo =
                        members[BYTES_RECEIVED] as? BigInteger ?: BigInteger.ZERO
                }
                type == OUTBOUND_RTP && members[KIND] == AUDIO -> {
                    newEntry.packetsSentAudio = members[PACKETS_SENT] as? Long ?: 0L
                    newEntry.bytesSentAudio = members[BYTES_SENT] as? BigInteger ?: BigInteger.ZERO
                }
                type == OUTBOUND_RTP && members[KIND] == VIDEO -> {
                    newEntry.packetsSentVideo = members[PACKETS_SENT] as? Long ?: 0L
                    newEntry.bytesSentVideo = members[BYTES_SENT] as? BigInteger ?: BigInteger.ZERO
                }
                type == REMOTE_INBOUND_RTP && members[KIND] == AUDIO -> {
                    newEntry.roundTripTimeAudio = members[ROUND_TRIP_TIME] as? Double ?: 0.0
                    newEntry.jitterAudioSent = members[JITTER] as? Double ?: 0.0
                    newEntry.packetsLostSentAudio = members[PACKETS_LOST] as? Int ?: 0
                }
                type == REMOTE_INBOUND_RTP && members[KIND] == VIDEO -> {
                    newEntry.roundTripTimeVideo = members[ROUND_TRIP_TIME] as? Double ?: 0.0
                    newEntry.jitterVideoSent = members[JITTER] as? Double ?: 0.0
                    newEntry.packetsLostSentVideo = members[PACKETS_LOST] as? Int ?: 0
                }
            }
        }
        rtcStatistics.add(0, newEntry)
        rtcStatistics = rtcStatistics.take(STATISTIC_SIZE).toMutableList()
    }

    fun getStatsInfo(): ConnectionStatistic? {
        if (rtcStatistics.size < 2) {
            return null
        }

        val current = rtcStatistics[0]
        val previous = rtcStatistics[1]


        return ConnectionStatistic(
            current.timestampInMicros,
            EntrySent(
                current.jitterAudioSent,
                deltaTo(current.bytesSentAudio, previous.bytesSentAudio),
                deltaTo(current.packetsSentAudio, previous.packetsSentAudio),
                deltaTo(current.packetsLostSentAudio, previous.packetsLostSentAudio),
                current.roundTripTimeAudio
            ),
            EntrySent(
                current.jitterVideoSent,
                deltaTo(current.bytesSentVideo, previous.bytesSentVideo),
                deltaTo(current.packetsSentVideo, previous.packetsSentVideo),
                deltaTo(current.packetsLostSentVideo, previous.packetsLostSentVideo),
                current.roundTripTimeVideo
            ),
            EntryReceived(
                current.jitterAudioReceived,
                deltaTo(current.bytesReceivedAudio, previous.bytesReceivedAudio),
                deltaTo(current.packetsReceivedAudio, previous.packetsReceivedAudio),
                deltaTo(current.packetsLostReceivedAudio, previous.packetsLostReceivedAudio)
            ),
            EntryReceived(
                current.jitterVideoReceived,
                deltaTo(current.bytesReceivedVideo, previous.bytesReceivedVideo),
                deltaTo(current.packetsReceivedVideo, previous.packetsReceivedVideo),
                deltaTo(current.packetsLostReceivedVideo, previous.packetsLostReceivedVideo)
            )
        )
    }

    private fun deltaTo(current: BigInteger, previous: BigInteger): Long {
        val delta = if (current < previous) {
            previous
        } else {
            current - previous
        }
        return delta.toLong()
    }

    private fun deltaTo(current: Int, previous: Int): Long {
        return deltaTo(current.toLong(), previous.toLong())
    }

    private fun deltaTo(current: Long, previous: Long): Long {
        return if (current < previous) {
            previous
        } else {
            current - previous
        }
    }


    private companion object {
        const val INBOUND_RTP = "inbound-rtp"
        const val OUTBOUND_RTP = "outbound-rtp"
        const val REMOTE_INBOUND_RTP = "remote-inbound-rtp"

        const val KIND = "kind"
        const val AUDIO = "audio"
        const val VIDEO = "video"

        const val BYTES_RECEIVED = "bytesReceived"
        const val BYTES_SENT = "bytesSent"
        const val JITTER = "jitter"
        const val PACKETS_SENT = "packetsSent"
        const val PACKETS_RECEIVED = "packetsReceived"
        const val PACKETS_LOST = "packetsLost"
        const val ROUND_TRIP_TIME = "roundTripTime"

        const val STATISTIC_SIZE = 2
    }

}
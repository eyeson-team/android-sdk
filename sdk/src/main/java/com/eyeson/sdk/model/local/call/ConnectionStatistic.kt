package com.eyeson.sdk.model.local.call

import com.eyeson.sdk.model.local.base.LocalBaseCommand

data class ConnectionStatistic(
    val timestampInMicros: Long,
    val audioOut: EntrySent,
    val videoOut: EntrySent,
    val audioIn: EntryReceived,
    val videoIn: EntryReceived
) : LocalBaseCommand {
    data class EntrySent(
        val jitterInSeconds: Double,
        val bytesSentDelta: Long,
        val packetsSentDelta: Long,
        val packetsLostDelta: Long,
        val roundTripTimeInSeconds: Double
    )

    data class EntryReceived(
        val jitterInSeconds: Double,
        val bytesReceivedDelta: Long,
        val packetsReceivedDelta: Long,
        val packetsLostDelta: Long
    )
}
package com.eyeson.sdk.model.local.call

import com.eyeson.sdk.model.local.base.LocalBaseCommand
import com.eyeson.sdk.model.local.statistic.ConnectionStatistic

internal data class ConnectionStatisticLocal(
    val timestampInMicros: Long,
    val audioOut: EntrySent,
    val videoOut: EntrySent,
    val audioIn: EntryReceived,
    val videoIn: EntryReceived
) : LocalBaseCommand {
    internal data class EntrySent(
        val jitterInSeconds: Double,
        val bytesSentDelta: Long,
        val packetsSentDelta: Long,
        val packetsLostDelta: Long,
        val roundTripTimeInSeconds: Double
    )

    internal data class EntryReceived(
        val jitterInSeconds: Double,
        val bytesReceivedDelta: Long,
        val packetsReceivedDelta: Long,
        val packetsLostDelta: Long
    )
}

internal fun ConnectionStatisticLocal.toLocal(): ConnectionStatistic {
    return ConnectionStatistic(
        timestampInMicros = timestampInMicros,
        audioOut = audioOut.toLocal(),
        videoOut = videoOut.toLocal(),
        audioIn = audioIn.toLocal(),
        videoIn = videoIn.toLocal()
    )
}

private fun ConnectionStatisticLocal.EntrySent.toLocal(): ConnectionStatistic.EntrySent {
    return ConnectionStatistic.EntrySent(
        jitterInSeconds = jitterInSeconds,
        bytesSentDelta = bytesSentDelta,
        packetsSentDelta = packetsSentDelta,
        packetsLostDelta = packetsLostDelta,
        roundTripTimeInSeconds = roundTripTimeInSeconds
    )
}

private fun ConnectionStatisticLocal.EntryReceived.toLocal(): ConnectionStatistic.EntryReceived {
    return ConnectionStatistic.EntryReceived(
        jitterInSeconds = jitterInSeconds,
        bytesReceivedDelta = bytesReceivedDelta,
        packetsReceivedDelta = packetsReceivedDelta,
        packetsLostDelta = packetsLostDelta
    )
}

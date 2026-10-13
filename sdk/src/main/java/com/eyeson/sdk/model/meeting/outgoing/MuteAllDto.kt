package com.eyeson.sdk.model.meeting.outgoing

import com.eyeson.sdk.serializer.AsStringSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MuteAllDto(
    @SerialName("command") val command: String = "message",
    @Serializable(with = ChannelIdentifierAsStringSerializer::class)
    @SerialName("identifier") var identifier: SubscribeToChannel.Identifier =
        SubscribeToChannel.Identifier(SubscribeToChannel.ROOM_CHANNEL),
    @Serializable(with = MuteAllDataAsStringSerializer::class)
    @SerialName("data") var data: Data = Data()
) {
    @Serializable
    data class Data(
        @SerialName("message") val message: String = STFU,
        @SerialName("action") val action: String = STFU
    )

    companion object {
        private const val STFU = "stfu"
    }
}

internal object MuteAllDataAsStringSerializer :
    KSerializer<MuteAllDto.Data> by AsStringSerializer(MuteAllDto.Data.serializer())
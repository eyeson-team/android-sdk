package com.eyeson.sdk.model.meeting.outgoing

import com.eyeson.sdk.serializer.AsStringSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SubscribeToChannel(
    @SerialName("command") val command: String,
    @Serializable(with = ChannelIdentifierAsStringSerializer::class)
    @SerialName("identifier") var identifier: Identifier
) {
    @Serializable
    data class Identifier(
        @SerialName("channel") val channel: String
    )

    companion object {
        internal const val ROOM_CHANNEL = "RoomChannel"
        internal const val USER_CHANNEL = "UserChannel"
        private const val SUBSCRIBE = "subscribe"
        private const val UNSUBSCRIBE = "unsubscribe"

        private fun getCommand(subscribe: Boolean): String = if (subscribe) {
            SUBSCRIBE
        } else {
            UNSUBSCRIBE
        }

        fun roomChannel(subscribe: Boolean): SubscribeToChannel {
            return SubscribeToChannel(getCommand(subscribe), Identifier(ROOM_CHANNEL))
        }

        fun userChannel(subscribe: Boolean): SubscribeToChannel {
            return SubscribeToChannel(getCommand(subscribe), Identifier(USER_CHANNEL))
        }
    }
}

/**
 * Shared by every outgoing ActionCable message, so their `identifier` strings match the one the
 * channel was subscribed with.
 */
internal object ChannelIdentifierAsStringSerializer :
    KSerializer<SubscribeToChannel.Identifier> by AsStringSerializer(SubscribeToChannel.Identifier.serializer())

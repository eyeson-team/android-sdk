package com.eyeson.sdk.model.meeting.outgoing

import com.eyeson.sdk.moshi.adapter.AsString
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SubscribeToChannel(
    @Json(name = "command") val command: String,
    @AsString @Json(name = "identifier") var identifier: Identifier
) {
    @JsonClass(generateAdapter = true)
    data class Identifier(
        @Json(name = "channel") val channel: String
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

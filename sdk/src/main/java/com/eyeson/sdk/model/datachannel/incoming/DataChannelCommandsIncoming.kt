package com.eyeson.sdk.model.datachannel.incoming


internal enum class DataChannelCommandsIncoming(val type: String) {
    PING("ping"),
    VOICE_ACTIVITY("voice_activity")
}


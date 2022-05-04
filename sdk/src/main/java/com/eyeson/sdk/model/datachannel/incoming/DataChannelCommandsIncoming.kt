package com.eyeson.sdk.model.datachannel.incoming


internal enum class DataChannelCommandsIncoming(val type: String) {
    PING("ping"),
    CHAT("chat"),
    SOURCE_UPDATE("source_update"),
    MEMBERLIST_UPDATE("memberlist"),
    VOICE_ACTIVITY("voice_activity"),
    RECORDING_STATUS("recording")
}


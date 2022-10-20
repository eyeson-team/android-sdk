package com.eyeson.sdk.model.sepp.incoming


internal enum class SeppCommandsIncoming(val type: String) {
    CALL_ACCEPTED("call_accepted"),
    CALL_REJECTED("call_rejected"),
    CALL_TERMINATED("call_terminated"),
    CALL_RESUMED("call_resumed"),

    CHAT("chat"),
    SOURCE_UPDATE("source_update"),
    MEMBERLIST_UPDATE("memberlist"),
    RECORDING_STATUS("recording")
}


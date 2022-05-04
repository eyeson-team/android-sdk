package com.eyeson.sdk.model.sepp.incoming


internal enum class SeppCommandsIncoming(val type: String) {
    CALL_ACCEPTED("call_accepted"),
    CALL_REJECTED("call_rejected"),
    CALL_TERMINATED("call_terminated")
}


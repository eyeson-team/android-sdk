package com.eyeson.sdk.model.sepp.outgoing


internal enum class SeppCommandsOutgoing(val type: String) {
    CALL_START("call_start"),
    CALL_TERMINATE("call_terminate"),
    CALL_RESUME("call_resume")
}


package com.eyeson.sdk.events

enum class CallRejectionReason(val rejectCode: Int) {
    UNSPECIFIED(-7),
    BAD_REQUEST(400),
    NOT_FOUND(404),
    FORBIDDEN(403),
    UNWANTED(607),
    GONE(410),
    ERROR(500);

    companion object {
        private val mapping = values().associateBy(CallRejectionReason::rejectCode)
        fun fromRejectCode(code: Int) = mapping[code] ?: ERROR
    }
}

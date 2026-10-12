package com.eyeson.sdk.events

public enum class CallRejectionReason(public val rejectCode: Int) {
    UNSPECIFIED(-7),
    BAD_REQUEST(400),
    FORBIDDEN(403),
    NOT_FOUND(404),
    GONE(410),
    ERROR(500),
    SERVICE_UNAVAILABLE(503),
    UNWANTED(607);

    public companion object {
        private val mapping = entries.associateBy(CallRejectionReason::rejectCode)
        public fun fromRejectCode(code: Int): CallRejectionReason = mapping[code] ?: ERROR
    }
}

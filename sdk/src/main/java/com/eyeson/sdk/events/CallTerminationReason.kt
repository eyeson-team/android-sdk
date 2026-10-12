package com.eyeson.sdk.events

public enum class CallTerminationReason(public val terminationCode:Int) {
    UNSPECIFIED(-7),
    OK(200),
    FORBIDDEN(403),
    ERROR(500),
    UNWANTED(607);

    public companion object {
        private val mapping =
            entries.associateBy(CallTerminationReason::terminationCode)

        public fun fromTerminationCode(code: Int): CallTerminationReason = mapping[code] ?: ERROR
    }
}
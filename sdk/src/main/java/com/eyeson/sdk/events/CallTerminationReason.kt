package com.eyeson.sdk.events

enum class CallTerminationReason(val terminationCode:Int) {
    UNSPECIFIED(-7),
    OK(200),
    FORBIDDEN(403),
    ERROR(500),
    UNWANTED(607);

    companion object {
        private val mapping =
            entries.associateBy(CallTerminationReason::terminationCode)

        fun fromTerminationCode(code: Int) = mapping[code] ?: ERROR
    }
}
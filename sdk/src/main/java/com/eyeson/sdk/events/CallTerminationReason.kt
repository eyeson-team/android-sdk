package com.eyeson.sdk.events

enum class CallTerminationReason(val terminationCode:Int) {
    OK(200),
    FORBIDDEN(403),
    ERROR(500),
    UNWANTED(607);

    companion object {
        private val mapping =
            values().associateBy(CallTerminationReason::terminationCode)

        fun fromTerminationCode(code: Int) = mapping[code] ?: ERROR
    }
}
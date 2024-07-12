package com.eyeson.sdk.events

enum class PresentationResponse(val responseCode: Int) {
    UNSPECIFIED(-7),
    OK(200),
    CREATED(201),
    BAD_REQUEST(400),
    NOT_FOUND(404),
    CONFLICT(409),
    GONE(410),
    ERROR(500);

    companion object {
        private val mapping = entries.associateBy(PresentationResponse::responseCode)
        fun fromResponseCode(code: Int) = mapping[code] ?: UNSPECIFIED
        fun isSuccessful(code: Int): Boolean {
            return code == OK.responseCode || code == CREATED.responseCode
        }
    }

    fun isSuccessful(): Boolean {
        return PresentationResponse.isSuccessful(responseCode)
    }
}

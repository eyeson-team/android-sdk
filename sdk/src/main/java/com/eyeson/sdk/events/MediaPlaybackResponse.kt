package com.eyeson.sdk.events

enum class MediaPlaybackResponse(val responseCode: Int) {
    UNSPECIFIED(-7),
    OK(200),
    CREATED(201),
    BAD_REQUEST(400),
    NOT_FOUND(404),
    GONE(410),
    ERROR(500);

    companion object {
        private val mapping = values().associateBy(MediaPlaybackResponse::responseCode)
        fun fromResponseCode(code: Int) = mapping[code] ?: UNSPECIFIED
        fun isSuccessful(code: Int): Boolean {
            return code == OK.responseCode || code == CREATED.responseCode
        }
    }

    fun isSuccessful(): Boolean {
        return MediaPlaybackResponse.isSuccessful(responseCode)
    }
}

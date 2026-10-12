package com.eyeson.sdk.events

public enum class MediaPlaybackResponse(public val responseCode: Int) {
    /**
     * The request got no response, e.g. because of a network failure, or one with a status code
     * not listed here.
     */
    UNSPECIFIED(-7),
    OK(200),
    CREATED(201),
    BAD_REQUEST(400),
    NOT_FOUND(404),
    GONE(410),
    ERROR(500);

    public companion object {
        private val mapping = entries.associateBy(MediaPlaybackResponse::responseCode)
        public fun fromResponseCode(code: Int): MediaPlaybackResponse = mapping[code] ?: UNSPECIFIED
        public fun isSuccessful(code: Int): Boolean {
            return code == OK.responseCode || code == CREATED.responseCode
        }
    }

    public fun isSuccessful(): Boolean {
        return MediaPlaybackResponse.isSuccessful(responseCode)
    }
}

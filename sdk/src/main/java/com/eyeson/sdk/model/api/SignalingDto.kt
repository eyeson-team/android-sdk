package com.eyeson.sdk.model.api


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SignalingDto(
    @Json(name = "type") val type: String,
    @Json(name = "options") val options: Options
) {
    @JsonClass(generateAdapter = true)
    data class Options(
        @Json(name = "client_id") val clientId: String,
        @Json(name = "conf_id") val confId: String,
        @Json(name = "auth_token") val authToken: String,
        @Json(name = "endpoint") val endpoint: String,
        @Json(name = "stun_servers") val stunServers: List<String>,
        @Json(name = "turn_servers") val turnServers: List<TurnServerDto>
    )
}

package com.eyeson.sdk.model.api


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SignalingDto(
    @SerialName("type") val type: String,
    @SerialName("options") val options: Options
) {
    @Serializable
    data class Options(
        @SerialName("client_id") val clientId: String,
        @SerialName("conf_id") val confId: String,
        @SerialName("auth_token") val authToken: String,
        @SerialName("endpoint") val endpoint: String,
        @SerialName("stun_servers") val stunServers: List<String>,
        @SerialName("turn_servers") val turnServers: List<TurnServerDto>
    )
}

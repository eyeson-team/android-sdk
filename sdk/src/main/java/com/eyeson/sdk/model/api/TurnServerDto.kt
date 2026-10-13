package com.eyeson.sdk.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TurnServerDto(
    @SerialName("urls") var urls: List<String>,
    @SerialName("username") var username: String,
    @SerialName("password") var password: String,
)
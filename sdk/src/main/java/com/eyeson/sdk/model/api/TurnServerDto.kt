package com.eyeson.sdk.model.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class TurnServerDto(
    @Json(name = "urls") var urls: List<String>,
    @Json(name = "username") var username: String,
    @Json(name = "password") var password: String,
)
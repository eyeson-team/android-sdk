package com.eyeson.sdk.model.api


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OptionsDto(
    @Json(name = "widescreen") val widescreen: Boolean
)
package com.eyeson.sdk.model.api


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
internal data class SnapshotDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "links") val links: Links,
    @Json(name = "creator") val creator: Creator,
    @Json(name = "created_at") val createdAt: Date
) {
    @JsonClass(generateAdapter = true)
    data class Links(
        @Json(name = "download") val download: String
    )

    @JsonClass(generateAdapter = true)
    data class Creator(
        @Json(name = "id") val id: String,
        @Json(name = "name") val name: String,
        @Json(name = "avatar") val avatar: String?,
        @Json(name = "guest") val guest: Boolean,
        @Json(name = "joined_at") val joinedAt: Date
    )
}
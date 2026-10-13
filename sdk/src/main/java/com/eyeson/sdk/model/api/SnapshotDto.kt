package com.eyeson.sdk.model.api


import com.eyeson.sdk.serializer.DateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
internal data class SnapshotDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("links") val links: Links,
    @SerialName("creator") val creator: Creator,
    @Serializable(with = DateSerializer::class)
    @SerialName("created_at") val createdAt: Date
) {
    @Serializable
    data class Links(
        @SerialName("download") val download: String
    )

    @Serializable
    data class Creator(
        @SerialName("id") val id: String,
        @SerialName("name") val name: String,
        @SerialName("avatar") val avatar: String?,
        @SerialName("guest") val guest: Boolean,
        @Serializable(with = DateSerializer::class)
        @SerialName("joined_at") val joinedAt: Date
    )
}
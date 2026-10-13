package com.eyeson.sdk.model.api

import com.eyeson.sdk.serializer.DateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
internal data class RoomDto(
    @SerialName("id") val id: String,
    @SerialName("name") var name: String,
    @SerialName("ready") var ready: Boolean,
    @Serializable(with = DateSerializer::class)
    @SerialName("started_at") val startedAt: Date,
    @SerialName("guest_token") var guestToken: String?
)


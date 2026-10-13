package com.eyeson.sdk.model.meeting.base

import com.eyeson.sdk.serializer.AsStringSerializer
import com.eyeson.sdk.serializer.MeetingIncomingMessageOrNullSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MeetingBaseCommandDto(
    @SerialName("type") val type: String?,
    @SerialName("command") val command: String?,
    @Serializable(with = MeetingIdentifierAsStringSerializer::class)
    @SerialName("identifier") var identifier: Identifier?,
    @Serializable(with = MeetingIncomingMessageOrNullSerializer::class)
    @SerialName("message") val message: MeetingBaseMessageDto?,
) {
    @Serializable
    data class Identifier(
        @SerialName("channel") val channel: String?
    )
}

internal object MeetingIdentifierAsStringSerializer :
    KSerializer<MeetingBaseCommandDto.Identifier> by AsStringSerializer(MeetingBaseCommandDto.Identifier.serializer())
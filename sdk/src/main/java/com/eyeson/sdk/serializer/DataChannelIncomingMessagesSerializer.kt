package com.eyeson.sdk.serializer

import com.eyeson.sdk.model.datachannel.base.DataChannelCommandDto
import com.eyeson.sdk.model.datachannel.base.UnknownCommandDto
import com.eyeson.sdk.model.datachannel.incoming.DataChannelCommandsIncoming
import com.eyeson.sdk.model.datachannel.incoming.PingDto
import com.eyeson.sdk.model.datachannel.incoming.VoiceActivityDto
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement

internal object DataChannelIncomingMessagesSerializer :
    JsonContentPolymorphicSerializer<DataChannelCommandDto>(DataChannelCommandDto::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<DataChannelCommandDto> {
        return when (element.typeLabel()) {
            DataChannelCommandsIncoming.PING.type -> PingDto.serializer()
            DataChannelCommandsIncoming.VOICE_ACTIVITY.type -> VoiceActivityDto.serializer()
            else -> UnknownCommandDto.serializer()
        }
    }
}

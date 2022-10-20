package com.eyeson.sdk.moshi.adapter

import com.eyeson.sdk.model.datachannel.base.DataChannelCommandDto
import com.eyeson.sdk.model.datachannel.base.UnknownCommandDto
import com.eyeson.sdk.model.datachannel.incoming.DataChannelCommandsIncoming
import com.eyeson.sdk.model.datachannel.incoming.PingDto
import com.eyeson.sdk.model.datachannel.incoming.VoiceActivityDto
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory

internal object DataChannelIncomingMessagesAdapter {
    fun provideDataChannelIncomingMessagesAdapterFactory(): PolymorphicJsonAdapterFactory<DataChannelCommandDto> {
        return PolymorphicJsonAdapterFactory
            .of(DataChannelCommandDto::class.java, "type")
            .withSubtype(PingDto::class.java, DataChannelCommandsIncoming.PING.type)
            .withSubtype(
                VoiceActivityDto::class.java,
                DataChannelCommandsIncoming.VOICE_ACTIVITY.type
            )
            .withDefaultValue(UnknownCommandDto())
    }
}
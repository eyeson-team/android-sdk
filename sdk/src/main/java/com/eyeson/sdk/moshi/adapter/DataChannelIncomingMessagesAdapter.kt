package com.eyeson.sdk.moshi.adapter

import com.eyeson.sdk.model.datachannel.base.DataChannelCommandDto
import com.eyeson.sdk.model.datachannel.base.UnknownCommandDto
import com.eyeson.sdk.model.datachannel.incoming.ChatIncomingDto
import com.eyeson.sdk.model.datachannel.incoming.DataChannelCommandsIncoming
import com.eyeson.sdk.model.datachannel.incoming.MemberListUpdateDto
import com.eyeson.sdk.model.datachannel.incoming.PingDto
import com.eyeson.sdk.model.datachannel.incoming.RecordingStatusDto
import com.eyeson.sdk.model.datachannel.incoming.SourceUpdateDto
import com.eyeson.sdk.model.datachannel.incoming.VoiceActivityDto
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory

internal object DataChannelIncomingMessagesAdapter {
    fun provideDataChannelIncomingMessagesAdapterFactory(): PolymorphicJsonAdapterFactory<DataChannelCommandDto> {
        return PolymorphicJsonAdapterFactory
            .of(DataChannelCommandDto::class.java, "type")
            .withSubtype(PingDto::class.java, DataChannelCommandsIncoming.PING.type)
            .withSubtype(ChatIncomingDto::class.java, DataChannelCommandsIncoming.CHAT.type)
            .withSubtype(
                SourceUpdateDto::class.java,
                DataChannelCommandsIncoming.SOURCE_UPDATE.type
            )
            .withSubtype(
                MemberListUpdateDto::class.java,
                DataChannelCommandsIncoming.MEMBERLIST_UPDATE.type
            )
            .withSubtype(
                VoiceActivityDto::class.java,
                DataChannelCommandsIncoming.VOICE_ACTIVITY.type
            )
            .withSubtype(
                RecordingStatusDto::class.java,
                DataChannelCommandsIncoming.RECORDING_STATUS.type
            )
            .withDefaultValue(UnknownCommandDto())
    }
}
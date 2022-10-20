package com.eyeson.sdk.moshi.adapter

import com.eyeson.sdk.model.sepp.base.SeppBaseCommandDto
import com.eyeson.sdk.model.sepp.base.UnknownCommandDto
import com.eyeson.sdk.model.sepp.incoming.CallAcceptedDto
import com.eyeson.sdk.model.sepp.incoming.CallRejectedDto
import com.eyeson.sdk.model.sepp.incoming.CallResumedDto
import com.eyeson.sdk.model.sepp.incoming.CallTerminatedDto
import com.eyeson.sdk.model.sepp.incoming.ChatIncomingDto
import com.eyeson.sdk.model.sepp.incoming.MemberListUpdateDto
import com.eyeson.sdk.model.sepp.incoming.RecordingStatusDto
import com.eyeson.sdk.model.sepp.incoming.SeppCommandsIncoming
import com.eyeson.sdk.model.sepp.incoming.SourceUpdateDto
import com.eyeson.sdk.model.sepp.shared.SdpUpdateDto
import com.eyeson.sdk.model.sepp.shared.SeppCommandsShared
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory

internal object SeppIncomingCommandsAdapter {
    fun provideSeppCommandsIncomingAdapterFactory(): PolymorphicJsonAdapterFactory<SeppBaseCommandDto> {
        return PolymorphicJsonAdapterFactory
            .of(SeppBaseCommandDto::class.java, "type")
            .withSubtype(CallAcceptedDto::class.java, SeppCommandsIncoming.CALL_ACCEPTED.type)
            .withSubtype(CallRejectedDto::class.java, SeppCommandsIncoming.CALL_REJECTED.type)
            .withSubtype(CallTerminatedDto::class.java, SeppCommandsIncoming.CALL_TERMINATED.type)
            .withSubtype(CallResumedDto::class.java, SeppCommandsIncoming.CALL_RESUMED.type)
            .withSubtype(SdpUpdateDto::class.java, SeppCommandsShared.SDP_UPDATE.type)
            .withSubtype(ChatIncomingDto::class.java, SeppCommandsIncoming.CHAT.type)
            .withSubtype(SourceUpdateDto::class.java, SeppCommandsIncoming.SOURCE_UPDATE.type)
            .withSubtype(
                MemberListUpdateDto::class.java,
                SeppCommandsIncoming.MEMBERLIST_UPDATE.type
            )
            .withSubtype(
                RecordingStatusDto::class.java,
                SeppCommandsIncoming.RECORDING_STATUS.type
            )
            .withDefaultValue(UnknownCommandDto())
    }
}
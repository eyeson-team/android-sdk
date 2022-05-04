package com.eyeson.sdk.moshi.adapter

import com.eyeson.sdk.model.sepp.base.SeppBaseCommandDto
import com.eyeson.sdk.model.sepp.base.UnknownCommandDto
import com.eyeson.sdk.model.sepp.incoming.CallAcceptedDto
import com.eyeson.sdk.model.sepp.incoming.CallRejectedDto
import com.eyeson.sdk.model.sepp.incoming.CallTerminatedDto
import com.eyeson.sdk.model.sepp.incoming.SeppCommandsIncoming
import com.eyeson.sdk.model.sepp.outgoing.CallStartDto
import com.eyeson.sdk.model.sepp.outgoing.CallTerminateDto
import com.eyeson.sdk.model.sepp.outgoing.SeppCommandsOutgoing
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
            .withSubtype(CallStartDto::class.java, SeppCommandsOutgoing.CALL_START.type)
            .withSubtype(CallTerminateDto::class.java, SeppCommandsOutgoing.CALL_TERMINATE.type)
            .withSubtype(SdpUpdateDto::class.java, SeppCommandsShared.SDP_UPDATE.type)
            .withDefaultValue(UnknownCommandDto())
    }
}
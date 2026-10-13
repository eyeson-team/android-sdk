package com.eyeson.sdk.serializer

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
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement

internal object SeppIncomingCommandsSerializer :
    JsonContentPolymorphicSerializer<SeppBaseCommandDto>(SeppBaseCommandDto::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<SeppBaseCommandDto> {
        return when (element.typeLabel()) {
            SeppCommandsIncoming.CALL_ACCEPTED.type -> CallAcceptedDto.serializer()
            SeppCommandsIncoming.CALL_REJECTED.type -> CallRejectedDto.serializer()
            SeppCommandsIncoming.CALL_TERMINATED.type -> CallTerminatedDto.serializer()
            SeppCommandsIncoming.CALL_RESUMED.type -> CallResumedDto.serializer()
            SeppCommandsShared.SDP_UPDATE.type -> SdpUpdateDto.serializer()
            SeppCommandsIncoming.CHAT.type -> ChatIncomingDto.serializer()
            SeppCommandsIncoming.SOURCE_UPDATE.type -> SourceUpdateDto.serializer()
            SeppCommandsIncoming.MEMBERLIST_UPDATE.type -> MemberListUpdateDto.serializer()
            SeppCommandsIncoming.RECORDING_STATUS.type -> RecordingStatusDto.serializer()
            else -> UnknownCommandDto.serializer()
        }
    }
}

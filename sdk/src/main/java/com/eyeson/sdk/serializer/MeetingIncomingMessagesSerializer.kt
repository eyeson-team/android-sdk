package com.eyeson.sdk.serializer

import com.eyeson.sdk.model.meeting.base.MeetingBaseMessageDto
import com.eyeson.sdk.model.meeting.base.UnknownMessageDto
import com.eyeson.sdk.model.meeting.incoming.BroadcastUpdateDto
import com.eyeson.sdk.model.meeting.incoming.ChatIncomingDto
import com.eyeson.sdk.model.meeting.incoming.CustomMessageDto
import com.eyeson.sdk.model.meeting.incoming.MeetingLockedDto
import com.eyeson.sdk.model.meeting.incoming.MeetingMessagesIncomingTypes
import com.eyeson.sdk.model.meeting.incoming.MuteLocalAudioDto
import com.eyeson.sdk.model.meeting.incoming.OptionsUpdateDto
import com.eyeson.sdk.model.meeting.incoming.PlaybackUpdateDto
import com.eyeson.sdk.model.meeting.incoming.PresentationUpdateDto
import com.eyeson.sdk.model.meeting.incoming.RecordingUpdateDto
import com.eyeson.sdk.model.meeting.incoming.RoomReadyDto
import com.eyeson.sdk.model.meeting.incoming.RoomSetupDto
import com.eyeson.sdk.model.meeting.incoming.SnapshotUpdateDto
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement

internal object MeetingIncomingMessagesSerializer :
    JsonContentPolymorphicSerializer<MeetingBaseMessageDto>(MeetingBaseMessageDto::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<MeetingBaseMessageDto> {
        return when (element.typeLabel()) {
            MeetingMessagesIncomingTypes.ROOM_SETUP.type -> RoomSetupDto.serializer()
            MeetingMessagesIncomingTypes.ROOM_READY.type -> RoomReadyDto.serializer()
            MeetingMessagesIncomingTypes.CHAT.type -> ChatIncomingDto.serializer()
            MeetingMessagesIncomingTypes.SNAPSHOT_UPDATE.type -> SnapshotUpdateDto.serializer()
            MeetingMessagesIncomingTypes.PLAYBACK_UPDATE.type -> PlaybackUpdateDto.serializer()
            MeetingMessagesIncomingTypes.RECORDING_UPDATE.type -> RecordingUpdateDto.serializer()
            MeetingMessagesIncomingTypes.BROADCASTS_UPDATE.type -> BroadcastUpdateDto.serializer()
            MeetingMessagesIncomingTypes.MUTE_LOCAL_AUDIO.type -> MuteLocalAudioDto.serializer()
            MeetingMessagesIncomingTypes.ROOM_LOCKED.type -> MeetingLockedDto.serializer()
            MeetingMessagesIncomingTypes.CUSTOM.type -> CustomMessageDto.serializer()
            MeetingMessagesIncomingTypes.PRESENTATION_UPDATE.type -> PresentationUpdateDto.serializer()
            MeetingMessagesIncomingTypes.OPTIONS_UPDATE.type -> OptionsUpdateDto.serializer()
            else -> UnknownMessageDto.serializer()
        }
    }
}

/**
 * The ActionCable envelope's `message`: `null` when it can't be decoded (pings carry a number),
 * instead of failing the whole envelope.
 */
internal object MeetingIncomingMessageOrNullSerializer :
    KSerializer<MeetingBaseMessageDto?> by NullOnDataMismatchSerializer(MeetingIncomingMessagesSerializer)

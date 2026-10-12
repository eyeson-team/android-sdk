package com.eyeson.sdk.moshi.adapter

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
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory

internal object MeetingIncomingMessagesAdapter {
    fun provideMeetingIncomingMessagesAdapterFactory(): PolymorphicJsonAdapterFactory<MeetingBaseMessageDto> {
        return PolymorphicJsonAdapterFactory
            .of(MeetingBaseMessageDto::class.java, "type")
            .withSubtype(RoomSetupDto::class.java, MeetingMessagesIncomingTypes.ROOM_SETUP.type)
            .withSubtype(RoomReadyDto::class.java, MeetingMessagesIncomingTypes.ROOM_READY.type)
            .withSubtype(ChatIncomingDto::class.java, MeetingMessagesIncomingTypes.CHAT.type)
            .withSubtype(
                SnapshotUpdateDto::class.java,
                MeetingMessagesIncomingTypes.SNAPSHOT_UPDATE.type
            )
            .withSubtype(
                PlaybackUpdateDto::class.java,
                MeetingMessagesIncomingTypes.PLAYBACK_UPDATE.type
            )
            .withSubtype(
                RecordingUpdateDto::class.java,
                MeetingMessagesIncomingTypes.RECORDING_UPDATE.type
            )
            .withSubtype(
                BroadcastUpdateDto::class.java,
                MeetingMessagesIncomingTypes.BROADCASTS_UPDATE.type
            )
            .withSubtype(
                MuteLocalAudioDto::class.java,
                MeetingMessagesIncomingTypes.MUTE_LOCAL_AUDIO.type
            )
            .withSubtype(MeetingLockedDto::class.java, MeetingMessagesIncomingTypes.ROOM_LOCKED.type)
            .withSubtype(CustomMessageDto::class.java, MeetingMessagesIncomingTypes.CUSTOM.type)
            .withSubtype(PresentationUpdateDto::class.java, MeetingMessagesIncomingTypes.PRESENTATION_UPDATE.type)
            .withSubtype(OptionsUpdateDto::class.java, MeetingMessagesIncomingTypes.OPTIONS_UPDATE.type)
            .withDefaultValue(UnknownMessageDto())
    }
}
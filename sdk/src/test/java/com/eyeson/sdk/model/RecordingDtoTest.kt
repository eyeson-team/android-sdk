package com.eyeson.sdk.model

import com.eyeson.sdk.di.NetworkModule
import com.eyeson.sdk.model.api.RecordingDto
import com.eyeson.sdk.model.local.meeting.toLocal
import com.eyeson.sdk.model.meeting.base.MeetingBaseCommandDto
import com.eyeson.sdk.model.meeting.incoming.RecordingUpdateDto
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `created_at` of a recording is epoch seconds; payloads are the examples from
 * https://docs.eyeson.com/docs/rest/references/recording.
 */
class RecordingDtoTest {
    private val json = NetworkModule.json

    @Test
    fun recordingUpdateMapsCreatedAtFromSeconds() {
        val command = json.decodeFromString(
            MeetingBaseCommandDto.serializer(),
            """{"identifier":"{\"channel\":\"RoomChannel\"}","message":{"type":"recording_update",
                "recording":$RECORDING}}"""
        )

        val recording = (command.message as RecordingUpdateDto).toLocal().toLocal()

        assertEquals(CREATED_AT_MILLIS, recording.createdAt.time)
    }

    @Test
    fun meetingRecordingMapsCreatedAtFromSeconds() {
        val recording = json.decodeFromString(RecordingDto.serializer(), RECORDING).toLocal()

        assertEquals(CREATED_AT_MILLIS, recording.createdAt.time)
    }

    private companion object {
        /** 2025-03-19T11:40:42Z */
        const val CREATED_AT_MILLIS = 1742384442000L

        const val RECORDING = """{"id":"67daad3af71a86c897058e33","created_at":1742384442,
            "duration":null,
            "links":{"self":"https://api.eyeson.team/recordings/67daad3af71a86c897058e33",
                "download":null},
            "user":{"id":"67daad29fe13f2745410bc88","name":"APIdoc","avatar":null,"guest":false,
                "joined_at":"2025-03-19T11:40:27.734Z"},
            "room":{"id":"API","name":"API magic","ready":true,
                "started_at":"2025-03-19T11:40:25.499Z","shutdown":false,
                "guest_token":"epYFNNduQtISh9MkoalvmCeN"}}"""
    }
}

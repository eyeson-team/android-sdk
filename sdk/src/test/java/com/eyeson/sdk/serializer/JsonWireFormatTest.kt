package com.eyeson.sdk.serializer

import com.eyeson.sdk.di.NetworkModule
import com.eyeson.sdk.model.datachannel.incoming.PingDto
import com.eyeson.sdk.model.datachannel.incoming.VoiceActivityDto
import com.eyeson.sdk.model.datachannel.outgoing.PongDto
import com.eyeson.sdk.model.datachannel.outgoing.fromLocal
import com.eyeson.sdk.model.local.datachannel.Pong
import com.eyeson.sdk.model.meeting.base.MeetingBaseCommandDto
import com.eyeson.sdk.model.meeting.base.UnknownMessageDto
import com.eyeson.sdk.model.meeting.incoming.CustomMessageDto
import com.eyeson.sdk.model.meeting.incoming.MeetingLockedDto
import com.eyeson.sdk.model.meeting.outgoing.MuteAllDto
import com.eyeson.sdk.model.meeting.outgoing.SubscribeToChannel
import com.eyeson.sdk.model.sepp.base.UnknownCommandDto
import com.eyeson.sdk.model.sepp.incoming.CallAcceptedDto
import com.eyeson.sdk.model.sepp.incoming.ChatIncomingDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import com.eyeson.sdk.model.datachannel.base.UnknownCommandDto as UnknownDataChannelCommandDto

/**
 * Pins the JSON the SDK sends and accepts, which must stay what the Moshi adapters produced.
 */
class JsonWireFormatTest {
    private val json = NetworkModule.json

    @Test
    fun muteAllEncodesDefaultsAsNestedJsonStrings() {
        assertEquals(
            """{"command":"message","identifier":"{\"channel\":\"RoomChannel\"}","data":"{\"message\":\"stfu\",\"action\":\"stfu\"}"}""",
            json.encodeToString(MuteAllDto.serializer(), MuteAllDto())
        )
    }

    @Test
    fun subscribeEncodesIdentifierAsJsonString() {
        assertEquals(
            """{"command":"subscribe","identifier":"{\"channel\":\"RoomChannel\"}"}""",
            json.encodeToString(
                SubscribeToChannel.serializer(),
                SubscribeToChannel.roomChannel(subscribe = true)
            )
        )
    }

    @Test
    fun pongEncodesType() {
        assertEquals(
            """{"type":"pong"}""",
            json.encodeToString(PongDto.serializer(), Pong().fromLocal())
        )
    }

    @Test
    fun seppKnownTypeDecodesSubtypeWithType() {
        val command = json.decodeFromString(
            SeppIncomingCommandsSerializer,
            """{"type":"call_accepted","msg_id":"m1","from":"conf","to":"client","extra":1,
                "data":{"call_id":"c1","sdp":{"type":"answer","sdp":"v=0"}}}"""
        )

        assertTrue(command is CallAcceptedDto)
        command as CallAcceptedDto
        assertEquals("call_accepted", command.type)
        assertEquals("c1", command.data.callId)
        assertEquals("v=0", command.data.sdp.sdp)
    }

    @Test
    fun seppUnknownTypeDecodesUnknownCommand() {
        val command = json.decodeFromString(
            SeppIncomingCommandsSerializer,
            """{"type":"something_new","data":{"x":1}}"""
        )

        assertTrue(command is UnknownCommandDto)
    }

    @Test
    fun seppAcceptsNumbersForStringFields() {
        val command = json.decodeFromString(
            SeppIncomingCommandsSerializer,
            """{"type":"chat","msg_id":42,"from":"a","to":"b",
                "data":{"call_id":"c","cid":7,"content":"hi","id":"x","ts":"2024-05-01T12:34:56.789Z"}}"""
        )

        command as ChatIncomingDto
        assertEquals("42", command.msgId)
        assertEquals("7", command.data.userId)
    }

    @Test
    fun meetingWelcomeHasNoMessage() {
        val command = decodeMeeting("""{"type":"welcome"}""")

        assertEquals("welcome", command.type)
        assertNull(command.identifier)
        assertNull(command.message)
    }

    @Test
    fun meetingPingWithNumericMessageHasNoMessage() {
        val command = decodeMeeting("""{"type":"ping","message":1712345678}""")

        assertEquals("ping", command.type)
        assertNull(command.message)
    }

    @Test
    fun meetingConfirmSubscriptionDecodesIdentifier() {
        val command = decodeMeeting(
            """{"identifier":"{\"channel\":\"RoomChannel\"}","type":"confirm_subscription"}"""
        )

        assertEquals("RoomChannel", command.identifier?.channel)
    }

    @Test
    fun meetingKnownMessageDecodesSubtype() {
        val command = decodeMeeting(
            """{"identifier":"{\"channel\":\"RoomChannel\"}","message":{"type":"lock","locked":true}}"""
        )

        assertEquals(MeetingLockedDto(type = "lock", locked = true), command.message)
    }

    @Test
    fun meetingMessageDecodesDate() {
        val command = decodeMeeting(
            """{"message":{"type":"custom","content":"x","cid":"u","created_at":"2024-05-01T12:34:56.789Z"}}"""
        )

        val message = command.message as CustomMessageDto
        assertEquals(MAY_FIRST_MILLIS, message.createdAt.time)
    }

    @Test
    fun meetingMessageThatFailsToDecodeIsNull() {
        val command = decodeMeeting(
            """{"identifier":"{\"channel\":\"RoomChannel\"}",
                "message":{"type":"custom","content":"x","cid":"u","created_at":"yesterday"}}"""
        )

        assertEquals("RoomChannel", command.identifier?.channel)
        assertNull(command.message)
    }

    @Test
    fun meetingUnknownOrMissingTypeDecodesUnknownMessage() {
        assertTrue(decodeMeeting("""{"message":{"type":"something_new"}}""").message is UnknownMessageDto)
        assertTrue(decodeMeeting("""{"message":{"foo":1}}""").message is UnknownMessageDto)
    }

    @Test
    fun dataChannelDecodesSubtypes() {
        assertTrue(decodeDataChannel("""{"type":"ping"}""") is PingDto)
        assertEquals(
            VoiceActivityDto(type = "voice_activity", userId = "u1", on = true),
            decodeDataChannel("""{"type":"voice_activity","cid":"u1","on":true}""")
        )
        assertTrue(decodeDataChannel("""{"type":"something_new"}""") is UnknownDataChannelCommandDto)
    }

    @Test
    fun malformedJsonThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            decodeDataChannel("""{"type":""")
        }
    }

    @Test
    fun dateAcceptsUtcAndOffsets() {
        assertEquals(
            MAY_FIRST_MILLIS,
            json.decodeFromString(DateSerializer, "\"2024-05-01T12:34:56.789Z\"").time
        )
        assertEquals(
            MAY_FIRST_MILLIS,
            json.decodeFromString(DateSerializer, "\"2024-05-01T14:34:56.789+02:00\"").time
        )
    }

    private fun decodeMeeting(text: String) =
        json.decodeFromString(MeetingBaseCommandDto.serializer(), text)

    private fun decodeDataChannel(text: String) =
        json.decodeFromString(DataChannelIncomingMessagesSerializer, text)

    private companion object {
        /** 2024-05-01T12:34:56.789Z */
        const val MAY_FIRST_MILLIS = 1714566896789L
    }
}

package com.eyeson.sdk.network

import app.cash.turbine.test
import com.eyeson.sdk.di.NetworkModule
import com.eyeson.sdk.model.local.api.MeetingOptions
import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.base.LocalBaseCommand
import com.eyeson.sdk.model.local.meeting.BroadcastUpdateLocal
import com.eyeson.sdk.model.local.meeting.CustomMessageLocal
import com.eyeson.sdk.model.local.meeting.MeetingLockedLocal
import com.eyeson.sdk.model.local.meeting.MuteLocalAudioLocal
import com.eyeson.sdk.model.local.meeting.OptionsUpdateLocal
import com.eyeson.sdk.model.local.meeting.PlaybackUpdateLocal
import com.eyeson.sdk.model.local.meeting.PresentationUpdateLocal
import com.eyeson.sdk.model.local.meeting.RecordingLocal
import com.eyeson.sdk.model.local.meeting.RoomReady
import com.eyeson.sdk.model.local.meeting.RoomSetup
import com.eyeson.sdk.model.local.meeting.SnapshotUpdateLocal
import com.eyeson.sdk.model.local.sepp.ChatIncoming
import com.eyeson.sdk.model.local.ws.WsOpen
import com.eyeson.sdk.model.meeting.base.MeetingBaseCommandDto
import com.eyeson.sdk.model.meeting.base.UnknownMessageDto
import com.eyeson.sdk.model.meeting.incoming.MeetingMessagesIncomingTypes
import com.eyeson.sdk.utils.TestLogger
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import java.time.Instant
import java.util.Date
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertIsNot
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pins how each message type of the meeting socket reaches [MeetingConnection.events]. A message
 * whose fields don't match is dropped without an error, so a mismatch has to show up here.
 *
 * The fixtures in `resources/meeting/` are the examples of the Meeting Observer reference
 * (https://docs.eyeson.com/docs/rest/advanced/meeting_observer). `stfu`, `room_setup` and
 * `room_ready` aren't documented there and were captured from a live meeting. Identifiers, tokens
 * and personal data are replaced.
 */
class MeetingMessagesTest {

    private val server = MockWebServer()
    private val serverSocket = ServerSocket()
    private lateinit var connection: MeetingConnection

    @BeforeTest
    fun setUp() {
        TestLogger.install()
        server.start()
        connection = MeetingConnection(testMeeting(server))
    }

    @AfterTest
    fun tearDown() {
        connection.disconnect()
        server.close()
    }

    @Test
    fun everyMessageTypeHasAFixtureThatDecodes() {
        MeetingMessagesIncomingTypes.entries.forEach { type ->
            val message = NetworkModule.json.decodeFromString(
                MeetingBaseCommandDto.serializer(),
                envelope(meetingFixture(type.type))
            ).message

            assertNotNull(message, "${type.type} did not decode")
            assertIsNot<UnknownMessageDto>(message, "${type.type} has no serializer branch")
            assertEquals(type.type, message.type)
        }
    }

    @Test
    fun chatIsAttributedToTheSendersClientId() = runTest {
        assertEquals(
            ChatIncoming(
                userId = "signaling-2",
                content = "Hello world!",
                messageId = "",
                timestamp = date("2023-12-11T09:59:33.892Z")
            ),
            receive(meetingFixture("chat"))
        )
    }

    @Test
    fun customMessageMapsContentSenderAndTime() = runTest {
        assertEquals(
            CustomMessageLocal(
                content = "Your custom message",
                createdAt = date("2023-12-11T10:01:49.353Z"),
                userId = "signaling-2"
            ),
            receive(meetingFixture("custom"))
        )
    }

    @Test
    fun lockMapsBothStates() = runTest {
        val locked = meetingFixture("lock")

        assertEquals(
            listOf(MeetingLockedLocal(locked = true), MeetingLockedLocal(locked = false)),
            receiveAll(locked, locked.replacing("true", "false"))
        )
    }

    @Test
    fun snapshotUpdateMapsSnapshotsAndTheirCreator() = runTest {
        assertEquals(
            SnapshotUpdateLocal(
                listOf(
                    SnapshotUpdateLocal.Snapshot(
                        id = "snapshot-1",
                        name = "1702288734",
                        creator = JOHN_DOE,
                        createdAt = date("2023-12-11T09:58:54.498Z"),
                        downloadLink = "https://fs.example.test/meetings/meeting-1/snapshots/" +
                            "1702288734.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256"
                    )
                )
            ),
            receive(meetingFixture("snapshot_update"))
        )
    }

    @Test
    fun playbackUpdateMapsWhatIsPlayingAndNothingOnceItEnded() = runTest {
        assertEquals(
            listOf(
                PlaybackUpdateLocal(
                    listOf(
                        PlaybackUpdateLocal.PlaybackInternal(
                            url = "https://media.example.test/videos/demo-video.webm",
                            name = null,
                            playId = "demo-video",
                            replacementId = null,
                            audio = true,
                            loopCount = 0
                        )
                    )
                ),
                PlaybackUpdateLocal(emptyList())
            ),
            receiveAll(
                meetingFixture("playback_update"),
                """{"type":"playback_update","playing":[]}"""
            )
        )
    }

    @Test
    fun playbackWithoutOptionalFieldsGetsTheDefaultsOfStartPlayback() = runTest {
        val playback = assertIs<PlaybackUpdateLocal>(
            receive(
                """{"type":"playback_update","playing":[
                    {"url":"https://media.example.test/clip.mp4","loop_count":null}]}"""
            )
        ).playing.single()

        assertEquals(
            PlaybackUpdateLocal.PlaybackInternal(
                url = "https://media.example.test/clip.mp4",
                name = null,
                playId = null,
                replacementId = null,
                audio = false,
                loopCount = 0
            ),
            playback
        )
    }

    @Test
    fun recordingUpdateMapsTheFinishedRecording() = runTest {
        assertEquals(
            RecordingLocal(
                id = "recording-1",
                duration = 5,
                downloadLink = RECORDING_DOWNLOAD,
                // created_at is in seconds.
                createdAt = date("2023-12-11T09:50:18Z"),
                user = JOHN_DOE
            ),
            receive(meetingFixture("recording_update"))
        )
    }

    @Test
    fun recordingUpdateOfARunningRecordingHasNoDurationOrDownload() = runTest {
        val running = meetingFixture("recording_update")
            .replacing("\"duration\": 5", "\"duration\": null")
            .replacing("\"$RECORDING_DOWNLOAD\"", "null")

        val recording = assertIs<RecordingLocal>(receive(running))

        assertEquals(null, recording.duration)
        assertEquals(null, recording.downloadLink)
    }

    @Test
    fun broadcastsUpdateMapsTheBroadcastsAndNothingOnceTheyEnded() = runTest {
        assertEquals(
            listOf(
                BroadcastUpdateLocal(
                    listOf(
                        BroadcastUpdateLocal.Broadcast(
                            id = "generic",
                            platform = "generic",
                            playerUrl = "",
                            user = JOHN_DOE
                        )
                    )
                ),
                BroadcastUpdateLocal(emptyList())
            ),
            receiveAll(
                meetingFixture("broadcasts_update"),
                """{"type":"broadcasts_update","broadcasts":[]}"""
            )
        )
    }

    @Test
    fun presentationUpdateMapsThePresenterAndNullOnceItStopped() = runTest {
        assertEquals(
            listOf(PresentationUpdateLocal(user = JOHN_DOE), PresentationUpdateLocal(user = null)),
            receiveAll(
                meetingFixture("presentation_update"),
                """{"type":"presentation_update","presentation":null}"""
            )
        )
    }

    @Test
    fun optionsUpdateMapsTheOptions() = runTest {
        assertEquals(
            OptionsUpdateLocal(
                MeetingOptions(
                    backgroundColor = "#121212",
                    broadcastAvailable = true,
                    customFields = emptyMap(),
                    exitUrl = null,
                    kickAvailable = true,
                    layout = "auto",
                    layoutAvailable = true,
                    layoutMap = emptyList(),
                    layoutName = "nine",
                    layoutUsers = List(9) { "" },
                    lockAvailable = true,
                    reactionAvailable = true,
                    recordingAvailable = true,
                    sfuMode = "ptp",
                    showLabel = true,
                    showNames = true,
                    suggestGuestNames = true,
                    voiceActivation = false,
                    widescreen = true
                )
            ),
            receive(meetingFixture("options_update"))
        )
    }

    @Test
    fun layoutMapKeepsOnlyCompleteRectangles() = runTest {
        val options = meetingFixture("options_update")
            .replacing("\"layout_map\": null", "\"layout_map\": [[0, 0, 640, 360], [640, 0, 640]]")

        assertEquals(
            listOf(MeetingOptions.LayoutMapEntry(x = 0, y = 0, width = 640, height = 360)),
            assertIs<OptionsUpdateLocal>(receive(options)).meetingOptions.layoutMap
        )
    }

    @Test
    fun muteAllMapsTheUserWhoRequestedIt() = runTest {
        assertEquals(
            MuteLocalAudioLocal(
                UserInfo(
                    id = "user-2",
                    name = "Jane Doe",
                    avatar = "https://media.example.test/avatars/user-2?ts=1793710516",
                    guest = false,
                    joinedAt = date("2026-11-03T12:55:49.298Z")
                )
            ),
            receive(meetingFixture("stfu"))
        )
    }

    @Test
    fun roomSetupCarriesTheMeetingBeforeItIsReady() = runTest {
        val meeting = assertIs<RoomSetup>(receive(meetingFixture("room_setup"))).meeting

        assertFalse(meeting.ready)
        assertFalse(meeting.room.ready)
        assertFalse(meeting.user.ready)
        // Account users are identified by their email.
        assertEquals("john.doe@example.test", meeting.user.id)
        assertEquals("test-setup-token", meeting.signaling.options.authToken)
        assertEquals(7, meeting.options.customFields?.size)
        assertEquals("en", meeting.options.customFields?.get("locale"))
    }

    @Test
    fun roomReadyCarriesTheReadyMeetingWithANewSignalingToken() = runTest {
        val meeting = assertIs<RoomReady>(receive(meetingFixture("room_ready"))).meeting

        assertTrue(meeting.ready)
        assertTrue(meeting.room.ready)
        assertTrue(meeting.user.ready)
        // Not the one of room_setup, so signaling has to connect with this meeting.
        assertEquals("test-ready-token", meeting.signaling.options.authToken)
        assertEquals("wss://sig.example.test/call", meeting.signaling.options.endpoint)
        assertEquals("https://api.example.test/rt?access_key=test-access-key", meeting.links.websocket)
    }

    /**
     * Opens the meeting socket, lets the server send each of [messages] in the RoomChannel
     * envelope and returns what the connection emits for them.
     */
    private suspend fun receiveAll(vararg messages: String): List<LocalBaseCommand> {
        server.enqueue(serverSocket.upgrade())
        var received = emptyList<LocalBaseCommand>()
        connection.events.test {
            connection.connect()
            assertEquals(WsOpen, awaitItem())

            messages.forEach { serverSocket.send(envelope(it)) }

            received = List(messages.size) { awaitItem() }
            expectNoEvents()
        }
        return received
    }

    private suspend fun receive(message: String): LocalBaseCommand = receiveAll(message).single()

    private fun meetingFixture(type: String): String = fixture("$type.json", dir = "meeting")

    private companion object {
        const val RECORDING_DOWNLOAD = "https://fs.example.test/meetings/meeting-1/" +
            "recording-1.webm?X-Amz-Algorithm=AWS4-HMAC-SHA256"

        val JOHN_DOE = UserInfo(
            id = "user-1",
            name = "John Doe",
            avatar = null,
            guest = false,
            joinedAt = date("2023-12-11T09:48:34.433Z")
        )

        fun date(text: String): Date = Date.from(Instant.parse(text))
    }
}

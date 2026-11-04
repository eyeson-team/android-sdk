package com.eyeson.sdk.network

import app.cash.turbine.test
import com.eyeson.sdk.di.NetworkModule
import com.eyeson.sdk.model.local.base.LocalBaseCommand
import com.eyeson.sdk.model.local.sepp.CallAccepted
import com.eyeson.sdk.model.local.sepp.CallRejected
import com.eyeson.sdk.model.local.sepp.CallResumed
import com.eyeson.sdk.model.local.sepp.CallTerminated
import com.eyeson.sdk.model.local.sepp.ChatIncoming
import com.eyeson.sdk.model.local.sepp.MemberListUpdate
import com.eyeson.sdk.model.local.sepp.RecordingStatusUpdate
import com.eyeson.sdk.model.local.sepp.SdpUpdate
import com.eyeson.sdk.model.local.sepp.SourceUpdate
import com.eyeson.sdk.model.local.ws.WsOpen
import com.eyeson.sdk.model.sepp.base.UnknownCommandDto
import com.eyeson.sdk.model.sepp.incoming.SeppCommandsIncoming
import com.eyeson.sdk.model.sepp.shared.SeppCommandsShared
import com.eyeson.sdk.serializer.SeppIncomingCommandsSerializer
import com.eyeson.sdk.utils.TestLogger
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import java.time.Instant
import java.util.Date
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertIsNot
import kotlin.test.assertNotNull

/**
 * Pins how each command type of the signaling socket reaches [SignalingConnection.events]. A
 * command whose fields don't match is dropped without an error, so a mismatch has to show up here.
 *
 * The SEPP protocol has no public reference. The fixtures in `resources/signaling/` follow
 * messages of live meetings, with identifiers replaced and each SDP cut down to its first lines.
 * `src` of a `source_update` uses the signaling ids `signaling-2`, `signaling-3`, our own
 * `client-1` and the playback `media-playback-1`.
 */
class SignalingMessagesTest {

    private val server = MockWebServer()
    private val serverSocket = ServerSocket()
    private lateinit var connection: SignalingConnection

    @BeforeTest
    fun setUp() {
        TestLogger.install()
        server.start()
        connection = SignalingConnection(testMeeting(server))
    }

    @AfterTest
    fun tearDown() {
        connection.disconnect()
        server.close()
    }

    @Test
    fun everyCommandTypeHasAFixtureThatDecodes() {
        val types = SeppCommandsIncoming.entries.map { it.type } +
            SeppCommandsShared.entries.map { it.type }

        types.forEach { type ->
            val command = NetworkModule.json.decodeFromString(
                SeppIncomingCommandsSerializer,
                signalingFixture(type)
            )

            assertIsNot<UnknownCommandDto>(command, "$type has no serializer branch")
            assertEquals(type, command.type)
        }
    }

    @Test
    fun callAcceptedMapsCallIdAndAnswer() = runTest {
        assertEquals(
            CallAccepted(callId = "call-1", sdp = ANSWER),
            receive(signalingFixture("call_accepted"))
        )
    }

    @Test
    fun callRejectedMapsTheRejectCode() = runTest {
        assertEquals(CallRejected(rejectCode = 403), receive(signalingFixture("call_rejected")))
    }

    @Test
    fun callTerminatedMapsTheTerminationCode() = runTest {
        assertEquals(
            CallTerminated(terminateCode = 200),
            receive(signalingFixture("call_terminated"))
        )
    }

    @Test
    fun callResumedMapsCallIdAndAnswer() = runTest {
        assertEquals(
            CallResumed(callId = "call-1", sdp = ANSWER),
            receive(signalingFixture("call_resumed"))
        )
    }

    @Test
    fun sdpUpdateMapsTheOfferWithItsLineBreaks() = runTest {
        // WebRTC needs the CRLF line breaks the JSON escapes.
        assertEquals(
            SdpUpdate(
                callId = "call-1",
                sdp = "v=0\r\no=- 4002704705 4002704706 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\n" +
                    "a=sfu-mode:on\r\n"
            ),
            receive(signalingFixture("sdp_update"))
        )
    }

    @Test
    fun chatMapsSenderAndATimestampWithoutFraction() = runTest {
        assertEquals(
            ChatIncoming(
                userId = "signaling-2",
                content = "This is a chat message.",
                messageId = "",
                timestamp = Date.from(Instant.parse("2022-02-17T09:45:06Z"))
            ),
            receive(signalingFixture("chat"))
        )
    }

    @Test
    fun sourceUpdateMarksNoPresenterAndNoScreenShareWithMinusOne() = runTest {
        // EyesonMeeting treats -1 like null: nobody is sharing their screen.
        assertEquals(
            SourceUpdate(
                sources = listOf("signaling-2", "signaling-3", "client-1"),
                videSources = listOf(2, 1),
                desktopStreamingId = -1,
                presenter = -1
            ),
            receive(signalingFixture("source_update"))
        )
    }

    @Test
    fun sourceUpdateDuringScreenSharePointsAtTheSharer() = runTest {
        val update = assertIs<SourceUpdate>(
            receive(signalingFixture("source_update_screen_share"))
        )

        assertEquals(listOf(1), update.videSources)
        assertEquals(1, update.presenter)
        assertEquals("signaling-3", update.sources[assertNotNull(update.desktopStreamingId)])
    }

    @Test
    fun sourceUpdateListsAPlaybackAsVideoSource() = runTest {
        val update = assertIs<SourceUpdate>(receive(signalingFixture("source_update_playback")))

        assertEquals(
            listOf("signaling-3", "client-1", "media-playback-1"),
            update.videSources.map { update.sources[it] }
        )
        assertEquals(-1, update.desktopStreamingId)
    }

    @Test
    fun memberListDuringPlaybackListsItsPlayIdButDoesNotCountIt() = runTest {
        assertEquals(
            MemberListUpdate(
                added = emptyList(),
                deleted = emptyList(),
                memberCountAfterUpdate = 3,
                mediaPlayIds = listOf("media-playback-1")
            ),
            receive(signalingFixture("memberlist"))
        )
    }

    @Test
    fun memberListMapsLeavingAndJoiningClients() = runTest {
        assertEquals(
            listOf(
                MemberListUpdate(
                    added = emptyList(),
                    deleted = listOf("signaling-3"),
                    memberCountAfterUpdate = 2,
                    mediaPlayIds = emptyList()
                ),
                MemberListUpdate(
                    added = listOf("signaling-3"),
                    deleted = emptyList(),
                    memberCountAfterUpdate = 3,
                    mediaPlayIds = emptyList()
                )
            ),
            receiveAll(signalingFixture("memberlist_leave"), signalingFixture("memberlist_join"))
        )
    }

    @Test
    fun recordingMapsActiveAndEnabled() = runTest {
        assertEquals(
            RecordingStatusUpdate(active = true, enabled = true),
            receive(signalingFixture("recording"))
        )
    }

    /**
     * Opens the signaling socket, lets the server send each of [commands] and returns what the
     * connection emits for them.
     */
    private suspend fun receiveAll(vararg commands: String): List<LocalBaseCommand> {
        server.enqueue(serverSocket.upgrade())
        var received = emptyList<LocalBaseCommand>()
        connection.events.test {
            connection.connect()
            assertEquals(WsOpen, awaitItem())

            commands.forEach { serverSocket.send(it) }

            received = List(commands.size) { awaitItem() }
            expectNoEvents()
        }
        return received
    }

    private suspend fun receive(command: String): LocalBaseCommand = receiveAll(command).single()

    private fun signalingFixture(name: String): String = fixture("$name.json", dir = "signaling")

    private companion object {
        const val ANSWER = "v=0\r\no=- 3852782210 3852782210 IN IP4 127.0.0.1\r\n"
    }
}

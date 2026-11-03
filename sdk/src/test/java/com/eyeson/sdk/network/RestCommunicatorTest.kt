package com.eyeson.sdk.network

import android.os.Build
import com.eyeson.sdk.BuildConfig
import com.eyeson.sdk.exceptions.internal.FaultyInfoException
import com.eyeson.sdk.utils.TestLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.Headers.Companion.headersOf
import java.net.URLDecoder
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Pins what [RestCommunicator] sends to and accepts from the eyeson API, independent of the HTTP
 * client underneath it.
 *
 * Join failures are mapped by exception type: [FaultyInfoException] carries the status code of a
 * rejected request, anything else counts as a generic error. The fire-and-forget requests return
 * the status code, or null if no response arrived.
 *
 * The fixtures in `resources/rest/` follow the example responses of the REST reference
 * (https://docs.eyeson.com/docs/rest/rest-reference), with identifiers and tokens replaced.
 */
class RestCommunicatorTest {

    private val server = MockWebServer()
    private lateinit var communicator: RestCommunicator

    @BeforeTest
    fun setUp() {
        TestLogger.install()
        server.start()
        communicator = RestCommunicator(server.url("/").toString())
    }

    @AfterTest
    fun tearDown() {
        server.close()
    }

    @Test
    fun getMeetingInfoGetsRoomAndParsesMeeting() = runTest {
        server.enqueue(jsonResponse(body = fixture("meeting.json")))

        val meeting = communicator.getMeetingInfo("test-access-key")

        val request = takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/rooms/test-access-key", request.target)
        assertEquals(listOf("application/json"), request.headers.values("Accept"))
        assertEquals(
            "eyeson-android-sdk/${BuildConfig.SDK_VERSION} (Android ${Build.VERSION.SDK_INT})",
            request.headers["User-Agent"]
        )

        assertEquals("test-access-key", meeting.accessKey)
        assertEquals("room-1", meeting.room.id)
        assertEquals(Instant.parse("2025-03-13T15:50:21.347Z").toEpochMilli(), meeting.room.startedAt.time)
        assertEquals("test-guest-token", meeting.room.guestToken)
        assertEquals("user-1", meeting.user.id)
        assertNull(meeting.user.avatar)
        assertEquals("https://app.example.test/?guest=test-guest-token", meeting.links.guestJoin)
        assertEquals("https://api.example.test/rt?access_key=test-access-key", meeting.links.websocket)
        assertNull(meeting.recording)
        assertEquals(emptyList(), meeting.playbacks)
        assertEquals("wss://sig.example.test/call", meeting.signaling.options.endpoint)
        assertEquals("test-auth-token", meeting.signaling.options.authToken)
        assertEquals(
            listOf(
                "turn:nuts.example.test:3478",
                "turn:nuts.example.test:3478?transport=tcp",
                "turns:nuts.example.test:443?transport=tcp",
            ),
            meeting.signaling.options.turnServers.single().urls
        )
        assertEquals("disabled", meeting.options.sfuMode)
    }

    @Test
    fun meetingWithActiveRecordingAndSnapshotParses() = runTest {
        server.enqueue(jsonResponse(body = fixture("meeting_recording.json")))

        val meeting = communicator.getMeetingInfo("test-access-key")

        val recording = assertNotNull(meeting.recording)
        assertEquals("recording-1", recording.id)
        // Seconds since the epoch, as the API documents it.
        assertEquals(1742384442L, recording.createdAt)
        assertNull(recording.duration)
        assertNull(recording.links.download)
        assertEquals("user-1", recording.user.id)
        val snapshot = meeting.snapshots.single()
        assertEquals("snapshot-1", snapshot.id)
        assertEquals(Instant.parse("2025-03-19T12:45:05.275Z").toEpochMilli(), snapshot.createdAt.time)
        assertEquals("user-1", snapshot.creator.id)
    }

    @Test
    fun pathParametersArePercentEncoded() = runTest {
        server.enqueue(jsonResponse(body = fixture("meeting.json")))

        communicator.getMeetingInfo("key with space")

        assertEquals("/rooms/key%20with%20space", takeRequest().target)
    }

    @Test
    fun getMeetingInfoAsGuestPostsForm() = runTest {
        server.enqueue(jsonResponse(code = 201, body = fixture("meeting.json")))

        val meeting = communicator.getMeetingInfoAsGuest(
            guestToken = "test-guest-token",
            name = "Jane Doe & Co",
            id = "external-1",
            avatar = "https://media.example.test/avatar.png?size=64",
        )

        val request = takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/guests/test-guest-token", request.target)
        assertFormContentType(request)
        assertEquals(
            mapOf(
                "name" to "Jane Doe & Co",
                "id" to "external-1",
                "avatar" to "https://media.example.test/avatar.png?size=64",
            ),
            request.formFields()
        )
        assertEquals("test-access-key", meeting.accessKey)
    }

    @Test
    fun getMeetingInfoAsGuestOmitsNullFields() = runTest {
        server.enqueue(jsonResponse(code = 201, body = fixture("meeting.json")))

        communicator.getMeetingInfoAsGuest("test-guest-token", "Jane", id = null, avatar = null)

        assertEquals(mapOf("name" to "Jane"), takeRequest().formFields())
    }

    @Test
    fun startPermalinkMeetingPostsAndParsesMeeting() = runTest {
        server.enqueue(jsonResponse(code = 201, body = fixture("meeting.json")))

        val meeting = communicator.startPermalinkMeeting("test-user-token")

        val request = takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/permalink/test-user-token", request.target)
        assertEquals("test-access-key", meeting.accessKey)
    }

    @Test
    fun meetingRequestsThrowFaultyInfoExceptionWithStatusCode() = runTest {
        server.enqueue(jsonResponse(code = 404, body = """{"error":"not found"}"""))
        server.enqueue(MockResponse(code = 401))
        server.enqueue(MockResponse(code = 410))

        assertEquals(404, assertFailsWith<FaultyInfoException> {
            communicator.getMeetingInfo("test-access-key")
        }.code)
        assertEquals(401, assertFailsWith<FaultyInfoException> {
            communicator.getMeetingInfoAsGuest("test-guest-token", "Jane", null, null)
        }.code)
        assertEquals(410, assertFailsWith<FaultyInfoException> {
            communicator.startPermalinkMeeting("test-user-token")
        }.code)
    }

    @Test
    fun meetingRequestWithUnparsableBodyThrowsGenericError() = runTest {
        server.enqueue(jsonResponse(body = """{"access_key":"test-access-key"}"""))

        val error = assertFails { communicator.getMeetingInfo("test-access-key") }

        assertFalse(error is FaultyInfoException)
    }

    @Test
    fun meetingRequestWithoutResponseThrowsGenericError() = runTest {
        server.close()

        val error = assertFails { communicator.getMeetingInfo("test-access-key") }

        assertFalse(error is FaultyInfoException)
    }

    @Test
    fun invalidApiUrlFailsInsideTheRequests() = runTest {
        val communicator = RestCommunicator("not a url")

        val error = assertFails { communicator.getMeetingInfo("test-access-key") }
        assertFalse(error is FaultyInfoException)
        assertNull(communicator.sendChatMessage("test-access-key", "hello"))
    }

    @Test
    fun apiUrlWithoutTrailingSlashIsAccepted() = runTest {
        server.enqueue(MockResponse(code = 201))
        val communicator = RestCommunicator(server.url("/").toString().removeSuffix("/"))

        assertEquals(201, communicator.sendChatMessage("test-access-key", "hello"))
        assertEquals("/rooms/test-access-key/messages", takeRequest().target)
    }

    @Test
    fun pathOfApiUrlIsDropped() = runTest {
        server.enqueue(MockResponse(code = 201))
        val communicator = RestCommunicator(server.url("/base/").toString())

        assertEquals(201, communicator.sendChatMessage("test-access-key", "hello"))
        assertEquals("/rooms/test-access-key/messages", takeRequest().target)
    }

    @Test
    fun sendChatMessagePostsFormAndReturnsStatusCode() = runTest {
        server.enqueue(MockResponse(code = 201))

        val code = communicator.sendChatMessage("test-access-key", "Hello & welcome = 100%")

        val request = takeRequest()
        assertEquals(201, code)
        assertEquals("POST", request.method)
        assertEquals("/rooms/test-access-key/messages", request.target)
        assertFormContentType(request)
        assertEquals(
            mapOf("type" to "chat", "content" to "Hello & welcome = 100%"),
            request.formFields()
        )
    }

    @Test
    fun sendCustomMessagePostsCustomType() = runTest {
        server.enqueue(MockResponse(code = 201))

        val code = communicator.sendCustomMessage("test-access-key", """{"foo":"bar"}""")

        assertEquals(201, code)
        assertEquals(
            mapOf("type" to "custom", "content" to """{"foo":"bar"}"""),
            takeRequest().formFields()
        )
    }

    @Test
    fun errorStatusIsReturnedAsIs() = runTest {
        server.enqueue(jsonResponse(code = 403, body = """{"error":"forbidden"}"""))

        assertEquals(403, communicator.sendChatMessage("test-access-key", "hello"))
    }

    @Test
    fun requestWithoutResponseReturnsNullAndLogs() = runTest {
        server.close()

        assertNull(communicator.sendChatMessage("test-access-key", "hello"))
        assertTrue(TestLogger.messages.any { it.startsWith("Chat message failed") })
    }

    @Test
    fun cancellationIsNotTurnedIntoNull() = runTest {
        server.enqueue(MockResponse.Builder().code(201).headersDelay(2, TimeUnit.SECONDS).build())
        var completed = false

        val request = launch(Dispatchers.Default) {
            communicator.sendChatMessage("test-access-key", "hello")
            completed = true
        }
        takeRequest()
        request.cancelAndJoin()

        assertFalse(completed)
        assertFalse(TestLogger.messages.any { it.startsWith("Chat message failed") })
    }

    @Test
    fun successfulResponseThatIsNotJsonIsAGenericError() = runTest {
        // Like the sign-in page of a captive portal.
        val portalPage = MockResponse(
            code = 200,
            headers = headersOf("Content-Type", "text/html"),
            body = "<html><body>Sign in to the Wi-Fi</body></html>"
        )
        server.enqueue(portalPage)
        server.enqueue(portalPage)

        val error = assertFails { communicator.getMeetingInfo("test-access-key") }
        assertFalse(error is FaultyInfoException)
        assertNull(communicator.getUserInfo("test-access-key", "signaling-2"))
    }

    @Test
    fun videoPlaybackPostsAllFields() = runTest {
        server.enqueue(MockResponse(code = 201))

        val code = communicator.videoPlayback(
            accessKey = "test-access-key",
            url = "https://media.example.test/clip.mp4?token=a&b=c",
            name = "Clip",
            playId = "play-1",
            replacementId = "user-1",
            audio = true,
            loopCount = -1,
        )

        val request = takeRequest()
        assertEquals(201, code)
        assertEquals("POST", request.method)
        assertEquals("/rooms/test-access-key/playbacks", request.target)
        assertFormContentType(request)
        assertEquals(
            mapOf(
                "url" to "https://media.example.test/clip.mp4?token=a&b=c",
                "play_id" to "play-1",
                "replacement_id" to "user-1",
                "name" to "Clip",
                "audio" to "true",
                "loop_count" to "-1",
            ),
            request.formFields()
        )
    }

    @Test
    fun videoPlaybackOmitsNullFields() = runTest {
        server.enqueue(MockResponse(code = 201))

        communicator.videoPlayback(
            accessKey = "test-access-key",
            url = "https://media.example.test/clip.mp4",
            name = null,
            playId = null,
            replacementId = null,
            audio = false,
            loopCount = 0,
        )

        assertEquals(
            mapOf(
                "url" to "https://media.example.test/clip.mp4",
                "audio" to "false",
                "loop_count" to "0",
            ),
            takeRequest().formFields()
        )
    }

    @Test
    fun stopVideoPlaybackDeletesPlayback() = runTest {
        server.enqueue(MockResponse(code = 200))

        val code = communicator.stopVideoPlayback("test-access-key", "play-1")

        val request = takeRequest()
        assertEquals(200, code)
        assertEquals("DELETE", request.method)
        assertEquals("/rooms/test-access-key/playbacks/play-1", request.target)
    }

    @Test
    fun presentationStartPostsAndStopDeletes() = runTest {
        server.enqueue(MockResponse(code = 201))
        server.enqueue(MockResponse(code = 200))

        assertEquals(201, communicator.startPresentation("test-access-key"))
        assertEquals(200, communicator.stopPresentation("test-access-key"))

        val start = takeRequest()
        assertEquals("POST", start.method)
        assertEquals("/rooms/test-access-key/presentation", start.target)
        val stop = takeRequest()
        assertEquals("DELETE", stop.method)
        assertEquals("/rooms/test-access-key/presentation", stop.target)
    }

    @Test
    fun getUserInfoGetsUserAndKeepsSignalingId() = runTest {
        server.enqueue(jsonResponse(body = fixture("user.json")))

        val user = assertNotNull(communicator.getUserInfo("test-access-key", "signaling-2"))

        val request = takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/rooms/test-access-key/users/signaling-2", request.target)
        assertEquals("signaling-2", user.signalingId)
        assertEquals("user-2", user.user.id)
        assertEquals("Jane Doe", user.user.name)
        assertEquals("https://media.example.test/avatar.png", user.user.avatar)
        assertTrue(user.user.guest)
        assertEquals(Instant.parse("2025-03-17T09:07:50.743Z").toEpochMilli(), user.user.joinedAt.time)
    }

    @Test
    fun getUserInfoReturnsNullOnErrorStatusOrUnparsableBody() = runTest {
        server.enqueue(MockResponse(code = 404))
        server.enqueue(jsonResponse(body = """{"id":"user-2"}"""))

        assertNull(communicator.getUserInfo("test-access-key", "signaling-2"))
        assertNull(communicator.getUserInfo("test-access-key", "signaling-2"))
    }

    @Test
    fun getUserInfoBatchSkipsFailedLookupsAndKeepsOrder() = runTest {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                when (request.target.substringAfterLast('/')) {
                    "missing" -> MockResponse(code = 404)
                    else -> jsonResponse(body = fixture("user.json"))
                }
        }

        val users = communicator.getUserInfo("test-access-key", listOf("first", "missing", "last"))

        assertEquals(listOf("first", "last"), users.map { it.signalingId })
    }

    @Test
    fun getPermalinkMeetingInfoGetsAndMapsPermalink() = runTest {
        server.enqueue(jsonResponse(body = fixture("permalink.json")))

        val info = assertNotNull(communicator.getPermalinkMeetingInfo("test-permalink-token"))

        val request = takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/permalink/test-permalink-token", request.target)
        assertEquals("permalink-1", info.id)
        assertEquals("test-guest-token", info.guestToken)
        assertNull(info.expiresAt)
        assertEquals("Test Room", info.room.name)
        assertNull(info.room.startedAt)
        assertEquals("Test Team", info.team.name)
        assertEquals(emptyList(), info.options.layoutMap)
    }

    @Test
    fun getPermalinkMeetingInfoReturnsNullOnErrorStatus() = runTest {
        server.enqueue(MockResponse(code = 404))

        assertNull(communicator.getPermalinkMeetingInfo("test-permalink-token"))
    }

    private fun takeRequest(): RecordedRequest =
        server.takeRequest(5, TimeUnit.SECONDS) ?: fail("No request reached the server")

    private fun assertFormContentType(request: RecordedRequest) {
        val contentType = request.headers["Content-Type"]
        assertEquals(
            true,
            contentType?.startsWith("application/x-www-form-urlencoded"),
            "Content-Type was $contentType"
        )
    }

    private fun RecordedRequest.formFields(): Map<String, String> =
        body?.utf8().orEmpty()
            .split('&')
            .filter { it.isNotEmpty() }
            .associate {
                val (name, value) = it.split('=', limit = 2)
                URLDecoder.decode(name, "UTF-8") to URLDecoder.decode(value, "UTF-8")
            }

    // Like the API, the response states its content type; Ktor decodes only a JSON one.
    private fun jsonResponse(code: Int = 200, body: String): MockResponse =
        MockResponse(code = code, headers = headersOf("Content-Type", "application/json"), body = body)

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResource("/rest/$name")) { "Missing fixture $name" }.readText()
}

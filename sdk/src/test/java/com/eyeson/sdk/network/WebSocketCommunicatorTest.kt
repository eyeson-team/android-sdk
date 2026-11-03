package com.eyeson.sdk.network

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.eyeson.sdk.di.NetworkModule
import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.local.base.LocalBaseCommand
import com.eyeson.sdk.model.local.call.ResumeCallLocal
import com.eyeson.sdk.model.local.call.StartCallLocal
import com.eyeson.sdk.model.local.meeting.MuteLocalAudioLocal
import com.eyeson.sdk.model.local.sepp.CallAccepted
import com.eyeson.sdk.model.local.sepp.CallStart
import com.eyeson.sdk.model.local.ws.ReconnectSignaling
import com.eyeson.sdk.model.local.ws.WsFailure
import com.eyeson.sdk.model.local.ws.WsOpen
import com.eyeson.sdk.utils.TestLogger
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.fail

/**
 * Pins how [WebSocketCommunicator] drives the meeting and signaling sockets: signaling connects
 * once the room is ready, each socket reconnects once after a failure, and calls are started,
 * resumed and terminated over signaling.
 */
class WebSocketCommunicatorTest {

    private val json = NetworkModule.json
    private val server = MockWebServer()
    private val meetingResponses = LinkedBlockingQueue<MockResponse>()
    private val signalingResponses = LinkedBlockingQueue<MockResponse>()
    private lateinit var meeting: MeetingDto
    private lateinit var readyMeeting: MeetingDto
    private lateinit var communicator: WebSocketCommunicator

    @BeforeTest
    fun setUp() {
        TestLogger.install()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val responses = when (request.url.encodedPath) {
                    "/rt" -> meetingResponses
                    "/call" -> signalingResponses
                    else -> return MockResponse(code = 404)
                }
                return responses.poll(5, TimeUnit.SECONDS) ?: MockResponse(code = 500)
            }
        }
        server.start()
        meeting = testMeeting(server)
        readyMeeting = testMeeting(server, authToken = "ready-token")
        communicator = WebSocketCommunicator(meeting)
    }

    @AfterTest
    fun tearDown() {
        communicator.terminateCall()
        server.close()
    }

    @Test
    fun roomReadyConnectsSignalingWithItsMeetingAndStartsCall() = runTest {
        communicator.events.test {
            startCall()
        }

        assertEquals("/rt", takeRequest().url.encodedPath)
        val signalingRequest = takeRequest()
        assertEquals("/call", signalingRequest.url.encodedPath)
        assertEquals("Bearer ready-token", signalingRequest.headers["Authorization"])
    }

    @Test
    fun eventsAreKeptUntilCollected() = runTest {
        val meetingSocket = enqueueSocket(meetingResponses)
        val signalingSocket = enqueueSocket(signalingResponses)

        communicator.connect()
        meetingSocket.send(roomReady(readyMeeting))
        signalingSocket.send(callAccepted("c1"))
        // Lets the call start before anything collects the events.
        Thread.sleep(300)

        communicator.events.test {
            assertEquals(WsOpen, awaitItem())
            assertEquals(StartCallLocal(readyMeeting), awaitItem())
            assertEquals(CallAccepted("c1", "v=0"), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun startCallSendsCallStartWithPlatformVersion() = runTest {
        communicator.events.test {
            val (_, signalingSocket) = startCall()

            communicator.startCall(CallStart("Jane", "v=0"), videoOnStart = false, version = "1.2.3")

            val callStart = signalingSocket.takeJson()
            assertEquals("call_start", callStart.string("type"))
            assertEquals("client-1", callStart.string("from"))
            assertEquals("conf-1", callStart.string("to"))
            val data = callStart.getValue("data").jsonObject
            assertEquals("android-1.2.3", data.string("platform"))
            assertEquals("Jane", data.string("display_name"))
            assertEquals("true", data.string("mute_video"))
            assertEquals("offer", data.getValue("sdp").jsonObject.string("type"))
            assertEquals("v=0", data.getValue("sdp").jsonObject.string("sdp"))
        }
    }

    @Test
    fun muteLocalAudioIsOnlyEmittedWhenRequestedByAnotherUser() = runTest {
        communicator.events.test {
            val (meetingSocket, _) = startCall()

            meetingSocket.send(muteLocalAudio(byUserId = "user-1"))
            meetingSocket.send(muteLocalAudio(byUserId = "user-2"))

            assertEquals("user-2", assertIs<MuteLocalAudioLocal>(awaitItem()).byUser.id)
            expectNoEvents()
        }
    }

    @Test
    fun sendMuteAllAndVideoMuteReachTheirSockets() = runTest {
        communicator.events.test {
            val (meetingSocket, signalingSocket) = startCall()

            communicator.sendMuteAll()
            communicator.setLocalVideoEnabled(false)

            assertEquals(SUBSCRIBE_ROOM_CHANNEL, meetingSocket.takeMessage())
            assertEquals(SUBSCRIBE_USER_CHANNEL, meetingSocket.takeMessage())
            assertEquals(
                """{"command":"message","identifier":"{\"channel\":\"RoomChannel\"}","data":"{\"message\":\"stfu\",\"action\":\"stfu\"}"}""",
                meetingSocket.takeMessage()
            )
            val muteVideo = signalingSocket.takeJson()
            assertEquals("mute_video", muteVideo.string("type"))
            assertEquals("true", muteVideo.getValue("data").jsonObject.string("on"))
            assertEquals("client-1", muteVideo.getValue("data").jsonObject.string("cid"))
        }
    }

    @Test
    fun droppedMeetingSocketReconnectsOnceThenReportsFailure() = runTest {
        communicator.events.test {
            val meetingSocket = enqueueSocket(meetingResponses)
            communicator.connect()
            assertEquals(WsOpen, awaitItem())

            val reconnectedSocket = enqueueSocket(meetingResponses)
            meetingSocket.drop()
            assertEquals(WsOpen, awaitItem())

            meetingResponses.put(MockResponse(code = 503))
            reconnectedSocket.drop()
            assertEquals(WsFailure, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun droppedSignalingSocketAsksForReconnectAndResumesCall() = runTest {
        communicator.events.test {
            val (_, signalingSocket) = startCall()
            signalingSocket.send(callAccepted("c1"))
            assertEquals(CallAccepted("c1", "v=0"), awaitItem())

            signalingSocket.drop()
            assertEquals(ReconnectSignaling, awaitItem())

            val newMeeting = testMeeting(server, authToken = "new-token")
            enqueueSocket(signalingResponses)
            communicator.reconnectToSignaling(newMeeting)
            assertEquals(ResumeCallLocal(newMeeting, "c1"), awaitItem())
        }

        val signalingRequests = List(server.requestCount) { takeRequest() }
            .filter { it.url.encodedPath == "/call" }
        assertEquals(
            listOf("Bearer ready-token", "Bearer new-token"),
            signalingRequests.map { it.headers["Authorization"] }
        )
    }

    @Test
    fun failedSignalingReconnectIsReported() = runTest {
        communicator.events.test {
            val (_, signalingSocket) = startCall()

            signalingSocket.drop()
            assertEquals(ReconnectSignaling, awaitItem())

            signalingResponses.put(MockResponse(code = 503))
            communicator.reconnectToSignaling(readyMeeting)
            assertEquals(WsFailure, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun terminateCallSendsCallTerminateAndClosesBothSockets() = runTest {
        communicator.events.test {
            val (meetingSocket, signalingSocket) = startCall()
            signalingSocket.send(callAccepted("c1"))
            assertEquals(CallAccepted("c1", "v=0"), awaitItem())

            communicator.terminateCall()

            val callTerminate = signalingSocket.takeJson()
            assertEquals("call_terminate", callTerminate.string("type"))
            assertEquals("c1", callTerminate.getValue("data").jsonObject.string("call_id"))
            assertEquals("200", callTerminate.getValue("data").jsonObject.string("term_code"))
            assertEquals(1001 to "BYE", signalingSocket.takeClosing())
            assertEquals(1001 to "BYE", meetingSocket.takeClosing())
            expectNoEvents()
        }
    }

    /**
     * Connects, lets the meeting socket report the room as ready and waits for the call to start.
     * Returns the server side of the meeting and the signaling socket.
     */
    private suspend fun ReceiveTurbine<LocalBaseCommand>.startCall(): Pair<ServerSocket, ServerSocket> {
        val meetingSocket = enqueueSocket(meetingResponses)
        val signalingSocket = enqueueSocket(signalingResponses)

        communicator.connect()
        assertEquals(WsOpen, awaitItem())

        meetingSocket.send(roomReady(readyMeeting))
        assertEquals(StartCallLocal(readyMeeting), awaitItem())
        return meetingSocket to signalingSocket
    }

    private fun enqueueSocket(responses: LinkedBlockingQueue<MockResponse>): ServerSocket {
        val socket = ServerSocket()
        responses.put(socket.upgrade())
        return socket
    }

    private fun takeRequest(): RecordedRequest =
        server.takeRequest(5, TimeUnit.SECONDS) ?: fail("No request reached the server")

    private fun ServerSocket.takeJson(): JsonObject =
        json.parseToJsonElement(takeMessage()).jsonObject

    private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content

    private fun roomReady(meeting: MeetingDto): String =
        """{"identifier":"{\"channel\":\"RoomChannel\"}","message":{"type":"room_ready",
            "content":${json.encodeToString(MeetingDto.serializer(), meeting)}}}"""

    private fun muteLocalAudio(byUserId: String): String =
        """{"identifier":"{\"channel\":\"RoomChannel\"}","message":{"type":"stfu",
            "user":{"id":"$byUserId","name":"Jane Doe","avatar":null,"guest":false,
            "joined_at":"2025-03-17T09:07:50.743Z"}}}"""

    private fun callAccepted(callId: String): String =
        """{"type":"call_accepted","msg_id":"m1","from":"conf","to":"client",
            "data":{"call_id":"$callId","sdp":{"type":"answer","sdp":"v=0"}}}"""
}

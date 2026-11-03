package com.eyeson.sdk.network

import app.cash.turbine.test
import com.eyeson.sdk.model.local.sepp.CallAccepted
import com.eyeson.sdk.model.local.ws.WsClosed
import com.eyeson.sdk.model.local.ws.WsFailure
import com.eyeson.sdk.model.local.ws.WsOpen
import com.eyeson.sdk.utils.TestLogger
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pins how [SignalingConnection] talks to the signaling socket (`signaling.options.endpoint`),
 * independent of the WebSocket client underneath it.
 */
class SignalingConnectionTest {

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
    fun connectOpensEndpointWithBearerToken() = runTest {
        server.enqueue(serverSocket.upgrade())

        connection.events.test {
            connection.connect()
            assertEquals(WsOpen, awaitItem())
        }

        val request = assertNotNull(server.takeRequest(5, TimeUnit.SECONDS))
        assertEquals("/call", request.target)
        assertEquals("websocket", request.headers["Upgrade"])
        assertEquals("Bearer test-auth-token", request.headers["Authorization"])
    }

    @Test
    fun onlyKnownCommandsAreEmitted() = runTest {
        server.enqueue(serverSocket.upgrade())

        connection.events.test {
            connection.connect()
            assertEquals(WsOpen, awaitItem())

            serverSocket.send("""{"type":"something_new","data":{"x":1}}""")
            serverSocket.send("""{"type":""")
            serverSocket.send(
                """{"type":"call_accepted","msg_id":"m1","from":"conf","to":"client",
                    "data":{"call_id":"c1","sdp":{"type":"answer","sdp":"v=0"}}}"""
            )

            assertEquals(CallAccepted(callId = "c1", sdp = "v=0"), awaitItem())
            expectNoEvents()
        }
        assertTrue(TestLogger.messages.any { it.contains("parsing FAILED") })
    }

    @Test
    fun sendMessageReachesServer() = runTest {
        server.enqueue(serverSocket.upgrade())

        connection.events.test {
            connection.connect()
            assertEquals(WsOpen, awaitItem())
        }
        assertTrue(connection.sendMessage("""{"type":"call_start"}"""))

        assertEquals("""{"type":"call_start"}""", serverSocket.takeMessage())
    }

    @Test
    fun rejectedUpgradeEmitsFailure() = runTest {
        server.enqueue(MockResponse(code = 403))

        connection.events.test {
            connection.connect()
            assertEquals(WsFailure, awaitItem())
        }
    }

    @Test
    fun droppedConnectionEmitsFailure() = runTest {
        server.enqueue(serverSocket.upgrade())

        connection.events.test {
            connection.connect()
            assertEquals(WsOpen, awaitItem())

            serverSocket.drop()

            assertEquals(WsFailure, awaitItem())
        }
    }

    @Test
    fun closeFromServerIsAnsweredAndEmitsClosed() = runTest {
        server.enqueue(serverSocket.upgrade())

        connection.events.test {
            connection.connect()
            assertEquals(WsOpen, awaitItem())

            serverSocket.awaitOpen().close(1000, "bye")

            assertEquals(WsClosed(1000, "bye"), awaitItem())
            assertEquals(1000, serverSocket.awaitClosed().first)
        }
    }

    @Test
    fun disconnectClosesWithGoingAway() = runTest {
        server.enqueue(serverSocket.upgrade())

        connection.events.test {
            connection.connect()
            assertEquals(WsOpen, awaitItem())

            connection.disconnect()

            assertEquals(1001 to "BYE", serverSocket.takeClosing())
            expectNoEvents()
        }
    }
}

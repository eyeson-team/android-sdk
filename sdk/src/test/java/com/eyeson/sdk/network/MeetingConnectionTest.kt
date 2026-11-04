package com.eyeson.sdk.network

import android.os.Build
import app.cash.turbine.test
import com.eyeson.sdk.BuildConfig
import com.eyeson.sdk.model.local.meeting.MeetingLockedLocal
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
 * Pins how [MeetingConnection] talks to the meeting socket (`links.websocket`), independent of
 * the WebSocket client underneath it.
 */
class MeetingConnectionTest {

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
    fun connectOpensMeetingLinkAndSubscribesToRoomAndUserChannel() = runTest {
        server.enqueue(serverSocket.upgrade())

        connection.events.test {
            connection.connect()
            assertEquals(WsOpen, awaitItem())
        }

        val request = assertNotNull(server.takeRequest(5, TimeUnit.SECONDS))
        assertEquals("/rt?access_key=test-access-key", request.target)
        assertEquals("websocket", request.headers["Upgrade"])
        assertEquals(
            "eyeson-android-sdk/${BuildConfig.SDK_VERSION} (Android ${Build.VERSION.SDK_INT})",
            request.headers["User-Agent"]
        )
        assertEquals(SUBSCRIBE_ROOM_CHANNEL, serverSocket.takeMessage())
        assertEquals(SUBSCRIBE_USER_CHANNEL, serverSocket.takeMessage())
    }

    @Test
    fun eventsBeforeCollectionAreKeptInOrder() = runTest {
        server.enqueue(serverSocket.upgrade())
        val lockStates = List(20) { it % 2 == 0 }

        connection.connect()
        serverSocket.awaitOpen()
        lockStates.forEach { locked ->
            serverSocket.send(
                """{"identifier":"{\"channel\":\"RoomChannel\"}","message":{"type":"lock","locked":$locked}}"""
            )
        }
        // Lets the messages arrive before anything collects them.
        Thread.sleep(300)

        connection.events.test {
            assertEquals(WsOpen, awaitItem())
            lockStates.forEach { locked ->
                assertEquals(MeetingLockedLocal(locked = locked), awaitItem())
            }
            expectNoEvents()
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
    fun onlyKnownMessagesAreEmitted() = runTest {
        server.enqueue(serverSocket.upgrade())

        connection.events.test {
            connection.connect()
            assertEquals(WsOpen, awaitItem())

            serverSocket.send("""{"type":"welcome"}""")
            serverSocket.send("""{"type":"ping","message":1712345678}""")
            serverSocket.send(
                """{"identifier":"{\"channel\":\"RoomChannel\"}","type":"confirm_subscription"}"""
            )
            serverSocket.send("""{"identifier":"{\"channel\":\"RoomChannel\"}","message":{"type":"something_new"}}""")
            serverSocket.send("""{"message":""")
            serverSocket.send(
                """{"identifier":"{\"channel\":\"RoomChannel\"}","message":{"type":"lock","locked":true}}"""
            )

            assertEquals(MeetingLockedLocal(locked = true), awaitItem())
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
        assertTrue(connection.sendMessage("""{"command":"message"}"""))

        assertEquals(SUBSCRIBE_ROOM_CHANNEL, serverSocket.takeMessage())
        assertEquals(SUBSCRIBE_USER_CHANNEL, serverSocket.takeMessage())
        assertEquals("""{"command":"message"}""", serverSocket.takeMessage())
    }

    @Test
    fun rejectedUpgradeEmitsFailure() = runTest {
        server.enqueue(MockResponse(code = 401))

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

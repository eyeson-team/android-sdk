package com.eyeson.sdk.network

import app.cash.turbine.test
import com.eyeson.sdk.di.NetworkModule
import com.eyeson.sdk.model.local.base.LocalBaseCommand
import com.eyeson.sdk.model.local.ws.WsFailure
import com.eyeson.sdk.model.local.ws.WsOpen
import com.eyeson.sdk.utils.TestLogger
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okio.ByteString.Companion.encodeUtf8
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pins the lifecycle [WebSocketConnection] gives its subclasses, through a connection that emits
 * every text frame as is: messages are queued until the socket opens, only text frames are
 * decoded, and nothing is emitted or sent after [WebSocketConnection.disconnect].
 */
class WebSocketConnectionTest {

    private val server = MockWebServer()
    private val serverSocket = ServerSocket()
    private lateinit var connection: TextConnection

    @BeforeTest
    fun setUp() {
        TestLogger.install()
        server.start()
        connection = TextConnection(server.url("/socket").toString())
    }

    @AfterTest
    fun tearDown() {
        connection.disconnect()
        server.close()
    }

    @Test
    fun messagesSentBeforeOpenAreDeliveredInOrder() = runTest {
        server.enqueue(serverSocket.upgrade(headersDelayMillis = 300))

        connection.connect()
        assertTrue(connection.sendMessage("first"))
        assertTrue(connection.sendMessage("second"))

        assertEquals("first", serverSocket.takeMessage())
        assertEquals("second", serverSocket.takeMessage())
    }

    @Test
    fun sendMessageFailsAfterDisconnect() = runTest {
        server.enqueue(serverSocket.upgrade())

        connection.events.test {
            connection.connect()
            assertEquals(WsOpen, awaitItem())

            connection.disconnect()

            assertFalse(connection.sendMessage("too late"))
        }
    }

    @Test
    fun sendMessageFailsAfterTheConnectionFailed() = runTest {
        server.enqueue(MockResponse(code = 401))

        connection.events.test {
            connection.connect()
            assertEquals(WsFailure, awaitItem())
        }

        // The queue is closed right after WsFailure is emitted.
        awaitCondition("sendMessage is rejected") { !connection.sendMessage("nobody listens") }
    }

    @Test
    fun disconnectWhileConnectingEmitsNothing() = runTest {
        server.enqueue(serverSocket.upgrade(headersDelayMillis = 300))

        connection.events.test {
            connection.connect()
            assertNotNull(server.takeRequest(5, TimeUnit.SECONDS))

            connection.disconnect()
            // Lets the delayed handshake complete, were it still going.
            Thread.sleep(600)

            expectNoEvents()
        }
    }

    @Test
    fun binaryFramesAreIgnored() = runTest {
        server.enqueue(serverSocket.upgrade())

        connection.events.test {
            connection.connect()
            assertEquals(WsOpen, awaitItem())

            serverSocket.sendBinary("binary".encodeUtf8())
            serverSocket.send("text")

            assertEquals(Text("text"), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun closingHandshakeAfterDisconnectEmitsNothing() = runTest {
        server.enqueue(serverSocket.upgrade())

        connection.events.test {
            connection.connect()
            assertEquals(WsOpen, awaitItem())

            connection.disconnect()
            assertEquals(1001, serverSocket.awaitClosed().first)
            // Logged right before the end of the handshake would be emitted as WsClosed.
            awaitCondition("the client saw the close") {
                TestLogger.messages.any { it.startsWith("TextConnection onClosed") }
            }

            expectNoEvents()
        }
    }
}

/** Emits every text frame as [Text]. */
private class TextConnection(private val link: String) :
    WebSocketConnection(NetworkModule.httpClient, Dispatchers.IO) {

    override fun HttpRequestBuilder.configureRequest() {
        url(webSocketUrl(link))
    }

    override fun decode(text: String): LocalBaseCommand = Text(text)
}

private data class Text(val text: String) : LocalBaseCommand

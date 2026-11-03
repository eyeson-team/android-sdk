package com.eyeson.sdk.network

import com.eyeson.sdk.di.NetworkModule
import com.eyeson.sdk.model.api.MeetingDto
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.internal.ws.RealWebSocket
import okio.ByteString
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.fail

internal const val SUBSCRIBE_ROOM_CHANNEL =
    """{"command":"subscribe","identifier":"{\"channel\":\"RoomChannel\"}"}"""
internal const val SUBSCRIBE_USER_CHANNEL =
    """{"command":"subscribe","identifier":"{\"channel\":\"UserChannel\"}"}"""

private const val TIMEOUT_SECONDS = 5L

internal fun fixture(name: String, dir: String = "rest"): String =
    checkNotNull(object {}.javaClass.getResource("/$dir/$name")) { "Missing fixture $dir/$name" }
        .readText()

/** [message] in the ActionCable envelope the meeting socket wraps every RoomChannel message in. */
internal fun envelope(message: String): String =
    """{"identifier":"{\"channel\":\"RoomChannel\"}","message":$message}"""

/**
 * Replaces [old] with [new]. Fails if [old] is missing, so that a variant of a fixture can't
 * silently stop differing from it.
 */
internal fun String.replacing(old: String, new: String): String {
    check(old in this) { "'$old' not found" }
    return replace(old, new)
}

/** Polls until [condition] holds, for state that can only be observed from the outside. */
internal fun awaitCondition(description: String, condition: () -> Boolean) {
    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS)
    while (!condition()) {
        if (System.nanoTime() > deadline) fail("Timed out waiting until $description")
        Thread.sleep(10)
    }
}

/**
 * The meeting of `meeting.json`, with its meeting socket on [server] at `/rt` and its signaling
 * socket at `/call`. Like the API, the meeting link uses an HTTP scheme and the signaling endpoint
 * a WebSocket scheme.
 */
internal fun testMeeting(server: MockWebServer, authToken: String = "test-auth-token"): MeetingDto {
    val meeting = NetworkModule.json.decodeFromString(MeetingDto.serializer(), fixture("meeting.json"))
    val signalingOptions = meeting.signaling.options.copy(
        endpoint = server.url("/call").toString().replaceFirst("http", "ws"),
        authToken = authToken,
    )
    return meeting.copy(
        links = meeting.links.copy(websocket = server.url("/rt?access_key=test-access-key").toString()),
        signaling = meeting.signaling.copy(options = signalingOptions),
    )
}

/** The server side of one WebSocket connection, recording what the client sends. */
internal class ServerSocket : WebSocketListener() {
    private val opened = CompletableFuture<WebSocket>()
    private val messages = LinkedBlockingQueue<String>()
    private val closings = LinkedBlockingQueue<Pair<Int, String>>()
    private val closed = CompletableFuture<Pair<Int, String>>()

    fun upgrade(headersDelayMillis: Long = 0): MockResponse =
        MockResponse.Builder()
            .headersDelay(headersDelayMillis, TimeUnit.MILLISECONDS)
            .webSocketUpgrade(this)
            .build()

    fun awaitOpen(): WebSocket = opened.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)

    fun send(text: String) {
        check(awaitOpen().send(text))
    }

    fun sendBinary(bytes: ByteString) {
        check(awaitOpen().send(bytes))
    }

    /**
     * Ends the connection without a close frame, like a network failure. `cancel()` only works on
     * the client side, so this fails the server's socket instead.
     */
    fun drop() {
        (awaitOpen() as RealWebSocket).failWebSocket(IOException("dropped"), null)
    }

    fun takeMessage(): String =
        messages.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS) ?: fail("The client sent no message")

    /** Whether the client sent a message that hasn't been taken yet. */
    fun hasMessages(): Boolean = messages.isNotEmpty()

    fun takeClosing(): Pair<Int, String> =
        closings.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS) ?: fail("The client did not close")

    /** Waits until both sides have closed, which needs the client to answer a close frame. */
    fun awaitClosed(): Pair<Int, String> = closed.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)

    override fun onOpen(webSocket: WebSocket, response: Response) {
        opened.complete(webSocket)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        messages.put(text)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        closings.put(code to reason)
        webSocket.close(code, null)
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        closed.complete(code to reason)
    }
}

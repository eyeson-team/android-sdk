package com.eyeson.sdk.network

import com.eyeson.sdk.model.local.base.LocalBaseCommand
import com.eyeson.sdk.model.local.ws.WsClosed
import com.eyeson.sdk.model.local.ws.WsFailure
import com.eyeson.sdk.model.local.ws.WsOpen
import com.eyeson.sdk.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.isSecure
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

// The API hands out https:// links, but Ktor opens WebSockets only for ws:// and wss://.
internal fun webSocketUrl(link: String): Url =
    URLBuilder(link).apply {
        protocol = if (protocol.isSecure()) URLProtocol.WSS else URLProtocol.WS
    }.build()

/**
 * Emits [WsOpen], the decoded commands, then [WsClosed] or [WsFailure]; nothing after
 * [disconnect]. [events] is unbounded and for a single collector, so nothing is lost before it.
 */
internal abstract class WebSocketConnection(
    private val httpClient: HttpClient,
    ioDispatcher: CoroutineDispatcher,
) {
    private val name = javaClass.simpleName
    private val connectionScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val outgoingMessages = Channel<String>(Channel.UNLIMITED)
    private val _events = Channel<LocalBaseCommand>(Channel.UNLIMITED)
    val events: Flow<LocalBaseCommand> = _events.receiveAsFlow()

    @Volatile
    private var session: WebSocketSession? = null

    @Volatile
    private var disconnected = false

    protected abstract fun HttpRequestBuilder.configureRequest()

    /** Null for frames the SDK ignores. An [IllegalArgumentException] drops the frame. */
    protected abstract fun decode(text: String): LocalBaseCommand?

    open fun connect() {
        connectionScope.launch {
            try {
                httpClient.webSocket(request = { configureRequest() }) {
                    session = this
                    Logger.d("$name onOpen")
                    emitEvent(WsOpen)

                    val sender = launch {
                        try {
                            for (message in outgoingMessages) {
                                outgoing.send(Frame.Text(message))
                            }
                        } catch (_: ClosedSendChannelException) {
                            // The session is closing; reported below.
                        }
                    }
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> handleText(frame.readText())
                            else -> Logger.d("$name onMessage ${frame.frameType}")
                        }
                    }
                    sender.cancel()

                    val reason = closeReason.await()
                    Logger.d("$name onClosed $reason")
                    emitEvent(
                        WsClosed(
                            code = reason?.code?.toInt() ?: CloseReason.Codes.NORMAL.code.toInt(),
                            reason = reason?.message.orEmpty()
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e("$name onFailure $e")
                emitEvent(WsFailure)
            } finally {
                outgoingMessages.close()
            }
        }
    }

    /** Queued until the connection is open. False after [disconnect]. */
    fun sendMessage(message: String): Boolean {
        return outgoingMessages.trySend(message).isSuccess
    }

    fun disconnect() {
        disconnected = true
        outgoingMessages.close()
        val session = session
        if (session == null) {
            connectionScope.cancel()
        } else {
            session.outgoing.trySend(Frame.Close(CloseReason(CloseReason.Codes.GOING_AWAY, BYE)))
        }
    }

    private fun handleText(text: String) {
        val command = try {
            decode(text)
        } catch (e: IllegalArgumentException) {
            Logger.e("$name: parsing FAILED $e")
            return
        }
        command?.let { emitEvent(it) }
    }

    private fun emitEvent(event: LocalBaseCommand) {
        if (!disconnected) {
            _events.trySend(event)
        }
    }

    private companion object {
        const val BYE = "BYE"
    }
}

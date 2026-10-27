package com.eyeson.sdk.network

import com.eyeson.sdk.di.NetworkModule
import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.local.base.LocalBaseCommand
import com.eyeson.sdk.model.local.ws.WsClosed
import com.eyeson.sdk.model.local.ws.WsFailure
import com.eyeson.sdk.model.local.ws.WsOpen
import com.eyeson.sdk.model.meeting.base.MeetingBaseCommandDto
import com.eyeson.sdk.model.meeting.base.MeetingBaseMessageDto
import com.eyeson.sdk.model.meeting.base.UnknownMessageDto
import com.eyeson.sdk.model.meeting.outgoing.SubscribeToChannel
import com.eyeson.sdk.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

internal class MeetingConnection(
    private val meeting: MeetingDto,
    private val json: Json = NetworkModule.json,
    private val okHttpClient: OkHttpClient = NetworkModule.okHttpClient,
) {
    private val meetingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var webSocketListener: WebSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Logger.d("WebSocket onOpen $response")
            emitEvent(WsOpen(response))
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (!text.containsPing()) {
                Logger.d("WebSocket onMessage: $text")
            }

            val command = try {
                json.decodeFromString(MeetingBaseCommandDto.serializer(), text)
            } catch (e: IllegalArgumentException) {
                // SerializationException is an IllegalArgumentException
                Logger.e("WebSocketCommands.BaseCommand: parsing FAILED $e")
                return
            }
            command.message?.let {
                handleMessages(it)
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Logger.d("WebSocket onMessage byte")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Logger.d("WebSocket onClosed $code $reason")
            emitEvent(WsClosed(code, reason))
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Logger.e("WebSocket onFailure $response; $t")
            emitEvent(WsFailure(response))
        }
    }

    private val socket: WebSocket by lazy {
        val client = okHttpClient.newBuilder()
            .pingInterval(PING_INTERVAL, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(meeting.links.websocket)
            .build()

        client.newWebSocket(request, webSocketListener)
    }

    private val _events = MutableSharedFlow<LocalBaseCommand>(0)
    val events = _events.asSharedFlow()

    fun connect() {
        socket.request()
        connectToChannels()
    }

    fun sendMessage(string: String): Boolean {
        return socket.send(string)
    }

    private fun connectToChannels() {
        val channels = listOf(
            SubscribeToChannel.roomChannel(subscribe = true),
            SubscribeToChannel.userChannel(subscribe = true)
        )

        channels.forEach { channel ->
            sendMessage(json.encodeToString(SubscribeToChannel.serializer(), channel))
        }
    }


    fun disconnect() {
        meetingScope.cancel()
        socket.close(1001, "BYE")
    }

    private fun handleMessages(message: MeetingBaseMessageDto) {
        when (message) {
            is UnknownMessageDto -> {
                Logger.d("Received a not supported message: $message")
                null
            }
            else -> {
                message.toLocal()
            }
        }?.let {
            emitEvent(it)
        }
    }

    private fun emitEvent(event: LocalBaseCommand) {
        meetingScope.launch {
            _events.emit(event)
        }
    }

    private fun String.containsPing(): Boolean {
        return contains(TYPE_PING)
    }

    companion object {
        private const val PING_INTERVAL = 5L
        private const val TYPE_PING = "\"type\":\"ping\""
    }

}
package com.eyeson.sdk.network

import com.eyeson.sdk.di.NetworkModule
import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.local.base.LocalBaseCommand
import com.eyeson.sdk.model.local.ws.WsClosed
import com.eyeson.sdk.model.local.ws.WsFailure
import com.eyeson.sdk.model.local.ws.WsOpen
import com.eyeson.sdk.model.sepp.base.SeppBaseCommandDto
import com.eyeson.sdk.model.sepp.base.UnknownCommandDto
import com.eyeson.sdk.utils.Logger
import com.squareup.moshi.JsonDataException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

internal class SignalingConnection(private val meeting: MeetingDto) {
    private val signalingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val moshi = NetworkModule.moshi

    private var webSocketListener: WebSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Logger.d("WebSocket onOpen $response")
            emitEvent(WsOpen(response))
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Logger.d("WebSocket onMessage: $text")

            val adapter = moshi.adapter(SeppBaseCommandDto::class.java)
            try {
                val command = adapter.fromJson(text)
                command?.let {
                    handleMessages(it)
                }
            } catch (e: JsonDataException) {
                Logger.e("WebSocketCommands.BaseCommand: parsing FAILED $e")
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
        val client = OkHttpClient.Builder()
            .pingInterval(PING_INTERVAL, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(meeting.signaling.options.endpoint)
            .addHeader("Authorization", "Bearer ${meeting.signaling.options.authToken}")
            .build()

        client.newWebSocket(request, webSocketListener)
    }

    private val _events = MutableSharedFlow<LocalBaseCommand>(0)
    val events = _events.asSharedFlow()

    fun connect() {
        socket.request()
    }

    fun sendMessage(message: String): Boolean {
        return socket.send(message)
    }

    fun disconnect() {
        signalingScope.cancel()
        socket.close(1001, "BYE")
    }

    private fun terminate() {
        signalingScope.cancel()
        socket.cancel()
    }

    private fun handleMessages(message: SeppBaseCommandDto) {
        when (message) {
            is UnknownCommandDto -> {
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
        signalingScope.launch {
            _events.emit(event)
        }
    }

    companion object {
        private const val PING_INTERVAL = 5L
    }

}
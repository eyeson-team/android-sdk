package com.eyeson.sdk.network

import com.eyeson.sdk.di.NetworkModule
import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.local.base.LocalBaseCommand
import com.eyeson.sdk.model.meeting.base.MeetingBaseCommandDto
import com.eyeson.sdk.model.meeting.base.UnknownMessageDto
import com.eyeson.sdk.model.meeting.outgoing.SubscribeToChannel
import com.eyeson.sdk.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.url
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json

internal class MeetingConnection(
    private val meeting: MeetingDto,
    private val json: Json = NetworkModule.json,
    httpClient: HttpClient = NetworkModule.httpClient,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WebSocketConnection(httpClient, ioDispatcher) {

    override fun HttpRequestBuilder.configureRequest() {
        url(webSocketUrl(meeting.links.websocket))
    }

    override fun connect() {
        super.connect()
        connectToChannels()
    }

    override fun decode(text: String): LocalBaseCommand? {
        if (!text.containsPing()) {
            Logger.d("WebSocket onMessage: $text")
        }

        val message = json.decodeFromString(MeetingBaseCommandDto.serializer(), text).message
        return when (message) {
            null -> null
            is UnknownMessageDto -> {
                Logger.d("Received a not supported message: $message")
                null
            }
            else -> message.toLocal()
        }
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

    private fun String.containsPing(): Boolean {
        return contains(TYPE_PING)
    }

    companion object {
        private const val TYPE_PING = "\"type\":\"ping\""
    }
}

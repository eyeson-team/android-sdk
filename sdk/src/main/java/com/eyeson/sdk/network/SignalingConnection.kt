package com.eyeson.sdk.network

import com.eyeson.sdk.di.NetworkModule
import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.local.base.LocalBaseCommand
import com.eyeson.sdk.model.sepp.base.UnknownCommandDto
import com.eyeson.sdk.serializer.SeppIncomingCommandsSerializer
import com.eyeson.sdk.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.url
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json

internal class SignalingConnection(
    private val meeting: MeetingDto,
    private val json: Json = NetworkModule.json,
    httpClient: HttpClient = NetworkModule.httpClient,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WebSocketConnection(httpClient, ioDispatcher) {

    override fun HttpRequestBuilder.configureRequest() {
        url(webSocketUrl(meeting.signaling.options.endpoint))
        bearerAuth(meeting.signaling.options.authToken)
    }

    override fun decode(text: String): LocalBaseCommand? {
        Logger.d("WebSocket onMessage: $text")

        return when (val command = json.decodeFromString(SeppIncomingCommandsSerializer, text)) {
            is UnknownCommandDto -> {
                Logger.d("Received a not supported message: $command")
                null
            }
            else -> command.toLocal()
        }
    }
}

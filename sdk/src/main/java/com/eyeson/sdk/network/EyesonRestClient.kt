package com.eyeson.sdk.network

import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.api.PermalinkDto
import com.eyeson.sdk.model.api.UserInMeetingDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.plugins.resources.delete
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ParametersBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import io.ktor.resources.Resource
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal class EyesonRestClient(httpClient: HttpClient, json: Json, apiUrl: String) {

    init {
        require(Url(apiUrl).protocolOrNull in setOf(URLProtocol.HTTP, URLProtocol.HTTPS)) {
            "Invalid API URL: $apiUrl"
        }
    }

    private val client = httpClient.config {
        install(ContentNegotiation) { json(json) }
        install(Resources)
        defaultRequest {
            url(apiUrl)
            url.pathSegments = emptyList()
        }
    }

    suspend fun joinMeetingAsGuest(
        guestToken: String,
        name: String,
        id: String?,
        avatar: String?,
    ): RestResponse<MeetingDto> =
        client.post(resource = Guest(guestToken = guestToken)) {
            formBody {
                append("name", name)
                appendNotNull("id", id)
                appendNotNull("avatar", avatar)
            }
        }.toRestResponse()

    suspend fun getMeetingInfo(accessKey: String): RestResponse<MeetingDto> =
        client.get(resource = Room(accessKey = accessKey)).toRestResponse()

    suspend fun getUserInfo(
        accessKey: String,
        userId: String,
    ): RestResponse<UserInMeetingDto> =
        client.get(
            resource = Room.User(Room(accessKey = accessKey), userId = userId)
        ).toRestResponse()

    suspend fun sendChatMessage(accessKey: String, message: String): RestResponse<Unit> =
        sendMessage(accessKey = accessKey, content = message, type = TYPE_CHAT)

    suspend fun sendCustomMessage(accessKey: String, content: String): RestResponse<Unit> =
        sendMessage(accessKey = accessKey, content = content, type = TYPE_CUSTOM)

    private suspend fun sendMessage(
        accessKey: String,
        content: String,
        type: String,
    ): RestResponse<Unit> =
        client.post(resource = Room.Messages(Room(accessKey = accessKey))) {
            formBody {
                append("type", type)
                append("content", content)
            }
        }.toRestResponse()

    suspend fun videoPlayback(
        accessKey: String,
        url: String,
        name: String?,
        playId: String?,
        replacementId: String?,
        audio: Boolean,
        loopCount: Int,
    ): RestResponse<Unit> =
        client.post(resource = Room.Playbacks(Room(accessKey = accessKey))) {
            formBody {
                append("url", url)
                appendNotNull("play_id", playId)
                appendNotNull("replacement_id", replacementId)
                appendNotNull("name", name)
                append("audio", audio.toString())
                append("loop_count", loopCount.toString())
            }
        }.toRestResponse()

    suspend fun stopVideoPlayback(
        accessKey: String,
        playId: String,
    ): RestResponse<Unit> =
        client.delete(
            resource = Room.Playback(Room(accessKey = accessKey), playId = playId)
        ).toRestResponse()

    suspend fun startPresentation(accessKey: String): RestResponse<Unit> =
        client.post(resource = Room.Presentation(Room(accessKey = accessKey))).toRestResponse()

    suspend fun stopPresentation(accessKey: String): RestResponse<Unit> =
        client.delete(resource = Room.Presentation(Room(accessKey = accessKey))).toRestResponse()

    suspend fun getPermalinkMeetingInfo(token: String): RestResponse<PermalinkDto> =
        client.get(resource = Permalink(token = token)).toRestResponse()

    suspend fun startPermalinkMeeting(userToken: String): RestResponse<MeetingDto> =
        client.post(resource = Permalink(token = userToken)).toRestResponse()

    private suspend inline fun <reified T> HttpResponse.toRestResponse(): RestResponse<T> =
        RestResponse(status.value, if (status.isSuccess()) body<T>() else null)

    private companion object {
        const val TYPE_CHAT = "chat"
        const val TYPE_CUSTOM = "custom"
    }
}

internal class RestResponse<out T>(val code: Int, val body: T?)

private fun HttpRequestBuilder.formBody(builder: ParametersBuilder.() -> Unit) {
    setBody(FormDataContent(parameters(builder)))
}

private fun ParametersBuilder.appendNotNull(name: String, value: String?) {
    value?.let { append(name, it) }
}

@Serializable
@Resource("/guests/{guestToken}")
private class Guest(val guestToken: String)

@Serializable
@Resource("/rooms/{accessKey}")
private class Room(val accessKey: String) {

    @Serializable
    @Resource("users/{userId}")
    class User(val parent: Room, val userId: String)

    @Serializable
    @Resource("messages")
    class Messages(val parent: Room)

    @Serializable
    @Resource("playbacks")
    class Playbacks(val parent: Room)

    @Serializable
    @Resource("playbacks/{playId}")
    class Playback(val parent: Room, val playId: String)

    @Serializable
    @Resource("presentation")
    class Presentation(val parent: Room)
}

@Serializable
@Resource("/permalink/{token}")
private class Permalink(val token: String)

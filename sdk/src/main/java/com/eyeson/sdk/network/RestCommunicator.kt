package com.eyeson.sdk.network

import com.eyeson.sdk.di.NetworkModule
import com.eyeson.sdk.exceptions.internal.FaultyInfoException
import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.api.toLocal
import com.eyeson.sdk.model.local.api.PermalinkMeetingInfo
import com.eyeson.sdk.model.local.api.UserWithSignaling
import com.eyeson.sdk.utils.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal class RestCommunicator(
    private val apiUrl: String,
    restClientFactory: (apiUrl: String) -> EyesonRestClient = NetworkModule::restClient,
) {
    // Lazy, so that an invalid apiUrl throws inside the requests below.
    private val restClient by lazy { restClientFactory(apiUrl) }

    suspend fun getMeetingInfo(accessKey: String): MeetingDto =
        restClient.getMeetingInfo(accessKey).bodyOrThrow()

    suspend fun getMeetingInfoAsGuest(
        guestToken: String,
        name: String,
        id: String?,
        avatar: String?,
    ): MeetingDto =
        restClient.joinMeetingAsGuest(guestToken, name, id, avatar).bodyOrThrow()

    suspend fun startPermalinkMeeting(userToken: String): MeetingDto =
        restClient.startPermalinkMeeting(userToken).bodyOrThrow()

    suspend fun getPermalinkMeetingInfo(token: String): PermalinkMeetingInfo? =
        responseOrNull("Permalink meeting info") {
            restClient.getPermalinkMeetingInfo(token)
        }?.body?.toLocal()

    suspend fun getUserInfo(accessKey: String, userId: String): UserWithSignaling? {
        val user = responseOrNull("User info lookup for $userId") {
            restClient.getUserInfo(accessKey, userId)
        }?.body?.toLocal() ?: return null
        return UserWithSignaling(user, userId)
    }

    suspend fun getUserInfo(accessKey: String, userIds: List<String>): List<UserWithSignaling> =
        coroutineScope {
            userIds.map {
                async {
                    getUserInfo(accessKey, it)
                }
            }.awaitAll().filterNotNull()
        }

    suspend fun sendChatMessage(accessKey: String, message: String): Int? =
        responseOrNull("Chat message") {
            restClient.sendChatMessage(accessKey, message)
        }?.code

    suspend fun sendCustomMessage(accessKey: String, content: String): Int? =
        responseOrNull("Custom message") {
            restClient.sendCustomMessage(accessKey, content)
        }?.code

    suspend fun videoPlayback(
        accessKey: String,
        url: String,
        name: String?,
        playId: String?,
        replacementId: String?,
        audio: Boolean,
        loopCount: Int,
    ): Int? =
        responseOrNull("Video playback start") {
            restClient.videoPlayback(accessKey, url, name, playId, replacementId, audio, loopCount)
        }?.code

    suspend fun stopVideoPlayback(accessKey: String, playId: String): Int? =
        responseOrNull("Video playback stop") {
            restClient.stopVideoPlayback(accessKey, playId)
        }?.code

    suspend fun startPresentation(accessKey: String): Int? =
        responseOrNull("Presentation start") {
            restClient.startPresentation(accessKey)
        }?.code

    suspend fun stopPresentation(accessKey: String): Int? =
        responseOrNull("Presentation stop") {
            restClient.stopPresentation(accessKey)
        }?.code

    private fun <T> RestResponse<T>.bodyOrThrow(): T = body ?: throw FaultyInfoException(code)

    // Most callers launch without an exception handler, where a throw would crash the host app.
    // An error status is a response and is returned as is.
    private inline fun <T> responseOrNull(
        description: String,
        request: () -> RestResponse<T>,
    ): RestResponse<T>? {
        return try {
            request()
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }
            Logger.w("$description failed: $e")
            null
        }
    }
}


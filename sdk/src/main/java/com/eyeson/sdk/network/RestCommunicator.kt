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
import retrofit2.Response

internal class RestCommunicator(private val apiUrl: String) {
    private val restClient by lazy { NetworkModule.restClient(apiUrl) }

    suspend fun getMeetingInfo(accessKey: String): MeetingDto = coroutineScope {
        val meetingInfo = restClient.getMeetingInfo(accessKey)
        meetingInfo.body() ?: throw FaultyInfoException(meetingInfo.code())
    }

    suspend fun getMeetingInfoAsGuest(
        guestToken: String,
        name: String,
        id: String?,
        avatar: String?,
    ): MeetingDto =
        coroutineScope {
            val meetingInfo = restClient.joinMeetingAsGuest(guestToken, name, id, avatar)
            meetingInfo.body() ?: throw FaultyInfoException(meetingInfo.code())
        }

    // A failed lookup must not cancel the other lookups of the batch below, nor the event
    // collector that requested it.
    suspend fun getUserInfo(accessKey: String, userId: String): UserWithSignaling? =
        coroutineScope {
            val userInfo = responseOrNull("User info lookup for $userId") {
                restClient.getUserInfo(accessKey, userId)
            }
            val user = userInfo?.body()?.toLocal() ?: return@coroutineScope null
            UserWithSignaling(user, userId)
        }

    suspend fun getUserInfo(accessKey: String, userIds: List<String>): List<UserWithSignaling> =
        coroutineScope {
            userIds.map {
                async {
                    getUserInfo(accessKey, it)
                }
            }.awaitAll().filterNotNull()
        }

    // The functions below return the HTTP status code, or null if the request got no response.

    suspend fun sendChatMessage(accessKey: String, message: String): Int? =
        coroutineScope {
            responseOrNull("Chat message") {
                restClient.sendChatMessage(accessKey, message)
            }?.code()
        }

    suspend fun sendCustomMessage(accessKey: String, content: String): Int? =
        coroutineScope {
            responseOrNull("Custom message") {
                restClient.sendCustomMessage(accessKey, content)
            }?.code()
        }

    suspend fun videoPlayback(
        accessKey: String,
        url: String,
        name: String?,
        playId: String?,
        replacementId: String?,
        audio: Boolean,
        loopCount: Int,
    ): Int? = coroutineScope {
        responseOrNull("Video playback start") {
            restClient.videoPlayback(accessKey, url, name, playId, replacementId, audio, loopCount)
        }?.code()
    }


    suspend fun stopVideoPlayback(
        accessKey: String,
        playId: String,
    ): Int? = coroutineScope {
        responseOrNull("Video playback stop") {
            restClient.stopVideoPlayback(accessKey, playId)
        }?.code()
    }

    suspend fun startPresentation(
        accessKey: String,
    ): Int? = coroutineScope {
        responseOrNull("Presentation start") {
            restClient.startPresentation(accessKey)
        }?.code()
    }

    suspend fun stopPresentation(
        accessKey: String,
    ): Int? = coroutineScope {
        responseOrNull("Presentation stop") {
            restClient.stopPresentation(accessKey)
        }?.code()
    }

    suspend fun getPermalinkMeetingInfo(token: String): PermalinkMeetingInfo? = coroutineScope {
        val permalinkInfo = responseOrNull("Permalink meeting info") {
            restClient.getPermalinkMeetingInfo(token)
        }
        permalinkInfo?.body()?.toLocal()
    }

    suspend fun startPermalinkMeeting(userToken: String): MeetingDto = coroutineScope {
        val meetingInfo = restClient.startPermalinkMeeting(userToken)
        meetingInfo.body() ?: throw FaultyInfoException(meetingInfo.code())
    }

    /**
     * Runs [request] and returns null if it throws instead of producing a response: a network
     * failure, a body that does not parse, or an [apiUrl] Retrofit rejects. Most callers run in
     * launches without an exception handler, where an exception would crash the host app. An
     * error status is a response and is returned as is.
     */
    private inline fun <T> responseOrNull(description: String, request: () -> Response<T>): Response<T>? {
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


package com.eyeson.sdk.network

import com.eyeson.sdk.di.NetworkModule
import com.eyeson.sdk.exceptions.internal.FaultyInfoException
import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.api.toLocal
import com.eyeson.sdk.model.local.api.UserWithSignaling
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal class RestCommunicator {
    private val restClient by lazy { NetworkModule.restClient }

    suspend fun getMeetingInfo(accessKey: String): MeetingDto = coroutineScope {
        val meetingInfo = restClient.getMeetingInfo(accessKey)
        meetingInfo.body() ?: throw FaultyInfoException(meetingInfo.code())
    }

    suspend fun getMeetingInfoAsGuest(
        guestToken: String,
        name: String,
        id: String?,
        avatar: String?
    ): MeetingDto =
        coroutineScope {
            val meetingInfo = restClient.joinMeetingAsGuest(guestToken, name, id, avatar)
            meetingInfo.body() ?: throw FaultyInfoException(meetingInfo.code())
        }

    suspend fun getUserInfo(accessKey: String, userId: String): UserWithSignaling? =
        coroutineScope {
            val userInfo = restClient.getUserInfo(accessKey, userId)
            val user = userInfo.body()?.toLocal() ?: return@coroutineScope null
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

    suspend fun sendCustomMessage(accessKey: String, content: String): Int =
        coroutineScope {
            restClient.sendCustomMessage(accessKey, content).code()
        }
}


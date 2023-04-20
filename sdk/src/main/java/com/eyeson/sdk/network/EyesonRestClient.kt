package com.eyeson.sdk.network

import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.api.UserInMeetingDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

internal class EyesonRestClient(
    private val eyesonApi: EyesonApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun joinMeetingAsGuest(
        guestToken: String,
        name: String,
        id: String?,
        avatar: String?
    ): Response<MeetingDto> = withContext(ioDispatcher) {
        eyesonApi.joinRoomAsGuest(guestToken = guestToken, name = name, id = id, avatar = avatar)
    }

    suspend fun getMeetingInfo(accessKey: String): Response<MeetingDto> =
        withContext(ioDispatcher) {
            eyesonApi.getRoomInfo(accessKey)
        }

    suspend fun getUserInfo(
        accessKey: String,
        userId: String
    ): Response<UserInMeetingDto> = withContext(ioDispatcher) {
        eyesonApi.getUsernameInRoom(accessKey, userId)
    }


    suspend fun sendCustomMessage(accessKey: String, content: String): Response<Unit> =
        withContext(ioDispatcher) {
            eyesonApi.sendCustomMessage(accessKey, "custom", content)
        }

    suspend fun videoPlayback(
        accessKey: String,
        url: String,
        name: String?,
        playId: String?,
        replacementId: String?,
        audio: Boolean
    ): Response<Unit> =
        withContext(ioDispatcher) {
            eyesonApi.videoPlayback(accessKey, url, playId, replacementId, name, audio)
        }

    suspend fun stopVideoPlayback(
        accessKey: String,
        playId: String,
    ): Response<Unit> =
        withContext(ioDispatcher) {
            eyesonApi.stopVideoPlayback(accessKey, playId)
        }
}
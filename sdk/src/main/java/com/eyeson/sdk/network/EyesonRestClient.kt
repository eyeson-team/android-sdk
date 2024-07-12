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
            eyesonApi.getRoomInfo(accessKey = accessKey)
        }

    suspend fun getUserInfo(
        accessKey: String,
        userId: String
    ): Response<UserInMeetingDto> = withContext(ioDispatcher) {
        eyesonApi.getUsernameInRoom(accessKey = accessKey, userId = userId)
    }

    suspend fun sendChatMessage(accessKey: String, message: String): Response<Unit> =
        sendMessage(accessKey = accessKey, content = message, type = TYPE_CHAT)

    suspend fun sendCustomMessage(accessKey: String, content: String): Response<Unit> =
        sendMessage(accessKey = accessKey, content = content, type = TYPE_CUSTOM)

    private suspend fun sendMessage(
        accessKey: String,
        content: String,
        type: String
    ): Response<Unit> =
        withContext(ioDispatcher) {
            eyesonApi.sendMessage(accessKey = accessKey, type = type, content = content)
        }

    suspend fun videoPlayback(
        accessKey: String,
        url: String,
        name: String?,
        playId: String?,
        replacementId: String?,
        audio: Boolean,
        loopCount: Int
    ): Response<Unit> =
        withContext(ioDispatcher) {
            eyesonApi.videoPlayback(
                accessKey = accessKey,
                url = url,
                playerId = playId,
                replacementId = replacementId,
                name = name,
                audio = audio,
                loopCount = loopCount
            )
        }

    suspend fun stopVideoPlayback(
        accessKey: String,
        playId: String,
    ): Response<Unit> =
        withContext(ioDispatcher) {
            eyesonApi.stopVideoPlayback(accessKey = accessKey, playerId = playId)
        }

    suspend fun startPresentation(accessKey: String): Response<Unit> =
        withContext(ioDispatcher) {
            eyesonApi.startPresentation(accessKey = accessKey)
        }

    suspend fun stopPresentation(accessKey: String): Response<Unit> =
        withContext(ioDispatcher) {
            eyesonApi.stopPresentation(accessKey = accessKey)
        }

    companion object {
        const val TYPE_CHAT = "chat"
        const val TYPE_CUSTOM = "custom"
    }
}
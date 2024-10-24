package  com.eyeson.sdk.network

import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.api.PermalinkDto
import com.eyeson.sdk.model.api.UserInMeetingDto
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

internal interface EyesonApi {
    @POST("/guests/{guestToken}")
    @FormUrlEncoded
    suspend fun joinRoomAsGuest(
        @Path("guestToken") guestToken: String,
        @Field("name") name: String,
        @Field("id") id: String?,
        @Field("avatar") avatar: String?,
    ): Response<MeetingDto>

    @GET("/rooms/{accessKey}")
    suspend fun getRoomInfo(
        @Path("accessKey") accessKey: String,
    ): Response<MeetingDto>

    @GET("/rooms/{accessKey}/users/{userId}")
    suspend fun getUsernameInRoom(
        @Path("accessKey") accessKey: String,
        @Path("userId") userId: String,
    ): Response<UserInMeetingDto>

    @POST("/rooms/{accessKey}/messages")
    @FormUrlEncoded
    suspend fun sendMessage(
        @Path("accessKey") accessKey: String,
        @Field("type") type: String,
        @Field("content") content: String,
    ): Response<Unit>

    @POST("/rooms/{accessKey}/playbacks")
    @FormUrlEncoded
    suspend fun videoPlayback(
        @Path("accessKey") accessKey: String,
        @Field("playback[url]") url: String,
        @Field("playback[play_id]") playerId: String?,
        @Field("playback[replacement_id]") replacementId: String?,
        @Field("playback[name]") name: String?,
        @Field("playback[audio]") audio: Boolean,
        @Field("playback[loop_count]") loopCount: Int,
    ): Response<Unit>

    @DELETE("/rooms/{accessKey}/playbacks/{playerId}")
    suspend fun stopVideoPlayback(
        @Path("accessKey") accessKey: String,
        @Path("playerId") playerId: String,
    ): Response<Unit>

    @POST("/rooms/{accessKey}/presentation")
    suspend fun startPresentation(
        @Path("accessKey") accessKey: String,
    ): Response<Unit>


    @DELETE("/rooms/{accessKey}/presentation")
    suspend fun stopPresentation(
        @Path("accessKey") accessKey: String,
    ): Response<Unit>

    @GET("/permalink/{token}")
    suspend fun getPermalinkMeetingInfo(@Path("token") token: String): Response<PermalinkDto>

    @POST("/permalink/{userToken}")
    suspend fun startPermalinkMeeting(@Path("userToken") userToken: String): Response<MeetingDto>
}

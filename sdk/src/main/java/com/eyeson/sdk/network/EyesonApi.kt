package  com.eyeson.sdk.network

import com.eyeson.sdk.model.api.MeetingDto
import com.eyeson.sdk.model.api.UserInMeetingDto
import retrofit2.Response
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
        @Field("avatar") avatar: String?
    ): Response<MeetingDto>

    @GET("/rooms/{accessKey}")
    suspend fun getRoomInfo(
        @Path("accessKey") accessKey: String
    ): Response<MeetingDto>

    @GET("/rooms/{accessKey}/users/{userId}")
    suspend fun getUsernameInRoom(
        @Path("accessKey") accessKey: String,
        @Path("userId") userId: String
    ): Response<UserInMeetingDto>

}

package com.eyeson.sdk.model.datachannel.incoming


import com.eyeson.sdk.model.datachannel.base.DataChannelCommandDto
import com.eyeson.sdk.model.local.datachannel.MemberListUpdate
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class MemberListUpdateDto(
    @Json(name = "type") override val type: String,
    @Json(name = "add") val added: List<Add>,
    @Json(name = "count") val count: Int,
    @Json(name = "del") val deleted: List<String>,
    @Json(name = "media") val media: List<Media>
) : DataChannelCommandDto {
    @JsonClass(generateAdapter = true)
    data class Add(
        @Json(name = "cid") val userId: String,
        @Json(name = "p") val platform: String
    )

    @JsonClass(generateAdapter = true)
    data class Media(
        @Json(name = "mid") val mediaId: String,
        @Json(name = "playid") val playId: String
    )

    override fun toLocal(): MemberListUpdate {
        return MemberListUpdate(
            added = added.map { it.userId },
            deleted = deleted,
            memberCountAfterUpdate = count
        )
    }
}

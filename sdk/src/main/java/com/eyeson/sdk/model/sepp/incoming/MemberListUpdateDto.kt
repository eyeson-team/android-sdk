package com.eyeson.sdk.model.sepp.incoming


import com.eyeson.sdk.model.local.base.LocalBaseCommand
import com.eyeson.sdk.model.local.sepp.MemberListUpdate
import com.eyeson.sdk.model.sepp.base.SeppBaseCommandDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class MemberListUpdateDto(
    @Json(name = "type") override val type: String,
    @Json(name = "msg_id") val msgId: String,
    @Json(name = "from") val from: String,
    @Json(name = "to") val to: String,
    @Json(name = "data") val data: Data
) : SeppBaseCommandDto {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "call_id") val callId: String,
        @Json(name = "add") val added: List<Add>,
        @Json(name = "count") val count: Int,
        @Json(name = "del") val deleted: List<String>,
        @Json(name = "media") val media: List<Media>
    ) {
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
    }

    override fun toLocal(): MemberListUpdate {
        return MemberListUpdate(
            added = data.added.map { it.userId },
            deleted = data.deleted,
            memberCountAfterUpdate = data.count,
            mediaPlayIds = data.media.map { it.playId }
        )
    }
}
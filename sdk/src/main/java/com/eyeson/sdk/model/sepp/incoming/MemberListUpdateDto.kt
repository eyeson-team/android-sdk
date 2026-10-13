package com.eyeson.sdk.model.sepp.incoming


import com.eyeson.sdk.model.local.base.LocalBaseCommand
import com.eyeson.sdk.model.local.sepp.MemberListUpdate
import com.eyeson.sdk.model.sepp.base.SeppBaseCommandDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MemberListUpdateDto(
    @SerialName("type") override val type: String,
    @SerialName("msg_id") val msgId: String,
    @SerialName("from") val from: String,
    @SerialName("to") val to: String,
    @SerialName("data") val data: Data
) : SeppBaseCommandDto {
    @Serializable
    data class Data(
        @SerialName("call_id") val callId: String,
        @SerialName("add") val added: List<Add>,
        @SerialName("count") val count: Int,
        @SerialName("del") val deleted: List<String>,
        @SerialName("media") val media: List<Media>
    ) {
        @Serializable
        data class Add(
            @SerialName("cid") val userId: String,
            @SerialName("p") val platform: String
        )

        @Serializable
        data class Media(
            @SerialName("mid") val mediaId: String,
            @SerialName("playid") val playId: String
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
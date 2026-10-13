package com.eyeson.sdk.model.sepp.incoming


import com.eyeson.sdk.model.local.sepp.SourceUpdate
import com.eyeson.sdk.model.sepp.base.SeppBaseCommandDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SourceUpdateDto(
    @SerialName("type") override val type: String,
    @SerialName("msg_id") val msgId: String,
    @SerialName("from") val from: String,
    @SerialName("to") val to: String,
    @SerialName("data") val data: Data
) : SeppBaseCommandDto {
    @Serializable
    data class Data(
        @SerialName("call_id") val callId: String,
        @SerialName("asrc") val audioSources: List<Int>,
        @SerialName("bcast") val broadcasting: Boolean,
        @SerialName("dims") val dimensions: List<Dim>,
        @SerialName("dsrc") val desktopStreamingId: Int?,
        @SerialName("l") val layout: Int,
        @SerialName("psrc") val presenter: Int?,
        @SerialName("src") val sources: List<String>,
        @SerialName("tovl") val textOverlay: Boolean,
        @SerialName("vsrc") val videSources: List<Int>
    ) {
        @Serializable
        data class Dim(
            @SerialName("h") val height: Int,
            @SerialName("w") val width: Int,
            @SerialName("x") val x: Int,
            @SerialName("y") val y: Int
        )
    }

    override fun toLocal(): SourceUpdate {
        return SourceUpdate(
            sources = data.sources,
            videSources = data.videSources,
            desktopStreamingId = data.desktopStreamingId,
            presenter = data.presenter
        )
    }
}
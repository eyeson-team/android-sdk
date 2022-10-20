package com.eyeson.sdk.model.sepp.incoming


import com.eyeson.sdk.model.local.sepp.SourceUpdate
import com.eyeson.sdk.model.sepp.base.SeppBaseCommandDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SourceUpdateDto(
    @Json(name = "type") override val type: String,
    @Json(name = "msg_id") val msgId: String,
    @Json(name = "from") val from: String,
    @Json(name = "to") val to: String,
    @Json(name = "data") val data: Data
) : SeppBaseCommandDto {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "call_id") val callId: String,
        @Json(name = "asrc") val audioSources: List<Int>,
        @Json(name = "bcast") val broadcasting: Boolean,
        @Json(name = "dims") val dimensions: List<Dim>,
        @Json(name = "dsrc") val desktopStreamingId: Int?,
        @Json(name = "l") val layout: Int,
        @Json(name = "psrc") val presenter: Int?,
        @Json(name = "src") val sources: List<String>,
        @Json(name = "tovl") val textOverlay: Boolean,
        @Json(name = "vsrc") val videSources: List<Int>
    ) {
        @JsonClass(generateAdapter = true)
        data class Dim(
            @Json(name = "h") val height: Int,
            @Json(name = "w") val width: Int,
            @Json(name = "x") val x: Int,
            @Json(name = "y") val y: Int
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
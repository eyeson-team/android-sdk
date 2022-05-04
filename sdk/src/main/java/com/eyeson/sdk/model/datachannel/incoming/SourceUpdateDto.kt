package com.eyeson.sdk.model.datachannel.incoming


import com.eyeson.sdk.model.datachannel.base.DataChannelCommandDto
import com.eyeson.sdk.model.local.datachannel.SourceUpdate
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SourceUpdateDto(
    @Json(name = "type") override val type: String,
    @Json(name = "asrc") val audioSources: List<Int>,
    @Json(name = "bcast") val broadcasting: Boolean,
    @Json(name = "dims") val dimensions: List<Dimensions>,
    @Json(name = "dsrc") val desktopStreamingId: Int?,
    @Json(name = "l") val layout: Int,
    @Json(name = "psrc") val presenter: Int?,
    @Json(name = "src") val sources: List<String>,
    @Json(name = "tovl") val textOverlay: Boolean,
    @Json(name = "vsrc") val videSources: List<Int>
) : DataChannelCommandDto {
    @JsonClass(generateAdapter = true)
    data class Dimensions(
        @Json(name = "h") val height: Int,
        @Json(name = "w") val width: Int,
        @Json(name = "x") val x: Int,
        @Json(name = "y") val y: Int
    )

    override fun toLocal(): SourceUpdate {
        return SourceUpdate(
            sources = sources,
            videSources = videSources,
            desktopStreamingId = desktopStreamingId,
            presenter = presenter
        )
    }
}

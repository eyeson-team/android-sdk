package com.eyeson.sdk.model.sepp.incoming

import com.eyeson.sdk.model.local.sepp.CallRejected
import com.eyeson.sdk.model.sepp.base.SeppBaseCommandDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class CallRejectedDto(
    @Json(name = "type") override val type: String,
    @Json(name = "msg_id") val msgId: String,
    @Json(name = "from") val from: String,
    @Json(name = "to") val to: String,
    @Json(name = "data") val data: Data
) : SeppBaseCommandDto {

    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "reject_code") val rejectCode: Int
    )

    override fun toLocal(): CallRejected {
        return CallRejected(data.rejectCode)
    }
}

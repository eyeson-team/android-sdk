package com.eyeson.sdk.model.datachannel.outgoing


import android.app.DatePickerDialog
import com.eyeson.sdk.model.datachannel.base.DataChannelCommandDto
import com.eyeson.sdk.model.local.base.UnknownCommand
import com.eyeson.sdk.model.local.datachannel.MuteVideo
import com.eyeson.sdk.model.local.datachannel.SetPresenter
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SetPresenterDto(
    @Json(name = "type") override val type: String,
    @Json(name = "on") val on: Boolean,
    @Json(name = "cid") val cid: String
) : DataChannelCommandDto {
    override fun toLocal(): UnknownCommand {
        return UnknownCommand()
    }
}

internal fun SetPresenter.fromLocal(): SetPresenterDto {
    return SetPresenterDto(type = "set_presenter", on = on, cid = cid)
}
package com.eyeson.sdk.model.meeting.incoming

import com.eyeson.sdk.model.api.MeetingOptionsDto
import com.eyeson.sdk.model.local.meeting.OptionsUpdate
import com.eyeson.sdk.model.meeting.base.MeetingBaseMessageDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class OptionsUpdateDto(
    @Json(name = "type") override val type: String,
    @Json(name = "options") val options: MeetingOptionsDto,
) : MeetingBaseMessageDto {
    override fun toLocal() = OptionsUpdate(options.toLocal())
}
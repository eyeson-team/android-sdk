package com.eyeson.sdk.model.meeting.incoming

import com.eyeson.sdk.model.api.MeetingOptionsDto
import com.eyeson.sdk.model.local.meeting.OptionsUpdateLocal
import com.eyeson.sdk.model.meeting.base.MeetingBaseMessageDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class OptionsUpdateDto(
    @SerialName("type") override val type: String,
    @SerialName("options") val options: MeetingOptionsDto,
) : MeetingBaseMessageDto {
    override fun toLocal() = OptionsUpdateLocal(options.toLocal())
}
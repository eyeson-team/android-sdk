package com.eyeson.sdk.model.meeting.incoming

import com.eyeson.sdk.model.api.PlaybackDto
import com.eyeson.sdk.model.local.meeting.PlaybackUpdate
import com.eyeson.sdk.model.meeting.base.MeetingBaseMessageDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class PlaybackUpdateDto(
    @Json(name = "type") override val type: String,
    @Json(name = "playing") val playing: List<PlaybackDto>
) : MeetingBaseMessageDto {
    override fun toLocal(): PlaybackUpdate {
        return PlaybackUpdate(
            playing.map {
                PlaybackUpdate.PlaybackInternal(
                    url = it.url,
                    name = it.name,
                    playId = it.playId,
                    replacementId = it.replacementId,
                    audio = it.audio ?: false,
                    loopCount = it.loopCount ?: 0
                )
            }
        )
    }
}

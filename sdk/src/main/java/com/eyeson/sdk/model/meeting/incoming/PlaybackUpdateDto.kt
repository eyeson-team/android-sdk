package com.eyeson.sdk.model.meeting.incoming

import com.eyeson.sdk.model.api.PlaybackDto
import com.eyeson.sdk.model.local.meeting.PlaybackUpdateLocal
import com.eyeson.sdk.model.meeting.base.MeetingBaseMessageDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PlaybackUpdateDto(
    @SerialName("type") override val type: String,
    @SerialName("playing") val playing: List<PlaybackDto>
) : MeetingBaseMessageDto {
    override fun toLocal(): PlaybackUpdateLocal {
        return PlaybackUpdateLocal(
            playing.map {
                PlaybackUpdateLocal.PlaybackInternal(
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

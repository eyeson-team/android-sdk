package com.eyeson.sdk.model.meeting.incoming

import com.eyeson.sdk.model.api.SnapshotDto
import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.meeting.SnapshotUpdate
import com.eyeson.sdk.model.meeting.base.MeetingBaseMessageDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SnapshotUpdateDto(
    @Json(name = "type") override val type: String,
    @Json(name = "snapshots") val snapshots: List<SnapshotDto>
) : MeetingBaseMessageDto {
    override fun toLocal(): SnapshotUpdate {
        return SnapshotUpdate(
            snapshots = snapshots.map {
                SnapshotUpdate.Snapshot(
                    id = it.id,
                    name = it.name,
                    creator = UserInfo(
                        id = it.creator.id,
                        name = it.creator.name,
                        avatar = it.creator.avatar,
                        guest = it.creator.guest,
                        joinedAt = it.creator.joinedAt
                    ),
                    createdAt = it.createdAt,
                    downloadLink = it.links.download
                )
            }
        )
    }
}

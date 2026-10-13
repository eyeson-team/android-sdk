package com.eyeson.sdk.model.meeting.incoming

import com.eyeson.sdk.model.api.SnapshotDto
import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.meeting.SnapshotUpdateLocal
import com.eyeson.sdk.model.meeting.base.MeetingBaseMessageDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SnapshotUpdateDto(
    @SerialName("type") override val type: String,
    @SerialName("snapshots") val snapshots: List<SnapshotDto>
) : MeetingBaseMessageDto {
    override fun toLocal(): SnapshotUpdateLocal {
        return SnapshotUpdateLocal(
            snapshots = snapshots.map {
                SnapshotUpdateLocal.Snapshot(
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

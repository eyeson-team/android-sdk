package com.eyeson.sdk.model.local.meeting

import com.eyeson.sdk.model.local.api.SnapshotUpdate
import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.base.LocalBaseCommand
import java.util.Date

internal data class SnapshotUpdateLocal(val snapshots: List<Snapshot>) : LocalBaseCommand {
    internal data class Snapshot(
        val id: String,
        val name: String,
        val creator: UserInfo,
        val createdAt: Date,
        val downloadLink: String
    )
}

internal fun SnapshotUpdateLocal.toLocal(): SnapshotUpdate {
    return SnapshotUpdate(
        snapshots = snapshots.map {
            SnapshotUpdate.Snapshot(
                id = it.id,
                name = it.name,
                creator = it.creator,
                createdAt = it.createdAt,
                downloadLink = it.downloadLink
            )
        }
    )
}

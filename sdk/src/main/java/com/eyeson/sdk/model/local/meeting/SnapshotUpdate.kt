package com.eyeson.sdk.model.local.meeting

import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.base.LocalBaseCommand
import java.util.*

data class SnapshotUpdate(val snapshots: List<Snapshot>) : LocalBaseCommand {

    data class Snapshot(
        val id: String,
        val name: String,
        val creator: UserInfo,
        val createdAt: Date,
        val downloadLink: String
    )
}


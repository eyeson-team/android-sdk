package com.eyeson.sdk.model.local.api

import java.util.Date

public data class SnapshotUpdate(val snapshots: List<Snapshot>) {
    public data class Snapshot(
        val id: String,
        val name: String,
        val creator: UserInfo,
        val createdAt: Date,
        val downloadLink: String
    )
}

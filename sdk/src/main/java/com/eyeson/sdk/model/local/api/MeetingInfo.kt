package com.eyeson.sdk.model.local.api

import java.util.Date

public data class MeetingInfo(
    val accessKey: String,
    val id: String,
    val name: String,
    val startedAt: Date,
    val user: UserInfo,
    val locked: Boolean,
    val guestToken: String?,
    val guestLink: String?,
    val activeRecording: Recording?,
    val activeBroadcasts: BroadcastUpdate?,
    val snapshots: SnapshotUpdate?,
    val meetingOptions: MeetingOptions,
    val playbacks: List<Playback>,
)
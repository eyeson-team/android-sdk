package com.eyeson.sdk.model.local.api

import com.eyeson.sdk.model.local.meeting.BroadcastUpdate
import com.eyeson.sdk.model.local.meeting.Recording
import com.eyeson.sdk.model.local.meeting.SnapshotUpdate
import java.util.Date

data class MeetingInfoInitial(
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
    val playbacks: List<PlaybackInitial>,
)
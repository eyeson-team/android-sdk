package com.eyeson.sdk.events

import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.call.ConnectionStatistic
import com.eyeson.sdk.model.local.meeting.BroadcastUpdate
import com.eyeson.sdk.model.local.meeting.PlaybackUpdate.Playback
import com.eyeson.sdk.model.local.meeting.Recording
import com.eyeson.sdk.model.local.meeting.SnapshotUpdate
import java.util.*

abstract class EyesonEventListener {
    open fun onPermissionsNeeded(neededPermissions: List<NeededPermissions>) {}
    open fun onMeetingJoining(
        name: String,
        startedAt: Date,
        user: UserInfo,
        locked: Boolean,
        guestToken: String,
        guestLink: String,
        activeRecording: Recording?,
        activeBroadcasts: BroadcastUpdate?,
        snapshots: SnapshotUpdate?,
        isWidescreen: Boolean
    ) {
    }

    open fun onMeetingJoined() {}
    open fun onMeetingJoinFailed(callRejectionReason: CallRejectionReason) {}
    open fun onMeetingTerminated(callTerminationReason: CallTerminationReason) {}
    open fun onMeetingLocked(locked: Boolean) {}

    open fun onStreamingModeChanged(p2p: Boolean) {}
    open fun onVideoSourceUpdate(visibleUsers: List<UserInfo>, presenter: UserInfo?) {}
    open fun onAudioMutedBy(user: UserInfo) {}
    open fun onMediaPlayback(playing: List<Playback>) {}
    open fun onBroadcastUpdate(activeBroadcasts: BroadcastUpdate) {}
    open fun onRecordingUpdate(recording: Recording) {}
    open fun onSnapshotUpdate(snapshots: SnapshotUpdate) {}
    open fun onConnectionStatisticUpdate(statistic: ConnectionStatistic) {}

    open fun onUserJoinedMeeting(users: List<UserInfo>) {}
    open fun onUserLeftMeeting(users: List<UserInfo>) {}
    open fun onUserListUpdate(users: List<UserInfo>) {}
    open fun onVoiceActivity(user: UserInfo, active: Boolean) {}

    open fun onChatMessageReceived(user: UserInfo, message: String, timestamp: Date) {}
    open fun onCustomMessageReceived(user: UserInfo, message: String, timestamp: Date) {}

    open fun onCameraSwitchDone(isFrontCamera: Boolean) {}
    open fun onCameraSwitchError(error: String) {}
}
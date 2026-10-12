package com.eyeson.sdk.events

import com.eyeson.sdk.model.local.api.BroadcastUpdate
import com.eyeson.sdk.model.local.api.MeetingInfoInitial
import com.eyeson.sdk.model.local.api.MeetingOptions
import com.eyeson.sdk.model.local.api.Playback
import com.eyeson.sdk.model.local.api.PresentationUpdate
import com.eyeson.sdk.model.local.api.Recording
import com.eyeson.sdk.model.local.api.SnapshotUpdate
import com.eyeson.sdk.model.local.api.UserInfo
import com.eyeson.sdk.model.local.statistic.ConnectionStatistic
import java.util.Date

public sealed interface EyesonEvent

public data class MeetingJoining(val meetingInfo: MeetingInfoInitial) : EyesonEvent
public data object MeetingJoined : EyesonEvent
public data class MeetingJoinFailed(val callRejectionReason: CallRejectionReason) : EyesonEvent
public data class MeetingTerminated(val callTerminationReason: CallTerminationReason) : EyesonEvent
public data class MeetingLocked(val locked: Boolean) : EyesonEvent

public data class StreamingModeChanged(val p2p: Boolean) : EyesonEvent
public data class VideoSourceUpdated(val visibleUsers: List<UserInfo>, val presenter: UserInfo?) : EyesonEvent

public data class AudioMutedBy(val user: UserInfo) : EyesonEvent
public data class MediaPlayback(val playing: List<Playback>) : EyesonEvent
public data class MediaPlaybackStarted(
    val playId: String?,
    val mediaPlaybackResponse: MediaPlaybackResponse,
) : EyesonEvent

public data class MediaPlaybackStopped(
    val playId: String,
    val mediaPlaybackResponse: MediaPlaybackResponse,
) : EyesonEvent

public data class PresentationUpdated(val presentationUpdate: PresentationUpdate) : EyesonEvent
public data class PresentationStarted(val presentationResponse: PresentationResponse) : EyesonEvent
public data class PresentationStopped(val presentationResponse: PresentationResponse) : EyesonEvent

public data class BroadcastUpdated(val activeBroadcasts: BroadcastUpdate) : EyesonEvent
public data class RecordingUpdated(val recording: Recording) : EyesonEvent
public data class SnapshotUpdated(val snapshots: SnapshotUpdate) : EyesonEvent
public data class ConnectionStatisticUpdated(val statistic: ConnectionStatistic) : EyesonEvent
public data class OptionsUpdated(val meetingOptions: MeetingOptions) : EyesonEvent

public data class UserJoined(val users: List<UserInfo>) : EyesonEvent
public data class UserLeft(val users: List<UserInfo>) : EyesonEvent
public data class UserListUpdated(val users: List<UserInfo>, val playbackPlayIds: List<String>) : EyesonEvent

public data class VoiceActivityChanged(val user: UserInfo, val active: Boolean) : EyesonEvent

public data class ChatMessageReceived(val user: UserInfo, val message: String, val timestamp: Date) : EyesonEvent

public data class CustomMessageReceived(val user: UserInfo, val message: String, val timestamp: Date) : EyesonEvent

public data class CameraOpened(val cameraName: String) : EyesonEvent
public data object CameraFirstFrameAvailable : EyesonEvent
public data object CameraClosed : EyesonEvent
public data object CameraDisconnected : EyesonEvent
public data class CameraFrozen(val error: String) : EyesonEvent
public data class CameraError(val error: String) : EyesonEvent
public data class CameraSwitchDone(val isFrontCamera: Boolean) : EyesonEvent
public data class CameraSwitchError(val error: String) : EyesonEvent

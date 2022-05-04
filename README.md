# eyeson Android SDK

Android SDK for eyeson video service incl. demo app

## Prerequisites

A webservice to host and maintain eyeson meetings is required.
The eyeson Android SDK acts as communication client for a valid meeting room *accessKey* or *guestToken*.
The respective keys can be obtained in exchange for an API key.
See API documentation at [eyeson developers](https://developers.eyeson.team/).

## Permissions 

The eyeson Android SDK uses the following permissions:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```
Add them to you manifest and make sure that you have requested the CAMERA and RECORD_AUDIO runtime permission before joining a meeting.
See the [Android documentation](https://developer.android.com/training/permissions/requesting) on how to request them.

# Usage
Create a meeting instance. For `eventListener` see [Events](#events)  

```kotlin
val eyesonMeeting = EyesonMeeting(eventListener, application)
```

## Video views
Create and bind video views. The binding of the views can be done before or after joining the meeting. \
Note that video will still be sent, event if the views are not initialized.

```xml
<org.webrtc.SurfaceViewRenderer
    android:id="@+id/remoteVideo"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />

<org.webrtc.SurfaceViewRenderer
    android:id="@+id/localVideo"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"/>
```

`eglBaseContext` can be obtained through the eyesonMeeting instance

```kotlin
binding.localVideo.init(eglBaseContext, null)
binding.localVideo.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED)
binding.localVideo.setEnableHardwareScaler(true)

binding.remoteVideo.init(eglBaseContext, null)
binding.remoteVideo.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED)
binding.remoteVideo.setEnableHardwareScaler(true)
```

Views must be released after the meeting has ended or terminated.
```kotlin
binding.localVideo.release()
binding.remoteVideo.release()
```

## Join meeting
Can either be done via `accessKey` or `guestToken`

```kotlin
eyesonMeeting.join(
    accessKey = accessKey,
    frontCamera = true,
    audiOnly = false,
    local = local,
    remote = remote,
    microphoneEnabledOnStart = true,
    videoEnabledOnStart = true
)           
```

```kotlin
eyesonMeeting.joinAsGuest(
    guestToken = guestToken,
    name = name,
    id = null, // optional 
    avatar = null, // optional URL
    frontCamera = true,
    audiOnly = false,
    local = local,
    remote = remote,
    microphoneEnabledOnStart = true,
    videoEnabledOnStart = true
)            
```

## Instance methods
```kotlin
// Leave the meeting. Doesn't end it for other participants 
fun leave() // Views still need to be released

// Set video targets
fun setLocalVideoTarget(target: VideoSink?)
fun setRemoteVideoTarget(target: VideoSink?)

// Enable/Disable outgoing video
fun setVideoEnabled(enable: Boolean)

// is local video enabled
fun isVideoEnabled(): Boolean

fun switchCamera()

fun isFrontCamera(): Boolean?

// send mute command to other participants
fun sendMuteOthers()

fun setMicrophoneEnabled(enable: Boolean)

// send chat message to all participants (including self)
fun sendChatMessage(message: String)

fun getEglContext(): EglBase.Context?
```

## Events
```kotlin
abstract class EyesonEventListener {
    open fun onPermissionsNeeded(neededPermissions: List<NeededPermissions>) {}
    open fun onMeetingJoining(
        name: String,
        startedAt: Date,
        user: UserInfo,
        locked: Boolean,
        guestToke: String,
        guestLink: String,
        activeRecording: Recording?,
        activeBroadcasts: BroadcastUpdate?,
        snapshots: SnapshotUpdate?
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


    open fun onUserJoinedMeeting(users: List<UserInfo>) {}
    open fun onUserLeftMeeting(users: List<UserInfo>) {}
    open fun onUserListUpdate(users: List<UserInfo>) {}
    open fun onVoiceActivity(user: UserInfo, active: Boolean){}

    open fun onChatMessageReceived(user: UserInfo, message: String, timestamp: Date) {}

    open fun onCameraSwitchDone(isFrontCamera: Boolean) {}
    open fun onCameraSwitchError(error: String) {}
}
```

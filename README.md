# Eyeson Android SDK

Documentation for the [Android SDK](https://docs.eyeson.com/docs/android/intro) 

## Prerequisites

A webservice to host and maintain Eyeson meetings is required.
The Eyeson Android SDK acts as communication client for a valid meeting room *accessKey* or *guestToken*.
The respective keys can be obtained in exchange for an API key.
See API documentation at [Eyeson developers](https://docs.eyeson.com/docs/rest/intro/).

# Installation
Add [JitPack](https://jitpack.io/) to your root `build.gradle` at the end of repositories:
```gradle
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

Add the dependency to your app-level `build.gradle`. Latest version: [![](https://jitpack.io/v/eyeson-team/android-sdk.svg)](https://jitpack.io/#eyeson-team/android-sdk)

```gradle
dependencies {
	implementation 'com.github.eyeson-team:android-sdk:VERSION'
}
```

# Usage
Check out the [documentation](https://docs.eyeson.com/docs/android/intro) for more detailed information about usage, events and references.


The following section shows the basic steps to join a meeting. Take a look at the sample app for the full implementation. [Compose sample](https://github.com/eyeson-team/android-sdk/tree/develop/app/src/main/java/com/eyeson/android) and [Basic View sample](https://github.com/eyeson-team/android-sdk/tree/develop/app/src/main/java/com/eyeson/android/ui/view)

### Permissions 

The Eyeson Android SDK uses the following permissions:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```
Add them to your manifest and make sure that you have requested the CAMERA and RECORD_AUDIO runtime
permission before joining a meeting. `join`, `joinAsGuest` and `connectPermalink` do not join if one
of them is missing; they return `JoinResult.MissingPermissions` with the missing ones instead. A
meeting that does not send video never opens the camera and one that does not send audio never
opens the microphone, so only the permission for what is actually sent is required (see
[Media directions](#media-directions)).

### Create meeting instance
```kotlin
val eyesonMeeting = EyesonMeeting(application)
```
### View
```xml
<com.eyeson.sdk.webrtc.VideoRenderer
    android:id="@+id/remoteVideo"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />

<com.eyeson.sdk.webrtc.VideoRenderer
    android:id="@+id/localVideo"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"/>
```
The SDK handles the EGL context internally, so `init()` takes no arguments.
```kotlin
localVideo.init()
localVideo.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)

remoteVideo.init()
remoteVideo.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
```

Renderers are independent of the meeting: the same views can be used again when an
`eyesonMeeting` instance joins another meeting. Release them with the view that holds them.
```kotlin
localVideo.release()
remoteVideo.release()
```

If your app renders or encodes video of its own, put its `EglBase` into the same share group as
the SDK's by creating it from `EyesonMeeting.eglBaseContext`. That is what keeps zero copy
rendering between your GL code and the SDK.
```kotlin
val appEglBase = EglBase.create(EyesonMeeting.eglBaseContext)
```
The SDK owns one EGL context per process. It is created on first use and never released, so
`eglBaseContext` is valid at any point: before the first `VideoRenderer`, without a meeting, and
after the last one left. There is no call order to get right.

If your app has to be the *root* of the share group rather than join the SDK's, hand your own
`EglBase` over instead. It stays yours to release and has to outlive every meeting and renderer.
Because the EGL context is process wide, it only arrives in time if nothing else of the SDK has
touched EGL yet — no `VideoRenderer.init()`, no earlier `EyesonMeeting`, and no earlier read of
`eglBaseContext`, which creates the context too. A later meeting carrying a different `EglBase`
throws an `IllegalStateException`. So the two are an either-or: own the share group this way, or
join the SDK's with `eglBaseContext`, never one after the other. Most apps do not need to own it;
joining has no such constraint.
```kotlin
val eyesonMeeting = EyesonMeeting(
    application = application,
    sdkOptions = SdkOptions(eglBase = myEglBase)
)
```

`SdkOptions` also carries the API endpoint (`apiUrl`), which defaults to the one of the build
flavor, and `videoCodecHardwareAcceleration`, which switches the video encoder and decoder to the
software codecs when set to `false`. Both are applied when the meeting is created.

### Events
All meeting events are delivered through the `events` flow. Nothing is replayed, so start collecting
before you join, otherwise the events of the joining phase are missed.
```kotlin
lifecycleScope.launch {
    eyesonMeeting.events.collect { event ->
        when (event) {
            is MeetingJoined -> Log.d(TAG, "joined")
            is MeetingTerminated -> Log.d(TAG, "terminated: ${event.callTerminationReason}")
            is ChatMessageReceived -> Log.d(TAG, "${event.user.name}: ${event.message}")
            else -> Unit
        }
    }
}
```

### Join meeting
The return value says whether the meeting is being joined. Only `JoinResult.Joining` means it is;
the outcome itself is reported through `events`.
```kotlin
when (val joinResult = eyesonMeeting.join(
    accessKey = accessKey,
    frontCamera = true,
    local = localVideo,
    remote = remoteVideo
)) {
    EyesonMeeting.JoinResult.Joining -> Unit // the outcome arrives on events

    is EyesonMeeting.JoinResult.MissingPermissions ->
        requestPermissions(
            joinResult.permissions.map { it.permissions }.toTypedArray(),
            RC_PERMISSIONS
        )

    EyesonMeeting.JoinResult.AlreadyJoined -> Unit // this instance is busy; see below
}
```
An instance holds one meeting at a time. Joining while one is running does nothing and returns
`AlreadyJoined` — the running meeting is untouched. To join a different meeting, either call
`leave()` on this instance first or use a separate `EyesonMeeting`: the limit is one meeting per
instance, not one per app. An instance whose meeting has ended needs neither — after
`MeetingJoinFailed` or `MeetingTerminated` the SDK has already left, so it can join again
immediately.

### Media directions
`mediaOptions` chooses whether audio and video are sent and received: `MediaOptions(sendAudio,
receiveAudio, sendVideo, receiveVideo)`, all `true` by default. They are fixed for the duration of
the meeting, and at least one of them has to be `true`. `getMediaOptions()` returns the ones of the
running meeting.

- Without `sendVideo` no camera is opened. It takes precedence over `videoEnabledOnStart` and a
  `screenShareInfo` passed for the start, and `setVideoEnabled`, `switchCamera`, `switchCameraTo`
  and `startScreenShare` do nothing.
- Without `sendAudio` no microphone is opened. It takes precedence over `microphoneEnabledOnStart`,
  and `setMicrophoneEnabled` does nothing.

Turning off both video directions joins a meeting without any video at all: no camera is opened, no
video is sent and none is received.
```kotlin
eyesonMeeting.join(
    accessKey = accessKey,
    frontCamera = true,
    local = null,
    remote = null,
    mediaOptions = MediaOptions(sendVideo = false, receiveVideo = false)
)
```
A listen-only participant sends nothing and needs neither CAMERA nor RECORD_AUDIO:
```kotlin
mediaOptions = MediaOptions(sendAudio = false, sendVideo = false)
```

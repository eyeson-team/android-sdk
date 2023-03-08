# eyeson Android SDK

Android SDK for eyeson video service incl. demo app

## Prerequisites

A webservice to host and maintain eyeson meetings is required.
The eyeson Android SDK acts as communication client for a valid meeting room *accessKey* or *guestToken*.
The respective keys can be obtained in exchange for an API key.
See API documentation at [eyeson developers](https://developers.eyeson.team/).

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


# Permissions 

The eyeson Android SDK uses the following permissions:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```
Add them to your manifest and make sure that you have requested the CAMERA and RECORD_AUDIO runtime permission before joining a meeting.
See the [Android documentation](https://developer.android.com/training/permissions/requesting) on how to request them.

# Usage
Create a meeting instance. For `eventListener` see [Events](#events)  

```kotlin
val eyesonMeeting = EyesonMeeting(eventListener, application)
```

## Video 
Create and bind video views. The binding of the views can be done before or after joining the meeting. \
Note that video will still be sent, event if the views are not initialized.

`eglBaseContext` can be obtained through the `eyesonMeeting` instance

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



```kotlin
binding.localVideo.init(viewModel.getEglContext())
binding.localVideo.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)

binding.remoteVideo.init(viewModel.getEglContext())
binding.remoteVideo.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
```

Views must be released after the meeting has ended or terminated.
```kotlin
binding.localVideo.release()
binding.remoteVideo.release()
```

### Jetpack Compose
```kotlin
val remoteView = rememberVideoRendererWithLifecycle(viewModel.getEglContext()) {
    viewModel.setRemoteVideoTarget(it)
}

val localView = rememberVideoRendererWithLifecycle(viewModel.getEglContext()) {
    viewModel.setLocalVideoTarget(it)
}
```

```kotlin
@Composable
fun rememberVideoRendererWithLifecycle(
    eglContext: EglBase.Context?,
    setTarget: (VideoRenderer?) -> Unit,
): VideoRenderer {
    val currentSetTarget by rememberUpdatedState(setTarget)

    val context = LocalContext.current
    val videoRenderer = remember {
        VideoRenderer(context).apply {
            init(eglContext)
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(key1 = lifecycle, key2 = videoRenderer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_CREATE) {
                currentSetTarget(videoRenderer)
            }
        }

        lifecycle.addObserver(observer)
        onDispose {
            currentSetTarget(null)
            videoRenderer.release()
            lifecycle.removeObserver(observer)
        }
    }
    return videoRenderer
}
```

```kotlin
@Composable
private fun VideoViews(
    showLocal: Boolean,
    fullSizeRemote: Boolean,
    remoteView: VideoRenderer,
    localView: VideoRenderer,
    setLocalTarget: (VideoRenderer?) -> Unit,
    modifier: Modifier = Modifier,
    modifierLocalView: Modifier = Modifier,
    wideScreen: Boolean = false
) {
    val remoteModifier = when {
        fullSizeRemote -> {
            Modifier
                .fillMaxSize()
        }
        wideScreen -> {
            Modifier.aspectRatio(16f / 9f)
        }
        else -> {
            Modifier.aspectRatio(4f / 3f)
        }
    }

    Box(modifier) {
        AndroidView(modifier = remoteModifier.align(Alignment.Center), factory = {
            remoteView
        })

        val localTarget = if (showLocal) {
            AndroidView(modifier = modifierLocalView
                .align(Alignment.BottomEnd),
                factory = { localView })

            localView
        } else {
            null
        }

        setLocalTarget(localTarget)
    }
}
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
    videoEnabledOnStart = true,
    screenShareInfo: ScreenShareInfo? = null
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
    videoEnabledOnStart = true,
    screenShareInfo: ScreenShareInfo? = null
)            
```
## Screen share
The local camera stream can be replaced with a capture of the screen. Either as a simple replacement of the own video, or as a full screen presentation.

In order to start the capturing, you need to acquire a media projection token. For that we will use the API from the [Jetpack Fragments](https://developer.android.com/jetpack/androidx/releases/fragment) library. 

```kotlin
val requestScreenCapturePermission =
registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == AppCompatActivity.RESULT_OK) {
        // Start screen share for running meeting 
        viewModel.startScreenShare(
            it.data ?: return@registerForActivityResult,
            screenCaptureAsPresentation,
            NOTIFICATION_ID,
            NOTIFICATION
        )
    }
} 

val manager = requireContext().getSystemService(MediaProjectionManager::class.java)
requestScreenCapturePermission.launch(manager.createScreenCaptureIntent())
```

From here you can join a meeting with the captured screen as your local video or switch from the local camera to the screen capture for an already running meeting. The SDK will start a foreground service on your behalf and post the provided notification.

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    requireContext().getSystemService(NotificationManager::class.java).apply {
        createNotificationChannel(
            NotificationChannel(
                "7", "CHANNEL_NAME", NotificationManager.IMPORTANCE_HIGH
            )
        )
    }
}
val notification = NotificationCompat.Builder(requireContext(), "7")
    .setOngoing(true)
    .setContentText("ScreenCapturerService is running in the foreground")
    .setContentTitle("Attention")
    .setPriority(PRIORITY_HIGH)
    .setSmallIcon(R.drawable.icon)
    .setCategory(Notification.CATEGORY_SERVICE)
    .build()

val screenShareInfo = EyesonMeeting.ScreenShareInfo(
    mediaProjectionPermissionResultData,
    42,
    notification
)

// Join meeting 
EyesonMeeting(
    eventListener = eventListener,
    application = getApplication()
).apply {
    join(
        accessKey = accessKey,
        frontCamera = true,
        audiOnly = false,
        local = local,
        remote = remote,
        microphoneEnabledOnStart = true,
        videoEnabledOnStart = true,
        screenShareInfo = screenShareInfo
    )
}

// Switch to screen share (running meeting)
eyesonMeeting.startScreenShare(screenShareInfo, asPresentation)
```

For Android API >= 33 make sure to add the `POST_NOTIFICATIONS` permission to your manifest and request the runtime permission as well.
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

In order to stop sharing call `stopScreenShare` or `leave`. This has to be done at some point, otherwise the foreground service continues to run, even if the user swipes the app from the Recents screen. To prevent this, call `leave` from your Activity /Fragment `onDestroy`

```kotlin
override fun onDestroy() {
    if (requireActivity().isFinishing) {
        eyesonMeeting.leave()
    }
    super.onDestroy()
}
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

// send chat/custom message to all participants (including self)
fun sendChatMessage(message: String)
fun sendCustomMessage(content: String)

fun getEglContext(): EglBase.Context?

fun isWidescreen(): Boolean

fun getUserInfo(): UserInfo?

// Switch to local video muted/unmuted
fun stopScreenShare(resumeLocalVideo: Boolean)

fun isScreenShareActive(): Boolean 

// Set current video (local or shared screen) as sole video 
fun setVideoAsPresentation()

// Stop the current presentation, regardless who stated it 
fun stopPresentation()
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


    open fun onUserJoinedMeeting(users: List<UserInfo>) {}
    open fun onUserLeftMeeting(users: List<UserInfo>) {}
    open fun onUserListUpdate(users: List<UserInfo>) {}
    open fun onVoiceActivity(user: UserInfo, active: Boolean){}

    open fun onChatMessageReceived(user: UserInfo, message: String, timestamp: Date) {}
    open fun onCustomMessageReceived(user: UserInfo, message: String, timestamp: Date) {}

    open fun onCameraSwitchDone(isFrontCamera: Boolean) {}
    open fun onCameraSwitchError(error: String) {}
}
```

# Audio manager 

Provides an implementation for automatic/manual audio routing during a call.

## Permissions 

Starting with Android 12 the runtime permission `BLUETOOTH_CONNECT` is needed in order to list/use connected Bluetooth devices. If the permission is not granted, Bluetooth devices can not be handled. Prior to Android 12 the `BLUETOOTH` is automatically granted.

## Usage
Create an instance of `EyesonAudioManager`. By default, the output device priority will be `Bluetooth > WiredHeadset > SpeakerPhone > Earpiece`. 

**NOTE**: When a wired headset is connected it is not possible to select earpiece as it will be overruled by the headset.

```kotlin
val audioManager = EyesonAudioManager(applicationContext)
```
When you join a meeting also start the manager and provide a listener to receive updated to selected/available devises

```kotlin
eyesonMeeting.join(
    accessKey,
    .......
)  
audioManager.start(object : EyesonAudioManager.AudioManagerEvents {
    override fun onAudioDeviceChanged(
        selectedAudioDevice: EyesonAudioManager.AudioDevice,
        availableAudioDevices: Set<EyesonAudioManager.AudioDevice>
    ) {
        // Selected/available devises will be reported here
    }
})         
```
Change the output device manually 
```kotlin
audioManager.selectAudioDevice(EyesonAudioManager.AudioDevice.SpeakerPhone)
```

After leaving the meeting also make sure to stop the manager.
```kotlin
audioManager.stop()
eyesonMeeting.leave()
```

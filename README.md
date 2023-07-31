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
Add them to your manifest and make sure that you have requested the CAMERA and RECORD_AUDIO runtime permission before joining a meeting.

### Crate meeting instance
```kotlin
val eyesonMeeting = EyesonMeeting(eventListener, application)
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
`eglBaseContext` can be obtained through the `eyesonMeeting` instance
```kotlin
localVideo.init(viewModel.getEglContext())
localVideo.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)

remoteVideo.init(viewModel.getEglContext())
remoteVideo.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
```

Views must be released after the meeting has ended or terminated.
```kotlin
localVideo.release()
remoteVideo.release()
```

### Join meeting
```kotlin
eyesonMeeting.join(
    accessKey = accessKey,
    frontCamera = true,
    audioOnly = false,
    local = localVideo,
    remote = remoteVideo
)           
```
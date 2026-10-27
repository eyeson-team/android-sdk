# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog] and this project adheres to
[Semantic Versioning].  

## [Unreleased]
### Added
- `EyesonMeeting`
  - `events`, a hot `SharedFlow<EyesonEvent>` without replay
  - `JoinResult`, returned by `join`, `joinAsGuest` and `connectPermalink`
  - `updateVideoOptions`
  - `updateScreenShareOptions`
  - `getMediaOptions`
  - `eglBaseContext`, static and valid for the whole process
- `AudioOptions`, fixed per `EyesonMeeting` instance
  - `audioMaxAverageBitrateKbps`
  - `echoCancellation`, `hardwareAcousticEchoCanceler`, `autoGainControl`, `highpassFilter`,
    `noiseSuppression`, `hardwareNoiseSuppressor`, `typingNoiseDetection`
  - `stereo`, opt-in via `@Experimental`
- `VideoOptions`
  - `videoResolution`, `videoFps`, `videoMaxBitrate`, `scaleResolutionDownBy`
- `ScreenShareOptions`
  - `screenShareResolution`, `screenShareFps`, `videoMaxBitrate`, `scaleResolutionDownBy`
- `VideoResolution`, `Resolution16by9`, `Resolution4by3`, `ScreenShareMaxResolution`
- `MediaOptions`, fixed per meeting, at least one `true`
  - `sendAudio`, `receiveAudio`, `sendVideo`, `receiveVideo`
- `SdkOptions`
  - `apiUrl`
  - `videoCodecHardwareAcceleration`
  - `eglBase`, only for apps that own the GL share group; set before the SDK first uses EGL
- Demo app
  - Video settings while in a meeting
  - Screen share settings while sharing the screen
### Changed
- `EyesonMeeting` constructor takes `audioOptions`, `videoOptions`, `screenShareOptions` and
  `sdkOptions`
  - `experimentalFeatureStereo` -> `AudioOptions(stereo = true)`
  - `customApiUrl` -> `SdkOptions(apiUrl = ...)`
- `join`, `joinAsGuest`, `connectPermalink`
  - `eventListener` removed, collect `events` before joining
  - `audioOnly` -> `mediaOptions` (`audioOnly = true` is
    `MediaOptions(sendVideo = false, receiveVideo = false)`)
  - Return `JoinResult` instead of `Unit`; `onPermissionsNeeded` -> `JoinResult.MissingPermissions`
  - `RECORD_AUDIO` only required when sending audio, `CAMERA` only when sending video
  - `mediaOptions` overrides `microphoneEnabledOnStart`, `videoEnabledOnStart` and `screenShareInfo`
- `EyesonMeeting`
  - Can join again after `leave()`
  - `MeetingTerminated` and `MeetingJoinFailed` emitted after leaving, so rejoining from their
    handler works
  - `enabledLogging` -> `enableLogging`
  - `getEglContext` -> static `eglBaseContext`
  - `setVideoEnabled`, `switchCamera`, `switchCameraTo`, `startScreenShare` no-ops without
    `sendVideo`
  - `setMicrophoneEnabled` and mutes by others ignored without `sendAudio`
- `VideoRenderer`
  - `init(sharedContext)` -> `init()`, always in the SDK's EGL share group
- `EyesonEventListener` callbacks -> `EyesonEvent`s
  - onMeetingJoining -> MeetingJoining
  - onMeetingJoined -> MeetingJoined
  - onMeetingJoinFailed -> MeetingJoinFailed
  - onMeetingTerminated -> MeetingTerminated
  - onMeetingLocked -> MeetingLocked
  - onStreamingModeChanged -> StreamingModeChanged
  - onVideoSourceUpdate -> VideoSourceUpdated
  - onAudioMutedBy -> AudioMutedBy
  - onMediaPlayback -> MediaPlayback
  - onMediaPlaybackStartResponse -> MediaPlaybackStarted
  - onMediaPlaybackStopResponse -> MediaPlaybackStopped
  - onPresentationUpdate -> PresentationUpdated
  - onPresentationStartResponse -> PresentationStarted
  - onPresentationStopResponse -> PresentationStopped
  - onBroadcastUpdate -> BroadcastUpdated
  - onRecordingUpdate -> RecordingUpdated
  - onSnapshotUpdate -> SnapshotUpdated
  - onConnectionStatisticUpdate -> ConnectionStatisticUpdated
  - onOptionsUpdate -> OptionsUpdated
  - onUserJoinedMeeting -> UserJoined
  - onUserLeftMeeting -> UserLeft
  - onUserListUpdate -> UserListUpdated
  - onVoiceActivity -> VoiceActivityChanged
  - onChatMessageReceived -> ChatMessageReceived
  - onCustomMessageReceived -> CustomMessageReceived
  - onCameraOpen -> CameraOpened
  - onCameraFirstFrameAvailable -> CameraFirstFrameAvailable
  - onCameraClosed -> CameraClosed
  - onCameraDisconnected -> CameraDisconnected
  - onCameraFrozen -> CameraFrozen
  - onCameraError -> CameraError
  - onCameraSwitchDone -> CameraSwitchDone
  - onCameraSwitchError -> CameraSwitchError
- Model packages
  - `BroadcastUpdate`, `Playback`, `PresentationUpdate`, `Recording`, `SnapshotUpdate`:
    `model.local.meeting` -> `model.local.api`
  - `ConnectionStatistic`: `model.local.call` -> `model.local.statistic`
- Defaults
  - Camera resolution `640x360` in 4:3 meetings too (was `640x480`)
  - Screen share resolution `1920x1080` (was the display resolution)
- JSON (de)serialization: Moshi -> kotlinx.serialization
- Demo app
  - View sample: data binding -> view binding
  - Predictive back disabled
  - Meeting layout follows the window size class instead of the orientation
  - `hiltViewModel` from `hilt-lifecycle-viewmodel-compose` instead of `hilt-navigation-compose`
  - Navigation Compose -> Navigation 3, with typed keys for the meeting's join modes
  - minSdk 24, required by Navigation 3
- Dependency updates
  - AndroidGradlePlugin `9.4.1`
  - Gradle `9.8.1`
  - Kotlin `2.4.21`
  - KSP `2.3.12`
  - Hilt `2.60.1`
  - OkHttp `5.4.0`
  - KotlinxCoroutines `1.11.0`
  - AndroidxCore `1.19.1`
  - AndroidxAnnotation `1.11.0`
  - AndroidxActivityCompose `1.13.0`
  - AndroidxComposeBom `2026.09.00`
  - AndroidxLifecycle `2.11.0`
  - AndroidxNavigation3 `1.2.0`
  - AndroidxHilt `1.4.0`
  - AndroidxFragment `1.9.1`
  - AndroidxAppcompat `1.8.0`
  - AndroidxConstraintLayout `2.2.2`
  - AndroidxDatastore `1.2.1`
  - Media3 `1.11.1`
  - Material `1.14.0`
  - Accompanist `0.37.3`
  - CodeScanner `2.3.2`
  - Retrofit `3.0.0`
  - KotlinxSerializationJson `1.11.0`
  - Coil `3.6.3`
  - AndroidxWindow `1.5.1`
  - AndroidxEspresso `3.7.0`
  - AndroidxTestExt `1.3.0`
- API level to 37
  - SDK: compileSdk
  - Demo apk: targetSdk and compileSdk
- Consuming apps need
  - Kotlin `2.3`+, as the SDK is built with Kotlin `2.4.21`
  - `compileSdk` `36`+, required by OkHttp `5.4.0`
  - OkHttp `5`, resolved transitively in place of OkHttp `4`
### Removed
- `EyesonEventListener`
- `LocalBaseCommand` and `model.local.meeting.OptionsUpdate` from the public API
- AndroidX Core dependency
- Demo app
  - Unused CameraX and Accompanist SystemUiController dependencies
### Fixed
- Custom API URL ignored for every `EyesonMeeting` after the first
- `join` stuck as a no-op after `onPermissionsNeeded` or `onMeetingJoinFailed`
- Crash on network failures in REST requests and user lookups; affected responses report
  `UNSPECIFIED`
- `getPermalinkMeetingInfo` throwing on network failures instead of returning `null`
- Concurrent user lookups fetching users twice and emitting events out of order
- `isFrontCamera` wrong after joining with the back camera, so `stopScreenShare` returned to the
  wrong camera
- Opus `maxaveragebitrate` duplicated, or written to a second `a=fmtp` line
## [Released]
## [1.2.0] - 2025-03-24
### Added
- version catalog `libs.versions.toml`
- build-logic
  - config convention plugins
  - constants
### Changed
- Dependency updates
  - Gradle `8.9.0`
  - CoroutinesAndroid `1.8.1`
  - AndroidxLifecycle `2.8.7`
  - AndroidMaterial `1.13.0-alpha11`
  - AndroidxAnnotation `1.9.1`
  - AndroidxAppcompat `1.7.0`
  - AndroidxComposeBom `2025.03.00`
  - AndroidxCore `1.15.0`
  - AndroidxEspresso `3.6.1`
  - AndroidxFragment `1.8.6`
  - AndroidxNavigation `2.8.9`
  - AndroidxTestExt `1.2.1`
  - Coil `2.7.0`
  - ConstraintLayout `2.2.1`
  - DatastorePreference `1.1.3`
  - DesugarJdkLibsNio `2.1.5`
  - Media3 `1.5.1`
- Demo app
  - Apply insets in View example
### Removed
-  buildSrc directory
## [1.1.19] - 2025-02-27
### Added
- MeetingInfo
  - `id`
  - `playbacks`
- MeetingInfoInitial
### Changed
- Demo app
  - MeetingActiveService start/stop
- Dependency updates
  - NavigationCompose `2.8.7`
- MeetingInfo
  - `guestToken` now nullable
  - `guestLink` now nullable
- EyesonEventListener
  - `onMeetingJoining` changed parameter to `MeetingInfoInitial`
  - `onMeetingJoined` removed meeting info parameter
- EyesonMeeting
  - `getMeetingInfo` changed to suspend function

## [1.1.18] - 2025-02-12
### Added
- EyesonMeeting
  - `setRemoteAudioEnabled`
- EyesonAudioManager
  - KDoc
- Demo App
  - Handling audio focus transient loss/gain 
### Changed
- EyesonAudioManager
  - `start` function signature
  - `requestAudioFocus` function signature

## [1.1.17] - 2025-01-31
### Added
- EyesonEventListener
  - `onCameraOpen`
  - `onCameraFirstFrameAvailable`
  - `onCameraClosed`
  - `onCameraDisconnected`
  - `onCameraFrozen`
  - `onCameraError`
- EyesonMeeting
  - `switchCameraTo`
  - KDoc
### Changed
- EyesonMeeting
  - `setLocalVideoEnabled` now also enables/disables video transmitting
- Dependency updates
  - Gradle 8.10.2
  - AGP 8.8.0
  - hilt = 2.55
  - hiltNavigationCompose = 1.2.0
### Fixed
- Guest link scan result was not handed back properly (Demo app)
## [1.1.16] - 2024-12-20
### Added
- Edge to edge support
### Changed
- Dependency updates
  - Accompanist `0.36.0`
  - Media3 `1.5.0`

## [1.1.15] - 2024-10-22
### Added
- Desugaring demo app
  - desugar_jdk_libs_nio `2.1.2`
### Changed
- Dependency updates
  - Kotlin `2.0.20` 
  - AndroidGradlePlugin `8.7.1`
  - KSP `2.0.20-1.0.25`
  - Eyeson WebrtcAndroid `1.0.1` (Supports [16 KB page sizes](https://developer.android.com/guide/practices/page-sizes))
- API level to 35
  - SDK: compileSdk
  - Demo apk: targetSdk and compileSdk
  
## [1.1.14] - 2024-10-03
### Added
- MeetingOptions
- EyesonEventListener
  - `onOptionsUpdate`
- Permalink support
- EyesonMeeting
  - `connectPermalink`
  - `getPermalinkMeetingInfo`
- CallRejectionReason
  - `SERVICE_UNAVAILABLE`
### Changed
- MeetingInfo
  - Moved `isWidescreen` -> **MeetingOptions** `widescreen`
  
## [1.1.13] - 2024-07-30
### Added
- ProGuard rules 

## [1.1.12] - 2024-07-30
### Fixed
- Some events have not been triggered

## [1.1.11] - 2024-07-16
### Added
- MeetingActiveService foreground service
### Changed
- Moved EyesonEventListener from EyesonMeeting construction to join/joinAsGuest

## [1.1.10] - 2024-07-12
### Added
- loopCount in `Playback`
### Changed
- Dependency updates
  - AndroidGradlePlugin `8.5.0`
  - Media3 `1.3.1`
- EyesonEventListener
  - `onMediaPlaybackEnded` removed
  - `onPresentationUpdate` added
  - `onPresentationStartResponse` added
  - `onPresentationStopResponse` added
### Fixed
- onMediaPlayback user id lookup

## [1.1.9] - 2024-06-26
### Added
- `FOREGROUND_SERVICE_MEDIA_PROJECTION` permission for Android SDK version >= 34

## [1.1.8] - 2024-04-11
### Added
- Root certificate `ISRG Root X1` for Android SDK version <= 25

## [1.1.7] - 2023-11-21
### Added
- Added SDK version info to `call_start`
### Changed
- Outgoing chat 
  - from WS to REST

## [1.1.6] - 2023-09-05
### Changed
- API level to 34
  - SDK: compileSdk
  - Demo apk: targetSdk and compileSdk
- Kapt to KSP for SDK
- Kotlin to 1.9.0
- Java version to 17

## [1.1.5] - 2023-07-31
### Changed
- Add loopCount parameter for startVideoPlayback
- Readme to only show basic usage and link to new [documentation](https://docs.eyeson.com/docs/android/intro)

## [1.1.4] - 2023-05-23
### Changed
- EyesonEventListener Signature changes
  - `onMeetingJoining`
  - `onMeetingJoined`
  
## [1.1.3] - 2023-05-15
### Changed
- EyesonEventListener Signature changes
  - `onUserListUpdate` to include additional media elements
  - `onMeetingJoining`
  - 
## [1.1.2] - 2023-04-24
### Added
- Media playback
### Changed
- Typo in `join` and `joinAsGuest` arguments
  - Might lead to breaking changes if named arguments are used
- onMediaPlayback's `Playback` object to include UserInfo
  - Might lead to breaking changes

## [1.1.1] - 2023-03-30
### Added
- Support for custom API url (internal use)

## [1.1.0] - 2023-03-10
### Added
- Custom message support
- New `CallRejectionReason` and `CallTerminationReason`
  - UNSPECIFIED
- New UI for demo app (compose)
  - View example reduced to basic setup and connection
- VideoRenderer
  - Replacement for `SurfaceViewRenderer`

### Changed
- Moved messaging from data channels to SEPP
  - except for voice activity
- Removed local `libwebrtc.aar`
  - moved to own [repo](https://github.com/eyeson-team/webrtc-android)

### Fixed
- Audio state was not properly set when initial microphone state was `muted`

## [1.0.3] - 2022-10-14
### Added
- Screen share
  - As full screen presentation or replacement of own video
  - option to stop ongoing presentations
- Permissions and service declaration to the SDK manifest
- AudioManager
- Experimental supported for stereo (opt in)
- Launcher icon

### Changed
- targetSdk and compileSdk to API level 33
- `EyesonMeeting.join` and `EyesonMeeting.joinAsGuest` method signature to support joining a meeting with screen share already on
  - default signature unchanged
- Set bitrateKbps to 64
- Update libwebrtc
  - Based on [4d47e0b2bec2c2bfeaa1e6dd40741c901414d22f](https://webrtc.googlesource.com/src/+/4d47e0b2bec2c2bfeaa1e6dd40741c901414d22f)
  


[Keep a Changelog]: http://keepachangelog.com/en/1.0.0/
[Semantic Versioning]: http://semver.org/spec/v2.0.0.html

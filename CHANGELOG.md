# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog] and this project adheres to
[Semantic Versioning].  

## [Unreleased]
## [Released]
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

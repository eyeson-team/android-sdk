# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog] and this project adheres to
[Semantic Versioning].  

## [Unreleased]
## [Released]
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

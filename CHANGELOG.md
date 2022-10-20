# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog] and this project adheres to
[Semantic Versioning].  

## [Unreleased]
### Added
- Custom message support

### Changed
- Moved messaging from data channels to SEPP
  - except for voice activity

## [Released]
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

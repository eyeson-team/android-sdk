# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog] and this project adheres to
[Semantic Versioning].  

## [Unreleased] 
### Added
- Screen share 
  - As full screen presentation or replacement of own video 
  - option to stop ongoing presentations
- Permissions and service declaration to the SDK manifest  
- AudioManager
- Stereo for supported devices 
- Launcher icon

### Changed
- targetSdk and compileSdk to API level 33
- `EyesonMeeting.join` and `EyesonMeeting.joinAsGuest` method signature to support joining a meeting with screen share already on
  - default signature unchanged
- set bitrateKbps to 64

## [Released]


[Keep a Changelog]: http://keepachangelog.com/en/1.0.0/
[Semantic Versioning]: http://semver.org/spec/v2.0.0.html

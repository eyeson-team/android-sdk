# AGENTS.md

Guidance for coding agents working in this repository.

## Overview

The **Eyeson Android SDK** is a Kotlin/Android library (`:sdk`, namespace `com.eyeson.sdk`) for joining
[eyeson](https://www.eyeson.com) video meetings: it talks to the eyeson REST API, the meeting and
signalling WebSockets, and WebRTC. `:app` is a demo app that exercises it.

Consumer-facing usage lives in [README.md](README.md) — link to it rather than restating it.

## Common Commands

```bash
./gradlew :sdk:compileProductionDebugKotlin :app:compileProductionDebugKotlin  # Fastest check
./gradlew :sdk:assembleProductionRelease                                       # The published variant
./gradlew :sdk:publishToMavenLocal                                             # com.eyeson:sdk:<VERSION>
./gradlew :app:assembleProductionDebug                                         # Demo app
```

Use `production` task names only. Other flavors come from gitignored `*/flavor-configurations.gradle`
and don't exist on a fresh clone. Don't run bare `./gradlew test` — it fans out across every flavor ×
build type.

There is no CI, no linter/formatter and no API dump check, so compilation and careful review are
the safety nets. WebRTC, signalling or meeting-UI changes need the demo app run against a live meeting.

## Architecture

### Module Structure

```
sdk/                 # The published library
app/                 # Demo app: Compose sample (ui/start, ui/meeting, ui/settings)
                     #   plus the legacy View sample (ui/view/, linked from the README)
build-logic/         # Included build with convention plugins + EyesonConstants
gradle/libs.versions.toml
```

### SDK Packages (`com.eyeson.sdk`)

```
EyesonMeeting.kt         # Public entry point: join/leave, media control, events flow
EyesonAudioManager.kt    # Public audio routing helper (process-global audio state)
options/                 # Public: SdkOptions, MediaOptions, AudioOptions, VideoOptions
events/                  # Public: EyesonEvent (sealed) and its payload types
model/local/api|statistic/  # Public models; everything else under model/ is internal
network/                 # REST (Retrofit), meeting + signalling WebSockets
callLogic/               # CallLogic: wires signalling to the PeerConnection
webrtc/                  # PeerConnectionClient, VideoRenderer (public), EyesonEglBase
moshi/adapter/           # Polymorphic adapters for incoming messages
service/                 # ScreenCapturerService (merged into consumer manifests)
di/NetworkModule.kt      # Hand-written DI; :sdk has no Hilt
utils/Logger.kt          # The only logger to use (never android.util.Log)
```

### How It Works

1. **Join**: `EyesonMeeting.join()`/`joinAsGuest()`/`connectPermalink()` fetch the meeting via REST,
   then open the meeting WebSocket (`MeetingConnection`) and the signalling WebSocket
   (`SignalingConnection`).
2. **Call**: `CallLogic` drives `PeerConnectionClient` from signalling messages. `MediaOptions` maps
   onto `PeerConnectionParameters.send*/receive*`.
3. **Events**: wire DTOs → `toLocal()` → internal `*Local` commands → mapped to public `EyesonEvent`s
   in `EyesonMeeting.handleWebSocketEvents`/`handleCallEvents`, emitted on the hot `events`
   `SharedFlow` (`replay = 0`).
4. **Leave**: `leave()` cancels the scope's children, not the scope, so an instance can re-join.

## Key Technologies

- **Kotlin 2.0**, **AGP 8.12**, min SDK 23, compile/target SDK 35, Java 17 bytecode
- **OkHttp + Retrofit + Moshi** (codegen) for REST and WebSockets
- **Kotlin Coroutines/Flow** for the public events API
- **eyeson-webrtc-android** exposed via `api(...)`, so `org.webrtc.*` types are part of the public API
- Demo app: **Jetpack Compose**, **Hilt** (KSP), **DataStore**, plus kapt for the View sample's
  data binding

Versions live in `gradle/libs.versions.toml` (the `gradle` key is the AGP version) and
`build-logic/convention/src/main/kotlin/EyesonConstants.kt`. SDK levels and Java settings live in
`build-logic/.../extensions/KotlinAndroid.kt`.

## Public API

The public surface is `EyesonMeeting`, `EyesonAudioManager`, `VideoRenderer`, `options/`, `events/`,
`model/local/api/` and `model/local/statistic/`. Everything else is `internal`. `:sdk` uses strict
`explicitApi()`, so public declarations need an explicit `public` and return type.

Nothing checks binary compatibility, so review each public change by hand. Common breaks:

- Adding or reordering a defaulted parameter in `EyesonMeeting`'s constructor.
- Adding an `EyesonEvent` subtype (it is `sealed`; consumers' exhaustive `when`s break).
- Bumping `eyeson-webrtc-android`.
- Adding a permission to `sdk/src/main/AndroidManifest.xml` (it merges into every consumer).

## Rules and Recipes

- **New `EyesonEvent`**: add the subtype, the emit branch in `handleWebSocketEvents`/`handleCallEvents`
  (those `when`s are non-exhaustive, so a missing branch silently never fires), and a branch in the
  demo's `MeetingViewModel.handleEvent` (which has no `else` on purpose).
- **New incoming message type**: enum entry, `@JsonClass(generateAdapter = true)` DTO,
  `.withSubtype(...)` in the matching `moshi/adapter/*Adapter.kt`, and `toLocal()`. Without the
  adapter entry it deserialises to `Unknown*Dto` and is dropped.
- **Events**: emit via `emitEyesonEvent()`; terminal events go only through `leaveAndEmit()`. Never
  touch `_events` directly. Code after the cancel in `leave()` must not suspend.
- **EGL**: `EyesonEglBase` is process-wide and never released. `SdkOptions.eglBase` throws if the SDK
  already created one — keep that throw; mismatched share groups mean black video.
- **`VideoRenderer`**: layout/EGL methods require the main thread; frame callbacks run on WebRTC
  threads. Never call `release()` from a frame callback.
- **Build**: new repositories go in `settings.gradle.kts`. `consumer-rules.pro` and the tail of
  `sdk/proguard-rules.pro` duplicate rules — change both. Don't add `jvmToolchain(...)`.
- **Line endings**: many files are CRLF (`core.autocrlf=true`). Don't normalise them.
- **Git**: the branch may hold someone else's uncommitted work. Commit only your own paths
  (`git commit -- <path>...`); no `git add -A`, `stash` or `reset --hard` without looking first.
- `gstreamercapture/` is untracked and belongs to another branch; ignore it.

## Demo App

- Screens follow `XRout` (stateful, Hilt ViewModel; the spelling is the existing convention) → `XScreen` (stateless) plus an
  `EyesonDemoTheme`-wrapped `@Preview`. User-visible strings go in `strings.xml`.
- Demo new SDK features in the Compose sample, but keep `ui/view/` compiling.
- Apps own audio: pair `EyesonAudioManager.stop()` with `leave()` (see `MainViewModel.disconnect()`).

## Version & Publishing

- Gradle publishes `com.eyeson:sdk`; consumers use JitPack's `com.github.eyeson-team:android-sdk`.
  The mismatch is expected.
- The integration branch is `develop` (`main` is stale). Topic branches are `feature/<snake_case>`.
- `EyesonConstants.VERSION` is also sent to the backend, so bump it only in a release commit.
- **Release**: bump `VERSION`, move CHANGELOG's `## [Released]` divider up, add
  `## [X.Y.Z] - YYYY-MM-DD`, commit, then push an annotated tag `X.Y.Z` (no `v`). Pushing the tag
  publishes; never re-tag.

## Commit Conventions

All new commits follow [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/):

```
<type>[(<scope>)][!]: <short description>

[optional body]

[optional footer(s)]
```

- Types: `feat`, `fix`, `docs`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`.
- Scope is optional. When used, pick one of `sdk`, `app`, `build`, `deps`, `readme`, `changelog`,
  or a finer one (`events`, `webrtc`, `network`, `options`).
- Subject: imperative, lowercase, no trailing period.
- Public-API changes need `!` **and** a `BREAKING CHANGE:` footer describing the migration:

```
refactor(sdk)!: replace EyesonEventListener with the events flow

BREAKING CHANGE: join()/joinAsGuest() no longer take an EyesonEventListener;
collect EyesonMeeting.events instead.
```

## CHANGELOG

[Keep a Changelog](https://keepachangelog.com/) + SemVer, using only `Added`/`Changed`/`Fixed`/`Removed`.
Entries are terse noun phrases without a trailing period, nested by API surface; demo-only changes go
under `- Demo app`. Dependency bumps use `- Dependency updates` / `  - <Lib> \`<version>\``. Preserve
trailing whitespace (it marks hard breaks). Any public-API change needs an `[Unreleased]` entry and a
README update.

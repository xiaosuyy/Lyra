[English](./README_EN.md) | [中文](./README.md)

<h1 align="center">Lyra</h1>

<div align="center">

<h3>✨ A local-first native Android audio player focused on lyrics and user-owned data 🎵</h3>

<p>
  <img alt="Android 9+" src="https://img.shields.io/badge/Android-9%2B-3DDC84?logo=android&logoColor=white" />
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.x-7F52FF?logo=kotlin&logoColor=white" />
  <img alt="License" src="https://img.shields.io/badge/License-GPL--3.0-blue" />
</p>

<p>
  <img src="icon/lyra.png" width="260" alt="Lyra logo" />
</p>

<p>
Lyra is a local-first native Android audio player built around a local library,
online playback, a rich lyrics experience, and user-owned data.
</p>

<p>
The app is written in native Android and supports Android 9 (API 28) and above.
</p>

🛠️ <strong>Active development</strong>

</div>

> [!WARNING]
> This project is for learning and research purposes only. Do not use it for illegal purposes.
> Access, play, or save content only when you have the rights, authorization, or relevant permission to do so.
> This project does not provide media content, keys, paywall/DRM/region-bypass mechanisms, a public media proxy, or media redistribution.
>
> This project and its maintainer do not accept any form of sponsorship, donation, or commercial funding.

---

> [!NOTE]
> Lyra does not provide a public cloud music library or media distribution service.
> Online playback requires authorization completed by the user on the device.
> Paid or restricted content still follows the applicable rules.

---

## Start here

If you just want to try the app, jump to [Getting Started](#getting-started).
If you want to know what the project can do, read [Why it stands out](#why-it-stands-out)
and [Key Features](#key-features).
If you plan to contribute, read [CONTRIBUTING_EN.md](./CONTRIBUTING_EN.md) first.

```text
Lyra
├── Local-first: cache, downloads, playlists, history, stats, settings
├── Online playback: search, playlists, favorites, and artist pages after user authorization
├── Optional sync: back up metadata to remote storage you control
├── Playback: Media3, lyrics, queue, fluid background, home screen widgets, floating/status bar lyrics
└── Recoverable runtime: safe mode, crash logs, ANR records, debug probes
```

## About

Lyra is a native Android audio player built on **Jetpack Compose + Media3**.
It does not operate a public cloud service. After the user completes account
authorization on the device, it provides online search and playback while
offering a complete local experience for playback, downloads, caching,
playlists, and backup/sync.

Current positioning:

- **Authorization is the capability**: online content requires the user to
  sign in on the device; credentials stay local.
- **Local-first**: playback cache, downloads, playlists, history, settings,
  and credentials are stored on the device by default.
- **Optional sync**: playlist, favorite, recent playback, and playback stats
  metadata can be synced to remote storage the user controls; nothing is sent
  to a centralized service operated by the maintainer.
- **Privacy and account safety**: no advertising SDK, third-party analytics,
  or crash analytics SDK; local playback history or stats are never written
  back to any online service.
- **Single Activity + Compose**: `MainActivity` is the only entry point; the UI
  is organized by a Compose `NavHost`, a dynamic bottom bar, the Mini Player,
  and the Now Playing overlay.
- **Startup and recovery**: the normal flow is
  `Loading -> Disclaimer -> Main`; after a crash or system ANR, the app enters
  `Safe Mode` first.
- **Test guardrails**: downloads and storage, sync merge, lyrics parsing,
  playback policy, config backup, and safe mode have unit or device tests.

---

## Why it stands out

- **Local-first, with a serious offline experience**:
  `NetworkStatusMonitor` detects offline state from the default network,
  and `offlineCachedImageRequest` blocks remote image requests while offline,
  preferring cached images. Home, Explore, Now Playing, lyrics, playlists, and
  downloads all receive `offlineMode`, so local files, downloaded audio,
  playback cache, cached artwork, and local playlists remain usable without a network.
- **Playback resilience is more than retries**:
  `PlayerManager` owns resolution, the queue, and failure recovery. When the
  current item is unplayable it tries available degradation strategies and can
  match a playable candidate as a fallback; playback errors refresh the current
  link, and consecutive failures skip or stop rather than loop forever.
- **Multi-level session fallback**:
  sessions keep valid identity information and maintain caches separately from
  anonymous sessions; rejected playback candidates move to a backup path instead
  of being retried indefinitely.
- **GLSL/AGSL fluid background**:
  the Now Playing background is loaded by `BgEffectPainter` from
  `assets/shaders/hyper_background_effect.glsl` and rendered frame-by-frame via
  `RuntimeShader`. The shader derives colors from the artwork, mixes dynamic
  blobs and subtle grain, and reacts to `uMusicLevel / uBeat` instead of simply
  blurring the cover.
- **Deep lyrics experience**:
  `SyncedLyricsView` and `AdvancedLyricsView` support line-by-line and
  word-by-word LRC (including end-of-line and bracket timestamps) as well as
  TTML highlighting, translations, transliteration, offset adjustment,
  tap-to-seek, long-press sharing, depth blur, edge fading, and full-screen
  lyrics. `LyricShareSheet` lets you select lines, copy text, share a song, or
  render a 1080px lyric card, and line highlighting is interpolated by both
  line range and original timestamps with drag-to-seek support.
- **Recoverable startup and runtime**:
  after a crash or ANR the app enters safe mode, keeps crash logs and ANR
  records, and disables high-risk modules so problems can be diagnosed.
- **Native desktop integration**:
  home screen widgets, launcher shortcuts, cast/output selection, floating
  lyrics, and status bar lyrics, with playback state synchronized with system
  media controls.

---

## Getting Started

### a. Download a Release (recommended)

1. Open the Releases page of this repository and download the latest version.
2. Which build should I choose?
- Most phones: `arm64-v8a`
- Older 32-bit devices: `armeabi-v7a`
- `x86` / `x86_64`: mostly for emulators, Intel devices, or Chromebooks

> [!IMPORTANT]
> The Release channel is not a strictly stable channel. Versions are published
> manually after a batch of features and may still contain undiscovered issues.

### b. Download a CI build

1. Open the Actions page of this repository and download the artifacts from
   the latest successful build.

> Default CI uploads an `arm64-v8a` APK; the manual release flow builds
> multiple ABI APKs.

### c. Build locally

1. Clone the repository with submodules:
   ```bash
   git clone --recursive <repository-url>
   cd Lyra
   ```
2. Open the project in the latest stable Android Studio and sync dependencies.
3. Build the debug variant:
   ```bash
   ./gradlew :app:assembleDebug
   ```
4. Install the APK:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
5. On first launch you only need to read the disclaimer. The playback
   notification permission can be enabled in Settings > General; local music
   permission is requested when you scan local files.
6. For developer tools, tap the **version number** seven times in Settings to
   enable developer mode; a `Debug` tab appears in the bottom bar.

> DEBUG builds are for testing only; performance and size do not represent the
> release build.

See [CONTRIBUTING_EN.md](./CONTRIBUTING_EN.md) for the release build and
signing flow.

---

## Key Features

- 🎧 **Local library and online playback**:
  local scanning, import, downloads, and caching; after authorization, online
  search, playlists, favorites, and artist pages.
- 🏠 **Home and continue playback**:
  home shows frequently used playlists, recommendations, and continue playback;
  recommendation sections can be refreshed on demand.
- 🗂️ **Library categories**:
  `Library` groups local, favorites, and online content, with secondary
  categories and search.
- 🎼 **Playlists and favorites**:
  create playlists, import/export, batch download, favorite songs, and follow
  artists. Playlist detail pages use the artwork's dominant color as the
  background, with list text and buttons tinted by background luminance.
- ▶️ **Playback control**:
  a real playback queue, shuffle and repeat, resume position, and automatic
  recovery from playback failures; queue actions include play next, add to
  queue, remove, and drag to reorder.
- 🎤 **Lyrics**:
  line, word, translated, and transliterated lyrics with offset adjustment,
  tap-to-seek, full-screen lyrics, floating lyrics, status bar lyrics, and
  lyric card sharing.
- 🎨 **UI and motion**:
  fluid animated background, glass bottom chrome, swipe-to-change songs on the
  mini player, cover flight animation, and artwork-based color themes.
- 📥 **Downloads and backup**:
  download management and cache cleanup, config export/restore, and optional
  remote metadata sync.
- 🧩 **Desktop integration**:
  home screen widgets, launcher shortcuts, and cast/output device selection.
- 🛡️ **Stability**:
  safe mode, crash logs, ANR records, and debug probes.

---

## Implementation Notes

### Build and versioning

- Native Android, Kotlin + Jetpack Compose.
- `minSdk 28`, targeting Android 9 and above.
- Media3 provides playback and system media controls.
- Gradle builds multiple ABIs; releases are signed by CI with a controlled key.

### Module structure

- `app/`: application shell with UI, playback core, data layer, and local storage.
- `core/`: playback, data, sync, logging, and other infrastructure.
- `ui/`: Compose screens, components, and theming.
- `data/`: Room, DataStore, and repository implementations.
- `docs/`: internal design notes and refactoring records.

### Entry and navigation

- `MainActivity` is the only entry point; a Compose `NavHost` organizes screens.
- The dynamic bottom bar, Mini Player, and Now Playing overlay span the main screens.
- Navigation and playback state are driven by shared ViewModels and state flows.

### Playback, cache, and service

- `PlayerManager` owns the active queue, index, and playback state, and handles
  failure recovery.
- Playback cache and downloads are managed separately; local content is
  preferred when offline.
- System media controls stay synchronized through MediaSession and notifications.

### Local data and security

- Playlists, favorites, history, and stats are stored in the local database.
- Settings and preferences are persisted with DataStore.
- Credentials and sync secrets are encrypted with the system Keystore.
- Exported config files contain sensitive data and are for personal migration only.

---

## Known Limitations

- Availability of online content depends on the authorization scope of the
  user's account and the applicable rules; some content may be unplayable due
  to permissions or region restrictions.
- The project does not provide ways to bypass payment, DRM, or region limits.
- Lyrics and artwork matching relies on public information; results may be
  delayed or inaccurate and can be adjusted manually.
- Playback stability depends on network conditions and online service status.
- Some advanced features depend on the system version and device capabilities.

---

## Privacy

- Lyra does not operate its own public media distribution service and does not
  include advertising SDKs, third-party analytics, or crash analytics SDKs.
- Playback cache, downloads, local playlists, history, stats, settings, and
  credentials are stored on the user's device by default.
- If the user enables remote sync, only metadata such as playlists, favorites,
  history, and playback stats is synced.
- Audio cache, downloads, credentials, and playback tokens are never uploaded
  to the developer.
- Android cloud backup / device transfer is disabled by default.

---

## Bug Report

Please use the repository's Issues to report bugs, repro steps, or logs.
Search for existing reports first and include device model, system version, and
a reproduction path when possible.

---

## Update Cycle

- The project is under active development; releases are published manually by
  feature batch.
- Playback, local data, sync, and recovery paths are maintained first.

---

## Acknowledgments

Thanks to Jetpack Compose, Media3, Coil, OkHttp, Kotlin Coroutines, and other
open-source projects that provide the foundation for Lyra.

---

## License

Lyra is released under the **GPL-3.0** license.

- ✅ You may use, modify, and distribute the software freely.
- ⚠️ Modified distributions under the root GPL-3.0 must continue to comply with GPL-3.0.
- 🧩 `app/src/main/cpp/README.md` grants a replacement license with attribution
  conditions only for the original native sources listed there; third-party
  sources and content outside that scope are not covered.
- ✍️ External native contributions are not automatically included in the
  replacement license scope unless the contributor explicitly records consent.
- 📚 See [LICENSE](./LICENSE) for the full terms.

---

# Contributing to Lyra

Read [CONTRIBUTING_EN.md](./CONTRIBUTING_EN.md) before contributing.

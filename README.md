# GlassTune

Offline Android music player with a frosted-glass UI (Kotlin, Jetpack Compose, Media3).

## Versions
AGP 8.5.2, Kotlin 2.0.20 (Compose compiler plugin), Gradle 8.7, JDK 17, compileSdk/targetSdk 34, minSdk 26,
Compose BOM 2024.09.00, Media3 1.4.1, Coil 2.7.0, Room 2.6.1.

## Features
- MediaStore library scan with auto-refresh on MediaStore changes, album art, search (with natural-language
  modifiers like `longer than 3 min`, `artist:name`, `album:name`), 7 sort modes
- Browsing: Songs / Albums / Artists / Folders / Favorites / Recent / Playlists / History / Top (most played)
- Media3 ExoPlayer in a foreground MediaSessionService: background playback, notification, lock screen,
  Bluetooth buttons, audio focus, headphone-unplug pause, offline voice search ("play <title>" via Assistant)
- Queue: play next, add, remove, jump, reorder, clear; queue and playback position survive app restarts
- Software audio FX (work on every device): 10-band graphic EQ with presets, stereo balance,
  approximate volume normalization — plus hardware Equalizer / Bass Boost / Virtualizer / Loudness Enhancer
  with graceful fallback where unsupported, custom presets (Room), per-device EQ profiles
- Reverb presets (PresetReverb), waveform visualizer (optional, runtime mic permission)
- Playlists (Room): create / rename / delete / reorder, M3U import & export
- Lyrics: synced LRC rendering, editor, import, stored per song
- Backup & restore (JSON): playlists, favorites, EQ presets, lyrics, FX settings, theme
- Sleep timer with 10 s fade-out, playback speed (0.75x–2x)
- Home-screen widget: play/pause, next, previous, favorite, album art (decoded off the main thread)
- Glass UI, Frosted Dark / AMOLED Black / Light themes, dynamic accent from artwork, Marvel wallpapers
- Real-time audio-reactive visuals: 10 visualizer styles (neon spectrum, liquid wave, particles, lasers,
  aurora, LED EQ, electric pulse, glass wave, dual side bars, minimal waveform) flowing on both sides of
  Now Playing, driven by true FFT analysis of the playing audio (mic permission used only for capture)
- Beat-reactive album artwork (glow, energy ring, subtle scale), immersive fullscreen visualizer mode
- Visual Effects settings: master/left/right toggles, style, intensity, speed, color presets,
  artwork glow, beat pulse, performance mode — all persisted
- Permission screen with denied state + settings shortcut

## Known limitations
- Volume normalization is an approximate rolling-RMS normalizer, not ReplayGain-tag based
- Crossfade and gapless configuration are not implemented (depends on ExoPlayer audio processing support)
- The 10-band graphic EQ is software DSP applied to the player's PCM stream (not lossless bypass)
- Android Auto uses the default MediaSession behavior; no custom browse tree yet
- Release APK is unsigned (debug signing only); add a signing config before distribution

## Build the APK on GitHub (no Android Studio needed)
1. Open the repo's **Actions** tab and wait for "Build GlassTune APK" to finish (or run it via "Run workflow").
2. Open the finished run, download the artifact **GlassTune-debug-apk** (or **GlassTune-release-apk**),
   unzip it and install the `.apk`.

## Build in Android Studio
Open this folder, let Gradle sync, press Run.

## Build

The debug and release APKs plus unit tests run in the GitHub Actions workflow in `.github/workflows/build.yml`.
Unit tests (LRC parser, M3U import/export) live in `app/src/test/`.

# GlassTune

Offline Android music player with a frosted-glass UI (Kotlin, Jetpack Compose, Media3).

## Versions
AGP 8.5.2, Kotlin 2.0.20 (Compose compiler plugin), Gradle 8.7, JDK 17, compileSdk/targetSdk 34, minSdk 26,
Compose BOM 2024.09.00, Media3 1.4.1, Coil 2.7.0.

## Features in this build
- MediaStore library scan (real device music), album art, search, sort, Albums / Artists / Folders / Favorites / Recent
- Media3 ExoPlayer in a foreground MediaSessionService: background playback, notification, lock screen, Bluetooth buttons, audio focus, headphone-unplug pause
- Now Playing: seek, shuffle, repeat off/all/one, queue (play next, add, remove, jump), speed, favorites, sleep timer with fade-out
- Glass UI, Dark / AMOLED / Light themes, 5 accent colors (persisted)
- Permission screen with denied state + settings shortcut

Not included yet: equalizer/audio effects, playlists (Room), lyrics, M3U, backup, widgets, queue reorder.

## Build the APK on GitHub (no Android Studio needed)
1. Open the repo's **Actions** tab and wait for "Build GlassTune APK" to finish (or run it via "Run workflow").
2. Open the finished run, download the artifact **GlassTune-debug-apk**, unzip it and install `app-debug.apk`.

## Build in Android Studio
Open this folder, let Gradle sync, press Run.


## Build

The Android debug APK is built by the GitHub Actions workflow in `.github/workflows/build.yml`.

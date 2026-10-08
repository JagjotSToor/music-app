# AURALIS by ZHEHR — v0.2 (local music player)

## What works
Library scan (incremental), Songs/Albums/Artists/Folders/Playlists, favorites, play history (Recently/Most played),
search, sort, Media3 background playback with notification + lock screen + Bluetooth, audio focus, headphone-unplug pause,
queue (reorder, remove, clear, save as playlist, persists across restarts), shuffle/repeat, speed, sleep timer,
mini-player, Now Playing (artwork-derived colors, cached blur, 3D tilt card), themes (System/Dark/AMOLED/Light), reduce motion.

## Not built yet
Equalizer/effects, visualizer, lyrics, tag editing, Theme Studio/photo themes, fuzzy search, drag-to-reorder,
crossfade, notification artwork guarantees, genres, online features (skipped on purpose).

## Get an APK without Android Studio (GitHub Actions)
1. Create a free GitHub account and a new empty repository.
2. On a PC, unzip this project, then in the folder run:
   git init && git add . && git commit -m "AURALIS v0.2" && git branch -M main
   git remote add origin <your repo URL> && git push -u origin main
3. On GitHub open the Actions tab > "Build AURALIS APK". When it finishes (about 5-10 minutes), open the run and
   download the artifact "AURALIS-debug-apk". It contains app-debug.apk.
4. Send app-debug.apk to your phone, open it, and allow "install unknown apps" for your file manager or browser.
If the build fails, open the failed step, copy the red error lines and send them to Claude.

## Or build locally
Open this folder in Android Studio, let Gradle sync, plug in the phone with USB debugging, press Run.

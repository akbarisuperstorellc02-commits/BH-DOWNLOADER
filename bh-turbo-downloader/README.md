# BH Turbo Downloader

Clean-room Android downloader project. This implementation is built independently in this directory.

## Product
- Kotlin + Jetpack Compose native Android app
- Premium dark, depth-inspired 3D glass visual style
- Internal WebView browser with download interception and hardened WebView settings
- HTTP/HTTPS downloads using segmented byte ranges when supported, with safe single-stream fallback
- Maximum 8 simultaneous segments per supported download
- Live progress and recent-download dashboard
- Pause and resume: single-stream tasks resume with HTTP Range; paused segmented tasks restart safely
- Persisted task history and partial transfer files in app-private durable storage
- Default public output on Android 10+: `Download/TurboBH`
- User-selectable Storage Access Framework destination with persisted permission
- App languages: English, Afghan Dari, Pashto and Arabic
- UI progress values use ASCII/English digits

## Implemented screens
- Home: URL download entry, turbo engine panel, recent task cards
- Browser: internal WebView, web navigation and download interception
- Files: task history, progress, pause and resume actions
- Settings: destination folder picker and per-app language selector

## Important limitations
- CI is building the debug APK; the build result must be checked before distributing an APK.
- Transfers currently run in the application process. WorkManager/foreground-service execution for reliable downloads after the app is swiped away is still pending.
- On Android 9 and older, the fallback destination is app-specific Downloads unless the user selects a folder.
- Browser tabs, browser history, file-type detection, retry policy, and automatic restart of interrupted downloads remain future work.
- A paused single-stream transfer can resume if the server honors HTTP Range. A segmented transfer that is interrupted is restarted safely rather than incorrectly continuing from a preallocated file.

## Build
Requirements: JDK 17, Android SDK Platform 36, Build Tools 36.0.0, Gradle 8.13.

From this directory:
```sh
gradle --no-daemon :app:assembleDebug
```

GitHub Actions workflow: `.github/workflows/bh-turbo-android.yml`

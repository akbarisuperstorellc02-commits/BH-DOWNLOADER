# BH Turbo Downloader

Clean-room Android downloader project. The application is being developed independently in this directory; the previous app source is not reused.

## Product direction
- Native Android application built with Kotlin and Jetpack Compose
- Premium 3D-inspired dark glass interface with layered gradients, depth, borders and elevated panels
- Internal WebView browser
- HTTP/HTTPS download engine with segmented byte-range transfer when supported by the server and single-stream fallback otherwise
- Download history persistence, with interrupted tasks identified on the next launch
- Public default destination: `Download/TurboBH` on Android 10 and newer
- Planned selectable destination folder, pause/resume, retries and background execution
- Planned UI languages: Afghan Dari, Pashto, Arabic and English
- English digits in every supported language

## Current implementation
- Native Android project scaffold (Kotlin, Compose, Material 3)
- Multi-screen home, browser, library and settings UI
- Dark layered visual system and 3D-inspired hero panel
- Download engine connected to the home URL action
- WebView download interception connected to the same engine
- Live transfer progress and task status in the library
- Download task history persisted locally (up to 100 records)
- Android 10+ saves completed files to the public `Download/TurboBH` directory through MediaStore
- GitHub Actions workflow configured to build a debug APK and upload it as an artifact

## Remaining work / verification
- Build and device verification are still pending; no APK is claimed as successfully built yet.
- On Android versions below 10, downloads currently use app-specific storage.
- User-selectable folder via Storage Access Framework is not implemented yet.
- Pause/resume, retry controls, durable background execution and robust restart recovery are not implemented yet. Persisted active tasks are marked Interrupted rather than automatically resumed.
- Localization is planned but the current UI is English.
- Browser tabs, download confirmation UI and full browser history are not implemented.

## Build requirements
- JDK 17
- Android SDK Platform 36 and Build Tools 36.0.0
- Gradle 8.13
- Android Gradle Plugin 8.13.2

From this directory:
```sh
gradle --no-daemon :app:assembleDebug
```

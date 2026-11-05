# BH Turbo Downloader

Clean-room Android downloader project. The application source is being developed independently in this directory; the previous app source is not reused.

## Product direction
- Android application built with Kotlin and Jetpack Compose
- Premium 3D-inspired dark glass interface with layered gradients, depth, borders and elevated panels
- Internal WebView browser shell
- HTTP/HTTPS download engine with byte-range segmented transfer when the server explicitly supports it; single-stream fallback otherwise
- Planned persistent queue, pause/resume, retry and Android Storage Access Framework destination selection
- Supported UI languages planned: Afghan Dari, Pashto, Arabic and English
- English digits in every supported language
- Default public destination planned: Download/TurboBH

## Current implementation
- Native Android project scaffold (Kotlin, Compose, Material 3)
- Multi-screen home, browser, library and settings UI
- Dark layered visual system and 3D-inspired hero panel
- Initial HTTP downloader engine and task-state model
- GitHub Actions workflow to compile a debug APK and upload it as an artifact

## Important status notes
The download engine is present as a separate module but is not yet connected to the home-screen action or a persistent background queue. The browser currently provides basic WebView navigation; browser-to-download capture, download prompts, tabs and safe browsing controls remain to be implemented. The library and settings screens are currently visual shells. No APK is claimed as built or device-tested until the CI build and Android device tests pass.

## Build requirements
- JDK 17
- Android SDK Platform 36 and Build Tools 36.0.0
- Gradle 8.13
- Android Gradle Plugin 8.13.2

From this directory:
\`\`\`sh
gradle --no-daemon :app:assembleDebug
\`\`\`

# BH Turbo Downloader

A clean-room Android downloader project. This implementation is being developed independently and does not reuse the previous app's source code.

## Product direction
- Android application, Kotlin + Jetpack Compose
- Premium dark interface
- Internal browser
- Resumable, segmented HTTP/HTTPS downloads when the server supports byte ranges
- Persistent download queue and user-selected destination through Android Storage Access Framework
- English digits in every supported UI language

## Initial project scaffold
This commit establishes the Android project structure and the first Compose screen. Download execution, browser integration, persistence, localization, and device testing are subsequent milestones.

## Build requirements
- JDK 17
- Android SDK Platform 36
- Android Gradle Plugin 8.13.2
- Gradle 8.13 or compatible wrapper

Open this directory in Android Studio to sync and build.

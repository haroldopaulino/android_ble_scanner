# BLE Insight

by Harold Paulino

BLE Insight is a modern Android Bluetooth Low Energy scanning app built with Kotlin and Jetpack Compose. It scans nearby BLE devices, displays discovered devices in a Material 3 interface, and shows useful details such as device name, MAC address, RSSI, bond state, and discoverability status.

## Features

- Bluetooth Low Energy device scanning
- Runtime permission handling for modern Android versions
- Material 3 Jetpack Compose interface
- Nearby device list with signal strength details
- Device metadata display
- Start and stop scanning controls
- Graceful scan error handling when Android denies privileged BLE scan access
- Separated data, domain, and presentation layers
- ViewModel-driven state management
- Current Android Gradle Plugin and SDK configuration

## Project Structure

```text
app/src/main/java/com/harold/ble_insight
├── data
│   ├── ble
│   └── repository
├── domain
│   ├── model
│   └── repository
├── presentation
│   └── scanner
├── ui
│   └── theme
├── BlePermissionProvider.kt
└── MainActivity.kt
```

## Build Requirements

- Android Studio Panda 4 or newer
- Android Gradle Plugin 9.2.0
- Gradle 9.4.1
- Kotlin Compose plugin 2.3.21
- Compose BOM 2026.04.01
- JDK 17 or newer
- Android SDK Platform 36
- Android SDK Build Tools 36.0.0
- Android device with Bluetooth Low Energy support
- Android 8.0 or newer

## Build Notes

This project intentionally keeps Android Gradle Plugin 9.2.0. If Android Studio reports that the latest supported AGP version is 9.1.0, Android Studio must be upgraded instead of downgrading this project.

This project uses AGP 9 built-in Kotlin support. The standalone `org.jetbrains.kotlin.android` plugin is not used.

## How to Run

1. Open the project in Android Studio Panda 4 or newer.
2. Sync Gradle.
3. Connect an Android device with Bluetooth enabled.
4. Run the app.
5. Grant Bluetooth permissions when prompted.
6. Tap the scan control to discover nearby BLE devices.

## Owner

Harold Paulino

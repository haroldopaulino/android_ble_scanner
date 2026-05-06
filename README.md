# BLE Insight

**Developer:** Harold Paulino

BLE Insight is a professional-grade Android application designed for high-performance Bluetooth Low Energy (BLE) discovery. Built with a focus on clean architecture and reactive programming, it provides a real-time, Material 3 interface for scanning and analyzing nearby BLE devices.

<img width="2437" height="1334" alt="geo_pulse_1" src="https://github.com/user-attachments/assets/7b28c07a-072f-4013-877f-25a927edb0ca" />
<img width="3023" height="1334" alt="geo_pulse_2" src="https://github.com/user-attachments/assets/bb1bcfc0-cde1-4f2d-abac-14f147454932" />


## 🚀 Key Features

*   **Real-time BLE Scanning:** Continuous discovery of Bluetooth Low Energy advertisements with live RSSI updates.
*   **Intelligent Device Management:** Automatic "upserting" and sorting of discovered devices based on signal strength (RSSI) to ensure the most relevant devices remain at the top.
*   **Modern UI Architecture:** 
    *   **Fixed Control Header:** Scan controls and real-time statistics (device count, strongest RSSI) remain anchored at the top for immediate access.
    *   **Independent List Scrolling:** Only the device list scrolls, providing a superior UX compared to standard full-page scrolling.
*   **Advanced Permission Handling:** Robust support for Android 12+ (API 31) granular Bluetooth permissions and legacy location requirements.
*   **Error Resiliency:** Specialized handling for Bluetooth availability, system-level scan failures, and security exceptions.

## 🛠 Technical Stack

*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose (Material 3)
*   **Architecture:** Clean Architecture + MVVM (Model-View-ViewModel)
*   **Reactive Streams:** Kotlin Coroutines & Flow (including `callbackFlow` for legacy API bridging)
*   **Build System:** Gradle (Kotlin DSL) with Android Gradle Plugin 9.2.0
*   **Testing:** 
    *   **JUnit 4** for unit testing.
    *   **Turbine** for reactive stream (Flow) verification.
    *   **Kotlinx-Coroutines-Test** for deterministic asynchronous testing.

## 🏗 Project Structure

The project follows Clean Architecture principles, ensuring a strict separation of concerns:

```text
app/src/main/java/com/harold/ble_insight
├── data            # Framework implementations & API bridging
│   ├── ble         # Android Bluetooth LE implementation (Scanner)
│   └── repository  # Repository implementations (Data mapping)
├── domain          # Pure business logic (Usecases, Models, Interfaces)
│   ├── model       # Domain entities
│   └── repository  # Repository interfaces (Abstraction)
├── presentation    # UI & State management
│   └── scanner     # Scanner Screen, ViewModel, and UI State
├── ui/theme        # Material 3 Design System implementation
├── BlePermissionProvider.kt  # Permission logic
└── MainActivity.kt           # Entry point & Activity-scoped logic
```

## 🧪 Quality Assurance

Extensive unit test coverage ensures the reliability of the core logic:

*   **ViewModel Tests:** Verifies state transitions, permission results, device sorting logic, and error state propagation.
*   **Repository Tests:** Ensures correct delegation between the domain interfaces and the hardware-specific scanner implementation.
*   **Flow Verification:** Uses Turbine to validate reactive device streams and lifecycle events.

## ⚙️ Build Requirements

*   **Android Studio Panda 4** or newer
*   **Android SDK Platform 36**
*   **Android Gradle Plugin 9.2.0**
*   **JDK 21**
*   **Minimum SDK:** API 26 (Android 8.0)

## 📖 How to Run

1.  Open the project in **Android Studio Panda 4** or newer.
2.  Sync Gradle dependencies.
3.  Connect an Android device with BLE support.
4.  Run the `:app` module.
5.  Grant requested Bluetooth and Location permissions to begin scanning.

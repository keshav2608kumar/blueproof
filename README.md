# BlueProof - BLE Attendance System

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-blue.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-SDK%2034-green.svg)](https://developer.android.com)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.109.0-teal.svg)](https://fastapi.tiangolo.com)

**BlueProof** is a high-assurance, tamper-resistant Bluetooth Low Energy (BLE) attendance verification system designed for university lecture halls, classrooms, and enterprise training environments. By combining rotating cryptographic challenges, hardware-bound HMAC authentication, and multi-sample RSSI proximity verification, BlueProof eliminates proxy check-ins ("buddy punching") without requiring QR code scanning, facial recognition, or intrusive location tracking.

---

## Table of Contents
- [Key Features](#key-features)
- [System Architecture](#system-architecture)
- [Prerequisites](#prerequisites)
- [Android App Build & Installation](#android-app-build--installation)
- [Backend Setup & Execution](#backend-setup--execution)
- [BLE Protocol Specification](#ble-protocol-specification)
- [Security Model & Defenses](#security-model--defenses)
- [User Guide](#user-guide)
- [Known Limitations](#known-limitations)
- [License](#license)

---

## Key Features

- **Cryptographic Challenge-Response**: Sessions generate ephemeral, 256-bit cryptographically secure challenges rotated every 30 seconds.
- **Physical Proximity Verification via RSSI**: Students must submit a burst of at least 5 RSSI samples. Median filtering ensures wall and distance boundaries cannot be breached.
- **Hardware-Bound Enrollment**: Students register their device ID alongside a 256-bit local enrollment key, preventing account-sharing across phones.
- **HMAC-SHA256 Integrity Verification**: Every check-in packet is digitally signed with `HMAC-SHA256(challenge:studentId:timestamp, enrollmentKey)`.
- **Offline-First Resilience**: Full local Room SQLite database support for both teachers and students. Check-ins are recorded immediately and synced to the cloud when connectivity resumes.
- **Real-Time Teacher Dashboard**: Dynamic live feed of incoming students, signal strengths, automatic acceptance/rejection categorization, and attendance rate metrics.
- **Comprehensive Audit Trail**: Tamper-evident logging of session start/pause/end events, challenge rotations, and check-in verifications.
- **REST API & CSV Export**: FastAPI backend providing automated reports, analytics, and CSV exports for integration with university Learning Management Systems (LMS).

---

## System Architecture

```
+-------------------------------------------------------------+
|                      Teacher Device                         |
|  - Jetpack Compose UI (Material 3)                          |
|  - BLE Advertiser & GATT Server                             |
|  - ChallengeGenerator (Rotating 256-bit tokens)             |
|  - SessionValidator (HMAC verify, RSSI median filter)       |
|  - Room DB (Sessions, Students, Attendance, Audit Logs)     |
+-------------------------------------------------------------+
                              ^
                              | BLE Advertising & GATT
                              v
+-------------------------------------------------------------+
|                      Student Device                         |
|  - Jetpack Compose UI (Scan & Check-In)                     |
|  - BLE Scanner & GATT Client                                |
|  - Local Secure Enrollment Key Storage                      |
|  - Multi-sample RSSI Collector (>= 5 samples)               |
|  - HMAC Signature Generator                                 |
+-------------------------------------------------------------+
                              |
                              | HTTP / REST (Sync & Reports)
                              v
+-------------------------------------------------------------+
|                    FastAPI Backend                          |
|  - REST Endpoints (/sessions, /students, /attendance)       |
|  - SQLAlchemy ORM & SQLite / PostgreSQL                     |
|  - CSV Exporter & Aggregate Analytics                       |
+-------------------------------------------------------------+
```

---

## Prerequisites

### Android Client
- **JDK 17** (Java Development Kit)
- **Android SDK Platform 34** (compileSdk 34, targetSdk 34, minSdk 26)
- **Android Device** running Android 8.0 (API 26) or higher with BLE Peripheral mode support (for Teacher mode) and BLE Central mode support (for Student mode).

### Backend Server
- **Python 3.10+**
- **pip** package manager

---

## Android App Build & Installation

### 1. Build Debug APK
From the root of the project:

```bash
# Windows
.\gradlew.bat assembleDebug

# Linux / macOS
./gradlew assembleDebug
```

The resulting APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 2. Run Unit Tests
To execute all local unit tests (cryptographic challenge generation, session validation, RSSI analyzer, attendance calculation):

```bash
# Windows
.\gradlew.bat test

# Linux / macOS
./gradlew test
```

### 3. Install on Physical Device
Connect your Android phone via USB with USB Debugging enabled:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> **Permissions Note**: On Android 12+ (API 31+), grant `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, and `BLUETOOTH_CONNECT`. On Android 8.0-11, grant `ACCESS_FINE_LOCATION` for BLE discovery.

---

## Backend Setup & Execution

The backend provides central student enrollment, cross-device session synchronization, analytics, and CSV data export.

### 1. Navigate and Create Virtual Environment
```bash
cd backend
python -m venv venv

# Windows
.\venv\Scripts\activate

# Linux / macOS
source venv/bin/activate
```

### 2. Install Dependencies
```bash
pip install -r requirements.txt
```

### 3. Configure Environment
```bash
# Copy example configuration
cp .env.example .env
```

### 4. Run Server
```bash
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

- API Base URL: `http://localhost:8000`
- Interactive Swagger UI: `http://localhost:8000/docs`
- Redoc Documentation: `http://localhost:8000/redoc`

---

## BLE Protocol Specification

BlueProof uses custom 128-bit UUIDs for Bluetooth GATT operations:

| Identifier | UUID | Description |
|---|---|---|
| **Service** | `0000b100-0000-1000-8000-00805f9b34fb` | BlueProof Attendance Service |
| **Session Info** | `0000b101-0000-1000-8000-00805f9b34fb` | Read/Notify: Active Session ID & Current Challenge |
| **Check-In** | `0000b102-0000-1000-8000-00805f9b34fb` | Write: Check-in packet (Student ID, timestamp, HMAC, RSSI) |
| **Response** | `0000b103-0000-1000-8000-00805f9b34fb` | Read/Notify: Acceptance status & verification token |

### Check-In Workflow
1. **Teacher Advertises**: Broadcasts `BLE_SERVICE_UUID` with manufacturer data containing the active session ID.
2. **Student Scans**: Discovers the service advertising packet and collects at least 5 consecutive RSSI readings.
3. **Challenge Acquisition**: Student connects to GATT and reads `SESSION_INFO_CHAR_UUID` to retrieve the current 64-character hex challenge.
4. **Signature Generation**: Student computes:
   $$\text{HMAC} = \text{HMAC-SHA256}(\text{challenge} \parallel ":" \parallel \text{studentId} \parallel ":" \parallel \text{timestamp}, \text{enrollmentKey})$$
5. **Submission**: Student writes payload to `CHECKIN_CHAR_UUID`.
6. **Validation**: Teacher verifies active status, challenge validity, HMAC signature, and RSSI median threshold. Returns response via `RESPONSE_CHAR_UUID`.

---

## Security Model & Defenses

| Attack Vector | Vulnerability in Standard Systems | BlueProof Defense Mechanism |
|---|---|---|
| **Replay Attacks** | Capturing a check-in code and submitting it later | Ephemeral 256-bit challenges rotate every 30s; expired challenges are rejected immediately. |
| **Proxy Attendance** | Sending credentials/QR screenshot to an absent peer | Devices sign with a local cryptographic key stored securely on enrolled hardware. |
| **Hallway / Remote Check-In** | Checking in from outside the lecture hall | Multi-sample RSSI median check enforces proximity (default -75 dBm threshold). |
| **Clock Skew Tampering** | Manipulating client system clock | Timestamps outside the allowed $\pm 60\text{s}$ tolerance window are rejected. |
| **Signal Spiking** | Fleeting reflections or anomalous RSSI bursts | Median of $\ge 5$ discrete measurements filters out transient RF spikes. |

---

## User Guide

### For Instructors (Teacher Mode)
1. **Launch App**: Select **Teacher Mode** on the home screen.
2. **Start Session**: Enter class details (e.g., "Computer Networks - Lecture 4"), set duration (e.g., 15 minutes), and configure the RSSI cutoff (default: `-75 dBm`).
3. **Broadcast**: Tap **Start Attendance**. Keep Bluetooth enabled. The device begins advertising and broadcasting rotating challenges.
4. **Live Monitoring**: View incoming student check-ins in real time with signal indicators:
   - 🟢 **Excellent** ($> -50\text{ dBm}$)
   - 🔵 **Good** ($> -65\text{ dBm}$)
   - 🟡 **Fair** ($> -75\text{ dBm}$)
   - 🔴 **Weak / Rejected** ($\le -75\text{ dBm}$)
5. **Finalize**: Tap **End Session**. View total attendance metrics and tap **Export CSV** to download the signed attendance sheet.

### For Students (Student Mode)
1. **Initial Enrollment**: On first launch, enter your Student ID and Full Name. The app generates your device enrollment key.
2. **Join Class**: When in the classroom, tap **Check In**.
3. **Automatic Detection**: The app scans for the teacher's broadcast, gathers signal samples, fetches the active challenge, signs the response, and submits.
4. **Confirmation**: A green checkmark displays confirmation code and recorded dBm signal strength.

---

## Known Limitations

- **RF Multipath Interference**: Large concrete pillars or dense metallic structures can attenuate BLE signals. Instructors can adjust the RSSI threshold from `-65 dBm` (small lab) to `-85 dBm` (large amphitheater) in Session Settings.
- **Manufacturer Battery Optimizations**: Certain Android OEMs aggressively kill background BLE GATT servers. Keeping the teacher screen active during an attendance window is recommended.
- **Hardware BLE Advertising Support**: While scanning is supported on all Android devices with BLE, GATT advertising requires hardware peripheral mode support (standard on modern smartphones since Android 8.0).

---

## License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

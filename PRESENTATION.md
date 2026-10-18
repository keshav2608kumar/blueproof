# BlueProof — Presentation Deck (PPT)
## Production-Quality BLE Attendance System with Anti-Proxy Safeguards

---

### SLIDE 1: Title Slide

# BlueProof
### Decentralized, Offline-First BLE Attendance System with Multi-Layer Anti-Proxy Safeguards

- **Domain**: IoT, Mobile Computing, Cyber-Physical Systems, EdTech
- **Platform**: Android Native (Kotlin + Jetpack Compose)
- **GitHub Repository**: [https://github.com/keshav2608kumar/BlueProof](https://github.com/keshav2608kumar/BlueProof)
- **Direct APK**: [Download BlueProof-debug.apk](https://github.com/keshav2608kumar/BlueProof/raw/main/dist/BlueProof-debug.apk)

> **Speaker Notes / Pitch Opening:**
> "Good morning, respected judges and professors. Today we present **BlueProof**, a production-grade, tamper-resistant classroom attendance platform engineered for Android. Unlike existing systems that rely on vulnerable QR codes, flaky classroom Wi-Fi, or expensive biometric queues, BlueProof creates a secure, offline, phone-to-phone Bluetooth radio bridge that marks attendance for an entire classroom in under 2 seconds while mathematically eliminating proxy attendance."

---

### SLIDE 2: Problem Statement & Existing Flaws

| Method | How It Works | Critical Vulnerabilities & Pain Points |
|---|---|---|
| **Paper Sign-in** | Physical roster passed around room | Proxy signatures ("buddy punching"), lost sheets, manual data entry |
| **QR Code Scanners** | Teacher displays QR code on screen | **Zero proxy resistance**: Students take a photo, send it over WhatsApp, and absent students check in from hostels |
| **Biometric Hardware** | Fingerprint scanner at classroom door | Bottlenecks & long queues, expensive specialized hardware, hygiene concerns, maintenance failures |
| **GPS Geofencing** | Mobile app checks GPS coordinates | Easily spoofed using mock location apps; indoor vertical accuracy (floors) is virtually zero |

> **Speaker Notes:**
> "Every university struggles with attendance fraud. QR codes are the worst offenders: one student snaps the QR code and shares it on WhatsApp, allowing dozens of absent students to check in from their dorm beds. Biometric machines cost thousands of dollars and cause 15-minute lines at the door. We set out to design a system that requires **zero external hardware**, operates **without internet**, and makes proxy attendance mathematically and physically impossible."

---

### SLIDE 3: The BlueProof Solution

### Core Innovation: Direct Radio-Frequency Cryptographic Handshake

1. **100% Offline-First Architecture**:
   - Communication happens directly phone-to-phone via Bluetooth Low Energy (BLE).
   - Zero dependency on classroom Wi-Fi, cellular internet, or cloud servers.
2. **Sub-2-Second Verification**:
   - Zero Bluetooth pairing required; connections use transient GATT read/write operations.
3. **Multi-Layer Anti-Proxy Safeguards**:
   - Prevents phone-sharing, stolen student IDs, outside-the-door punching, and replay attacks.
4. **Zero Extra Hardware**:
   - Works on standard Android smartphones using existing onboard Bluetooth chips.

> **Speaker Notes:**
> "BlueProof transforms the teacher's smartphone into a secure Bluetooth Low Energy beacon and GATT server. Student phones scan for the broadcast, measure physical proximity through calibrated signal strength, perform a cryptographic challenge-response handshake, and log attendance into an onboard Room SQLite database. It works in basements, rural schools, or anywhere with zero connectivity."

---

### SLIDE 4: High-Level Architecture & Data Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             TEACHER PHONE (Host)                            │
│                                                                             │
│  [BleAdvertiser] ──(Broadcasts Session ID over BLE Advertisements)──┐       │
│                                                                     │       │
│  [GattServer] <───(Transient Direct Radio Link - Zero Pairing)──────┼───┐   │
│        │                                                            │   │   │
│  [SessionManager]                                                   │   │   │
│    ├── 30-Second Rotating Cryptographic Challenge                   │   │   │
│    ├── Hardware Device Exclusivity Engine (1 Phone = 1 Student)     │   │   │
│    ├── HMAC-SHA256 Cryptographic Signature Verification            │   │   │
│    └── Multi-Sample Median RSSI Proximity Filter                    │   │   │
│        │                                                            │   │   │
│  [Room SQLite DB] (Persists Sessions, Records, and Audit Logs)      │   │   │
└─────────────────────────────────────────────────────────────────────┼───┼───┘
                                                                      │   │
                     Bluetooth Low Energy (2.4 GHz RF)                │   │
                     NO INTERNET / NO WI-FI REQUIRED                  │   │
                                                                      │   │
┌─────────────────────────────────────────────────────────────────────┼───┼───┐
│                             STUDENT PHONE 1                         │   │   │
│  [BleScanner]  <────────────────────────────────────────────────────┘   │   │
│  [GattClient]  ──(Submits HMAC Signature + Hardware Device ID)──────────┘   │
└─────────────────────────────────────────────────────────────────────────────┼───┐
                                                                              │   │
┌─────────────────────────────────────────────────────────────────────────────┼───┼───┐
│                             STUDENT PHONE 2, 3 ... N                        │   │   │
│  [BleScanner]  <────────────────────────────────────────────────────────────┘   │   │
│  [GattClient]  ──(Submits Concurrent Check-ins Protected by Mutex)──────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

> **Speaker Notes:**
> "Here is our architectural topology. Notice that the entire loop closes strictly between the devices. The teacher's phone runs an active GATT server and advertising beacon. The student phones discover the beacon, accumulate signal readings, connect, complete the handshake, and disconnect within 2 seconds. The teacher's SessionManager uses a thread-safe mutex so 50+ students can connect concurrently without dropping packets."

---

### SLIDE 5: The 5-Pillar Anti-Proxy Security Matrix

```
       ┌─────────────────────────────────────────────────────────────────┐
       │             BLUEPROOF 5-PILLAR SECURITY MATRIX                  │
       └─────────────────────────────────────────────────────────────────┘
                                        │
    ┌───────────────────┬───────────────┴───────────────┬───────────────────┐
    ▼                   ▼                               ▼                   ▼
[Pillar 1]          [Pillar 2]                      [Pillar 3]          [Pillar 4]
Hardware Device     Enrolled Device                 Multi-Sample RSSI   30s Rotating
Exclusivity         Lock                            Proximity Filter    Challenge
(Blocks Buddy       (Blocks Stolen                  (Blocks Outside     (Blocks Replay
Punching)            ID Spoofing)                    Punching)           Attacks)
```

1. **Hardware Device Exclusivity**:
   - Each phone has an immutable hardware identifier (`Settings.Secure.ANDROID_ID`).
   - One physical device can only submit attendance for **one student per session**.
   - If Student A switches student IDs on their phone to punch for Student B $\rightarrow$ **INSTANT REJECTION**.
2. **Enrolled Device Lock**:
   - A student ID is cryptographically bound to the student's personal phone on first registration.
   - Friends cannot take an absent student's ID and enter it into their own phones.
3. **Multi-Sample RSSI Proximity Filtering**:
   - Instead of trusting single noisy Bluetooth readings, the scanner collects **5 to 20 samples** and calculates the **statistical median**.
   - Rejects signals weaker than the teacher's configurable boundary threshold (e.g. $< -75\text{ dBm}$).
4. **Rotating 32-Byte Cryptographic Challenge**:
   - Generated using `java.security.SecureRandom`.
   - Rotates automatically every **30 seconds**. Captured Bluetooth packets expire in $< 30\text{s}$.
5. **HMAC-SHA256 Authentication**:
   - Secret enrollment keys are **never transmitted over the air**.
   - Student proves identity by signing: `HMAC(challenge + studentId + timestamp)`.

> **Speaker Notes:**
> "This slide outlines our core technical differentiator. Other apps trust what the phone sends. We do not. First: Hardware Device Exclusivity ensures that even if someone logs out and logs in as their friend, the physical phone ID has already been marked as used in that session, throwing an instant red alert. Second: Proximity filtering uses a mathematical median of RSSI samples to ensure the student is physically within classroom walls. Third: Challenge-response HMAC crypto ensures tokens cannot be captured, replayed, or spoofed."

---

### SLIDE 6: Edge Case Resolution: Lost / Replaced Phones

### The Dilemma:
*If a student loses their phone or buys a new device, how do they check in without compromising the Anti-Proxy device lock?*

### The BlueProof Solution: Teacher-Controlled Device Lock Reset

```
[Student buys new phone] ──> Installs BlueProof & Registers ID
                                       │
                              Attempts Check-in
                                       │
                                       ▼
                       Blocked: "Device Mismatch Alert"
                                       │
                                       ▼
         Teacher opens Dashboard ──> Taps "Enrolled (X)" Card
                                       │
                                       ▼
         Finds Student Name ───────> Taps [Reset Lock] Button
                                       │
                                       ▼
         Student retries ──────────> ACCEPTED!
                                     New phone's hardware ID securely re-bound
```

- **Zero Admin Overhead**: Solved right inside the classroom in 2 taps.
- **Audit-Logged**: Every reset event is logged with old device ID, new device ID, and timestamp.

> **Speaker Notes:**
> "In real-world deployment, students lose phones or upgrade devices. In insecure systems, anyone can register on any phone at any time. In BlueProof, the teacher dashboard features a dedicated 'Enrolled Students & Device Lock' manager. If a student brings a new phone, the teacher taps 'Reset Lock'. The system clears the old device lock, re-binds the new phone's hardware ID upon next check-in, and logs the transfer into the audit trail."

---

### SLIDE 7: Technology Stack & Technical Dependencies

### Android Client Application
- **Language**: Kotlin 1.9.22 (100% Native)
- **UI Toolkit**: Jetpack Compose + Material 3 (BOM `2024.02.02`)
- **Architecture**: MVVM with Kotlin Coroutines & `StateFlow`
- **Local Persistence**: Jetpack Room SQLite Database `2.6.1` with KSP
- **Bluetooth Stack**: Android BLE APIs (`BluetoothLeAdvertiser`, `BluetoothLeScanner`, `BluetoothGattServer`)
- **Serialization**: Google Gson `2.10.1`
- **Security**: `java.security.SecureRandom`, `javax.crypto.Mac` (HmacSHA256)
- **Minimum OS**: Android 8.0 Oreo (API 26) | **Target OS**: Android 14 (API 34)

### Optional Cloud Aggregation Backend
- **Framework**: FastAPI (Python 3.11+, ASGI)
- **Server**: Uvicorn
- **ORM / Database**: SQLAlchemy 2.0 with SQLite / PostgreSQL support
- **Data Validation**: Pydantic v2

> **Speaker Notes:**
> "We selected modern, battle-tested technologies. We chose Jetpack Compose with Material 3 for a smooth 60fps declarative UI without legacy XML bloat. Room Database provides compile-time verified SQL queries and reactive Flow streams. For cryptography, we avoided third-party bloat and utilized hardware-backed Java Cryptography extensions."

---

### SLIDE 8: End-to-End Cryptographic Handshake Protocol

```
TEACHER PHONE                                                    STUDENT PHONE
─────────────                                                    ─────────────
      │                                                                │
      │ 1. Advertises BLE Packet: [Service UUID + Session Hash]       │
      │ ─────────────────────────────────────────────────────────────> │
      │                                                                │
      │                                    2. BleScanner buffers 5+    │
      │                                       RSSI samples; calculates │
      │                                       Median RSSI (-65 dBm)    │
      │                                                                │
      │ 3. Transient GATT Connection (Zero Pairing)                   │
      │ <───────────────────────────────────────────────────────────── │
      │                                                                │
      │ 4. Read SESSION_INFO characteristic                           │
      │ <───────────────────────────────────────────────────────────── │
      │ 5. Returns: {sessionId, challenge, expiresAt, teacherProfile}  │
      │ ─────────────────────────────────────────────────────────────> │
      │                                                                │
      │                                    6. Computes HMAC-SHA256:    │
      │                                       Msg = challenge:id:time  │
      │                                       Sig = HMAC(Msg, key)     │
      │                                                                │
      │ 7. Write CHECKIN char: {id, deviceId, hmac, timestamp, rssi}  │
      │ <───────────────────────────────────────────────────────────── │
      │                                                                │
      │ 8. SessionManager verifies:                                   │
      │    - Not duplicate in session?                                 │
      │    - Device ID not used by another student?                    │
      │    - Device ID matches enrolled device?                        │
      │    - Challenge unexpired (< 30s)?                              │
      │    - HMAC signature valid?                                     │
      │    - Median RSSI >= threshold?                                 │
      │                                                                │
      │ 9. Writes AttendanceRecordEntity into Room DB                 │
      │ 10. Emits RESPONSE notification: "ACCEPTED"                   │
      │ ─────────────────────────────────────────────────────────────> │
      │                                                                │
      │ 11. Closes GATT Connection                                     │
      │ < - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - > │
```

> **Speaker Notes:**
> "Here is the exact packet sequence over Bluetooth. Notice steps 4 through 7: the student's secret key is never sent across the airwaves. Only the HMAC signature is transmitted. The teacher's phone evaluates all anti-proxy rules in step 8 before writing to SQLite and emitting the confirmation notification. Total elapsed time: between 1.2 to 1.8 seconds per student."

---

### SLIDE 9: Database Schema & Entity Relationships

```
┌─────────────────────────────────┐       ┌─────────────────────────────────┐
│          STUDENTS               │       │            SESSIONS             │
├─────────────────────────────────┤       ├─────────────────────────────────┤
│ id: Long (PK)                   │       │ id: Long (PK)                   │
│ studentId: String (UNIQUE)      │       │ sessionId: String (UNIQUE)      │
│ name: String                    │       │ name: String                    │
│ deviceId: String (INDEX)        │       │ teacherName: String             │
│ enrollmentKey: String (256-bit) │       │ teacherId: String               │
│ enrolledAt: Long                │       │ startTime: Long, endTime: Long  │
│ isActive: Boolean               │       │ rssiThreshold: Int (-75 dBm)    │
└─────────────────────────────────┘       │ status: ACTIVE | PAUSED | ENDED │
                 │                        └─────────────────────────────────┘
                 │                                         │
                 │         ┌───────────────────────────────┘
                 ▼         ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                           ATTENDANCE_RECORDS                              │
├───────────────────────────────────────────────────────────────────────────┤
│ id: Long (PK)                                                             │
│ studentId: String (INDEX)                                                 │
│ sessionId: String (INDEX)                                                 │
│ studentName: String                                                       │
│ checkInTime: Long                                                         │
│ rssiMedian: Int                                                           │
│ rssiSamples: String (JSON Array)                                          │
│ deviceId: String (INDEX)  <── Enforces Physical Phone Exclusivity        │
│ status: ACCEPTED | REJECTED | PENDING_REVIEW                              │
│ rejectionReason: String?                                                  │
│ challengeUsed: String                                                     │
│ syncedToBackend: Boolean                                                  │
├───────────────────────────────────────────────────────────────────────────┤
│ UNIQUE INDEX: (studentId, sessionId)                                      │
│ COMPOSITE INDEX: (sessionId, deviceId)                                    │
└───────────────────────────────────────────────────────────────────────────┘
```

> **Speaker Notes:**
> "Our database schema is optimized for speed and relational integrity. Notice the unique composite index on studentId plus sessionId in attendance_records. This makes double check-ins physically impossible at the SQLite engine level. Additionally, our composite index on sessionId plus deviceId allows sub-millisecond lookups when checking whether a physical phone has already been used in an active session."

---

### SLIDE 10: Complete UI/UX Suite — 11 Production Screens

| Screen Name | User Role | Core Features & Actions |
|---|:---:|---|
| **WelcomeScreen** | Both | Clean role selection card ("Teacher" vs. "Student") |
| **StudentRegistrationScreen** | Student | Student ID & Name enrollment with auto-login persistence |
| **TeacherDashboardScreen** | Teacher | **Faculty Profile Card**, **Enrolled Students Manager**, Quick Start |
| **StartSessionScreen** | Teacher | Pre-filled Faculty info, duration picker, RSSI slider |
| **BleScanScreen** | Student | Live scanning radar, signal strength bars, one-tap Check-In |
| **LiveSessionScreen** | Teacher | Connected device counter, countdown timer, live attendance feed |
| **StudentDashboardScreen** | Student | Personal greeting, today's attendance status, history launcher |
| **AttendanceHistoryScreen** | Both | Chronological session archives and past check-in records |
| **AnalyticsScreen** | Both | Custom bar charts, donut charts, and attendance rates |
| **SessionDetailsScreen** | Teacher | Granular audit trail, status filters, **Export CSV button** |
| **SettingsScreen** | Both | BLE hardware capabilities check, device ID viewer, diagnostics |

> **Speaker Notes:**
> "BlueProof features 11 complete, polished Material 3 screens. We strictly adhered to a professional Deep Navy and Restrained Teal palette suitable for university deployments. Every screen handles empty states, loading states, error dialogs, and permission flows gracefully."

---

### SLIDE 11: Analytics, Visualizations & Data Export

### 1. Zero-Dependency Canvas Charts (`SimpleChart.kt`)
- **Bar Chart**: Visualizes Present vs. Absent counts side-by-side.
- **Donut Chart**: Breaks down attendance distribution (Accepted, Rejected, Pending).
- **Line Chart**: Tracks attendance trends over the last 10 lecture sessions.
- *Engineered directly on Compose Canvas without adding heavy third-party chart dependencies.*

### 2. Instant CSV Export (`CsvExporter.kt`)
- Generates standard RFC 4180 CSV files directly on the teacher's phone.
- Fields exported: `Student ID`, `Student Name`, `Check-in Timestamp`, `Median RSSI`, `Status`, `Rejection Reason`, `Device ID`.
- Saves to Android device storage for direct opening in Excel or Google Sheets.

> **Speaker Notes:**
> "To keep the binary lightweight (under 17 MB), we engineered our own custom charting engine using Jetpack Compose Canvas. Teachers can review session analytics immediately, tap 'Export CSV', and email the spreadsheet to administrative portals without needing any backend connection."

---

### SLIDE 12: Rigorous Verification: 55 Automated Tests Passing

```
========================================================================================
Test Suite                                  Scope Verified                     Result
========================================================================================
com.blueproof.session.AntiProxyEndToEndTest  Buddy punching, phone reuse,       11/11 PASSED
                                            device lock reset, stolen ID
com.blueproof.session.SessionValidatorTest   Token expiry, weak RSSI, HMAC      11/11 PASSED
com.blueproof.session.ChallengeGeneratorTest 32-byte entropy, HMAC-SHA256       10/10 PASSED
com.blueproof.util.RssiAnalyzerTest         Median math, boundary threshold     7/7 PASSED
com.blueproof.util.AttendanceCalculatorTest Percentage math, zero-student edge  6/6 PASSED
backend/test_api.py (FastAPI Integration)   Cloud REST endpoints, anti-proxy   10/10 PASSED
========================================================================================
TOTAL AUTOMATED TEST VERIFICATION                                              55/55 PASSED (100%)
========================================================================================
```

### Key Attack Scenarios Verified:
-  **Phone-Switch Attack**: Alice checks in $\rightarrow$ logs out $\rightarrow$ enters Bob's ID on same phone $\rightarrow$ **BLOCKED**.
-  **Stolen ID Attack**: Charlie enters Bob's ID on Charlie's phone $\rightarrow$ **BLOCKED (Device Mismatch)**.
-  **Corridor Attack**: Student connects from outside classroom ($-88\text{ dBm}$) $\rightarrow$ **BLOCKED (Weak RSSI)**.
-  **Replay Attack**: Attacker captures BLE packet and replays after 35 seconds $\rightarrow$ **BLOCKED (Expired Challenge)**.
-  **Phone Replacement**: Student gets new phone $\rightarrow$ Teacher resets lock $\rightarrow$ **ACCEPTED & RE-BOUND**.

> **Speaker Notes:**
> "We did not just write code; we proved its reliability through 55 comprehensive automated tests. Every single attack vector—same-phone buddy punching, stolen credentials, corridor spoofing, and replay attacks—was simulated and verified to fail, while valid students check in seamlessly."

---

### SLIDE 13: Feature & Security Comparison Matrix

| Feature / Attack Vector | Paper Roster | QR Code App | Biometric Terminal | GPS App | **BlueProof** |
|---|:---:|:---:|:---:|:---:|:---:|
| **Infrastructure Cost** | \$0 | \$0 | \$500 - \$2,000 | \$0 | **\$0 (Uses existing phones)** |
| **Offline Operation** | Yes | No | Partial | No | **100% Offline (Direct BLE)** |
| **Check-in Speed** | 10-15 mins | 2-3 mins | 10-15 mins | 2-5 mins | **< 2 seconds** |
| **Photo-Share Proxy Resistance** | N/A | **FAILS** | Protected | Protected | **IMMUNE (Direct BLE Link)** |
| **Buddy-Punching Resistance** | **FAILS** | **FAILS** | Protected | **FAILS** | **PROTECTED (Device Exclusivity)** |
| **Corridor / Outside Punching** | **FAILS** | **FAILS** | Protected | **FAILS** | **PROTECTED (RSSI Filtering)** |
| **Replay Attack Resistance** | N/A | **FAILS** | Protected | **FAILS** | **PROTECTED (30s Challenge)** |
| **Lost Phone Recovery** | N/A | Re-login | Admin reset | Re-login | **2-Tap Teacher Reset** |

> **Speaker Notes:**
> "Here is our competitive landscape matrix. Compared to QR codes, BlueProof completely eliminates photo-sharing proxies. Compared to biometric machines, BlueProof requires zero capital expenditure and zero maintenance. It combines the zero-cost advantage of mobile software with the security guarantees of biometric hardware."

---

### SLIDE 14: Future Roadmap & Extensibility

```
┌─────────────────────────┐    ┌─────────────────────────┐    ┌─────────────────────────┐
│         PHASE 1         │    │         PHASE 2         │    │         PHASE 3         │
│     Current Release     │    │     Upcoming Update     │    │     Enterprise Scale    │
├─────────────────────────┤    ├─────────────────────────┤    ├─────────────────────────┤
│ • Direct BLE Handshake  │    │ • BLE Mesh Relaying     │    │ • On-device Face Auth   │
│ • Hardware Device Lock  │    │   (Expands to 500-seat  │    │   biometric pairing     │
│ • Local Room SQLite DB  │    │   auditoriums)          │    │ • University ERP        │
│ • CSV Export Engine     │    │ • Background attendance │    │   adapters (Canvas,     │
│ • 55 Passing Unit Tests │    │   beacon mode           │    │   Blackboard, Moodle)   │
└─────────────────────────┘    └─────────────────────────┘    └─────────────────────────┘
```

> **Speaker Notes:**
> "Looking ahead, BlueProof's architecture easily extends into BLE Mesh networking for massive 500-seat lecture halls, optional on-device biometric confirmation using Android BiometricPrompt, and automated plugins for enterprise university learning management systems like Moodle and Canvas."

---

### SLIDE 15: Conclusion & Demonstration Links

### Key Takeaways:
1. **Zero Hardware Cost**: Runs on standard smartphones.
2. **100% Offline Resilience**: Operates without Wi-Fi, internet, or cloud servers.
3. **Hardened Anti-Proxy Design**: First BLE attendance platform enforcing physical hardware device exclusivity.
4. **Production-Ready**: 55 automated tests, 11 Material 3 screens, and compiled APK available right now.

---

### Project Links & Downloads:
- **Full Source Code**: [https://github.com/keshav2608kumar/BlueProof](https://github.com/keshav2608kumar/BlueProof)
- **Installable APK**: [Download BlueProof-debug.apk (16.36 MB)](https://github.com/keshav2608kumar/BlueProof/raw/main/dist/BlueProof-debug.apk)
- **Direct Raw Download URL**:
  ```text
  https://github.com/keshav2608kumar/BlueProof/raw/main/dist/BlueProof-debug.apk
  ```

---

# Thank You!
### Questions & Answers

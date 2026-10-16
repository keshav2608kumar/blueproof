package com.blueproof.util

import java.util.UUID

object Constants {
    // BLE Service and Characteristic UUIDs
    val BLE_SERVICE_UUID: UUID = UUID.fromString("0000b100-0000-1000-8000-00805f9b34fb")
    val SESSION_INFO_CHAR_UUID: UUID = UUID.fromString("0000b101-0000-1000-8000-00805f9b34fb")
    val CHECKIN_CHAR_UUID: UUID = UUID.fromString("0000b102-0000-1000-8000-00805f9b34fb")
    val RESPONSE_CHAR_UUID: UUID = UUID.fromString("0000b103-0000-1000-8000-00805f9b34fb")
    val DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // BLE RSSI and Challenge thresholds
    const val DEFAULT_RSSI_THRESHOLD: Int = -75
    const val MIN_RSSI_SAMPLES: Int = 5
    const val CHALLENGE_VALIDITY_MS: Long = 30_000L
    const val MAX_CHECKIN_ATTEMPTS: Int = 3
    const val SESSION_DEFAULT_DURATION_MIN: Int = 15

    // Manufacturer ID for custom advertising data
    const val BLE_MANUFACTURER_ID: Int = 0x02FF

    // Attendance Statuses
    const val STATUS_ACCEPTED: String = "ACCEPTED"
    const val STATUS_REJECTED: String = "REJECTED"
    const val STATUS_PENDING_REVIEW: String = "PENDING_REVIEW"

    // Session Statuses
    const val SESSION_STATUS_ACTIVE: String = "ACTIVE"
    const val SESSION_STATUS_PAUSED: String = "PAUSED"
    const val SESSION_STATUS_ENDED: String = "ENDED"

    // Audit Log Event Types
    const val AUDIT_SESSION_STARTED: String = "SESSION_STARTED"
    const val AUDIT_SESSION_PAUSED: String = "SESSION_PAUSED"
    const val AUDIT_SESSION_RESUMED: String = "SESSION_RESUMED"
    const val AUDIT_SESSION_ENDED: String = "SESSION_ENDED"
    const val AUDIT_CHALLENGE_ROTATED: String = "CHALLENGE_ROTATED"
    const val AUDIT_CHECKIN_ACCEPTED: String = "CHECKIN_ACCEPTED"
    const val AUDIT_CHECKIN_REJECTED: String = "CHECKIN_REJECTED"
    const val AUDIT_STUDENT_ENROLLED: String = "STUDENT_ENROLLED"
}

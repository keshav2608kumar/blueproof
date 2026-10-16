package com.blueproof.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance_records",
    indices = [
        Index(value = ["studentId", "sessionId"], unique = true),
        Index(value = ["sessionId", "deviceId"]),
        Index(value = ["sessionId"]),
        Index(value = ["studentId"])
    ]
)
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: String,
    val sessionId: String,
    val studentName: String,
    val checkInTime: Long = System.currentTimeMillis(),
    val rssiMedian: Int,
    val rssiSamples: String,
    val status: String,
    val rejectionReason: String? = null,
    val challengeUsed: String,
    val syncedToBackend: Boolean = false,
    val deviceId: String = ""
)

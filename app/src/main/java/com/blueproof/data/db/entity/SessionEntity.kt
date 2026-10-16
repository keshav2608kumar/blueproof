package com.blueproof.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    indices = [
        Index(value = ["sessionId"], unique = true)
    ]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val name: String,
    val teacherId: String = "TCH-001",
    val teacherName: String,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val durationMinutes: Int = 15,
    val status: String = "ACTIVE",
    val rssiThreshold: Int = -75,
    val currentChallenge: String? = null,
    val challengeExpiresAt: Long? = null
)

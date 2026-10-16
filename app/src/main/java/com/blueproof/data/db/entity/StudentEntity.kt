package com.blueproof.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "students",
    indices = [
        Index(value = ["studentId"], unique = true),
        Index(value = ["deviceId"])
    ]
)
data class StudentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: String,
    val name: String,
    val deviceId: String,
    val enrollmentKey: String,
    val enrolledAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

package com.blueproof.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.blueproof.data.db.entity.AttendanceRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: AttendanceRecordEntity): Long

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId ORDER BY checkInTime DESC")
    suspend fun getRecordsForSession(sessionId: String): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId ORDER BY checkInTime DESC")
    fun getRecordsForSessionFlow(sessionId: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId ORDER BY checkInTime DESC")
    suspend fun getRecordsForStudent(studentId: String): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId AND sessionId = :sessionId LIMIT 1")
    suspend fun findByStudentAndSession(studentId: String, sessionId: String): AttendanceRecordEntity?

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId AND sessionId = :sessionId AND status = 'ACCEPTED' LIMIT 1")
    suspend fun findAcceptedByStudentAndSession(studentId: String, sessionId: String): AttendanceRecordEntity?

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId AND deviceId = :deviceId AND status = 'ACCEPTED' LIMIT 1")
    suspend fun findAcceptedBySessionAndDevice(sessionId: String, deviceId: String): AttendanceRecordEntity?

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId AND deviceId = :deviceId LIMIT 1")
    suspend fun findBySessionAndDevice(sessionId: String, deviceId: String): AttendanceRecordEntity?

    @Query("SELECT COUNT(*) FROM attendance_records WHERE sessionId = :sessionId AND status = :status")
    suspend fun countBySessionIdAndStatus(sessionId: String, status: String): Int

    @Query("SELECT COUNT(*) FROM attendance_records WHERE studentId = :studentId AND status = 'ACCEPTED'")
    suspend fun countAcceptedForStudent(studentId: String): Int

    @Query("SELECT * FROM attendance_records WHERE syncedToBackend = 0 ORDER BY checkInTime ASC")
    suspend fun getUnsyncedRecords(): List<AttendanceRecordEntity>

    @Update
    suspend fun update(record: AttendanceRecordEntity)

    @Query("SELECT COUNT(*) FROM attendance_records WHERE sessionId = :sessionId")
    suspend fun countRecordsForSession(sessionId: String): Int
}

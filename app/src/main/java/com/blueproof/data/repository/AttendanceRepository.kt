package com.blueproof.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.blueproof.data.db.BlueProofDatabase
import com.blueproof.data.db.dao.AttendanceDao
import com.blueproof.data.db.dao.AuditLogDao
import com.blueproof.data.db.dao.SessionDao
import com.blueproof.data.db.dao.StudentDao
import com.blueproof.data.db.entity.AttendanceRecordEntity
import com.blueproof.data.db.entity.AuditLogEntity
import com.blueproof.data.db.entity.SessionEntity
import com.blueproof.data.db.entity.StudentEntity
import com.blueproof.util.Constants
import kotlinx.coroutines.flow.Flow
import java.security.SecureRandom
import java.util.UUID

data class SessionStats(
    val total: Int,
    val accepted: Int,
    val rejected: Int,
    val pending: Int
)

open class AttendanceRepository(
    private val studentDao: StudentDao,
    private val sessionDao: SessionDao,
    private val attendanceDao: AttendanceDao,
    private val auditLogDao: AuditLogDao
) {
    constructor(database: BlueProofDatabase) : this(
        database.studentDao(),
        database.sessionDao(),
        database.attendanceDao(),
        database.auditLogDao()
    )

    suspend fun registerStudent(
        studentId: String,
        name: String,
        deviceId: String
    ): StudentEntity {
        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        val enrollmentKey = randomBytes.joinToString("") { "%02x".format(it) }

        val student = StudentEntity(
            studentId = studentId,
            name = name,
            deviceId = deviceId,
            enrollmentKey = enrollmentKey,
            enrolledAt = System.currentTimeMillis(),
            isActive = true
        )
        studentDao.insert(student)
        addAuditLog(
            eventType = Constants.AUDIT_STUDENT_ENROLLED,
            studentId = studentId,
            details = "Student $name ($studentId) registered on device $deviceId"
        )
        return student
    }

    suspend fun registerStudentWithKey(
        studentId: String,
        name: String,
        deviceId: String,
        enrollmentKey: String
    ): StudentEntity {
        val student = StudentEntity(
            studentId = studentId,
            name = name,
            deviceId = deviceId,
            enrollmentKey = enrollmentKey,
            enrolledAt = System.currentTimeMillis(),
            isActive = true
        )
        studentDao.insert(student)
        return student
    }

    suspend fun getUnsyncedRecords(): List<AttendanceRecordEntity> {
        return attendanceDao.getUnsyncedRecords()
    }

    suspend fun markRecordSynced(record: AttendanceRecordEntity) {
        attendanceDao.update(record.copy(syncedToBackend = true))
    }

    suspend fun getStudent(studentId: String): StudentEntity? {
        return studentDao.findByStudentId(studentId)
    }

    suspend fun getStudentByDevice(deviceId: String): StudentEntity? {
        return studentDao.findByDeviceId(deviceId)
    }

    suspend fun getAllStudents(): List<StudentEntity> {
        return studentDao.getAllStudents()
    }

    suspend fun deleteStudent(studentId: String): Boolean {
        val deleted = studentDao.deleteByStudentId(studentId) > 0
        if (deleted) {
            addAuditLog(
                eventType = Constants.AUDIT_STUDENT_ENROLLED,
                studentId = studentId,
                details = "Student enrollment deleted for $studentId"
            )
        }
        return deleted
    }

    suspend fun resetStudentDevice(studentId: String): Boolean {
        val student = studentDao.findByStudentId(studentId) ?: return false
        studentDao.update(student.copy(deviceId = ""))
        addAuditLog(
            eventType = Constants.AUDIT_STUDENT_ENROLLED,
            studentId = studentId,
            details = "Device lock reset for student ${student.name} ($studentId)"
        )
        return true
    }

    suspend fun createSession(
        name: String,
        teacherName: String,
        teacherId: String = "TCH-001",
        durationMinutes: Int = Constants.SESSION_DEFAULT_DURATION_MIN,
        rssiThreshold: Int = Constants.DEFAULT_RSSI_THRESHOLD
    ): SessionEntity {
        val sessionId = UUID.randomUUID().toString()
        val session = SessionEntity(
            sessionId = sessionId,
            name = name,
            teacherId = teacherId,
            teacherName = teacherName,
            startTime = System.currentTimeMillis(),
            durationMinutes = durationMinutes,
            status = Constants.SESSION_STATUS_ACTIVE,
            rssiThreshold = rssiThreshold
        )
        sessionDao.insert(session)
        addAuditLog(
            eventType = Constants.AUDIT_SESSION_STARTED,
            sessionId = sessionId,
            details = "Session '$name' started by teacher $teacherName ($teacherId)"
        )
        return session
    }

    suspend fun getSession(sessionId: String): SessionEntity? {
        return sessionDao.findBySessionId(sessionId)
    }

    suspend fun getActiveSession(): SessionEntity? {
        return sessionDao.getActiveSession()
    }

    suspend fun updateSession(session: SessionEntity) {
        sessionDao.update(session)
    }

    suspend fun endSession(sessionId: String) {
        val session = sessionDao.findBySessionId(sessionId)
        if (session != null) {
            val updated = session.copy(
                status = Constants.SESSION_STATUS_ENDED,
                endTime = System.currentTimeMillis()
            )
            sessionDao.update(updated)
            addAuditLog(
                eventType = Constants.AUDIT_SESSION_ENDED,
                sessionId = sessionId,
                details = "Session '${session.name}' ended"
            )
        }
    }

    suspend fun recordAttendance(record: AttendanceRecordEntity): Result<Long> {
        return try {
            val id = attendanceDao.insert(record)
            val eventType = if (record.status == Constants.STATUS_ACCEPTED) {
                Constants.AUDIT_CHECKIN_ACCEPTED
            } else {
                Constants.AUDIT_CHECKIN_REJECTED
            }
            addAuditLog(
                eventType = eventType,
                sessionId = record.sessionId,
                studentId = record.studentId,
                details = "Check-in ${record.status}: RSSI median=${record.rssiMedian} dBm, reason=${record.rejectionReason ?: "None"}"
            )
            Result.success(id)
        } catch (e: SQLiteConstraintException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAttendanceForSession(sessionId: String): List<AttendanceRecordEntity> {
        return attendanceDao.getRecordsForSession(sessionId)
    }

    suspend fun getAttendanceForStudent(studentId: String): List<AttendanceRecordEntity> {
        return attendanceDao.getRecordsForStudent(studentId)
    }

    suspend fun isDuplicate(studentId: String, sessionId: String): Boolean {
        return attendanceDao.findAcceptedByStudentAndSession(studentId, sessionId) != null
    }

    suspend fun getAttendanceForDeviceInSession(deviceId: String, sessionId: String): AttendanceRecordEntity? {
        if (deviceId.isBlank()) return null
        return attendanceDao.findAcceptedBySessionAndDevice(sessionId, deviceId)
    }

    suspend fun getSessionStats(sessionId: String): SessionStats {
        val total = attendanceDao.countRecordsForSession(sessionId)
        val accepted = attendanceDao.countBySessionIdAndStatus(sessionId, Constants.STATUS_ACCEPTED)
        val rejected = attendanceDao.countBySessionIdAndStatus(sessionId, Constants.STATUS_REJECTED)
        val pending = attendanceDao.countBySessionIdAndStatus(sessionId, Constants.STATUS_PENDING_REVIEW)
        return SessionStats(
            total = total,
            accepted = accepted,
            rejected = rejected,
            pending = pending
        )
    }

    suspend fun addAuditLog(
        eventType: String,
        sessionId: String? = null,
        studentId: String? = null,
        details: String
    ) {
        val log = AuditLogEntity(
            timestamp = System.currentTimeMillis(),
            eventType = eventType,
            sessionId = sessionId,
            studentId = studentId,
            details = details
        )
        auditLogDao.insert(log)
    }

    suspend fun getAuditLogs(sessionId: String): List<AuditLogEntity> {
        return auditLogDao.getLogsForSession(sessionId)
    }

    suspend fun getAllSessions(): List<SessionEntity> {
        return sessionDao.getAllSessions()
    }

    fun getAttendanceForSessionFlow(sessionId: String): Flow<List<AttendanceRecordEntity>> {
        return attendanceDao.getRecordsForSessionFlow(sessionId)
    }
}

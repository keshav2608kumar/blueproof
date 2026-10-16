package com.blueproof.session

import com.blueproof.data.db.entity.AttendanceRecordEntity
import com.blueproof.data.db.entity.SessionEntity
import com.blueproof.data.repository.AttendanceRepository
import com.blueproof.util.Constants
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SessionManager(
    private val repository: AttendanceRepository,
    private val scope: CoroutineScope
) {
    private val _currentSession = MutableStateFlow<SessionEntity?>(null)
    val currentSession: StateFlow<SessionEntity?> = _currentSession.asStateFlow()

    private val validator = SessionValidator()
    private val checkInMutex = Mutex()
    private var challengeRotationJob: Job? = null
    private val gson = Gson()

    suspend fun startSession(
        name: String,
        teacherName: String,
        teacherId: String = "TCH-001",
        durationMinutes: Int = Constants.SESSION_DEFAULT_DURATION_MIN,
        rssiThreshold: Int = Constants.DEFAULT_RSSI_THRESHOLD
    ): SessionEntity {
        val session = repository.createSession(name, teacherName, teacherId, durationMinutes, rssiThreshold)
        val challenge = ChallengeGenerator.generateChallenge()
        val expiresAt = System.currentTimeMillis() + Constants.CHALLENGE_VALIDITY_MS
        val updatedSession = session.copy(
            currentChallenge = challenge,
            challengeExpiresAt = expiresAt
        )
        repository.updateSession(updatedSession)
        _currentSession.value = updatedSession

        repository.addAuditLog(
            eventType = Constants.AUDIT_SESSION_STARTED,
            sessionId = session.sessionId,
            details = """{"name":"$name","teacherName":"$teacherName","teacherId":"$teacherId","duration":$durationMinutes}"""
        )

        startChallengeRotation(updatedSession.sessionId)
        return updatedSession
    }

    suspend fun pauseSession() {
        val session = _currentSession.value ?: return
        val paused = session.copy(status = Constants.SESSION_STATUS_PAUSED)
        repository.updateSession(paused)
        _currentSession.value = paused
        challengeRotationJob?.cancel()
        repository.addAuditLog(Constants.AUDIT_SESSION_PAUSED, session.sessionId, details = "{}")
    }

    suspend fun resumeSession() {
        val session = _currentSession.value ?: return
        val challenge = ChallengeGenerator.generateChallenge()
        val expiresAt = System.currentTimeMillis() + Constants.CHALLENGE_VALIDITY_MS
        val resumed = session.copy(
            status = Constants.SESSION_STATUS_ACTIVE,
            currentChallenge = challenge,
            challengeExpiresAt = expiresAt
        )
        repository.updateSession(resumed)
        _currentSession.value = resumed
        startChallengeRotation(resumed.sessionId)
        repository.addAuditLog(Constants.AUDIT_SESSION_RESUMED, session.sessionId, details = "{}")
    }

    suspend fun endSession() {
        val session = _currentSession.value ?: return
        challengeRotationJob?.cancel()
        val ended = session.copy(
            status = Constants.SESSION_STATUS_ENDED,
            endTime = System.currentTimeMillis(),
            currentChallenge = null,
            challengeExpiresAt = null
        )
        repository.updateSession(ended)
        _currentSession.value = null
        repository.addAuditLog(Constants.AUDIT_SESSION_ENDED, session.sessionId, details = "{}")
    }

    suspend fun rotateChallenge(): String {
        val session = _currentSession.value ?: throw IllegalStateException("No active session")
        val challenge = ChallengeGenerator.generateChallenge()
        val expiresAt = System.currentTimeMillis() + Constants.CHALLENGE_VALIDITY_MS
        val updated = session.copy(currentChallenge = challenge, challengeExpiresAt = expiresAt)
        repository.updateSession(updated)
        _currentSession.value = updated
        return challenge
    }

    fun getCurrentChallenge(): Pair<String, Long>? {
        val session = _currentSession.value ?: return null
        val challenge = session.currentChallenge ?: return null
        val expiresAt = session.challengeExpiresAt ?: return null
        return Pair(challenge, expiresAt)
    }

    suspend fun processCheckIn(
        studentId: String,
        studentName: String = "",
        deviceId: String = "",
        enrollmentKey: String = "",
        challenge: String,
        timestamp: Long,
        hmac: String,
        rssiMedian: Int,
        rssiSamples: List<Int>
    ): AttendanceRecordEntity {
        return checkInMutex.withLock {
            val session = _currentSession.value
                ?: throw IllegalStateException("No active session")

            // 1. Check for duplicate submission by the same student in this session
            if (repository.isDuplicate(studentId, session.sessionId)) {
                return@withLock AttendanceRecordEntity(
                    studentId = studentId,
                    sessionId = session.sessionId,
                    studentName = studentName.ifEmpty { "Student $studentId" },
                    checkInTime = System.currentTimeMillis(),
                    rssiMedian = rssiMedian,
                    rssiSamples = gson.toJson(rssiSamples),
                    status = Constants.STATUS_REJECTED,
                    rejectionReason = "Duplicate check-in: already recorded for this session",
                    challengeUsed = challenge,
                    deviceId = deviceId
                )
            }

            // 2. ANTI-PROXY SAFEGUARD: Physical Hardware Device Exclusivity
            // One physical phone (deviceId) can only submit attendance for ONE student per session.
            // Prevents buddy punching / proxy attendance where one phone submits for multiple IDs.
            if (deviceId.isNotBlank()) {
                val priorDeviceRecord = repository.getAttendanceForDeviceInSession(deviceId, session.sessionId)
                if (priorDeviceRecord != null && priorDeviceRecord.studentId != studentId) {
                    val proxyReason = "Proxy attempt rejected: Physical phone already used by ${priorDeviceRecord.studentName} (${priorDeviceRecord.studentId}) in this session."
                    val proxyRecord = AttendanceRecordEntity(
                        studentId = studentId,
                        sessionId = session.sessionId,
                        studentName = studentName.ifEmpty { "Student $studentId" },
                        checkInTime = System.currentTimeMillis(),
                        rssiMedian = rssiMedian,
                        rssiSamples = gson.toJson(rssiSamples),
                        status = Constants.STATUS_REJECTED,
                        rejectionReason = proxyReason,
                        challengeUsed = challenge,
                        deviceId = deviceId
                    )
                    try {
                        repository.recordAttendance(proxyRecord)
                    } catch (_: Exception) {}
                    repository.addAuditLog(
                        eventType = Constants.AUDIT_CHECKIN_REJECTED,
                        sessionId = session.sessionId,
                        studentId = studentId,
                        details = """{"proxyAlert":true,"deviceId":"$deviceId","attemptedStudentId":"$studentId","priorStudentId":"${priorDeviceRecord.studentId}","reason":"$proxyReason"}"""
                    )
                    return@withLock proxyRecord
                }
            }

            // 3. Look up or auto-enroll student on teacher device
            var student = repository.getStudent(studentId)
            if (student == null && enrollmentKey.isNotEmpty()) {
                student = repository.registerStudentWithKey(
                    studentId = studentId,
                    name = studentName.ifEmpty { "Student $studentId" },
                    deviceId = deviceId,
                    enrollmentKey = enrollmentKey
                )
                repository.addAuditLog(
                    eventType = Constants.AUDIT_STUDENT_ENROLLED,
                    sessionId = session.sessionId,
                    studentId = studentId,
                    details = """{"name":"${student.name}","autoEnrolled":true,"deviceId":"$deviceId"}"""
                )
            } else if (student != null && student.deviceId.isBlank() && enrollmentKey.isNotEmpty()) {
                // Device lock was reset by teacher (e.g. student replaced a lost/broken phone)
                val targetName = if (studentName.isNotEmpty()) studentName else student.name
                val rebound = repository.registerStudentWithKey(
                    studentId = student.studentId,
                    name = targetName,
                    deviceId = deviceId,
                    enrollmentKey = enrollmentKey
                )
                student = rebound
                repository.addAuditLog(
                    eventType = Constants.AUDIT_STUDENT_ENROLLED,
                    sessionId = session.sessionId,
                    studentId = studentId,
                    details = """{"name":"${rebound.name}","deviceRebound":true,"newDeviceId":"$deviceId"}"""
                )
            }

            // 4. Validate (including device binding check)
            val result = validator.validateCheckIn(
                session = session,
                student = student,
                challenge = challenge,
                timestamp = timestamp,
                hmac = hmac,
                rssiMedian = rssiMedian,
                rssiSampleCount = rssiSamples.size,
                deviceId = deviceId
            )

            val record = AttendanceRecordEntity(
                studentId = studentId,
                sessionId = session.sessionId,
                studentName = student?.name ?: studentName.ifEmpty { "Student $studentId" },
                checkInTime = System.currentTimeMillis(),
                rssiMedian = rssiMedian,
                rssiSamples = gson.toJson(rssiSamples),
                status = result.status,
                rejectionReason = result.reason,
                challengeUsed = challenge,
                deviceId = deviceId
            )

            try {
                repository.recordAttendance(record)
            } catch (e: Exception) {
                // Already exists - duplicate
            }

            val auditType = if (result.isValid) Constants.AUDIT_CHECKIN_ACCEPTED else Constants.AUDIT_CHECKIN_REJECTED
            repository.addAuditLog(
                eventType = auditType,
                sessionId = session.sessionId,
                studentId = studentId,
                details = """{"rssi":$rssiMedian,"deviceId":"$deviceId","status":"${result.status}","reason":"${result.reason ?: ""}"}"""
            )

            record
        }
    }

    private fun startChallengeRotation(sessionId: String) {
        challengeRotationJob?.cancel()
        challengeRotationJob = scope.launch {
            while (isActive) {
                delay(Constants.CHALLENGE_VALIDITY_MS - 2000) // Rotate 2s before expiry
                try {
                    rotateChallenge()
                } catch (e: Exception) {
                    break
                }
            }
        }
    }
}

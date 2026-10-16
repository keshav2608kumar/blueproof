package com.blueproof.session

import com.blueproof.data.db.dao.AttendanceDao
import com.blueproof.data.db.dao.AuditLogDao
import com.blueproof.data.db.dao.SessionDao
import com.blueproof.data.db.dao.StudentDao
import com.blueproof.data.db.entity.AttendanceRecordEntity
import com.blueproof.data.db.entity.AuditLogEntity
import com.blueproof.data.db.entity.SessionEntity
import com.blueproof.data.db.entity.StudentEntity
import com.blueproof.data.repository.AttendanceRepository
import com.blueproof.util.Constants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class AntiProxyEndToEndTest {

    private var sessionJob = Job()
    private var sessionScope = CoroutineScope(Dispatchers.Unconfined + sessionJob)

    private lateinit var fakeStudentDao: FakeStudentDao
    private lateinit var fakeSessionDao: FakeSessionDao
    private lateinit var fakeAttendanceDao: FakeAttendanceDao
    private lateinit var fakeAuditLogDao: FakeAuditLogDao

    private lateinit var repository: AttendanceRepository
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        sessionJob = Job()
        sessionScope = CoroutineScope(Dispatchers.Unconfined + sessionJob)

        fakeStudentDao = FakeStudentDao()
        fakeSessionDao = FakeSessionDao()
        fakeAttendanceDao = FakeAttendanceDao()
        fakeAuditLogDao = FakeAuditLogDao()

        repository = AttendanceRepository(
            studentDao = fakeStudentDao,
            sessionDao = fakeSessionDao,
            attendanceDao = fakeAttendanceDao,
            auditLogDao = fakeAuditLogDao
        )

        sessionManager = SessionManager(repository, sessionScope)
    }

    @After
    fun tearDown() {
        runBlocking {
            try {
                sessionManager.endSession()
            } catch (_: Exception) {}
        }
        sessionJob.cancel()
    }

    @Test
    fun test1_teacherStartsSessionWithCustomIdAndName() = runTest {
        val session = sessionManager.startSession(
            name = "CS201 Distributed Systems",
            teacherName = "Dr. Alexander Vance",
            teacherId = "TCH-1004",
            durationMinutes = 45,
            rssiThreshold = -75
        )

        assertEquals("CS201 Distributed Systems", session.name)
        assertEquals("Dr. Alexander Vance", session.teacherName)
        assertEquals("TCH-1004", session.teacherId)
        assertEquals(Constants.SESSION_STATUS_ACTIVE, session.status)
        assertNotNull(session.currentChallenge)
        assertTrue(session.currentChallenge!!.length == 64)

        // Verify audit log
        val logs = fakeAuditLogDao.logs
        assertTrue(logs.any { it.eventType == Constants.AUDIT_SESSION_STARTED && it.details.contains("TCH-1004") })
    }

    @Test
    fun test2_threeStudentsNormalCheckInSuccess() = runTest {
        val session = sessionManager.startSession(
            name = "CS101 Attendance",
            teacherName = "Dr. Alexander Vance",
            teacherId = "TCH-1004",
            rssiThreshold = -75
        )
        val challenge = session.currentChallenge!!

        // Student 1: Alice on Phone A
        val aliceKey = ChallengeGenerator.generateEnrollmentKey()
        val aliceTime = System.currentTimeMillis()
        val aliceHmac = ChallengeGenerator.computeHmac(challenge, "S101", aliceTime, aliceKey)
        val aliceRecord = sessionManager.processCheckIn(
            studentId = "S101",
            studentName = "Alice Smith",
            deviceId = "PHONE_ALICE_001",
            enrollmentKey = aliceKey,
            challenge = challenge,
            timestamp = aliceTime,
            hmac = aliceHmac,
            rssiMedian = -65,
            rssiSamples = listOf(-64, -66, -65, -63, -67)
        )
        assertEquals(Constants.STATUS_ACCEPTED, aliceRecord.status)
        assertNull(aliceRecord.rejectionReason)

        // Student 2: Bob on Phone B
        val bobKey = ChallengeGenerator.generateEnrollmentKey()
        val bobTime = System.currentTimeMillis()
        val bobHmac = ChallengeGenerator.computeHmac(challenge, "S102", bobTime, bobKey)
        val bobRecord = sessionManager.processCheckIn(
            studentId = "S102",
            studentName = "Bob Jones",
            deviceId = "PHONE_BOB_002",
            enrollmentKey = bobKey,
            challenge = challenge,
            timestamp = bobTime,
            hmac = bobHmac,
            rssiMedian = -68,
            rssiSamples = listOf(-68, -69, -67, -70, -68)
        )
        assertEquals(Constants.STATUS_ACCEPTED, bobRecord.status)
        assertNull(bobRecord.rejectionReason)

        // Student 3: Charlie on Phone C
        val charlieKey = ChallengeGenerator.generateEnrollmentKey()
        val charlieTime = System.currentTimeMillis()
        val charlieHmac = ChallengeGenerator.computeHmac(challenge, "S103", charlieTime, charlieKey)
        val charlieRecord = sessionManager.processCheckIn(
            studentId = "S103",
            studentName = "Charlie Brown",
            deviceId = "PHONE_CHARLIE_003",
            enrollmentKey = charlieKey,
            challenge = challenge,
            timestamp = charlieTime,
            hmac = charlieHmac,
            rssiMedian = -62,
            rssiSamples = listOf(-62, -61, -63, -62, -64)
        )
        assertEquals(Constants.STATUS_ACCEPTED, charlieRecord.status)

        // Verify session stats
        val stats = repository.getSessionStats(session.sessionId)
        assertEquals(3, stats.total)
        assertEquals(3, stats.accepted)
        assertEquals(0, stats.rejected)
    }

    @Test
    fun test3_proxyAttackSamePhoneSwitchedIdRejected() = runTest {
        val session = sessionManager.startSession(
            name = "CS101 Attendance",
            teacherName = "Dr. Alexander Vance",
            teacherId = "TCH-1004"
        )
        val challenge = session.currentChallenge!!

        // Step 1: Alice legitimately checks in on Physical Phone A
        val aliceKey = ChallengeGenerator.generateEnrollmentKey()
        val aliceTime = System.currentTimeMillis()
        val aliceHmac = ChallengeGenerator.computeHmac(challenge, "S101", aliceTime, aliceKey)
        val aliceResult = sessionManager.processCheckIn(
            studentId = "S101",
            studentName = "Alice Smith",
            deviceId = "PHYSICAL_PHONE_A",
            enrollmentKey = aliceKey,
            challenge = challenge,
            timestamp = aliceTime,
            hmac = aliceHmac,
            rssiMedian = -65,
            rssiSamples = listOf(-64, -65, -66, -65, -67)
        )
        assertEquals(Constants.STATUS_ACCEPTED, aliceResult.status)

        // Step 2: Proxy Attack! User switches student ID to "Eve (S104)" on the SAME Phone A
        val eveKey = ChallengeGenerator.generateEnrollmentKey()
        val eveTime = System.currentTimeMillis()
        val eveHmac = ChallengeGenerator.computeHmac(challenge, "S104", eveTime, eveKey)
        val eveResult = sessionManager.processCheckIn(
            studentId = "S104",
            studentName = "Eve Williams",
            deviceId = "PHYSICAL_PHONE_A", // SAME PHYSICAL PHONE!
            enrollmentKey = eveKey,
            challenge = challenge,
            timestamp = eveTime,
            hmac = eveHmac,
            rssiMedian = -65,
            rssiSamples = listOf(-64, -65, -66, -65, -67)
        )

        // Step 3: MUST BE REJECTED AS PROXY
        assertEquals(Constants.STATUS_REJECTED, eveResult.status)
        assertNotNull(eveResult.rejectionReason)
        assertTrue(
            "Expected proxy rejection reason, got: ${eveResult.rejectionReason}",
            eveResult.rejectionReason!!.contains("Proxy attempt rejected: Physical phone already used by Alice Smith (S101)")
        )

        // Verify audit log has proxyAlert = true
        val proxyLogs = fakeAuditLogDao.logs.filter { it.details.contains("proxyAlert") }
        assertEquals(1, proxyLogs.size)
        assertTrue(proxyLogs.first().details.contains("attemptedStudentId\":\"S104"))
        assertTrue(proxyLogs.first().details.contains("priorStudentId\":\"S101"))

        // Verify stats: 1 accepted, 1 rejected
        val stats = repository.getSessionStats(session.sessionId)
        assertEquals(2, stats.total)
        assertEquals(1, stats.accepted)
        assertEquals(1, stats.rejected)
    }

    @Test
    fun test4_deviceMismatchAttackDifferentPhoneSameIdRejected() = runTest {
        val session = sessionManager.startSession("CS101", "Dr. Vance", "TCH-1004")
        val challenge = session.currentChallenge!!

        // Student registered on Phone B
        val bob = repository.registerStudent("S102", "Bob Jones", "PHYSICAL_PHONE_B")
        val timestamp = System.currentTimeMillis()
        val hmac = ChallengeGenerator.computeHmac(challenge, bob.studentId, timestamp, bob.enrollmentKey)

        // Attacker attempts check-in for Bob from Phone D
        val result = sessionManager.processCheckIn(
            studentId = "S102",
            studentName = "Bob Jones",
            deviceId = "ROGUE_PHONE_D", // Different device!
            enrollmentKey = bob.enrollmentKey,
            challenge = challenge,
            timestamp = timestamp,
            hmac = hmac,
            rssiMedian = -65,
            rssiSamples = listOf(-65, -66, -64, -65, -67)
        )

        assertEquals(Constants.STATUS_REJECTED, result.status)
        assertNotNull(result.rejectionReason)
        assertTrue(result.rejectionReason!!.contains("Device mismatch"))
    }

    @Test
    fun test5_duplicateCheckInSameStudentRejected() = runTest {
        val session = sessionManager.startSession("CS101", "Dr. Vance", "TCH-1004")
        val challenge = session.currentChallenge!!

        val aliceKey = ChallengeGenerator.generateEnrollmentKey()
        val timestamp = System.currentTimeMillis()
        val hmac = ChallengeGenerator.computeHmac(challenge, "S101", timestamp, aliceKey)

        // First check-in
        val first = sessionManager.processCheckIn(
            studentId = "S101",
            studentName = "Alice",
            deviceId = "PHONE_A",
            enrollmentKey = aliceKey,
            challenge = challenge,
            timestamp = timestamp,
            hmac = hmac,
            rssiMedian = -65,
            rssiSamples = listOf(-65, -65, -65, -65, -65)
        )
        assertEquals(Constants.STATUS_ACCEPTED, first.status)

        // Second check-in by Alice in same session
        val second = sessionManager.processCheckIn(
            studentId = "S101",
            studentName = "Alice",
            deviceId = "PHONE_A",
            enrollmentKey = aliceKey,
            challenge = challenge,
            timestamp = timestamp + 1000,
            hmac = ChallengeGenerator.computeHmac(challenge, "S101", timestamp + 1000, aliceKey),
            rssiMedian = -65,
            rssiSamples = listOf(-65, -65, -65, -65, -65)
        )
        assertEquals(Constants.STATUS_REJECTED, second.status)
        assertTrue(second.rejectionReason!!.contains("Duplicate check-in"))
    }

    @Test
    fun test6_weakRssiOutsideClassroomRejected() = runTest {
        val session = sessionManager.startSession("CS101", "Dr. Vance", "TCH-1004", rssiThreshold = -75)
        val challenge = session.currentChallenge!!

        val key = ChallengeGenerator.generateEnrollmentKey()
        val time = System.currentTimeMillis()
        val hmac = ChallengeGenerator.computeHmac(challenge, "S105", time, key)

        // Weak RSSI (-85 dBm < -75 dBm)
        val result = sessionManager.processCheckIn(
            studentId = "S105",
            studentName = "Dave Outside",
            deviceId = "PHONE_DAVE",
            enrollmentKey = key,
            challenge = challenge,
            timestamp = time,
            hmac = hmac,
            rssiMedian = -85,
            rssiSamples = listOf(-85, -86, -84, -85, -87)
        )
        assertEquals(Constants.STATUS_REJECTED, result.status)
        assertTrue(result.rejectionReason!!.contains("Signal too weak"))
    }

    @Test
    fun test7_insufficientRssiSamplesRejected() = runTest {
        val session = sessionManager.startSession("CS101", "Dr. Vance", "TCH-1004")
        val challenge = session.currentChallenge!!

        val key = ChallengeGenerator.generateEnrollmentKey()
        val time = System.currentTimeMillis()
        val hmac = ChallengeGenerator.computeHmac(challenge, "S106", time, key)

        // Only 2 samples (< 5)
        val result = sessionManager.processCheckIn(
            studentId = "S106",
            studentName = "Eve Fast",
            deviceId = "PHONE_EVE",
            enrollmentKey = key,
            challenge = challenge,
            timestamp = time,
            hmac = hmac,
            rssiMedian = -60,
            rssiSamples = listOf(-60, -61)
        )
        assertEquals(Constants.STATUS_REJECTED, result.status)
        assertTrue(result.rejectionReason!!.contains("Insufficient proximity samples"))
    }

    @Test
    fun test8_expiredChallengeRejected() = runTest {
        val session = sessionManager.startSession("CS101", "Dr. Vance", "TCH-1004")
        val key = ChallengeGenerator.generateEnrollmentKey()
        val time = System.currentTimeMillis()

        // Expired challenge from 5 minutes ago
        val oldChallenge = ChallengeGenerator.generateChallenge()
        val hmac = ChallengeGenerator.computeHmac(oldChallenge, "S107", time, key)

        val result = sessionManager.processCheckIn(
            studentId = "S107",
            studentName = "Replay Attacker",
            deviceId = "PHONE_REPLAY",
            enrollmentKey = key,
            challenge = oldChallenge,
            timestamp = time,
            hmac = hmac,
            rssiMedian = -60,
            rssiSamples = listOf(-60, -61, -59, -60, -62)
        )
        assertEquals(Constants.STATUS_REJECTED, result.status)
        assertTrue(result.rejectionReason!!.contains("Invalid or expired challenge"))
    }

    @Test
    fun test9_tamperedHmacRejected() = runTest {
        val session = sessionManager.startSession("CS101", "Dr. Vance", "TCH-1004")
        val challenge = session.currentChallenge!!
        val key = ChallengeGenerator.generateEnrollmentKey()
        val time = System.currentTimeMillis()

        // Bogus HMAC
        val bogusHmac = "0123456789abcdef".repeat(4)

        val result = sessionManager.processCheckIn(
            studentId = "S108",
            studentName = "Tamperer",
            deviceId = "PHONE_TAMPER",
            enrollmentKey = key,
            challenge = challenge,
            timestamp = time,
            hmac = bogusHmac,
            rssiMedian = -60,
            rssiSamples = listOf(-60, -61, -59, -60, -62)
        )
        assertEquals(Constants.STATUS_REJECTED, result.status)
        assertTrue(result.rejectionReason!!.contains("HMAC mismatch"))
    }

    @Test
    fun test10_sessionPausedAndResumedLifecycle() = runTest {
        val session = sessionManager.startSession("CS101", "Dr. Vance", "TCH-1004")
        val key = ChallengeGenerator.generateEnrollmentKey()

        // 1. Pause session
        sessionManager.pauseSession()
        val time = System.currentTimeMillis()
        val hmac = ChallengeGenerator.computeHmac(session.currentChallenge ?: "", "S109", time, key)

        val pausedResult = sessionManager.processCheckIn(
            studentId = "S109",
            studentName = "Student 109",
            deviceId = "PHONE_109",
            enrollmentKey = key,
            challenge = session.currentChallenge ?: "",
            timestamp = time,
            hmac = hmac,
            rssiMedian = -60,
            rssiSamples = listOf(-60, -61, -59, -60, -62)
        )
        assertEquals(Constants.STATUS_REJECTED, pausedResult.status)
        assertTrue(pausedResult.rejectionReason!!.contains("Session is not active"))

        // 2. Resume session with student S110
        sessionManager.resumeSession()
        val newChallenge = sessionManager.currentSession.value!!.currentChallenge!!
        val resumeTime = System.currentTimeMillis()
        val key2 = ChallengeGenerator.generateEnrollmentKey()
        val validHmac = ChallengeGenerator.computeHmac(newChallenge, "S110", resumeTime, key2)

        val resumeResult = sessionManager.processCheckIn(
            studentId = "S110",
            studentName = "Student 110",
            deviceId = "PHONE_110",
            enrollmentKey = key2,
            challenge = newChallenge,
            timestamp = resumeTime,
            hmac = validHmac,
            rssiMedian = -60,
            rssiSamples = listOf(-60, -61, -59, -60, -62)
        )
        assertEquals(Constants.STATUS_ACCEPTED, resumeResult.status)

        // 3. End session
        sessionManager.endSession()
        try {
            sessionManager.processCheckIn(
                studentId = "S111",
                studentName = "Student 111",
                deviceId = "PHONE_111",
                enrollmentKey = key2,
                challenge = newChallenge,
                timestamp = resumeTime,
                hmac = validHmac,
                rssiMedian = -60,
                rssiSamples = listOf(-60, -61, -59, -60, -62)
            )
            fail("Expected IllegalStateException on ended session")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("No active session"))
        }
    }

    @Test
    fun test11_lostPhoneReplacedAndDeviceLockResetAllowsNewPhoneCheckIn() = runBlocking {
        sessionManager.startSession(
            name = "Device Reset Lab",
            teacherName = "Prof. Davis",
            teacherId = "TCH-555",
            durationMinutes = 30,
            rssiThreshold = -75
        )

        val (challenge, _) = sessionManager.getCurrentChallenge()!!
        val studentId = "STU_LOST_01"
        val oldPhoneId = "OLD_PHONE_HARDWARE_111"
        val oldKey = ChallengeGenerator.generateEnrollmentKey()

        // 1. Initial check-in from old phone -> ACCEPTED
        val time1 = System.currentTimeMillis()
        val hmac1 = ChallengeGenerator.computeHmac(challenge, studentId, time1, oldKey)
        val result1 = sessionManager.processCheckIn(
            studentId = studentId,
            studentName = "Frank",
            deviceId = oldPhoneId,
            enrollmentKey = oldKey,
            challenge = challenge,
            timestamp = time1,
            hmac = hmac1,
            rssiMedian = -62,
            rssiSamples = listOf(-62, -63, -61, -62, -64)
        )
        assertEquals(Constants.STATUS_ACCEPTED, result1.status)
        assertEquals(oldPhoneId, repository.getStudent(studentId)?.deviceId)

        // Rotate session to next class
        sessionManager.endSession()
        sessionManager.startSession(
            name = "Next Class Lab",
            teacherName = "Prof. Davis",
            teacherId = "TCH-555",
            durationMinutes = 30,
            rssiThreshold = -75
        )
        val (nextChallenge, _) = sessionManager.getCurrentChallenge()!!

        // 2. Frank lost his old phone and bought a new phone
        val newPhoneId = "NEW_PHONE_HARDWARE_999"
        val newKey = ChallengeGenerator.generateEnrollmentKey()
        val time2 = System.currentTimeMillis()
        val hmac2 = ChallengeGenerator.computeHmac(nextChallenge, studentId, time2, newKey)

        // 3. Trying to check in on new phone WITHOUT teacher reset -> REJECTED (Anti-Proxy protects student account)
        val rejectedResult = sessionManager.processCheckIn(
            studentId = studentId,
            studentName = "Frank",
            deviceId = newPhoneId,
            enrollmentKey = newKey,
            challenge = nextChallenge,
            timestamp = time2,
            hmac = hmac2,
            rssiMedian = -60,
            rssiSamples = listOf(-60, -61, -59, -60, -62)
        )
        assertEquals(Constants.STATUS_REJECTED, rejectedResult.status)
        assertTrue(rejectedResult.rejectionReason!!.contains("registered to another physical device") ||
                rejectedResult.rejectionReason!!.contains("HMAC mismatch"))

        // 4. Teacher resets the device lock for Frank (e.g. from Teacher Dashboard -> Enrolled -> Reset Lock)
        val resetSuccess = repository.resetStudentDevice(studentId)
        assertTrue(resetSuccess)
        assertEquals("", repository.getStudent(studentId)?.deviceId)

        // 5. Frank checks in again from his new phone -> ACCEPTED!
        val acceptedResult = sessionManager.processCheckIn(
            studentId = studentId,
            studentName = "Frank",
            deviceId = newPhoneId,
            enrollmentKey = newKey,
            challenge = nextChallenge,
            timestamp = time2,
            hmac = hmac2,
            rssiMedian = -60,
            rssiSamples = listOf(-60, -61, -59, -60, -62)
        )
        assertEquals(Constants.STATUS_ACCEPTED, acceptedResult.status)

        // 6. Frank's account is now bound to the new phone
        val updatedStudent = repository.getStudent(studentId)
        assertNotNull(updatedStudent)
        assertEquals(newPhoneId, updatedStudent?.deviceId)
        assertEquals(newKey, updatedStudent?.enrollmentKey)
    }
}

// -------------------------------------------------------------
// IN-MEMORY FAKE DAOS FOR ROBUST ZERO-DEPENDENCY TESTING
// -------------------------------------------------------------

class FakeStudentDao : StudentDao {
    val students = mutableMapOf<String, StudentEntity>()

    override suspend fun insert(student: StudentEntity): Long {
        students[student.studentId] = student
        return students.size.toLong()
    }

    override suspend fun getAllActiveStudents(): List<StudentEntity> =
        students.values.filter { it.isActive }.sortedBy { it.name }

    override suspend fun getAllStudents(): List<StudentEntity> =
        students.values.sortedBy { it.name }

    override suspend fun findByStudentId(studentId: String): StudentEntity? = students[studentId]

    override suspend fun findByDeviceId(deviceId: String): StudentEntity? =
        students.values.find { it.deviceId == deviceId }

    override suspend fun countStudents(): Int = students.size

    override suspend fun update(student: StudentEntity) {
        students[student.studentId] = student
    }

    override suspend fun deleteByStudentId(studentId: String): Int {
        val removed = students.remove(studentId)
        return if (removed != null) 1 else 0
    }
}

class FakeSessionDao : SessionDao {
    val sessions = mutableMapOf<String, SessionEntity>()

    override suspend fun insert(session: SessionEntity): Long {
        sessions[session.sessionId] = session
        return sessions.size.toLong()
    }

    override suspend fun getAllSessions(): List<SessionEntity> =
        sessions.values.sortedByDescending { it.startTime }

    override fun getAllSessionsFlow(): Flow<List<SessionEntity>> =
        flowOf(sessions.values.sortedByDescending { it.startTime })

    override suspend fun findBySessionId(sessionId: String): SessionEntity? = sessions[sessionId]

    override suspend fun getActiveSession(): SessionEntity? =
        sessions.values.find { it.status == Constants.SESSION_STATUS_ACTIVE }

    override suspend fun getActiveSessions(): List<SessionEntity> =
        sessions.values.filter { it.status == Constants.SESSION_STATUS_ACTIVE }.sortedByDescending { it.startTime }

    override suspend fun update(session: SessionEntity) {
        sessions[session.sessionId] = session
    }

    override suspend fun countSessions(): Int = sessions.size

    override suspend fun getRecentSessions(limit: Int): List<SessionEntity> =
        sessions.values.sortedByDescending { it.startTime }.take(limit)
}

class FakeAttendanceDao : AttendanceDao {
    val records = mutableListOf<AttendanceRecordEntity>()

    override suspend fun insert(record: AttendanceRecordEntity): Long {
        val existingIndex = records.indexOfFirst { it.studentId == record.studentId && it.sessionId == record.sessionId }
        if (existingIndex >= 0) {
            records[existingIndex] = record
            return existingIndex.toLong()
        }
        records.add(record)
        return records.size.toLong()
    }

    override suspend fun getRecordsForSession(sessionId: String): List<AttendanceRecordEntity> =
        records.filter { it.sessionId == sessionId }.sortedByDescending { it.checkInTime }

    override fun getRecordsForSessionFlow(sessionId: String): Flow<List<AttendanceRecordEntity>> =
        flowOf(records.filter { it.sessionId == sessionId })

    override suspend fun getRecordsForStudent(studentId: String): List<AttendanceRecordEntity> =
        records.filter { it.studentId == studentId }.sortedByDescending { it.checkInTime }

    override suspend fun findByStudentAndSession(studentId: String, sessionId: String): AttendanceRecordEntity? =
        records.find { it.studentId == studentId && it.sessionId == sessionId }

    override suspend fun findAcceptedByStudentAndSession(studentId: String, sessionId: String): AttendanceRecordEntity? =
        records.find { it.studentId == studentId && it.sessionId == sessionId && it.status == Constants.STATUS_ACCEPTED }

    override suspend fun findAcceptedBySessionAndDevice(sessionId: String, deviceId: String): AttendanceRecordEntity? =
        records.find { it.sessionId == sessionId && it.deviceId == deviceId && it.status == Constants.STATUS_ACCEPTED }

    override suspend fun findBySessionAndDevice(sessionId: String, deviceId: String): AttendanceRecordEntity? =
        records.find { it.sessionId == sessionId && it.deviceId == deviceId }

    override suspend fun countBySessionIdAndStatus(sessionId: String, status: String): Int =
        records.count { it.sessionId == sessionId && it.status == status }

    override suspend fun countAcceptedForStudent(studentId: String): Int =
        records.count { it.studentId == studentId && it.status == Constants.STATUS_ACCEPTED }

    override suspend fun getUnsyncedRecords(): List<AttendanceRecordEntity> =
        records.filter { !it.syncedToBackend }

    override suspend fun update(record: AttendanceRecordEntity) {
        val index = records.indexOfFirst { it.studentId == record.studentId && it.sessionId == record.sessionId }
        if (index >= 0) records[index] = record
    }

    override suspend fun countRecordsForSession(sessionId: String): Int =
        records.count { it.sessionId == sessionId }
}

class FakeAuditLogDao : AuditLogDao {
    val logs = mutableListOf<AuditLogEntity>()

    override suspend fun insert(log: AuditLogEntity): Long {
        logs.add(log)
        return logs.size.toLong()
    }

    override suspend fun getLogsForSession(sessionId: String): List<AuditLogEntity> =
        logs.filter { it.sessionId == sessionId }.sortedByDescending { it.timestamp }

    override fun getLogsForSessionFlow(sessionId: String): Flow<List<AuditLogEntity>> =
        flowOf(logs.filter { it.sessionId == sessionId })

    override suspend fun getRecentLogs(limit: Int): List<AuditLogEntity> =
        logs.sortedByDescending { it.timestamp }.take(limit)
}

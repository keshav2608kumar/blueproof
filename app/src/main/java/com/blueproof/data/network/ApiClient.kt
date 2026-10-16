package com.blueproof.data.network

import com.blueproof.data.db.entity.AttendanceRecordEntity
import com.blueproof.data.db.entity.SessionEntity
import com.blueproof.data.db.entity.StudentEntity
import com.blueproof.data.repository.AttendanceRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ApiClient(private var baseUrl: String = DEFAULT_BASE_URL) {

    companion object {
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8000" // Android emulator loopback or LAN IP
    }

    private val gson = Gson()

    fun setBaseUrl(url: String) {
        baseUrl = url.trimEnd('/')
    }

    fun getBaseUrl(): String = baseUrl

    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/health")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
            }
            conn.responseCode == HttpURLConnection.HTTP_OK
        } catch (_: Exception) {
            false
        }
    }

    suspend fun registerStudent(student: StudentEntity): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                addProperty("student_id", student.studentId)
                addProperty("name", student.name)
                addProperty("device_id", student.deviceId)
                addProperty("enrollment_key", student.enrollmentKey)
            }
            val response = postJson("$baseUrl/students/register", body.toString())
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startSession(session: SessionEntity): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                addProperty("session_id", session.sessionId)
                addProperty("name", session.name)
                addProperty("teacher_name", session.teacherName)
                addProperty("duration_minutes", session.durationMinutes)
                addProperty("rssi_threshold", session.rssiThreshold)
                addProperty("initial_challenge", session.currentChallenge ?: "")
            }
            val response = postJson("$baseUrl/sessions/start", body.toString())
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun endSession(sessionId: String, endTime: Long): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                addProperty("end_time", endTime)
            }
            val response = postJson("$baseUrl/sessions/$sessionId/end", body.toString())
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitAttendance(record: AttendanceRecordEntity): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                addProperty("student_id", record.studentId)
                addProperty("session_id", record.sessionId)
                addProperty("student_name", record.studentName)
                addProperty("check_in_time", record.checkInTime)
                addProperty("rssi_median", record.rssiMedian)
                addProperty("rssi_samples", record.rssiSamples)
                addProperty("status", record.status)
                addProperty("rejection_reason", record.rejectionReason)
                addProperty("challenge_used", record.challengeUsed)
            }
            val response = postJson("$baseUrl/attendance/submit", body.toString())
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncUnsyncedRecords(repository: AttendanceRepository): Int = withContext(Dispatchers.IO) {
        var syncedCount = 0
        try {
            val unsynced = repository.getUnsyncedRecords()
            for (record in unsynced) {
                val res = submitAttendance(record)
                if (res.isSuccess) {
                    repository.markRecordSynced(record)
                    syncedCount++
                }
            }
        } catch (_: Exception) {
        }
        syncedCount
    }

    private fun postJson(endpoint: String, jsonBody: String): String {
        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            doOutput = true
            connectTimeout = 5000
            readTimeout = 5000
        }

        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(jsonBody)
            writer.flush()
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
            reader.readText()
        }

        if (code !in 200..299) {
            throw RuntimeException("HTTP $code: $response")
        }
        return response
    }
}

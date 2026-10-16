package com.blueproof.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blueproof.BlueProofApp
import com.blueproof.ble.BleAdvertiser
import com.blueproof.ble.GattServer
import com.blueproof.data.db.entity.AttendanceRecordEntity
import com.blueproof.data.db.entity.SessionEntity
import com.blueproof.data.db.entity.StudentEntity
import com.blueproof.data.repository.SessionStats
import com.blueproof.session.SessionManager
import com.blueproof.util.Constants
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TeacherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as BlueProofApp).repository
    val sessionManager = SessionManager(repository, viewModelScope)
    val bleAdvertiser = BleAdvertiser(application)
    val gattServer = GattServer(application, sessionManager, viewModelScope)

    private val teacherPrefs = application.getSharedPreferences("blueproof_teacher_prefs", Context.MODE_PRIVATE)

    private val _teacherName = MutableStateFlow(
        teacherPrefs.getString("teacher_name", "Dr. Alexander Vance") ?: "Dr. Alexander Vance"
    )
    val teacherName: StateFlow<String> = _teacherName.asStateFlow()

    private val _teacherId = MutableStateFlow(
        teacherPrefs.getString("teacher_id", "TCH-1004") ?: "TCH-1004"
    )
    val teacherId: StateFlow<String> = _teacherId.asStateFlow()

    val currentSession: StateFlow<SessionEntity?> = sessionManager.currentSession
    val isAdvertising: StateFlow<Boolean> = bleAdvertiser.isAdvertising
    val advertisingError: StateFlow<String?> = bleAdvertiser.advertisingError
    val connectedDevices: StateFlow<Int> = gattServer.connectedDevices

    private val _attendanceList = MutableStateFlow<List<AttendanceRecordEntity>>(emptyList())
    val attendanceList: StateFlow<List<AttendanceRecordEntity>> = _attendanceList.asStateFlow()

    private val _sessionStats = MutableStateFlow(SessionStats(0, 0, 0, 0))
    val sessionStats: StateFlow<SessionStats> = _sessionStats.asStateFlow()

    private val _recentSessions = MutableStateFlow<List<SessionEntity>>(emptyList())
    val recentSessions: StateFlow<List<SessionEntity>> = _recentSessions.asStateFlow()

    private val _enrolledCount = MutableStateFlow(0)
    val enrolledCount: StateFlow<Int> = _enrolledCount.asStateFlow()

    private val _enrolledStudents = MutableStateFlow<List<StudentEntity>>(emptyList())
    val enrolledStudents: StateFlow<List<StudentEntity>> = _enrolledStudents.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun updateTeacherProfile(name: String, id: String) {
        val trimmedName = name.trim().ifEmpty { "Dr. Alexander Vance" }
        val trimmedId = id.trim().ifEmpty { "TCH-1004" }
        _teacherName.value = trimmedName
        _teacherId.value = trimmedId
        teacherPrefs.edit()
            .putString("teacher_name", trimmedName)
            .putString("teacher_id", trimmedId)
            .apply()
    }

    init {
        loadDashboardData()
        // Listen to check-ins from GATT server
        viewModelScope.launch {
            gattServer.lastCheckIn.collect { record ->
                if (record != null) {
                    val current = _attendanceList.value.toMutableList()
                    val existingIndex = current.indexOfFirst { it.studentId == record.studentId }
                    if (existingIndex >= 0) {
                        current[existingIndex] = record
                    } else {
                        current.add(0, record)
                    }
                    _attendanceList.value = current
                    updateStats()
                }
            }
        }
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _recentSessions.value = repository.getAllSessions()
            val students = repository.getAllStudents()
            _enrolledStudents.value = students
            _enrolledCount.value = students.size
        }
    }

    fun resetStudentDeviceLock(studentId: String) {
        viewModelScope.launch {
            repository.resetStudentDevice(studentId)
            loadDashboardData()
        }
    }

    fun deleteStudent(studentId: String) {
        viewModelScope.launch {
            repository.deleteStudent(studentId)
            loadDashboardData()
        }
    }

    fun startSession(
        name: String,
        teacherName: String = _teacherName.value,
        teacherId: String = _teacherId.value,
        durationMinutes: Int = Constants.SESSION_DEFAULT_DURATION_MIN,
        rssiThreshold: Int = Constants.DEFAULT_RSSI_THRESHOLD
    ) {
        viewModelScope.launch {
            try {
                updateTeacherProfile(teacherName, teacherId)
                val session = sessionManager.startSession(name, teacherName, teacherId, durationMinutes, rssiThreshold)
                gattServer.start()
                bleAdvertiser.startAdvertising(session.sessionId)
                _attendanceList.value = emptyList()
                _sessionStats.value = SessionStats(0, 0, 0, 0)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to start session: ${e.message}"
            }
        }
    }

    fun pauseSession() {
        viewModelScope.launch {
            sessionManager.pauseSession()
            bleAdvertiser.stopAdvertising()
        }
    }

    fun resumeSession() {
        viewModelScope.launch {
            sessionManager.resumeSession()
            val session = currentSession.value
            if (session != null) {
                bleAdvertiser.startAdvertising(session.sessionId)
            }
        }
    }

    fun endSession() {
        viewModelScope.launch {
            bleAdvertiser.stopAdvertising()
            gattServer.stop()
            sessionManager.endSession()
            loadDashboardData()
        }
    }

    private suspend fun updateStats() {
        val session = currentSession.value ?: return
        _sessionStats.value = repository.getSessionStats(session.sessionId)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TeacherViewModel(application) as T
        }
    }
}

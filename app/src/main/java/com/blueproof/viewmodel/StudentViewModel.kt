package com.blueproof.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blueproof.BlueProofApp
import com.blueproof.ble.BleScanner
import com.blueproof.ble.GattClient
import com.blueproof.data.db.entity.AttendanceRecordEntity
import com.blueproof.data.db.entity.StudentEntity
import com.blueproof.util.Constants
import com.blueproof.util.RssiAnalyzer
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StudentViewModel(application: Application) : AndroidViewModel(application) {

    enum class ScanState {
        IDLE, SCANNING, CONNECTING, CHECKING_IN, SUCCESS, FAILED, ALREADY_CHECKED_IN
    }

    private val repository = (application as BlueProofApp).repository
    val bleScanner = BleScanner(application)
    val gattClient = GattClient(application)
    private val prefs = application.getSharedPreferences("blueproof_student_prefs", Context.MODE_PRIVATE)

    val discoveredSessions = bleScanner.discoveredSessions
    val isScanning = bleScanner.isScanning

    private val _currentStudent = MutableStateFlow<StudentEntity?>(null)
    val currentStudent: StateFlow<StudentEntity?> = _currentStudent.asStateFlow()

    private val _scanState = MutableStateFlow(ScanState.IDLE)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _checkInResult = MutableStateFlow<GattClient.CheckInResult?>(null)
    val checkInResult: StateFlow<GattClient.CheckInResult?> = _checkInResult.asStateFlow()

    private val _attendanceHistory = MutableStateFlow<List<AttendanceRecordEntity>>(emptyList())
    val attendanceHistory: StateFlow<List<AttendanceRecordEntity>> = _attendanceHistory.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    @SuppressLint("HardwareIds")
    val deviceId: String = Settings.Secure.getString(
        application.contentResolver,
        Settings.Secure.ANDROID_ID
    ) ?: "device_${System.currentTimeMillis()}"

    init {
        // Auto-restore previously logged in student so user never has to register again
        viewModelScope.launch {
            val savedId = prefs.getString("active_student_id", null)
            if (!savedId.isNullOrBlank()) {
                val student = repository.getStudent(savedId)
                if (student != null) {
                    _currentStudent.value = student
                    loadAttendanceHistory()
                    return@launch
                }
            }
            // Fallback: check if device is already registered
            val deviceStudent = repository.getStudentByDevice(deviceId)
            if (deviceStudent != null) {
                _currentStudent.value = deviceStudent
                prefs.edit().putString("active_student_id", deviceStudent.studentId).apply()
                loadAttendanceHistory()
                return@launch
            }
            // Fallback: check if any student exists locally
            val allStudents = repository.getAllStudents()
            if (allStudents.isNotEmpty()) {
                val first = allStudents.first()
                _currentStudent.value = first
                prefs.edit().putString("active_student_id", first.studentId).apply()
                loadAttendanceHistory()
            }
        }
    }

    fun registerStudent(studentId: String, name: String) {
        viewModelScope.launch {
            try {
                val trimmedId = studentId.trim()
                val trimmedName = name.trim()
                val existing = repository.getStudent(trimmedId)
                val student = if (existing != null) {
                    // Update existing registration smoothly
                    val updated = existing.copy(name = trimmedName, deviceId = deviceId)
                    repository.registerStudentWithKey(
                        studentId = updated.studentId,
                        name = updated.name,
                        deviceId = updated.deviceId,
                        enrollmentKey = updated.enrollmentKey
                    )
                    updated
                } else {
                    repository.registerStudent(trimmedId, trimmedName, deviceId)
                }
                _currentStudent.value = student
                prefs.edit().putString("active_student_id", student.studentId).apply()
                _errorMessage.value = null
                loadAttendanceHistory()
            } catch (e: Exception) {
                _errorMessage.value = "Registration failed: ${e.message}"
            }
        }
    }

    fun loginStudent(studentId: String) {
        viewModelScope.launch {
            val trimmedId = studentId.trim()
            val student = repository.getStudent(trimmedId)
            if (student != null) {
                _currentStudent.value = student
                prefs.edit().putString("active_student_id", student.studentId).apply()
                _errorMessage.value = null
                loadAttendanceHistory()
            } else {
                _errorMessage.value = "Student ID not found. Please register first."
            }
        }
    }

    fun logout() {
        _currentStudent.value = null
        prefs.edit().remove("active_student_id").apply()
        _attendanceHistory.value = emptyList()
        _errorMessage.value = null
    }

    fun startScanning() {
        _scanState.value = ScanState.SCANNING
        bleScanner.clearResults()
        bleScanner.startScanning()
    }

    fun stopScanning() {
        bleScanner.stopScanning()
        if (_scanState.value == ScanState.SCANNING) {
            _scanState.value = ScanState.IDLE
        }
    }

    fun checkIn(session: BleScanner.DiscoveredSession) {
        val student = _currentStudent.value ?: run {
            _errorMessage.value = "Please register or sign in first"
            return
        }

        viewModelScope.launch {
            try {
                _scanState.value = ScanState.CONNECTING
                stopScanning()

                val medianRssi = bleScanner.getMedianRssi(session.deviceAddress) ?: session.rssi
                val samples = bleScanner.getRssiSamples(session.deviceAddress).ifEmpty { listOf(session.rssi) }

                _scanState.value = ScanState.CHECKING_IN

                val result = gattClient.connectAndCheckIn(
                    deviceAddress = session.deviceAddress,
                    studentId = student.studentId,
                    studentName = student.name,
                    deviceId = student.deviceId,
                    enrollmentKey = student.enrollmentKey,
                    rssiMedian = medianRssi,
                    rssiSamples = samples
                )

                _checkInResult.value = result

                if (result.success) {
                    _scanState.value = ScanState.SUCCESS
                    loadAttendanceHistory()
                } else if (result.reason?.contains("duplicate", ignoreCase = true) == true) {
                    _scanState.value = ScanState.ALREADY_CHECKED_IN
                } else {
                    _scanState.value = ScanState.FAILED
                }
            } catch (e: Exception) {
                _scanState.value = ScanState.FAILED
                _checkInResult.value = GattClient.CheckInResult(
                    success = false,
                    status = "FAILED",
                    reason = e.message ?: "Unexpected error during check-in",
                    sessionId = null,
                    teacherName = null,
                    checkInTime = null
                )
            }
        }
    }

    fun loadAttendanceHistory() {
        val student = _currentStudent.value ?: return
        viewModelScope.launch {
            _attendanceHistory.value = repository.getAttendanceForStudent(student.studentId)
        }
    }

    fun resetScanState() {
        _scanState.value = ScanState.IDLE
        _checkInResult.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StudentViewModel(application) as T
        }
    }
}

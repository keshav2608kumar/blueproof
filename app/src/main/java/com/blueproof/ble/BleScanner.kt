package com.blueproof.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import com.blueproof.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class BleScanner(private val context: Context) {

    data class DiscoveredSession(
        val deviceAddress: String,
        val sessionIdShort: String,
        val rssi: Int,
        val rssiSamples: List<Int> = emptyList(),
        val lastSeen: Long = System.currentTimeMillis()
    )

    private var scanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredSessions = MutableStateFlow<List<DiscoveredSession>>(emptyList())
    val discoveredSessions: StateFlow<List<DiscoveredSession>> = _discoveredSessions.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    private val sessionsMap = ConcurrentHashMap<String, DiscoveredSession>()
    private val lock = Any()

    @SuppressLint("MissingPermission")
    fun startScanning(): Boolean {
        if (_isScanning.value) return true

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            _scanError.value = "Bluetooth is not enabled"
            return false
        }

        scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _scanError.value = "BLE scanner not available"
            return false
        }

        clearResults()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                try {
                    result ?: return
                    val record = result.scanRecord ?: return
                    val device = result.device ?: return
                    val address = device.address ?: return
                    val rssi = result.rssi

                    // Match session via Service UUID or Manufacturer Data
                    val hasServiceUuid = record.serviceUuids?.contains(ParcelUuid(Constants.BLE_SERVICE_UUID)) == true
                    val manufacturerData = record.getManufacturerSpecificData(Constants.BLE_MANUFACTURER_ID)
                    val hasManufacturerData = manufacturerData != null
                    val deviceName = record.deviceName ?: device.name ?: ""
                    val isBlueProof = deviceName.contains("BlueProof", ignoreCase = true) ||
                            deviceName.contains("Teacher", ignoreCase = true)

                    if (!hasServiceUuid && !hasManufacturerData && !isBlueProof) {
                        return
                    }

                    val sessionShort = manufacturerData?.let { bytes ->
                        bytes.joinToString("") { String.format("%02x", it) }
                    } ?: address.takeLast(5).replace(":", "")

                    synchronized(lock) {
                        val existing = sessionsMap[address]
                        val samples = if (existing != null) {
                            (existing.rssiSamples + rssi).takeLast(20)
                        } else {
                            listOf(rssi)
                        }

                        sessionsMap[address] = DiscoveredSession(
                            deviceAddress = address,
                            sessionIdShort = sessionShort,
                            rssi = rssi,
                            rssiSamples = samples,
                            lastSeen = System.currentTimeMillis()
                        )

                        _discoveredSessions.value = sessionsMap.values.toList()
                    }
                } catch (_: Exception) {
                }
            }

            override fun onScanFailed(errorCode: Int) {
                _isScanning.value = false
                _scanError.value = when (errorCode) {
                    SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
                    SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App registration failed"
                    SCAN_FAILED_FEATURE_UNSUPPORTED -> "BLE scanning not supported"
                    SCAN_FAILED_INTERNAL_ERROR -> "Internal scan error"
                    else -> "Scan failed (error $errorCode)"
                }
            }
        }

        try {
            // Software filtering catches all devices without dropping packets due to chip hardware filters
            scanner?.startScan(emptyList(), settings, scanCallback)
            _isScanning.value = true
            _scanError.value = null
            return true
        } catch (e: SecurityException) {
            _scanError.value = "Bluetooth permission not granted"
            return false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        try {
            scanCallback?.let { scanner?.stopScan(it) }
        } catch (_: Exception) {
        }
        _isScanning.value = false
        scanCallback = null
    }

    fun getMedianRssi(deviceAddress: String): Int? {
        val samples = synchronized(lock) { sessionsMap[deviceAddress]?.rssiSamples } ?: return null
        if (samples.isEmpty()) return null
        val sorted = samples.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2 else sorted[mid]
    }

    fun getRssiSamples(deviceAddress: String): List<Int> {
        return synchronized(lock) { sessionsMap[deviceAddress]?.rssiSamples?.toList() } ?: emptyList()
    }

    fun clearResults() {
        synchronized(lock) {
            sessionsMap.clear()
            _discoveredSessions.value = emptyList()
        }
    }
}

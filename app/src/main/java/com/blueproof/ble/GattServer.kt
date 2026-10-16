package com.blueproof.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.content.Context
import com.blueproof.data.db.entity.AttendanceRecordEntity
import com.blueproof.session.SessionManager
import com.blueproof.util.Constants
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

class GattServer(
    private val context: Context,
    private val sessionManager: SessionManager,
    private val scope: CoroutineScope
) {
    private var gattServer: BluetoothGattServer? = null
    private val gson = Gson()

    private val _connectedDevices = MutableStateFlow(0)
    val connectedDevices: StateFlow<Int> = _connectedDevices.asStateFlow()

    private val _lastCheckIn = MutableStateFlow<AttendanceRecordEntity?>(null)
    val lastCheckIn: StateFlow<AttendanceRecordEntity?> = _lastCheckIn.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val connectedDeviceSet = mutableSetOf<String>()
    private val pendingWrites = mutableMapOf<String, ByteArray>()

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            ?: return false

        val callback = object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
                val address = device?.address ?: return
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        connectedDeviceSet.add(address)
                        _connectedDevices.value = connectedDeviceSet.size
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        connectedDeviceSet.remove(address)
                        pendingWrites.remove(address)
                        _connectedDevices.value = connectedDeviceSet.size
                    }
                }
            }

            @SuppressLint("MissingPermission")
            override fun onCharacteristicReadRequest(
                device: BluetoothDevice?,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic?
            ) {
                try {
                    val charUuid = characteristic?.uuid ?: run {
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, byteArrayOf())
                        return
                    }

                    when (charUuid) {
                        Constants.SESSION_INFO_CHAR_UUID -> {
                            val session = sessionManager.currentSession.value
                            val challengePair = sessionManager.getCurrentChallenge()
                            val json = JsonObject().apply {
                                addProperty("sessionId", session?.sessionId ?: "")
                                addProperty("challenge", challengePair?.first ?: "")
                                addProperty("expiresAt", challengePair?.second ?: 0L)
                                addProperty("teacherName", session?.teacherName ?: "")
                                addProperty("teacherId", session?.teacherId ?: "TCH-001")
                                addProperty("sessionName", session?.name ?: "")
                            }
                            val data = json.toString().toByteArray(Charsets.UTF_8)
                            val chunk = if (offset < data.size) data.copyOfRange(offset, data.size) else byteArrayOf()
                            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, chunk)
                        }
                        Constants.RESPONSE_CHAR_UUID -> {
                            val lastRecord = _lastCheckIn.value
                            val json = JsonObject().apply {
                                addProperty("status", lastRecord?.status ?: "NO_DATA")
                                addProperty("reason", lastRecord?.rejectionReason ?: "")
                                addProperty("checkInTime", lastRecord?.checkInTime ?: 0L)
                            }
                            val data = json.toString().toByteArray(Charsets.UTF_8)
                            val chunk = if (offset < data.size) data.copyOfRange(offset, data.size) else byteArrayOf()
                            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, chunk)
                        }
                        else -> {
                            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, byteArrayOf())
                        }
                    }
                } catch (_: Exception) {
                    try {
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, byteArrayOf())
                    } catch (_: Exception) {}
                }
            }

            @SuppressLint("MissingPermission")
            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice?,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic?,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray?
            ) {
                try {
                    if (characteristic?.uuid != Constants.CHECKIN_CHAR_UUID) {
                        if (responseNeeded) {
                            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, byteArrayOf())
                        }
                        return
                    }

                    val address = device?.address ?: ""

                    if (preparedWrite) {
                        val existing = pendingWrites.getOrDefault(address, byteArrayOf())
                        pendingWrites[address] = existing + (value ?: byteArrayOf())
                        if (responseNeeded) {
                            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value ?: byteArrayOf())
                        }
                        return
                    }

                    val payload = value ?: byteArrayOf()
                    processCheckInPayload(address, payload)

                    if (responseNeeded) {
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, byteArrayOf())
                    }
                } catch (_: Exception) {
                    if (responseNeeded) {
                        try {
                            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, byteArrayOf())
                        } catch (_: Exception) {}
                    }
                }
            }

            @SuppressLint("MissingPermission")
            override fun onExecuteWrite(device: BluetoothDevice?, requestId: Int, execute: Boolean) {
                try {
                    val address = device?.address ?: ""
                    if (execute) {
                        val accumulated = pendingWrites.remove(address)
                        if (accumulated != null) {
                            processCheckInPayload(address, accumulated)
                        }
                    } else {
                        pendingWrites.remove(address)
                    }
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, byteArrayOf())
                } catch (_: Exception) {
                    try {
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, byteArrayOf())
                    } catch (_: Exception) {}
                }
            }

            override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
                // MTU negotiated with client
            }

            override fun onDescriptorWriteRequest(
                device: BluetoothDevice?,
                requestId: Int,
                descriptor: BluetoothGattDescriptor?,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray?
            ) {
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, byteArrayOf())
                }
            }

            override fun onDescriptorReadRequest(
                device: BluetoothDevice?,
                requestId: Int,
                offset: Int,
                descriptor: BluetoothGattDescriptor?
            ) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0,
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            }
        }

        try {
            gattServer = bluetoothManager.openGattServer(context, callback)
            if (gattServer == null) return false

            val service = BluetoothGattService(
                Constants.BLE_SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )

            // Session Info characteristic (READ)
            val sessionInfoChar = BluetoothGattCharacteristic(
                Constants.SESSION_INFO_CHAR_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
            service.addCharacteristic(sessionInfoChar)

            // Check-in characteristic (WRITE)
            val checkInChar = BluetoothGattCharacteristic(
                Constants.CHECKIN_CHAR_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
            )
            service.addCharacteristic(checkInChar)

            // Response characteristic (READ + NOTIFY)
            val responseChar = BluetoothGattCharacteristic(
                Constants.RESPONSE_CHAR_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
            val cccd = BluetoothGattDescriptor(
                Constants.DESCRIPTOR_UUID,
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
            )
            responseChar.addDescriptor(cccd)
            service.addCharacteristic(responseChar)

            gattServer?.addService(service)
            _isRunning.value = true
            return true
        } catch (e: SecurityException) {
            return false
        }
    }

    private fun processCheckInPayload(address: String, payload: ByteArray) {
        scope.launch {
            try {
                val jsonStr = String(payload, Charsets.UTF_8)
                val json = gson.fromJson(jsonStr, JsonObject::class.java)
                val studentId = json.get("studentId")?.asString ?: return@launch
                val studentName = json.get("studentName")?.asString ?: "Student $studentId"
                val deviceId = json.get("deviceId")?.asString ?: ""
                val enrollmentKey = json.get("enrollmentKey")?.asString ?: ""
                val challenge = json.get("challenge")?.asString ?: return@launch
                val timestamp = json.get("timestamp")?.asLong ?: System.currentTimeMillis()
                val hmac = json.get("hmac")?.asString ?: return@launch
                val rssiMedian = json.get("rssiMedian")?.asInt ?: -100
                val rssiSamplesArr = json.get("rssiSamples")?.asJsonArray
                val rssiSamples = rssiSamplesArr?.map { it.asInt } ?: listOf(rssiMedian)

                val record = sessionManager.processCheckIn(
                    studentId = studentId,
                    studentName = studentName,
                    deviceId = deviceId,
                    enrollmentKey = enrollmentKey,
                    challenge = challenge,
                    timestamp = timestamp,
                    hmac = hmac,
                    rssiMedian = rssiMedian,
                    rssiSamples = rssiSamples
                )

                _lastCheckIn.value = record
            } catch (e: Exception) {
                // Invalid payload - ignore
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        try {
            gattServer?.clearServices()
            gattServer?.close()
        } catch (_: Exception) {
        }
        gattServer = null
        connectedDeviceSet.clear()
        pendingWrites.clear()
        _connectedDevices.value = 0
        _isRunning.value = false
    }
}

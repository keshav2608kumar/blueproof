package com.blueproof.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build
import com.blueproof.session.ChallengeGenerator
import com.blueproof.util.Constants
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import java.util.*

class GattClient(private val context: Context) {

    data class CheckInResult(
        val success: Boolean,
        val status: String,
        val reason: String?,
        val sessionId: String?,
        val teacherName: String?,
        val checkInTime: Long?
    )

    data class SessionInfo(
        val sessionId: String,
        val challenge: String,
        val expiresAt: Long,
        val teacherName: String,
        val teacherId: String = "TCH-001",
        val sessionName: String
    )

    private val gson = Gson()

    @SuppressLint("MissingPermission")
    suspend fun connectAndCheckIn(
        deviceAddress: String,
        studentId: String,
        studentName: String = "",
        deviceId: String,
        enrollmentKey: String,
        rssiMedian: Int,
        rssiSamples: List<Int>
    ): CheckInResult = withContext(Dispatchers.IO) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            ?: return@withContext CheckInResult(false, "FAILED", "Bluetooth not available", null, null, null)

        val adapter = bluetoothManager.adapter
            ?: return@withContext CheckInResult(false, "FAILED", "No Bluetooth adapter", null, null, null)

        val device = try {
            adapter.getRemoteDevice(deviceAddress)
        } catch (e: Exception) {
            return@withContext CheckInResult(false, "FAILED", "Invalid device address", null, null, null)
        }

        val result = CompletableDeferred<CheckInResult>()
        var gatt: BluetoothGatt? = null
        val timeoutJob = CoroutineScope(Dispatchers.IO).launch {
            delay(15_000)
            if (result.isActive) {
                result.complete(CheckInResult(false, "TIMEOUT", "Connection timed out. Move closer to teacher.", null, null, null))
                try {
                    withContext(Dispatchers.Main) {
                        gatt?.disconnect()
                        gatt?.close()
                    }
                } catch (_: Exception) {}
            }
        }

        fun safeComplete(res: CheckInResult) {
            if (result.isActive) {
                result.complete(res)
            }
            try {
                gatt?.disconnect()
                gatt?.close()
            } catch (_: Exception) {}
        }

        var currentSessionInfo: SessionInfo? = null

        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt?, status: Int, newState: Int) {
                try {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        // Request high MTU for JSON payloads
                        val mtuRequested = g?.requestMtu(512) ?: false
                        if (!mtuRequested) {
                            g?.discoverServices()
                        }
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        if (result.isActive) {
                            safeComplete(CheckInResult(false, "DISCONNECTED", "Connection closed by teacher", null, null, null))
                        }
                    }
                } catch (e: Exception) {
                    safeComplete(CheckInResult(false, "FAILED", "Connection error: ${e.message}", null, null, null))
                }
            }

            override fun onMtuChanged(g: BluetoothGatt?, mtu: Int, status: Int) {
                try {
                    g?.discoverServices()
                } catch (e: Exception) {
                    safeComplete(CheckInResult(false, "FAILED", "Discovery error: ${e.message}", null, null, null))
                }
            }

            override fun onServicesDiscovered(g: BluetoothGatt?, status: Int) {
                try {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        safeComplete(CheckInResult(false, "FAILED", "Service discovery failed (code $status)", null, null, null))
                        return
                    }

                    val service = g?.getService(Constants.BLE_SERVICE_UUID)
                    if (service == null) {
                        safeComplete(CheckInResult(false, "FAILED", "BlueProof service not found on teacher device", null, null, null))
                        return
                    }

                    val sessionInfoChar = service.getCharacteristic(Constants.SESSION_INFO_CHAR_UUID)
                    if (sessionInfoChar == null) {
                        safeComplete(CheckInResult(false, "FAILED", "Session info characteristic not found", null, null, null))
                        return
                    }

                    g.readCharacteristic(sessionInfoChar)
                } catch (e: Exception) {
                    safeComplete(CheckInResult(false, "FAILED", "Read error: ${e.message}", null, null, null))
                }
            }

            // Android 13+ (API 33) overload
            override fun onCharacteristicRead(
                g: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int
            ) {
                handleRead(g, characteristic, value, status)
            }

            // Legacy Android overload
            @Deprecated("Deprecated in Java")
            override fun onCharacteristicRead(
                g: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                if (g != null && characteristic != null) {
                    @Suppress("DEPRECATION")
                    val value = characteristic.value ?: byteArrayOf()
                    handleRead(g, characteristic, value, status)
                }
            }

            private fun handleRead(
                g: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int
            ) {
                try {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        safeComplete(CheckInResult(false, "FAILED", "Failed to read data from teacher (code $status)", null, null, null))
                        return
                    }

                    when (characteristic.uuid) {
                        Constants.SESSION_INFO_CHAR_UUID -> {
                            val data = String(value, Charsets.UTF_8)
                            val json = gson.fromJson(data, JsonObject::class.java)
                            val sessionInfo = SessionInfo(
                                sessionId = json.get("sessionId")?.asString ?: "",
                                challenge = json.get("challenge")?.asString ?: "",
                                expiresAt = json.get("expiresAt")?.asLong ?: 0L,
                                teacherName = json.get("teacherName")?.asString ?: "",
                                teacherId = json.get("teacherId")?.asString ?: "TCH-001",
                                sessionName = json.get("sessionName")?.asString ?: ""
                            )
                            currentSessionInfo = sessionInfo

                            if (sessionInfo.sessionId.isEmpty() || sessionInfo.challenge.isEmpty()) {
                                safeComplete(CheckInResult(false, "FAILED", "Received empty session info", null, null, null))
                                return
                            }

                            // Compute HMAC using challenge and enrollmentKey
                            val timestamp = System.currentTimeMillis()
                            val hmac = ChallengeGenerator.computeHmac(
                                sessionInfo.challenge, studentId, timestamp, enrollmentKey
                            )

                            val payload = JsonObject().apply {
                                addProperty("studentId", studentId)
                                addProperty("studentName", studentName)
                                addProperty("deviceId", deviceId)
                                addProperty("enrollmentKey", enrollmentKey)
                                addProperty("challenge", sessionInfo.challenge)
                                addProperty("timestamp", timestamp)
                                addProperty("hmac", hmac)
                                addProperty("rssiMedian", rssiMedian)
                                add("rssiSamples", gson.toJsonTree(rssiSamples))
                            }

                            val service = g.getService(Constants.BLE_SERVICE_UUID)
                            val checkInChar = service?.getCharacteristic(Constants.CHECKIN_CHAR_UUID)
                            if (checkInChar == null) {
                                safeComplete(CheckInResult(false, "FAILED", "Check-in characteristic not found", null, null, null))
                                return
                            }

                            val payloadBytes = payload.toString().toByteArray(Charsets.UTF_8)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                g.writeCharacteristic(checkInChar, payloadBytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                            } else {
                                @Suppress("DEPRECATION")
                                checkInChar.value = payloadBytes
                                @Suppress("DEPRECATION")
                                checkInChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                                @Suppress("DEPRECATION")
                                g.writeCharacteristic(checkInChar)
                            }
                        }
                        Constants.RESPONSE_CHAR_UUID -> {
                            val data = String(value, Charsets.UTF_8)
                            val json = gson.fromJson(data, JsonObject::class.java)
                            val respStatus = json.get("status")?.asString ?: "UNKNOWN"
                            val reason = json.get("reason")?.asString
                            val checkInTime = json.get("checkInTime")?.asLong

                            safeComplete(
                                CheckInResult(
                                    success = respStatus == Constants.STATUS_ACCEPTED,
                                    status = respStatus,
                                    reason = reason,
                                    sessionId = currentSessionInfo?.sessionId,
                                    teacherName = currentSessionInfo?.teacherName,
                                    checkInTime = checkInTime
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    safeComplete(CheckInResult(false, "FAILED", "Data processing error: ${e.message}", null, null, null))
                }
            }

            override fun onCharacteristicWrite(
                g: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                try {
                    if (characteristic?.uuid == Constants.CHECKIN_CHAR_UUID) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            CoroutineScope(Dispatchers.IO).launch {
                                delay(300) // Brief delay for server processing
                                val service = g?.getService(Constants.BLE_SERVICE_UUID)
                                val responseChar = service?.getCharacteristic(Constants.RESPONSE_CHAR_UUID)
                                if (responseChar != null && g != null) {
                                    try {
                                        g.readCharacteristic(responseChar)
                                    } catch (_: Exception) {
                                        safeComplete(CheckInResult(true, "ACCEPTED", "Check-in accepted", currentSessionInfo?.sessionId, currentSessionInfo?.teacherName, System.currentTimeMillis()))
                                    }
                                } else {
                                    safeComplete(CheckInResult(true, "ACCEPTED", "Check-in accepted", currentSessionInfo?.sessionId, currentSessionInfo?.teacherName, System.currentTimeMillis()))
                                }
                            }
                        } else {
                            safeComplete(CheckInResult(false, "FAILED", "Write failed (code $status)", null, null, null))
                        }
                    }
                } catch (e: Exception) {
                    safeComplete(CheckInResult(false, "FAILED", "Write error: ${e.message}", null, null, null))
                }
            }
        }

        // Connect GATT on Main thread (prevents Android Handler/Looper crash)
        try {
            gatt = withContext(Dispatchers.Main) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
                } else {
                    device.connectGatt(context, false, callback)
                }
            }
        } catch (e: Exception) {
            timeoutJob.cancel()
            return@withContext CheckInResult(false, "FAILED", "Connection failed: ${e.message}", null, null, null)
        }

        try {
            val finalResult = result.await()
            timeoutJob.cancel()
            try {
                withContext(Dispatchers.Main) {
                    gatt?.disconnect()
                    gatt?.close()
                }
            } catch (_: Exception) {}
            finalResult
        } catch (e: Exception) {
            timeoutJob.cancel()
            try {
                withContext(Dispatchers.Main) {
                    gatt?.disconnect()
                    gatt?.close()
                }
            } catch (_: Exception) {}
            CheckInResult(false, "FAILED", "Error: ${e.message}", null, null, null)
        }
    }
}

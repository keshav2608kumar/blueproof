package com.blueproof.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import com.blueproof.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BleAdvertiser(private val context: Context) {

    private var advertiser: BluetoothLeAdvertiser? = null
    private var advertiseCallback: AdvertiseCallback? = null

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _advertisingError = MutableStateFlow<String?>(null)
    val advertisingError: StateFlow<String?> = _advertisingError.asStateFlow()

    fun isSupported(): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: return false
        return adapter.isMultipleAdvertisementSupported
    }

    @SuppressLint("MissingPermission")
    fun startAdvertising(sessionId: String): Boolean {
        if (_isAdvertising.value) return true

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            _advertisingError.value = "Bluetooth is not enabled"
            return false
        }

        if (!adapter.isMultipleAdvertisementSupported) {
            _advertisingError.value = "This device does not support BLE advertising"
            return false
        }

        advertiser = adapter.bluetoothLeAdvertiser
        if (advertiser == null) {
            _advertisingError.value = "Failed to get BLE advertiser"
            return false
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .setTimeout(0) // Advertise indefinitely
            .build()

        // Encode short session ID (first 4 bytes of session UUID hash)
        val sessionBytes = sessionId.hashCode().let { hash ->
            byteArrayOf(
                (hash shr 24).toByte(),
                (hash shr 16).toByte(),
                (hash shr 8).toByte(),
                hash.toByte()
            )
        }

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(ParcelUuid(Constants.BLE_SERVICE_UUID))
            .addManufacturerData(Constants.BLE_MANUFACTURER_ID, sessionBytes)
            .build()

        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                _isAdvertising.value = true
                _advertisingError.value = null
            }

            override fun onStartFailure(errorCode: Int) {
                _isAdvertising.value = false
                _advertisingError.value = when (errorCode) {
                    ADVERTISE_FAILED_ALREADY_STARTED -> "Already advertising"
                    ADVERTISE_FAILED_DATA_TOO_LARGE -> "Advertise data too large"
                    ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "Feature not supported"
                    ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal error"
                    ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many advertisers"
                    else -> "Advertising failed (error $errorCode)"
                }
            }
        }

        try {
            advertiser?.startAdvertising(settings, data, advertiseCallback)
            return true
        } catch (e: SecurityException) {
            _advertisingError.value = "Bluetooth permission not granted"
            return false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        try {
            advertiseCallback?.let { advertiser?.stopAdvertising(it) }
        } catch (_: SecurityException) {
        }
        _isAdvertising.value = false
        advertiseCallback = null
    }
}

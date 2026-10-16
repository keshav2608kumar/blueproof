package com.blueproof.ui.screens

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blueproof.ui.theme.NavyPrimary
import com.blueproof.ui.theme.StatusAccepted
import com.blueproof.ui.theme.StatusRejected
import com.blueproof.util.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val adapter = bluetoothManager?.adapter

    val isBtEnabled = adapter?.isEnabled == true
    val isAdvertisingSupported = adapter?.isMultipleAdvertisementSupported == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Device Status") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyPrimary,
                    titleContentColor = MaterialTheme.colorScheme.surface,
                    navigationIconContentColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bluetooth Hardware Status
            Text(
                text = "Bluetooth Capabilities",
                style = MaterialTheme.typography.titleMedium,
                color = NavyPrimary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingItem(
                        label = "Bluetooth Hardware",
                        value = if (adapter != null) "Available" else "Not Available",
                        isOk = adapter != null
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    SettingItem(
                        label = "Bluetooth State",
                        value = if (isBtEnabled) "Enabled" else "Disabled",
                        isOk = isBtEnabled
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    SettingItem(
                        label = "BLE Advertising Support",
                        value = if (isAdvertisingSupported) "Supported (Teacher Mode OK)" else "Not Supported (Student Only)",
                        isOk = isAdvertisingSupported
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    SettingItem(
                        label = "BLE Scanning Support",
                        value = "Supported",
                        isOk = true
                    )
                }
            }

            // System Information
            Text(
                text = "System Information",
                style = MaterialTheme.typography.titleMedium,
                color = NavyPrimary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingItem(
                        label = "Android Version",
                        value = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                        isOk = true
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    SettingItem(
                        label = "Device Model",
                        value = "${Build.MANUFACTURER} ${Build.MODEL}",
                        isOk = true
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    SettingItem(
                        label = "App Version",
                        value = "BlueProof v1.0.0",
                        isOk = true
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    SettingItem(
                        label = "Default RSSI Threshold",
                        value = "${Constants.DEFAULT_RSSI_THRESHOLD} dBm",
                        isOk = true
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    SettingItem(
                        label = "Challenge Validity",
                        value = "${Constants.CHALLENGE_VALIDITY_MS / 1000}s rotation",
                        isOk = true
                    )
                }
            }

            // Security Notes
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Anti-Proxy Safeguards",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• 32-byte cryptographically secure challenges rotate every 30s\n" +
                                "• HMAC-SHA256 device-bound check-in signatures\n" +
                                "• Multi-sample RSSI proximity verification (min 5 samples)\n" +
                                "• Database-level unique constraint prevents duplicate check-ins\n" +
                                "• All events recorded in local tamper-evident audit log",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingItem(
    label: String,
    value: String,
    isOk: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (isOk) StatusAccepted else StatusRejected
        )
    }
}

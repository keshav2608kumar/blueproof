package com.blueproof.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blueproof.ble.BleScanner
import com.blueproof.ui.components.RequestBlePermissions
import com.blueproof.ui.theme.*
import com.blueproof.util.RssiAnalyzer
import com.blueproof.viewmodel.StudentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleScanScreen(
    viewModel: StudentViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scanState by viewModel.scanState.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val discoveredSessions by viewModel.discoveredSessions.collectAsState()
    val checkInResult by viewModel.checkInResult.collectAsState()
    var permissionsGranted by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopScanning()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find Attendance Session") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stopScanning()
                        viewModel.resetScanState()
                        onBack()
                    }) {
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // BLE Permissions check
            RequestBlePermissions(
                onAllGranted = {
                    permissionsGranted = true
                    viewModel.startScanning()
                }
            )

            // Result Card (when completed)
            if (checkInResult != null) {
                ResultCard(
                    result = checkInResult!!,
                    onDone = {
                        viewModel.resetScanState()
                        onBack()
                    },
                    onRetry = {
                        viewModel.resetScanState()
                        viewModel.startScanning()
                    }
                )
                return@Column
            }

            // Scanning animation & controls
            if (isScanning) {
                val infiniteTransition = rememberInfiniteTransition(label = "scan")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotation"
                )

                Spacer(modifier = Modifier.height(16.dp))
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    tint = TealAccent,
                    modifier = Modifier
                        .size(48.dp)
                        .rotate(rotation)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Scanning for teacher's broadcast...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { viewModel.stopScanning() }) {
                    Text("Stop Scanning")
                }
            } else if (scanState == StudentViewModel.ScanState.CONNECTING ||
                scanState == StudentViewModel.ScanState.CHECKING_IN) {
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator(color = TealAccent)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (scanState == StudentViewModel.ScanState.CONNECTING)
                        "Connecting to teacher..."
                    else
                        "Submitting cryptographically signed check-in...",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.startScanning() },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    enabled = permissionsGranted
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Scanning")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Discovered Sessions List
            if (discoveredSessions.isEmpty() && isScanning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Make sure you're near your teacher\nand their session is active.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(discoveredSessions, key = { it.deviceAddress }) { session ->
                        DiscoveredSessionCard(
                            session = session,
                            isCheckingIn = scanState == StudentViewModel.ScanState.CONNECTING ||
                                    scanState == StudentViewModel.ScanState.CHECKING_IN,
                            onCheckIn = { viewModel.checkIn(session) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoveredSessionCard(
    session: BleScanner.DiscoveredSession,
    isCheckingIn: Boolean,
    onCheckIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val signalStrength = RssiAnalyzer.signalStrength(session.rssi)
    val signalPct = RssiAnalyzer.signalPercentage(session.rssi)
    val signalColor = when {
        session.rssi > -65 -> StatusAccepted
        session.rssi > -75 -> StatusPending
        else -> StatusRejected
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Teacher Session",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Session Tag: ${session.sessionIdShort}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Signal indicator
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${session.rssi} dBm",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = signalColor
                    )
                    Text(
                        text = "$signalStrength (${session.rssiSamples.size} samples)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Signal bar
            LinearProgressIndicator(
                progress = { signalPct.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = signalColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCheckIn,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                enabled = !isCheckingIn
            ) {
                Text("Check In to This Session")
            }
        }
    }
}

@Composable
fun ResultCard(
    result: com.blueproof.ble.GattClient.CheckInResult,
    onDone: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val icon = if (result.success) Icons.Default.CheckCircle else Icons.Default.Cancel
            val color = if (result.success) StatusAccepted else StatusRejected

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (result.success) "Check-In Verified!" else "Check-In Failed",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = result.reason ?: if (result.success)
                    "Your attendance has been cryptographically verified and recorded."
                else
                    "Your check-in could not be verified.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (result.success && !result.teacherName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Instructor: ${result.teacherName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TealAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (result.success) {
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Text("Done")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDone,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

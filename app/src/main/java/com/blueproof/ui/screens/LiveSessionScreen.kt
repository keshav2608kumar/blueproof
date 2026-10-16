package com.blueproof.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blueproof.ui.components.AttendanceListItem
import com.blueproof.ui.components.StatusCard
import com.blueproof.ui.theme.*
import com.blueproof.viewmodel.TeacherViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveSessionScreen(
    viewModel: TeacherViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSession by viewModel.currentSession.collectAsState()
    val attendanceList by viewModel.attendanceList.collectAsState()
    val sessionStats by viewModel.sessionStats.collectAsState()
    val isAdvertising by viewModel.isAdvertising.collectAsState()
    val connectedDevices by viewModel.connectedDevices.collectAsState()
    val enrolledCount by viewModel.enrolledCount.collectAsState()

    var remainingSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(currentSession) {
        val session = currentSession ?: return@LaunchedEffect
        val endTime = session.startTime + (session.durationMinutes * 60_000L)
        while (true) {
            val remaining = (endTime - System.currentTimeMillis()) / 1000L
            remainingSeconds = remaining.coerceAtLeast(0L)
            delay(1000)
        }
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val countdownText = String.format("%02d:%02d", minutes, seconds)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentSession?.name ?: "Live Session") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.endSession(); onBack() }) {
                        Icon(Icons.Default.Stop, contentDescription = "End Session", tint = StatusRejected)
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
        ) {
            // Live Status Card
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isAdvertising) Icons.Default.Sensors else Icons.Default.SensorsOff,
                                contentDescription = null,
                                tint = if (isAdvertising) StatusAccepted else StatusRejected,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAdvertising) "BROADCASTING" else "PAUSED",
                                fontWeight = FontWeight.Bold,
                                color = if (isAdvertising) StatusAccepted else StatusRejected,
                                fontSize = 13.sp
                            )
                        }

                        // Countdown
                        Text(
                            text = countdownText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Session Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isAdvertising) {
                            OutlinedButton(
                                onClick = { viewModel.pauseSession() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pause")
                            }
                        } else {
                            Button(
                                onClick = { viewModel.resumeSession() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resume")
                            }
                        }

                        Button(
                            onClick = { viewModel.endSession(); onBack() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusRejected)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("End")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stat Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusCard(
                    title = "Checked In",
                    value = attendanceList.size.toString(),
                    color = NavyPrimary,
                    modifier = Modifier.weight(1f)
                )
                StatusCard(
                    title = "Verified",
                    value = sessionStats.accepted.toString(),
                    color = StatusAccepted,
                    modifier = Modifier.weight(1f)
                )
                StatusCard(
                    title = "Flagged",
                    value = (sessionStats.rejected + sessionStats.pending).toString(),
                    color = if (sessionStats.rejected > 0) StatusRejected else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Attendance List Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Check-Ins (${attendanceList.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = NavyPrimary
                )
                Text(
                    text = "Connected: $connectedDevices",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Attendance Records List
            if (attendanceList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Waiting for students to check in...\nStudents should scan for this session.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(attendanceList) { record ->
                        AttendanceListItem(record = record)
                    }
                }
            }
        }
    }
}

package com.blueproof.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.blueproof.viewmodel.StudentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    viewModel: StudentViewModel,
    onJoinSessionClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSwitchStudentClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentStudent by viewModel.currentStudent.collectAsState()
    val attendanceHistory by viewModel.attendanceHistory.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAttendanceHistory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Portal") },
                actions = {
                    IconButton(onClick = onHistoryClick) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyPrimary,
                    titleContentColor = MaterialTheme.colorScheme.surface,
                    actionIconContentColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Student Profile Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = NavyPrimary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = currentStudent?.name ?: "Student",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "ID: ${currentStudent?.studentId ?: "—"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                viewModel.logout()
                                onSwitchStudentClick()
                            }
                        ) {
                            Text("Switch ID", color = TealAccent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Join Session Action Button
            item {
                Button(
                    onClick = onJoinSessionClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
                ) {
                    Icon(Icons.Default.BluetoothSearching, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Join Attendance Session",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Stats
            item {
                val totalCheckIns = attendanceHistory.size
                val acceptedCount = attendanceHistory.count { it.status == "ACCEPTED" }
                val rate = if (totalCheckIns > 0) (acceptedCount * 100 / totalCheckIns) else 0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatusCard(
                        title = "Check-Ins",
                        value = totalCheckIns.toString(),
                        color = NavyPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatusCard(
                        title = "Verified",
                        value = "$rate%",
                        color = StatusAccepted,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Recent Check-Ins Header
            item {
                Text(
                    text = "Recent Check-Ins",
                    style = MaterialTheme.typography.titleLarge,
                    color = NavyPrimary
                )
            }

            if (attendanceHistory.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No attendance records yet.\nJoin a session when your teacher starts one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(attendanceHistory.take(5)) { record ->
                    AttendanceListItem(record = record)
                }
            }
        }
    }
}

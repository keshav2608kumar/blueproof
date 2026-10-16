package com.blueproof.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blueproof.data.db.entity.SessionEntity
import com.blueproof.ui.components.StatusCard
import com.blueproof.ui.theme.*
import com.blueproof.util.AttendanceCalculator
import com.blueproof.viewmodel.TeacherViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(
    viewModel: TeacherViewModel,
    onStartSessionClick: () -> Unit,
    onViewLiveSessionClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSessionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSession by viewModel.currentSession.collectAsState()
    val recentSessions by viewModel.recentSessions.collectAsState()
    val enrolledCount by viewModel.enrolledCount.collectAsState()
    val enrolledStudents by viewModel.enrolledStudents.collectAsState()
    val teacherName by viewModel.teacherName.collectAsState()
    val teacherId by viewModel.teacherId.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showEnrolledStudentsDialog by remember { mutableStateOf(false) }
    var editName by remember(teacherName) { mutableStateOf(teacherName) }
    var editId by remember(teacherId) { mutableStateOf(teacherId) }

    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teacher Dashboard") },
                actions = {
                    IconButton(onClick = onAnalyticsClick) {
                        Icon(Icons.Default.BarChart, contentDescription = "Analytics")
                    }
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
        },
        floatingActionButton = {
            if (currentSession == null) {
                FloatingActionButton(
                    onClick = onStartSessionClick,
                    containerColor = TealAccent,
                    contentColor = MaterialTheme.colorScheme.surface
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Start Session")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Active Session Banner (if running)
            if (currentSession != null) {
                item {
                    ElevatedCard(
                        onClick = onViewLiveSessionClick,
                        colors = CardDefaults.elevatedCardColors(containerColor = NavyPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SESSION IN PROGRESS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TealLight,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentSession?.name ?: "",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.surface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Tap to view live attendance",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                tint = TealLight,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            // Teacher Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = NavyPrimary.copy(alpha = 0.1f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Badge,
                                        contentDescription = null,
                                        tint = NavyPrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = teacherName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = NavyPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Faculty ID: $teacherId",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = {
                            editName = teacherName
                            editId = teacherId
                            showEditProfileDialog = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = TealAccent
                            )
                        }
                    }
                }
            }

            // Stat Cards Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatusCard(
                        title = "Enrolled",
                        value = enrolledCount.toString(),
                        color = NavyPrimary,
                        icon = Icons.Default.People,
                        subtitle = "Tap to manage",
                        onClick = { showEnrolledStudentsDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    StatusCard(
                        title = "Sessions",
                        value = recentSessions.size.toString(),
                        color = TealAccent,
                        icon = Icons.Default.Event,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Action
            if (currentSession == null) {
                item {
                    Button(
                        onClick = onStartSessionClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                    ) {
                        Icon(Icons.Default.BluetoothSearching, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Attendance Session", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Recent Sessions Header
            item {
                Text(
                    text = "Recent Sessions",
                    style = MaterialTheme.typography.titleLarge,
                    color = NavyPrimary
                )
            }

            if (recentSessions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No sessions yet. Tap 'Start Session' to begin.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(recentSessions) { session ->
                    SessionCard(session = session, onClick = { onSessionClick(session.sessionId) })
                }
            }
        }
    }

    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Faculty Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Faculty Name") },
                        placeholder = { Text("e.g., Dr. Alexander Vance") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editId,
                        onValueChange = { editId = it },
                        label = { Text("Teacher / Faculty ID") },
                        placeholder = { Text("e.g., TCH-1004") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateTeacherProfile(editName, editId)
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Enrolled Students & Device Lock Management Dialog
    if (showEnrolledStudentsDialog) {
        AlertDialog(
            onDismissRequest = { showEnrolledStudentsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.People, contentDescription = null, tint = NavyPrimary)
                    Text("Enrolled Students (${enrolledStudents.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                if (enrolledStudents.isEmpty()) {
                    Text(
                        "No students enrolled yet.\n\nStudents are automatically enrolled with their physical phone on their first valid attendance check-in.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                "If a student lost or replaced their phone, tap 'Reset Lock' so they can check in with their new device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(enrolledStudents) { student ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(student.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Student ID: ${student.studentId}", style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    if (student.deviceId.isNotBlank()) {
                                        Text(
                                            "Phone Locked: ...${student.deviceId.takeLast(8)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NavyPrimary
                                        )
                                    } else {
                                        Text(
                                            "Phone Lock: Unlocked (New phone can bind)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StatusAccepted,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (student.deviceId.isNotBlank()) {
                                            OutlinedButton(
                                                onClick = { viewModel.resetStudentDeviceLock(student.studentId) },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Reset Lock", fontSize = 12.sp)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        TextButton(
                                            onClick = { viewModel.deleteStudent(student.studentId) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            colors = ButtonDefaults.textButtonColors(contentColor = StatusRejected),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Delete", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEnrolledStudentsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun SessionCard(
    session: SessionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(session.startTime))

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = AttendanceCalculator.sessionStatus(session),
                    style = MaterialTheme.typography.labelSmall,
                    color = when (session.status) {
                        "ACTIVE" -> StatusAccepted
                        "PAUSED" -> StatusPending
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

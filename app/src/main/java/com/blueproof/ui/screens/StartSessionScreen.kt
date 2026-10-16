package com.blueproof.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blueproof.ui.components.RequestBlePermissions
import com.blueproof.ui.theme.NavyPrimary
import com.blueproof.ui.theme.StatusRejected
import com.blueproof.ui.theme.TealAccent
import com.blueproof.util.Constants
import com.blueproof.viewmodel.TeacherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartSessionScreen(
    viewModel: TeacherViewModel,
    onSessionStarted: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val savedTeacherName by viewModel.teacherName.collectAsState()
    val savedTeacherId by viewModel.teacherId.collectAsState()

    var sessionName by remember { mutableStateOf("") }
    var teacherName by remember(savedTeacherName) { mutableStateOf(savedTeacherName) }
    var teacherId by remember(savedTeacherId) { mutableStateOf(savedTeacherId) }
    var selectedDuration by remember { mutableIntStateOf(Constants.SESSION_DEFAULT_DURATION_MIN) }
    var rssiThreshold by remember { mutableFloatStateOf(Constants.DEFAULT_RSSI_THRESHOLD.toFloat()) }
    var permissionsGranted by remember { mutableStateOf(false) }

    val advertisingError by viewModel.advertisingError.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val currentSession by viewModel.currentSession.collectAsState()

    val durations = listOf(5, 10, 15, 20, 30, 45, 60)

    LaunchedEffect(currentSession) {
        if (currentSession != null) {
            onSessionStarted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Start Attendance Session") },
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // BLE Permissions check
            RequestBlePermissions(
                onAllGranted = { permissionsGranted = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Session Name Input
            OutlinedTextField(
                value = sessionName,
                onValueChange = { sessionName = it },
                label = { Text("Session / Course Name") },
                placeholder = { Text("e.g., CS101 - Lecture 5") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Teacher Name & ID Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = teacherName,
                    onValueChange = { teacherName = it },
                    label = { Text("Teacher Name") },
                    placeholder = { Text("Dr. Vance") },
                    singleLine = true,
                    modifier = Modifier.weight(1.3f)
                )

                OutlinedTextField(
                    value = teacherId,
                    onValueChange = { teacherId = it },
                    label = { Text("Teacher ID") },
                    placeholder = { Text("TCH-1004") },
                    singleLine = true,
                    modifier = Modifier.weight(0.9f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Duration Selector
            Text(
                text = "Session Duration",
                style = MaterialTheme.typography.titleMedium,
                color = NavyPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                durations.take(4).forEach { min ->
                    FilterChip(
                        selected = selectedDuration == min,
                        onClick = { selectedDuration = min },
                        label = { Text("${min}m") }
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                durations.drop(4).forEach { min ->
                    FilterChip(
                        selected = selectedDuration == min,
                        onClick = { selectedDuration = min },
                        label = { Text("${min}m") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Proximity Threshold Slider
            Text(
                text = "Proximity Threshold: ${rssiThreshold.toInt()} dBm",
                style = MaterialTheme.typography.titleMedium,
                color = NavyPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Controls how close students must be. -75 dBm is recommended for standard classrooms.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = rssiThreshold,
                onValueChange = { rssiThreshold = it },
                valueRange = -90f..-50f,
                steps = 7,
                colors = SliderDefaults.colors(
                    thumbColor = TealAccent,
                    activeTrackColor = TealAccent
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Far (-90 dBm)", style = MaterialTheme.typography.labelSmall)
                Text("Near (-50 dBm)", style = MaterialTheme.typography.labelSmall)
            }

            // Error Display
            val err = advertisingError ?: errorMessage
            if (err != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = StatusRejected.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = err,
                        color = StatusRejected,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Start Button
            Button(
                onClick = {
                    val name = sessionName.ifBlank { "Class Session - ${System.currentTimeMillis() % 10000}" }
                    viewModel.startSession(
                        name = name,
                        teacherName = teacherName.ifBlank { savedTeacherName },
                        teacherId = teacherId.ifBlank { savedTeacherId },
                        durationMinutes = selectedDuration,
                        rssiThreshold = rssiThreshold.toInt()
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                enabled = permissionsGranted
            ) {
                Text(
                    text = "Broadcast & Start Session",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

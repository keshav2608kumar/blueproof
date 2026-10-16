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
import com.blueproof.ui.theme.NavyPrimary
import com.blueproof.ui.theme.StatusAccepted
import com.blueproof.ui.theme.StatusRejected
import com.blueproof.ui.theme.TealAccent
import com.blueproof.viewmodel.StudentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentRegistrationScreen(
    viewModel: StudentViewModel,
    onRegistered: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isRegisterMode by remember { mutableStateOf(true) }
    var studentId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    val currentStudent by viewModel.currentStudent.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(currentStudent) {
        if (currentStudent != null) {
            onRegistered()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isRegisterMode) "Student Registration" else "Student Sign In") },
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isRegisterMode) "Enroll Your Device" else "Sign In to Your Account",
                style = MaterialTheme.typography.headlineMedium,
                color = NavyPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isRegisterMode)
                    "Your device will be enrolled with a unique cryptographic key for anti-proxy verification."
                else
                    "Enter your Student ID to access your attendance records.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = studentId,
                onValueChange = { studentId = it },
                label = { Text("Student ID") },
                placeholder = { Text("e.g., STU-2024-001") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (isRegisterMode) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    placeholder = { Text("e.g., Jane Doe") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage ?: "",
                    color = StatusRejected,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (isRegisterMode) {
                        if (studentId.isNotBlank() && name.isNotBlank()) {
                            viewModel.registerStudent(studentId, name)
                        }
                    } else {
                        if (studentId.isNotBlank()) {
                            viewModel.loginStudent(studentId)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                enabled = studentId.isNotBlank() && (!isRegisterMode || name.isNotBlank())
            ) {
                Text(
                    text = if (isRegisterMode) "Enroll & Continue" else "Sign In",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = {
                    isRegisterMode = !isRegisterMode
                    viewModel.clearError()
                }
            ) {
                Text(
                    text = if (isRegisterMode)
                        "Already enrolled? Sign in here"
                    else
                        "Need to enroll? Register here",
                    color = TealAccent
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Device Info
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Device Fingerprint",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = viewModel.deviceId.take(16) + "...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

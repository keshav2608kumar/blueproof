package com.blueproof.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blueproof.ui.components.BarChart
import com.blueproof.ui.components.DonutChart
import com.blueproof.ui.components.LineChart
import com.blueproof.ui.components.StatusCard
import com.blueproof.ui.theme.*
import com.blueproof.viewmodel.AnalyticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val analyticsData by viewModel.analyticsData.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAnalytics()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance Analytics") },
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
            // Stat Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusCard(
                    title = "Sessions",
                    value = analyticsData.totalSessions.toString(),
                    color = NavyPrimary,
                    icon = Icons.Default.BarChart,
                    modifier = Modifier.weight(1f)
                )
                StatusCard(
                    title = "Check-Ins",
                    value = analyticsData.totalCheckIns.toString(),
                    color = TealAccent,
                    icon = Icons.Default.People,
                    modifier = Modifier.weight(1f)
                )
                StatusCard(
                    title = "Overall Rate",
                    value = "${analyticsData.overallAttendanceRate.toInt()}%",
                    color = StatusAccepted,
                    icon = Icons.Default.Percent,
                    modifier = Modifier.weight(1f)
                )
            }

            // Attendance Breakdown (Donut)
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Attendance Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        color = NavyPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DonutChart(
                        segments = analyticsData.statusDonut,
                        colors = listOf(StatusAccepted, StatusRejected, StatusPending)
                    )
                }
            }

            // Present vs Absent (Bar Chart)
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Verified Present vs Flagged / Rejected",
                        style = MaterialTheme.typography.titleMedium,
                        color = NavyPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val maxVal = analyticsData.presentAbsentBreakdown.maxOfOrNull { it.second } ?: 10f
                    BarChart(
                        data = analyticsData.presentAbsentBreakdown,
                        maxValue = maxVal.coerceAtLeast(1f),
                        barColor = NavyPrimary
                    )
                }
            }

            // Session Trend (Line Chart)
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Attendance Trend (% over recent sessions)",
                        style = MaterialTheme.typography.titleMedium,
                        color = NavyPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LineChart(
                        data = analyticsData.sessionTrend,
                        lineColor = TealAccent
                    )
                }
            }
        }
    }
}
